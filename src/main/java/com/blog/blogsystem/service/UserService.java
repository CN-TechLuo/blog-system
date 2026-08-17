package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ChangePasswordRequest;
import com.blog.blogsystem.dto.LoginRequest;
import com.blog.blogsystem.dto.RefreshTokenRequest;
import com.blog.blogsystem.dto.RefreshTokenResponse;
import com.blog.blogsystem.dto.RegisterRequest;
import com.blog.blogsystem.dto.UserInfoResponse;

public interface UserService {

    ApiResponse<Object> register(RegisterRequest request, String clientIp);

    ApiResponse<RefreshTokenResponse> login(LoginRequest request, String clientIp);

    ApiResponse<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);

    ApiResponse<UserInfoResponse> getUserInfo(Integer userId);

    ApiResponse<Void> changePassword(Integer userId, ChangePasswordRequest request);

}
