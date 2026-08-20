package com.blog.blogsystem.dto;

import com.blog.blogsystem.util.PasswordUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度需在2-20位之间")
    private String username;

    @Size(max = 30, message = "昵称长度不能超过30位")
    private String nickname;

    @Pattern(regexp = "^(|\\d{11})$", message = "请输入正确的11位手机号或留空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度需在8-50位之间")
    @Pattern(regexp = PasswordUtil.PASSWORD_REGEX,
             message = "密码必须包含字母、数字和特殊字符")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 64, message = "验证码ID不合法")
    private String captchaId;

    @Size(max = 16, message = "验证码答案不合法")
    private String captchaAnswer;
}
