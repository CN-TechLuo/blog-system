package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ForgotPasswordRequest;
import com.blog.blogsystem.dto.ResetPasswordRequest;

import java.util.List;
import java.util.Map;

/**
 * 密码找回服务：邮箱找回账号、申请/重置密码
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface PasswordRecoveryService {

    /**
     * 按邮箱查询绑定的用户名（脱敏返回，防账号枚举）
     *
     * @param email 邮箱
     * @return 统一响应（data 为脱敏用户名列表）
     */
    ApiResponse<List<String>> findUsernamesByEmail(String email);

    /**
     * 申请重置密码（发送重置邮件，防账号枚举统一响应）
     *
     * @param request 找回密码请求（账号 + 绑定邮箱/手机号）
     * @return 统一响应
     */
    ApiResponse<Map<String, Object>> requestReset(ForgotPasswordRequest request);

    /**
     * 使用令牌重置密码（令牌单次有效，成功后吊销全部会话）
     *
     * @param request 重置密码请求（令牌 + 新密码）
     * @return 统一响应
     */
    ApiResponse<Void> resetPassword(ResetPasswordRequest request);

}
