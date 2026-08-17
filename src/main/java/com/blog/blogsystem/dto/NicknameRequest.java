package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NicknameRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称不能超过30位")
    private String nickname;

}
