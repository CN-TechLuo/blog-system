-- ============================================================
-- V4: 安全加固
--   1. email 唯一（重复邮箱保留 id 最小的账号，其余置 NULL）
--   2. password_reset_token.token_hash 唯一
--   3. report(reporter_id, target_type, target_id) 唯一
--   4. ai_usage.call_count（按上游调用计量）
--   5. 外键改为 ON DELETE CASCADE
-- 执行前请先备份数据库。
-- ============================================================

-- 1. 归一化空邮箱
UPDATE `user` SET `email` = NULL WHERE `email` = '';

-- 2. 重复邮箱去重：保留最早账号，其余账号的 email 置 NULL
UPDATE `user` u
JOIN (
    SELECT `email`, MIN(`id`) AS keep_id
    FROM `user`
    WHERE `email` IS NOT NULL AND `email` <> ''
    GROUP BY `email`
    HAVING COUNT(*) > 1
) d ON u.`email` = d.`email` AND u.`id` <> d.keep_id
SET u.`email` = NULL;

-- 3. email 唯一
ALTER TABLE `user` ADD UNIQUE INDEX `uk_email` (`email`);

-- 4. 重置令牌哈希唯一（先清理历史重复）
DELETE t1 FROM `password_reset_token` t1
JOIN `password_reset_token` t2
  ON t1.`token_hash` = t2.`token_hash` AND t1.`id` > t2.`id`;
ALTER TABLE `password_reset_token` ADD UNIQUE INDEX `uk_token_hash` (`token_hash`);

-- 5. 同一举报人对同一对象只保留一条记录
DELETE r1 FROM `report` r1
JOIN `report` r2
  ON r1.`reporter_id` = r2.`reporter_id`
 AND r1.`target_type` = r2.`target_type`
 AND r1.`target_id` = r2.`target_id`
 AND r1.`id` > r2.`id`;
ALTER TABLE `report` ADD UNIQUE INDEX `uk_reporter_target` (`reporter_id`, `target_type`, `target_id`);

-- 6. AI 上游调用次数（一次请求可能分块调用多次）
ALTER TABLE `ai_usage` ADD COLUMN `call_count` INT DEFAULT 1 NOT NULL AFTER `output_chars`;

-- 7. 清理孤儿 AI 用量后补外键
DELETE FROM `ai_usage` WHERE `user_id` NOT IN (SELECT `id` FROM `user`);
ALTER TABLE `ai_usage`
  ADD CONSTRAINT `fk_ai_usage_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

-- 8. 用户/文章外键改为 ON DELETE CASCADE
ALTER TABLE `article` DROP FOREIGN KEY `fk_article_user`;
ALTER TABLE `article`
  ADD CONSTRAINT `fk_article_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

ALTER TABLE `comment` DROP FOREIGN KEY `fk_comment_article`;
ALTER TABLE `comment` DROP FOREIGN KEY `fk_comment_user`;
ALTER TABLE `comment`
  ADD CONSTRAINT `fk_comment_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

ALTER TABLE `feedback` DROP FOREIGN KEY `fk_feedback_user`;
ALTER TABLE `feedback`
  ADD CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

ALTER TABLE `article_like` DROP FOREIGN KEY `fk_article_like_user`;
ALTER TABLE `article_like` DROP FOREIGN KEY `fk_article_like_article`;
ALTER TABLE `article_like`
  ADD CONSTRAINT `fk_article_like_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_article_like_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE;

ALTER TABLE `bookmark` DROP FOREIGN KEY `fk_bookmark_user`;
ALTER TABLE `bookmark` DROP FOREIGN KEY `fk_bookmark_article`;
ALTER TABLE `bookmark`
  ADD CONSTRAINT `fk_bookmark_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_bookmark_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE;

ALTER TABLE `follow` DROP FOREIGN KEY `fk_follow_follower`;
ALTER TABLE `follow` DROP FOREIGN KEY `fk_follow_followee`;
ALTER TABLE `follow`
  ADD CONSTRAINT `fk_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

ALTER TABLE `notification` DROP FOREIGN KEY `fk_notification_user`;
ALTER TABLE `notification`
  ADD CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

ALTER TABLE `password_reset_token` DROP FOREIGN KEY `fk_password_reset_token_user`;
ALTER TABLE `password_reset_token`
  ADD CONSTRAINT `fk_password_reset_token_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;

ALTER TABLE `article_tag` DROP FOREIGN KEY `fk_article_tag_article`;
ALTER TABLE `article_tag` DROP FOREIGN KEY `fk_article_tag_tag`;
ALTER TABLE `article_tag`
  ADD CONSTRAINT `fk_article_tag_article` FOREIGN KEY (`article_id`) REFERENCES `article`(`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_article_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `tag`(`id`) ON DELETE CASCADE;

ALTER TABLE `report` DROP FOREIGN KEY `fk_report_reporter`;
ALTER TABLE `report`
  ADD CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user`(`id`) ON DELETE CASCADE;
