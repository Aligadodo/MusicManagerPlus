# 服务端模块 API 接口和实现方案

## 1. 服务端架构概述

### 1.1 设计目标
- **提供统一的API接口**：为多端客户端（JavaFX、Flutter Web）提供一致的接口
- **支持实时通信**：通过WebSocket提供任务状态的实时更新
- **高性能**：支持大文件处理和并发任务执行
- **安全性**：实现认证授权和访问控制
- **可扩展性**：支持插件系统和未来功能扩展

### 1.2 技术选型
- **框架**：Spring Boot 3.2+
- **语言**：Java 21+
- **API**：RESTful + WebSocket
- **构建工具**：Maven 3.9+
- **依赖注入**：Spring IoC
- **安全**：Spring Security + JWT
- **测试**：JUnit 5 + Mockito

## 2. 模块结构

```
backend/
├── src/main/java/com/filemanager/backend/
│   ├── config/            # 配置类
│   │   ├── AppConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── WebSocketConfig.java
│   │   └── ...
│   ├── controller/        # API 控制器
│   │   ├── FileController.java
│   │   ├── StrategyController.java
│   │   ├── TaskController.java
│   │   ├── PluginController.java
│   │   ├── ConfigController.java
│   │   ├── LogController.java
│   │   └── ws/            # WebSocket 控制器
│   │       ├── TaskWebSocketHandler.java
│   │       └── WebSocketConfig.java
│   ├── service/           # 服务层
│   │   ├── impl/          # 具体实现
│   │   │   ├── FileServiceImpl.java
│   │   │   ├── StrategyServiceImpl.java
│   │   │   ├── TaskServiceImpl.java
│   │   │   ├── PluginServiceImpl.java
│   │   │   ├── ConfigServiceImpl.java
│   │   │   └── LogServiceImpl.java
│   │   └── ...
│   ├── repository/        # 数据访问
│   │   ├── ConfigRepository.java
│   │   ├── TaskRepository.java
│   │   ├── FileSystemRepository.java
│   │   └── ...
│   ├── model/             # 数据模型
│   │   ├── entity/        # 持久化实体
│   │   │   ├── TaskEntity.java
│   │   │   ├── ConfigEntity.java
│   │   │   └── ...
│   │   └── dto/            # 数据传输对象
│   │       ├── FileInfoDTO.java
│   │       ├── TaskRequestDTO.java
│   │       ├── TaskStatusDTO.java
│   │       └── ...
│   ├── util/              # 工具类
│   │   ├── ThreadPoolUtil.java
│   │   ├── FileOperationUtil.java
│   │   ├── SecurityUtil.java
│   │   └── ...
│   ├── exception/         # 异常处理
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ApiException.java
│   │   └── ...
│   └── Application.java    # 应用入口
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── ...
└── pom.xml                # Maven 配置
```

## 3. RESTful API 接口设计

### 3.1 文件操作 API

#### 3.1.1 目录扫描
- **接口**：`GET /api/files/scan`
- **参数**：
  - `path`：目录路径（必填）
  - `minDepth`：最小深度（可选，默认 0）
  - `maxDepth`：最大深度（可选，默认 3）
  - `pattern`：文件匹配模式（可选）
- **返回**：`List<FileInfoDTO>`
- **权限**：无需认证

#### 3.1.2 文件信息
- **接口**：`GET /api/files/info`
- **参数**：
  - `path`：文件路径（必填）
- **返回**：`FileInfoDTO`
- **权限**：无需认证

#### 3.1.3 文件存在检查
- **接口**：`POST /api/files/exists`
- **请求体**：`{ "paths": ["path1", "path2"] }`
- **返回**：`Map<String, Boolean>`
- **权限**：无需认证

#### 3.1.4 文件操作
- **接口**：`POST /api/files/operation`
- **请求体**：
  ```json
  {
    "operation": "copy|move|delete|rename",
    "source": "source/path",
    "target": "target/path",
    "options": {}
  }
  ```
- **返回**：`{ "success": true, "message": "操作成功" }`
- **权限**：需要认证

### 3.2 策略/插件 API

#### 3.2.1 获取可用策略列表
- **接口**：`GET /api/strategies`
- **返回**：`List<StrategyInfoDTO>`
- **权限**：无需认证

