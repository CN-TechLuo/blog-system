package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.ApiResponse;
import com.blog.blogsystem.dto.CaptchaResponse;
import com.blog.blogsystem.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码（公开接口）：注册/登录前获取
 */
@RestController
@RequestMapping("/api/captcha")
@Tag(name = "验证码", description = "图形验证码获取")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping
    @Operation(summary = "获取图形验证码")
    public ResponseEntity<ApiResponse<CaptchaResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("验证码获取成功", captchaService.generate()));
    }

}
