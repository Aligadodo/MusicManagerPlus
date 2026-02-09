# FileManager Plus API接口完整文档

## 概述

本文档详细描述FileManager Plus项目的所有REST API和WebSocket接口，包括请求格式、响应格式、错误处理等。

## 一、API设计原则

### 1.1 RESTful设计规范
- 使用HTTP动词表示操作类型（GET/POST/PUT/DELETE）
- 使用资源路径表示资源类型
- 使用HTTP状态码表示操作结果
- 使用JSON格式进行数据交换
- 支持CORS跨域请求

### 1.2 统一响应格式
#### 成功响应
```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

#### 错误响应
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述",
    "details": {}
  },
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 1.3 HTTP状态码
- **200 OK**: 请求成功
- **201 Created**: 资源创建成功
- **400 Bad Request**: 请求参数错误
- **401 Unauthorized**: 未授权
- **403 Forbidden**: 禁止访问
- **404 Not Found**: 资源不存在
- **500 Internal Server Error**: 服务器内部错误

## 二、策略相关API

### 2.1 获取所有策略
**接口**: `GET /api/strategies`

**描述**: 获取所有可用的策略列表

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": "file-collection",
      "name": "文件收集策略",
      "description": "基于文件名相似度进行文件归类",
      "version": "1.0.0"
    }
  ],
  "message": "获取策略列表成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 2.2 获取指定策略信息
**接口**: `GET /api/strategies/{id}`

**描述**: 获取指定策略的详细信息

**路径参数**:
- `id`: 策略ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": "file-collection",
    "name": "文件收集策略",
    "description": "基于文件名相似度进行文件归类",
    "version": "1.0.0",
    "configFields": [
      {
        "name": "targetDirectory",
        "label": "目标目录",
        "type": "directory",
        "defaultValue": "/tmp/collected",
        "description": "文件收集的目标目录",
        "required": true
      }
    ]
  },
  "message": "获取策略信息成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 2.3 获取策略配置
**接口**: `GET /api/strategies/{id}/config`

**描述**: 获取指定策略的当前配置

**路径参数**:
- `id`: 策略ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "configValues": {
      "targetDirectory": "/tmp/collected",
      "recursive": true
    },
    "preconditionGroups": [
      {
        "id": "default",
        "name": "默认条件组",
        "logicType": "AND",
        "preconditions": [
          {
            "id": "exist-condition",
            "field": "fileExists",
            "operator": "equals",
            "value": true,
            "description": "文件存在"
          }
        ]
      }
    ],
    "empty": false
  },
  "message": "获取策略配置成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 2.4 更新策略配置
**接口**: `POST /api/strategies/{id}/config`

**描述**: 更新指定策略的配置

**路径参数**:
- `id`: 策略ID

**请求体**:
```json
{
  "configValues": {
    "targetDirectory": "/tmp/new-collection",
    "recursive": false
  },
  "preconditionGroups": [
    {
      "id": "default",
      "name": "默认条件组",
      "logicType": "AND",
      "preconditions": [
        {
          "id": "exist-condition",
          "field": "fileExists",
          "operator": "equals",
          "value": true,
          "description": "文件存在"
        }
      ]
    }
  ]
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "策略配置更新成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 2.5 执行策略
**接口**: `POST /api/strategies/{id}/execute`

**描述**: 执行指定策略

**路径参数**:
- `id`: 策略ID

**请求体**:
```json
{
  "filePaths": [
    "/path/to/file1.mp3",
    "/path/to/file2.mp3"
  ],
  "config": {
    "configValues": {
      "targetDirectory": "/tmp/collected"
    },
    "preconditionGroups": []
  }
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "taskId": "task-123456",
    "changes": [
      {
        "id": "change-1",
        "originalName": "/path/to/file1.mp3",
        "newName": "/tmp/collected/file1.mp3",
        "filePath": "/path/to/file1.mp3",
        "changed": true,
        "operationType": "MOVE",
        "status": "PENDING"
      }
    ]
  },
  "message": "策略执行成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 2.6 预览策略执行结果
**接口**: `POST /api/strategies/{id}/preview`

**描述**: 预览指定策略的执行结果

**路径参数**:
- `id`: 策略ID

**请求体**: 同执行策略

**响应示例**:
```json
{
  "success": true,
  "data": {
    "changes": [
      {
        "id": "change-1",
        "originalName": "/path/to/file1.mp3",
        "newName": "/tmp/collected/file1.mp3",
        "filePath": "/path/to/file1.mp3",
        "changed": true,
        "operationType": "MOVE",
        "status": "PENDING"
      }
    ]
  },
  "message": "策略预览成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 三、插件相关API

### 3.1 获取所有插件
**接口**: `GET /api/plugins`

**描述**: 获取所有可用的插件列表

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": [
    {
      "id": "file-collection",
      "name": "文件收集插件",
      "description": "基于文件名相似度进行文件归类",
      "version": "1.0.0"
    }
  ],
  "message": "获取插件列表成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 3.2 获取指定插件信息
**接口**: `GET /api/plugins/{id}`

**描述**: 获取指定插件的详细信息

**路径参数**:
- `id`: 插件ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": "file-collection",
    "name": "文件收集插件",
    "description": "基于文件名相似度进行文件归类",
    "version": "1.0.0",
    "parameters": [
      {
        "name": "targetDirectory",
        "label": "目标目录",
        "type": "directory",
        "defaultValue": "/tmp/collected",
        "description": "文件收集的目标目录",
        "required": true
      }
    ]
  },
  "message": "获取插件信息成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 3.3 获取插件配置
