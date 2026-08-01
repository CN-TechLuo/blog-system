package com.blog.blogsystme.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章数据对象
 */
@Data
public class Article {

    private Integer id;
    private String title;
    private String content;
    private Integer userId;
    private Integer viewCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
