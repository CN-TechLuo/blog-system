package com.blog.blogsystme.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Article {

    private Integer id;

    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 200, message = "文章标题长度需在1-200位之间")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private Integer userId;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
