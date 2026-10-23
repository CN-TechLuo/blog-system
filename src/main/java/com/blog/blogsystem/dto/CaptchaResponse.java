package com.blog.blogsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 图形验证码响应：captchaId 用于提交校验，svg 字段为 PNG data URI
 * （字段名保留以兼容旧前端，值不再包含可读文本）
 *
 * @author CN-TechLuo
 * @date 2026/08/21
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
