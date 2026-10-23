package com.blog.blogsystem.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器：为所有响应添加 X-Content-Type-Options 等防护头
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Component
public class SecurityHeadersFilter implements Filter {

    /** API / 上传资源的严格 CSP */
    private static final String CSP_VALUE = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
    /** 非 API 响应（错误页等）的兜底 CSP，Swagger 路径除外 */
    private static final String CSP_FALLBACK = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "0");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=(), usb=()");
        httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        httpResponse.setHeader("Origin-Agent-Cluster", "?1");
        httpResponse.setHeader("X-Download-Options", "noopen");

        // HSTS 始终下发：浏览器只在 HTTPS 下生效；反向代理终止 TLS 时 isSecure() 不可靠
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        String path = httpRequest.getRequestURI();
        boolean swagger = path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs");
        if (!swagger) {
            if (path.startsWith("/api/") || path.startsWith("/uploads/")) {
                httpResponse.setHeader("Content-Security-Policy", CSP_VALUE);
            } else {
                httpResponse.setHeader("Content-Security-Policy", CSP_FALLBACK);
            }
        }
        if (path.startsWith("/api/")) {
            // 认证/重置/验证码/数据导出等敏感响应禁止任何缓存
            if (isSensitivePath(path)) {
                httpResponse.setHeader("Cache-Control", "no-store");
            }
            // API 响应不允许被第三方页面以 no-cors 方式读取
            httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        }

        chain.doFilter(request, response);
    }

    private boolean isSensitivePath(String path) {
        return path.contains("/login")
                || path.contains("/refresh")
                || path.contains("/password")
                || path.contains("/forgot-")
                || path.contains("/captcha")
                || path.contains("/logout")
                || path.contains("/export-data")
                || path.contains("/delete-account")
                || path.contains("/audit-logs")
                || path.startsWith("/api/bootstrap");
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
