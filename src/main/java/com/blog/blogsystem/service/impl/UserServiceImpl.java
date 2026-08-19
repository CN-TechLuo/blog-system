package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ChangePasswordRequest;
import com.blog.blogsystem.dto.LoginRequest;
import com.blog.blogsystem.dto.RefreshTokenRequest;
import com.blog.blogsystem.dto.RefreshTokenResponse;
import com.blog.blogsystem.dto.RegisterRequest;
import com.blog.blogsystem.dto.UserInfoResponse;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.service.UserService;
import com.blog.blogsystem.util.AuditLogger;
import com.blog.blogsystem.util.JwtUtil;
import com.blog.blogsystem.util.MaskUtil;
import com.blog.blogsystem.util.PasswordUtil;
import com.blog.blogsystem.util.RateLimiterUtil;
import com.blog.blogsystem.util.SensitiveWordFilter;
import com.blog.blogsystem.util.XssUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;

    /** 登录限流：单账号 5 次/分钟，单 IP 30 次/分钟（缓解反代共享 IP 误伤） */
    private static final int MAX_LOGIN_PER_MINUTE = 5;
    private static final int MAX_LOGIN_PER_IP_MINUTE = 30;
    /** 注册限流：单账号名 3 次/分钟，单 IP 20 次/分钟 */
    private static final int MAX_REGISTER_PER_MINUTE = 3;
    private static final int MAX_REGISTER_PER_IP_MINUTE = 20;

    /** 登录失败锁定：连续失败 5 次锁定 15 分钟（DB 持久化，重启不失效） */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public ApiResponse<Object> register(RegisterRequest request, String clientIp) {
        if (RateLimiterUtil.isBlocked("reg:ip:" + clientIp, MAX_REGISTER_PER_IP_MINUTE)
                || RateLimiterUtil.isBlocked("reg:acc:" + clientIp + ":" + request.getUsername(), MAX_REGISTER_PER_MINUTE)) {
            log.warn("注册速率限制触发: IP={}, username={}", clientIp, request.getUsername());
            return ApiResponse.fail("注册请求过于频繁，请稍后重试");
        }

        log.debug("收到注册请求: username={}", request.getUsername());

        if (SensitiveWordFilter.containsSensitive(request.getUsername())
                || SensitiveWordFilter.containsSensitive(request.getNickname())) {
            return ApiResponse.fail("用户名或昵称包含违规词汇");
        }

        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            log.info("注册失败: 用户名 {} 已存在", request.getUsername());
            return ApiResponse.fail("用户名已存在，注册失败");
        }

        String encodedPassword = PasswordUtil.encode(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? XssUtil.escape(request.getNickname().trim()) : null);
        user.setPassword(encodedPassword);
        user.setEmail(request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim() : null);
        user.setPhone(request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone().trim() : null);
        int rows = userMapper.insert(user);

        log.info("注册结果: username={}, rows={}", request.getUsername(), rows);

        if (rows > 0) {
            return ApiResponse.success("注册成功");
        }
        return ApiResponse.fail("注册失败，请稍后重试");
    }

    @Override
    public ApiResponse<RefreshTokenResponse> login(LoginRequest request, String clientIp) {
        if (RateLimiterUtil.isBlocked("login:ip:" + clientIp, MAX_LOGIN_PER_IP_MINUTE)
                || RateLimiterUtil.isBlocked("login:acc:" + clientIp + ":" + request.getUsername(), MAX_LOGIN_PER_MINUTE)) {
            log.warn("登录速率限制触发: IP={}, username={}", clientIp, request.getUsername());
            return ApiResponse.fail("登录请求过于频繁，请稍后重试");
        }

        log.debug("收到登录请求: username={}", request.getUsername());

        User user;
        if (request.getUsername() != null && request.getUsername().matches("\\d{11}")) {
            user = userMapper.findByPhone(request.getUsername());
        } else {
            user = userMapper.findByUsername(request.getUsername());
        }

        // 账号锁定检查（DB 持久化）
        if (user != null && user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            AuditLogger.log("LOGIN_LOCKED", "ip=" + clientIp + ", username=" + request.getUsername());
            return ApiResponse.fail("连续失败次数过多，账号已锁定至 "
                    + user.getLockedUntil().toString().replace('T', ' ') + "，请稍后再试");
        }

        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            if (user != null) {
                userMapper.incrementFailedAttempts(user.getId());
                int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
                if (attempts >= MAX_FAILED_ATTEMPTS) {
                    userMapper.lockUser(user.getId(), LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                    AuditLogger.log("ACCOUNT_LOCKED", "ip=" + clientIp + ", username=" + request.getUsername());
                    return ApiResponse.fail("连续失败次数过多，账号已锁定 " + LOCK_MINUTES + " 分钟");
                }
                AuditLogger.log("LOGIN_FAIL", "ip=" + clientIp + ", username=" + request.getUsername()
                        + ", failCount=" + attempts);
            } else {
                AuditLogger.log("LOGIN_FAIL", "ip=" + clientIp + ", username=" + request.getUsername()
                        + ", reason=user_not_found");
            }
            log.info("登录失败: username={}", request.getUsername());
            return ApiResponse.fail("用户名或密码错误");
        }

        if (user.getFailedAttempts() != null && user.getFailedAttempts() > 0) {
            userMapper.clearFailedAttempts(user.getId());
        }

        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getTokenVersion() != null ? user.getTokenVersion() : 0);

        AuditLogger.log("LOGIN_SUCCESS", "userId=" + user.getId() + ", username=" + user.getUsername()
                + ", ip=" + clientIp);
        log.info("登录成功: username={}", user.getUsername());

        RefreshTokenResponse data = new RefreshTokenResponse();
        data.setToken(accessToken);
        data.setRefreshToken(refreshToken);
        data.setUsername(user.getUsername());
        data.setUserId(user.getId());
        data.setExpiresIn(JwtUtil.ACCESS_EXPIRATION_MS / 1000);
        return ApiResponse.success("登录成功", data);
    }

    @Override
    public ApiResponse<RefreshTokenResponse> refreshToken(RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ApiResponse.fail("refreshToken 不能为空");
        }

        Integer userId = JwtUtil.getUserIdFromToken(request.getRefreshToken());
        if (userId == null) {
            return ApiResponse.fail("refreshToken 无效或已过期");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        int tokenVersionInToken = JwtUtil.getTokenVersionFromToken(request.getRefreshToken());
        int currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (tokenVersionInToken != currentVersion) {
            log.warn("Refresh token 版本不匹配，已被失效: userId={}, tokenVersion={}, currentVersion={}",
                    userId, tokenVersionInToken, currentVersion);
            AuditLogger.log("REFRESH_REJECT", "userId=" + userId + ", reason=token_version_mismatch");
            return ApiResponse.fail("refreshToken 已失效，请重新登录");
        }

        // 旋转刷新：每次刷新递增 tokenVersion，旧 refresh token 立即失效（防重放）
        int newVersion = currentVersion + 1;
        userMapper.incrementTokenVersion(userId);
        String newAccessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), newVersion);

        RefreshTokenResponse data = new RefreshTokenResponse();
        data.setToken(newAccessToken);
        data.setRefreshToken(newRefreshToken);
        data.setUsername(user.getUsername());
        data.setUserId(user.getId());
        data.setExpiresIn(JwtUtil.ACCESS_EXPIRATION_MS / 1000);
        return ApiResponse.success("token 刷新成功", data);
    }

    @Override
    public ApiResponse<UserInfoResponse> getUserInfo(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }
        UserInfoResponse info = new UserInfoResponse();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setNickname(user.getNickname());
        info.setEmail(MaskUtil.maskEmail(user.getEmail()));
        info.setPhone(MaskUtil.maskPhone(user.getPhone()));
        info.setRole(user.getRole());
        info.setAvatarUrl(user.getAvatarUrl());
        info.setCreateTime(user.getCreateTime());
        return ApiResponse.success("查询成功", info);
    }

    @Override
    @Transactional
    public ApiResponse<Void> changePassword(Integer userId, ChangePasswordRequest request) {
        User user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        if (!PasswordUtil.matches(request.getOldPassword(), user.getPassword())) {
            return ApiResponse.fail("原密码错误");
        }

        String newEncoded = PasswordUtil.encode(request.getNewPassword());
        userMapper.updatePassword(userId, newEncoded);
        userMapper.incrementTokenVersion(userId);
        AuditLogger.log("PASSWORD_CHANGED", "userId=" + userId);
        log.info("密码修改成功，已失效所有 refresh token: userId={}", userId);
        return ApiResponse.success("密码修改成功");
    }

}
