package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Feedback;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 反馈数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface FeedbackMapper {

    /**
     * 新增反馈记录
     *
     * @param feedback 反馈实体
     * @return 影响行数
     */
    @Insert("INSERT INTO feedback (user_id, title, content) VALUES (#{userId}, #{title}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Feedback feedback);

    /**
     * 分页查询用户的反馈列表
     *
     * @param userId   用户ID
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 反馈列表
     */
    @Select("SELECT * FROM feedback WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Feedback> findByUserId(@Param("userId") Integer userId,
                                @Param("start") int start,
                                @Param("pageSize") int pageSize);

    /**
     * 统计用户的反馈总数
     *
     * @param userId 用户ID
     * @return 反馈总数
     */
    @Select("SELECT COUNT(*) FROM feedback WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Integer userId);

    /**
     * 按 ID 查询反馈（校验归属，防止越权）
     *
     * @param id     反馈ID
     * @param userId 用户ID
     * @return 反馈实体，不存在返回 null
     */
    @Select("SELECT * FROM feedback WHERE id = #{id} AND user_id = #{userId}")
    Feedback findByIdAndUser(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 查询全部反馈（联查用户名/昵称，管理端用）
     *
     * @return 反馈列表
     */
    @Select("SELECT f.*, u.username, u.nickname FROM feedback f LEFT JOIN user u ON f.user_id = u.id ORDER BY f.create_time DESC")
    List<Feedback> findAllWithUser();

    /**
     * 分页查询全部反馈（联查用户名/昵称，管理端用）
     *
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 反馈列表
     */
    @Select("SELECT f.*, u.username, u.nickname FROM feedback f LEFT JOIN user u ON f.user_id = u.id ORDER BY f.create_time DESC LIMIT #{start}, #{pageSize}")
    List<Feedback> findAllWithUserPage(@Param("start") int start, @Param("pageSize") int pageSize);

    /**
     * 统计全部反馈总数
     *
     * @return 反馈总数
     */
    @Select("SELECT COUNT(*) FROM feedback")
    int countAll();

    /**
     * 更新反馈处理状态
     *
     * @param id     反馈ID
     * @param status 新状态（pending/resolved）
     * @return 影响行数
     */
    @Update("UPDATE feedback SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    /**
     * 删除用户的全部反馈（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM feedback WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

}
