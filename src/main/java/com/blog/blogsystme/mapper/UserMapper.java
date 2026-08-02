package com.blog.blogsystme.mapper;

import com.blog.blogsystme.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    int insert(User user);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    @Select("<script>SELECT * FROM user WHERE id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<User> findByIds(@Param("ids") List<Integer> ids);

    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    @Select("SELECT token_version FROM user WHERE id = #{id}")
    Integer findTokenVersion(@Param("id") Integer id);

    @Update("UPDATE user SET token_version = token_version + 1 WHERE id = #{id}")
    int incrementTokenVersion(@Param("id") Integer id);

}
