package com.blog.blogsystme.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static volatile String SECRET;
    private static volatile SecretKey KEY;

    /** Access Token 有效期：2 小时 */
    public static final long ACCESS_EXPIRATION_MS = 2 * 60 * 60 * 1000L;

    /** Refresh Token 有效期：7 天 */
    public static final long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    static {
        initKey();
    }

    private static synchronized void initKey() {
        if (KEY != null) return;
        String secret = resolveSecret();
        if (secret == null || secret.isBlank()) {
            log.error("JWT 密钥未配置！应用无法启动");
            throw new IllegalStateException(
                    "JWT_SECRET 环境变量或 -Djwt.secret 系统属性必须配置。生成命令: openssl rand -base64 32");
        }
        log.info("JWT 密钥已从环境变量/系统属性加载成功");
        SECRET = secret;
        KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    }

    private static String resolveSecret() {
        String secret = System.getProperty("jwt.secret");
        if (secret != null && !secret.isBlank()) return secret;
        secret = System.getenv("JWT_SECRET");
        if (secret != null && !secret.isBlank()) return secret;
        return null;
    }

    public static String generateAccessToken(Integer userId, String username) {
        return generateToken(userId, username, ACCESS_EXPIRATION_MS);
    }

    public static String generateRefreshToken(Integer userId, String username) {
        return generateToken(userId, username, REFRESH_EXPIRATION_MS);
    }

    private static String generateToken(Integer userId, String username, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public static Integer getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object userId = claims.get("userId");
            return userId instanceof Integer ? (Integer) userId : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
