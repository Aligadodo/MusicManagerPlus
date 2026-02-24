# API接口设计文档

## 概述

本文档描述了MusicManagerPlus后端的所有REST API接口，包括任务管理、配置管理、文件操作等核心功能。

## API基础信息

### 基础URL

```
http://localhost:8080/api
```

### 通用响应格式

#### 成功响应

```json
{
  "success": true,
  "data": {
    // 响应数据
  },
  "message": "操作成功"
}
```

#### 错误响应

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述"
  }
}
```

### HTTP状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 任务管理API

### 1. 创建任务

**接口**: `POST /api/tasks`

**请求体**:
```json
{
  "taskName": "测试任务",
  "sourceDirectories": [
    {
      "path": "/music",
      "recursive": true,
      "depth": 4
    }
  ],
  "strategies": [
    {
      "strategyId": "fileRename",
      "enabled": true,
      "config": {
        "pattern": "{artist}/{album}/{track} - {title}"
      }
    }
  ]
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "taskId": "task-123456",
    "taskName": "测试任务",
    "status": "CREATED",
    "createdAt": 1234567890,
    "configSnapshotId": "snapshot-789"
  }
}
```

### 2. 获取任务列表

**接口**: `GET /api/tasks`

**查询参数**:
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 10)
- `status`: 任务状态 (可选)

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 100,
    "list": [
      {
        "taskId": "task-123456",
        "taskName": "测试任务",
        "status": "COMPLETED",
        "progress": 1.0,
        "createdAt": 1234567890
      }
    ]
  }
}
```

### 3. 获取任务详情

**接口**: `GET /api/tasks/{taskId}`

**路径参数**:
- `taskId`: 任务ID

**响应**:
```json
{
  "success": true,
  "data": {
    "taskId": "task-123456",
    "taskName": "测试任务",
    "status": "COMPLETED",
    "currentStage": "EXECUTED",
    "progress": 1.0,
    "message": "任务完成",
    "createdAt": 1234567890,
    "updatedAt": 1234567900,
    "completedAt": 1234568000,
    "configSnapshotId": "snapshot-789",
    "configSnapshot": {
      "snapshotId": "snapshot-789",
      "snapshotName": "测试任务配置",
      "configData": {
        // 配置数据
      }
    },
    "stages": {
      "scan": {
        "status": "COMPLETED",
        "totalFiles": 1000,
        "totalSize": 1073741824,
        "scanStartTime": 1234567890,
        "scanEndTime": 1234567900,
        "scanDuration": 10
      },
      "preview": {
        "status": "COMPLETED",
        "totalFiles": 1000,
        "processedFiles": 1000,
        "changedFiles": 500,
        "unchangedFiles": 500,
        "previewStartTime": 1234567900,
        "previewEndTime": 1234567950,
        "previewDuration": 50
      },
      "execution": {
        "status": "COMPLETED",
        "executionCount": 1,
        "totalFiles": 500,
        "processedFiles": 500,
        "successCount": 495,
        "failedCount": 5,
        "skippedCount": 0,
        "executionStartTime": 1234567950,
        "executionEndTime": 1234568000,
        "executionDuration": 50
      }
    }
  }
}
```

### 4. 删除任务

**接口**: `DELETE /api/tasks/{taskId}`

**路径参数**:
- `taskId`: 任务ID

**响应**:
```json
{
  "success": true,
  "message": "任务删除成功"
}
```

### 5. 执行扫描

**接口**: `POST /api/tasks/{taskId}/scan`

**路径参数**:
- `taskId`: 任务ID

**响应**:
```json
{
  "success": true,
  "message": "扫描已启动"
}
```

### 6. 执行预览

**接口**: `POST /api/tasks/{taskId}/preview`

**路径参数**:
- `taskId`: 任务ID

**请求体**:
```json
{
  "limit": 1000
}
```

**响应**:
```json
{
  "success": true,
  "message": "预览已启动"
}
```

### 7. 执行任务

**接口**: `POST /api/tasks/{taskId}/execute`

**路径参数**:
- `taskId`: 任务ID

**请求体**:
```json
{
  "selectedRecords": ["record-1", "record-2"]
}
```

**响应**:
```json
{
  "success": true,
  "message": "任务执行已启动"
}
```

### 8. 取消任务

**接口**: `POST /api/tasks/{taskId}/cancel`

