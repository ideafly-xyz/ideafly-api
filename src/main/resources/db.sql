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
                        `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '职位ID',
                        `user_id` INT UNSIGNED COMMENT '发布者用户ID (关联用户表，如果需要)',
                        `post_title` VARCHAR(100) NOT NULL COMMENT '帖子标题',
                        `post_content` TEXT COMMENT '帖子内容',
                        `contact_info` VARCHAR(100) NOT NULL COMMENT '联系方式',
                        `recruitment_type` SMALLINT UNSIGNED COMMENT '招聘类型 (1=外包零活, 2=企业直招, 3=猎头中介, 4=员工内推, 5=组队合伙)',
                        `profession` SMALLINT UNSIGNED COMMENT '职业 (1=开发, 2=产品, 3=设计, 4=运营, 5=写作, 6=运维, 7=其它)',
                        `work_type` SMALLINT UNSIGNED COMMENT '工作方式 (1=全职坐班, 2=远程工作, 3=线上兼职, 4=同城驻场)',
                        `city` SMALLINT UNSIGNED COMMENT '城市 (1=海外, 2=北京, 3=上海, 4=广州, 5=深圳, 6=杭州, 7=成都, 8=西安, 9=厦门, 10=武汉, 11=长沙, 12=苏州, 13=郑州, 14=南京, 15=云南, 16=海南, 17=大理, 18=其他)',
                        `industry_domain` SMALLINT UNSIGNED COMMENT '领域 (1=移动互联网, 2=电子商务, 3=教育培训, 4=金融, 5=IT服务, 6=人工智能, 7=游戏, 8=文化传媒, 9=医疗健康, 10=其他)',
                        `likes` INT UNSIGNED DEFAULT 0 COMMENT '点赞数',
                        `comments` INT UNSIGNED DEFAULT 0 COMMENT '评论数',
                        `favorites` INT UNSIGNED DEFAULT 0 COMMENT '收藏数',
                        `shares` INT UNSIGNED DEFAULT 0 COMMENT '分享数',
                        `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        INDEX `idx_user_id` (`user_id`),
                        INDEX `idx_recruitment_type` (`recruitment_type`),
                        INDEX `idx_profession` (`profession`),
                        INDEX `idx_work_type` (`work_type`),
                        INDEX `idx_city` (`city`),
                        INDEX `idx_industry_domain` (`industry_domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位信息表';

CREATE TABLE `job_comments` (
                                `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
                                `job_id` INT UNSIGNED NOT NULL COMMENT '职位ID (关联 jobs 表)',
                                `user_id` INT UNSIGNED NOT NULL COMMENT '评论用户ID (关联用户表)',
                                `parent_comment_id` INT UNSIGNED COMMENT '父级评论ID (用于实现评论回复)',
                                `content` TEXT NOT NULL COMMENT '评论内容',
                                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                INDEX `idx_job_id` (`job_id`),
                                INDEX `idx_user_id` (`user_id`),
                                INDEX `idx_parent_comment_id` (`parent_comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位评论表';

CREATE TABLE `job_likes` (
                             `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
                             `job_id` INT UNSIGNED NOT NULL COMMENT '职位ID (关联 jobs 表)',
                             `user_id` INT UNSIGNED NOT NULL COMMENT '点赞用户ID (关联用户表)',
                             `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
                             INDEX `idx_job_id` (`job_id`),
                             INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位点赞表';


CREATE TABLE `job_favorites` (
                                 `id` INT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
                                 `job_id` INT UNSIGNED NOT NULL COMMENT '职位ID (关联 jobs 表)',
                                 `user_id` INT UNSIGNED NOT NULL COMMENT '收藏用户ID (关联用户表)',
                                 `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
                                 INDEX `idx_job_id` (`job_id`),
                                 INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位收藏表';