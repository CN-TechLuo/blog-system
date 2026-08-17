package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Follow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FollowMapper {

    @Insert("INSERT INTO follow (follower_id, followee_id) VALUES (#{followerId}, #{followeeId})")
    int insert(Follow follow);

    @Delete("DELETE FROM follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int delete(@Param("followerId") Integer followerId, @Param("followeeId") Integer followeeId);

    @Delete("DELETE FROM follow WHERE follower_id = #{userId} OR followee_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM follow WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    int exists(@Param("followerId") Integer followerId, @Param("followeeId") Integer followeeId);

    @Select("SELECT COUNT(*) FROM follow WHERE follower_id = #{userId}")
    int countFollowing(@Param("userId") Integer userId);

    @Select("SELECT COUNT(*) FROM follow WHERE followee_id = #{userId}")
    int countFollowers(@Param("userId") Integer userId);
}
