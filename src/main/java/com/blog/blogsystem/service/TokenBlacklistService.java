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
 * JWT 黑名单（登出吊销）：
 * - 内存缓存热点 jti，避免每个请求都查库
 * - DB 持久化，重启后从库中加载未过期项
 * - 定时清理过期记录
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final Map<String, Boolean> blacklistCache = new ConcurrentHashMap<>();

    public TokenBlacklistService(TokenBlacklistMapper tokenBlacklistMapper) {
        this.tokenBlacklistMapper = tokenBlacklistMapper;
    }

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

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) return false;
        if (blacklistCache.containsKey(jti)) return true;
        try {
            String hit = tokenBlacklistMapper.findByJti(jti);
            if (hit != null) {
                blacklistCache.put(jti, Boolean.TRUE);
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void blacklist(String jti, LocalDateTime expireTime) {
        if (jti == null || jti.isBlank()) return;
        blacklistCache.put(jti, Boolean.TRUE);
        try {
            tokenBlacklistMapper.insert(jti, expireTime);
        } catch (Exception e) {
            log.warn("JWT 黑名单写入失败: jti={}", jti, e);
        }
    }

    @Scheduled(fixedRate = 3600_000)
    public void cleanExpired() {
        try {
            int rows = tokenBlacklistMapper.deleteExpired();
            if (rows > 0) {
                blacklistCache.clear();
                loadFromDb();
                log.info("JWT 黑名单清理: 删除 {} 条过期记录", rows);
            }
        } catch (Exception ignored) {}
    }
}
