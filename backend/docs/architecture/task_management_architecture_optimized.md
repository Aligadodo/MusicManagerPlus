# 任务管理架构优化设计文档

## 1. 文件存储结构优化

### 1.1 目录结构设计

```
~/.MusicManagerPlus/tasks/
└── {taskId}/                          # 任务ID作为根目录
    ├── config.json                    # 配置快照（单个小文件）
    ├── task.log                       # 执行日志（流式写入）
    ├── statistics.json                 # 统计信息（定时更新）
    ├── scan/                         # 文件扫描结果
    │   ├── data.json                 # 扫描数据（每行一条JSON）
    │   └── statistics.json          # 扫描统计
    ├── preview/                       # 预览分析结果
    │   ├── data.json                 # 预览数据（每行一条JSON）
    │   └── statistics.json          # 预览统计
    └── execution/                     # 执行结果（支持多次执行）
        ├── execution_001/            # 第1次执行
        │   ├── data.json             # 执行数据（每行一条JSON）
        │   └── statistics.json      # 执行统计
        ├── execution_002/            # 第2次执行
        │   ├── data.json
        │   └── statistics.json
        └── ...
```

### 1.2 文件命名规则

- **配置快照**：`config.json` - 单个小文件，包含完整配置
- **执行日志**：`task.log` - 流式写入，包含详细日志
- **统计信息**：`statistics.json` - 定时更新，包含各阶段统计
- **数据文件**：`data.json` - 每行一条JSON，避免大文件
- **执行目录**：`execution_001`, `execution_002`, ... - 支持多次执行

## 2. 数据与统计信息分离存储

### 2.1 存储原则

**数据文件（data.json）**：
- 每行一条JSON记录
- 只包含记录本身的数据
- 不包含统计信息

**统计文件（statistics.json）**：
- 单个小文件
- 包含该阶段的统计信息
- 定时或定量更新

### 2.2 数据文件格式

**scan/data.json**：
```json
{"filePath":"/path/to/file1.mp3","fileName":"file1.mp3","fileSize":1024,"lastModified":1234567890}
{"filePath":"/path/to/file2.mp3","fileName":"file2.mp3","fileSize":2048,"lastModified":1234567891}
```

**preview/data.json**：
```json
{"originalName":"file1.mp3","newName":"file1_01.mp3","operationType":"RENAME","changed":true,"extraParams":{"reason":"序号补零"}}
{"originalName":"file2.mp3","newName":"file2_02.mp3","operationType":"RENAME","changed":true,"extraParams":{"reason":"序号补零"}}
```

**execution/execution_001/data.json**：
```json
{"originalName":"file1.mp3","newName":"file1_01.mp3","operationType":"RENAME","status":"SUCCESS","executionTime":1234567890,"errorMessage":null,"retryCount":0}
{"originalName":"file2.mp3","newName":"file2_02.mp3","operationType":"RENAME","status":"FAILED","executionTime":1234567891,"errorMessage":"文件不存在","retryCount":0}
```

### 2.3 统计文件格式

**scan/statistics.json**：
```json
{
  "totalFiles": 100,
  "totalSize": 1024000,
  "scanStartTime": 1234567890,
  "scanEndTime": 1234567990,
  "scanDuration": 1000,
  "fileTypeStats": {
    "mp3": 80,
    "flac": 20
  }
}
```

**preview/statistics.json**：
```json
{
  "totalFiles": 100,
  "processedFiles": 100,
  "changedFiles": 85,
  "unchangedFiles": 15,
  "operationStats": {
    "RENAME": 50,
    "MOVE": 30,
    "DELETE": 5
  },
  "previewStartTime": 1234568000,
  "previewEndTime": 1234569000,
  "previewDuration": 1000
}
```

**execution/execution_001/statistics.json**：
```json
{
  "totalFiles": 85,
  "processedFiles": 85,
  "successCount": 80,
  "failedCount": 3,
  "skippedCount": 2,
  "operationStats": {
    "RENAME": {"SUCCESS": 45, "FAILED": 3, "SKIPPED": 2},
    "MOVE": {"SUCCESS": 30, "FAILED": 0, "SKIPPED": 0}
  },
  "executionStartTime": 1234569000,
  "executionEndTime": 1234570000,
  "executionDuration": 1000
}
```

## 3. 任务执行机制设计

### 3.1 任务生命周期

```
创建任务 → 文件扫描（仅一次） → 预览分析（仅一次） → 执行（可多次）
```

### 3.2 各阶段特性

| 阶段 | 执行次数 | 可重复 | 说明 |
|------|----------|--------|------|
| 文件扫描 | 1次 | 否 | 扫描源目录，生成原始文件列表 |
| 预览分析 | 1次 | 否 | 基于扫描结果和配置分析变更 |
| 执行 | N次 | 是 | 基于预览结果执行，可重复执行 |

### 3.3 执行次数管理

- **第1次执行**：`execution_001/`
- **第2次执行**：`execution_002/`
- **第N次执行**：`execution_NNN/`

