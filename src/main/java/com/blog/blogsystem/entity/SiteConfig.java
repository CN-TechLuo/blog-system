package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点配置（对应 site_config 表：管理员联系邮箱）
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
@Data
public class SiteConfig {

    /** 主键 */
    private Integer id;
    /** 站点管理员联系邮箱（浏览页左下角公开展示） */
    private String contactEmail;
    /** 更新时间 */
    private LocalDateTime updateTime;

}
