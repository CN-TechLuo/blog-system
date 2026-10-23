package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户反馈数据对象（对应 feedback 表）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class Feedback {

    /** 主键 */
    private Integer id;
    /** 反馈用户ID（外键 user.id） */
    private Integer userId;
    /** 反馈标题 */
    private String title;
    /** 反馈内容 */
    private String content;
    /** 处理状态（pending/resolved） */
    private String status;
    /** 创建时间 */
    private LocalDateTime createTime;

}
