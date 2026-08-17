package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈数据对象
 */
@Data
public class Feedback {

    private Integer id;
    private Integer userId;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createTime;

}
