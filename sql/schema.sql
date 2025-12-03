-- =============================================
-- Campus Mail System - Database Schema
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS campus_mail
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE campus_mail;

-- =============================================
-- 用户表
-- =============================================
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 邮件账户表
-- =============================================
CREATE TABLE IF NOT EXISTS `mail_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `email_address` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `display_name` VARCHAR(50) DEFAULT NULL COMMENT '显示名称',
    `smtp_host` VARCHAR(100) DEFAULT NULL COMMENT 'SMTP服务器',
    `smtp_port` INT DEFAULT 25 COMMENT 'SMTP端口',
    `is_default` TINYINT DEFAULT 0 COMMENT '是否默认账户',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_mail_account_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件账户表';

-- =============================================
-- 邮件表
-- =============================================
CREATE TABLE IF NOT EXISTS `mail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '邮件ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `account_id` BIGINT DEFAULT NULL COMMENT '邮件账户ID',
    `folder` VARCHAR(20) NOT NULL DEFAULT 'inbox' COMMENT '文件夹: inbox/sent/drafts/trash',
    `from_address` VARCHAR(100) NOT NULL COMMENT '发件人地址',
    `to_address` TEXT NOT NULL COMMENT '收件人地址(多个用逗号分隔)',
    `cc_address` TEXT DEFAULT NULL COMMENT '抄送地址',
    `bcc_address` TEXT DEFAULT NULL COMMENT '密送地址',
    `subject` VARCHAR(255) DEFAULT NULL COMMENT '邮件主题',
    `content` LONGTEXT DEFAULT NULL COMMENT '邮件内容(HTML)',
    `plain_content` LONGTEXT DEFAULT NULL COMMENT '纯文本内容',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
    `is_starred` TINYINT DEFAULT 0 COMMENT '是否星标',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    `has_attachment` TINYINT DEFAULT 0 COMMENT '是否有附件',
    `priority` TINYINT DEFAULT 3 COMMENT '优先级: 1-高, 3-普通, 5-低',
    `send_time` DATETIME DEFAULT NULL COMMENT '发送时间',
    `receive_time` DATETIME DEFAULT NULL COMMENT '接收时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_folder` (`folder`),
    KEY `idx_from_address` (`from_address`),
    KEY `idx_send_time` (`send_time`),
    FULLTEXT KEY `ft_subject_content` (`subject`, `plain_content`),
    CONSTRAINT `fk_mail_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mail_account` FOREIGN KEY (`account_id`) REFERENCES `mail_account` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件表';

-- =============================================
-- 附件表
-- =============================================
CREATE TABLE IF NOT EXISTS `attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '附件ID',
    `mail_id` BIGINT DEFAULT NULL COMMENT '邮件ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_type` VARCHAR(100) DEFAULT NULL COMMENT '文件MIME类型',
    `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `storage_path` VARCHAR(500) NOT NULL COMMENT '存储路径',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_mail_id` (`mail_id`),
    CONSTRAINT `fk_attachment_mail` FOREIGN KEY (`mail_id`) REFERENCES `mail` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件表';

-- =============================================
-- 联系人表
-- =============================================
CREATE TABLE IF NOT EXISTS `contact` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱地址',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '电话号码',
    `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_email` (`email`),
    CONSTRAINT `fk_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人表';

-- =============================================
-- 插入测试数据
-- =============================================

-- 测试用户 (密码: 123456，使用BCrypt加密)
INSERT INTO `user` (`username`, `password`, `email`, `nickname`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@campus.mail', '管理员', 1),
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'zhangsan@campus.mail', '张三', 1),
('lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'lisi@campus.mail', '李四', 1);

-- 测试邮件账户
INSERT INTO `mail_account` (`user_id`, `email_address`, `display_name`, `smtp_host`, `smtp_port`, `is_default`) VALUES
(1, 'admin@campus.mail', '管理员', 'localhost', 25, 1),
(2, 'zhangsan@campus.mail', '张三', 'localhost', 25, 1),
(3, 'lisi@campus.mail', '李四', 'localhost', 25, 1);

-- 测试联系人
INSERT INTO `contact` (`user_id`, `contact_name`, `email`, `phone`, `notes`) VALUES
(1, '张三', 'zhangsan@campus.mail', '13800138001', '同事'),
(1, '李四', 'lisi@campus.mail', '13800138002', '同事'),
(2, '管理员', 'admin@campus.mail', '13800138000', '系统管理员'),
(2, '李四', 'lisi@campus.mail', '13800138002', '朋友'),
(3, '管理员', 'admin@campus.mail', '13800138000', '系统管理员'),
(3, '张三', 'zhangsan@campus.mail', '13800138001', '朋友');

-- 测试邮件
INSERT INTO `mail` (`user_id`, `account_id`, `folder`, `from_address`, `to_address`, `subject`, `content`, `plain_content`, `is_read`, `send_time`, `receive_time`) VALUES
(2, 2, 'inbox', 'admin@campus.mail', 'zhangsan@campus.mail', '欢迎使用校园邮件系统', '<h1>欢迎！</h1><p>这是您收到的第一封邮件。</p>', '欢迎！这是您收到的第一封邮件。', 0, NOW(), NOW()),
(2, 2, 'inbox', 'lisi@campus.mail', 'zhangsan@campus.mail', '周末活动通知', '<p>周末有个聚会，记得参加！</p>', '周末有个聚会，记得参加！', 0, NOW(), NOW()),
(3, 3, 'inbox', 'admin@campus.mail', 'lisi@campus.mail', '系统通知', '<p>系统将于今晚维护。</p>', '系统将于今晚维护。', 1, NOW(), NOW()),
(1, 1, 'sent', 'admin@campus.mail', 'zhangsan@campus.mail,lisi@campus.mail', '欢迎使用校园邮件系统', '<h1>欢迎！</h1><p>这是您收到的第一封邮件。</p>', '欢迎！这是您收到的第一封邮件。', 1, NOW(), NULL);

-- =============================================
-- 完成
-- =============================================
SELECT '数据库初始化完成！' AS message;