**接口**: `GET /api/plugins/{id}/config`

**描述**: 获取指定插件的当前配置

**路径参数**:
- `id`: 插件ID

**响应示例**: 同策略配置

### 3.4 更新插件配置
**接口**: `POST /api/plugins/{id}/config`

**描述**: 更新指定插件的配置

**路径参数**:
- `id`: 插件ID

**请求体**: 同更新策略配置

**响应示例**: 同更新策略配置

### 3.5 执行插件
**接口**: `POST /api/plugins/{id}/execute`

**描述**: 执行指定插件

**路径参数**:
- `id`: 插件ID

**请求体**: 同执行策略

**响应示例**: 同执行策略

### 3.6 预览插件执行结果
**接口**: `POST /api/plugins/{id}/preview`

**描述**: 预览指定插件的执行结果

**路径参数**:
- `id`: 插件ID

**请求体**: 同执行策略

**响应示例**: 同预览策略

### 3.7 重新加载插件
**接口**: `POST /api/plugins/reload`

**描述**: 重新加载所有插件

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": {
    "loadedPlugins": 15,
    "failedPlugins": 0
  },
  "message": "插件重新加载成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 3.8 获取内部插件
**接口**: `GET /api/plugins/internal`

**描述**: 获取所有内部插件

**请求参数**: 无

**响应示例**: 同获取所有插件

### 3.9 获取外部插件
**接口**: `GET /api/plugins/external`

**描述**: 获取所有外部插件

**请求参数**: 无

**响应示例**: 同获取所有插件

### 3.10 扫描外部插件
**接口**: `POST /api/plugins/scan`

**描述**: 扫描指定目录中的外部插件

**请求体**:
```json
{
  "directory": "/path/to/plugins"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "foundPlugins": 5,
    "plugins": [
      {
        "path": "/path/to/plugins/custom-plugin.jar",
        "id": "custom-plugin",
        "name": "自定义插件",
        "version": "1.0.0"
      }
    ]
  },
  "message": "插件扫描成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 3.11 加载外部插件
**接口**: `POST /api/plugins/load-external`

**描述**: 加载指定的外部插件

**请求体**:
```json
{
  "pluginPath": "/path/to/plugins/custom-plugin.jar"
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "外部插件加载成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 3.12 重新加载外部插件
**接口**: `POST /api/plugins/reload-external`

**描述**: 重新加载所有外部插件

**请求参数**: 无

**响应示例**: 同重新加载插件

## 四、流水线相关API

### 4.1 获取流水线配置
**接口**: `GET /api/pipeline`

**描述**: 获取当前流水线配置

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": {
    "plugins": [
      {
        "id": "file-collection",
        "enabled": true,
        "config": {
          "targetDirectory": "/tmp/collected"
        }
      }
    ]
  },
  "message": "获取流水线配置成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 4.2 更新流水线配置
