package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章数据对象（对应 article 表）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Data
public class Article {

    /** 主键 */
    private Integer id;
    /** 标题 */
    private String title;
    /** 正文内容（Markdown，存储前经 XSS 清洗） */
    private String content;
    /** 作者用户ID（外键 user.id） */
    private Integer userId;
    /** 浏览量（冗余计数） */
    private Integer viewCount;
    /** 点赞数（冗余计数） */
    private Integer likeCount;
    /** 收藏数（冗余计数） */
    private Integer bookmarkCount;
    /** 评论数（冗余计数） */
    private Integer commentCount;
    /** 分享数（冗余计数） */
    private Integer shareCount;
    /** 热度分（热榜排序依据） */
    private Double hotScore;
    /** 封面图URL */
    private String coverUrl;
    /** AI 生成标识（1=含 AI 生成内容，深度合成合规要求显著标识） */
    private Integer aiGenerated;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 作者昵称（非表字段，查询时联表填充） */
    private transient String authorName;
    /** 作者头像（非表字段，查询时联表填充） */
    private transient String authorAvatar;
    /** 当前用户是否已点赞（非表字段） */
    private transient Boolean isLiked;
    /** 当前用户是否已收藏（非表字段） */
    private transient Boolean isBookmarked;
    /** 当前用户是否已关注作者（非表字段） */
    private transient Boolean isFollowing;
    /** 文章标签列表（非表字段） */
    private transient List<String> tags;

}
