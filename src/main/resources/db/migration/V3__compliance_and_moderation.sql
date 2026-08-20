-- ============================================================
-- V3: 上线前合规整改（举报处置闭环 + AI 生成内容标识）
-- 说明：Flyway 保证本脚本只执行一次；V1/V2 已应用后执行本脚本
-- ============================================================

-- 1. 文章增加 AI 生成标识（《互联网信息服务深度合成管理规定》要求显著标识）
ALTER TABLE `article` ADD COLUMN `ai_generated` TINYINT DEFAULT 0 NOT NULL COMMENT 'AI 生成标识 0=否 1=是' AFTER `cover_url`;

-- 2. 用户举报表（投诉举报闭环：上报 → 管理端处置 → 删除内容/驳回）
CREATE TABLE IF NOT EXISTS `report` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `reporter_id` INT NOT NULL,
  `target_type` VARCHAR(16) NOT NULL COMMENT '举报对象: article | comment',
  `target_id` INT NOT NULL,
  `reason` VARCHAR(32) NOT NULL COMMENT '举报原因分类',
  `detail` VARCHAR(500) DEFAULT NULL COMMENT '补充说明',
  `status` VARCHAR(16) DEFAULT 'pending' COMMENT 'pending=待处理 resolved=已处理',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_status_time` (`status`, `create_time`),
  INDEX `idx_target` (`target_type`, `target_id`),
  INDEX `idx_reporter` (`reporter_id`),
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户举报表';
