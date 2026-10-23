package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改昵称请求
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class NicknameRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称不能超过30位")
    private String nickname;

}
