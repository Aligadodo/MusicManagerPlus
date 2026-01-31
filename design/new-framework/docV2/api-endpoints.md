# API端点文档

## 概述

本文档提供了FileManager Plus后端服务中可用的API端点的全面概述。API遵循RESTful原则，提供文件操作、策略管理、任务执行、插件管理、配置和日志记录的端点。

## 基础URL

所有API端点都相对于后端服务的基础URL。默认情况下，本地运行时为`http://localhost:8080`。

## 认证

大多数API端点需要认证。后端为了简单起见使用HTTP Basic Authentication。如下所述，某些只读操作允许未经认证的访问。

## 端点类别

### 1. 文件操作

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/files/scan` | 扫描目录中的文件 | 公开 |
| GET | `/api/files/info` | 获取特定文件的信息 | 公开 |
| POST | `/api/files/exists` | 检查多个文件是否存在 | 公开 |
| POST | `/api/files/operation` | 执行文件操作（复制、移动、删除、重命名） | 需要 |

#### 使用示例

**扫描目录：**
```http
GET /api/files/scan?path=/path/to/directory&minDepth=0&maxDepth=3&pattern=*.mp3
```

**获取文件信息：**
```http
GET /api/files/info?path=/path/to/file.mp3
```

**检查文件是否存在：**
```http
POST /api/files/exists
Content-Type: application/json

{
  "paths": [
    "/path/to/file1.mp3",
    "/path/to/file2.mp3"
  ]
}
```

**文件操作：**
```http
POST /api/files/operation
Content-Type: application/json

{
  "operation": "copy",
  "source": "/path/to/source.mp3",
  "target": "/path/to/target.mp3"
}
```

### 2. 策略管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/strategies` | 获取所有可用策略 | 公开 |
| GET | `/api/strategies/{id}` | 获取特定策略的信息 | 需要 |
| GET | `/api/strategies/{id}/config` | 获取特定策略的配置 | 需要 |
| POST | `/api/strategies/{id}/config` | 更新特定策略的配置 | 需要 |
| POST | `/api/strategies/{id}/analyze` | 使用特定策略分析文件 | 需要 |
| POST | `/api/strategies/{id}/execute` | 在文件上执行策略 | 需要 |

#### 使用示例

**获取所有策略：**
```http
GET /api/strategies
```

**获取策略信息：**
```http
GET /api/strategies/file-collection
```

**获取策略配置：**
```http
GET /api/strategies/file-collection/config
```

**更新策略配置：**
```http
POST /api/strategies/file-collection/config
Content-Type: application/json

{
  "values": {
    "targetDirectory": "/path/to/target",
    "recursive": true
  }
}
```

**分析文件：**
```http
POST /api/strategies/file-collection/analyze
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "targetDirectory": "/path/to/target"
    }
  }
}
```

**执行策略：**
```http
POST /api/strategies/file-collection/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "targetDirectory": "/path/to/target"
    }
  }
}
```

### 3. 任务管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| POST | `/api/tasks` | 创建新任务 | 需要 |
| GET | `/api/tasks/{id}` | 获取特定任务的状态 | 需要 |
| GET | `/api/tasks` | 获取所有任务（可选过滤） | 需要 |
| POST | `/api/tasks/{id}/execute` | 执行特定任务 | 需要 |
| POST | `/api/tasks/{id}/cancel` | 取消运行中的任务 | 需要 |
| DELETE | `/api/tasks/{id}` | 删除任务 | 需要 |

#### 使用示例

**创建任务：**
```http
POST /api/tasks
Content-Type: application/json

{
  "strategyId": "file-collection",
  "filePaths": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "strategyConfig": {
    "values": {
      "targetDirectory": "/path/to/target"
    }
  }
}
```

**获取任务状态：**
```http
GET /api/tasks/task-1234567890
```

**获取所有任务：**
```http
GET /api/tasks?status=RUNNING&page=1&size=20
```

**执行任务：**
```http
POST /api/tasks/task-1234567890/execute
```

**取消任务：**
```http
POST /api/tasks/task-1234567890/cancel
```

**删除任务：**
```http
DELETE /api/tasks/task-1234567890
```

### 4. 插件管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/plugins` | 获取所有可用插件 | 需要 |
| GET | `/api/plugins/{id}` | 获取特定插件的信息 | 需要 |
| GET | `/api/plugins/{id}/config` | 获取特定插件的配置 | 需要 |
| POST | `/api/plugins/{id}/config` | 更新特定插件的配置 | 需要 |
| POST | `/api/plugins/{id}/execute` | 在文件上执行插件 | 需要 |
| POST | `/api/plugins/reload` | 重新加载所有插件 | 需要 |

#### 使用示例

**获取所有插件：**
```http
GET /api/plugins
```

**获取插件信息：**
```http
GET /api/plugins/file-cleanup
```

**获取插件配置：**
```http
GET /api/plugins/file-cleanup/config
```

**更新插件配置：**
```http
POST /api/plugins/file-cleanup/config
Content-Type: application/json

{
  "values": {
    "maxFileAgeDays": 30,
    "deleteEmptyDirectories": true
  }
}
```

**执行插件：**
```http
POST /api/plugins/file-cleanup/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "maxFileAgeDays": 30
    }
  }
}
```

**重新加载插件：**
```http
POST /api/plugins/reload
```

### 5. 配置管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/config` | 获取所有配置设置 | 需要 |
| GET | `/api/config/{key}` | 获取特定配置值 | 需要 |
| POST | `/api/config` | 更新多个配置设置 | 需要 |
| POST | `/api/config/{key}` | 更新特定配置值 | 需要 |
| DELETE | `/api/config/{key}` | 删除特定配置值 | 需要 |
| DELETE | `/api/config` | 清除所有配置设置 | 需要 |

