package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.AiUsage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiUsageMapper {

    @Insert("INSERT INTO ai_usage (user_id, api_type, input_chars, output_chars) " +
            "VALUES (#{userId}, #{apiType}, #{inputChars}, #{outputChars})")
    int insert(@Param("userId") Integer userId, @Param("apiType") String apiType,
               @Param("inputChars") int inputChars, @Param("outputChars") int outputChars);

    @Select("SELECT COUNT(*) FROM ai_usage WHERE user_id = #{userId} AND create_time >= CURDATE()")
    int countToday(@Param("userId") Integer userId);

    @Select("SELECT a.id, a.user_id, u.username, a.api_type, a.input_chars, a.output_chars, a.create_time " +
            "FROM ai_usage a LEFT JOIN user u ON u.id = a.user_id " +
            "ORDER BY a.id DESC LIMIT #{limit}")
    List<AiUsage> findRecent(@Param("limit") int limit);
}
