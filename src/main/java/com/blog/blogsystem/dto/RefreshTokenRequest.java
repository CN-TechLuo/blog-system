package com.blog.blogsystem.dto;

import lombok.Data;

/**
 * Refresh Token 请求
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class RefreshTokenRequest {

    private String refreshToken;

}
