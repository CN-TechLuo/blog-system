-- ============================================================
-- V7 迁移脚本（已有数据库执行一次）
-- 内容：
--   1. 孤儿数据清理（外键前置条件）
--   2. article 新增 hot_score + 热榜索引 + 复合索引 + FULLTEXT 搜索索引
--   3. 全部业务表补外键约束
-- 用法: mysql -u root -p < migration-v7.sql
-- 执行前建议备份: mysqldump -u root -p blog_db > backup_$(date +%F).sql
-- ============================================================
USE `blog_db`;

-- ------------------------------------------------------------
-- 1. 孤儿数据清理
-- ------------------------------------------------------------
DELETE FROM `article` WHERE user_id NOT IN (SELECT id FROM `user`);
DELETE FROM `comment` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR article_id NOT IN (SELECT id FROM `article`)
   OR (parent_id IS NOT NULL AND parent_id NOT IN (SELECT id FROM (SELECT id FROM `comment`) c));
DELETE FROM `article_like` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR article_id NOT IN (SELECT id FROM `article`);
DELETE FROM `bookmark` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR article_id NOT IN (SELECT id FROM `article`);
DELETE FROM `follow` WHERE follower_id NOT IN (SELECT id FROM `user`)
   OR followee_id NOT IN (SELECT id FROM `user`);
DELETE FROM `notification` WHERE user_id NOT IN (SELECT id FROM `user`)
   OR (from_user_id IS NOT NULL AND from_user_id NOT IN (SELECT id FROM `user`))
   OR (article_id IS NOT NULL AND article_id NOT IN (SELECT id FROM `article`))
   OR (comment_id IS NOT NULL AND comment_id NOT IN (SELECT id FROM `comment`));
DELETE FROM `feedback` WHERE user_id NOT IN (SELECT id FROM `user`);
DELETE FROM `password_reset_token` WHERE user_id NOT IN (SELECT id FROM `user`);
DELETE FROM `article_tag` WHERE article_id NOT IN (SELECT id FROM `article`)
   OR tag_id NOT IN (SELECT id FROM `tag`);

-- ------------------------------------------------------------
-- 2. 文章热榜字段与索引
-- ------------------------------------------------------------
ALTER TABLE `article` ADD COLUMN `hot_score` DOUBLE DEFAULT 0 NOT NULL AFTER `share_count`;
UPDATE `article` SET `hot_score` = `like_count` + `view_count` * 0.1 + `comment_count` * 2;
ALTER TABLE `article` ADD INDEX `idx_hot_score` (`hot_score`);
ALTER TABLE `article` ADD INDEX `idx_create_time_id` (`create_time`, `id`);
ALTER TABLE `article` ADD FULLTEXT INDEX `ft_title` (`title`) WITH PARSER ngram;

-- ------------------------------------------------------------
-- 3. 外键约束
-- ------------------------------------------------------------
ALTER TABLE `article`
  ADD CONSTRAINT `fk_article_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`);

ALTER TABLE `comment`
  ADD CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`),
  ADD CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  ADD CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE;

ALTER TABLE `feedback`
  ADD CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`);

ALTER TABLE `article_like`
  ADD CONSTRAINT `fk_article_like_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  ADD CONSTRAINT `fk_article_like_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`);

ALTER TABLE `bookmark`
  ADD CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  ADD CONSTRAINT `fk_bookmark_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`);

ALTER TABLE `follow`
  ADD CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`),
  ADD CONSTRAINT `fk_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user`(`id`);

ALTER TABLE `notification`
  ADD CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  ADD CONSTRAINT `fk_notification_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user`(`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_notification_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_notification_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment`(`id`) ON DELETE CASCADE;

ALTER TABLE `password_reset_token`
  ADD CONSTRAINT `fk_password_reset_token_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`);

ALTER TABLE `article_tag`
  ADD CONSTRAINT `fk_article_tag_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`),
  ADD CONSTRAINT `fk_article_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`);

-- ============================================================
-- V7 迁移完成
-- ============================================================
