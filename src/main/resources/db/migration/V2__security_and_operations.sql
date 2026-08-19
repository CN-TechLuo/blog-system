-- ============================================================
-- V2: 企业级安全/运营加固（JWT 黑名单 + AI 用量 + 登录锁定持久化）
-- 说明：Flyway 保证本脚本只执行一次；存量库通过 baseline 跳过 V1 后执行本脚本
-- ============================================================

-- 1. JWT 黑名单（登出吊销）
CREATE TABLE IF NOT EXISTS `token_blacklist` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `jti` VARCHAR(64) NOT NULL,
  `expire_time` DATETIME NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE INDEX `uk_jti` (`jti`),
  INDEX `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='JWT 黑名单';

-- 2. AI 调用用量记录
CREATE TABLE IF NOT EXISTS `ai_usage` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `api_type` VARCHAR(32) NOT NULL,
  `input_chars` INT DEFAULT 0,
  `output_chars` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_time` (`user_id`, `create_time`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用用量记录';

-- 3. 登录失败锁定持久化（重启不失效）
ALTER TABLE `user` ADD COLUMN `failed_attempts` INT DEFAULT 0 NOT NULL AFTER `role`;
ALTER TABLE `user` ADD COLUMN `locked_until` DATETIME DEFAULT NULL AFTER `failed_attempts`;
