# 任务管理系统 API 文档

## 1. 概述

本文档描述了任务管理系统的完整API接口，包括任务注册中心、任务生命周期管理、任务执行控制等功能。

## 2. 基础信息

### 2.1 接口基础

- **Base URL**: `http://localhost:8080/api`
- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8

### 2.2 通用响应格式

#### 成功响应

```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": 1642234567890
}
```

#### 失败响应

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误信息",
    "details": "详细错误信息"
  },
  "timestamp": 1642234567890
}
```

### 2.3 通用错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

## 3. 任务管理接口

### 3.1 创建任务

**接口**: `POST /tasks`

**请求参数**:

```json
{
  "taskName": "音乐文件整理",
  "sourceDirectories": [
    {
      "path": "/Users/music/source1",
      "depth": 4,
      "recursive": true,
      "includePatterns": ["*.mp3", "*.flac"],
      "excludePatterns": ["*.tmp"]
    }
  ],
  "pipelineConfig": {
    "pipelineId": "default-pipeline",
    "name": "默认流水线",
    "items": [
      {
        "strategyId": "advanced-rename",
        "strategyName": "高级重命名",
        "enabled": true,
        "order": 1,
        "config": {
          "pattern": "artist-title",
          "replaceRules": []
        }
      }
    ]
  },
  "globalSettings": {
    "maxThreads": 10,
    "timeout": 300000,
    "dryRun": false,
    "overwrite": false,
    "backup": false,
    "retryCount": 3,
    "retryInterval": 1000
  }
}
```

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "taskName": "音乐文件整理",
    "createdAt": 1642234567890,
    "status": "CREATED",
    "currentStage": "CREATED",
    "overallProgress": 0.0,
    "message": "任务已创建"
  }
}
```

### 3.2 获取任务列表

**接口**: `GET /tasks`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认20 |
| status | string | 否 | 状态筛选 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "taskId": "task-1234567890-1",
        "taskName": "音乐文件整理",
        "createdAt": 1642234567890,
        "status": "PREVIEWED",
        "currentStage": "PREVIEWED",
        "overallProgress": 80.0,
        "message": "预览完成"
      }
    ],
    "total": 10,
    "page": 1,
    "size": 20,
    "totalPages": 1
  }
}
```

### 3.3 获取任务详情

**接口**: `GET /tasks/{taskId}`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "taskName": "音乐文件整理",
    "createdAt": 1642234567890,
    "status": "PREVIEWED",
    "currentStage": "PREVIEWED",
    "overallProgress": 80.0,
    "message": "预览完成",
    "configSnapshot": {
      "sourceDirectories": [],
      "pipelineConfig": {},
      "globalSettings": {}
    },
    "stages": {
      "scan": {
        "status": "COMPLETED",
        "scanStartTime": 1642234567890,
        "scanEndTime": 1642234577890,
        "scannedFiles": 1500,
        "totalFiles": 1500
      },
      "preview": {
        "status": "COMPLETED",
        "previewStartTime": 1642234577890,
        "previewEndTime": 1642234617890,
        "analyzedFiles": 1500,
        "totalChanges": 1200
      },
      "execution": {
        "status": "PENDING",
        "executionStartTime": null,
        "executionEndTime": null,
        "executedFiles": 0,
        "successCount": 0,
        "failedCount": 0,
        "executionCount": 0
      }
    }
  }
}
```

### 3.4 删除任务

**接口**: `DELETE /tasks/{taskId}`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "deleted": true
  },
  "message": "任务已删除"
}
```

### 3.5 取消任务

**接口**: `POST /tasks/{taskId}/cancel`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "cancelled": true
  },
  "message": "任务已取消"
}
```

## 4. 任务执行接口

### 4.1 开始扫描

**接口**: `POST /tasks/{taskId}/scan`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "status": "SCANNING",
    "message": "扫描已开始"
  }
}
```

### 4.2 开始预览

**接口**: `POST /tasks/{taskId}/preview`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "status": "PREVIEWING",
    "message": "预览已开始"
  }
}
```

### 4.3 开始执行

**接口**: `POST /tasks/{taskId}/execute`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

```json
{
  "executeAll": true,
  "selectedRecordIds": []
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| executeAll | boolean | 否 | 是否执行全部，默认true |
| selectedRecordIds | array | 否 | 选中的记录ID列表 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "executionNum": 1,
    "status": "EXECUTING",
    "message": "执行已开始"
  }
}
```

## 5. 任务注册中心接口

### 5.1 获取所有任务

**接口**: `GET /tasks/registry/all`

**响应示例**:

```json
{
  "success": true,
  "data": {
    "tasks": [
      {
        "taskId": "task-1234567890-1",
        "taskName": "音乐文件整理",
        "status": "PREVIEWED",
        "createdAt": 1642234567890,
        "updatedAt": 1642234567890
      }
    ],
    "total": 10,
    "runningCount": 2
  }
}
```

### 5.2 获取任务统计信息

**接口**: `GET /tasks/registry/statistics`

**响应示例**:

```json
{
  "success": true,
  "data": {
    "totalTasks": 100,
    "runningTasks": 5,
    "completedTasks": 80,
    "failedTasks": 10,
    "cancelledTasks": 5,
    "statusDistribution": {
      "CREATED": 5,
      "SCANNING": 2,
      "SCANNED": 3,
      "PREVIEWING": 1,
      "PREVIEWED": 10,
      "EXECUTING": 2,
      "COMPLETED": 75,
      "FAILED": 8,
      "CANCELLED": 4
    }
  }
}
```

