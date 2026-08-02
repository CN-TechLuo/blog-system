package com.blog.blogsystme.mapper;

import com.blog.blogsystme.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Insert("INSERT INTO comment (article_id, user_id, content) VALUES (#{articleId}, #{userId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} ORDER BY create_time ASC LIMIT #{start}, #{pageSize}")
    List<Comment> findByArticleId(@Param("articleId") Integer articleId,
                                  @Param("start") int start,
                                  @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId}")
    int countByArticleId(@Param("articleId") Integer articleId);

    @Select("SELECT * FROM comment WHERE id = #{id}")
    Comment findById(@Param("id") Integer id);

    @Delete("DELETE FROM comment WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

}
