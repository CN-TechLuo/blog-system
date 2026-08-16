package com.blog.blogsystme.service;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.ForgotPasswordRequest;
import com.blog.blogsystme.dto.ResetPasswordRequest;

import java.util.List;
import java.util.Map;

public interface PasswordRecoveryService {

    ApiResponse<List<String>> findUsernamesByEmail(String email);

    ApiResponse<Map<String, Object>> requestReset(ForgotPasswordRequest request);

    ApiResponse<Void> resetPassword(ResetPasswordRequest request);

}
