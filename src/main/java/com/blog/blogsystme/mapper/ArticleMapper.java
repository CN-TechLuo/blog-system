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

    @Insert("INSERT INTO article (title, content, user_id, cover_url) VALUES (#{title}, #{content}, #{userId}, #{coverUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @Select("SELECT * FROM article WHERE id = #{id}")
    Article findById(Integer id);

    @Update("UPDATE article SET title = #{title}, content = #{content}, cover_url = #{coverUrl} WHERE id = #{id} AND user_id = #{userId}")
    int updateByAuthor(Article article);

    @Delete("DELETE FROM article WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndAuthor(@Param("id") Integer id, @Param("userId") Integer userId);

    @Update("UPDATE article SET view_count = view_count + 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Integer id);

    @Select("SELECT COUNT(*) FROM article")
    int count();

    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, create_time, update_time " +
            "FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);

    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, create_time, update_time " +
            "FROM article WHERE title LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> searchByTitle(@Param("keyword") String keyword,
                                @Param("start") int start,
                                @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM article WHERE title LIKE CONCAT('%', #{keyword}, '%')")
    int countByKeyword(@Param("keyword") String keyword);

    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, create_time, update_time " +
            "FROM article WHERE MATCH(title) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE) " +
            "ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> searchByTitleFulltext(@Param("keyword") String keyword,
                                        @Param("start") int start,
                                        @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM article WHERE MATCH(title) AGAINST(#{keyword} IN NATURAL LANGUAGE MODE)")
    int countByKeywordFulltext(@Param("keyword") String keyword);

    @Select("SELECT a.id, a.title, a.user_id, a.view_count, a.like_count, a.bookmark_count, a.comment_count, a.share_count, a.cover_url, a.create_time, a.update_time " +
            "FROM article a INNER JOIN follow f ON a.user_id = f.followee_id " +
            "WHERE f.follower_id = #{userId} ORDER BY a.create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findFollowingFeed(@Param("userId") Integer userId, @Param("start") int start, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM article a INNER JOIN follow f ON a.user_id = f.followee_id WHERE f.follower_id = #{userId}")
    int countFollowingFeed(@Param("userId") Integer userId);

    @Select("SELECT id, title, user_id, view_count, like_count, bookmark_count, comment_count, share_count, cover_url, create_time, update_time " +
            "FROM article ORDER BY hot_score DESC, id DESC LIMIT #{start}, #{pageSize}")
    List<Article> findHotFeed(@Param("start") int start, @Param("pageSize") int pageSize);

    @Update("UPDATE article SET like_count = like_count + 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Integer id);

    @Update("UPDATE article SET like_count = like_count - 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(@Param("id") Integer id);

    @Update("UPDATE article SET bookmark_count = bookmark_count + 1 WHERE id = #{id}")
    int incrementBookmarkCount(@Param("id") Integer id);

    @Update("UPDATE article SET bookmark_count = bookmark_count - 1 WHERE id = #{id} AND bookmark_count > 0")
    int decrementBookmarkCount(@Param("id") Integer id);

    @Update("UPDATE article SET comment_count = comment_count + 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Integer id);

    @Update("UPDATE article SET comment_count = comment_count - 1, hot_score = like_count + view_count * 0.1 + comment_count * 2 WHERE id = #{id} AND comment_count > 0")
    int decrementCommentCount(@Param("id") Integer id);

    @Select("SELECT id, title, user_id, view_count, like_count, comment_count, create_time FROM article ORDER BY create_time DESC")
    List<Article> findAllAdmin();

    @Select("SELECT id, title, user_id, view_count, like_count, comment_count, create_time FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findAllAdminPage(@Param("start") int start, @Param("pageSize") int pageSize);

    @Delete("DELETE FROM article WHERE id = #{id}")
    int deleteByAdmin(@Param("id") Integer id);

    @Delete("DELETE FROM article WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

}
