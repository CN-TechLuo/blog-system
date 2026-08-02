package com.blog.blogsystme.mapper;

import com.blog.blogsystme.entity.Feedback;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FeedbackMapper {

    @Insert("INSERT INTO feedback (user_id, title, content) VALUES (#{userId}, #{title}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Feedback feedback);

    @Select("SELECT * FROM feedback WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Feedback> findByUserId(@Param("userId") Integer userId,
                                @Param("start") int start,
                                @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM feedback WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM feedback WHERE id = #{id} AND user_id = #{userId}")
    Feedback findByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

}
