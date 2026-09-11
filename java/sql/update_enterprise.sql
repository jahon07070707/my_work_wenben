-- Enterprise features tables (run if DB already exists)
CREATE TABLE IF NOT EXISTS sentiment_alert (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_type  VARCHAR(30)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    level       VARCHAR(20)  DEFAULT 'warning',
    status      TINYINT      DEFAULT 0,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created (created_at)
);

CREATE TABLE IF NOT EXISTS llm_report (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_type VARCHAR(30)  DEFAULT 'daily',
    content     TEXT         NOT NULL,
    created_by  VARCHAR(50),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);