#### 3.2.2 获取策略配置
- **接口**：`GET /api/strategies/{id}/config`
- **参数**：
  - `id`：策略 ID（必填）
- **返回**：`StrategyConfigDTO`
- **权限**：无需认证

#### 3.2.3 更新策略配置
- **接口**：`POST /api/strategies/{id}/config`
- **参数**：
  - `id`：策略 ID（必填）
- **请求体**：`StrategyConfigDTO`
- **返回**：`{ "success": true, "message": "配置更新成功" }`
- **权限**：需要认证

#### 3.2.4 分析文件
- **接口**：`POST /api/strategies/{id}/analyze`
- **参数**：
  - `id`：策略 ID（必填）
- **请求体**：
  ```json
  {
    "files": ["file1/path", "file2/path"],
    "config": {}
  }
  ```
- **返回**：`List<ChangeRecord>`
- **权限**：需要认证

### 3.3 任务 API

#### 3.3.1 创建任务
- **接口**：`POST /api/tasks`
- **请求体**：`TaskRequestDTO`
- **返回**：`{ "taskId": "task-123" }`
- **权限**：需要认证

#### 3.3.2 获取任务状态
- **接口**：`GET /api/tasks/{id}`
- **参数**：
  - `id`：任务 ID（必填）
- **返回**：`TaskStatusDTO`
- **权限**：需要认证

#### 3.3.3 获取任务列表
- **接口**：`GET /api/tasks`
- **参数**：
  - `status`：任务状态过滤（可选）
  - `page`：页码（可选，默认 1）
  - `size`：每页大小（可选，默认 20）
- **返回**：`Page<TaskStatusDTO>`
- **权限**：需要认证

#### 3.3.4 执行任务
- **接口**：`POST /api/tasks/{id}/execute`
- **参数**：
  - `id`：任务 ID（必填）
- **返回**：`{ "success": true, "message": "任务开始执行" }`
- **权限**：需要认证

#### 3.3.5 取消任务
- **接口**：`POST /api/tasks/{id}/cancel`
- **参数**：
  - `id`：任务 ID（必填）
- **返回**：`{ "success": true, "message": "任务已取消" }`
- **权限**：需要认证

### 3.4 配置 API

#### 3.4.1 获取全局配置
- **接口**：`GET /api/config/global`
- **返回**：`GlobalConfigDTO`
- **权限**：需要认证

#### 3.4.2 更新全局配置
- **接口**：`POST /api/config/global`
- **请求体**：`GlobalConfigDTO`
- **返回**：`{ "success": true, "message": "配置更新成功" }`
- **权限**：需要认证

#### 3.4.3 获取用户配置
- **接口**：`GET /api/config/user`
- **返回**：`UserConfigDTO`
- **权限**：需要认证

#### 3.4.4 更新用户配置
- **接口**：`POST /api/config/user`
- **请求体**：`UserConfigDTO`
- **返回**：`{ "success": true, "message": "配置更新成功" }`
- **权限**：需要认证

### 3.5 日志 API

#### 3.5.1 获取操作日志
- **接口**：`GET /api/logs/operation`
- **参数**：
  - `level`：日志级别（可选）
  - `startTime`：开始时间（可选）
  - `endTime`：结束时间（可选）
  - `page`：页码（可选，默认 1）
  - `size`：每页大小（可选，默认 50）
- **返回**：`Page<LogEntryDTO>`
- **权限**：需要认证

#### 3.5.2 获取错误日志
- **接口**：`GET /api/logs/error`
- **参数**：
  - `startTime`：开始时间（可选）
  - `endTime`：结束时间（可选）
  - `page`：页码（可选，默认 1）
  - `size`：每页大小（可选，默认 50）
- **返回**：`Page<LogEntryDTO>`
- **权限**：需要认证

## 4. WebSocket 接口设计

### 4.1 任务状态更新
- **端点**：`/ws/tasks/{taskId}`
- **消息格式**：
  ```json
  {
    "taskId": "task-123",
    "status": "RUNNING|SUCCESS|FAILED|CANCELLED",
    "progress": 0.75,
    "message": "正在处理文件...",
    "timestamp": "2026-01-30T12:00:00Z"
  }
  ```
- **权限**：需要认证

