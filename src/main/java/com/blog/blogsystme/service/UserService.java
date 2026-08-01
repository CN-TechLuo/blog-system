package com.blog.blogsystme.service;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.ChangePasswordRequest;
import com.blog.blogsystme.dto.LoginRequest;
import com.blog.blogsystme.dto.RefreshTokenRequest;
import com.blog.blogsystme.dto.RefreshTokenResponse;
import com.blog.blogsystme.dto.RegisterRequest;
import com.blog.blogsystme.dto.UserInfoResponse;

public interface UserService {

    ApiResponse<Object> register(RegisterRequest request, String clientIp);

    ApiResponse<RefreshTokenResponse> login(LoginRequest request, String clientIp);

    ApiResponse<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);

    ApiResponse<UserInfoResponse> getUserInfo(Integer userId);

    ApiResponse<Void> changePassword(Integer userId, ChangePasswordRequest request);

}
