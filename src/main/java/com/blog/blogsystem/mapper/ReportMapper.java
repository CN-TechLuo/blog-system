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

@Mapper
public interface ReportMapper {

    @Insert("INSERT INTO report (reporter_id, target_type, target_id, reason, detail) " +
            "VALUES (#{reporterId}, #{targetType}, #{targetId}, #{reason}, #{detail})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Report report);

    @Select("SELECT COUNT(*) FROM report WHERE reporter_id = #{reporterId} " +
            "AND target_type = #{targetType} AND target_id = #{targetId} AND status = 'pending'")
    int countPendingByReporterAndTarget(@Param("reporterId") Integer reporterId,
                                        @Param("targetType") String targetType,
                                        @Param("targetId") Integer targetId);

    @Select("SELECT * FROM report WHERE id = #{id}")
    Report findById(@Param("id") Integer id);

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

    @Select("<script>" +
            "SELECT COUNT(*) FROM report r " +
            "<where><if test='status != null and status != \"\"'>r.status = #{status}</if></where>" +
            "</script>")
    int countByStatus(@Param("status") String status);

    @Update("UPDATE report SET status = 'resolved' WHERE id = #{id}")
    int markResolved(@Param("id") Integer id);

    @Delete("DELETE FROM report WHERE reporter_id = #{userId}")
    int deleteByReporterId(@Param("userId") Integer userId);

}