### 5.3 重新执行任务

**接口**: `POST /tasks/{taskId}/restart`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

```json
{
  "fromStage": "SCAN"
}
```

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| fromStage | string | 是 | 重新开始的阶段：SCAN/PREVIEW/EXECUTE |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890-1",
    "restarted": true,
    "fromStage": "SCAN",
    "status": "SCANNED",
    "message": "任务已准备重新执行"
  }
}
```

## 6. 任务状态枚举

| 状态值 | 说明 |
|--------|------|
| CREATED | 任务已创建 |
| SCANNING | 正在扫描 |
| SCANNED | 扫描完成 |
| PREVIEWING | 正在预览 |
| PREVIEWED | 预览完成 |
| EXECUTING | 正在执行 |
| COMPLETED | 执行完成 |
| FAILED | 执行失败 |
| CANCELLED | 已取消 |

## 7. 前端调用示例

### 7.1 创建任务

```dart
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/models/task_request.dart';

final taskService = TaskService(apiClient);

final request = TaskRequest(
  strategyId: 'test-strategy',
  filePaths: ['/Users/music/source1'],
  strategyConfig: StrategyConfig({
    'strategyName': '音乐整理策略',
  }),
  taskName: '音乐文件整理',
);

final taskId = await taskService.createTask(request);
print('任务创建成功: $taskId');
```

### 7.2 获取任务列表

```dart
final response = await taskService.getTaskList(
  page: 1,
  size: 20,
  status: 'PREVIEWED',
);

if (response['success']) {
  final tasks = response['tasks'] as List;
  print('共 ${response['count']} 个任务');
  for (var task in tasks) {
    print('${task['taskId']}: ${task['taskName']}');
  }
}
```

### 7.3 获取任务详情

```dart
final taskInfo = await taskService.getTaskInfo('task-1234567890-1');

print('任务状态: ${taskInfo.status}');
print('当前阶段: ${taskInfo.currentStage}');
print('整体进度: ${taskInfo.overallProgress}%');
```

### 7.4 删除任务

```dart
final response = await taskService.deleteTask('task-1234567890-1');

if (response['success']) {
  print('任务已删除');
}
```

### 7.5 重新执行任务

```dart
final response = await taskService.restartTask(
  'task-1234567890-1',
  fromStage: 'SCAN',
);

if (response['success']) {
  print('任务已重新执行');
}
```

## 8. WebSocket 实时更新

### 8.1 连接 WebSocket

**接口**: `ws://localhost:8080/ws/tasks/{taskId}`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

### 8.2 消息格式

#### 任务进度更新

```json
{
  "type": "TASK_PROGRESS",
  "data": {
    "taskId": "task-1234567890-1",
    "currentStage": "SCANNING",
    "overallProgress": 50.0,
    "message": "正在扫描文件..."
  }
}
```

#### 任务状态更新

```json
{
  "type": "TASK_STATUS",
  "data": {
    "taskId": "task-1234567890-1",
    "status": "PREVIEWED",
    "message": "预览完成"
  }
}
```

#### 任务完成

```json
{
  "type": "TASK_COMPLETED",
  "data": {
    "taskId": "task-1234567890-1",
    "status": "COMPLETED",
    "message": "任务已完成"
  }
}
```

#### 任务失败

```json
{
  "type": "TASK_FAILED",
  "data": {
    "taskId": "task-1234567890-1",
    "status": "FAILED",
    "message": "任务失败",
    "error": {
      "code": "SCAN_TIMEOUT",
      "message": "扫描超时"
    }
  }
}
```

## 9. 测试用例

### 9.1 端到端测试

项目包含完整的端到端测试用例，覆盖以下场景：

1. 创建任务并验证基本字段
2. 验证任务列表分页功能
3. 验证任务状态筛选功能
4. 验证任务详情完整性
5. 验证任务配置快照
6. 验证任务阶段信息
7. 验证多任务并发创建
8. 验证任务ID唯一性
9. 验证任务时间戳
10. 验证空任务列表处理
11. 验证不存在的任务ID
12. 验证任务状态枚举值
13. 验证任务进度范围
14. 验证任务消息字段
15. 验证任务当前阶段字段
16. 验证任务名称字段
17. 验证文件路径列表
18. 验证策略配置

运行测试：

```bash
cd clients/flutter-web-cli
flutter test test/task_management_comprehensive_e2e_test.dart
```

## 10. 注意事项

1. **任务ID格式**: 任务ID格式为 `task-{timestamp}-{counter}`，确保唯一性
2. **状态转换**: 任务状态只能按照预定义的转换规则进行转换
3. **并发控制**: 同一时间只能有一个任务处于执行状态
4. **删除限制**: 只有停止状态的任务才能被删除
5. **重新执行**: 重新执行任务会重置任务状态和进度
6. **WebSocket连接**: 建议在任务开始前建立WebSocket连接以接收实时更新
7. **分页参数**: 分页从1开始，每页最大数量为100
8. **错误处理**: 所有API调用都应该进行错误处理，并显示用户友好的错误信息

## 11. 更新日志

### 2026-02-14

- 新增任务注册中心功能
- 支持多任务管理
- 新增任务重新执行接口
- 新增任务统计信息接口
- 优化任务列表查询性能
- 修复任务详情刷新的字段匹配问题
- 添加完整的端到端测试用例
