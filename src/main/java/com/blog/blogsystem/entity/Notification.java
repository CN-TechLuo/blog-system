package com.blog.blogsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 站内通知（对应 notification 表：点赞/评论/关注等互动触发）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class Notification {

    /** 主键 */
    private Integer id;
    /** 接收通知的用户ID（外键 user.id） */
    private Integer userId;
    /** 通知类型（like/comment/follow 等） */
    private String type;
    /** 操作发起者用户ID（点赞/评论/关注者） */
    private Integer fromUserId;
    /** 关联文章ID（外键 article.id，评论/点赞通知使用） */
    private Integer articleId;
    /** 关联评论ID（外键 comment.id，评论通知使用） */
    private Integer commentId;
    /** 通知内容 */
    private String content;
    /** 是否已读 */
    private Boolean isRead;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 操作发起者用户名（冗余字段，列表展示用） */
    private String fromUsername;

}
