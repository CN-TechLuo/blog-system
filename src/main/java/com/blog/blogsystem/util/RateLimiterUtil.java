package com.blog.blogsystem.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 滑动窗口限流工具（分钟 + 天两个维度）
 * 与业务解耦，供登录/注册/找回密码/AI 等场景复用
 */
public final class RateLimiterUtil {

    private record Window(long startMs, long count) {}

    private static final ConcurrentHashMap<String, Window> WINDOWS = new ConcurrentHashMap<>();
    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            WINDOWS.entrySet().removeIf(e -> now - e.getValue().startMs() > DAY_MS);
        }, 60, 60, TimeUnit.SECONDS);
    }

    private RateLimiterUtil() {}

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
        long now = System.currentTimeMillis();
        Window minute = WINDOWS.compute(key, (k, v) ->
                (v == null || now - v.startMs() > MINUTE_MS) ? new Window(now, 1) : new Window(v.startMs(), v.count() + 1));
        Window day = maxPerDay == Integer.MAX_VALUE ? null
                : WINDOWS.compute(key + "|day", (k, v) ->
                        (v == null || now - v.startMs() > DAY_MS) ? new Window(now, 1) : new Window(v.startMs(), v.count() + 1));
        boolean blocked = minute.count() > maxPerMinute;
        if (!blocked && day != null) {
            blocked = day.count() > maxPerDay;
        }
        return blocked;
    }

}
