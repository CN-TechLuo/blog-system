package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveWordFilterTest {

    @Test
    void shouldDetectSensitiveWord() {
        assertTrue(SensitiveWordFilter.containsSensitive("这里有色情内容"));
        assertTrue(SensitiveWordFilter.containsSensitive("fuck off"));
        assertNotNull(SensitiveWordFilter.findHit("平台提供赌博服务"));
    }

    @Test
    void shouldAllowNormalContent() {
        assertFalse(SensitiveWordFilter.containsSensitive("今天天气不错，分享一篇技术文章"));
        assertFalse(SensitiveWordFilter.containsSensitive("操作系统的调度算法"));
        assertFalse(SensitiveWordFilter.containsSensitive(""));
        assertFalse(SensitiveWordFilter.containsSensitive(null));
        assertNull(SensitiveWordFilter.findHit("普通内容"));
    }

    @Test
    void shouldMaskSensitiveWord() {
        assertEquals("这是**内容", SensitiveWordFilter.mask("这是色情内容"));
    }
}
