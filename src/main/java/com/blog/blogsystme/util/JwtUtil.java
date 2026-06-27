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
     * 初始化密钥，必须通过环境变量或系统属性配置，未配置则拒绝启动
     */
    private static synchronized void initKey() {
        if (KEY != null) return;
        String secret = resolveSecret();
        if (secret == null || secret.isBlank()) {
            log.error("============================================");
            log.error("JWT 密钥未配置！应用无法启动");
            log.error("请设置环境变量 JWT_SECRET 或系统属性 -Djwt.secret");
            log.error("生成密钥命令: openssl rand -base64 32");
            log.error("============================================");
            throw new IllegalStateException(
                    "JWT_SECRET 环境变量或 -Djwt.secret 系统属性必须配置。生成命令: openssl rand -base64 32");
        }
        log.info("JWT 密钥已从环境变量/系统属性加载成功");
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

    // token 有效期：2小时（毫秒）
    private static final long EXPIRATION = 2 * 60 * 60 * 1000L;

    /**
     * 生成 JWT token，包含 userId 和 username 两个 claims
     */
    public static String generateToken(Integer userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION);
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 安全获取用户名，token 无效时返回 null
     */
    public static String getUsernameFromToken(String token) {
        try {
            return parseToken(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 token 中提取 userId，避免额外的数据库查询。
     * token 无效或 claim 缺失时返回 null。
     */
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

    public static boolean validateToken(String token) {
        return getUsernameFromToken(token) != null;
    }
}