**路径参数**:
- `taskId`: 任务ID

**响应**:
```json
{
  "success": true,
  "message": "任务已取消"
}
```

### 9. 重启任务

**接口**: `POST /api/tasks/{taskId}/restart`

**路径参数**:
- `taskId`: 任务ID

**请求体**:
```json
{
  "stage": "scan" // scan, preview, execution
}
```

**响应**:
```json
{
  "success": true,
  "message": "任务已重启"
}
```

## 任务数据API

### 1. 获取扫描数据

**接口**: `GET /api/tasks/{taskId}/scan/data`

**路径参数**:
- `taskId`: 任务ID

**查询参数**:
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 100)

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 1000,
    "list": [
      {
        "filePath": "/music/song1.mp3",
        "fileName": "song1.mp3",
        "fileSize": 1048576,
        "lastModified": 1234567890
      }
    ]
  }
}
```

### 2. 获取扫描统计

**接口**: `GET /api/tasks/{taskId}/scan/statistics`

**路径参数**:
- `taskId`: 任务ID

**响应**:
```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "totalFiles": 1000,
    "totalSize": 1073741824,
    "scanStartTime": 1234567890,
    "scanEndTime": 1234567900,
    "scanDuration": 10
  }
}
```

### 3. 获取预览数据

**接口**: `GET /api/tasks/{taskId}/preview/data`

**路径参数**:
- `taskId`: 任务ID

**查询参数**:
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 100)
- `status`: 状态过滤 (可选)

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 500,
    "list": [
      {
        "recordId": "record-1",
        "filePath": "/music/song1.mp3",
        "fileName": "song1.mp3",
        "operationType": "RENAME",
        "newPath": "/music/Artist/Album/01 - Song.mp3",
        "status": "PENDING"
      }
    ]
  }
}
```

### 4. 获取预览统计

**接口**: `GET /api/tasks/{taskId}/preview/statistics`

**路径参数**:
- `taskId`: 任务ID

**响应**:
```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "totalFiles": 1000,
    "processedFiles": 1000,
    "changedFiles": 500,
    "unchangedFiles": 500,
    "previewStartTime": 1234567900,
    "previewEndTime": 1234567950,
    "previewDuration": 50
  }
}
```

### 5. 获取执行数据

**接口**: `GET /api/tasks/{taskId}/execution/data`

**路径参数**:
- `taskId`: 任务ID

**查询参数**:
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 100)
- `status`: 状态过滤 (可选)

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 500,
    "list": [
      {
        "recordId": "record-1",
        "filePath": "/music/song1.mp3",
        "fileName": "song1.mp3",
        "operationType": "RENAME",
        "newPath": "/music/Artist/Album/01 - Song.mp3",
        "status": "SUCCESS",
        "errorMessage": null
      }
    ]
  }
}
```

### 6. 获取执行统计

**接口**: `GET /api/tasks/{taskId}/execution/statistics`

**路径参数**:
- `taskId`: 任务ID

**响应**:
```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "executionCount": 1,
    "totalFiles": 500,
    "processedFiles": 500,
    "successCount": 495,
    "failedCount": 5,
    "skippedCount": 0,
    "executionStartTime": 1234567950,
    "executionEndTime": 1234568000,
    "executionDuration": 50
  }
}
```

### 7. 获取任务日志

**接口**: `GET /api/tasks/{taskId}/logs`

**路径参数**:
- `taskId`: 任务ID

**查询参数**:
- `page`: 页码 (默认: 1)
- `size`: 每页大小 (默认: 100)
- `level`: 日志级别 (可选: INFO, WARN, ERROR)

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 100,
    "list": [
      {
        "timestamp": 1234567890,
        "level": "INFO",
        "message": "开始扫描文件",
        "stage": "SCAN"
      }
    ]
  }
}
```

## 配置管理API

### 1. 获取所有配置

**接口**: `GET /api/config`

**响应**:
```json
{
  "success": true,
  "data": {
    "globalSettings": {
      "previewThreads": 4,
      "executionThreads": 4,
      "threadPoolMode": "auto",
      "autoRefresh": true,
      "previewLimit": 1000
    },
    "appearance": {
      "theme": "light",
      "primaryColor": "#1976D2",
      "fontSize": 14,
      "language": "zh-CN"
    },
    "strategies": {
      "fileRename": {
        "pattern": "{artist}/{album}/{track} - {title}"
      }
    }
  }
}
```

