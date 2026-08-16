package com.blog.blogsystme.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertTrue("a1!".matches(PasswordUtil.PASSWORD_REGEX));
        assertFalse("Test1234".matches(PasswordUtil.PASSWORD_REGEX), "缺少特殊字符应不通过");
        assertFalse("abcdefg!".matches(PasswordUtil.PASSWORD_REGEX), "缺少数字应不通过");
        assertFalse("12345678!".matches(PasswordUtil.PASSWORD_REGEX), "缺少字母应不通过");
        assertFalse("abcdefg12".matches(PasswordUtil.PASSWORD_REGEX), "缺少特殊字符应不通过");
    }

}
