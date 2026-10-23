package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.util.AuditLogger;
import com.blog.blogsystem.util.ClientIpResolver;
import com.blog.blogsystem.util.RateLimiterUtil;
import com.blog.blogsystem.util.RoleConst;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * 一次性管理员引导：仅在系统尚无管理员时开放，需配置 admin.bootstrap-token
 * 并通过 X-Bootstrap-Token 请求头提交，避免“先注册 admin 用户名即提权”。
 *
 * @author CN-TechLuo
 * @date 2026/10/23
 */
@RestController
@RequestMapping("/api/bootstrap")
public class AdminBootstrapController {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapController.class);
    private static final int MAX_ATTEMPTS_PER_MINUTE = 5;

    private final UserMapper userMapper;
    private final ClientIpResolver clientIpResolver;

    @Value("${admin.bootstrap-token:}")
    private String bootstrapToken;

    public AdminBootstrapController(UserMapper userMapper, ClientIpResolver clientIpResolver) {
        this.userMapper = userMapper;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/admin")
    public ResponseEntity<ApiResponse<Void>> bootstrap(
            @RequestHeader(value = "X-Bootstrap-Token", required = false) String token,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        if (!isBootstrapConfigured()) {
            return ResponseEntity.status(404).body(ApiResponse.fail("未开放管理员引导"));
        }

        String ip = clientIpResolver.resolve(request);
        if (RateLimiterUtil.isBlocked("bootstrap:ip:" + ip, MAX_ATTEMPTS_PER_MINUTE)) {
            return ResponseEntity.status(429).body(ApiResponse.fail("操作过于频繁，请稍后重试"));
        }

        if (!tokenMatches(token)) {
            AuditLogger.log("ADMIN_BOOTSTRAP_DENIED", "ip=" + ip + ", reason=bad_token");
            return ResponseEntity.status(403).body(ApiResponse.fail("引导令牌无效"));
        }

        if (userMapper.countAdmins() > 0) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("管理员已存在，引导已关闭"));
        }

        Integer userId = parseUserId(body.get("userId"));
        if (userId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("userId 不合法"));
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("用户不存在"));
        }

        userMapper.updateRole(userId, RoleConst.ADMIN);
        AuditLogger.log("ADMIN_BOOTSTRAP", "userId=" + userId + ", ip=" + ip);
        log.warn("管理员引导完成: userId={}, ip={}", userId, ip);
        return ResponseEntity.ok(ApiResponse.success("管理员引导完成"));
    }

    /** 未配置、过短或仍为示例占位值时视为未开放（fail-closed） */
    private boolean isBootstrapConfigured() {
        return bootstrapToken != null
                && !bootstrapToken.isBlank()
                && !bootstrapToken.startsWith("CHANGE_ME")
                && bootstrapToken.length() >= 16;
    }

    private boolean tokenMatches(String token) {
        if (token == null) {
            return false;
        }
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                bootstrapToken.getBytes(StandardCharsets.UTF_8));
    }

    private Integer parseUserId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            int id = Integer.parseInt(String.valueOf(value).trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
