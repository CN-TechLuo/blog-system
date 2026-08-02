package com.blog.blogsystme.dto;

import com.blog.blogsystme.util.PasswordUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 50, message = "密码长度需在8-50位之间")
    @Pattern(regexp = PasswordUtil.PASSWORD_REGEX, message = "密码必须包含字母、数字和特殊字符")
    private String newPassword;

}
