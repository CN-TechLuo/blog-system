package com.blog.blogsystme.dto;

import com.blog.blogsystme.util.PasswordUtil;
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

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度需在8-50位之间")
    @Pattern(regexp = PasswordUtil.PASSWORD_REGEX,
             message = "密码必须包含字母、数字和特殊字符")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;
}
