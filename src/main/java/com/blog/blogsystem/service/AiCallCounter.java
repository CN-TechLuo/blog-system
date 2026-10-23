package com.blog.blogsystem.service;

import org.springframework.stereotype.Component;

/**
 * 单次 HTTP 请求内的 AI 上游调用计数器（ThreadLocal）。
 * 用于按上游调用次数而非请求次数计量配额。
 *
 * @author CN-TechLuo
 * @date 2026/10/23
 */
@Component
public class AiCallCounter {

    private final ThreadLocal<Integer> count = ThreadLocal.withInitial(() -> 0);

    public void increment() {
        count.set(count.get() + 1);
    }

    public int getAndReset() {
        int value = count.get();
        count.remove();
        return value;
    }
}
