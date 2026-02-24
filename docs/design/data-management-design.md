# 数据管理系统设计文档

## 概述

数据管理系统负责管理MusicManagerPlus的所有数据持久化，包括任务信息、变更记录、操作日志、配置快照等，提供统一的数据库访问接口。

## 系统架构

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                   Service Layer                          │
│                  (服务层)                                │
├─────────────────────────────────────────────────────────────┤
│ TaskInfoService                                        │
│ TaskStageService                                       │
│ TaskOperationLogService                                │
│ ChangeRecordService                                    │
│ ConfigSnapshotService                                   │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                   Mapper Layer                           │
│                  (数据访问层)                             │
├─────────────────────────────────────────────────────────────┤
│ TaskInfoMapper                                        │
│ TaskStageMapper                                       │
│ TaskOperationLogMapper                                 │
│ ChangeRecordMapper                                    │
│ ConfigSnapshotMapper                                  │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                   MyBatis                              │
│                  (ORM框架)                               │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────────┐
│                   SQLite Database                       │
│                  (数据库)                                 │
└─────────────────────────────────────────────────────────────┘
```

## 数据库设计

### 数据库配置

- **数据库类型**: SQLite
- **连接池**: HikariCP
- **ORM框架**: MyBatis
- **自动初始化**: 是
- **外键约束**: 启用

### 连接配置

```yaml
spring:
  datasource:
    driver-class-name: org.sqlite.JDBC
    url: jdbc:sqlite:~/.MusicManagerPlus/music_manager.db
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-init-sql: PRAGMA foreign_keys = ON
```

## 数据表设计

### 1. task_info (任务信息表)

存储任务的基本信息和状态。

```sql
CREATE TABLE task_info (
    task_id TEXT PRIMARY KEY,
    task_name TEXT NOT NULL,
    status TEXT NOT NULL,
    current_stage TEXT,
    overall_progress REAL DEFAULT 0,
    message TEXT,
    config_snapshot_id TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    completed_at DATETIME,
    FOREIGN KEY (config_snapshot_id) REFERENCES config_snapshot(snapshot_id)
);

CREATE INDEX idx_task_info_status ON task_info(status);
CREATE INDEX idx_task_info_created_at ON task_info(created_at);
CREATE INDEX idx_task_info_config_snapshot ON task_info(config_snapshot_id);
```

**字段说明**:
- `task_id`: 任务唯一标识
- `task_name`: 任务名称
- `status`: 任务状态 (CREATED, SCANNING, SCANNED, PREVIEWING, PREVIEWED, EXECUTING, COMPLETED, FAILED, CANCELLED)
- `current_stage`: 当前阶段 (CREATED, SCANNING, PREVIEWING, EXECUTING)
- `overall_progress`: 整体进度 (0.0 - 1.0)
- `message`: 任务消息
- `config_snapshot_id`: 关联的配置快照ID
- `created_at`: 创建时间
- `updated_at`: 更新时间
- `completed_at`: 完成时间

### 2. task_stage (任务阶段表)

存储任务各阶段的详细信息。

```sql
CREATE TABLE task_stage (
    stage_id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL,
    stage_type TEXT NOT NULL,
    status TEXT NOT NULL,
    stage_data TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);

CREATE INDEX idx_task_stage_task_id ON task_stage(task_id);
CREATE INDEX idx_task_stage_type ON task_stage(stage_type);
```

**字段说明**:
- `stage_id`: 阶段ID
- `task_id`: 关联的任务ID
- `stage_type`: 阶段类型 (SCAN, PREVIEW, EXECUTION)
- `status`: 阶段状态 (PENDING, RUNNING, COMPLETED, FAILED)
- `stage_data`: 阶段数据 (JSON格式)
- `created_at`: 创建时间
- `updated_at`: 更新时间

### 3. task_operation_log (任务操作日志表)

记录任务的操作历史。

```sql
CREATE TABLE task_operation_log (
    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    operation_data TEXT,
    operator TEXT,
    operation_time DATETIME NOT NULL,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);

CREATE INDEX idx_task_operation_log_task_id ON task_operation_log(task_id);
CREATE INDEX idx_task_operation_log_time ON task_operation_log(operation_time);
```

**字段说明**:
- `log_id`: 日志ID
- `task_id`: 关联的任务ID
- `operation_type`: 操作类型 (CREATE, SCAN, PREVIEW, EXECUTE, CANCEL, RESTART, DELETE)
- `operation_data`: 操作数据 (JSON格式)
- `operator`: 操作者
- `operation_time`: 操作时间

### 4. change_record (变更记录表)

记录文件变更信息。

```sql
CREATE TABLE change_record (
    record_id INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_name TEXT NOT NULL,
    operation_type TEXT NOT NULL,
    operation_data TEXT,
    status TEXT NOT NULL,
    error_message TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (task_id) REFERENCES task_info(task_id) ON DELETE CASCADE
);

