package com.blog.blogsystme.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Bookmark {
    private Integer id;
    private Integer userId;
    private Integer articleId;
    private LocalDateTime createTime;
}
