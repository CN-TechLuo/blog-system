package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.service.CaptchaStore;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存验证码存储（单机部署）。多实例部署请切换 CAPTCHA_STORE=redis。
 */
public class InMemoryCaptchaStore implements CaptchaStore {

    private record Entry(String answer, long expireAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void put(String captchaId, String answer, long ttlMs) {
        store.put(captchaId, new Entry(answer, System.currentTimeMillis() + ttlMs));
    }

    @Override
    public String take(String captchaId) {
        Entry entry = store.remove(captchaId);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireAt()) return null;
        return entry.answer();
    }

    @Override
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (now > it.next().getValue().expireAt()) {
                it.remove();
            }
        }
    }

}