CREATE INDEX idx_change_record_task_id ON change_record(task_id);
CREATE INDEX idx_change_record_file_path ON change_record(file_path);
CREATE INDEX idx_change_record_operation_type ON change_record(operation_type);
CREATE INDEX idx_change_record_status ON change_record(status);
```

**字段说明**:
- `record_id`: 记录ID
- `task_id`: 关联的任务ID
- `file_path`: 文件路径
- `file_name`: 文件名
- `operation_type`: 操作类型 (RENAME, MOVE, COPY, DELETE, CONVERT, METADATA_UPDATE)
- `operation_data`: 操作数据 (JSON格式)
- `status`: 状态 (PENDING, SUCCESS, FAILED, SKIPPED)
- `error_message`: 错误消息
- `created_at`: 创建时间
- `updated_at`: 更新时间

### 5. config_snapshot (配置快照表)

存储配置快照信息。

```sql
CREATE TABLE config_snapshot (
    snapshot_id TEXT PRIMARY KEY,
    snapshot_name TEXT NOT NULL,
    snapshot_type TEXT NOT NULL,
    config_data TEXT NOT NULL,
    description TEXT,
    is_template BOOLEAN DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    created_by TEXT
);

CREATE INDEX idx_config_snapshot_type ON config_snapshot(snapshot_type);
CREATE INDEX idx_config_snapshot_is_template ON config_snapshot(is_template);
CREATE INDEX idx_config_snapshot_created_at ON config_snapshot(created_at);
```

**字段说明**:
- `snapshot_id`: 快照ID
- `snapshot_name`: 快照名称
- `snapshot_type`: 快照类型 (TASK, TEMPLATE, SYSTEM)
- `config_data`: 配置数据 (JSON格式)
- `description`: 描述
- `is_template`: 是否为模板
- `created_at`: 创建时间
- `updated_at`: 更新时间
- `created_by`: 创建者

### 6. system_config (系统配置表)

存储系统配置信息。

```sql
CREATE TABLE system_config (
    config_id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key TEXT NOT NULL UNIQUE,
    config_value TEXT,
    config_type TEXT NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE INDEX idx_system_config_key ON system_config(config_key);
```

**字段说明**:
- `config_id`: 配置ID
- `config_key`: 配置键
- `config_value`: 配置值
- `config_type`: 配置类型 (STRING, INTEGER, BOOLEAN, JSON)
- `description`: 描述
- `created_at`: 创建时间
- `updated_at`: 更新时间

## 数据访问层设计

### Mapper接口

#### TaskInfoMapper

```java
public interface TaskInfoMapper {
    int insert(TaskInfoPO taskInfo);
    int update(TaskInfoPO taskInfo);
    int deleteByTaskId(String taskId);
    TaskInfoPO selectByTaskId(String taskId);
    List<TaskInfoPO> selectAll();
    List<TaskInfoPO> selectByStatus(String status);
    List<TaskInfoPO> selectByConfigSnapshotId(String snapshotId);
}
```

#### ChangeRecordMapper

```java
public interface ChangeRecordMapper {
    int insert(ChangeRecordPO changeRecord);
    int update(ChangeRecordPO changeRecord);
    int deleteByRecordId(Integer recordId);
    int deleteByTaskId(String taskId);
    ChangeRecordPO selectByRecordId(Integer recordId);
    List<ChangeRecordPO> selectByTaskId(String taskId);
    List<ChangeRecordPO> selectByFilePath(String filePath);
    List<ChangeRecordPO> selectByOperationType(String operationType);
    List<ChangeRecordPO> selectByStatus(String status);
    int getLastInsertId();
}
```

#### ConfigSnapshotMapper

```java
public interface ConfigSnapshotMapper {
    int insert(ConfigSnapshotPO configSnapshot);
    int update(ConfigSnapshotPO configSnapshot);
    int deleteBySnapshotId(String snapshotId);
    ConfigSnapshotPO selectBySnapshotId(String snapshotId);
    List<ConfigSnapshotPO> selectAll();
    List<ConfigSnapshotPO> selectByType(String type);
    List<ConfigSnapshotPO> selectByTemplate(boolean isTemplate);
}
```

## 服务层设计

### TaskInfoService

```java
@Service
public class TaskInfoService {
    
    @Autowired
    private TaskInfoMapper taskInfoMapper;
    
    public void createTask(TaskInfoPO taskInfo);
    public void updateTask(TaskInfoPO taskInfo);
    public void deleteTask(String taskId);
    public TaskInfoPO getTask(String taskId);
    public List<TaskInfoPO> getAllTasks();
    public List<TaskInfoPO> getTasksByStatus(String status);
}
```

### ChangeRecordService

```java
@Service
public class ChangeRecordService {
    
    @Autowired
    private ChangeRecordMapper changeRecordMapper;
    
    public void createChangeRecord(ChangeRecordPO changeRecord);
    public void updateChangeRecord(ChangeRecordPO changeRecord);
    public void deleteChangeRecord(Integer recordId);
    public void deleteChangeRecordsByTaskId(String taskId);
    public ChangeRecordPO getChangeRecord(Integer recordId);
    public List<ChangeRecordPO> getChangeRecordsByTaskId(String taskId);
    public List<ChangeRecordPO> getChangeRecordsByFilePath(String filePath);
    public List<ChangeRecordPO> getChangeRecordsByOperationType(String operationType);
    public List<ChangeRecordPO> getChangeRecordsByStatus(String status);
}
```

### ConfigSnapshotService

```java
@Service
public class ConfigSnapshotService {
    
    @Autowired
    private ConfigSnapshotMapper configSnapshotMapper;
    
    public void createSnapshot(ConfigSnapshotPO configSnapshot);
    public void updateSnapshot(ConfigSnapshotPO configSnapshot);
    public void deleteSnapshot(String snapshotId);
    public ConfigSnapshotPO getSnapshot(String snapshotId);
    public List<ConfigSnapshotPO> getAllSnapshots();
    public List<ConfigSnapshotPO> getSnapshotsByType(String type);
    public List<ConfigSnapshotPO> getTemplateSnapshots();
    public TaskConfigSnapshot getOrCreateSnapshot(Map<String, Object> config);
}
```

## 事务管理

### 事务配置

```java
@Configuration
public class TransactionConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

### 事务使用

```java
@Transactional
public void createTaskWithRecords(TaskInfoPO taskInfo, List<ChangeRecordPO> records) {
    taskInfoMapper.insert(taskInfo);
    for (ChangeRecordPO record : records) {
        record.setTaskId(taskInfo.getTaskId());
        changeRecordMapper.insert(record);
    }
}
```

## 数据库初始化

### 初始化脚本

```sql
-- 创建任务信息表
CREATE TABLE IF NOT EXISTS task_info (...);

-- 创建任务阶段表
CREATE TABLE IF NOT EXISTS task_stage (...);

-- 创建任务操作日志表
CREATE TABLE IF NOT EXISTS task_operation_log (...);

-- 创建变更记录表
CREATE TABLE IF NOT EXISTS change_record (...);

-- 创建配置快照表
CREATE TABLE IF NOT EXISTS config_snapshot (...);

-- 创建系统配置表
CREATE TABLE IF NOT EXISTS system_config (...);
```

### 自动初始化

```java
@Component
public class DatabaseInitializer {
    
    @Autowired
    private DataSource dataSource;
    
    @PostConstruct
    public void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/init.sql"));
        } catch (Exception e) {
            logger.error("数据库初始化失败", e);
        }
    }
}
```

## 数据备份和恢复

### 数据库备份

```java
public void backupDatabase(String backupPath) {
    String dbPath = "~/.MusicManagerPlus/music_manager.db";
    Files.copy(Paths.get(dbPath), Paths.get(backupPath));
}
```

### 数据库恢复

```java
public void restoreDatabase(String backupPath) {
    String dbPath = "~/.MusicManagerPlus/music_manager.db";
    Files.copy(Paths.get(backupPath), Paths.get(dbPath), 
              StandardCopyOption.REPLACE_EXISTING);
}
```

## 性能优化

### 索引优化

- 为常用查询字段创建索引
- 使用复合索引优化多条件查询
- 定期分析和优化索引

### 查询优化

- 使用分页查询避免大量数据加载
- 使用批量操作减少数据库访问次数
- 使用连接池管理数据库连接

### 缓存策略

- 缓存常用配置数据
- 缓存任务状态信息
- 使用LRU缓存策略

## 数据迁移

### 版本管理

```java
public interface DatabaseMigration {
    int getVersion();
    void migrate(Connection conn) throws SQLException;
}
```

### 迁移执行

```java
public void migrateDatabase() {
    int currentVersion = getCurrentVersion();
    List<DatabaseMigration> migrations = getMigrations();
    
    for (DatabaseMigration migration : migrations) {
        if (migration.getVersion() > currentVersion) {
            migration.migrate(connection);
            updateVersion(migration.getVersion());
        }
    }
}
```

## 数据安全

### 数据加密

- 敏感字段加密存储
- 使用AES加密算法
- 密钥安全管理

### 访问控制

- 数据库访问权限控制
- SQL注入防护
- 参数化查询

### 审计日志

- 数据访问日志
- 数据修改日志
- 异常访问日志

## 总结

数据管理系统提供了完整的数据库访问接口，支持任务信息、变更记录、操作日志、配置快照等数据的持久化。系统设计注重性能优化、数据安全和可扩展性，为整个应用提供了稳定可靠的数据管理能力。
