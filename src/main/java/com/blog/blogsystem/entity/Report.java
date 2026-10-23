package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户举报记录（对应 report 表，投诉举报闭环）
 *
 * @author CN-TechLuo
 * @date 2026/08/21
 */
@Data
public class Report {

    /** 主键 */
    private Integer id;
    /** 举报人用户ID（外键 user.id） */
    private Integer reporterId;
    /** 举报对象类型（article | comment） */
    private String targetType;
    /** 举报对象ID */
    private Integer targetId;
    /** 举报原因分类 */
    private String reason;
    /** 补充说明 */
    private String detail;
    /** 处理状态（pending=待处理 resolved=已处理） */
    private String status;
    /** 创建时间 */
    private LocalDateTime createTime;

    /** 举报人用户名（非表字段，联查填充） */
    private transient String reporterName;
    /** 被举报文章标题（非表字段，联查填充） */
    private transient String targetTitle;
    /** 被举报评论内容（非表字段，联查填充） */
    private transient String targetContent;

}
