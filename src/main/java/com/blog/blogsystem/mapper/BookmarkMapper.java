package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Bookmark;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookmarkMapper {

    @Insert("INSERT INTO bookmark (user_id, article_id) VALUES (#{userId}, #{articleId})")
    int insert(Bookmark bookmark);

    @Delete("DELETE FROM bookmark WHERE user_id = #{userId} AND article_id = #{articleId}")
    int delete(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    @Delete("DELETE FROM bookmark WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    @Delete("DELETE FROM bookmark WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);

    @Select("SELECT COUNT(*) FROM bookmark WHERE user_id = #{userId} AND article_id = #{articleId}")
    int exists(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    @Select("<script>SELECT article_id FROM bookmark WHERE user_id = #{userId} AND article_id IN " +
            "<foreach item='id' collection='articleIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Integer> findBookmarkedArticleIds(@Param("userId") Integer userId, @Param("articleIds") List<Integer> articleIds);

    @Select("SELECT * FROM bookmark WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Bookmark> findByUserId(@Param("userId") Integer userId, @Param("start") int start, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM bookmark WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);
}