**接口**: `POST /api/pipeline`

**描述**: 更新流水线配置

**请求体**:
```json
{
  "plugins": [
    {
      "id": "file-collection",
      "enabled": true,
      "config": {
        "targetDirectory": "/tmp/collected"
      }
    }
  ]
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "流水线配置更新成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 4.3 执行流水线
**接口**: `POST /api/pipeline/execute`

**描述**: 执行流水线

**请求体**:
```json
{
  "filePaths": [
    "/path/to/file1.mp3",
    "/path/to/file2.mp3"
  ]
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "pipelineId": "pipeline-123456",
    "taskId": "task-123456"
  },
  "message": "流水线执行成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 五、文件相关API

### 5.1 获取文件列表
**接口**: `GET /api/files`

**描述**: 获取指定目录的文件列表

**请求参数**:
- `path`: 目录路径（必需）
- `recursive`: 是否递归（可选，默认false）
- `pattern`: 文件匹配模式（可选）

**响应示例**:
```json
{
  "success": true,
  "data": {
    "files": [
      {
        "name": "file1.mp3",
        "path": "/path/to/file1.mp3",
        "size": 1024000,
        "isDirectory": false,
        "lastModified": "2026-02-08T22:00:00Z"
      }
    ]
  },
  "message": "获取文件列表成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 5.2 扫描文件
**接口**: `POST /api/files/scan`

**描述**: 扫描指定目录的文件

**请求体**:
```json
{
  "path": "/path/to/directory",
  "minDepth": 0,
  "maxDepth": 3,
  "pattern": "*.mp3"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "files": [
      {
        "name": "file1.mp3",
        "path": "/path/to/file1.mp3",
        "size": 1024000,
        "isDirectory": false,
        "lastModified": "2026-02-08T22:00:00Z"
      }
    ]
  },
  "message": "文件扫描成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 5.3 获取文件信息
**接口**: `GET /api/files/info`

**描述**: 获取指定文件的详细信息

**请求参数**:
- `path`: 文件路径（必需）

**响应示例**:
```json
{
  "success": true,
  "data": {
    "name": "file1.mp3",
    "path": "/path/to/file1.mp3",
    "size": 1024000,
    "isDirectory": false,
    "lastModified": "2026-02-08T22:00:00Z",
    "created": "2026-02-08T22:00:00Z",
    "extension": "mp3",
    "metadata": {
      "title": "歌曲标题",
      "artist": "艺术家",
      "album": "专辑",
      "year": 2026
    }
  },
  "message": "获取文件信息成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 5.4 文件操作
**接口**: `POST /api/files/operation`

**描述**: 执行文件操作（移动、复制、删除等）

**请求体**:
```json
{
  "operation": "move",
  "source": "/path/to/source.mp3",
  "target": "/path/to/target.mp3"
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "success": true,
    "message": "移动成功"
  },
  "message": "文件操作成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 六、任务相关API

### 6.1 获取任务列表
**接口**: `GET /api/tasks`

**描述**: 获取所有任务列表

**请求参数**:
- `status`: 任务状态过滤（可选）
- `limit`: 返回数量限制（可选）