### 4.2 全局进度更新
- **端点**：`/ws/progress`
- **消息格式**：
  ```json
  {
    "totalTasks": 10,
    "completedTasks": 3,
    "runningTasks": 2,
    "failedTasks": 0,
    "totalProgress": 0.3,
    "timestamp": "2026-01-30T12:00:00Z"
  }
  ```
- **权限**：需要认证

### 4.3 文件操作进度
- **端点**：`/ws/files/{operationId}`
- **消息格式**：
  ```json
  {
    "operationId": "op-123",
    "operation": "upload|download|scan",
    "progress": 0.6,
    "currentFile": "file.txt",
    "totalFiles": 10,
    "completedFiles": 6,
    "timestamp": "2026-01-30T12:00:00Z"
  }
  ```
- **权限**：需要认证

## 5. 服务层实现

### 5.1 核心服务

#### 5.1.1 FileService

```java
package com.filemanager.backend.service;

import com.filemanager.domain.dto.FileInfoDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;
import java.util.Map;

public interface FileService {
    
    /**
     * 扫描目录
     */
    List<FileInfoDTO> scanDirectory(String path, int minDepth, int maxDepth, String pattern);
    
    /**
     * 获取文件信息
     */
    FileInfoDTO getFileInfo(String path);
    
    /**
     * 检查文件是否存在
     */
    Map<String, Boolean> checkExists(List<String> paths);
    
    /**
     * 复制文件
     */
    boolean copy(String source, String target);
    
    /**
     * 移动文件
     */
    boolean move(String source, String target);
    
    /**
     * 删除文件
     */
    boolean delete(String path);
    
    /**
     * 重命名文件
     */
    boolean rename(String source, String target);
    
    /**
     * 获取文件内容
     */
    byte[] getFileContent(String path);
    
    /**
     * 写入文件内容
     */
    boolean writeFileContent(String path, byte[] content);
}
```

#### 5.1.2 StrategyService

```java
package com.filemanager.backend.service;

import com.filemanager.domain.dto.StrategyInfoDTO;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public interface StrategyService {
    
    /**
     * 获取可用策略列表
     */
    List<StrategyInfoDTO> getAvailableStrategies();
    
    /**
     * 获取策略信息
     */
    StrategyInfoDTO getStrategyInfo(String strategyId);
    
    /**
     * 获取策略配置
     */
    StrategyConfigDTO getStrategyConfig(String strategyId);
    
    /**
     * 更新策略配置
     */
    boolean updateStrategyConfig(String strategyId, StrategyConfigDTO config);
    
    /**
     * 分析文件
     */
    List<ChangeRecord> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfigDTO config);
    
    /**
     * 执行策略
     */
    List<ChangeRecord> executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config);
}
```

#### 5.1.3 TaskService

```java
package com.filemanager.backend.service;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public interface TaskService {
    
    /**
     * 创建任务
     */
    String createTask(TaskRequestDTO request);
    
    /**
     * 获取任务状态
     */
    TaskStatusDTO getTaskStatus(String taskId);
    
    /**
     * 获取任务列表
     */
    List<TaskStatusDTO> getTasks(String status, int page, int size);
    
    /**
     * 执行任务
     */
    boolean executeTask(String taskId);
    
    /**
     * 取消任务
     */
    boolean cancelTask(String taskId);
    
    /**
     * 获取任务结果
     */
    List<ChangeRecord> getTaskResults(String taskId);
    
    /**
     * 删除任务
     */
    boolean deleteTask(String taskId);
}
```

#### 5.1.4 PluginService

```java
package com.filemanager.backend.service;

import com.filemanager.domain.dto.PluginInfoDTO;
import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public interface PluginService {
    
    /**
     * 获取可用插件列表
     */
    List<PluginInfoDTO> getAvailablePlugins();
    
    /**
     * 获取插件信息
     */
    PluginInfoDTO getPluginInfo(String pluginId);
    
    /**
     * 获取插件配置
     */
    PluginConfigDTO getPluginConfig(String pluginId);
    
    /**
     * 更新插件配置
     */
    boolean updatePluginConfig(String pluginId, PluginConfigDTO config);
    
    /**
     * 执行插件
     */
    List<ChangeRecord> executePlugin(String pluginId, List<String> filePaths, PluginConfigDTO config);
    
    /**
     * 重载插件
     */
    boolean reloadPlugins();
}
```

### 5.2 服务实现

#### 5.2.1 FileServiceImpl

