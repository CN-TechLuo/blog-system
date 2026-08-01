package com.blog.blogsystme.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应
 */
@Data
public class UserInfoResponse {

    private Integer id;
    private String username;
    private String email;
    private LocalDateTime createTime;

}
