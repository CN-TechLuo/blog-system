package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @Size(max = 64, message = "验证码ID不合法")
    private String captchaId;

    @Size(max = 16, message = "验证码答案不合法")
    private String captchaAnswer;

}
