package com.blog.blogsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发表评论请求
 */
@Data
public class CommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;

}
