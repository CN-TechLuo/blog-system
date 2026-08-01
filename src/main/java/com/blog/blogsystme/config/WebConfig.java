package com.blog.blogsystme.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * CORS 跨域配置 + JWT 认证拦截器注册
 * 生产环境应将 allowedOrigins 限制为具体的前端域名
 * 通过配置文件 cors.allowed-origins 控制（逗号分隔）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                    Arrays.stream(allowedOrigins.split(","))
                          .map(String::trim)
                          .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有 /api/article/** 写操作（POST/PUT/DELETE），GET 请求在拦截器内部放行
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/article/**");
    }

}
