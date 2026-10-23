package com.blog.blogsystem.entity;

import lombok.Data;

/**
 * 文章标签（对应 tag 表，含文章数冗余统计）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class Tag {

    /** 主键 */
    private Integer id;
    /** 标签名称 */
    private String name;
    /** 关联文章数（冗余统计） */
    private Integer articleCount;

}
