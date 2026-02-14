# 前后端接口设计文档

## 1. 接口概览

### 1.1 基础信息

- **Base URL**: `http://localhost:8080/api`
- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8
- **认证方式**: Bearer Token（待实现）

### 1.2 通用响应格式

#### 1.2.1 成功响应

```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": 1642234567890
}
```

#### 1.2.2 失败响应

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

### 1.3 通用错误码

| 错误码 | 说明 |
|--------|------|
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 |

### 1.4 分页参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |

### 1.5 分页响应格式

```json
{
  "success": true,
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

## 2. 任务管理接口

### 2.1 创建任务

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
    "taskId": "task-1234567890",
    "taskName": "音乐文件整理",
    "createdAt": 1642234567890,
    "status": "CREATED",
    "currentStage": "CREATED",
    "progress": 0,
    "message": "任务已创建"
  }
}
```

### 2.2 获取任务列表

**接口**: `GET /tasks`

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |
| status | string | 否 | 状态筛选 |
| keyword | string | 否 | 关键词搜索 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "taskId": "task-1234567890",
        "taskName": "音乐文件整理",
        "createdAt": 1642234567890,
        "status": "PREVIEWED",
        "currentStage": "PREVIEWED",
        "progress": 80,
        "message": "预览完成"
      }
    ],
    "total": 10,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  }
}
```

### 2.3 获取任务详情

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
    "taskId": "task-1234567890",
    "taskName": "音乐文件整理",
    "createdAt": 1642234567890,
    "status": "PREVIEWED",
    "currentStage": "PREVIEWED",
    "progress": 80,
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
        "scanDuration": 10000,
        "totalFiles": 1500
      },
      "preview": {
        "status": "COMPLETED",
        "previewStartTime": 1642234577890,
        "previewEndTime": 1642234617890,
        "previewDuration": 40000,
        "totalFiles": 1500,
        "processedFiles": 1500,
        "changedFiles": 1200,
        "unchangedFiles": 300
      },
      "execution": {
        "status": "COMPLETED",
        "executionCount": 3,
        "currentExecution": "execution_003",
        "executionStartTime": 1642234617890,
        "executionEndTime": 1642234677890,
        "executionDuration": 60000,
        "totalFiles": 1200,
        "processedFiles": 1200,
        "successCount": 1180,
        "failedCount": 15,
        "skippedCount": 5
      }
    }
  }
}
```

### 2.4 删除任务

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
    "taskId": "task-1234567890",
    "deleted": true
  },
  "message": "任务已删除"
}
```

### 2.5 取消任务

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
    "taskId": "task-1234567890",
    "cancelled": true
  },
  "message": "任务已取消"
}
```

## 3. 任务执行接口

### 3.1 开始扫描

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
    "taskId": "task-1234567890",
    "status": "SCANNING",
    "message": "扫描已开始"
  }
}
```

### 3.2 开始预览

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
    "taskId": "task-1234567890",
    "status": "PREVIEWING",
    "message": "预览已开始"
  }
}
```

### 3.3 开始执行

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
    "taskId": "task-1234567890",
    "executionNum": 1,
    "status": "EXECUTING",
    "message": "执行已开始"
  }
}
```

### 3.4 重试失败

**接口**: `POST /tasks/{taskId}/retry`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "taskId": "task-1234567890",
    "executionNum": 2,
    "status": "EXECUTING",
    "message": "重试已开始"
  }
}
```

## 4. 文件扫描结果接口

### 4.1 获取扫描统计信息

**接口**: `GET /tasks/{taskId}/scan/statistics`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "scanStartTime": 1642234567890,
    "scanEndTime": 1642234577890,
    "scanDuration": 10000,
    "totalFiles": 1500
  }
}
```

### 4.2 获取扫描文件列表

**接口**: `GET /tasks/{taskId}/scan/files`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |
| keyword | string | 否 | 关键词搜索 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "filePath": "/Users/music/source1/song1.mp3",
        "fileName": "song1.mp3",
        "fileSize": 5242880,
        "lastModified": 1642234567890
      }
    ],
    "total": 1500,
    "page": 1,
    "pageSize": 20,
    "totalPages": 75
  }
}
```

### 4.3 导出扫描文件列表

**接口**: `GET /tasks/{taskId}/scan/files/export`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| format | string | 否 | 导出格式，csv/json，默认csv |

**响应**: 文件流

## 5. 预览结果接口

### 5.1 获取预览统计信息

