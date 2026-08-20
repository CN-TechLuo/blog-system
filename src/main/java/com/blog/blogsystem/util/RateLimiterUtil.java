package com.blog.blogsystem.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 滑动窗口限流工具（分钟 + 天两个维度），与业务解耦，
 * 供登录/注册/找回密码/AI 等场景复用。
 * 底层存储通过 {@link #init(RateLimitStore)} 注入：
 * 单机默认内存实现，多实例由 RateLimitConfig 切换为 Redis 实现。
 */
public final class RateLimiterUtil {

    private static volatile RateLimitStore store = new InMemoryRateLimitStore();

    private RateLimiterUtil() {}

    /** 由 RateLimitConfig 在启动时注入实现 */
    public static void init(RateLimitStore rateLimitStore) {
        if (rateLimitStore != null) {
            store = rateLimitStore;
        }
    }

    /**
     * 判断是否被限流（仅分钟维度）
     * @param key          限流维度键（如 IP+账号）
     * @param maxPerMinute 每分钟最大次数
     * @return true 表示应拒绝请求
     */
    public static boolean isBlocked(String key, int maxPerMinute) {
        return isBlocked(key, maxPerMinute, Integer.MAX_VALUE);
    }

    /**
     * 判断是否被限流（分钟 + 天两个维度）
     * @param key        限流维度键
     * @param maxPerMinute 每分钟最大次数
     * @param maxPerDay  每天最大次数
     * @return true 表示应拒绝请求
     */
    public static boolean isBlocked(String key, int maxPerMinute, int maxPerDay) {
        return store.isBlocked(key, maxPerMinute, maxPerDay);
    }

}
