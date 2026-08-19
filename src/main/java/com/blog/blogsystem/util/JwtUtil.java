package com.blog.blogsystem.util;

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

    /** JWT 签发者标识：解析时强校验，防止跨应用令牌混用 */
    public static final String ISSUER = "blog-system";

    /** JWT 受众标识：解析时强校验 */
    public static final String AUDIENCE = "blog-web";

    /** Access Token 有效期：30 分钟（企业级建议 15-30 分钟，缩短泄露窗口） */
    public static final long ACCESS_EXPIRATION_MS = 30 * 60 * 1000L;

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
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            log.error("JWT 密钥不是合法的 Base64 编码");
            throw new IllegalStateException("JWT_SECRET 必须是 Base64 编码的随机密钥");
        }
        if (keyBytes.length < 32) {
            log.error("JWT 密钥强度不足（需 ≥32 字节，实际 {} 字节）", keyBytes.length);
            throw new IllegalStateException(
                    "JWT_SECRET 强度不足：HS256 需要至少 32 字节（256 位）。生成命令: openssl rand -base64 32");
        }
        log.info("JWT 密钥已从环境变量/系统属性加载成功");
        SECRET = secret;
        KEY = Keys.hmacShaKeyFor(keyBytes);
    }

    private static String resolveSecret() {
        String secret = System.getProperty("jwt.secret");
        if (secret != null && !secret.isBlank()) return secret;
        secret = System.getenv("JWT_SECRET");
        if (secret != null && !secret.isBlank()) return secret;
        return null;
    }

    public static String generateRefreshToken(Integer userId, String username, int tokenVersion) {
        return generateToken(userId, username, REFRESH_EXPIRATION_MS, tokenVersion);
    }

    public static String generateAccessToken(Integer userId, String username) {
        return generateToken(userId, username, ACCESS_EXPIRATION_MS, 0);
    }

    private static String generateToken(Integer userId, String username, long expirationMs) {
        return generateToken(userId, username, expirationMs, 0);
    }

    private static String generateToken(Integer userId, String username, long expirationMs, int tokenVersion) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .id(java.util.UUID.randomUUID().toString())
                .subject(username)
                .claim("userId", userId)
                .claim("tokenVersion", tokenVersion)
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

    public static String getJtiFromToken(String token) {
        try {
            return parseToken(token).getId();
        } catch (Exception e) {
            return null;
        }
    }

    public static java.util.Date getExpirationFromToken(String token) {
        try {
            return parseToken(token).getExpiration();
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

    public static Integer getTokenVersionFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            Object version = claims.get("tokenVersion");
            return version instanceof Integer ? (Integer) version : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
