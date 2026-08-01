package com.blog.blogsystme.config;

import com.blog.blogsystme.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器，统一提取当前用户 ID 并存入 request attribute。
 * 拦截所有 /api/article/** 非 GET 请求（POST/PUT/DELETE），
 * GET 请求（列表、详情）在 preHandle 中直接放行。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getRequestURI().startsWith("/api/article/") && "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("请求缺少 Authorization 头或格式不正确: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"未登录或 token 格式错误\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(7);
        Integer userId = JwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            log.debug("Token 无效或已过期: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"token 无效或已过期\",\"data\":null}");
            return false;
        }

        // 将 userId 存入 request attribute，供 Controller 使用
        request.setAttribute("userId", userId);
        return true;
    }
}
