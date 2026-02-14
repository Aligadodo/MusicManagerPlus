# 任务管理架构设计文档

## 1. 当前架构分析

### 1.1 现有问题
- **伪任务机制**：只能同时有一个任务运行，实际是单任务系统
- **无持久化**：任务结果只存储在内存中，重启后丢失
- **无快照机制**：任务执行时依赖当前配置，用户修改配置会影响运行中的任务
- **无预览结果**：预览和执行混在一起，无法分别管理
- **无进度统计**：缺少详细的进度信息和统计数据
- **无任务管理**：无法查询、检索、重试失败任务

### 1.2 当前数据流
```
用户配置 → 策略分析 → 内存中的ChangeRecord → 直接执行 → 结果丢失
```

## 2. 新架构设计

### 2.1 核心概念

#### 2.1.1 任务生命周期
```
创建 → 预览分析 → 预览完成 → 选择执行 → 执行中 → 执行完成
```

#### 2.1.2 任务类型
- **预览任务**：只分析不执行，生成预览结果
- **执行任务**：基于预览结果执行实际操作

#### 2.1.3 任务状态
- `CREATED`：任务已创建
- `PREVIEWING`：正在预览分析
- `PREVIEW_COMPLETED`：预览完成
- `EXECUTING`：正在执行
- `COMPLETED`：执行完成
- `FAILED`：执行失败
- `CANCELLED`：已取消

### 2.2 数据模型

#### 2.2.1 任务快照（TaskSnapshot）
```json
{
  "taskId": "task-1234567890",
  "taskType": "PREVIEW" | "EXECUTE",
  "createdAt": 1234567890,
  "configSnapshot": {
    "sourceDirectories": [
      {
        "path": "/path/to/dir",
        "threadCount": 4
      }
    ],
    "pipelineConfig": {
      "pipelineId": "pipeline-123",
      "name": "默认流水线",
      "items": [
        {
          "pluginId": "advanced-rename",
          "enabled": true,
          "config": {...},
          "preconditionGroups": [...]
        }
      ]
    },
    "globalSettings": {
      "maxThreads": 10,
      "timeout": 300000,
      "dryRun": false
    }
  },
  "status": "PREVIEWING",
  "progress": 0.5,
  "message": "正在分析文件..."
}
```

#### 2.2.2 预览结果（PreviewResult）
```json
{
  "taskId": "task-1234567890",
  "changeRecords": [
    {
      "originalName": "周杰伦-青花瓷.mp3",
      "newName": "周杰伦-青花瓷_01.mp3",
      "originalPath": "/path/to/周杰伦-青花瓷.mp3",
      "newPath": "/path/to/周杰伦-青花瓷_01.mp3",
      "operationType": "RENAME",
      "status": "PENDING",
      "changed": true,
      "extraParams": {
        "reason": "序号补零",
        "originalIndex": 1
      },
      "timestamp": 1234567890
    }
  ],
  "statistics": {
    "totalFiles": 100,
    "processedFiles": 100,
    "changedFiles": 85,
    "unchangedFiles": 15,
    "operationStats": {
      "RENAME": 50,
      "MOVE": 30,
      "DELETE": 5
    }
  },
  "createdAt": 1234567890,
  "completedAt": 1234567990
}
```

#### 2.2.3 执行结果（ExecutionResult）
```json
{
  "taskId": "task-1234567890",
  "previewTaskId": "task-1234567880",
  "changeRecords": [
    {
      "originalName": "周杰伦-青花瓷.mp3",
      "newName": "周杰伦-青花瓷_01.mp3",
      "originalPath": "/path/to/周杰伦-青花瓷.mp3",
      "newPath": "/path/to/周杰伦-青花瓷_01.mp3",
      "operationType": "RENAME",
      "status": "SUCCESS" | "FAILED" | "SKIPPED",
      "changed": true,
      "extraParams": {
        "reason": "序号补零",
        "originalIndex": 1
      },
      "executionTime": 1234567890,
      "errorMessage": null,
      "retryCount": 0
    }
  ],
  "statistics": {
    "totalFiles": 85,
    "processedFiles": 85,
    "successCount": 80,
    "failedCount": 3,
    "skippedCount": 2,
    "operationStats": {
      "RENAME": { "SUCCESS": 45, "FAILED": 3, "SKIPPED": 2 },
      "MOVE": { "SUCCESS": 30, "FAILED": 0, "SKIPPED": 0 }
    }
  },
  "createdAt": 1234567990,
  "startedAt": 1234568000,
  "completedAt": 1234569000,
  "duration": 1000
}
```

### 2.3 文件存储结构

```
~/.MusicManagerPlus/
├── tasks/
│   ├── snapshots/
│   │   ├── task-1234567890.json
│   │   ├── task-1234567891.json
│   │   └── ...
│   ├── previews/
│   │   ├── task-1234567890.json
│   │   ├── task-1234567891.json
│   │   └── ...
│   ├── executions/
│   │   ├── task-1234567890.json
│   │   ├── task-1234567891.json
│   │   └── ...
│   └── progress/
│       ├── task-1234567890.json
│       ├── task-1234567891.json
│       └── ...
└── logs/
    ├── task-1234567890.log
    ├── task-1234567891.log
    └── ...
```

### 2.4 新数据流

```
用户配置 → 创建任务快照 → 预览分析 → 预览结果持久化 → 用户选择执行 → 
基于快照执行 → 执行结果持久化 → 任务完成
```

## 3. 核心功能设计

### 3.1 任务快照机制

