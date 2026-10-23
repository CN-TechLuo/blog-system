package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.CaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码（4 位字符 PNG，Java2D 绘制）： - 注册/登录前置校验，缓解撞库与批量注册 - 单次有效、5 分钟过期、定时清理 - 关闭时 fail-closed（拒绝登录/注册），避免配置失手导致防护失效 - 存储可插拔（CaptchaStore）：单机内存 / Redis 多实例共享（CAPTCHA_STORE=redis）
 *
 * @author CN-TechLuo
 * @date 2026/08/21
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int WIDTH = 140;
    private static final int HEIGHT = 46;

    /** 去除 0/O/1/I 等易混字符，降低正常用户识别成本 */
    private static final char[] CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final CaptchaStore store;

    @Value("${app.captcha.enabled:true}")
    private boolean enabled;

    public CaptchaService(CaptchaStore captchaStore) {
        this.store = captchaStore;
    }

    /**
     * 生成验证码，返回 captchaId + PNG data URI（前端直接用 img src 渲染）
     */
    public CaptchaResponse generate() {
        String code = randomCode(4);
        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, code, TTL_MS);
        return new CaptchaResponse(id, renderPng(code), enabled);
    }

    /**
     * 校验并作废（单次有效，忽略大小写）。验证码关闭时按 fail-closed 拒绝。
     */
    public boolean verify(String captchaId, String answer) {
        if (!enabled) {
            log.error("验证码功能已关闭，按 fail-closed 策略拒绝请求");
            return false;
        }
        if (captchaId == null || captchaId.isBlank() || answer == null || answer.isBlank()) {
            return false;
        }
        String expected = store.take(captchaId);
        return expected != null && expected.equalsIgnoreCase(answer.trim());
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanExpired() {
        store.cleanExpired();
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_CHARS[RANDOM.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private String renderPng(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(242, 243, 245));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(170, 220));
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }

            int step = (WIDTH - 24) / code.length();
            for (int i = 0; i < code.length(); i++) {
                AffineTransform original = g.getTransform();
                double angle = Math.toRadians(RANDOM.nextInt(41) - 20);
                int x = 12 + i * step + RANDOM.nextInt(6);
                int y = 33 + RANDOM.nextInt(7) - 3;
                g.rotate(angle, x, y);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26 + RANDOM.nextInt(5)));
                g.setColor(randomColor(20, 120));
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(original);
            }

            for (int i = 0; i < 60; i++) {
                image.setRGB(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), randomColor(140, 220).getRGB());
            }
        } finally {
            g.dispose();
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", out)) {
                throw new IllegalStateException("PNG 编码器不可用");
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码图片生成失败", e);
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range),
                min + RANDOM.nextInt(range));
    }
}