**执行场景**：
1. **全量执行**：基于所有预览结果执行
2. **部分执行**：基于选中的预览结果执行
3. **重试失败**：基于失败的记录重新执行
4. **重新执行**：基于上一次执行结果重新执行

## 4. 流式处理机制

### 4.1 流式读取

**原则**：
- 从前一个阶段的结果文件流式读取
- 逐条处理，避免一次性加载大文件
- 处理完一条立即写入下一阶段文件

**实现**：
```java
try (BufferedReader reader = Files.newBufferedReader(prevResultFile);
     BufferedWriter writer = Files.newBufferedWriter(nextResultFile)) {
    String line;
    while ((line = reader.readLine()) != null) {
        // 处理单条记录
        String processedLine = processRecord(line);
        // 立即写入
        writer.write(processedLine);
        writer.newLine();
    }
}
```

### 4.2 流式写入队列

**多线程写入保护**：
```java
private final BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>(1000);
private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

public void writeRecord(String record) {
    writeQueue.offer(record);
}

// 单线程写入器
writeExecutor.submit(() -> {
    try (BufferedWriter writer = Files.newBufferedWriter(dataFile)) {
        String record;
        while ((record = writeQueue.poll()) != null) {
            writer.write(record);
            writer.newLine();
        }
    }
});
```

### 4.3 流式处理优势

1. **内存友好**：不一次性加载大文件到内存
2. **实时性**：处理完一条立即写入，用户可以实时看到结果
3. **可恢复**：程序崩溃后可以从已写入的文件恢复
4. **性能好**：减少IO操作，提高处理速度

## 5. 统计信息更新策略

### 5.1 更新触发条件

**定时更新**：
- 每处理100条记录更新一次
- 或者每隔5秒更新一次

**定量更新**：
- 每处理10%的记录更新一次
- 或者每处理1000条记录更新一次

**阶段完成更新**：
- 阶段开始时更新初始状态
- 阶段完成时更新最终状态

### 5.2 统计信息结构

**任务级统计（statistics.json）**：
```json
{
  "taskId": "task-1234567890",
  "currentStage": "EXECUTION",
  "stages": {
    "SCAN": {
      "status": "COMPLETED",
      "totalFiles": 100,
      "startTime": 1234567890,
      "endTime": 1234567990,
      "duration": 1000
    },
    "PREVIEW": {
      "status": "COMPLETED",
      "totalFiles": 100,
      "changedFiles": 85,
      "startTime": 1234568000,
      "endTime": 1234569000,
      "duration": 1000
    },
    "EXECUTION": {
      "status": "RUNNING",
      "executionCount": 1,
      "currentExecution": "execution_001",
      "totalFiles": 85,
      "processedFiles": 42,
      "successCount": 40,
      "failedCount": 2,
      "startTime": 1234569000,
      "progress": 0.5
    }
  }
}
```

## 6. 多阶段结果展示

### 6.1 任务详情页面结构

```
任务详情页面
├── 任务信息卡片
│   ├── 任务ID
│   ├── 创建时间
│   ├── 当前阶段
│   └── 整体进度
├── 配置快照卡片（可折叠）
│   ├── 源目录配置
│   ├── 流水线配置
│   └── 全局参数配置
├── 文件扫描结果卡片（可折叠）
│   ├── 统计信息
│   └── 文件列表（分页）
├── 预览分析结果卡片（可折叠）
│   ├── 统计信息
│   └── 变更记录列表（分页）
└── 执行结果卡片（可折叠）
    ├── 执行历史（Tab页）
    │   ├── 第1次执行
    │   ├── 第2次执行
    │   └── ...
    └── 当前执行结果列表（分页）
```

### 6.2 列表展示优化

**分页加载**：
- 默认每页显示50条记录
- 支持滚动加载更多
- 避免一次性加载大量数据

**虚拟滚动**：
- 只渲染可见区域的记录
- 滚动时动态加载
- 提高大数据量下的性能

**字段精简**：
- 列表只显示关键字段
- 详细信息通过点击展开或右键菜单查看

## 7. 右键菜单操作

### 7.1 记录级别右键菜单

**文件扫描记录右键菜单**：
```
右键菜单
├── 打开文件
├── 在文件管理器中显示
├── 复制文件路径
├── 查看文件信息
└── 从预览中排除
```

**预览记录右键菜单**：
```
右键菜单
├── 打开原始文件
├── 在文件管理器中显示
├── 复制文件路径
├── 查看变更详情
├── 从执行中排除
└── 强制执行此记录
```

**执行记录右键菜单**：
```
右键菜单
├── 打开原始文件
├── 打开目标文件
├── 在文件管理器中显示
├── 复制文件路径
├── 查看执行详情
├── 查看错误日志
├── 重试此记录
├── 从下次执行中排除
└── 撤销此操作
```

### 7.2 批量操作菜单

**批量选择操作**：
```
批量操作菜单
├── 全选
├── 反选
├── 清除选择
├── ───────────
├── 执行选中记录
├── 从执行中排除
├── 导出选中记录
└── 删除选中记录
```

## 8. 任务事务机制

### 8.1 事务概念

