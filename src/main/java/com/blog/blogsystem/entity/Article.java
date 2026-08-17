package com.blog.blogsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章数据对象
 */
@Data
public class Article {

    private Integer id;
    private String title;
    private String content;
    private Integer userId;
    private Integer viewCount;
    private Integer likeCount;
    private Integer bookmarkCount;
    private Integer commentCount;
    private Integer shareCount;
    private Double hotScore;
    private String coverUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private transient String authorName;
    private transient String authorAvatar;
    private transient Boolean isLiked;
    private transient Boolean isBookmarked;
    private transient Boolean isFollowing;
    private transient List<String> tags;

}