```java
package com.filemanager.backend.service.impl;

import com.filemanager.backend.service.FileService;
import com.filemanager.domain.dto.FileInfoDTO;
import com.filemanager.exception.FileManagerException;
import com.filemanager.util.file.FileUtil;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileServiceImpl implements FileService {
    
    @Override
    public List<FileInfoDTO> scanDirectory(String path, int minDepth, int maxDepth, String pattern) {
        List<FileInfoDTO> fileInfos = new ArrayList<>();
        Path rootPath = Paths.get(path);
        
        if (!Files.exists(rootPath)) {
            throw new FileManagerException("Directory not found: " + path);
        }
        
        try {
            Files.walk(rootPath, maxDepth)
                .filter(p -> Files.isRegularFile(p) || Files.isDirectory(p))
                .filter(p -> {
                    int depth = (int) rootPath.relativize(p).getNameCount();
                    return depth >= minDepth;
                })
                .filter(p -> {
                    if (pattern == null || pattern.isEmpty()) {
                        return true;
                    }
                    return p.getFileName().toString().matches(pattern);
                })
                .forEach(p -> {
                    FileInfoDTO info = convertToFileInfo(p);
                    fileInfos.add(info);
                });
        } catch (IOException e) {
            throw new FileManagerException("Error scanning directory: " + e.getMessage());
        }
        
        return fileInfos;
    }
    
    @Override
    public FileInfoDTO getFileInfo(String path) {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            throw new FileManagerException("File not found: " + path);
        }
        return convertToFileInfo(filePath);
    }
    
    @Override
    public Map<String, Boolean> checkExists(List<String> paths) {
        Map<String, Boolean> result = new HashMap<>();
        for (String path : paths) {
            result.put(path, Files.exists(Paths.get(path)));
        }
        return result;
    }
    
    // 其他方法实现...
    
    private FileInfoDTO convertToFileInfo(Path path) {
        FileInfoDTO info = new FileInfoDTO();
        info.setPath(path.toString());
        info.setName(path.getFileName().toString());
        info.setDirectory(Files.isDirectory(path));
        
        try {
            if (Files.isRegularFile(path)) {
                info.setSize(Files.size(path));
                info.setLastModified(Files.getLastModifiedTime(path).toMillis());
            }
        } catch (IOException e) {
            // 忽略错误
        }
        
        return info;
    }
}
```

#### 5.2.2 TaskServiceImpl

```java
package com.filemanager.backend.service.impl;

import com.filemanager.backend.service.TaskService;
import com.filemanager.backend.service.StrategyService;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.type.TaskStatus;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class TaskServiceImpl implements TaskService {
    
    private final Map<String, TaskExecution> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    @Autowired
    private StrategyService strategyService;
    
    @Override
    public String createTask(TaskRequestDTO request) {
        String taskId = "task-" + System.currentTimeMillis();
        TaskExecution execution = new TaskExecution(taskId, request);
        tasks.put(taskId, execution);
        return taskId;
    }
    
    @Override
    public TaskStatusDTO getTaskStatus(String taskId) {
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        return execution.getStatus();
    }
    
    @Override
    public boolean executeTask(String taskId) {
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        
        if (execution.getStatus().getStatus() == TaskStatus.RUNNING) {
            throw new IllegalStateException("Task is already running");
        }
        
        Future<?> future = executorService.submit(() -> {
            try {
                execution.execute(strategyService);
            } catch (Exception e) {
                execution.setError(e.getMessage());
            }
        });
        
        execution.setFuture(future);
        return true;
    }
    
    // 其他方法实现...
    
    private static class TaskExecution {
        private final String taskId;
        private final TaskRequestDTO request;
        private TaskStatusDTO status;
        private Future<?> future;
        private List<ChangeRecord> results;
        
        public TaskExecution(String taskId, TaskRequestDTO request) {
            this.taskId = taskId;
            this.request = request;
            this.status = new TaskStatusDTO();
            this.status.setTaskId(taskId);
            this.status.setStatus(TaskStatus.PENDING);
            this.status.setProgress(0.0);
            this.results = new ArrayList<>();
        }
        
        public void execute(StrategyService strategyService) {
            status.setStatus(TaskStatus.RUNNING);
            status.setProgress(0.0);
            status.setMessage("Task started");
            
            try {
                // 执行任务逻辑
                results = strategyService.executeStrategy(
                    request.getStrategyId(),
                    request.getFilePaths(),
                    request.getStrategyConfig()
                );
                
                status.setStatus(TaskStatus.SUCCESS);
                status.setProgress(1.0);
                status.setMessage("Task completed successfully");
            } catch (Exception e) {
                status.setStatus(TaskStatus.FAILED);
                status.setMessage("Task failed: " + e.getMessage());
            }
        }
        
        public void setError(String errorMessage) {
            status.setStatus(TaskStatus.FAILED);
            status.setMessage(errorMessage);
        }
        
        // getters and setters...
    }
}
```

