package com.blog.blogsystme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户反馈请求
 */
@Data
public class FeedbackCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度需在1-200位之间")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

}
