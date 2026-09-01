package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.CaptchaResponse;
import com.blog.blogsystem.service.impl.InMemoryCaptchaStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaServiceTest {

    private CaptchaService service = new CaptchaService(new InMemoryCaptchaStore());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    @Test
    void generateThenVerifyCorrectAnswer() {
        CaptchaResponse r = service.generate();
        assertNotNull(r.getCaptchaId());
        assertTrue(r.getSvg().contains("svg"));

        int expected = parseAnswer(r.getSvg());
        assertTrue(service.verify(r.getCaptchaId(), String.valueOf(expected)), "正确答案应通过");
    }

    @Test
    void captchaIsSingleUse() {
        CaptchaResponse r = service.generate();
        int expected = parseAnswer(r.getSvg());
        assertTrue(service.verify(r.getCaptchaId(), String.valueOf(expected)));
        assertFalse(service.verify(r.getCaptchaId(), String.valueOf(expected)), "验证码只能使用一次");
    }

    @Test
    void wrongAnswerRejected() {
        CaptchaResponse r = service.generate();
        int expected = parseAnswer(r.getSvg());
        assertFalse(service.verify(r.getCaptchaId(), String.valueOf(expected + 1)), "错误答案应拒绝");
    }

    @Test
    void unknownCaptchaIdRejected() {
        assertFalse(service.verify("nonexistent-id", "1"));
        assertFalse(service.verify(null, "1"));
    }

    @Test
    void disabledCaptchaBypassesVerification() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertTrue(service.verify(null, null), "验证码关闭时直接通过");
    }

    private int parseAnswer(String svg) {
        Matcher m = Pattern.compile("(\\d+) ([+\\-]) (\\d+) = \\?").matcher(svg);
        assertTrue(m.find(), "SVG 应包含算术表达式");
        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(3));
        return "+".equals(m.group(2)) ? a + b : a - b;
    }

}
