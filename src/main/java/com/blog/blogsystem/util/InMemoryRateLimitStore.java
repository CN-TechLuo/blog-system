package com.blog.blogsystem.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 内存限流存储（单机部署）：滑动窗口（分钟 + 天），
 * 进程内有效，多实例部署请切换 RATE_LIMIT_STORE=redis。
 */
public class InMemoryRateLimitStore implements RateLimitStore {

    private record Window(long startMs, long count) {}

    private static final long MINUTE_MS = 60_000L;
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitStore() {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            windows.entrySet().removeIf(e -> now - e.getValue().startMs() > DAY_MS);
        }, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public boolean isBlocked(String key, int maxPerMinute, int maxPerDay) {
        long now = System.currentTimeMillis();
        Window minute = windows.compute(key, (k, v) ->
                (v == null || now - v.startMs() > MINUTE_MS) ? new Window(now, 1) : new Window(v.startMs(), v.count() + 1));
        Window day = maxPerDay == Integer.MAX_VALUE ? null
                : windows.compute(key + "|day", (k, v) ->
                        (v == null || now - v.startMs() > DAY_MS) ? new Window(now, 1) : new Window(v.startMs(), v.count() + 1));
        boolean blocked = minute.count() > maxPerMinute;
        if (!blocked && day != null) {
            blocked = day.count() > maxPerDay;
        }
        return blocked;
    }

}