**接口**: `GET /tasks/{taskId}/preview/statistics`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "previewStartTime": 1642234577890,
    "previewEndTime": 1642234617890,
    "previewDuration": 40000,
    "totalFiles": 1500,
    "processedFiles": 1500,
    "changedFiles": 1200,
    "unchangedFiles": 300
  }
}
```

### 5.2 获取预览变更记录

**接口**: `GET /tasks/{taskId}/preview/records`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |
| operationType | string | 否 | 操作类型筛选 |
| changed | boolean | 否 | 是否变更筛选 |
| keyword | string | 否 | 关键词搜索 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "recordId": "record-001",
        "originalName": "song1.mp3",
        "newName": "周杰伦-青花瓷.mp3",
        "filePath": "/Users/music/source1/song1.mp3",
        "newPath": "/Users/music/source1/周杰伦-青花瓷.mp3",
        "operationType": "RENAME",
        "changed": true,
        "extraParams": {
          "pattern": "artist-title",
          "artist": "周杰伦",
          "title": "青花瓷"
        },
        "status": "PENDING",
        "reason": "根据元数据自动生成",
        "analyzeTime": 1642234577890
      }
    ],
    "total": 1200,
    "page": 1,
    "pageSize": 20,
    "totalPages": 60
  }
}
```

### 5.3 导出预览变更记录

**接口**: `GET /tasks/{taskId}/preview/records/export`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| format | string | 否 | 导出格式，csv/json，默认csv |

**响应**: 文件流

## 6. 执行结果接口

### 6.1 获取执行历史

**接口**: `GET /tasks/{taskId}/execution/history`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**响应示例**:

```json
{
  "success": true,
  "data": [
    {
      "executionNum": 1,
      "executionId": "execution_001",
      "status": "COMPLETED",
      "executionStartTime": 1642234617890,
      "executionEndTime": 1642234627890,
      "executionDuration": 10000,
      "totalFiles": 1200,
      "processedFiles": 1200,
      "successCount": 1180,
      "failedCount": 15,
      "skippedCount": 5
    },
    {
      "executionNum": 2,
      "executionId": "execution_002",
      "status": "COMPLETED",
      "executionStartTime": 1642234627890,
      "executionEndTime": 1642234637890,
      "executionDuration": 10000,
      "totalFiles": 15,
      "processedFiles": 15,
      "successCount": 15,
      "failedCount": 0,
      "skippedCount": 0
    },
    {
      "executionNum": 3,
      "executionId": "execution_003",
      "status": "COMPLETED",
      "executionStartTime": 1642234637890,
      "executionEndTime": 1642234647890,
      "executionDuration": 10000,
      "totalFiles": 1200,
      "processedFiles": 1200,
      "successCount": 1180,
      "failedCount": 15,
      "skippedCount": 5
    }
  ]
}
```

### 6.2 获取执行统计信息

**接口**: `GET /tasks/{taskId}/execution/{executionNum}/statistics`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |
| executionNum | int | 是 | 执行次数 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "executionStartTime": 1642234617890,
    "executionEndTime": 1642234627890,
    "executionDuration": 10000,
    "totalFiles": 1200,
    "processedFiles": 1200,
    "successCount": 1180,
    "failedCount": 15,
    "skippedCount": 5
  }
}
```

### 6.3 获取执行记录

**接口**: `GET /tasks/{taskId}/execution/{executionNum}/records`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |
| executionNum | int | 是 | 执行次数 |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |
| status | string | 否 | 状态筛选 |
| operationType | string | 否 | 操作类型筛选 |
| keyword | string | 否 | 关键词搜索 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "recordId": "record-001",
        "originalName": "song1.mp3",
        "newName": "周杰伦-青花瓷.mp3",
        "filePath": "/Users/music/source1/song1.mp3",
        "newPath": "/Users/music/source1/周杰伦-青花瓷.mp3",
        "operationType": "RENAME",
        "changed": true,
        "extraParams": {
          "pattern": "artist-title",
          "artist": "周杰伦",
          "title": "青花瓷"
        },
        "status": "SUCCESS",
        "reason": "根据元数据自动生成",
        "analyzeTime": 1642234577890,
        "executeTime": 1642234617890,
        "errorMessage": null,
        "retryCount": 0
      }
    ],
    "total": 1200,
    "page": 1,
    "pageSize": 20,
    "totalPages": 60
  }
}
```

### 6.4 导出执行记录

**接口**: `GET /tasks/{taskId}/execution/{executionNum}/records/export`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |
| executionNum | int | 是 | 执行次数 |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| format | string | 否 | 导出格式，csv/json，默认csv |

**响应**: 文件流

## 7. 任务日志接口

### 7.1 获取任务日志

