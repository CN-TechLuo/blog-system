package com.blog.blogsystme.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论数据对象
 */
@Data
public class Comment {

    private Integer id;
    private Integer articleId;
    private Integer userId;
    private String content;
    private LocalDateTime createTime;

    /** 评论者用户名（非数据库字段） */
    private transient String username;

}