**响应示例**:
```json
{
  "success": true,
  "data": {
    "tasks": [
      {
        "id": "task-123456",
        "type": "file-collection",
        "status": "RUNNING",
        "progress": 50,
        "message": "处理中...",
        "createdAt": "2026-02-08T22:00:00Z",
        "updatedAt": "2026-02-08T22:00:00Z"
      }
    ]
  },
  "message": "获取任务列表成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 6.2 获取指定任务信息
**接口**: `GET /api/tasks/{id}`

**描述**: 获取指定任务的详细信息

**路径参数**:
- `id`: 任务ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": "task-123456",
    "type": "file-collection",
    "status": "RUNNING",
    "progress": 50,
    "message": "处理中...",
    "createdAt": "2026-02-08T22:00:00Z",
    "updatedAt": "2026-02-08T22:00:00Z",
    "changes": [
      {
        "id": "change-1",
        "originalName": "/path/to/file1.mp3",
        "newName": "/tmp/collected/file1.mp3",
        "filePath": "/path/to/file1.mp3",
        "changed": true,
        "operationType": "MOVE",
        "status": "SUCCESS"
      }
    ]
  },
  "message": "获取任务信息成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 6.3 创建任务
**接口**: `POST /api/tasks`

**描述**: 创建新任务

**请求体**:
```json
{
  "type": "file-collection",
  "filePaths": [
    "/path/to/file1.mp3",
    "/path/to/file2.mp3"
  ],
  "config": {
    "targetDirectory": "/tmp/collected"
  }
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "taskId": "task-123456"
  },
  "message": "任务创建成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 6.4 取消任务
**接口**: `POST /api/tasks/{id}/cancel`

**描述**: 取消指定任务

**路径参数**:
- `id`: 任务ID

**响应示例**:
```json
{
  "success": true,
  "message": "任务取消成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 七、日志相关API

### 7.1 获取日志列表
**接口**: `GET /api/logs`

**描述**: 获取日志列表

**请求参数**:
- `level`: 日志级别过滤（可选）
- `limit`: 返回数量限制（可选）
- `offset`: 偏移量（可选）

**响应示例**:
```json
{
  "success": true,
  "data": {
    "logs": [
      {
        "id": "log-1",
        "level": "INFO",
        "message": "策略执行成功",
        "timestamp": "2026-02-08T22:00:00Z",
        "source": "StrategyService"
      }
    ]
  },
  "message": "获取日志列表成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 7.2 获取指定日志信息
**接口**: `GET /api/logs/{id}`

**描述**: 获取指定日志的详细信息

**路径参数**:
- `id`: 日志ID

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": "log-1",
    "level": "INFO",
    "message": "策略执行成功",
    "timestamp": "2026-02-08T22:00:00Z",
    "source": "StrategyService",
    "details": {
      "strategyId": "file-collection",
      "taskId": "task-123456"
    }
  },
  "message": "获取日志信息成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 八、配置相关API

### 8.1 获取配置
**接口**: `GET /api/config`

**描述**: 获取系统配置

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": {
    "maxConcurrentTasks": 5,
    "defaultOutputDirectory": "/tmp/output",
    "enableAutoBackup": true,
    "backupInterval": 3600
  },
  "message": "获取配置成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 8.2 更新配置
**接口**: `POST /api/config`

**描述**: 更新系统配置

**请求体**:
```json
{
  "maxConcurrentTasks": 10,
  "defaultOutputDirectory": "/tmp/new-output",
  "enableAutoBackup": false,
  "backupInterval": 7200
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "配置更新成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 九、枚举相关API

### 9.1 获取枚举值
**接口**: `GET /api/enums/{enumName}`

**描述**: 获取指定枚举的所有值

**路径参数**:
- `enumName`: 枚举名称

**响应示例**:
```json
{
  "success": true,
  "data": {
    "name": "TaskStatus",
    "values": [
      {
        "value": "PENDING",
        "label": "等待中"
      },
      {
        "value": "RUNNING",
        "label": "运行中"
      },
      {
        "value": "SUCCESS",
        "label": "成功"
      },
      {
        "value": "FAILED",
        "label": "失败"
      }
    ]
  },
  "message": "获取枚举值成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 十、源目录相关API

### 10.1 获取源目录列表
**接口**: `GET /api/source-directories`

**描述**: 获取所有源目录

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": {
    "directories": [
      {
        "id": "dir-1",
        "path": "/path/to/source1",
        "name": "源目录1",
        "enabled": true
      }
    ]
  },
  "message": "获取源目录列表成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 10.2 添加源目录
