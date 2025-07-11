-- 创建数据库 ideafly
CREATE DATABASE ideafly DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库 ideafly
USE ideafly;
CREATE TABLE users (
                       id CHAR(36) PRIMARY KEY COMMENT '用户ID，主键，UUID',
                       username VARCHAR(50)  COMMENT '用户名，唯一',
                       email VARCHAR(100)  COMMENT '邮箱，唯一，用于登录和找回密码',
                       mobile VARCHAR(20)  COMMENT '手机号，唯一，用于登录和短信通知',
                       password_hash VARCHAR(255)  COMMENT '密码哈希值，存储加密后的密码',
                       avatar VARCHAR(255) COMMENT '头像URL',
                       nickname VARCHAR(50) COMMENT '昵称',
                       first_name VARCHAR(50) COMMENT '名字',
                       last_name VARCHAR(50) COMMENT '姓氏',
                       gender smallint COMMENT '性别，枚举类型：1 (男), 2 (女), 0 (其他)',
                       location VARCHAR(100) COMMENT '所在地',
                       bio TEXT COMMENT '个人简介',
                       website VARCHAR(255) COMMENT '个人网站URL',
                       wechat_id VARCHAR(50)  COMMENT '微信号，唯一',
                       telegram_id VARCHAR(50)  COMMENT 'Telegram ID，唯一',
                       telegram_username VARCHAR(50) COMMENT 'Telegram用户名',
                       status smallint DEFAULT 1 COMMENT '账号状态，枚举类型：1 (激活), 0 (已删除)，默认 1',
                       role smallint DEFAULT 0 COMMENT '用户角色，0普通用户，1管理员，默认0',
                       created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间戳',
                       updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新为最新时间戳'
) COMMENT='用户基础信息表，存储用户的基本信息，例如用户名、密码、个人资料等';

CREATE TABLE `jobs` (
                        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '作品ID',
                        `user_id` CHAR(36) COMMENT '发布者用户ID (关联用户表)',
                        `post_title` VARCHAR(100) NOT NULL COMMENT '作品标题',
                        `post_content` TEXT NOT NULL COMMENT '作品内容',
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        INDEX `idx_user_id` (`user_id`),
                        INDEX `idx_created_at` (`created_at`) COMMENT '用于游标分页的索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作品信息表';

CREATE TABLE `post_comments` (
                                `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
                                `job_id` INT UNSIGNED NOT NULL COMMENT '职位ID (关联 jobs 表)',
                                `user_id` CHAR(36) NOT NULL COMMENT '评论用户ID (关联用户表)',
                                `parent_comment_id` INT UNSIGNED COMMENT '父级评论ID (用于实现评论树结构)',
                                `reply_to_comment_id` INT UNSIGNED COMMENT '回复的评论ID (标识回复关系)',
                                `content` TEXT NOT NULL COMMENT '评论内容',
                                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                INDEX `idx_job_id` (`job_id`),
                                INDEX `idx_user_id` (`user_id`),
                                INDEX `idx_parent_comment_id` (`parent_comment_id`),
                                INDEX `idx_reply_to_comment_id` (`reply_to_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位评论表';

CREATE TABLE `job_likes` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    `job_id` INT UNSIGNED NOT NULL COMMENT '职位ID (关联 jobs 表)',
    `user_id` CHAR(36) NOT NULL COMMENT '点赞用户ID (关联用户表)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    `status` TINYINT DEFAULT 1 COMMENT '点赞状态: 1(有效), 0(已取消)',
    UNIQUE KEY `unique_like` (`job_id`, `user_id`) COMMENT '防止重复点赞',
    INDEX `idx_job_id` (`job_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位点赞表';

CREATE TABLE `job_favorites` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    `job_id` INT UNSIGNED NOT NULL COMMENT '职位ID (关联 jobs 表)',
    `user_id` CHAR(36) NOT NULL COMMENT '收藏用户ID (关联用户表)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `status` TINYINT DEFAULT 1 COMMENT '收藏状态: 1(有效), 0(已取消)',
    UNIQUE KEY `unique_favorite` (`job_id`, `user_id`) COMMENT '防止重复收藏',
    INDEX `idx_job_id` (`job_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位收藏表';

CREATE TABLE `user_follows` (
    `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '关注关系ID',
    `follower_id` CHAR(36) NOT NULL COMMENT '关注者用户ID (谁关注别人)',
    `followed_id` CHAR(36) NOT NULL COMMENT '被关注者用户ID (被关注的人)',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    `status` TINYINT DEFAULT 1 COMMENT '关注状态: 1(有效), 0(已取消)',
    UNIQUE KEY `unique_follow` (`follower_id`, `followed_id`) COMMENT '防止重复关注',
    INDEX `idx_follower_id` (`follower_id`),
    INDEX `idx_followed_id` (`followed_id`),
    INDEX `idx_status` (`status`),
    CONSTRAINT `chk_self_follow` CHECK (`follower_id` != `followed_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';