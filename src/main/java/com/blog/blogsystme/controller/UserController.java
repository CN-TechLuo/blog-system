package com.blog.blogsystme.controller;

import com.blog.blogsystme.util.JwtUtil;
import com.blog.blogsystme.util.PasswordUtil;
import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.LoginRequest;
import com.blog.blogsystme.entity.User;
import com.blog.blogsystme.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")

public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserMapper userMapper;

    /**
     * 速率限制记录：{窗口起始时间戳, 请求次数}
     */
    private record RateWindow(long startMs, long count) {}

    /** 登录速率限制：每个 IP 最多 5 次/分钟 */
    private static final ConcurrentHashMap<String, RateWindow> LOGIN_RATE_LIMIT = new ConcurrentHashMap<>();
    private static final int MAX_LOGIN_PER_MINUTE = 5;

    /** 注册速率限制：每个 IP 最多 3 次/分钟（独立于登录限制） */
    private static final ConcurrentHashMap<String, RateWindow> REGISTER_RATE_LIMIT = new ConcurrentHashMap<>();
    private static final int MAX_REGISTER_PER_MINUTE = 3;

    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleaner");
            t.setDaemon(true);
            return t;
        });
        Runnable cleanMap = (Runnable & java.io.Serializable) () -> {
            long now = System.currentTimeMillis();
            int removed = 0;
            for (Map.Entry<String, RateWindow> entry : LOGIN_RATE_LIMIT.entrySet()) {
                if (now - entry.getValue().startMs() > RATE_LIMIT_WINDOW_MS) {
                    LOGIN_RATE_LIMIT.remove(entry.getKey());
                    removed++;
                }
            }
            for (Map.Entry<String, RateWindow> entry : REGISTER_RATE_LIMIT.entrySet()) {
                if (now - entry.getValue().startMs() > RATE_LIMIT_WINDOW_MS) {
                    REGISTER_RATE_LIMIT.remove(entry.getKey());
                    removed++;
                }
            }
            if (removed > 0) {
                log.debug("速率限制清理: 移除 {} 个过期条目", removed);
            }
        };
        cleaner.scheduleAtFixedRate(cleanMap, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 注册接口：POST /api/user/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody User user,
                                                         HttpServletRequest request) {
        String clientIp = getClientIp(request);

        // 速率限制检查：每个 IP 每分钟最多 3 次注册尝试（独立于登录限制）
        if (recordAndCheckRateLimit(REGISTER_RATE_LIMIT, MAX_REGISTER_PER_MINUTE, clientIp)) {
            log.warn("注册速率限制触发: IP={}, username={}", clientIp, user.getUsername());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.fail("注册请求过于频繁，请稍后重试"));
        }

        log.debug("收到注册请求: username={}", user.getUsername());

        // 1. 检查用户名是否已存在
        User existUser = userMapper.findByUsername(user.getUsername());

        if (existUser != null) {
            log.info("注册失败: 用户名 {} 已存在", user.getUsername());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("用户名已存在，注册失败"));
        }

        // 2. 密码 BCrypt 加密
        String encodedPassword = PasswordUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 3. 插入数据库
        int rows = userMapper.insert(user);

        log.info("注册结果: username={}, rows={}", user.getUsername(), rows);

        if (rows > 0) {
            return ResponseEntity.ok(ApiResponse.success("注册成功"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("注册失败，请稍后重试"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                                   HttpServletRequest request) {
        String clientIp = getClientIp(request);

        // 速率限制检查：每个 IP 每分钟最多 5 次登录尝试（独立于注册限制）
        if (recordAndCheckRateLimit(LOGIN_RATE_LIMIT, MAX_LOGIN_PER_MINUTE, clientIp)) {
            log.warn("登录速率限制触发: IP={}, username={}", clientIp, loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.fail("登录请求过于频繁，请稍后重试"));
        }

        log.debug("收到登录请求: username={}", loginRequest.getUsername());

        // 1. 根据用户名查询用户
        User user = userMapper.findByUsername(loginRequest.getUsername());

        // 2. 验证密码（统一错误消息，防止用户枚举攻击）
        if (user == null || !PasswordUtil.matches(loginRequest.getPassword(), user.getPassword())) {
            log.info("登录失败: username={}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("用户名或密码错误"));
        }

        // 3. 生成 JWT token（嵌入 userId 避免后续请求查库）
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        log.info("登录成功: username={}", user.getUsername());

        // 4. 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", user.getUsername());
        data.put("userId", user.getId());
        return ResponseEntity.ok(ApiResponse.success("登录成功", data));
    }

    /**
     * 记录请求并检查是否超过速率限制（有副作用：会递增计数器）
     *
     * @param rateMap     目标速率限制 Map
     * @param maxAttempts 窗口内最大允许次数
     * @param ip          客户端 IP
     * @return true 表示已超过限制，应拒绝请求
     */
    private boolean recordAndCheckRateLimit(ConcurrentHashMap<String, RateWindow> rateMap,
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

    /**
     * 获取客户端真实 IP（考虑代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}