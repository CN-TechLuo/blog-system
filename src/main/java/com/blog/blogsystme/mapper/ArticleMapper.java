package com.blog.blogsystme.mapper;
import com.blog.blogsystme.entity.Article;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ArticleMapper {

    @Insert("INSERT INTO article (title, content, user_id) VALUES (#{title}, #{content}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Article article);

    @Select("SELECT * FROM article WHERE id = #{id}")
    Article findById(Integer id);

    /**
     * @deprecated 该方法返回硬编码 LIMIT 100，无分页参数，可能静默截断数据。
     *             请使用 {@link #findByPage(int, int)} 配合 {@link #count()} 进行分页查询。
     */
    @Deprecated
    @Select("SELECT * FROM article ORDER BY create_time DESC LIMIT 100")
    List<Article> findAll();

    /** SQL 层面鉴权更新：仅作者本人可更新 */
    @Update("UPDATE article SET title = #{title}, content = #{content} WHERE id = #{id} AND user_id = #{userId}")
    int updateByAuthor(Article article);

    /** SQL 层面鉴权删除：仅作者本人可删除 */
    @Delete("DELETE FROM article WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndAuthor(@Param("id") Integer id, @Param("userId") Integer userId);

    /** 浏览量 +1 */
    @Update("UPDATE article SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(@Param("id") Integer id);

    /** 查询文章总数（用于分页） */
    @Select("SELECT COUNT(*) FROM article")
    int count();

    /**
     * 分页查询：列表页不返回 content 大字段，减少数据传输
     */
    @Select("SELECT id, title, user_id, view_count, create_time, update_time " +
            "FROM article ORDER BY create_time DESC LIMIT #{start}, #{pageSize}")
    List<Article> findByPage(@Param("start") int start, @Param("pageSize") int pageSize);

}