#### 3.1.1 功能描述
- 在创建任务时，保存当前所有配置的快照
- 包括源目录配置、流水线配置、全局参数配置
- 任务执行时基于快照，不受用户修改配置影响

#### 3.1.2 实现要点
- 创建任务时生成唯一taskId
- 深度复制当前配置
- 将快照保存为JSON文件
- 提供快照查询和恢复功能

### 3.2 预览结果持久化

#### 3.2.1 功能描述
- 预览任务完成后，将ChangeRecord持久化到文件
- 每条记录一行JSON格式
- 支持分页查询和检索
- 支持按状态、操作类型等过滤

#### 3.2.2 实现要点
- 预览完成后写入文件
- 文件格式：每行一个JSON对象
- 提供查询API：分页、过滤、搜索
- 支持导出功能

### 3.3 执行结果持久化

#### 3.3.1 功能描述
- 执行任务完成后，将执行结果持久化到文件
- 记录每条记录的执行状态、执行时间、错误信息
- 支持重试失败记录
- 支持按状态查询

#### 3.3.2 实现要点
- 执行过程中实时更新结果文件
- 记录详细执行信息
- 支持失败记录重试
- 提供执行历史查询

### 3.4 任务进度统计

#### 3.4.1 功能描述
- 内存中维护任务进度统计
- 实时更新进度信息
- 通过WebSocket推送到前端
- 支持进度文件持久化

#### 3.4.2 实现要点
- 实时更新进度统计
- 定期持久化进度文件
- WebSocket实时推送
- 支持进度恢复

### 3.5 任务查询和检索

#### 3.5.1 功能描述
- 支持按任务状态查询
- 支持按时间范围查询
- 支持按操作类型过滤
- 支持关键词搜索
- 支持分页查询

#### 3.5.2 实现要点
- 提供RESTful API
- 支持多种查询条件
- 实现分页功能
- 优化查询性能

### 3.6 任务操作扩展

#### 3.6.1 功能描述
- 支持重试失败记录
- 支持跳过特定记录
- 支持批量操作
- 支持任务克隆

#### 3.6.2 实现要点
- 提供重试API
- 支持选择性重试
- 批量操作支持
- 任务克隆功能

## 4. API设计

### 4.1 任务管理API

```
POST   /api/tasks                    # 创建任务
GET    /api/tasks                    # 获取任务列表
GET    /api/tasks/{id}              # 获取任务详情
POST   /api/tasks/{id}/preview      # 预览任务
POST   /api/tasks/{id}/execute      # 执行任务
POST   /api/tasks/{id}/cancel       # 取消任务
DELETE /api/tasks/{id}              # 删除任务
POST   /api/tasks/{id}/clone        # 克隆任务
```

### 4.2 预览结果API

```
GET    /api/tasks/{id}/preview-results     # 获取预览结果
GET    /api/tasks/{id}/preview-results/{recordId}  # 获取单条预览结果
POST   /api/tasks/{id}/preview-results/execute  # 执行预览结果
POST   /api/tasks/{id}/preview-results/{recordId}/retry  # 重试单条记录
```

### 4.3 执行结果API

```
GET    /api/tasks/{id}/execution-results    # 获取执行结果
GET    /api/tasks/{id}/execution-results/{recordId}  # 获取单条执行结果
POST   /api/tasks/{id}/execution-results/retry-failed  # 重试失败记录
```

### 4.4 任务进度API

```
GET    /api/tasks/{id}/progress       # 获取任务进度
WebSocket /ws/tasks/{id}/progress    # 实时进度推送
```

## 5. 前端界面设计

### 5.1 任务列表页面
- 显示所有任务
- 支持状态过滤
- 支持时间范围查询
- 支持关键词搜索
- 分页显示

### 5.2 任务详情页面
- 显示任务快照信息
- 显示任务进度
- 显示预览结果
- 显示执行结果
- 支持操作按钮

### 5.3 预览结果页面
- 显示预览结果列表
- 支持分页和过滤
- 支持选择记录
- 支持批量执行
- 支持导出

### 5.4 执行结果页面
- 显示执行结果列表
- 支持分页和过滤
- 显示执行状态
- 支持重试失败记录
- 支持查看日志

## 6. 实现优先级

### 6.1 高优先级
1. 任务快照机制
2. 预览结果持久化
3. 执行结果持久化
4. 基础任务管理API

### 6.2 中优先级
5. 任务进度统计
6. 任务查询和检索
7. 基础前端界面更新

### 6.3 低优先级
8. 任务操作扩展（重试等）
9. 高级查询功能
10. 性能优化

## 7. 技术要点

### 7.1 并发控制
- 使用线程池管理任务执行
- 限制同时运行的任务数量
- 支持任务优先级

### 7.2 文件操作
- 使用NIO提高文件操作性能
- 支持大文件处理
- 文件操作原子性保证

### 7.3 错误处理
- 完善的异常处理机制
- 错误信息记录和恢复
- 支持任务重试

### 7.4 性能优化
- 文件读写优化
- 查询性能优化
- 内存使用优化

## 8. 兼容性考虑

### 8.1 向后兼容
- 保留现有API接口
- 数据格式兼容
- 渐进式迁移

### 8.2 数据迁移
- 提供数据迁移工具
- 支持旧数据导入
- 数据格式转换

## 9. 测试策略

### 9.1 单元测试
- 任务快照测试
- 预览结果测试
- 执行结果测试
- API测试

### 9.2 集成测试
- 完整任务流程测试
- 并发任务测试
- 错误恢复测试

### 9.3 性能测试
- 大文件处理测试
- 并发任务测试
- 查询性能测试
