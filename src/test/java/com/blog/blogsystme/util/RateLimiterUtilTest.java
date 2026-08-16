package com.blog.blogsystme.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterUtilTest {

    @Test
    void shouldBlockAfterExceedingMinuteLimit() {
        String key = "test:minute:" + System.nanoTime();
        assertFalse(RateLimiterUtil.isBlocked(key, 2), "第 1 次不拦截");
        assertFalse(RateLimiterUtil.isBlocked(key, 2), "第 2 次不拦截");
        assertTrue(RateLimiterUtil.isBlocked(key, 2), "第 3 次应拦截");
    }

    @Test
    void dayLimitShouldBlockIndependently() {
        String key = "test:day:" + System.nanoTime();
        assertFalse(RateLimiterUtil.isBlocked(key, 100, 1), "当天第 1 次不拦截");
        assertTrue(RateLimiterUtil.isBlocked(key, 100, 1), "超过日限额应拦截");
    }

    @Test
    void differentKeysShouldNotInterfere() {
        String a = "test:keyA:" + System.nanoTime();
        String b = "test:keyB:" + System.nanoTime();
        assertFalse(RateLimiterUtil.isBlocked(a, 2));
        assertFalse(RateLimiterUtil.isBlocked(a, 2));
        assertTrue(RateLimiterUtil.isBlocked(a, 2));
        assertFalse(RateLimiterUtil.isBlocked(b, 2), "不同键互不影响");
    }

}
