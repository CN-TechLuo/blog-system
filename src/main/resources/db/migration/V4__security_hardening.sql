-- ============================================================
-- V4：邮箱唯一约束（配合"邮箱作为密码找回唯一标识"）
--
-- 背景：旧实现里 user.email 只有普通索引，一个邮箱可绑定多个账号；
-- 而找回密码按邮箱定位账号，多账号时无法判断该重置谁（会重置到他人账号）。
-- 本迁移先收敛历史重复数据，再加唯一索引。
--
-- 收敛策略：同一邮箱只保留最早注册（id 最小）的账号绑定，
-- 其余账号的 email 置空（用户可重新绑定自己的邮箱），不删除任何账号。
-- ============================================================

UPDATE `user` u
JOIN (
    SELECT email, MIN(id) AS keep_id
    FROM `user`
    WHERE email IS NOT NULL AND email <> ''
    GROUP BY email
    HAVING COUNT(*) > 1
) dup ON u.email = dup.email AND u.id <> dup.keep_id
SET u.email = NULL;

-- MySQL 的唯一索引允许多个 NULL，所以未绑定邮箱的账号不受影响
ALTER TABLE `user` ADD UNIQUE INDEX `uk_email` (`email`);
