package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 密码重置令牌
 */
@Data
public class PasswordResetToken {

    private Integer id;
    private Integer userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime createTime;

}
