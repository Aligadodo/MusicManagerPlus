# API Endpoints Documentation

## Overview

This document provides a comprehensive overview of the API endpoints available in the FileManager Plus backend service. The API follows RESTful principles and provides endpoints for file operations, strategy management, task execution, plugin management, configuration, and logging.

## Base URL

All API endpoints are relative to the base URL of the backend service. By default, this is `http://localhost:8080` when running locally.

## Authentication

Most API endpoints require authentication. The backend uses HTTP Basic Authentication for simplicity. Unauthenticated access is allowed for certain read-only operations as specified below.

## Endpoint Categories

### 1. File Operations

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| GET | `/files/scan` | Scan a directory for files | Public |
| GET | `/files/info` | Get information about a specific file | Public |
| POST | `/files/exists` | Check if multiple files exist | Public |
| POST | `/files/operation` | Perform file operations (copy, move, delete, rename) | Required |

#### Example Usage

**Scan Directory:**
```http
GET /files/scan?path=/path/to/directory&minDepth=0&maxDepth=3&pattern=*.mp3
```

**Get File Info:**
```http
GET /files/info?path=/path/to/file.mp3
```

**Check Files Exist:**
```http
POST /files/exists
Content-Type: application/json

{
  "paths": [
    "/path/to/file1.mp3",
    "/path/to/file2.mp3"
  ]
}
```

**File Operation:**
```http
POST /files/operation
Content-Type: application/json

{
  "operation": "copy",
  "source": "/path/to/source.mp3",
  "target": "/path/to/target.mp3"
}
```

### 2. Strategy Management

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| GET | `/strategies` | Get all available strategies | Public |
| GET | `/strategies/{id}` | Get information about a specific strategy | Required |
| GET | `/strategies/{id}/config` | Get configuration for a specific strategy | Required |
| POST | `/strategies/{id}/config` | Update configuration for a specific strategy | Required |
| POST | `/strategies/{id}/analyze` | Analyze files using a specific strategy | Required |
| POST | `/strategies/{id}/execute` | Execute a strategy on files | Required |

#### Example Usage

**Get All Strategies:**
```http
GET /strategies
```

**Get Strategy Info:**
```http
GET /strategies/file-collection
```

**Get Strategy Config:**
```http
GET /strategies/file-collection/config
```

**Update Strategy Config:**
```http
POST /strategies/file-collection/config
Content-Type: application/json

{
  "values": {
    "targetDirectory": "/path/to/target",
    "recursive": true
  }
}
```

**Analyze Files:**
```http
POST /strategies/file-collection/analyze
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

**Execute Strategy:**
```http
POST /strategies/file-collection/execute
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

### 3. Task Management

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| POST | `/tasks` | Create a new task | Required |
| GET | `/tasks/{id}` | Get status of a specific task | Required |
| GET | `/tasks` | Get all tasks with optional filtering | Required |
| POST | `/tasks/{id}/execute` | Execute a specific task | Required |
| POST | `/tasks/{id}/cancel` | Cancel a running task | Required |
| DELETE | `/tasks/{id}` | Delete a task | Required |

#### Example Usage

**Create Task:**
```http
POST /tasks
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

**Get Task Status:**
```http
GET /tasks/task-1234567890
```

**Get All Tasks:**
```http
GET /tasks?status=RUNNING&page=1&size=20
```

**Execute Task:**
```http
POST /tasks/task-1234567890/execute
```

**Cancel Task:**
```http
POST /tasks/task-1234567890/cancel
```

**Delete Task:**
```http
DELETE /tasks/task-1234567890
```

### 4. Plugin Management

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| GET | `/plugins` | Get all available plugins | Required |
| GET | `/plugins/{id}` | Get information about a specific plugin | Required |
| GET | `/plugins/{id}/config` | Get configuration for a specific plugin | Required |
| POST | `/plugins/{id}/config` | Update configuration for a specific plugin | Required |
| POST | `/plugins/{id}/execute` | Execute a plugin on files | Required |
| POST | `/plugins/reload` | Reload all plugins | Required |

#### Example Usage

**Get All Plugins:**
```http
GET /plugins
```

**Get Plugin Info:**
```http
GET /plugins/file-cleanup
```

**Get Plugin Config:**
```http
GET /plugins/file-cleanup/config
```

**Update Plugin Config:**
```http
POST /plugins/file-cleanup/config
Content-Type: application/json

{
  "values": {
    "maxFileAgeDays": 30,
    "deleteEmptyDirectories": true
  }
}
```

**Execute Plugin:**
```http
POST /plugins/file-cleanup/execute
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

**Reload Plugins:**
```http
POST /plugins/reload
```

### 5. Configuration Management

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| GET | `/config` | Get all configuration settings | Required |
| GET | `/config/{key}` | Get a specific configuration value | Required |
| POST | `/config` | Update multiple configuration settings | Required |
| POST | `/config/{key}` | Update a specific configuration value | Required |
| DELETE | `/config/{key}` | Delete a specific configuration value | Required |
| DELETE | `/config` | Clear all configuration settings | Required |

#### Example Usage

**Get All Configs:**
```http
GET /config
```

**Get Specific Config:**
```http
GET /config/maxConcurrentTasks
```

**Update Multiple Configs:**
```http
POST /config
Content-Type: application/json

{
  "maxConcurrentTasks": 5,
  "defaultScanDepth": 3
}
```

**Update Specific Config:**
```http
POST /config/maxConcurrentTasks
Content-Type: application/json

10
```

**Delete Specific Config:**
```http
DELETE /config/oldSetting
```

**Clear All Configs:**
```http
DELETE /config
```

### 6. Log Management

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| GET | `/logs` | Get all logs with optional filtering | Required |
| POST | `/logs` | Add a new log entry | Required |
| DELETE | `/logs` | Clear all logs | Required |

#### Example Usage

**Get Logs:**
```http
GET /logs?level=ERROR&source=plugin&page=1&size=50
```

**Add Log Entry:**
```http
POST /logs
Content-Type: application/json

{
  "level": "INFO",
  "message": "Task completed successfully",
  "source": "task-service"
}
```

**Clear Logs:**
```http
DELETE /logs
```

## Error Handling

All API endpoints return appropriate HTTP status codes to indicate the success or failure of the request:

| Status Code | Description |
|-------------|-------------|
| 200 OK | Request completed successfully |
| 400 Bad Request | Invalid request parameters or format |
| 401 Unauthorized | Authentication required |
| 404 Not Found | Resource not found |
| 500 Internal Server Error | Server-side error occurred |

Error responses typically include a JSON object with error details:

```json
{
  "error": "Error message describing the issue"
}
```

## Rate Limiting

Currently, there is no rate limiting implemented. This may be added in future versions to prevent abuse.

## Versioning

The API does not currently use versioning in the URL path. Future changes will maintain backward compatibility or introduce versioning as needed.

## WebSocket Endpoints

In addition to RESTful endpoints, the backend also provides WebSocket endpoints for real-time communication:

| Endpoint | Description |
|----------|-------------|
| `/ws/progress` | Real-time task progress updates |
| `/ws/task` | Real-time task status updates |
| `/ws/file` | Real-time file operation updates |

These WebSocket endpoints use the STOMP protocol for communication.

## Conclusion

This API documentation provides a comprehensive overview of the endpoints available in the FileManager Plus backend service. For more detailed information about specific endpoints or their implementation, refer to the source code or contact the development team.
