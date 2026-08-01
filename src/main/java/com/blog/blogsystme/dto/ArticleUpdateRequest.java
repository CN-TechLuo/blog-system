package com.blog.blogsystme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文章更新请求 DTO
 */
@Data
public class ArticleUpdateRequest {

    @NotNull(message = "文章ID不能为空")
    private Integer id;

    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 200, message = "文章标题长度需在1-200位之间")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    private String content;

}
