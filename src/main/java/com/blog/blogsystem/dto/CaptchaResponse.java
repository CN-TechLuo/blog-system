package com.blog.blogsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 图形验证码响应：captchaId 用于提交校验，svg 为内联 SVG 图片
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    private String captchaId;
    private String svg;
    /** 验证码功能是否开启（关闭时前端可隐藏输入框） */
    private boolean enabled;

}
