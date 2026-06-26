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

    // JWT 密钥：优先级 系统属性 -Djwt.secret > 环境变量 JWT_SECRET > Spring配置 jwt.secret
    // 生产环境必须通过环境变量注入
    // 生成密钥: openssl rand -base64 32
    private static volatile String SECRET;
    private static volatile SecretKey KEY;

    static {
        initKey();
    }

    /**
     * 初始化密钥，如果环境变量和系统属性都没有配置，使用开发默认值（仅开发环境）
     */
    private static synchronized void initKey() {
        if (KEY != null) return;
        String secret = resolveSecret();
        if (secret == null || secret.isBlank()) {
            // 开发环境默认密钥（Base64编码），生产环境务必通过环境变量覆盖
            secret = "REDACTED_DEV_SECRET_SET_ENV_VARIABLE";
            log.warn("============================================");
            log.warn("JWT 使用开发默认密钥！生产环境请设置 JWT_SECRET 环境变量");
            log.warn("生成密钥命令: openssl rand -base64 32");
            log.warn("============================================");
        } else {
            log.info("JWT 密钥已从环境变量/系统属性加载成功");
        }
        SECRET = secret;
        KEY = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    }

    private static String resolveSecret() {
        String secret = System.getProperty("jwt.secret");
        if (secret != null && !secret.isBlank()) {
            return secret;
        }
        secret = System.getenv("JWT_SECRET");
        if (secret != null && !secret.isBlank()) {
            return secret;
        }
        return null;
    }

    // token 有效期：7天（毫秒）
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    public static String generateToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION);
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public static boolean validateToken(String token) {
        try {
            getUsernameFromToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}