### 2. 获取指定配置

**接口**: `GET /api/config/{key}`

**路径参数**:
- `key`: 配置键 (例如: appearance, globalSettings)

**响应**:
```json
{
  "success": true,
  "data": {
    "theme": "light",
    "primaryColor": "#1976D2",
    "fontSize": 14,
    "language": "zh-CN"
  }
}
```

### 3. 更新配置

**接口**: `PUT /api/config`

**请求体**:
```json
{
  "globalSettings": {
    "previewThreads": 8
  },
  "appearance": {
    "theme": "dark"
  }
}
```

**响应**:
```json
{
  "success": true,
  "message": "配置更新成功"
}
```

### 4. 更新指定配置

**接口**: `PUT /api/config/{key}`

**路径参数**:
- `key`: 配置键

**请求体**:
```json
{
  "theme": "dark",
  "fontSize": 16
}
```

**响应**:
```json
{
  "success": true,
  "message": "配置更新成功"
}
```

### 5. 备份配置

**接口**: `POST /api/config/backup`

**请求体**:
```json
{
  "name": "配置备份-20240217"
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "backupId": "backup-123",
    "backupName": "配置备份-20240217",
    "backupPath": "/backups/config-backup-20240217.json",
    "createdAt": 1234567890
  }
}
```

### 6. 恢复配置

**接口**: `POST /api/config/restore`

**请求体**:
```json
{
  "backupId": "backup-123"
}
```

**响应**:
```json
{
  "success": true,
  "message": "配置恢复成功"
}
```

### 7. 获取配置模板

**接口**: `GET /api/config/templates`

**查询参数**:
- `type`: 模板类型 (可选: TASK, TEMPLATE, SYSTEM)

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 10,
    "list": [
      {
        "snapshotId": "snapshot-1",
        "snapshotName": "标准配置",
        "snapshotType": "TEMPLATE",
        "configData": {
          // 配置数据
        },
        "isTemplate": true,
        "createdAt": 1234567890
      }
    ]
  }
}
```

### 8. 创建配置模板

**接口**: `POST /api/config/templates`

**请求体**:
```json
{
  "snapshotName": "我的配置",
  "description": "自定义配置模板",
  "configData": {
    // 配置数据
  }
}
```

**响应**:
```json
{
  "success": true,
  "data": {
    "snapshotId": "snapshot-789",
    "snapshotName": "我的配置",
    "description": "自定义配置模板",
    "createdAt": 1234567890
  }
}
```

## 文件操作API

### 1. 获取文件列表

**接口**: `GET /api/files`

**查询参数**:
- `path`: 目录路径
- `recursive`: 是否递归 (默认: false)
- `depth`: 递归深度 (默认: 1)

**响应**:
```json
{
  "success": true,
  "data": {
    "path": "/music",
    "files": [
      {
        "name": "song1.mp3",
        "path": "/music/song1.mp3",
        "size": 1048576,
        "isDirectory": false,
        "lastModified": 1234567890
      }
    ]
  }
}
```

### 2. 删除文件

**接口**: `DELETE /api/files`

**请求体**:
```json
{
  "paths": ["/music/song1.mp3", "/music/song2.mp3"]
}
```

**响应**:
```json
{
  "success": true,
  "message": "文件删除成功"
}
```

## 策略管理API

### 1. 获取策略列表

**接口**: `GET /api/strategies`

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 10,
    "list": [
      {
        "strategyId": "fileRename",
        "strategyName": "文件重命名",
        "description": "根据规则重命名文件",
        "enabled": true,
        "config": {
          "pattern": "{artist}/{album}/{track} - {title}"
        }
      }
    ]
  }
}
```

### 2. 获取策略详情

**接口**: `GET /api/strategies/{strategyId}`

**路径参数**:
- `strategyId`: 策略ID

**响应**:
```json
{
  "success": true,
  "data": {
    "strategyId": "fileRename",
    "strategyName": "文件重命名",
    "description": "根据规则重命名文件",
    "enabled": true,
    "config": {
      "pattern": "{artist}/{album}/{track} - {title}"
    },
    "parameters": [
      {
        "key": "pattern",
        "type": "string",
        "description": "重命名模式",
        "required": true
      }
    ]
  }
}
```

### 3. 更新策略配置

**接口**: `PUT /api/strategies/{strategyId}`

