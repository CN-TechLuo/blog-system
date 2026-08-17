package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章创建请求 DTO
 */
@Data
public class ArticleCreateRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 200, message = "文章标题长度需在1-200位之间")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    @Size(max = 500, message = "封面URL长度不能超过500位")
    @Pattern(regexp = "^(|/uploads/[A-Za-z0-9._-]+|https?://[\\w.-]+/.*)$", message = "封面URL格式不正确")
    private String coverUrl;

}