**接口**: `POST /api/source-directories`

**描述**: 添加新的源目录

**请求体**:
```json
{
  "path": "/path/to/new-source",
  "name": "新源目录",
  "enabled": true
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": "dir-2"
  },
  "message": "源目录添加成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 10.3 删除源目录
**接口**: `DELETE /api/source-directories/{id}`

**描述**: 删除指定的源目录

**路径参数**:
- `id`: 源目录ID

**响应示例**:
```json
{
  "success": true,
  "message": "源目录删除成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 十一、线程池相关API

### 11.1 获取线程池状态
**接口**: `GET /api/thread-pool`

**描述**: 获取线程池状态

**请求参数**: 无

**响应示例**:
```json
{
  "success": true,
  "data": {
    "corePoolSize": 5,
    "maxPoolSize": 10,
    "activeCount": 3,
    "queueSize": 2,
    "completedTaskCount": 100
  },
  "message": "获取线程池状态成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 11.2 更新线程池配置
**接口**: `POST /api/thread-pool`

**描述**: 更新线程池配置

**请求体**:
```json
{
  "corePoolSize": 10,
  "maxPoolSize": 20
}
```

**响应示例**:
```json
{
  "success": true,
  "message": "线程池配置更新成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 十二、WebSocket接口

### 12.1 文件操作WebSocket
**端点**: `ws://localhost:8080/ws/file-operation`

**描述**: 用于推送文件操作进度和状态

**消息格式**:
```json
{
  "type": "progress",
  "data": {
    "taskId": "task-123456",
    "operation": "move",
    "progress": 50,
    "message": "移动中...",
    "source": "/path/to/source.mp3",
    "target": "/path/to/target.mp3"
  }
}
```

### 12.2 进度更新WebSocket
**端点**: `ws://localhost:8080/ws/progress`

**描述**: 用于推送任务执行进度

**消息格式**:
```json
{
  "type": "progress",
  "data": {
    "taskId": "task-123456",
    "progress": 50,
    "message": "处理中...",
    "total": 100,
    "current": 50
  }
}
```

### 12.3 任务状态WebSocket
**端点**: `ws://localhost:8080/ws/task`

**描述**: 用于推送任务状态变化

**消息格式**:
```json
{
  "type": "status",
  "data": {
    "taskId": "task-123456",
    "status": "RUNNING",
    "message": "任务运行中",
    "timestamp": "2026-02-08T22:00:00Z"
  }
}
```

## 十三、错误处理

### 13.1 错误码定义
- **INVALID_PARAMETER**: 参数错误
- **RESOURCE_NOT_FOUND**: 资源不存在
- **UNAUTHORIZED**: 未授权
- **FORBIDDEN**: 禁止访问
- **INTERNAL_ERROR**: 内部错误
- **PLUGIN_NOT_FOUND**: 插件不存在
- **STRATEGY_NOT_FOUND**: 策略不存在
- **TASK_NOT_FOUND**: 任务不存在
- **FILE_OPERATION_ERROR**: 文件操作错误
- **CONFIGURATION_ERROR**: 配置错误

### 13.2 错误响应示例
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "指定的资源不存在",
    "details": {
      "resourceType": "strategy",
      "resourceId": "non-existent-strategy"
    }
  },
  "timestamp": "2026-02-08T22:00:00Z"
}
```

## 十四、总结

本文档详细描述了FileManager Plus项目的所有REST API和WebSocket接口，包括请求格式、响应格式、错误处理等。API设计遵循RESTful规范，提供统一的响应格式和错误处理机制，为前端开发提供了清晰的接口定义。

---

**文档版本**: 1.0  
**创建日期**: 2026-02-08  
**维护者**: FileManager Plus Team
