package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Tag;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface TagMapper {

    @Insert("INSERT INTO tag (name, article_count) VALUES (#{name}, 1) " +
            "ON DUPLICATE KEY UPDATE article_count = article_count + 1")
    int insertOrIncrement(@Param("name") String name);

    @Update("UPDATE tag SET article_count = article_count - 1 WHERE name = #{name} AND article_count > 0")
    int decrementCount(@Param("name") String name);

    @Select("SELECT * FROM tag ORDER BY article_count DESC LIMIT 20")
    List<Tag> findHotTags();

    @Insert("INSERT INTO article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    int insertArticleTag(@Param("articleId") Integer articleId, @Param("tagId") Integer tagId);

    @Select("SELECT t.name FROM tag t INNER JOIN article_tag at ON t.id = at.tag_id WHERE at.article_id = #{articleId}")
    List<String> findTagsByArticleId(@Param("articleId") Integer articleId);

    @Select("SELECT name FROM tag WHERE name = #{name}")
    String findByName(@Param("name") String name);
}
