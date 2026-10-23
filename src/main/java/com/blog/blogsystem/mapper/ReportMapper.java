package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Report;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 举报数据访问接口（分页联查举报人/被举报内容摘要）
 *
 * @author CN-TechLuo
 * @date 2026/08/21
 */
@Mapper
public interface ReportMapper {

    /**
     * 新增举报记录
     *
     * @param report 举报实体
     * @return 影响行数
     */
    @Insert("INSERT INTO report (reporter_id, target_type, target_id, reason, detail) " +
            "VALUES (#{reporterId}, #{targetType}, #{targetId}, #{reason}, #{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Report report);

    /**
     * 统计同一用户对同一对象的待处理举报数（防重复举报）
     *
     * @param reporterId 举报人用户ID
     * @param targetType 举报对象类型（article/comment）
     * @param targetId   举报对象ID
     * @return 待处理举报数
     */
    @Select("SELECT COUNT(*) FROM report WHERE reporter_id = #{reporterId} " +
            "AND target_type = #{targetType} AND target_id = #{targetId} AND status = 'pending'")
    int countPendingByReporterAndTarget(@Param("reporterId") Integer reporterId,
                                        @Param("targetType") String targetType,
                                        @Param("targetId") Integer targetId);

    /**
     * 按 ID 查询举报记录
     *
     * @param id 举报ID
     * @return 举报实体，不存在返回 null
     */
    @Select("SELECT * FROM report WHERE id = #{id}")
    Report findById(@Param("id") Integer id);

    /**
     * 分页查询举报列表（联查举报人用户名与被举报内容摘要，待处理优先）
     *
     * @param status   状态筛选（null 查全部，pending/resolved）
     * @param start    偏移量
     * @param pageSize 每页条数
     * @return 举报列表
     */
    @Select("<script>" +
            "SELECT r.*, u.username AS reporter_name, " +
            "CASE WHEN r.target_type = 'article' THEN (SELECT a.title FROM article a WHERE a.id = r.target_id) END AS target_title, " +
            "CASE WHEN r.target_type = 'comment' THEN (SELECT c.content FROM comment c WHERE c.id = r.target_id) END AS target_content " +
            "FROM report r LEFT JOIN user u ON u.id = r.reporter_id " +
            "<where><if test='status != null and status != \"\"'>r.status = #{status}</if></where> " +
            "ORDER BY (r.status = 'pending') DESC, r.create_time DESC LIMIT #{start}, #{pageSize}" +
            "</script>")
    List<Report> findByPage(@Param("status") String status,
                            @Param("start") int start,
                            @Param("pageSize") int pageSize);

    /**
     * 统计举报总数（可按状态筛选）
     *
     * @param status 状态筛选（null 查全部）
     * @return 举报总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM report r " +
            "<where><if test='status != null and status != \"\"'>r.status = #{status}</if></where>" +
            "</script>")
    int countByStatus(@Param("status") String status);

    /**
     * 标记举报已处理
     *
     * @param id 举报ID
     * @return 影响行数
     */
    @Update("UPDATE report SET status = 'resolved' WHERE id = #{id}")
    int markResolved(@Param("id") Integer id);

    /**
     * 删除用户发起的全部举报（账号注销时级联清理）
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM report WHERE reporter_id = #{userId}")
    int deleteByReporterId(@Param("userId") Integer userId);

}
