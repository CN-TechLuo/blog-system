package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.CaptchaResponse;
import com.blog.blogsystem.service.impl.InMemoryCaptchaStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaServiceTest {

    private final InMemoryCaptchaStore store = new InMemoryCaptchaStore();
    private final CaptchaService service = new CaptchaService(store);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    @Test
    void generateThenVerifyCorrectAnswer() {
        CaptchaResponse r = service.generate();
        assertNotNull(r.getCaptchaId());
        assertTrue(r.getSvg().contains("svg"));
        // 关键回归：题目不能再以明文出现在响应里（旧实现可被正则直接读出答案）
        assertTrue(r.getSvg().contains("data:image/png;base64"), "验证码应栅格化后内嵌");
        assertFalse(r.getSvg().matches("(?s).*\\d+ [+-] \\d+ = \\?.*"), "答案不能明文出现在响应中");

        String expected = store.peek(r.getCaptchaId());
        assertNotNull(expected);
        assertTrue(service.verify(r.getCaptchaId(), expected), "正确答案应通过");
    }

    @Test
    void captchaIsSingleUse() {
        CaptchaResponse r = service.generate();
        String expected = store.peek(r.getCaptchaId());
        assertTrue(service.verify(r.getCaptchaId(), expected));
        assertFalse(service.verify(r.getCaptchaId(), expected), "验证码只能使用一次");
    }

    @Test
    void wrongAnswerRejected() {
        CaptchaResponse r = service.generate();
        int expected = Integer.parseInt(store.peek(r.getCaptchaId()));
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

}
