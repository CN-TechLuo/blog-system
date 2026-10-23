package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 密码重置令牌（对应 password_reset_token 表）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class PasswordResetToken {

    /** 主键 */
    private Integer id;
    /** 用户ID（外键 user.id） */
    private Integer userId;
    /** 重置令牌 SHA-256 哈希（不存储明文令牌） */
    private String tokenHash;
    /** 过期时间（默认 30 分钟） */
    private LocalDateTime expiresAt;
    /** 是否已使用（防重放） */
    private Boolean used;
    /** 创建时间 */
    private LocalDateTime createTime;

}