#### 使用示例

**获取所有配置：**
```http
GET /api/config
```

**获取特定配置：**
```http
GET /api/config/maxConcurrentTasks
```

**更新多个配置：**
```http
POST /api/config
Content-Type: application/json

{
  "maxConcurrentTasks": 5,
  "defaultScanDepth": 3
}
```

**更新特定配置：**
```http
POST /api/config/maxConcurrentTasks
Content-Type: application/json

10
```

**删除特定配置：**
```http
DELETE /api/config/oldSetting
```

**清除所有配置：**
```http
DELETE /api/config
```

### 6. 日志管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/logs` | 获取所有日志（可选过滤） | 需要 |
| POST | `/api/logs` | 添加新日志条目 | 需要 |
| DELETE | `/api/logs` | 清除所有日志 | 需要 |

#### 使用示例

**获取日志：**
```http
GET /api/logs?level=ERROR&source=plugin&page=1&size=50
```

**添加日志条目：**
```http
POST /api/logs
Content-Type: application/json

{
  "level": "INFO",
  "message": "任务执行成功",
  "source": "task-service"
}
```

**清除日志：**
```http
DELETE /api/logs
```

### 7. 源目录管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/source-directories` | 获取所有源目录 | 需要 |
| POST | `/api/source-directories` | 添加新源目录 | 需要 |
| DELETE | `/api/source-directories` | 清除所有源目录 | 需要 |
| DELETE | `/api/source-directories/{id}` | 移除特定源目录 | 需要 |
| PUT | `/api/source-directories/{id}/threads` | 更新特定源目录的线程数 | 需要 |

#### 使用示例

**获取所有源目录：**
```http
GET /api/source-directories
```

**添加源目录：**
```http
POST /api/source-directories
Content-Type: application/json

{
  "path": "/path/to/source",
  "threadCount": 4
}
```

**移除源目录：**
```http
DELETE /api/source-directories/path/to/source
```

**清除所有源目录：**
```http
DELETE /api/source-directories
```

**更新线程数：**
```http
PUT /api/source-directories/path/to/source/threads
Content-Type: application/json

{
  "threadCount": 8
}
```

### 8. 流水线管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/pipeline` | 获取当前流水线配置 | 需要 |
| POST | `/api/pipeline` | 更新流水线配置 | 需要 |
| POST | `/api/pipeline/analyze` | 分析流水线以预览更改 | 需要 |
| POST | `/api/pipeline/execute` | 在源目录上执行流水线 | 需要 |

#### 使用示例

**获取流水线：**
```http
GET /api/pipeline
```

**更新流水线：**
```http
POST /api/pipeline
Content-Type: application/json

[
  {
    "strategyId": "rename",
    "name": "重命名策略",
    "config": {
      "pattern": "*.mp3",
      "replacement": "{artist} - {title}.mp3"
    }
  },
  {
    "strategyId": "move",
    "name": "移动策略",
    "config": {
      "targetDirectory": "/path/to/target"
    }
  }
]
```

**分析流水线：**
```http
POST /api/pipeline/analyze
Content-Type: application/json

{
  "sourceDirectories": ["/path/to/source1", "/path/to/source2"],
  "pipeline": [
    {
      "strategyId": "rename",
      "config": {}
    }
  ]
}
```

**执行流水线：**
```http
POST /api/pipeline/execute
Content-Type: application/json

{
  "sourceDirectories": ["/path/to/source1", "/path/to/source2"],
  "pipeline": [
    {
      "strategyId": "rename",
      "config": {}
    }
  ]
}
```

### 9. 线程池管理

| 方法 | 端点 | 描述 | 认证 |
|------|------|------|------|
| GET | `/api/thread-pool` | 获取线程池配置 | 需要 |
| PUT | `/api/thread-pool/preview` | 更新预览线程数 | 需要 |
| PUT | `/api/thread-pool/execution` | 更新执行线程数 | 需要 |

#### 使用示例

**获取线程池配置：**
```http
GET /api/thread-pool
```

**更新预览线程数：**
```http
PUT /api/thread-pool/preview
Content-Type: application/json

{
  "threads": 8
}
```

**更新执行线程数：**
```http
PUT /api/thread-pool/execution
Content-Type: application/json

{
  "threads": 16
}
```

## 错误处理

所有API端点返回适当的HTTP状态码，以指示请求的成功或失败：

| 状态码 | 描述 |
|--------|------|
| 200 OK | 请求成功完成 |
| 400 Bad Request | 无效的请求参数或格式 |
| 401 Unauthorized | 需要认证 |
| 404 Not Found | 资源未找到 |
| 500 Internal Server Error | 服务器端错误发生 |

错误响应通常包括带有错误详细信息的JSON对象：

```json
{
  "error": "描述问题的错误信息"
}
```

## 速率限制

当前未实现速率限制。将来可能会添加此功能以防止滥用。

## 版本控制

API目前不在URL路径中使用版本控制。未来的更改将保持向后兼容性或根据需要引入版本控制。

## WebSocket端点

除了RESTful端点外，后端还提供WebSocket端点用于实时通信：

| 端点 | 描述 |
|------|------|
| `/ws/progress` | 实时任务进度更新 |
| `/ws/task` | 实时任务状态更新 |
| `/ws/file` | 实时文件操作更新 |

这些WebSocket端点使用STOMP协议进行通信。

## 结论

本文档提供了FileManager Plus后端服务中可用端点的全面概述。有关特定端点或其实现的更多详细信息，请参考源代码或联系开发团队。