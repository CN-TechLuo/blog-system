package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.AiUsage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 用量数据访问接口
 *
 * @author CN-TechLuo
 * @date 2026/08/19
 */
@Mapper
public interface AiUsageMapper {

    /**
     * 新增一条 AI 调用记录
     *
     * @param userId      用户ID
     * @param apiType     调用的 AI 功能类型
     * @param inputChars  输入字符数
     * @param outputChars 输出字符数
     * @param callCount   上游调用次数
     * @return 影响行数
     */
    @Insert("INSERT INTO ai_usage (user_id, api_type, input_chars, output_chars, call_count) " +
            "VALUES (#{userId}, #{apiType}, #{inputChars}, #{outputChars}, #{callCount})")
    int insert(@Param("userId") Integer userId, @Param("apiType") String apiType,
               @Param("inputChars") int inputChars, @Param("outputChars") int outputChars,
               @Param("callCount") int callCount);

    /**
     * 删除用户的全部 AI 用量记录（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM ai_usage WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);

    /**
     * 统计用户今日 AI 调用次数（用于每日配额校验）
     *
     * @param userId 用户ID
     * @return 今日调用次数
     */
    @Select("SELECT COALESCE(SUM(GREATEST(call_count, 1)), 0) FROM ai_usage " +
            "WHERE user_id = #{userId} AND create_time >= CURDATE()")
    int countToday(@Param("userId") Integer userId);

    /**
     * 查询最近 N 条 AI 用量记录（联查用户名，管理端展示）
     *
     * @param limit 条数上限
     * @return 用量记录列表
     */
    @Select("SELECT a.id, a.user_id, u.username, a.api_type, a.input_chars, a.output_chars, " +
            "a.call_count, a.create_time " +
            "FROM ai_usage a LEFT JOIN user u ON u.id = a.user_id " +
            "ORDER BY a.id DESC LIMIT #{limit}")
    List<AiUsage> findRecent(@Param("limit") int limit);
}
