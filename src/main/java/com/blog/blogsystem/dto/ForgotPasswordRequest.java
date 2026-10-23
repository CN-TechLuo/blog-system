package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 找回密码申请请求（仅账号；重置链接只通过绑定邮箱发送）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过100位")
    private String account;

}
