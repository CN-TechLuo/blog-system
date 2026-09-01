package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.service.CaptchaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 验证码存储（多实例共享）：SET PX 过期 + GETDEL 单次取出。
 * 生产多副本环境必须使用本实现（docker-compose 已内置 redis）。
 */
public class RedisCaptchaStore implements CaptchaStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCaptchaStore.class);

    private final StringRedisTemplate redis;

    public RedisCaptchaStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void put(String captchaId, String answer, long ttlMs) {
        try {
            redis.opsForValue().set("captcha:" + captchaId, answer, Duration.ofMillis(ttlMs));
        } catch (Exception e) {
            log.warn("Redis 验证码写入失败: {}", captchaId, e);
        }
    }

    @Override
    public String take(String captchaId) {
        try {
            // GETDEL 原子取出并删除（Redis 6.2+，docker 内置 redis:7）
            return redis.opsForValue().getAndDelete("captcha:" + captchaId);
        } catch (Exception e) {
            // Redis 故障时返回 null 视为验证码无效，宁可拒绝也不放行弱校验
            log.warn("Redis 验证码读取失败: {}", captchaId, e);
            return null;
        }
    }

    @Override
    public void cleanExpired() {
        // 依赖 TTL 自动过期，无需清理
    }

}
