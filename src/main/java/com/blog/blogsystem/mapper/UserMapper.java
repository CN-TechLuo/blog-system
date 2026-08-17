package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO user (username, nickname, password, email, phone) VALUES (#{username}, #{nickname}, #{password}, #{email}, #{phone})")
    int insert(User user);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User findByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM user WHERE email = #{email}")
    List<User> findByEmail(@Param("email") String email);

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

    @Update("UPDATE user SET avatar_url = #{avatarUrl} WHERE id = #{id}")
    int updateAvatar(@Param("id") Integer id, @Param("avatarUrl") String avatarUrl);

    @Update("UPDATE user SET nickname = #{nickname} WHERE id = #{id}")
    int updateNickname(@Param("id") Integer id, @Param("nickname") String nickname);

    @Update("UPDATE user SET phone = #{phone} WHERE id = #{id}")
    int updatePhone(@Param("id") Integer id, @Param("phone") String phone);

    @Select("SELECT * FROM user ORDER BY id")
    List<User> findAll();

    @Select("SELECT * FROM user ORDER BY id LIMIT #{start}, #{pageSize}")
    List<User> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM user")
    int countAll();

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    @Update("UPDATE user SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") Integer id, @Param("role") String role);

    @Select("SELECT COUNT(*) FROM user WHERE role = 'admin'")
    int countAdmins();

}
