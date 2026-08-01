package com.blog.blogsystme.controller;

import com.blog.blogsystme.dto.ApiResponse;
import com.blog.blogsystme.dto.ChangePasswordRequest;
import com.blog.blogsystme.dto.LoginRequest;
import com.blog.blogsystme.dto.RefreshTokenRequest;
import com.blog.blogsystme.dto.RefreshTokenResponse;
import com.blog.blogsystme.dto.RegisterRequest;
import com.blog.blogsystme.dto.UserInfoResponse;
import com.blog.blogsystme.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "注册、登录、token 刷新、个人信息")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ResponseEntity<ApiResponse<Object>> register(@Valid @RequestBody RegisterRequest request,
                                                         HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        ApiResponse<Object> result = userService.register(request, clientIp);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> login(@Valid @RequestBody LoginRequest request,
                                                                    HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        ApiResponse<RefreshTokenResponse> result = userService.login(request, clientIp);
        return ResponseEntity.status(result.isSuccess() ? 200 : 401).body(result);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新 Access Token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(@RequestBody RefreshTokenRequest request) {
        ApiResponse<RefreshTokenResponse> result = userService.refreshToken(request);
        return ResponseEntity.status(result.isSuccess() ? 200 : 401).body(result);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<ApiResponse<UserInfoResponse>> me(@RequestAttribute("userId") Integer userId) {
        ApiResponse<UserInfoResponse> result = userService.getUserInfo(userId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestAttribute("userId") Integer userId,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        ApiResponse<Void> result = userService.changePassword(userId, request);
        return ResponseEntity.status(result.isSuccess() ? 200 : 400).body(result);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
