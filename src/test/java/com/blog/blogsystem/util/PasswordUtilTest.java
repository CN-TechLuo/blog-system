package com.blog.blogsystem.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void encodeAndMatchesShouldWork() {
        String encoded = PasswordUtil.encode("Test1234!");
        assertTrue(encoded.startsWith("$2"));
        assertTrue(PasswordUtil.matches("Test1234!", encoded));
        assertFalse(PasswordUtil.matches("Wrong1234!", encoded));
    }

    @Test
    void passwordRegexShouldRequireLetterDigitAndSpecialChar() {
        assertTrue("Test1234!".matches(PasswordUtil.PASSWORD_REGEX));
        assertTrue("Ab1!aaaa".matches(PasswordUtil.PASSWORD_REGEX));
        assertFalse("Aa1!aaa".matches(PasswordUtil.PASSWORD_REGEX), "少于8位应不通过");
        assertFalse("Test1234".matches(PasswordUtil.PASSWORD_REGEX), "缺少特殊字符应不通过");
        assertFalse("abcdefg!".matches(PasswordUtil.PASSWORD_REGEX), "缺少数字应不通过");
        assertFalse("12345678!".matches(PasswordUtil.PASSWORD_REGEX), "缺少字母应不通过");
        assertFalse("abcdefg12".matches(PasswordUtil.PASSWORD_REGEX), "缺少特殊字符应不通过");
    }

    @Test
    void bcryptShouldRejectPasswordsOver72Bytes() {
        assertTrue(PasswordUtil.isBcryptCompatible("Test1234!"));
        assertFalse(PasswordUtil.isBcryptCompatible("中".repeat(25)), "25 个中文字符为 75 字节");
        assertThrows(IllegalArgumentException.class,
                () -> PasswordUtil.encode("中".repeat(25)));
    }

}
