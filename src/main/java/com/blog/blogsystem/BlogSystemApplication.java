package com.blog.blogsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类：启用缓存与定时任务，Spring Boot 入口
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class BlogSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogSystemApplication.class, args);
    }

}
