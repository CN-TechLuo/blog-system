package com.blog.blogsystem.util;

/**
 * 限流存储抽象：单机内存 / Redis（多实例）可插拔，
 * 由 RateLimitConfig 根据 app.rate-limit.store 选择实现。
 */
public interface RateLimitStore {

    /**
     * 判断是否被限流（分钟 + 天两个维度，固定窗口近似）
     * @param key          限流维度键（如 IP+账号）
     * @param maxPerMinute 每分钟最大次数
     * @param maxPerDay    每天最大次数（Integer.MAX_VALUE 表示不限制）
     * @return true 表示应拒绝请求
     */
    boolean isBlocked(String key, int maxPerMinute, int maxPerDay);

}
