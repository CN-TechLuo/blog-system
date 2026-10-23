package com.blog.blogsystem.mapper;

import com.blog.blogsystem.entity.Tag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 标签数据访问接口（含文章-标签关联）
 *
 * @author CN-TechLuo
 * @date 2026/08/17
 */
@Mapper
public interface TagMapper {

    /**
     * 新增标签或计数加一（name 唯一键，存在则 article_count+1）
     *
     * @param name 标签名
     * @return 影响行数
     */
    @Insert("INSERT INTO tag (name, article_count) VALUES (#{name}, 1) " +
            "ON DUPLICATE KEY UPDATE article_count = article_count + 1")
    int insertOrIncrement(@Param("name") String name);

    /**
     * 标签文章数减一（删除文章时调用）
     *
     * @param name 标签名
     * @return 影响行数
     */
    @Update("UPDATE tag SET article_count = article_count - 1 WHERE name = #{name} AND article_count > 0")
    int decrementCount(@Param("name") String name);

    /**
     * 查询热门标签（按文章数倒序取前 20）
     *
     * @return 标签列表
     */
    @Select("SELECT * FROM tag ORDER BY article_count DESC LIMIT 20")
    List<Tag> findHotTags();

    /**
     * 新增文章-标签关联
     *
     * @param articleId 文章ID
     * @param tagId     标签ID
     * @return 影响行数
     */
    @Insert("INSERT INTO article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    int insertArticleTag(@Param("articleId") Integer articleId, @Param("tagId") Integer tagId);

    /**
     * 查询文章的全部标签名
     *
     * @param articleId 文章ID
     * @return 标签名列表
     */
    @Select("SELECT t.name FROM tag t INNER JOIN article_tag at ON t.id = at.tag_id WHERE at.article_id = #{articleId}")
    List<String> findTagsByArticleId(@Param("articleId") Integer articleId);

    /**
     * 按名称查询标签
     *
     * @param name 标签名
     * @return 标签名，不存在返回 null
     */
    @Select("SELECT name FROM tag WHERE name = #{name}")
    String findByName(@Param("name") String name);

    /**
     * 删除文章的全部标签关联（文章删除时级联清理）
     *
     * @param articleId 文章ID
     * @return 影响行数
     */
    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    int deleteArticleTags(@Param("articleId") Integer articleId);
}