**任务事务**：
- 一个任务包含多个阶段
- 每个阶段可以独立执行
- 执行阶段可以多次重复

### 8.2 事务状态管理

**任务状态**：
- `CREATED`：任务已创建
- `SCANNING`：正在扫描文件
- `PREVIEWING`：正在预览分析
- `EXECUTING`：正在执行
- `COMPLETED`：执行完成
- `FAILED`：执行失败
- `CANCELLED`：已取消

**阶段状态**：
- `PENDING`：等待执行
- `RUNNING`：正在执行
- `COMPLETED`：执行完成
- `FAILED`：执行失败
- `SKIPPED`：已跳过

### 8.3 事务恢复机制

**断点恢复**：
- 程序崩溃后可以从已写入的文件恢复
- 检查各阶段的完成状态
- 从断点继续执行

**进度恢复**：
- 读取统计文件获取当前进度
- 从进度点继续处理
- 避免重复处理已完成的记录

## 9. API接口设计

### 9.1 任务管理API

```
POST   /api/tasks                        # 创建任务
GET    /api/tasks                        # 获取任务列表
GET    /api/tasks/{id}                  # 获取任务详情
DELETE /api/tasks/{id}                  # 删除任务
POST   /api/tasks/{id}/cancel           # 取消任务
GET    /api/tasks/{id}/progress         # 获取任务进度
```

### 9.2 阶段操作API

```
POST   /api/tasks/{id}/scan            # 执行文件扫描
POST   /api/tasks/{id}/preview         # 执行预览分析
POST   /api/tasks/{id}/execute         # 执行任务
POST   /api/tasks/{id}/execute/selected # 执行选中的记录
POST   /api/tasks/{id}/retry-failed   # 重试失败的记录
```

### 9.3 结果查询API

```
GET    /api/tasks/{id}/scan/data              # 获取扫描数据（分页）
GET    /api/tasks/{id}/scan/statistics        # 获取扫描统计
GET    /api/tasks/{id}/preview/data          # 获取预览数据（分页）
GET    /api/tasks/{id}/preview/statistics    # 获取预览统计
GET    /api/tasks/{id}/execution/{num}/data # 获取执行数据（分页）
GET    /api/tasks/{id}/execution/{num}/statistics # 获取执行统计
GET    /api/tasks/{id}/execution/list        # 获取所有执行历史
```

### 9.4 记录操作API

```
GET    /api/tasks/{id}/records/{recordId}    # 获取单条记录详情
POST   /api/tasks/{id}/records/{recordId}/retry # 重试单条记录
POST   /api/tasks/{id}/records/{recordId}/exclude # 从执行中排除
POST   /api/tasks/{id}/records/batch-execute # 批量执行记录
```

## 10. 性能优化策略

### 10.1 文件IO优化

**批量写入**：
- 使用BufferedWriter批量写入
- 设置合适的缓冲区大小（如8KB）
- 减少磁盘IO次数

**异步写入**：
- 使用队列缓冲写入请求
- 单线程串行化写入
- 避免多线程写入冲突

### 10.2 内存优化

**流式处理**：
- 不一次性加载大文件到内存
- 使用BufferedReader逐行读取
- 处理完一条立即释放内存

**对象池**：
- 重用对象减少GC压力
- 使用软引用缓存常用对象
- 及时释放不再使用的对象

### 10.3 查询优化

**索引支持**：
- 为常用查询字段建立索引
- 支持快速过滤和搜索
- 避免全表扫描

**分页查询**：
- 限制每次查询的记录数
- 支持游标分页
- 减少网络传输量

## 11. 错误处理和恢复

### 11.1 错误分类

**可恢复错误**：
- 文件暂时被占用
- 网络暂时不可用
- 磁盘空间不足

**不可恢复错误**：
- 文件权限不足
- 文件损坏
- 配置错误

### 11.2 恢复策略

**自动重试**：
- 可恢复错误自动重试3次
- 每次重试间隔递增（1s, 2s, 4s）
- 重试失败后标记为失败

**手动重试**：
- 用户可以手动重试失败的记录
- 支持批量重试
- 记录重试历史

## 12. 日志记录

### 12.1 日志级别

**INFO**：正常操作日志
**WARN**：警告信息（如跳过某些记录）
**ERROR**：错误信息
**DEBUG**：调试信息（仅在开发环境）

### 12.2 日志格式

```
[2024-02-14 16:00:00.123] [INFO] [task-1234567890] [SCAN] 开始扫描文件: /path/to/file1.mp3
[2024-02-14 16:00:00.234] [INFO] [task-1234567890] [SCAN] 扫描完成: file1.mp3, 大小: 1024
[2024-02-14 16:00:00.345] [ERROR] [task-1234567890] [EXECUTION] 执行失败: file2.mp3, 错误: 文件不存在
```

### 12.3 日志轮转

**按大小轮转**：
- 单个日志文件最大10MB
- 超过大小后创建新文件
- 保留最近5个日志文件

**按时间轮转**：
- 每天创建一个新日志文件
- 保留最近30天的日志文件
