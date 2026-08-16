package com.blog.blogsystme.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {
    private Integer id;
    private Integer userId;
    private String type;
    private Integer fromUserId;
    private Integer articleId;
    private Integer commentId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createTime;
    private String fromUsername;
}
