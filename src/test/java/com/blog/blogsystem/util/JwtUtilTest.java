package com.blog.blogsystem.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    @BeforeAll
    static void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                "unit-test-secret-key-32-bytes-long!!".getBytes());
        System.setProperty("jwt.secret", secret);
    }

    @Test
    void generateAndParseShouldRoundTrip() {
        String token = JwtUtil.generateAccessToken(42, "alice", 5);
        assertEquals(42, JwtUtil.getUserIdFromToken(token));
        assertEquals("alice", JwtUtil.getUsernameFromToken(token));
        assertEquals(5, JwtUtil.getTokenVersionFromToken(token));
        assertEquals(JwtUtil.TYPE_ACCESS, JwtUtil.getTokenType(token));
        assertTrue(JwtUtil.isAccessToken(token));
        assertFalse(JwtUtil.isRefreshToken(token));
    }

    @Test
    void refreshTokenShouldCarryTokenVersionAndType() {
        String token = JwtUtil.generateRefreshToken(7, "bob", 3);
        assertEquals(3, JwtUtil.getTokenVersionFromToken(token));
        assertEquals(7, JwtUtil.getUserIdFromToken(token));
        assertEquals(JwtUtil.TYPE_REFRESH, JwtUtil.getTokenType(token));
        assertTrue(JwtUtil.isRefreshToken(token));
        assertFalse(JwtUtil.isAccessToken(token));
    }

    @Test
    void invalidTokenShouldNotReportTokenVersion() {
        assertNull(JwtUtil.getTokenVersionFromToken("invalid.token.value"));
        assertNull(JwtUtil.getTokenType("invalid.token.value"));
    }

    @Test
    void invalidTokenShouldReturnNull() {
        assertNull(JwtUtil.getUserIdFromToken("invalid.token.value"));
        assertNull(JwtUtil.getUserIdFromToken(""));
    }

    @Test
    void accessTokenShouldExpireInThirtyMinutes() {
        assertTrue(JwtUtil.ACCESS_EXPIRATION_MS == 30 * 60 * 1000L);
    }

}
