package com.blog.blogsystem.config;

import com.blog.blogsystem.service.TokenBlacklistService;
import com.blog.blogsystem.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器，统一提取当前用户 ID 并存入 request attribute。
 * - 非 GET 的受保护路径：必须携带有效 token，否则拒绝
 * - /api/article/** 的 GET 请求：公开放行；若携带有效 token 则解析出
 *   userId 存入 request attribute（用于点赞/收藏状态回显），无效则忽略
 * - 登出后 jti 进入黑名单的 token 一律拒绝
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private final TokenBlacklistService tokenBlacklistService;

    public AuthInterceptor(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 关注流必须登录（其余文章 GET 接口公开）
        boolean protectedFeed = "/api/article/feed/following".equals(request.getRequestURI());

        if (request.getRequestURI().startsWith("/api/article/") && "GET".equalsIgnoreCase(request.getMethod())
                && !protectedFeed) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (!isBlacklisted(token)) {
                    Integer viewerId = JwtUtil.getUserIdFromToken(token);
                    if (viewerId != null) {
                        request.setAttribute("userId", viewerId);
                    }
                }
            }
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("请求缺少 Authorization 头或格式不正确: {} {}", request.getMethod(), request.getRequestURI());
            throw new AuthenticationException(HttpStatus.UNAUTHORIZED, "未登录或 token 格式错误");
        }

        String token = authHeader.substring(7);
        Integer userId = JwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            log.debug("Token 无效或已过期: {} {}", request.getMethod(), request.getRequestURI());
            throw new AuthenticationException(HttpStatus.UNAUTHORIZED, "token 无效或已过期");
        }
        if (isBlacklisted(token)) {
            log.debug("Token 已登出吊销: {} {}", request.getMethod(), request.getRequestURI());
            throw new AuthenticationException(HttpStatus.UNAUTHORIZED, "token 已失效，请重新登录");
        }

        request.setAttribute("userId", userId);
        return true;
    }

    private boolean isBlacklisted(String token) {
        String jti = JwtUtil.getJtiFromToken(token);
        return jti != null && tokenBlacklistService.isBlacklisted(jti);
    }
}
