package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiUsage {
    private Long id;
    private Integer userId;
    private String username;
    private String apiType;
    private Integer inputChars;
    private Integer outputChars;
    private LocalDateTime createTime;
}
