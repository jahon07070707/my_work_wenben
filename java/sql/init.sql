-- 智能文本分类与情感分析系统 - 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS text_analysis DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE text_analysis;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(50),
    role        VARCHAR(20)  DEFAULT 'user' COMMENT 'admin/user',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- 数据源配置表
CREATE TABLE IF NOT EXISTS data_source (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL COMMENT '数据源名称',
    url         VARCHAR(500) NOT NULL COMMENT '采集地址',
    source_type VARCHAR(30)  NOT NULL COMMENT 'news/comment/rss',
    category    VARCHAR(30)  COMMENT '默认分类',
    status      TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    description VARCHAR(500),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- 采集任务表
CREATE TABLE IF NOT EXISTS collection_task (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_id     BIGINT,
    status        VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/running/success/failed',
    total_count   INT         DEFAULT 0,
    success_count INT         DEFAULT 0,
    error_msg     TEXT,
    started_at    DATETIME,
    finished_at   DATETIME,
    created_at    DATETIME    DEFAULT CURRENT_TIMESTAMP
);

-- 原始文本表
CREATE TABLE IF NOT EXISTS raw_text (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_id    BIGINT,
    task_id      BIGINT,
    title        VARCHAR(500),
    content      TEXT         NOT NULL,
    author       VARCHAR(100),
    url          VARCHAR(500),
    publish_time DATETIME,
    collected_at DATETIME     DEFAULT CURRENT_TIMESTAMP,
    is_analyzed  TINYINT      DEFAULT 0 COMMENT '0未分析 1已分析'
);

-- 分析结果表
CREATE TABLE IF NOT EXISTS analysis_result (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    text_id              BIGINT       NOT NULL,
    category             VARCHAR(30)  NOT NULL COMMENT '分类标签',
    category_confidence  DECIMAL(5,4),
    sentiment            VARCHAR(20)  NOT NULL COMMENT 'positive/negative/neutral',
    sentiment_confidence DECIMAL(5,4),
    keywords             VARCHAR(500) COMMENT '关键词,逗号分隔',
    analyzed_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_text_id (text_id),
    INDEX idx_category (category),
    INDEX idx_sentiment (sentiment)
);

-- 热点话题统计表
CREATE TABLE IF NOT EXISTS hot_topic (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    keyword    VARCHAR(100) NOT NULL,
    count      INT          DEFAULT 1,
    stat_date  DATE         NOT NULL,
    UNIQUE KEY uk_keyword_date (keyword, stat_date)
);

-- 系统统计快照表
CREATE TABLE IF NOT EXISTS daily_stats (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date       DATE NOT NULL UNIQUE,
    total_texts     INT  DEFAULT 0,
    positive_count  INT  DEFAULT 0,
    negative_count  INT  DEFAULT 0,
    neutral_count   INT  DEFAULT 0,
    category_json   TEXT COMMENT '各分类数量JSON'
);

-- 初始管理员 (密码: admin123)
INSERT INTO sys_user (username, password, nickname, role) VALUES
('admin', '$2b$10$PH6B00sby891aNNi/c4n5uEc3MkPHtsMauz8M68Xj6VvtUkVAZ2YO', '系统管理员', 'admin');

-- 预置数据源
INSERT INTO data_source (name, url, source_type, category, description) VALUES
('新浪科技RSS', 'https://rss.sina.com.cn/tech/rollnews.xml', 'rss', '科技', '新浪科技频道滚动新闻'),
('新浪财经RSS', 'https://rss.sina.com.cn/finance/stock.xml', 'rss', '财经', '新浪财经股票频道'),
('新浪体育RSS', 'https://rss.sina.com.cn/sports/sportsroll.xml', 'rss', '体育', '新浪体育滚动新闻'),
('新浪娱乐RSS', 'https://rss.sina.com.cn/ent/entroll.xml', 'rss', '娱乐', '新浪娱乐滚动新闻'),
('人民网时政', 'http://www.people.com.cn/', 'news', '社会', '人民网时政新闻（示例采集）'),
('本地评论数据集', 'data/samples/comments.csv', 'comment', NULL, '预置电商/电影评论样本数据');

-- 舆情预警记录表
CREATE TABLE IF NOT EXISTS sentiment_alert (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_type  VARCHAR(30)  NOT NULL COMMENT 'negative_spike/keyword/negative_ratio',
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    level       VARCHAR(20)  DEFAULT 'warning' COMMENT 'info/warning/danger',
    status      TINYINT      DEFAULT 0 COMMENT '0未读 1已读',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created (created_at)
);

-- AI舆情报告表
CREATE TABLE IF NOT EXISTS llm_report (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_type VARCHAR(30)  DEFAULT 'daily',
    content     TEXT         NOT NULL,
    created_by  VARCHAR(50),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);
