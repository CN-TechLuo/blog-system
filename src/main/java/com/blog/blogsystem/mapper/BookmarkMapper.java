package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Bookmark;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 收藏数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface BookmarkMapper {

    /**
     * 新增收藏记录
     *
     * @param bookmark 收藏实体（含用户ID与文章ID）
     * @return 影响行数
     */
    @Insert("INSERT INTO bookmark (user_id, article_id) VALUES (#{userId}, #{articleId})")
    int insert(Bookmark bookmark);

    /**
     * 取消收藏
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM bookmark WHERE user_id = #{userId} AND article_id = #{articleId}")
    int delete(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /**
     * 删除用户的全部收藏（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM bookmark WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 删除文章的收藏记录（文章删除时级联清理）
     *
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM bookmark WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);

    /**
     * 判断用户是否已收藏该文章
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 1=已收藏 0=未收藏
     */
    @Select("SELECT COUNT(*) FROM bookmark WHERE user_id = #{userId} AND article_id = #{articleId}")
    int exists(@Param("userId") Integer userId, @Param("articleId") Integer articleId);

    /**
     * 批量查询用户已收藏的文章ID集合（用于列表标记收藏状态）
     *
     * @param userId     用户ID
     * @param articleIds 待查询的文章ID集合
     * @return 已收藏的文章ID列表
     */
    @Select("<script>SELECT article_id FROM bookmark WHERE user_id = #{userId} AND article_id IN " +
            "<foreach item='id' collection='articleIds' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Integer> findBookmarkedArticleIds(@Param("userId") Integer userId, @Param("articleIds") List<Integer> articleIds);

    /**
     * 分页查询用户的收藏列表
     *
     * @param userId   用户ID
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 收藏记录列表
     */
    @Select("SELECT * FROM bookmark WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Bookmark> findByUserId(@Param("userId") Integer userId, @Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 统计用户的收藏总数
     *
     * @param userId 用户ID
     * @return 收藏总数
     */
    @Select("SELECT COUNT(*) FROM bookmark WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);
}
