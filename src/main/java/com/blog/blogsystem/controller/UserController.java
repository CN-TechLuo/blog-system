package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.ChangePasswordRequest;
import com.blog.blogsystem.dto.LoginRequest;
import com.blog.blogsystem.dto.RefreshTokenRequest;
import com.blog.blogsystem.dto.RefreshTokenResponse;
import com.blog.blogsystem.dto.NicknameRequest;
import com.blog.blogsystem.dto.PhoneRequest;
import com.blog.blogsystem.dto.RegisterRequest;
import com.blog.blogsystem.dto.UserInfoResponse;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.mapper.UserMapper;
import com.blog.blogsystem.service.UserService;
import com.blog.blogsystem.util.ImageUtil;
import com.blog.blogsystem.util.XssUtil;
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

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "注册、登录、个人信息")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
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

    @PostMapping("/avatar")
    @Operation(summary = "上传头像 (Base64)")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestAttribute("userId") Integer userId,
                                                             @RequestBody Map<String, String> body) {
        String base64 = body.get("image");
        if (base64 == null || base64.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("请提供图片数据"));
        }
        if (base64.contains(",")) {
            base64 = base64.substring(base64.indexOf(",") + 1);
        }
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("图片数据格式错误"));
        }
        if (imageBytes.length > 2 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("图片不能超过 2MB"));
        }

        try {
            String ext = ImageUtil.detectImageType(imageBytes);
            if (ext == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("不支持的图片格式，仅支持 JPG/PNG/GIF/WEBP"));
            }
            // 服务端重编码，剥离 polyglot 载荷
            imageBytes = ImageUtil.sanitizeImage(imageBytes, ext);
            File dir = new File("uploads");
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    throw new IOException("无法创建上传目录: " + dir.getAbsolutePath());
                }
            }
            String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            File dest = new File(dir, filename);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(imageBytes);
            }
            String avatarUrl = "/uploads/" + filename;
            userMapper.updateAvatar(userId, avatarUrl);
            return ResponseEntity.ok(ApiResponse.success("头像上传成功", avatarUrl));
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return ResponseEntity.internalServerError().body(ApiResponse.fail("上传失败"));
        }
    }

    @PutMapping("/nickname")
    @Operation(summary = "修改昵称")
    public ResponseEntity<ApiResponse<Void>> updateNickname(@RequestAttribute("userId") Integer userId,
                                                             @Valid @RequestBody NicknameRequest request) {
        userMapper.updateNickname(userId, XssUtil.escape(request.getNickname().trim()));
        return ResponseEntity.ok(ApiResponse.success("昵称修改成功"));
    }

    @PutMapping("/phone")
    @Operation(summary = "绑定手机号")
    public ResponseEntity<ApiResponse<Void>> bindPhone(@RequestAttribute("userId") Integer userId,
                                                        @Valid @RequestBody PhoneRequest request) {
        User existing = userMapper.findByPhone(request.getPhone());
        if (existing != null && !existing.getId().equals(userId)) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("该手机号已被其他用户绑定"));
        }
        userMapper.updatePhone(userId, request.getPhone());
        return ResponseEntity.ok(ApiResponse.success("手机号绑定成功"));
    }

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

}
