package com.blog.blogsystem.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TokenBlacklistMapper {

    @Insert("INSERT INTO token_blacklist (jti, expire_time) VALUES (#{jti}, #{expireTime})")
    int insert(@Param("jti") String jti, @Param("expireTime") LocalDateTime expireTime);

    @Select("SELECT jti FROM token_blacklist WHERE jti = #{jti}")
    String findByJti(@Param("jti") String jti);

    @Select("SELECT jti FROM token_blacklist WHERE expire_time > NOW()")
    List<String> findActive();

    @Delete("DELETE FROM token_blacklist WHERE expire_time <= NOW()")
    int deleteExpired();
}
