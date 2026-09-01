package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimitStoreTest {

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RedisRateLimitStore store = new RedisRateLimitStore(redis);

    @Test
    void blocksWhenCountExceedsLimit() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L, 2L, 3L);

        assertFalse(store.isBlocked("k1", 2, Integer.MAX_VALUE), "第 1 次放行");
        assertFalse(store.isBlocked("k1", 2, Integer.MAX_VALUE), "第 2 次放行");
        assertTrue(store.isBlocked("k1", 2, Integer.MAX_VALUE), "第 3 次拦截");
    }

    @Test
    void setsExpiryOnFirstIncrement() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);

        assertFalse(store.isBlocked("k2", 10, Integer.MAX_VALUE));
        verify(redis).expire(anyString(), eq(java.time.Duration.ofSeconds(60)));
    }

    @Test
    void redisFailureFailsOpen() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenThrow(new RuntimeException("redis down"));

        assertFalse(store.isBlocked("k3", 1, 1), "Redis 故障时放行（fail-open）");
    }

    @Test
    void dayLimitCheckedIndependently() {
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L, 2L);

        assertTrue(store.isBlocked("k4", 100, 1), "日限额 1 被第 2 次调用突破");
    }

}
