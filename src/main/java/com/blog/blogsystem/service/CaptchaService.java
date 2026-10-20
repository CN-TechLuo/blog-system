package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.CaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码（算术题，服务端栅格化）：
 * - 注册/登录前置校验，缓解撞库与批量注册
 * - 单次有效、5 分钟过期、定时清理
 * - app.captcha.enabled=false 时关闭校验（开发/内网环境）
 * - 存储可插拔（CaptchaStore）：单机内存 / Redis 多实例共享（CAPTCHA_STORE=redis）
 *
 * 为什么必须栅格化：旧实现把 "1 + 2 = ?" 以 SVG &lt;text&gt; 明文输出，
 * 脚本正则取值即可作答，验证码等于不存在。现在表达式只画进位图，
 * 响应里只有 base64 PNG，OCR 才需要成本。
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int WIDTH = 140;
    private static final int HEIGHT = 46;

    private final CaptchaStore store;

    @Value("${app.captcha.enabled:true}")
    private boolean enabled;

    public CaptchaService(CaptchaStore captchaStore) {
        this.store = captchaStore;
    }

    /**
     * 生成验证码，返回 captchaId + SVG 字符串（内嵌 base64 PNG，前端直接渲染）
     */
    public CaptchaResponse generate() {
        int a = RANDOM.nextInt(9) + 1;
        int b = RANDOM.nextInt(9) + 1;
        boolean plus = RANDOM.nextBoolean();
        int answer = plus ? a + b : a - b;
        String expression = a + " " + (plus ? "+" : "-") + " " + b + " = ?";

        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, String.valueOf(answer), TTL_MS);

        return new CaptchaResponse(id, renderAsSvgWithRaster(expression), enabled);
    }

    /**
     * 校验并作废（单次有效）。验证码关闭时直接通过。
     */
    public boolean verify(String captchaId, String answer) {
        if (!enabled) return true;
        if (captchaId == null || captchaId.isBlank() || answer == null || answer.isBlank()) return false;
        String expected = store.take(captchaId);
        return expected != null && expected.equals(answer.trim());
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanExpired() {
        store.cleanExpired();
    }

    /**
     * 把表达式画进位图，再以 data URI 嵌进 SVG：
     * 继续保持前端 "拿 svg 字段直接渲染" 的契约，但响应里不再有可读的题目文本。
     */
    private String renderAsSvgWithRaster(String expression) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(242, 243, 245));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(200 + RANDOM.nextInt(30), 201 + RANDOM.nextInt(30), 204 + RANDOM.nextInt(30)));
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }
            // 干扰点
            for (int i = 0; i < 40; i++) {
                g.setColor(new Color(190 + RANDOM.nextInt(50), 190 + RANDOM.nextInt(50), 190 + RANDOM.nextInt(50)));
                g.fillRect(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), 2, 2);
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
            int x = 8;
            for (char c : expression.toCharArray()) {
                int dy = RANDOM.nextInt(9) - 4;
                double rotate = (RANDOM.nextDouble() - 0.5) * 0.45;
                g.setColor(new Color(30 + RANDOM.nextInt(70), 30 + RANDOM.nextInt(70), 30 + RANDOM.nextInt(70)));
                java.awt.geom.AffineTransform old = g.getTransform();
                g.rotate(rotate, x + 6, HEIGHT / 2.0 + dy);
                g.drawString(String.valueOf(c), x, HEIGHT / 2 + 8 + dy);
                g.setTransform(old);
                x += 14;
            }
            // 轻微整体覆盖一层噪点，进一步降低 OCR 置信度
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            g.setColor(Color.GRAY);
            for (int i = 0; i < 120; i++) {
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }
        } finally {
            g.dispose();
        }

        String base64;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            base64 = Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            log.error("验证码图片生成失败", e);
            throw new IllegalStateException("验证码生成失败", e);
        }

        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + WIDTH + "\" height=\"" + HEIGHT
                + "\" viewBox=\"0 0 " + WIDTH + " " + HEIGHT + "\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#f2f3f5\" rx=\"6\"/>"
                + "<image width=\"" + WIDTH + "\" height=\"" + HEIGHT
                + "\" href=\"data:image/png;base64," + base64 + "\"/>"
                + "</svg>";
    }
}
