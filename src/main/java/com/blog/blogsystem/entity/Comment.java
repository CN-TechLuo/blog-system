package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论数据对象
 */
@Data
public class Comment {

    private Integer id;
    private Integer articleId;
    private Integer userId;
    private String content;
    private Integer parentId;
    private Integer likeCount;
    private LocalDateTime createTime;

    private transient String username;
    private transient List<Comment> replies;

}
