package com.blog.blogsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 文章收藏记录（对应 bookmark 表）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class Bookmark {

    /** 主键 */
    private Integer id;
    /** 收藏用户ID（外键 user.id） */
    private Integer userId;
    /** 文章ID（外键 article.id） */
    private Integer articleId;
    /** 创建时间 */
    private LocalDateTime createTime;

}
