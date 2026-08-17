package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "绑定邮箱或手机号不能为空")
    private String verify;

}
