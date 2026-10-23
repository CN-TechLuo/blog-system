package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论数据对象（对应 comment 表，支持嵌套回复）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class Comment {

    /** 主键 */
    private Integer id;
    /** 文章ID（外键 article.id） */
    private Integer articleId;
    /** 评论用户ID（外键 user.id） */
    private Integer userId;
    /** 评论内容 */
    private String content;
    /** 父评论ID（null 表示顶层评论，回复随父评论级联删除） */
    private Integer parentId;
    /** 点赞数（冗余计数） */
    private Integer likeCount;
    /** 创建时间 */
    private LocalDateTime createTime;

    /** 评论者用户名（非表字段，联查填充） */
    private transient String username;
    /** 子评论列表（非表字段，递归组装） */
    private transient List<Comment> replies;

}
