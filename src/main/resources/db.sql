-- 创建数据库 ideafly
CREATE DATABASE ideafly DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库 ideafly
USE ideafly;
CREATE TABLE users (
                       id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID，主键，自增',
                       username VARCHAR(50)  COMMENT '用户名，唯一',
                       email VARCHAR(100)  COMMENT '邮箱，唯一，用于登录和找回密码',
                       mobile VARCHAR(20)  COMMENT '手机号，唯一，用于登录和短信通知',
                       password_hash VARCHAR(255)  COMMENT '密码哈希值，存储加密后的密码',
                       avatar_url VARCHAR(255) COMMENT '头像URL',
                       nickname VARCHAR(50) COMMENT '昵称',
                       real_name VARCHAR(50) COMMENT '真实姓名',
                       gender smallint COMMENT '性别，枚举类型：1 (男), 2 (女), 0 (其他)',
                       location VARCHAR(100) COMMENT '所在地',
                       personal_bio TEXT COMMENT '个人简介',
                       website_url VARCHAR(255) COMMENT '个人网站URL',
                       wechat_id VARCHAR(50)  COMMENT '微信号，唯一',
                       github_id VARCHAR(50)  COMMENT 'GitHub ID，唯一',
                       registration_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间，默认当前时间戳',
                       last_login_time datetime COMMENT '最后登录时间',
                       account_status smallint DEFAULT 1 COMMENT '账号状态，枚举类型：1 (激活), 2 (未激活), 3 (封禁), 0 (已删除)，默认 1',
                       created_at datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，默认当前时间戳',
                       updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间，自动更新为最新时间戳'
) COMMENT='用户基础信息表，存储用户的基本信息，例如用户名、密码、个人资料等';