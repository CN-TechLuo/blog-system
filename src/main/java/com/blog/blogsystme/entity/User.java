package com.blog.blogsystme.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据对象
 */
@Data
public class User {

    private Integer id;
    private String username;
    private String nickname;

    @JsonIgnore
    private String password;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;
    private Integer tokenVersion;
    private LocalDateTime createTime;

}
