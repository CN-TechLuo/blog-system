package com.blog.blogsystem.service;

/**
 * 验证码存储抽象：单机内存 / Redis（多实例）可插拔，
 * 由 CaptchaConfig 根据 app.captcha.store 选择实现。
 */
public interface CaptchaStore {

    /** 保存验证码答案，ttlMs 后过期 */
    void put(String captchaId, String answer, long ttlMs);

    /** 取出并作废（单次有效）；不存在或已过期返回 null */
    String take(String captchaId);

    /** 清理过期条目（Redis 实现为 no-op，依赖 TTL 自动过期） */
    void cleanExpired();

}
