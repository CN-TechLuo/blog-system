package com.blog.blogsystme.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI blogSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("博客系统 API")
                        .description("基于 Spring Boot 4 + MyBatis + JWT 的博客系统 REST API")
                        .version("1.0.0")
                        .license(new License().name("MIT").url("https://github.com/CN-TechLuo/blog-system")));
    }

}
