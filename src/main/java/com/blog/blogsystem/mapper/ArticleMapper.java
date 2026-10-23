package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文章数据访问接口（列表/搜索/热榜/关注流等查询）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface ArticleMapper {

    /**
     * 新增文章（ai_generated 为空时默认 0）
     *
     * @param article 文章实体
     * @return 影响行数
     */
    @Insert("INSERT INTO article (title, content, user_id, cover_url, ai_generated) " +
            "VALUES (#{title}, #{content}, #{userId}, #{coverUrl}, COALESCE(#{aiGenerated}, 0))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    /**
     * 按 ID 查询文章
     *
     * @param id 文章ID
     * @return 文章实体，不存在返回 null
     */
    @Select("SELECT * FROM article WHERE id = #{id}")
    Article findById(Integer id);

    /**
     * 作者更新自己的文章（校验 userId 归属）
     *
     * @param article 文章实体（含 id/userId/新内容）
     * @return 影响行数
     */
    @Update("UPDATE article SET title = #{title}, content = #{content}, cover_url = #{coverUrl}, " +
            "ai_generated = COALESCE(#{aiGenerated}, 0) WHERE id = #{id} AND user_id = #{userId}")
    int updateByAuthor(Article article);

    /**
     * 作者删除自己的文章
     *
     * @param id     文章ID
     * @param userId 作者用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM article WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndAuthor(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 浏览量加一并更新热度分
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Update("UPDATE article SET view_count = view_count + 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Integer id);

    /**
     * 统计文章总数
     *
     * @return 文章总数
     */
    @Select("SELECT COUNT(*) FROM article")
    int count();

    /**
     * 分页查询文章列表（按创建时间倒序，列表页用）
     *
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 文章列表（不含正文）
     */
    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, ai_generated, create_time, update_time " +
            "FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 按标题模糊搜索文章（LIKE，分页）
     *
     * @param keyword  搜索关键词
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 文章列表
     */
    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, ai_generated, create_time, update_time " +
            "FROM article WHERE title LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> searchByTitle(@Param("keyword") String keyword,
                                @Param("start") int start,
                                @Param("pageSize") int pageSize);

    /**
     * 统计标题模糊搜索命中数
     *
     * @param keyword 搜索关键词
     * @return 命中总数
     */
    @Select("SELECT COUNT(*) FROM article WHERE title LIKE CONCAT('%', #{keyword}, '%')")
    int countByKeyword(@Param("keyword") String keyword);

    /**
     * 按标题全文索引搜索（ngram FULLTEXT，分页）
     *
     * @param keyword  搜索关键词
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 文章列表
     */
    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, ai_generated, create_time, update_time " +
            "FROM article WHERE MATCH(title) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> searchByTitleFulltext(@Param("keyword") String keyword,
                                        @Param("start") int start,
                                        @Param("pageSize") int pageSize);

    /**
     * 统计全文索引搜索命中数
     *
     * @param keyword 搜索关键词
     * @return 命中总数
     */
    @Select("SELECT COUNT(*) FROM article WHERE MATCH(title) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)")
    int countByKeywordFulltext(@Param("keyword") String keyword);

    /**
     * 分页查询关注用户的文章（关注流）
     *
     * @param userId   当前用户ID
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 文章列表
     */
    @Select("SELECT a.id, a.title, a.user_id, a.view_count, a.like_count, a.bookmark_count, a.comment_count, a.share_count, a.cover_url, a.ai_generated, a.create_time, a.update_time " +
            "FROM article a INNER JOIN follow f ON a.user_id = f.followee_id " +
            "WHERE f.follower_id = #{userId} ORDER BY a.create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findFollowingFeed(@Param("userId") Integer userId, @Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 统计关注流文章总数
     *
     * @param userId 当前用户ID
     * @return 文章总数
     */
    @Select("SELECT COUNT(*) FROM article a INNER JOIN follow f ON a.user_id = f.followee_id WHERE f.follower_id = #{userId}")
    int countFollowingFeed(@Param("userId") Integer userId);

    /**
     * 分页查询热榜文章（按热度分倒序）
     *
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 文章列表
     */
    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, ai_generated, create_time, update_time " +
            "FROM article ORDER BY hot_score DESC, id DESC LIMIT #{start}, #{pageSize}")
    List<Article> findHotFeed(@Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 点赞数加一并重算热度分
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Update("UPDATE article SET like_count = like_count + 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Integer id);

    /**
     * 点赞数减一并重算热度分（不低于 0）
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Update("UPDATE article SET like_count = like_count - 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(@Param("id") Integer id);

    /**
     * 收藏数加一
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Update("UPDATE article SET bookmark_count = bookmark_count + 1 WHERE id = #{id}")
    int incrementBookmarkCount(@Param("id") Integer id);

    /**
     * 收藏数减一（不低于 0）
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Update("UPDATE article SET bookmark_count = bookmark_count - 1 WHERE id = #{id} AND bookmark_count > 0")
    int decrementBookmarkCount(@Param("id") Integer id);

    /**
     * 评论数加一并重算热度分
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Update("UPDATE article SET comment_count = comment_count + 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Integer id);

    /**
     * 评论数减一并重算热度分（不低于 0）
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Update("UPDATE article SET comment_count = comment_count - 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id} AND comment_count > 0")
    int decrementCommentCount(@Param("id") Integer id);

    /**
     * 查询全部文章（管理端，不含正文）
     *
     * @return 文章列表
     */
    @Select("SELECT id, title, user_id, view_count, like_count, comment_count, create_time FROM article ORDER BY create_time DESC")
    List<Article> findAllAdmin();

    /**
     * 分页查询全部文章（管理端，不含正文）
     *
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 文章列表
     */
    @Select("SELECT id, title, user_id, view_count, like_count, comment_count, create_time FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findAllAdminPage(@Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 管理员删除文章（无归属校验，举报处置用）
     *
     * @param id 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM article WHERE id = #{id}")
    int deleteByAdmin(@Param("id") Integer id);

    /**
     * 删除用户的全部文章（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM article WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 查询用户的全部文章ID（账号注销时用于级联清理关联数据）
     *
     * @param userId 用户ID
     * @return 文章ID列表
     */
    @Select("SELECT id FROM article WHERE user_id = #{userId}")
    List<Integer> findIdsByUserId(@Param("userId") Integer userId);

    /**
     * 查询用户全部文章的封面 URL（账号注销时清理 uploads 文件）
     *
     * @param userId 用户ID
     * @return 封面 URL 列表
     */
    @Select("SELECT cover_url FROM article WHERE user_id = #{userId} AND cover_url IS NOT NULL")
    List<String> findCoverUrlsByUserId(@Param("userId") Integer userId);

    /**
     * 查询用户的文章（数据导出用，限条数，不含正文）
     *
     * @param userId 用户ID
     * @param limit  条数上限
     * @return 文章列表
     */
    @Select("SELECT id, title, view_count, like_count, comment_count, ai_generated, create_time " +
            "FROM article WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<Article> findByUserId(@Param("userId") Integer userId, @Param("limit") int limit);

    /**
     * 统计用户的文章总数
     *
     * @param userId 用户ID
     * @return 文章总数
     */
    @Select("SELECT COUNT(*) FROM article WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);

}
