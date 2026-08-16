package com.blog.blogsystme.mapper;

import com.blog.blogsystme.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PasswordResetTokenMapper {

    @Insert("INSERT INTO password_reset_token (user_id, token_hash, expires_at) VALUES (#{userId}, #{tokenHash}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PasswordResetToken token);

    @Select("SELECT * FROM password_reset_token WHERE token_hash = #{tokenHash} AND used = 0 ORDER BY id DESC LIMIT 1")
    PasswordResetToken findByHash(@Param("tokenHash") String tokenHash);

    @Update("UPDATE password_reset_token SET used = 1 WHERE id = #{id}")
    int markUsed(@Param("id") Integer id);

    @Update("UPDATE password_reset_token SET used = 1 WHERE user_id = #{userId} AND used = 0")
    int invalidateAllForUser(@Param("userId") Integer userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM password_reset_token WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

}
