package com.blog.blogsystme.mapper;

import com.blog.blogsystme.entity.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ArticleMapper {

    @Insert("INSERT INTO article (title, content, user_id) VALUES (#{title}, #{content}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @Select("SELECT * FROM article WHERE id = #{id}")
    Article findById(Integer id);

    @Update("UPDATE article SET title = #{title}, content = #{content} WHERE id = #{id} AND user_id = #{userId}")
    int updateByAuthor(Article article);

    @Delete("DELETE FROM article WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndAuthor(@Param("id") Integer id, @Param("userId") Integer userId);

    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Integer id);

    @Select("SELECT COUNT(*) FROM article")
    int count();

    @Select("SELECT id, title, user_id, view_count, create_time, update_time " +
            "FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);

    @Select("SELECT id, title, user_id, view_count, create_time, update_time " +
            "FROM article WHERE title LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> searchByTitle(@Param("keyword") String keyword,
                                @Param("start") int start,
                                @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM article WHERE title LIKE CONCAT('%', #{keyword}, '%')")
    int countByKeyword(@Param("keyword") String keyword);

}
