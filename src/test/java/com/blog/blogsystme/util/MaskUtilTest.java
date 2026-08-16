package com.blog.blogsystme.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MaskUtilTest {

    @Test
    void maskPhoneShouldMaskMiddleDigits() {
        assertEquals("138****5678", MaskUtil.maskPhone("13812345678"));
        assertEquals("123456", MaskUtil.maskPhone("123456"), "不足 7 位原样返回");
        assertNull(MaskUtil.maskPhone(null));
    }

    @Test
    void maskEmailShouldMaskNamePart() {
        assertEquals("te***@example.com", MaskUtil.maskEmail("test@example.com"));
        assertEquals("a***@example.com", MaskUtil.maskEmail("a@example.com"));
        assertEquals("***@example.com", MaskUtil.maskEmail("@example.com"));
        assertEquals("noemail", MaskUtil.maskEmail("noemail"));
    }

    @Test
    void maskUsernameShouldMaskMiddle() {
        assertEquals("zh***an", MaskUtil.maskUsername("zhangsan"));
        assertEquals("a***d", MaskUtil.maskUsername("abcd"));
        assertEquals("a*", MaskUtil.maskUsername("ab"));
        assertEquals("a*", MaskUtil.maskUsername("a"));
        assertNull(MaskUtil.maskUsername(null));
    }

}
