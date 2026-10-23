package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Follow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 关注关系数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface FollowMapper {

    /**
     * 新增关注关系
     *
     * @param follow 关注实体（含关注者与被关注者ID）
     * @return 影响行数
     */
    @Insert("INSERT INTO follow (follower_id, followee_id) VALUES (#{followerId}, #{followeeId})")
    int insert(Follow follow);

    /**
     * 取消关注
     *
     * @param followerId 关注者用户ID
     * @param followeeId 被关注者用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int delete(@Param("followerId") Integer followerId, @Param("followeeId") Integer followeeId);

    /**
     * 删除用户全部关注关系（作为关注者或被关注者，账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM follow WHERE follower_id = #{userId} OR followee_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 判断关注关系是否存在
     *
     * @param followerId 关注者用户ID
     * @param followeeId 被关注者用户ID
     * @return 1=已关注 0=未关注
     */
    @Select("SELECT COUNT(*) FROM follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int exists(@Param("followerId") Integer followerId, @Param("followeeId") Integer followeeId);

    /**
     * 统计用户关注的人数
     *
     * @param userId 用户ID
     * @return 关注数
     */
    @Select("SELECT COUNT(*) FROM follow WHERE follower_id = #{userId}")
    int countFollowing(@Param("userId") Integer userId);

    /**
     * 统计用户的粉丝数
     *
     * @param userId 用户ID
     * @return 粉丝数
     */
    @Select("SELECT COUNT(*) FROM follow WHERE followee_id = #{userId}")
    int countFollowers(@Param("userId") Integer userId);
}
