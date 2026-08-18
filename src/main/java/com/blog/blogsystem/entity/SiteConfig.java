package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteConfig {
    private Integer id;
    private String contactEmail;
    private LocalDateTime updateTime;
}
