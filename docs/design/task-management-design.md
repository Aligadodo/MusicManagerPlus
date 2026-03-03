# 任务管理系统设计文档

## 概述

任务管理系统是MusicManagerPlus的核心功能模块，负责管理文件处理任务的创建、执行、监控和生命周期管理。

## 系统架构

### 核心组件

```
┌─────────────────────────────────────────────────────────────┐
│                     TaskController                        │
│                   (REST API 层)                          │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ├───────────────────────────────────────┐
                   │                                   │
                   ▼                                   ▼
┌──────────────────────────────┐    ┌──────────────────────────────┐
│   TaskExecutionService      │    │   TaskStorageService       │
│   (任务执行服务)            │    │   (任务存储服务)            │
├──────────────────────────────┤    ├──────────────────────────────┤
│ - 创建任务                 │    │ - 保存任务信息             │
│ - 执行扫描                 │    │ - 加载任务信息             │
│ - 执行预览                 │    │ - 保存扫描数据             │
│ - 执行任务                 │    │ - 保存预览数据             │
│ - 取消任务                 │    │ - 保存执行数据             │
│ - 重启任务                 │    │ - 保存统计信息             │
└──────────┬───────────────────┘    └──────────┬───────────────────┘
           │                                   │
           └─────────────────┬─────────────────┘
                             │
                             ▼
                   ┌──────────────────────────────┐
                   │   TaskRegistry             │
                   │   (任务注册中心)            │
                   ├──────────────────────────────┤
                   │ - 任务注册                 │
                   │ - 状态管理                 │
                   │ - 生命周期管理             │
                   └──────────────────────────────┘
```

### 数据流

```
用户请求 → TaskController → TaskExecutionService → TaskStorageService
                                        ↓
                                  TaskRegistry
                                        ↓
                                  StrategyService
                                        ↓
                                  文件系统/数据库
```

## 任务生命周期

### 任务状态转换

```
CREATED → SCANNING → SCANNED → PREVIEWING → PREVIEWED → EXECUTING → COMPLETED
   ↓          ↓           ↓           ↓            ↓           ↓
CANCELLED  FAILED      FAILED      FAILED       FAILED      FAILED
```

### 状态说明

| 状态 | 说明 | 可执行操作 |
|------|------|-----------|
| CREATED | 任务已创建，等待执行 | 扫描、删除 |
| SCANNING | 正在扫描文件 | 取消 |
| SCANNED | 扫描完成 | 预览、重新扫描、删除 |
| PREVIEWING | 正在预览分析 | 取消 |
| PREVIEWED | 预览完成 | 执行、重新预览、删除 |
| EXECUTING | 正在执行任务 | 取消 |
| COMPLETED | 任务执行完成 | 重新执行、删除 |
| FAILED | 任务执行失败 | 重试、删除 |
| CANCELLED | 任务已取消 | 重新执行、删除 |
| SKIP | 跳过执行（无数据） | 重新执行、删除 |

### 阶段状态说明

#### 扫描阶段状态
- **PENDING**: 等待扫描
- **RUNNING**: 正在扫描
- **COMPLETED**: 扫描完成
- **FAILED**: 扫描失败
- **SKIP**: 跳过扫描（无源目录）

#### 预览阶段状态
- **PENDING**: 等待预览
- **RUNNING**: 正在预览
- **COMPLETED**: 预览完成
- **FAILED**: 预览失败
- **SKIP**: 跳过预览（扫描到0个文件）

#### 执行阶段状态
- **PENDING**: 等待执行
- **RUNNING**: 正在执行
- **COMPLETED**: 执行完成
- **FAILED**: 执行失败
- **SKIP**: 跳过执行（扫描到0个文件）

## 任务执行流程

### 三阶段执行模型

#### 1. 扫描阶段 (Scan Stage)

**目标**: 扫描源目录，收集文件信息

**流程**:
```
开始扫描 → 遍历目录 → 过滤文件 → 收集文件信息 → 保存扫描数据 → 更新统计信息 → 完成
```

**关键功能**:
- 递归目录遍历
- 文件类型过滤
- 文件大小限制
- 并行文件扫描
- 流式数据写入

