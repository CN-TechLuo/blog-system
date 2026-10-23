package com.blog.blogsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据对象（对应 user 表）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class User {

    /** 主键 */
    private Integer id;
    /** 用户名（登录凭证，唯一） */
    private String username;
    /** 昵称（展示用） */
    private String nickname;
    /** 密码哈希（BCrypt 存储，序列化时忽略） */
    @JsonIgnore
    private String password;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 角色（user/admin） */
    private String role;
    /** 连续登录失败次数 */
    private Integer failedAttempts;
    /** 登录锁定截止时间 */
    private LocalDateTime lockedUntil;
    /** 头像URL */
    private String avatarUrl;
    /** 令牌版本号（改密/登出时递增，使旧 refresh token 全部失效） */
    private Integer tokenVersion;
    /** 创建时间 */
    private LocalDateTime createTime;

}
