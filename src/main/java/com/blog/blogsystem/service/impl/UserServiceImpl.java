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
import com.blog.blogsystem.service.CaptchaService;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 用户服务实现：注册/登录（锁定/限流/验证码）、刷新令牌（tokenVersion 校验）、改密
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final CaptchaService captchaService;

    /** 登录限流：单 IP+账号 5 次/分钟，单 IP 30 次/分钟，单账号 10 次/分钟 */
    private static final int MAX_LOGIN_PER_MINUTE = 5;
    private static final int MAX_LOGIN_PER_IP_MINUTE = 30;
    private static final int MAX_LOGIN_PER_ACCOUNT_MINUTE = 10;
    /** 注册限流：单账号名 3 次/分钟，单 IP 20 次/分钟 */
    private static final int MAX_REGISTER_PER_MINUTE = 3;
    private static final int MAX_REGISTER_PER_IP_MINUTE = 20;

    private static final String GENERIC_REGISTER_FAIL = "注册失败，请检查输入或稍后重试";

    public UserServiceImpl(UserMapper userMapper, CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.captchaService = captchaService;
    }

    @Override
    @Transactional
    public ApiResponse<Object> register(RegisterRequest request, String clientIp) {
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer())) {
            return ApiResponse.fail("验证码错误或已过期");
        }
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

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            return ApiResponse.fail("手机号绑定需短信验证，当前未开放");
        }

        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            log.info("注册失败: 用户名 {} 已存在", request.getUsername());
            return ApiResponse.fail(GENERIC_REGISTER_FAIL);
        }

        String email = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim().toLowerCase(Locale.ROOT) : null;
        if (email != null && !userMapper.findByEmail(email).isEmpty()) {
            log.info("注册失败: 邮箱已被使用");
            return ApiResponse.fail(GENERIC_REGISTER_FAIL);
        }

        if (!PasswordUtil.isBcryptCompatible(request.getPassword())) {
            return ApiResponse.fail("密码过长，UTF-8 编码后不能超过 72 字节");
        }
        String encodedPassword = PasswordUtil.encode(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname() != null && !request.getNickname().isBlank()
                ? XssUtil.escape(request.getNickname().trim()) : null);
        user.setPassword(encodedPassword);
        user.setEmail(email);
        // M4：手机号未经过短信 OTP 验证，注册阶段一律不落库
        user.setPhone(null);

        int rows;
        try {
            rows = userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            log.info("注册失败: 唯一键冲突 username={}", request.getUsername());
            return ApiResponse.fail(GENERIC_REGISTER_FAIL);
        }

        log.info("注册结果: username={}, rows={}", request.getUsername(), rows);

        if (rows > 0) {
            return ApiResponse.success("注册成功");
        }
        return ApiResponse.fail(GENERIC_REGISTER_FAIL);
    }

    @Override
    public ApiResponse<RefreshTokenResponse> login(LoginRequest request, String clientIp) {
        if (!captchaService.verify(request.getCaptchaId(), request.getCaptchaAnswer())) {
            return ApiResponse.fail("验证码错误或已过期");
        }
        String loginKey = request.getUsername() == null ? "" : request.getUsername().trim().toLowerCase(Locale.ROOT);
        if (RateLimiterUtil.isBlocked("login:ip:" + clientIp, MAX_LOGIN_PER_IP_MINUTE)
                || RateLimiterUtil.isBlocked("login:acc:" + clientIp + ":" + loginKey, MAX_LOGIN_PER_MINUTE)
                || RateLimiterUtil.isBlocked("login:acct:" + loginKey, MAX_LOGIN_PER_ACCOUNT_MINUTE)) {
            log.warn("登录速率限制触发: IP={}, username={}", clientIp, request.getUsername());
            return ApiResponse.fail("登录请求过于频繁，请稍后重试");
        }

        log.debug("收到登录请求: username={}", request.getUsername());

        // M4：手机号需短信 OTP 验证后才可用于登录，当前仅支持用户名
        User user = userMapper.findByUsername(request.getUsername());

        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            if (user != null) {
                userMapper.incrementFailedAttempts(user.getId());
                int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
                AuditLogger.log("LOGIN_FAIL", "ip=" + clientIp + ", username=" + request.getUsername()
                        + ", failCount=" + attempts);
            } else {
                AuditLogger.log("LOGIN_FAIL", "ip=" + clientIp + ", username=" + request.getUsername()
                        + ", reason=user_not_found");
            }
            log.info("登录失败: username={}", request.getUsername());
            // M3：不暴露账号是否存在、是否被锁定、精确解锁时间
            return ApiResponse.fail("用户名或密码错误");
        }

        if (user.getFailedAttempts() != null && user.getFailedAttempts() > 0) {
            userMapper.clearFailedAttempts(user.getId());
        }

        int tokenVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), tokenVersion);
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), tokenVersion);

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

        if (!JwtUtil.isRefreshToken(request.getRefreshToken())) {
            return ApiResponse.fail("refreshToken 无效或已过期");
        }

        Integer userId = JwtUtil.getUserIdFromToken(request.getRefreshToken());
        if (userId == null) {
            return ApiResponse.fail("refreshToken 无效或已过期");
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return ApiResponse.fail("用户不存在");
        }

        Integer tokenVersionInToken = JwtUtil.getTokenVersionFromToken(request.getRefreshToken());
        if (tokenVersionInToken == null) {
            return ApiResponse.fail("refreshToken 无效或已过期");
        }
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
        String newAccessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername(), newVersion);
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

        if (!PasswordUtil.isBcryptCompatible(request.getNewPassword())) {
            return ApiResponse.fail("新密码过长，UTF-8 编码后不能超过 72 字节");
        }
        String newEncoded = PasswordUtil.encode(request.getNewPassword());
        userMapper.updatePassword(userId, newEncoded);
        userMapper.incrementTokenVersion(userId);
        AuditLogger.log("PASSWORD_CHANGED", "userId=" + userId);
        log.info("密码修改成功，已失效所有 refresh token: userId={}", userId);
        return ApiResponse.success("密码修改成功");
    }

}
