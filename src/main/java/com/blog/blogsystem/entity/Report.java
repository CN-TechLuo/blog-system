package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户举报记录（投诉举报闭环）
 */
@Data
public class Report {

    private Integer id;
    private Integer reporterId;
    /** 举报对象: article | comment */
    private String targetType;
    private Integer targetId;
    /** 举报原因分类 */
    private String reason;
    /** 补充说明 */
    private String detail;
    /** pending=待处理 resolved=已处理 */
    private String status;
    private LocalDateTime createTime;

    private transient String reporterName;
    private transient String targetTitle;
    private transient String targetContent;

}
