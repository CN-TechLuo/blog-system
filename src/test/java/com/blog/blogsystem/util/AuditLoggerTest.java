package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLoggerTest {

    @Test
    void sanitizeShouldMaskEmailAndPhone() {
        String detail = "userId=1, email=alice@example.com, phone=13812345678, ip=203.0.113.5";

        assertEquals("userId=1, email=al***@example.com, phone=138****5678, ip=203.0.113.5",
                AuditLogger.sanitize(detail));
    }

    @Test
    void sanitizeShouldKeepNonPiiDetail() {
        assertEquals("userId=1, ip=127.0.0.1", AuditLogger.sanitize("userId=1, ip=127.0.0.1"));
    }
}
