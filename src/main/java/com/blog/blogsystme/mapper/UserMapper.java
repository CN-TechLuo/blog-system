package com.blog.blogsystme.mapper;
import com.blog.blogsystme.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper  // 告诉Spring：这是一个MyBatis的接口，需要生成代理对象
public interface UserMapper {

    // 插入用户数据（注册）
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    int insert(User user);  // 返回受影响的行数（成功应为1）

    // 根据用户名查询用户（登录时用）
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);
}