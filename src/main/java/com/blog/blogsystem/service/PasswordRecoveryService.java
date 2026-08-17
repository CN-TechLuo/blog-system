package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ForgotPasswordRequest;
import com.blog.blogsystem.dto.ResetPasswordRequest;

import java.util.List;
import java.util.Map;

public interface PasswordRecoveryService {

    ApiResponse<List<String>> findUsernamesByEmail(String email);

    ApiResponse<Map<String, Object>> requestReset(ForgotPasswordRequest request);

    ApiResponse<Void> resetPassword(ResetPasswordRequest request);

}
