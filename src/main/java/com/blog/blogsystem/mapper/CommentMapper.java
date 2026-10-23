package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论数据访问接口（含嵌套回复与级联删除）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface CommentMapper {

    /**
     * 新增评论
     *
     * @param comment 评论实体
     * @return 影响行数
     */
    @Insert("INSERT INTO comment (article_id, user_id, content) VALUES (#{articleId}, #{userId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    /**
     * 分页查询文章的评论（按时间正序）
     *
     * @param articleId 文章ID
     * @param start     偏移量
     * @param pageSize  每页条数
     * @return 评论列表
     */
    @Select("SELECT * FROM comment WHERE article_id = #{articleId} ORDER BY create_time ASC LIMIT #{start}, #{pageSize}")
    List<Comment> findByArticleId(@Param("articleId") Integer articleId,
                                  @Param("start") int start,
                                  @Param("pageSize") int pageSize);

    /**
     * 统计文章的评论总数
     *
     * @param articleId 文章ID
     * @return 评论总数
     */
    @Select("SELECT COUNT(*) FROM comment WHERE article_id = #{articleId}")
    int countByArticleId(@Param("articleId") Integer articleId);

    /**
     * 按 ID 查询评论
     *
     * @param id 评论ID
     * @return 评论实体，不存在返回 null
     */
    @Select("SELECT * FROM comment WHERE id = #{id}")
    Comment findById(@Param("id") Integer id);

    /**
     * 按 ID 查询评论（校验归属，防止越权）
     *
     * @param id     评论ID
     * @param userId 用户ID
     * @return 评论实体，不存在返回 null
     */
    @Select("SELECT * FROM comment WHERE id = #{id} AND user_id = #{userId}")
    Comment findByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 用户删除自己的评论（子回复随父评论级联删除）
     *
     * @param id     评论ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 删除用户的全部评论（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 管理员删除评论（无归属校验，举报处置用）
     *
     * @param id 评论ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment WHERE id = #{id}")
    int deleteByAdmin(@Param("id") Integer id);

    /**
     * 查询用户的评论（数据导出用，限条数）
     *
     * @param userId 用户ID
     * @param limit  条数上限
     * @return 评论列表
     */
    @Select("SELECT id, article_id, content, create_time FROM comment WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT #{limit}")
    List<Comment> findByUserId(@Param("userId") Integer userId, @Param("limit") int limit);

    /**
     * 删除文章的全部评论（文章删除时级联清理）
     *
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM comment WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);

    /**
     * 统计全站评论总数
     *
     * @return 评论总数
     */
    @Select("SELECT COUNT(*) FROM comment")
    int countAll();

}