## 6. 控制器实现

### 6.1 FileController

```java
package com.filemanager.backend.controller;

import com.filemanager.backend.service.FileService;
import com.filemanager.domain.dto.FileInfoDTO;
import com.filemanager.exception.FileManagerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {
    
    @Autowired
    private FileService fileService;
    
    @GetMapping("/scan")
    public ResponseEntity<List<FileInfoDTO>> scanDirectory(
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "0") int minDepth,
            @RequestParam(required = false, defaultValue = "3") int maxDepth,
            @RequestParam(required = false) String pattern) {
        try {
            List<FileInfoDTO> fileInfos = fileService.scanDirectory(path, minDepth, maxDepth, pattern);
            return ResponseEntity.ok(fileInfos);
        } catch (FileManagerException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @GetMapping("/info")
    public ResponseEntity<FileInfoDTO> getFileInfo(@RequestParam String path) {
        try {
            FileInfoDTO fileInfo = fileService.getFileInfo(path);
            return ResponseEntity.ok(fileInfo);
        } catch (FileManagerException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @PostMapping("/exists")
    public ResponseEntity<Map<String, Boolean>> checkExists(@RequestBody Map<String, List<String>> request) {
        try {
            List<String> paths = request.get("paths");
            if (paths == null) {
                return ResponseEntity.badRequest().body(null);
            }
            Map<String, Boolean> result = fileService.checkExists(paths);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    // 其他方法...
}
```

### 6.2 TaskController

```java
package com.filemanager.backend.controller;

import com.filemanager.backend.service.TaskService;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @PostMapping
    public ResponseEntity<Map<String, String>> createTask(@RequestBody TaskRequestDTO request) {
        try {
            String taskId = taskService.createTask(request);
            return ResponseEntity.ok(Map.of("taskId", taskId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TaskStatusDTO> getTaskStatus(@PathVariable String id) {
        try {
            TaskStatusDTO status = taskService.getTaskStatus(id);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<TaskStatusDTO>> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        try {
            List<TaskStatusDTO> tasks = taskService.getTasks(status, page, size);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeTask(@PathVariable String id) {
        try {
            boolean success = taskService.executeTask(id);
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", "Task execution started"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Map.of("success", false, "message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String id) {
        try {
            boolean success = taskService.cancelTask(id);
            return ResponseEntity.ok(Map.of(
                "success", success,
                "message", "Task cancelled"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    // 其他方法...
}
```

## 7. WebSocket 实现

### 7.1 TaskWebSocketHandler

```java
package com.filemanager.backend.controller.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemanager.backend.service.TaskService;
import com.filemanager.domain.dto.TaskStatusDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskWebSocketHandler extends TextWebSocketHandler {
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> taskMonitors = new ConcurrentHashMap<>();
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String taskId = session.getUri().getPath().split("/")[3];
        sessions.put(taskId, session);
        
        // 启动任务监控
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        taskMonitors.put(taskId, executor);
        
        executor.scheduleAtFixedRate(() -> {
            try {
                if (session.isOpen()) {
                    TaskStatusDTO status = taskService.getTaskStatus(taskId);
                    String message = objectMapper.writeValueAsString(status);
                    session.sendMessage(new TextMessage(message));
                    
                    // 如果任务完成，停止监控
                    if (status.getStatus().isFinalState()) {
                        executor.shutdown();
                        taskMonitors.remove(taskId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String taskId = session.getUri().getPath().split("/")[3];
        sessions.remove(taskId);
        
        // 停止任务监控
        ScheduledExecutorService executor = taskMonitors.remove(taskId);
        if (executor != null) {
            executor.shutdown();
        }
    }
}
```

