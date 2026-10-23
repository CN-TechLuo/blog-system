package com.blog.blogsystem.service;

import org.springframework.stereotype.Service;

import com.blog.blogsystem.mapper.TokenBlacklistMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 黑名单（登出吊销）： - 内存缓存热点 jti，避免每个请求都查库 - DB 持久化，重启后从库中加载未过期项 - 定时清理过期记录
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final Map<String, Boolean> blacklistCache = new ConcurrentHashMap<>();

    public TokenBlacklistService(TokenBlacklistMapper tokenBlacklistMapper) {
        this.tokenBlacklistMapper = tokenBlacklistMapper;
    }

    /** 应用启动时从 DB 预热黑名单缓存（表未迁移时容错跳过） */
    @PostConstruct
    public void loadFromDb() {
        try {
            List<String> active = tokenBlacklistMapper.findActive();
            active.forEach(jti -> blacklistCache.put(jti, Boolean.TRUE));
            log.info("JWT 黑名单加载完成: {} 个有效条目", active.size());
        } catch (Exception e) {
            log.warn("JWT 黑名单加载失败（可能表尚未创建，Flyway 迁移后生效）", e);
        }
    }

    /**
     * 判断 token 是否已被吊销（先查缓存，未命中回源 DB 并回填缓存）
     *
     * @param jti token 唯一标识
     * @return true=已吊销
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        if (blacklistCache.containsKey(jti)) return true;
        try {
            String hit = tokenBlacklistMapper.findByJti(jti);
            if (hit != null) {
                blacklistCache.put(jti, Boolean.TRUE);
                return true;
            }
        } catch (Exception e) {
            // fail-closed：黑名单存储不可用时拒绝 token，避免已登出令牌复活
            log.error("JWT 黑名单查询失败，按 fail-closed 拒绝: jti={}", jti, e);
            return true;
        }
        return false;
    }

    /**
     * 吊销 token（写入缓存并落库）
     *
     * @param jti        token 唯一标识
     * @param expireTime 过期时间（到期后可被清理）
     */
    public void blacklist(String jti, LocalDateTime expireTime) {
        if (jti == null || jti.isBlank()) return;
        blacklistCache.put(jti, Boolean.TRUE);
        try {
            tokenBlacklistMapper.insert(jti, expireTime);
        } catch (Exception e) {
            // 本地缓存仍标记为已吊销；调用方（登出）还会递增 tokenVersion 作为权威撤销手段
            log.error("JWT 黑名单持久化失败（本地缓存仍生效，tokenVersion 将撤销全部令牌）: jti={}", jti, e);
        }
    }

    /** 定时清理已过期黑名单记录并重建缓存（每小时） */
    @Scheduled(fixedRate = 3600_000)
    public void cleanExpired() {
        try {
            int rows = tokenBlacklistMapper.deleteExpired();
            if (rows > 0) {
                blacklistCache.clear();
                loadFromDb();
                log.info("JWT 黑名单清理: 删除 {} 条过期记录", rows);
            }
        } catch (Exception e) {
            log.warn("JWT 黑名单清理失败", e);
        }
    }
}
