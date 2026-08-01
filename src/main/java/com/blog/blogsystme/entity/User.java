package com.blog.blogsystme.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户数据对象
 */
@Data
public class User {

    private Integer id;
    private String username;
    private String password;
    private String email;
    private LocalDateTime createTime;

}
