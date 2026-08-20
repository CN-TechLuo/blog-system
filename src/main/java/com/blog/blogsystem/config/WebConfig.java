package com.blog.blogsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.util.Arrays;

/**
 * CORS 跨域配置 + JWT 认证拦截器注册 + 静态资源映射
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
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadDir = new File("uploads");
        if (!uploadDir.exists()) uploadDir.mkdirs();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir.getAbsolutePath() + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                    Arrays.stream(allowedOrigins.split(","))
                          .map(String::trim)
                          .toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(1800);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/article/**")
                .addPathPatterns("/api/comment/**")
                .addPathPatterns("/api/feedback/**")
                .addPathPatterns("/api/social/**")
                .addPathPatterns("/api/notification/**")
                .addPathPatterns("/api/ai/**")
                .addPathPatterns("/api/user/me")
                .addPathPatterns("/api/user/password")
                .addPathPatterns("/api/user/avatar")
                .addPathPatterns("/api/cover/**")
                .addPathPatterns("/api/admin/**")
                .addPathPatterns("/api/user/nickname")
                .addPathPatterns("/api/user/phone")
                .addPathPatterns("/api/user/logout")
                .addPathPatterns("/api/user/export-data")
                .addPathPatterns("/api/user/delete-account")
                .addPathPatterns("/api/report/**");
    }

}
