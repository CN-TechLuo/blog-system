package com.blog.blogsystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class UserInfoResponse {

    private Integer id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;
    private LocalDateTime createTime;

}
