package com.blog.blogsystem.dto;

import lombok.Data;

/**
 * Token 响应（含 refreshToken）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class RefreshTokenResponse {

    private String token;
    private String refreshToken;
    private String username;
    private Integer userId;
    private Long expiresIn;

}
