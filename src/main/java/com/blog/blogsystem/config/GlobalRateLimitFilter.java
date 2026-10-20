package com.blog.blogsystem.config;

import com.blog.blogsystem.util.ClientIpUtil;
import com.blog.blogsystem.util.RateLimiterUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * L7 全局 API 限流：所有 /api/** 请求按 IP 限流（默认 600 次/分钟），
 * 防止爬虫/DoS 拖垮服务。业务层仍保留更细粒度的限流（登录/注册/找回/AI）。
 * 信任模型：反向代理必须覆盖 X-Forwarded-For（勿透传客户端伪造值）。
 */
@Component
public class GlobalRateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(GlobalRateLimitFilter.class);
    private static final int MAX_PER_IP_MINUTE = 600;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        if (!path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String ip = resolveClientIp(httpRequest);
        if (RateLimiterUtil.isBlocked("api:ip:" + ip, MAX_PER_IP_MINUTE)) {
            log.warn("全局 API 限流触发: IP={}, path={}", ip, path);
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.setHeader("Retry-After", "60");
            httpResponse.getWriter().write(
                    "{\"success\":false,\"message\":\"请求过于频繁，请稍后重试\",\"data\":null}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        // 只信任可信代理追加的跳数；直连请求的 XFF 一律忽略（防伪造）
        return ClientIpUtil.resolve(request);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
