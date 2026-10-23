package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 用量统计记录（对应 ai_usage 表，用于每日配额与成本核算）
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
@Data
public class AiUsage {

    /** 主键 */
    private Long id;
    /** 用户ID（外键 user.id） */
    private Integer userId;
    /** 用户名（冗余字段，管理端统计展示用） */
    private String username;
    /** AI 功能类型（如 chat/polish/generate） */
    private String apiType;
    /** 输入字符数 */
    private Integer inputChars;
    /** 输出字符数 */
    private Integer outputChars;
    /** 上游调用次数（长文分块时 >1） */
    private Integer callCount;
    /** 创建时间 */
    private LocalDateTime createTime;

}
