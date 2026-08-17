package com.blog.blogsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Follow {
    private Integer id;
    private Integer followerId;
    private Integer followeeId;
    private LocalDateTime createTime;
}
