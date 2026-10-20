package com.blog.blogsystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    /**
     * 仅接受邮箱。旧版本的 verify 字段已删除：
     * "能说出邮箱/手机号"不构成持有性证明，持有性只能通过邮件里的令牌证明。
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String account;

}
