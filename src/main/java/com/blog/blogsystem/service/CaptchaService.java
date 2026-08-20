package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.CaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码（算术题 SVG，无第三方依赖）：
 * - 注册/登录前置校验，缓解撞库与批量注册
 * - 内存存储、单次有效、5 分钟过期、定时清理
 * - app.captcha.enabled=false 时关闭校验（开发/内网环境）
 * 注意：多实例部署时需共享存储（Redis），否则各节点验证码互不识别；
 * 已接入限流存储抽象（RateLimitStore），验证码可随同升级为 Redis。
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private record Entry(String answer, long expireAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Value("${app.captcha.enabled:true}")
    private boolean enabled;

    /**
     * 生成验证码，返回 captchaId + SVG 字符串（前端直接渲染）
     */
    public CaptchaResponse generate() {
        int a = RANDOM.nextInt(9) + 1;
        int b = RANDOM.nextInt(9) + 1;
        boolean plus = RANDOM.nextBoolean();
        int answer = plus ? a + b : a - b;
        String op = plus ? "+" : "-";
        String expression = a + " " + op + " " + b + " = ?";

        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new Entry(String.valueOf(answer), System.currentTimeMillis() + TTL_MS));

        return new CaptchaResponse(id, renderSvg(expression), enabled);
    }

    /**
     * 校验并作废（单次有效）。验证码关闭时直接通过。
     */
    public boolean verify(String captchaId, String answer) {
        if (!enabled) return true;
        if (captchaId == null || captchaId.isBlank() || answer == null || answer.isBlank()) return false;
        Entry entry = store.remove(captchaId);
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expireAt()) return false;
        return entry.answer().equals(answer.trim());
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            if (now > it.next().getValue().expireAt()) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) log.debug("验证码过期清理: {} 条", removed);
    }

    private String renderSvg(String expression) {
        int w = 140, h = 46;
        StringBuilder noise = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int x1 = RANDOM.nextInt(w), y1 = RANDOM.nextInt(h);
            int x2 = RANDOM.nextInt(w), y2 = RANDOM.nextInt(h);
            noise.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
                 .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
                 .append("\" stroke=\"#c8c9cc\" stroke-width=\"1\"/>");
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + w + "\" height=\"" + h + "\" viewBox=\"0 0 "
                + w + " " + h + "\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#f2f3f5\" rx=\"6\"/>"
                + noise
                + "<text x=\"50%\" y=\"50%\" dominant-baseline=\"central\" text-anchor=\"middle\" "
                + "font-family=\"Consolas,monospace\" font-size=\"20\" font-weight=\"bold\" "
                + "fill=\"#303133\" letter-spacing=\"1\">" + expression + "</text>"
                + "</svg>";
    }

}
