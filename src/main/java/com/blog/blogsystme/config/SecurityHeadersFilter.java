package com.blog.blogsystme.config;

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

@Component
public class SecurityHeadersFilter implements Filter {

    /** 仅对 API 与静态上传资源响应附加 CSP，避免影响 Swagger UI 页面 */
    private static final String CSP_VALUE = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

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

        // HSTS 仅在 HTTPS 下下发，防止用户被 HTTP 明文响应误导
        if (httpRequest.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        String path = httpRequest.getRequestURI();
        if (path.startsWith("/api/") || path.startsWith("/uploads/")) {
            httpResponse.setHeader("Content-Security-Policy", CSP_VALUE);
        }
        if (path.startsWith("/api/")) {
            // 认证相关响应禁止任何缓存，防止 token 被中间缓存留存
            if (path.contains("/login") || path.contains("/refresh")) {
                httpResponse.setHeader("Cache-Control", "no-store");
            }
            // API 响应不允许被第三方页面以 no-cors 方式读取
            httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
