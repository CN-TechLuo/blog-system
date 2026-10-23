package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ChangePasswordRequest;
import com.blog.blogsystem.dto.LoginRequest;
import com.blog.blogsystem.dto.RefreshTokenRequest;
import com.blog.blogsystem.dto.RefreshTokenResponse;
import com.blog.blogsystem.dto.RegisterRequest;
import com.blog.blogsystem.dto.UserInfoResponse;

/**
 * 用户服务：注册/登录/刷新令牌/资料/改密
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
public interface UserService {

    /**
     * 用户注册（验证码 + 限流 + 敏感词审核用户名/昵称）
     *
     * @param request  注册请求
     * @param clientIp 客户端 IP（限流维度）
     * @return 统一响应
     */
    ApiResponse<Object> register(RegisterRequest request, String clientIp);

    /**
     * 用户登录（验证码 + 限流 + 失败锁定，DB 持久化）
     *
     * @param request  登录请求
     * @param clientIp 客户端 IP（限流维度）
     * @return 统一响应（data 为 token 对）
     */
    ApiResponse<RefreshTokenResponse> login(LoginRequest request, String clientIp);

    /**
     * 刷新令牌（校验 tokenVersion，单次有效旋转）
     *
     * @param request 刷新请求（refreshToken）
     * @return 统一响应（data 为新 token 对）
     */
    ApiResponse<RefreshTokenResponse> refreshToken(RefreshTokenRequest request);

    /**
     * 查询用户信息
     *
     * @param userId 用户ID
     * @return 统一响应（data 为用户信息）
     */
    ApiResponse<UserInfoResponse> getUserInfo(Integer userId);

    /**
     * 修改密码（校验原密码，成功后吊销全部会话）
     *
     * @param userId  用户ID
     * @param request 改密请求
     * @return 统一响应
     */
    ApiResponse<Void> changePassword(Integer userId, ChangePasswordRequest request);

}
