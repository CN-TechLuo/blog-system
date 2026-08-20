package com.blog.blogsystem.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis 限流存储（多实例部署）：固定窗口（分钟 + 天），
 * 通过 INCR + EXPIRE 实现，键带 TTL 自动过期，无需清理任务。
 * 生产多副本环境必须使用本实现（配合 docker-compose 中的 redis 服务）。
 */
public class RedisRateLimitStore implements RateLimitStore {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStore.class);
    private static final long DAY_SECONDS = 24 * 60 * 60L;

    private final StringRedisTemplate redis;

    public RedisRateLimitStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isBlocked(String key, int maxPerMinute, int maxPerDay) {
        boolean blocked = false;
        try {
            blocked = incr("rl:" + key + ":m", 60) > maxPerMinute;
            if (!blocked && maxPerDay != Integer.MAX_VALUE) {
                blocked = incr("rl:" + key + ":d", DAY_SECONDS) > maxPerDay;
            }
        } catch (Exception e) {
            // Redis 故障时放行（fail-open），避免限流组件拖垮业务；依赖 DB 锁定/敏感词等兜底
            log.warn("Redis 限流执行失败，本次放行: key={}", key, e);
        }
        return blocked;
    }

    private long incr(String key, long ttlSeconds) {
        Long value = redis.opsForValue().increment(key);
        if (value != null && value == 1L) {
            redis.expire(key, Duration.ofSeconds(ttlSeconds));
        }
        return value == null ? 0L : value;
    }

}
