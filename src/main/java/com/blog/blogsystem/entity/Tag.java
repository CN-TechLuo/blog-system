package com.blog.blogsystem.entity;

import lombok.Data;

@Data
public class Tag {
    private Integer id;
    private String name;
    private Integer articleCount;
}
