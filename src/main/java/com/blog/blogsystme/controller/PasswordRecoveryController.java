package com.blog.blogsystme.controller;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.ForgotPasswordRequest;
import com.blog.blogsystme.dto.ResetPasswordRequest;
import com.blog.blogsystme.service.PasswordRecoveryService;
import com.blog.blogsystme.util.RateLimiterUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Tag(name = "账号找回", description = "找回账号与密码重置")
public class PasswordRecoveryController {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryController.class);

    private final PasswordRecoveryService recoveryService;

    /** 每个 IP+账号 每分钟最多尝试次数 */
    private static final int MAX_RECOVERY_PER_MINUTE = 3;

    public PasswordRecoveryController(PasswordRecoveryService recoveryService) {
        this.recoveryService = recoveryService;
    }

    @PostMapping("/forgot-username")
    @Operation(summary = "通过邮箱找回账号")
    public ResponseEntity<ApiResponse<List<String>>> forgotUsername(HttpServletRequest request,
                                                                    @RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        if (email.isBlank() || !email.matches("^[\\w.-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请输入正确的邮箱地址"));
        }
        String ip = request.getRemoteAddr();
        String key = "find:" + ip + ":" + email.trim().toLowerCase();
        if (RateLimiterUtil.isBlocked(key, MAX_RECOVERY_PER_MINUTE)) {
            log.warn("找回账号速率限制触发: IP={}, email={}", ip, email);
            return ResponseEntity.status(429).body(ApiResponse.fail("操作过于频繁，请稍后重试"));
        }
        ApiResponse<List<String>> result = recoveryService.findUsernamesByEmail(email.trim());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "申请密码重置")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(HttpServletRequest request,
                                                                            @Valid @RequestBody ForgotPasswordRequest req) {
        String ip = request.getRemoteAddr();
        String account = req.getAccount() == null ? "" : req.getAccount().trim();
        String key = "reset:" + ip + ":" + account;
        if (RateLimiterUtil.isBlocked(key, MAX_RECOVERY_PER_MINUTE)) {
            log.warn("密码重置申请速率限制触发: IP={}, account={}", ip, account);
            return ResponseEntity.status(429).body(ApiResponse.fail("操作过于频繁，请稍后重试"));
        }
        ApiResponse<Map<String, Object>> result = recoveryService.requestReset(req);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public ResponseEntity<ApiResponse<Void>> resetPassword(HttpServletRequest request,
                                                             @Valid @RequestBody ResetPasswordRequest req) {
        String ip = request.getRemoteAddr();
        String tokenKey = req.getToken() == null ? "" : req.getToken().trim().substring(0, Math.min(16, req.getToken().trim().length()));
        String key = "resetpwd:" + ip + ":" + tokenKey;
        if (RateLimiterUtil.isBlocked(key, MAX_RECOVERY_PER_MINUTE)) {
            log.warn("密码重置速率限制触发: IP={}", ip);
            return ResponseEntity.status(429).body(ApiResponse.fail("操作过于频繁，请稍后重试"));
        }
        ApiResponse<Void> result = recoveryService.resetPassword(req);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }
}
