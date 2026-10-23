package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.CaptchaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaServiceTest {

    private static final String PNG_PREFIX = "data:image/png;base64,";

    private final RecordingCaptchaStore store = new RecordingCaptchaStore();
    private final CaptchaService service = new CaptchaService(store);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    @Test
    void generateReturnsPngDataUri() {
        CaptchaResponse r = service.generate();

        assertNotNull(r.getCaptchaId());
        assertTrue(r.getSvg().startsWith(PNG_PREFIX));
        byte[] png = Base64.getDecoder().decode(r.getSvg().substring(PNG_PREFIX.length()));
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
        assertEquals('N', png[2]);
        assertEquals('G', png[3]);
        assertEquals(4, store.lastAnswer().length());
    }

    @Test
    void generateThenVerifyCorrectAnswer() {
        CaptchaResponse r = service.generate();
        String code = store.lastAnswer();

        assertTrue(service.verify(r.getCaptchaId(), code));
        assertFalse(service.verify(r.getCaptchaId(), code), "验证码只能使用一次");
    }

    @Test
    void verifyIsCaseInsensitive() {
        CaptchaResponse r = service.generate();
        String code = store.lastAnswer();

        assertTrue(service.verify(r.getCaptchaId(), code.toLowerCase()));
    }

    @Test
    void wrongAnswerRejected() {
        CaptchaResponse r = service.generate();
        String wrong = "ZZZZ".equals(store.lastAnswer()) ? "YYYY" : "ZZZZ";

        assertFalse(service.verify(r.getCaptchaId(), wrong));
    }

    @Test
    void unknownCaptchaIdRejected() {
        assertFalse(service.verify("nonexistent-id", "1"));
        assertFalse(service.verify(null, "1"));
    }

    @Test
    void disabledCaptchaRejectsVerification() {
        ReflectionTestUtils.setField(service, "enabled", false);

        assertFalse(service.verify(null, null), "验证码关闭时必须 fail-closed");
    }

    private static final class RecordingCaptchaStore implements CaptchaStore {

        private final Map<String, String> answers = new ConcurrentHashMap<>();
        private volatile String lastAnswer;

        @Override
        public void put(String captchaId, String answer, long ttlMs) {
            answers.put(captchaId, answer);
            lastAnswer = answer;
        }

        @Override
        public String take(String captchaId) {
            return answers.remove(captchaId);
        }

        @Override
        public void cleanExpired() {
        }

        String lastAnswer() {
            return lastAnswer;
        }
    }
}
