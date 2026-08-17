package com.blog.blogsystem.dto;

import lombok.Data;

/**
 * Refresh Token 请求
 */
@Data
public class RefreshTokenRequest {

    private String refreshToken;

}
