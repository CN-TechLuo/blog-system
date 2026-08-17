package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NotificationMapper {

    @Insert("INSERT INTO notification (user_id, type, from_user_id, article_id, comment_id, content) " +
            "VALUES (#{userId}, #{type}, #{fromUserId}, #{articleId}, #{commentId}, #{content})")
    int insert(Notification notification);

    @Select("SELECT * FROM notification WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Notification> findByUserId(@Param("userId") Integer userId, @Param("start") int start, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Integer userId);

    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Integer userId);

    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Integer id, @Param("userId") Integer userId);

    @Delete("DELETE FROM notification WHERE user_id = #{userId} OR from_user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    @Delete("DELETE FROM notification WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);
}
