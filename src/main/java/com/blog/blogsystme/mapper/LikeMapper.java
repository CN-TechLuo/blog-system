package com.blog.blogsystme.mapper;

import com.blog.blogsystme.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LikeMapper {

    @Insert("INSERT INTO article_like (user_id, article_id) VALUES (#{userId}, #{articleId})")
    int insert(ArticleLike like);

    @Delete("DELETE FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int delete(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    @Delete("DELETE FROM article_like WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    @Delete("DELETE FROM article_like WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);

    @Select("SELECT COUNT(*) FROM article_like WHERE article_id = #{articleId}")
    int countByArticleId(@Param("articleId") Integer articleId);

    @Select("SELECT COUNT(*) FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int exists(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    @Select("<script>SELECT article_id FROM article_like WHERE user_id = #{userId} AND article_id IN " +
            "<foreach item='id' collection='articleIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Integer> findLikedArticleIds(@Param("userId") Integer userId, @Param("articleIds") List<Integer> articleIds);

    @Select("SELECT COUNT(*) FROM article_like")
    int countAll();

}