**输出**:
- 扫描数据文件: `{taskId}/scan/data.json`
- 扫描统计信息: `{taskId}/scan/statistics.json`

#### 2. 预览阶段 (Preview Stage)

**目标**: 分析文件变更，生成预览结果

**流程**:
```
开始预览 → 读取扫描数据 → 应用策略 → 分析变更 → 生成预览结果 → 保存预览数据 → 完成
```

**关键功能**:
- 变更检测
- 策略应用
- 变更记录生成
- 预览限制控制
- 并行处理

**输出**:
- 预览数据文件: `{taskId}/preview/data.json`
- 预览统计信息: `{taskId}/preview/statistics.json`
- 变更记录: 数据库 change_record 表

#### 3. 执行阶段 (Execution Stage)

**目标**: 执行文件操作，应用变更

**流程**:
```
开始执行 → 读取预览数据 → 执行文件操作 → 更新进度 → 记录结果 → 保存执行数据 → 完成
```

**关键功能**:
- 文件操作执行
- 进度跟踪
- 错误处理
- 重试机制
- 并行执行

**输出**:
- 执行数据文件: `{taskId}/execution/data.json`
- 执行统计信息: `{taskId}/execution/statistics.json`
- 操作日志: 数据库 task_operation_log 表

## 任务数据结构

### TaskInfo

```java
public class TaskInfo {
    private String taskId;              // 任务ID
    private String taskName;            // 任务名称
    private long createdAt;             // 创建时间
    private long updatedAt;             // 更新时间
    private TaskStatus status;          // 任务状态
    private String currentStage;         // 当前阶段
    private double progress;            // 整体进度
    private String message;             // 任务消息
    private String configSnapshotId;     // 配置快照ID
    private TaskConfigSnapshot configSnapshot; // 配置快照
    private TaskStages stages;         // 任务阶段信息
}
```

### TaskStages

```java
public class TaskStages {
    private ScanStage scan;           // 扫描阶段
    private PreviewStage preview;      // 预览阶段
    private ExecutionStage execution;    // 执行阶段
}
```

### ScanStage

```java
public class ScanStage {
    private String status;             // 状态: PENDING, RUNNING, COMPLETED, FAILED, SKIP
    private int totalFiles;           // 总文件数
    private Long totalSize;            // 总大小（Long类型，可为null）
    private Long scanStartTime;        // 扫描开始时间（Long类型，可为null）
    private Long scanEndTime;          // 扫描结束时间（Long类型，可为null）
    private Long scanDuration;         // 扫描持续时间（Long类型，可为null）
    private Map<String, Integer> fileTypeStats; // 文件类型统计
}
```

### PreviewStage

```java
public class PreviewStage {
    private String status;             // 状态: PENDING, RUNNING, COMPLETED, FAILED, SKIP
    private int totalFiles;           // 总文件数
    private int processedFiles;        // 已处理文件数
    private int changedFiles;         // 变更文件数
    private int unchangedFiles;       // 未变更文件数
    private Long previewStartTime;     // 预览开始时间（Long类型，可为null）
    private Long previewEndTime;       // 预览结束时间（Long类型，可为null）
    private Long previewDuration;      // 预览持续时间（Long类型，可为null）
}
```

### ExecutionStage

```java
public class ExecutionStage {
    private String status;             // 状态: PENDING, RUNNING, COMPLETED, FAILED, SKIP
    private int executionCount;       // 执行次数
    private int totalFiles;           // 总文件数
    private int processedFiles;        // 已处理文件数
    private int successCount;         // 成功数
    private int failedCount;          // 失败数
    private int skippedCount;         // 跳过数
    private Long executionStartTime;   // 执行开始时间（Long类型，可为null）
    private Long executionEndTime;     // 执行结束时间（Long类型，可为null）
    private Long executionDuration;    // 执行持续时间（Long类型，可为null）
}
```

## 配置快照管理

### 快照创建时机

- 任务创建时自动创建配置快照
- 相同配置复用现有快照
- 配置变更时创建新快照

### 快照结构