### 7.2 WebSocketConfig

```java
package com.filemanager.backend.config;

import com.filemanager.backend.controller.ws.TaskWebSocketHandler;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TaskWebSocketHandler(), "/ws/tasks/**")
                .setAllowedOrigins("*")
                .withSockJS();
        
        registry.addHandler(new ProgressWebSocketHandler(), "/ws/progress")
                .setAllowedOrigins("*")
                .withSockJS();
        
        registry.addHandler(new FileOperationWebSocketHandler(), "/ws/files/**")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
```

## 8. 安全性设计

### 8.1 认证授权
- **JWT 认证**：使用 JSON Web Token 进行无状态认证
- **角色基于访问控制**：实现不同角色的权限控制
- **API 安全**：对敏感操作进行权限验证

### 8.2 输入验证
- **请求参数验证**：使用 Spring Validation 进行参数验证
- **文件路径安全**：防止路径遍历攻击
- **SQL 注入防护**：使用参数化查询

### 8.3 传输安全
- **HTTPS**：生产环境使用 HTTPS
- **CORS 配置**：正确配置跨域资源共享
- **敏感信息加密**：加密存储敏感信息

## 9. 性能优化

### 9.1 并发处理
- **线程池优化**：根据系统资源动态调整线程池大小
- **异步处理**：使用 CompletableFuture 进行异步操作
- **批量处理**：对批量操作进行优化

### 9.2 文件操作优化
- **流式处理**：大文件使用流式处理
- **缓存机制**：缓存文件元数据和常用操作结果
- **并行扫描**：目录扫描使用并行流

### 9.3 数据库优化
- **索引优化**：为常用查询添加索引
- **连接池**：使用高性能连接池
- **批量操作**：减少数据库交互次数

## 10. 部署方案

### 10.1 本地部署
- **可执行 JAR**：使用 Spring Boot 打包为可执行 JAR
- **配置文件**：通过 application.yml 配置
- **环境变量**：支持通过环境变量覆盖配置

### 10.2 容器部署
- **Docker 镜像**：提供官方 Docker 镜像
- **Docker Compose**：支持多容器部署
- **Kubernetes**：支持 Kubernetes 部署

### 10.3 云部署
- **AWS**：支持 AWS EC2、EKS 部署
- **Azure**：支持 Azure VM、AKS 部署
- **GCP**：支持 GCP GCE、GKE 部署

## 11. 监控与日志

### 11.1 监控系统
- **Spring Boot Actuator**：提供健康检查、指标监控
- **Prometheus + Grafana**：监控系统指标和性能
- **ELK Stack**：日志收集和分析

### 11.2 日志系统
- **SLF4J + Logback**：统一日志框架
- **结构化日志**：使用 JSON 格式记录日志
- **日志级别管理**：支持动态调整日志级别

## 12. 测试策略

### 12.1 单元测试
- **JUnit 5**：单元测试框架
- **Mockito**：模拟对象
- **AssertJ**：断言库

### 12.2 集成测试
- **Spring Boot Test**：集成测试框架
- **TestContainers**：容器化测试环境

### 12.3 端到端测试
- **Selenium**：Web 端到端测试
- **Postman**：API 测试

## 13. 结论

### 13.1 设计优势
- **统一的 API 接口**：为多端客户端提供一致的接口
- **实时通信**：通过 WebSocket 提供实时任务状态更新
- **高性能**：支持大文件处理和并发任务执行
- **安全性**：实现了认证授权和访问控制
- **可扩展性**：支持插件系统和未来功能扩展

### 13.2 实施建议
1. **分阶段实施**：先实现核心 API，再扩展功能
2. **充分测试**：每个模块都要进行充分的测试
3. **监控部署**：部署后要监控系统性能和稳定性
4. **持续集成**：使用 CI/CD 工具自动化构建和部署
5. **文档完善**：保持 API 文档的及时更新

### 13.3 未来展望
- **微服务架构**：未来可考虑拆分为微服务
- **服务网格**：使用 Istio 等服务网格技术
- **云原生**：拥抱云原生技术栈
- **AI 集成**：集成 AI 能力提升文件处理智能化水平

---

**API 设计文档**：服务端模块 API 接口和实现方案  
**版本**：1.0  
**日期**：2026-01-30  
**适用范围**：FileManager Plus 项目技术迁移