**接口**: `GET /tasks/{taskId}/logs`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认20 |
| level | string | 否 | 日志级别筛选 |
| keyword | string | 否 | 关键词搜索 |

**响应示例**:

```json
{
  "success": true,
  "data": {
    "list": [
      {
        "timestamp": 1642234567890,
        "level": "INFO",
        "message": "[INFO] [SCAN] 开始扫描文件"
      },
      {
        "timestamp": 1642234577890,
        "level": "INFO",
        "message": "[INFO] [SCAN] 扫描完成，共 1500 个文件"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

### 7.2 导出任务日志

**接口**: `GET /tasks/{taskId}/logs/export`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

**请求参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| format | string | 否 | 导出格式，txt/json，默认txt |

**响应**: 文件流

## 8. WebSocket接口

### 8.1 连接WebSocket

**接口**: `ws://localhost:8080/ws/tasks/{taskId}`

**路径参数**:

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| taskId | string | 是 | 任务ID |

### 8.2 消息格式

#### 8.2.1 任务进度更新

```json
{
  "type": "TASK_PROGRESS",
  "data": {
    "taskId": "task-1234567890",
    "currentStage": "SCANNING",
    "progress": 50,
    "message": "正在扫描文件..."
  }
}
```

#### 8.2.2 阶段状态更新

```json
{
  "type": "STAGE_STATUS",
  "data": {
    "taskId": "task-1234567890",
    "stage": "SCAN",
    "status": "RUNNING",
    "startTime": 1642234567890,
    "processedCount": 750,
    "totalCount": 1500
  }
}
```

#### 8.2.3 任务完成

```json
{
  "type": "TASK_COMPLETED",
  "data": {
    "taskId": "task-1234567890",
    "status": "COMPLETED",
    "message": "任务已完成"
  }
}
```

#### 8.2.4 任务失败

```json
{
  "type": "TASK_FAILED",
  "data": {
    "taskId": "task-1234567890",
    "status": "FAILED",
    "message": "任务失败: 扫描超时",
    "error": {
      "code": "SCAN_TIMEOUT",
      "message": "扫描超时",
      "details": "扫描操作超过300秒"
    }
  }
}
```

#### 8.2.5 任务取消

```json
{
  "type": "TASK_CANCELLED",
  "data": {
    "taskId": "task-1234567890",
    "status": "CANCELLED",
    "message": "任务已取消"
  }
}
```

## 9. 前端API调用示例

### 9.1 创建任务

```javascript
import { TaskService } from './services/task_service.dart';

final taskService = TaskService();

final taskRequest = {
  'taskName': '音乐文件整理',
  'sourceDirectories': [
    {
      'path': '/Users/music/source1',
      'depth': 4,
      'recursive': true,
    }
  ],
  'pipelineConfig': {
    'pipelineId': 'default-pipeline',
    'name': '默认流水线',
    'items': [
      {
        'strategyId': 'advanced-rename',
        'strategyName': '高级重命名',
        'enabled': true,
        'order': 1,
        'config': {
          'pattern': 'artist-title',
        }
      }
    ]
  },
  'globalSettings': {
    'maxThreads': 10,
    'timeout': 300000,
  }
};

final response = await taskService.createTask(taskRequest);
if (response.success) {
  print('任务创建成功: ${response.data['taskId']}');
} else {
  print('任务创建失败: ${response.error['message']}');
}
```

### 9.2 获取任务列表

```javascript
final response = await taskService.getTaskList(
  page: 1,
  pageSize: 20,
  status: 'PREVIEWED',
  keyword: '音乐',
);

if (response.success) {
  final tasks = response.data['list'];
  print('共 ${response.data['total']} 个任务');
  for (var task in tasks) {
    print('${task['taskId']}: ${task['taskName']}');
  }
}
```

### 9.3 获取任务详情

```javascript
final response = await taskService.getTaskDetail('task-1234567890');

if (response.success) {
  final taskInfo = response.data;
  print('任务状态: ${taskInfo['status']}');
  print('当前阶段: ${taskInfo['currentStage']}');
  print('整体进度: ${taskInfo['progress']}%');
}
```

### 9.4 开始扫描

```javascript
final response = await taskService.startScan('task-1234567890');

if (response.success) {
  print('扫描已开始');
} else {
  print('扫描启动失败: ${response.error['message']}');
}
```

### 9.5 获取预览变更记录

```javascript
final response = await taskService.getPreviewRecords(
  'task-1234567890',
  page: 1,
  pageSize: 20,
  operationType: 'RENAME',
  changed: true,
);

if (response.success) {
  final records = response.data['list'];
  print('共 ${response.data['total']} 条变更记录');
  for (var record in records) {
    print('${record['originalName']} -> ${record['newName']}');
  }
}
```

