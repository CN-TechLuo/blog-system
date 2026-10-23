package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.ArticleLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 点赞数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface LikeMapper {

    /**
     * 新增点赞记录
     *
     * @param like 点赞实体（含用户ID与文章ID）
     * @return 影响行数
     */
    @Insert("INSERT INTO article_like (user_id, article_id) VALUES (#{userId}, #{articleId})")
    int insert(ArticleLike like);

    /**
     * 取消点赞
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int delete(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /**
     * 删除用户的全部点赞（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM article_like WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 删除文章的点赞记录（文章删除时级联清理）
     *
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM article_like WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);

    /**
     * 统计文章的点赞总数
     *
     * @param articleId 文章ID
     * @return 点赞总数
     */
    @Select("SELECT COUNT(*) FROM article_like WHERE article_id = #{articleId}")
    int countByArticleId(@Param("articleId") Integer articleId);

    /**
     * 判断用户是否已点赞该文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 1=已点赞 0=未点赞
     */
    @Select("SELECT COUNT(*) FROM article_like WHERE user_id = #{userId} AND article_id = #{articleId}")
    int exists(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /**
     * 批量查询用户已点赞的文章ID集合（用于列表标记点赞状态）
     *
     * @param userId     用户ID
     * @param articleIds 待查询的文章ID集合
     * @return 已点赞的文章ID列表
     */
    @Select("<script>SELECT article_id FROM article_like WHERE user_id = #{userId} AND article_id IN " +
            "<foreach item='id' collection='articleIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Integer> findLikedArticleIds(@Param("userId") Integer userId, @Param("articleIds") List<Integer> articleIds);

    /**
     * 统计全站点赞总数
     *
     * @return 点赞总数
     */
    @Select("SELECT COUNT(*) FROM article_like")
    int countAll();

}
