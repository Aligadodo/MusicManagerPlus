-- 任务信息表
CREATE TABLE IF NOT EXISTS task_info (
    task_id VARCHAR(64) PRIMARY KEY,
    task_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_stage VARCHAR(32),
    overall_progress DOUBLE DEFAULT 0.0,
    message TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    completed_at DATETIME,
    config_snapshot_id VARCHAR(64)
);

-- 任务阶段表
CREATE TABLE IF NOT EXISTS task_stage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL,
    stage_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    duration BIGINT,
    total_files INTEGER DEFAULT 0,
    processed_files INTEGER DEFAULT 0,
    success_count INTEGER DEFAULT 0,
    failed_count INTEGER DEFAULT 0,
    changed_files INTEGER DEFAULT 0,
    stats_json TEXT,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);

-- 变更记录表
CREATE TABLE IF NOT EXISTS change_record (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL,
    record_id VARCHAR(64) NOT NULL,
    original_name VARCHAR(512) NOT NULL,
    new_name VARCHAR(512),
    file_path VARCHAR(1024) NOT NULL,
    new_path VARCHAR(1024),
    operation_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    changed BOOLEAN DEFAULT 0,
    selected BOOLEAN DEFAULT 0,
    fail_reason TEXT,
    extra_params TEXT,
    analyze_time DATETIME,
    execute_time DATETIME,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);

-- 任务操作日志表
CREATE TABLE IF NOT EXISTS task_operation_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    operation_stage VARCHAR(32),
    operator VARCHAR(64),
    operation_time DATETIME NOT NULL,
    operation_detail TEXT,
    result VARCHAR(32),
    error_message TEXT,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);

-- 配置快照表
CREATE TABLE IF NOT EXISTS config_snapshot (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    snapshot_name VARCHAR(255) NOT NULL,
    snapshot_type VARCHAR(32) NOT NULL,
    config_data TEXT NOT NULL,
    description TEXT,
    is_template BOOLEAN DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(64)
);

-- 配置模板表
CREATE TABLE IF NOT EXISTS config_template (
    template_id VARCHAR(64) PRIMARY KEY,
    template_name VARCHAR(255) NOT NULL,
    template_type VARCHAR(32) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    category VARCHAR(128),
    tags TEXT,
    description TEXT,
    is_default BOOLEAN DEFAULT 0,
    usage_count INTEGER DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by VARCHAR(64),
    FOREIGN KEY (snapshot_id) REFERENCES config_snapshot(snapshot_id) ON DELETE CASCADE
);

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    config_key VARCHAR(128) PRIMARY KEY,
    config_value TEXT NOT NULL,
    config_type VARCHAR(32) NOT NULL,
    description TEXT,
    category VARCHAR(64),
    is_encrypted BOOLEAN DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_task_info_status ON task_info(status);
CREATE INDEX IF NOT EXISTS idx_task_info_created_at ON task_info(created_at);
CREATE INDEX IF NOT EXISTS idx_task_info_updated_at ON task_info(updated_at);

CREATE INDEX IF NOT EXISTS idx_task_stage_task_stage ON task_stage(task_id, stage_type);
CREATE INDEX IF NOT EXISTS idx_task_stage_task_status ON task_stage(task_id, status);

CREATE INDEX IF NOT EXISTS idx_change_record_task_status ON change_record(task_id, status);
CREATE INDEX IF NOT EXISTS idx_change_record_task_operation ON change_record(task_id, operation_type);
CREATE INDEX IF NOT EXISTS idx_change_record_task_changed ON change_record(task_id, changed);
CREATE INDEX IF NOT EXISTS idx_change_record_original_name ON change_record(original_name);
CREATE INDEX IF NOT EXISTS idx_change_record_file_path ON change_record(file_path);

CREATE INDEX IF NOT EXISTS idx_task_operation_log_task_time ON task_operation_log(task_id, operation_time);
CREATE INDEX IF NOT EXISTS idx_task_operation_log_task_type ON task_operation_log(task_id, operation_type);

CREATE INDEX IF NOT EXISTS idx_config_snapshot_type ON config_snapshot(snapshot_type);
CREATE INDEX IF NOT EXISTS idx_config_snapshot_is_template ON config_snapshot(is_template);
CREATE INDEX IF NOT EXISTS idx_config_snapshot_created_at ON config_snapshot(created_at);

CREATE INDEX IF NOT EXISTS idx_config_template_type ON config_template(template_type);
CREATE INDEX IF NOT EXISTS idx_config_template_category ON config_template(category);
CREATE INDEX IF NOT EXISTS idx_config_template_is_default ON config_template(is_default);
CREATE INDEX IF NOT EXISTS idx_config_template_usage_count ON config_template(usage_count);

CREATE INDEX IF NOT EXISTS idx_system_config_category ON system_config(category);

-- 任务执行日志表
CREATE TABLE IF NOT EXISTS task_execution_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id VARCHAR(64) NOT NULL,
    timestamp BIGINT NOT NULL,
    log_level VARCHAR(16) NOT NULL,
    log_type VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    details TEXT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_task_execution_log_task_time ON task_execution_log(task_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_task_execution_log_task_level ON task_execution_log(task_id, log_level);
CREATE INDEX IF NOT EXISTS idx_task_execution_log_task_type ON task_execution_log(task_id, log_type);
