-- V1.0.0__init_lifetracker_tables.sql
-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt哈希密码',
    nickname VARCHAR(50) NOT NULL COMMENT '用户昵称',
    avatar VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 自定义习惯表
CREATE TABLE IF NOT EXISTS user_habit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(100) NOT NULL COMMENT '习惯名称',
    icon VARCHAR(50) DEFAULT '🔥' COMMENT 'Emoji/图标',
    color VARCHAR(20) DEFAULT '#10B981' COMMENT '主题色',
    target_days INT DEFAULT 7 COMMENT '每周目标天数',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除(0否 1是)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 习惯打卡流水表
CREATE TABLE IF NOT EXISTS habit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    habit_id BIGINT NOT NULL COMMENT '习惯ID',
    log_date DATE NOT NULL COMMENT '打卡自然日(YYYY-MM-DD)',
    score INT DEFAULT 1 COMMENT '打卡得分权重',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_habit_date (user_id, habit_id, log_date),
    INDEX idx_user_date (user_id, log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 24小时时间块表
CREATE TABLE IF NOT EXISTS user_timeblock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    block_index INT NOT NULL COMMENT '时间块索引 0~47',
    category VARCHAR(50) NOT NULL COMMENT '分类(WORK/STUDY/SPORT/REST/SLEEP)',
    note VARCHAR(255) DEFAULT '' COMMENT '备注说明',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date_block (user_id, record_date, block_index),
    INDEX idx_user_record (user_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
