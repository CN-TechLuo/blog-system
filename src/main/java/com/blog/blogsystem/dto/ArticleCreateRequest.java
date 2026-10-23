package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章创建请求 DTO
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class ArticleCreateRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 200, message = "文章标题长度需在1-200位之间")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    @Size(max = 100000, message = "文章内容不能超过100000字")
    private String content;

    @Size(max = 500, message = "封面URL长度不能超过500位")
    @Pattern(regexp = "^(|/uploads/[A-Za-z0-9._-]+)$", message = "封面仅支持本站上传的图片")
    private String coverUrl;

    /** 是否含 AI 生成内容（深度合成合规：true 时文章显著标识"AI 生成"） */
    private Boolean aiGenerated;

}
