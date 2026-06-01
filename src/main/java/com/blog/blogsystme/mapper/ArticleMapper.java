package com.blog.blogsystme.mapper;
import com.blog.blogsystme.entity.Article;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleMapper {

    @Insert("INSERT INTO article (title, content, user_id) VALUES (#{title}, #{content}, #{authorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @Select("SELECT * FROM article WHERE id = #{id}")
    Article findById(Integer id);

    @Select("SELECT * FROM article ORDER BY create_time DESC")
    List<Article> findAll();

    @Update("UPDATE article SET title = #{title}, content = #{content} WHERE id = #{id}")
    int update(Article article);

    @Delete("DELETE FROM article WHERE id = #{id}")
    int deleteById(Integer id);
}