```java
public class TaskConfigSnapshot {
    private String snapshotId;         // 快照ID
    private String snapshotName;       // 快照名称
    private String snapshotType;       // 快照类型: TASK, TEMPLATE
    private Map<String, Object> configData; // 配置数据
    private String description;        // 描述
    private boolean isTemplate;       // 是否为模板
    private long createdAt;          // 创建时间
    private long updatedAt;          // 更新时间
    private String createdBy;        // 创建者
}
```

### 快照复用机制

1. 计算配置的MD5哈希值
2. 查询数据库中是否存在相同哈希的快照
3. 如果存在，复用现有快照
4. 如果不存在，创建新快照

## 并发控制

### 线程池配置

```java
// 任务执行线程池
ExecutorService taskExecutor = Executors.newFixedThreadPool(5);

// 文件处理线程池
ExecutorService processingExecutor = Executors.newFixedThreadPool(10);

// 数据写入线程池
ExecutorService writeExecutor = Executors.newFixedThreadPool(5);
```

### 并发安全

- 使用ConcurrentHashMap管理运行中的任务
- 使用BlockingQueue实现流式数据写入
- 使用AtomicInteger进行原子计数
- 使用ReentrantLock保护关键资源

## 错误处理

### 错误类型

1. **文件系统错误**: 文件不存在、权限不足、磁盘空间不足
2. **策略执行错误**: 策略配置错误、策略执行失败
3. **并发错误**: 任务冲突、资源竞争
4. **数据库错误**: 连接失败、查询失败、事务失败

### 错误处理策略

1. **重试机制**: 对于临时性错误，自动重试
2. **降级处理**: 对于非关键错误，继续执行
3. **任务取消**: 对于严重错误，取消任务
4. **错误记录**: 记录错误日志和操作日志

## 性能优化

### 流式处理

- 扫描数据流式写入
- 预览数据流式处理
- 执行结果流式输出

### 批量操作

- 批量数据库操作
- 批量文件操作
- 批量网络请求

### 缓存机制

- 配置快照缓存
- 文件信息缓存
- 策略结果缓存

## 监控和日志

### 任务监控

- 任务状态实时更新
- 进度信息实时推送
- 错误信息实时通知

### 日志记录

- 任务创建日志
- 任务执行日志
- 操作日志
- 错误日志

## API接口

### 任务管理接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/tasks | POST | 创建任务 |
| /api/tasks | GET | 获取任务列表 |
| /api/tasks/{taskId} | GET | 获取任务详情 |
| /api/tasks/{taskId} | DELETE | 删除任务 |
| /api/tasks/{taskId}/scan | POST | 执行扫描 |
| /api/tasks/{taskId}/preview | POST | 执行预览 |
| /api/tasks/{taskId}/execute | POST | 执行任务 |
| /api/tasks/{taskId}/cancel | POST | 取消任务 |
| /api/tasks/{taskId}/restart | POST | 重启任务 |

### 任务数据接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/tasks/{taskId}/scan/data | GET | 获取扫描数据 |
| /api/tasks/{taskId}/scan/statistics | GET | 获取扫描统计 |
| /api/tasks/{taskId}/preview/data | GET | 获取预览数据 |
| /api/tasks/{taskId}/preview/statistics | GET | 获取预览统计 |
| /api/tasks/{taskId}/execution/data | GET | 获取执行数据 |
| /api/tasks/{taskId}/execution/statistics | GET | 获取执行统计 |
| /api/tasks/{taskId}/logs | GET | 获取任务日志 |

## 数据库设计

### task_info 表

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
```

### task_stage 表

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
```

### task_operation_log 表

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
```

## 扩展性设计

### 插件化策略

- 策略接口标准化
- 策略动态加载
- 策略配置管理

### 分布式支持

- 任务分发机制
- 状态同步机制
- 结果聚合机制

### 云存储支持

- 云存储适配器
- 云文件操作
- 云数据同步

## 安全性

### 权限控制

- 文件访问权限
- 任务操作权限
- 配置修改权限

### 数据安全

- 敏感信息加密
- 操作日志审计
- 数据备份机制

## 总结

任务管理系统是MusicManagerPlus的核心模块，提供了完整的任务生命周期管理、三阶段执行模型、配置快照管理、并发控制和错误处理等功能。系统设计注重性能优化、可扩展性和安全性，为文件处理任务提供了稳定可靠的基础设施。
