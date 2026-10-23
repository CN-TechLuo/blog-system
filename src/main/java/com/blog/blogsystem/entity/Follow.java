package com.blog.blogsystem.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户关注关系（对应 follow 表）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class Follow {

    /** 主键 */
    private Integer id;
    /** 关注者用户ID（外键 user.id） */
    private Integer followerId;
    /** 被关注者用户ID（外键 user.id） */
    private Integer followeeId;
    /** 创建时间 */
    private LocalDateTime createTime;

}