### 9.6 开始执行

```javascript
final response = await taskService.startExecute(
  'task-1234567890',
  executeAll: true,
  selectedRecordIds: [],
);

if (response.success) {
  print('执行已开始，执行次数: ${response.data['executionNum']}');
} else {
  print('执行启动失败: ${response.error['message']}');
}
```

### 9.7 WebSocket监听

```javascript
import 'package:web_socket_channel/web_socket_channel.dart';

final channel = WebSocketChannel.connect(
  Uri.parse('ws://localhost:8080/ws/tasks/task-1234567890')
);

channel.stream.listen((message) {
  final data = jsonDecode(message);
  final type = data['type'];
  
  switch (type) {
    case 'TASK_PROGRESS':
      final progress = data['data'];
      print('任务进度: ${progress['progress']}%');
      print('当前阶段: ${progress['currentStage']}');
      break;
      
    case 'STAGE_STATUS':
      final stage = data['data'];
      print('阶段状态: ${stage['stage']} - ${stage['status']}');
      print('处理进度: ${stage['processedCount']}/${stage['totalCount']}');
      break;
      
    case 'TASK_COMPLETED':
      print('任务已完成');
      break;
      
    case 'TASK_FAILED':
      final error = data['data']['error'];
      print('任务失败: ${error['message']}');
      break;
      
    case 'TASK_CANCELLED':
      print('任务已取消');
      break;
  }
});
```

## 10. 错误处理

### 10.1 网络错误

```javascript
try {
  final response = await taskService.getTaskList();
} catch (e) {
  if (e is SocketException) {
    print('网络连接失败，请检查网络设置');
  } else if (e is TimeoutException) {
    print('请求超时，请稍后重试');
  } else {
    print('未知错误: $e');
  }
}
```

### 10.2 业务错误

```javascript
final response = await taskService.createTask(taskRequest);

if (!response.success) {
  final error = response.error;
  switch (error['code']) {
    case 'INVALID_PARAMS':
      print('请求参数错误: ${error['message']}');
      break;
    case 'TASK_NOT_FOUND':
      print('任务不存在');
      break;
    case 'TASK_ALREADY_RUNNING':
      print('任务正在运行中，请稍后重试');
      break;
    case 'SCAN_TIMEOUT':
      print('扫描超时，请增加超时时间或减少扫描范围');
      break;
    case 'EXECUTION_FAILED':
      print('执行失败: ${error['message']}');
      break;
    default:
      print('未知错误: ${error['message']}');
  }
}
```

## 11. 性能优化

### 11.1 请求缓存

```javascript
final taskService = TaskService();

// 启用缓存
taskService.enableCache(expireTime: Duration(minutes: 5));

// 获取任务列表（会使用缓存）
final response = await taskService.getTaskList();

// 清除缓存
taskService.clearCache();
```

### 11.2 请求防抖

```javascript
import 'package:async/async.dart';

final debouncedSearch = Debouncer(
  duration: Duration(milliseconds: 500),
  onValue: (keyword) async {
    final response = await taskService.getTaskList(keyword: keyword);
    // 更新UI
  }
);

// 搜索框输入
searchController.addListener(() {
  debouncedSearch.value = searchController.text;
});
```

### 11.3 分页加载

```javascript
int currentPage = 1;
bool isLoading = false;
bool hasMore = true;

Future<void> loadMoreTasks() async {
  if (isLoading || !hasMore) return;
  
  isLoading = true;
  
  final response = await taskService.getTaskList(
    page: currentPage,
    pageSize: 20,
  );
  
  if (response.success) {
    final tasks = response.data['list'];
    taskList.addAll(tasks);
    
    currentPage++;
    hasMore = tasks.length >= 20;
  }
  
  isLoading = false;
}
```

## 12. 安全考虑

### 12.1 认证

```javascript
final taskService = TaskService();

// 设置认证Token
taskService.setAuthToken('your-auth-token');

// 清除认证Token
taskService.clearAuthToken();
```

### 12.2 数据验证

```javascript
// 前端验证
if (taskName.isEmpty) {
  showError('任务名称不能为空');
  return;
}

if (sourceDirectories.isEmpty) {
  showError('请至少添加一个源目录');
  return;
}

// 发送请求
final response = await taskService.createTask(taskRequest);
```

### 12.3 XSS防护

```javascript
// 转义HTML
String escapeHtml(String text) {
  return text
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
```
