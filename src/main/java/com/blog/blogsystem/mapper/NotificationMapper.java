package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface NotificationMapper {

    /**
     * 新增通知（点赞/评论/关注等互动触发）
     *
     * @param notification 通知实体
     * @return 影响行数
     */
    @Insert("INSERT INTO notification (user_id, type, from_user_id, article_id, comment_id, content) " +
            "VALUES (#{userId}, #{type}, #{fromUserId}, #{articleId}, #{commentId}, #{content})")
    int insert(Notification notification);

    /**
     * 分页查询用户的通知列表（按时间倒序）
     *
     * @param userId   用户ID
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 通知列表
     */
    @Select("SELECT * FROM notification WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Notification> findByUserId(@Param("userId") Integer userId, @Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 统计用户的通知总数
     *
     * @param userId 用户ID
     * @return 通知总数
     */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);

    /**
     * 统计用户的未读通知数
     *
     * @param userId 用户ID
     * @return 未读数
     */
    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Integer userId);

    /**
     * 标记用户全部通知为已读
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(@Param("userId") Integer userId);

    /**
     * 标记单条通知已读（校验归属，防止越权）
     *
     * @param id     通知ID
     * @param userId 用户ID
     * @return 影响行数
     */
    @Update("UPDATE notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markRead(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 删除用户相关通知（收到的 + 发出的，账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM notification WHERE user_id = #{userId} OR from_user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 删除文章相关通知（文章删除时级联清理）
     *
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM notification WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Integer articleId);
}
