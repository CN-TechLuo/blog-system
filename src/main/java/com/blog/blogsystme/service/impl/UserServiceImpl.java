package com.blog.blogsystme.service.impl;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.ChangePasswordRequest;
import com.blog.blogsystme.dto.LoginRequest;
import com.blog.blogsystme.dto.RefreshTokenRequest;
import com.blog.blogsystme.dto.RefreshTokenResponse;
import com.blog.blogsystme.dto.RegisterRequest;
import com.blog.blogsystme.dto.UserInfoResponse;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.UserMapper;
import com.blog.blogsystme.service.UserService;
import com.blog.blogsystme.util.JwtUtil;
import com.blog.blogsystme.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;

    private record RateWindow(long startMs, long count) {}

    private static final ConcurrentHashMap<String, RateWindow> LOGIN_RATE_LIMIT = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, RateWindow> REGISTER_RATE_LIMIT = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_PER_MINUTE = 5;
    private static final int MAX_REGISTER_PER_MINUTE = 3;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            LOGIN_RATE_LIMIT.entrySet().removeIf(e -> now - e.getValue().startMs() > RATE_LIMIT_WINDOW_MS);
            REGISTER_RATE_LIMIT.entrySet().removeIf(e -> now - e.getValue().startMs() > RATE_LIMIT_WINDOW_MS);
        }, 60, 60, TimeUnit.SECONDS);
    }

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public ApiResponse<Object> register(RegisterRequest request, String clientIp) {
        if (checkRateLimit(REGISTER_RATE_LIMIT, MAX_REGISTER_PER_MINUTE, clientIp)) {
            log.warn("注册速率限制触发: IP={}, username={}", clientIp, request.getUsername());
            return ApiResponse.fail("注册请求过于频繁，请稍后重试");
        }

        log.debug("收到注册请求: username={}", request.getUsername());

        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            log.info("注册失败: 用户名 {} 已存在", request.getUsername());
            return ApiResponse.fail("用户名已存在，注册失败");
        }

        String encodedPassword = PasswordUtil.encode(request.getPassword());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(encodedPassword);
        user.setEmail(request.getEmail());
        int rows = userMapper.insert(user);

        log.info("注册结果: username={}, rows={}", request.getUsername(), rows);

        if (rows > 0) {
            return ApiResponse.success("注册成功");
        }
        return ApiResponse.fail("注册失败，请稍后重试");
    }

    @Override
    public ApiResponse<RefreshTokenResponse> login(LoginRequest request, String clientIp) {
        if (checkRateLimit(LOGIN_RATE_LIMIT, MAX_LOGIN_PER_MINUTE, clientIp)) {
            log.warn("登录速率限制触发: IP={}, username={}", clientIp, request.getUsername());
            return ApiResponse.fail("登录请求过于频繁，请稍后重试");
        }

        log.debug("收到登录请求: username={}", request.getUsername());

        User user = userMapper.findByUsername(request.getUsername());
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            log.info("登录失败: username={}", request.getUsername());
            return ApiResponse.fail("用户名或密码错误");
        }

        String accessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getTokenVersion() != null ? user.getTokenVersion() : 0);

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
            return ApiResponse.fail("refreshToken 已失效，请重新登录");
        }

        String newAccessToken = JwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String newRefreshToken = JwtUtil.generateRefreshToken(user.getId(), user.getUsername(), currentVersion);

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
        info.setEmail(user.getEmail());
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
        log.info("密码修改成功，已失效所有 refresh token: userId={}", userId);
        return ApiResponse.success("密码修改成功");
    }

    private boolean checkRateLimit(ConcurrentHashMap<String, RateWindow> rateMap,
                                   int maxAttempts, String ip) {
        long now = System.currentTimeMillis();
        RateWindow window = rateMap.compute(ip, (key, val) -> {
            if (val == null || now - val.startMs() > RATE_LIMIT_WINDOW_MS) {
                return new RateWindow(now, 1);
            }
            return new RateWindow(val.startMs(), val.count() + 1);
        });
        return window.count() > maxAttempts;
    }

}
