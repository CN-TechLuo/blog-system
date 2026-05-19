package com.blog.blogsystme.entity;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class User {
    private Integer id;             //用户ID,包装类型可null
    private String username;        //用户名
    private String password;        //密码
    private String email;           //邮箱
    private LocalDateTime createTime;   //注册时间

}
