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

    /** 作者名（非数据库字段，查询时填充） */
    private transient String authorName;

}