**路径参数**:
- `strategyId`: 策略ID

**请求体**:
```json
{
  "enabled": true,
  "config": {
    "pattern": "{artist}/{album}/{track} - {title}"
  }
}
```

**响应**:
```json
{
  "success": true,
  "message": "策略配置更新成功"
}
```

## 插件管理API

### 1. 获取插件列表

**接口**: `GET /api/plugins`

**响应**:
```json
{
  "success": true,
  "data": {
    "total": 5,
    "list": [
      {
        "pluginId": "plugin-1",
        "pluginName": "音频转换插件",
        "version": "1.0.0",
        "enabled": true,
        "description": "支持多种音频格式转换"
      }
    ]
  }
}
```

### 2. 获取插件详情

**接口**: `GET /api/plugins/{pluginId}`

**路径参数**:
- `pluginId`: 插件ID

**响应**:
```json
{
  "success": true,
  "data": {
    "pluginId": "plugin-1",
    "pluginName": "音频转换插件",
    "version": "1.0.0",
    "enabled": true,
    "description": "支持多种音频格式转换",
    "config": {
      // 插件配置
    }
  }
}
```

### 3. 启用/禁用插件

**接口**: `PUT /api/plugins/{pluginId}/enabled`

**路径参数**:
- `pluginId`: 插件ID

**请求体**:
```json
{
  "enabled": true
}
```

**响应**:
```json
{
  "success": true,
  "message": "插件状态更新成功"
}
```

## 系统管理API

### 1. 获取系统信息

**接口**: `GET /api/system/info`

**响应**:
```json
{
  "success": true,
  "data": {
    "version": "1.0.0",
    "buildTime": "2024-02-17",
    "javaVersion": "1.8.0",
    "osName": "Mac OS X",
    "osVersion": "10.15.7"
  }
}
```

### 2. 获取系统状态

**接口**: `GET /api/system/status`

**响应**:
```json
{
  "success": true,
  "data": {
    "status": "running",
    "uptime": 3600,
    "memoryUsage": 512,
    "diskUsage": 1024,
    "activeTasks": 5
  }
}
```

### 3. 获取枚举值

**接口**: `GET /api/enums/{enumType}`

**路径参数**:
- `enumType`: 枚举类型 (例如: TaskStatus, OperationType)

**响应**:
```json
{
  "success": true,
  "data": {
    "CREATED": "已创建",
    "SCANNING": "扫描中",
    "SCANNED": "已扫描",
    "PREVIEWING": "预览中",
    "PREVIEWED": "已预览",
    "EXECUTING": "执行中",
    "COMPLETED": "已完成",
    "FAILED": "失败",
    "CANCELLED": "已取消"
  }
}
```

## WebSocket接口

### 任务进度推送

**订阅**: `/topic/tasks/{taskId}/progress`

**消息格式**:
```json
{
  "type": "progress",
  "taskId": "task-123456",
  "stage": "SCAN",
  "progress": 0.5,
  "message": "正在扫描文件..."
}
```

### 任务状态变更

**订阅**: `/topic/tasks/{taskId}/status`

**消息格式**:
```json
{
  "type": "status",
  "taskId": "task-123456",
  "oldStatus": "SCANNING",
  "newStatus": "SCANNED",
  "timestamp": 1234567890
}
```

### 配置更新通知

**订阅**: `/topic/config/update`

**消息格式**:
```json
{
  "type": "config_update",
  "key": "theme",
  "value": "dark",
  "timestamp": 1234567890
}
```

## 错误码

| 错误码 | 说明 |
|--------|------|
| TASK_NOT_FOUND | 任务不存在 |
| TASK_ALREADY_RUNNING | 任务已在运行 |
| INVALID_TASK_STATUS | 无效的任务状态 |
| INVALID_CONFIG | 无效的配置 |
| STRATEGY_NOT_FOUND | 策略不存在 |
| PLUGIN_NOT_FOUND | 插件不存在 |
| FILE_NOT_FOUND | 文件不存在 |
| PERMISSION_DENIED | 权限不足 |
| INTERNAL_ERROR | 内部错误 |

## 总结

本文档描述了MusicManagerPlus后端的所有REST API接口，包括任务管理、配置管理、文件操作、策略管理、插件管理和系统管理等核心功能。所有接口都遵循统一的响应格式和错误处理机制，为前端提供了稳定可靠的API服务。
