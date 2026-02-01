# 老架构流程设计思路与代码实现

## 1. 老架构设计概述

老架构是基于JavaFX的桌面应用，具有完整的文件管理和处理流程。本文档将详细介绍老架构下的核心流程设计思路，并与新架构进行对比分析。

## 2. 核心流程设计

### 2.1 执行流程设计

#### 老架构执行流程

```
用户选择目录 → 配置策略 → 预览分析 → 确认执行 → 执行完成
```

**详细步骤：**

1. **目录选择**
   - 用户通过文件选择器选择目标目录
   - 系统验证目录有效性
   - 目录路径持久化存储

2. **策略配置**
   - 用户选择需要执行的策略
   - 系统加载策略默认参数
   - 用户根据需求调整参数
   - 参数配置持久化存储

3. **预览分析**
   - 系统扫描目录下的文件
   - 应用策略分析文件
   - 生成变更预览
   - 显示变更统计信息

4. **确认执行**
   - 用户查看预览结果
   - 确认需要执行的变更
   - 系统验证执行条件

5. **执行完成**
   - 系统执行变更操作
   - 显示执行进度
   - 生成执行报告
   - 更新文件状态

#### 新架构对应实现

**代码位置：** `backend/src/main/java/com/filemanager/backend/controller/PipelineController.java`

```java
@PostMapping("/analyze")
public ResponseEntity<Map<String, Object>> analyzePipeline(@RequestBody Map<String, Object> request) {
    try {
        if (taskManager.isTaskRunning()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "已有任务在运行，请先中止");
            return ResponseEntity.badRequest().body(result);
        }

        currentChanges.clear();
        taskManager.clearAllTasks();

        List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
        List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

        if (sourceDirectories == null || sourceDirectories.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "源目录不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        if (pipeline == null || pipeline.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "流水线不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        String taskId = taskManager.createTask("preview");
        taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEWING);
        taskManager.setCurrentTaskRunning(true);
        taskManager.updateTaskStep(taskId, "初始化预览任务");
        taskManager.updateTaskMessage(taskId, "开始分析流水线...");

        // 异步执行预览分析
        CompletableFuture.runAsync(() -> {
            try {
                // 执行预览分析逻辑
                // ...
            } catch (Exception e) {
                e.printStackTrace();
                taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEW_FAILED);
                taskManager.updateTaskMessage(taskId, "预览失败: " + e.getMessage());
            } finally {
                taskManager.setCurrentTaskRunning(false);
            }
        }, executorService);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskId", taskId);
        result.put("message", "预览任务已开始执行");
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
```

### 2.2 目录选择流程

#### 老架构目录选择

- **目录验证**：确保选择的是目录而非文件
- **路径标准化**：统一路径格式
- **持久化存储**：保存用户选择的目录
- **批量选择**：支持多选目录

#### 新架构对应实现

**代码位置：** `backend/src/main/java/com/filemanager/backend/controller/SourceDirectoryController.java`

```java
@PostMapping
public ResponseEntity<Map<String, Object>> addSourceDirectory(@RequestBody Map<String, Object> request) {
    try {
        String path = (String) request.get("path");
        int threadCount = (Integer) request.getOrDefault("threadCount", 4);

        // 检查路径是否已存在
        for (SourceDirectory dir : sourceDirectories) {
            if (dir.getPath().equals(path)) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("message", "路径已存在");
                return ResponseEntity.badRequest().body(errorResult);
            }
        }

        sourceDirectories.add(new SourceDirectory(path, threadCount));
        saveSourceDirectoriesConfig(); // 持久化存储
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "源目录添加成功");
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
```

### 2.3 策略配置流程

#### 老架构策略配置

- **参数验证**：确保必填参数已配置
- **默认值填充**：未配置参数使用默认值
- **条件参数**：根据其他参数值动态显示/隐藏参数
- **模块化配置**：支持复杂策略的模块化配置

#### 新架构对应实现

**代码位置：** `backend/src/main/java/com/filemanager/backend/service/impl/StrategyServiceImpl.java`

```java
@Override
public StrategyConfigDTO getStrategyConfig(String strategyId) {
    logger.info("[Service] 获取策略配置 - strategyId: {}", strategyId);
    StrategyConfigDTO config = strategyConfigs.get(strategyId);
    if (config == null) {
        logger.info("[Service] 策略配置不存在，创建默认配置 - strategyId: {}", strategyId);
        config = new StrategyConfigDTO();
        // 设置默认配置
        switch (strategyId) {
            case "file-collection":
                config.setValue("targetDirectory", "/tmp/collected");
                config.setValue("recursive", true);
                break;
            case "metadata-scraper":
                config.setValue("source", "本地推断 (仅生成清单)");
                config.setValue("threads", 4);
                config.setValue("lyricsEnabled", true);
                config.setValue("coverEnabled", true);
                config.setValue("albumInfoEnabled", true);
                config.setValue("maxRequests", 10);
                config.setValue("periodMs", 1000);
                break;
            // 其他策略默认配置...
        }
        strategyConfigs.put(strategyId, config);
    }
    logger.info("[Service] 返回策略配置 - strategyId: {}, 配置项数量: {}", strategyId, config.getConfigValues() != null ? config.getConfigValues().size() : 0);
    return config;
}
```

### 2.4 预览分析流程

#### 老架构预览分析

- **文件扫描**：递归扫描目录下的文件
- **策略应用**：对每个文件应用策略分析
- **变更生成**：生成文件变更记录
- **统计分析**：计算变更统计信息
- **进度反馈**：实时显示扫描和分析进度

#### 新架构对应实现

**代码位置：** `backend/src/main/java/com/filemanager/backend/controller/PipelineController.java`

```java
CompletableFuture.runAsync(() -> {
    try {
        System.out.println("[Pipeline] 开始预览分析，任务ID: " + taskId);
        System.out.println("[Pipeline] 源目录: " + sourceDirectories);
        System.out.println("[Pipeline] 流水线节点数量: " + pipeline.size());

        taskManager.updateTaskStep(taskId, "输出流水线配置信息");
        StringBuilder configSummary = new StringBuilder();
        configSummary.append("=== 流水线配置信息 ===\n");
        configSummary.append("源目录数量: " + sourceDirectories.size() + "\n");
        for (int i = 0; i < sourceDirectories.size(); i++) {
            configSummary.append("  目录" + (i + 1) + ": " + sourceDirectories.get(i) + "\n");
        }
        configSummary.append("流水线节点数量: " + pipeline.size() + "\n");
        
        for (int i = 0; i < pipeline.size(); i++) {
            Map<String, Object> pluginConfig = pipeline.get(i);
            String pluginId = (String) pluginConfig.get("pluginId");
            Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");
            configSummary.append("  节点" + (i + 1) + ": " + pluginId);
            if (configMap != null && !configMap.isEmpty()) {
                configSummary.append(" (参数: " + configMap.size() + "个)");
            }
            configSummary.append("\n");
        }
        
        System.out.println(configSummary.toString());
        taskManager.updateTaskLogMessage(taskId, configSummary.toString());

        taskManager.updateTaskStep(taskId, "扫描文件");
        taskManager.updateTaskMessage(taskId, "正在扫描文件...");
        
        List<ChangeRecord> allChanges = new ArrayList<>();
        int totalFiles = 0;
        int scannedFiles = 0;
        
        for (String directory : sourceDirectories) {
            File dir = new File(directory);
            if (dir.exists() && dir.isDirectory()) {
                int fileCount = countFiles(dir);
                totalFiles += fileCount;
                System.out.println("[Pipeline] 目录 " + directory + " 包含 " + fileCount + " 个文件");
            }
        }
        
        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), 0, totalFiles);

        int completed = 0;
        for (Map<String, Object> pluginConfig : pipeline) {
            if (!taskManager.isTaskRunning()) {
                taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                taskManager.updateTaskMessage(taskId, "任务已中止");
                break;
            }

            String pluginId = (String) pluginConfig.get("pluginId");
            Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");

            taskManager.updateTaskStep(taskId, "执行节点: " + pluginId);
            taskManager.updateTaskMessage(taskId, "正在执行节点: " + pluginId);
            System.out.println("[Pipeline] 执行节点: " + pluginId);

            PluginConfigDTO config = new PluginConfigDTO();
            if (configMap != null) {
                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                    config.setValue(entry.getKey(), entry.getValue());
                }
            }

            List<ChangeRecord> changes = pluginService.previewPlugin(pluginId, sourceDirectories, config);
            allChanges.addAll(changes);
            completed++;

            scannedFiles = (int) ((double) completed / pipeline.size() * totalFiles);
            taskManager.updateTaskProgress(taskId, completed, pipeline.size());
            taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), scannedFiles, totalFiles);
            
            String progressMessage = String.format("节点 %d/%d 完成，发现 %d 个变更", completed, pipeline.size(), changes.size());
            taskManager.updateTaskMessage(taskId, progressMessage);
            System.out.println("[Pipeline] " + progressMessage);
        }

        currentChanges.addAll(allChanges);
        
        if (taskManager.isTaskRunning()) {
            taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEW_COMPLETED);
            taskManager.updateTaskMessage(taskId, "预览完成，共发现 " + allChanges.size() + " 个变更");
            taskManager.updateTaskChanges(taskId, !allChanges.isEmpty(), allChanges.size());
            taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), totalFiles, totalFiles);
            System.out.println("[Pipeline] 预览完成，共发现 " + allChanges.size() + " 个变更");
        } else {
            taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
            taskManager.updateTaskMessage(taskId, "任务已中止");
        }
    } catch (Exception e) {
        e.printStackTrace();
        taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEW_FAILED);
        taskManager.updateTaskMessage(taskId, "预览失败: " + e.getMessage());
        System.err.println("[Pipeline] 预览失败: " + e.getMessage());
    } finally {
        taskManager.setCurrentTaskRunning(false);
    }
}, executorService);
```

### 2.5 执行流程

#### 老架构执行

- **任务创建**：创建执行任务
- **执行准备**：验证执行条件
- **执行过程**：按顺序执行变更
- **进度反馈**：实时显示执行进度
- **异常处理**：处理执行过程中的异常
- **结果处理**：生成执行结果报告

#### 新架构对应实现

**代码位置：** `backend/src/main/java/com/filemanager/backend/controller/PipelineController.java`

```java
@PostMapping("/execute")
public ResponseEntity<Map<String, Object>> executePipeline(@RequestBody Map<String, Object> request) {
    try {
        if (taskManager.isTaskRunning()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "已有任务在运行，请先中止");
            return ResponseEntity.badRequest().body(result);
        }

        currentChanges.clear();
        taskManager.clearAllTasks();

        List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
        List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

        if (sourceDirectories == null || sourceDirectories.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "源目录不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        if (pipeline == null || pipeline.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "流水线不能为空");
            return ResponseEntity.badRequest().body(result);
        }

        String taskId = taskManager.createTask("execute");
        taskManager.updateTaskStatus(taskId, TaskStatus.EXECUTING);
        taskManager.setCurrentTaskRunning(true);
        taskManager.updateTaskStep(taskId, "初始化执行任务");
        taskManager.updateTaskMessage(taskId, "开始执行流水线...");

        // 异步执行任务
        CompletableFuture.runAsync(() -> {
            try {
                // 执行流水线逻辑
                // ...
            } catch (Exception e) {
                e.printStackTrace();
                taskManager.updateTaskStatus(taskId, TaskStatus.EXECUTION_FAILED);
                taskManager.updateTaskMessage(taskId, "执行失败: " + e.getMessage());
            } finally {
                taskManager.setCurrentTaskRunning(false);
            }
        }, executorService);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("taskId", taskId);
        result.put("message", "执行任务已开始执行");
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
```

## 3. 状态管理设计

### 3.1 任务状态流程

#### 老架构状态管理

- **准备状态**：任务初始化，等待执行
- **执行状态**：任务正在执行
- **完成状态**：任务执行完成
- **失败状态**：任务执行失败
- **取消状态**：任务被用户取消

#### 新架构对应实现

**代码位置：** `backend/src/main/java/com/filemanager/domain/enums/TaskStatus.java`

```java
public enum TaskStatus {
    /**
     * 准备就绪 - 任务已创建，等待开始执行
     */
    READY("准备就绪"),
    
    /**
     * 预览中 - 正在进行预览分析
     */
    PREVIEWING("预览中"),
    
    /**
     * 预览完成 - 预览分析已完成
     */
    PREVIEW_COMPLETED("预览完成"),
    
    /**
     * 预览失败 - 预览分析失败
     */
    PREVIEW_FAILED("预览失败"),
    
    /**
     * 执行中 - 正在执行任务
     */
    EXECUTING("执行中"),
    
    /**
     * 执行完成 - 任务执行完成
     */
    EXECUTION_COMPLETED("执行完成"),
    
    /**
     * 执行失败 - 任务执行失败
     */
    EXECUTION_FAILED("执行失败"),
    
    /**
     * 已中止 - 任务被用户中止
     */
    CANCELLED("已中止");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public boolean canTransitionTo(TaskStatus target) {
        switch (this) {
            case READY:
                return target == PREVIEWING || target == CANCELLED;
            case PREVIEWING:
                return target == PREVIEW_COMPLETED || target == PREVIEW_FAILED || target == CANCELLED;
            case PREVIEW_COMPLETED:
                return target == EXECUTING || target == CANCELLED;
            case PREVIEW_FAILED:
                return target == PREVIEWING || target == CANCELLED;
            case EXECUTING:
                return target == EXECUTION_COMPLETED || target == EXECUTION_FAILED || target == CANCELLED;
            case EXECUTION_COMPLETED:
                return target == PREVIEWING || target == CANCELLED;
            case EXECUTION_FAILED:
                return target == PREVIEWING || target == CANCELLED;
            case CANCELLED:
                return target == PREVIEWING;
            default:
                return false;
        }
    }
    
    public boolean isCompleted() {
        return this == PREVIEW_COMPLETED || this == EXECUTION_COMPLETED;
    }
    
    public boolean isFailed() {
        return this == PREVIEW_FAILED || this == EXECUTION_FAILED;
    }
    
    public boolean isRunning() {
        return this == PREVIEWING || this == EXECUTING;
    }
    
    public String getDescription() {
        return description;
    }
}
```

## 4. 配置持久化设计

### 4.1 老架构配置持久化

- **本地存储**：使用文件系统存储配置
- **配置文件**：XML或JSON格式的配置文件
- **自动加载**：应用启动时自动加载配置
- **实时保存**：配置变更时实时保存

#### 新架构对应实现

**代码位置：** `backend/src/main/java/com/filemanager/backend/controller/SourceDirectoryController.java`

```java
private void loadSourceDirectoriesConfig() {
    try {
        File configFile = new File(configFilePath);
        if (configFile.exists()) {
            System.out.println("[SourceDirectory] 找到配置文件，开始加载: " + configFilePath);
            FileReader reader = new FileReader(configFile);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> configList = mapper.readValue(reader, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            sourceDirectories.clear();
            for (Map<String, Object> config : configList) {
                String path = (String) config.get("path");
                int threadCount = (Integer) config.getOrDefault("threadCount", 4);
                sourceDirectories.add(new SourceDirectory(path, threadCount));
            }
            reader.close();
            System.out.println("[SourceDirectory] 配置加载成功，源目录数量: " + sourceDirectories.size());
        } else {
            System.out.println("[SourceDirectory] 配置文件不存在，使用默认空配置: " + configFilePath);
            sourceDirectories.clear();
        }
    } catch (Exception e) {
        System.err.println("[SourceDirectory] 配置加载失败: " + e.getMessage());
        e.printStackTrace();
        sourceDirectories.clear();
    }
}

private void saveSourceDirectoriesConfig() {
    try {
        List<Map<String, Object>> configList = new ArrayList<>();
        for (SourceDirectory dir : sourceDirectories) {
            Map<String, Object> config = new HashMap<>();
            config.put("path", dir.getPath());
            config.put("threadCount", dir.getThreadCount());
            configList.add(config);
        }
        File configFile = new File(configFilePath);
        FileWriter writer = new FileWriter(configFile);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.writeValue(writer, configList);
        writer.close();
        System.out.println("[SourceDirectory] 配置保存成功，源目录数量: " + sourceDirectories.size());
    } catch (Exception e) {
        System.err.println("[SourceDirectory] 配置保存失败: " + e.getMessage());
        e.printStackTrace();
    }
}
```

## 5. 前后端交互设计

### 5.1 老架构交互设计

- **同步交互**：用户操作后等待响应
- **进度对话框**：长时间操作显示进度对话框
- **结果反馈**：操作完成后显示结果
- **错误处理**：异常情况弹出错误对话框

#### 新架构对应实现

**代码位置：** `clients/flutter-web-cli/lib/pages/preview_page.dart`

```dart
Future<void> _analyzePipeline() async {
  if (!_validateConfiguration()) {
    return;
  }

  if (!_validatePipelineParameters()) {
    return;
  }

  setState(() {
    _taskState = TaskState.previewing;
    _errorMessage = '';
    _progress = 0;
    _remainingTime = '计算中...';
    _currentStep = '初始化预览任务';
    _message = '开始分析流水线...';
    _hasChanges = false;
    _changeCount = 0;
    _scannedFiles = 0;
    _totalFiles = 0;
    _logMessage = '';
  });

  try {
    final sourcePaths = _sourceDirectories.map((d) => d.path).toList();
    final result = await _pipelineService.analyzePipeline(sourcePaths, _pipeline);

    if (result['success'] == true) {
      _taskId = result['taskId'];
      _showSuccess(result['message'] ?? '分析任务已开始执行');
      
      await _fetchChanges();
      _startStatusTimer();
    } else {
      setState(() {
        _taskState = TaskState.previewFailed;
        _errorMessage = result['message'] ?? '分析任务提交失败';
      });
      _showError(_errorMessage);
    }
  } catch (e) {
    setState(() {
      _taskState = TaskState.previewFailed;
      _errorMessage = '分析流水线失败: $e';
    });
    _showError(_errorMessage);
  }
}

void _startStatusTimer() {
  _statusTimer?.cancel();
  _statusTimer = Timer.periodic(Duration(seconds: 1), (timer) async {
    if (!mounted) {
      timer.cancel();
      return;
    }

    try {
      final status = await _pipelineService.getPipelineStatus();
      setState(() {
        _progress = status['progress'] ?? 0;
        _remainingTime = status['remainingTime'] ?? '00:00:00';
        _currentStep = status['currentStep'] ?? '';
        _message = status['message'] ?? '';
        _hasChanges = status['hasChanges'] ?? false;
        _changeCount = status['changeCount'] ?? 0;
        _currentDirectory = status['currentDirectory'] ?? '';
        _scannedFiles = status['scannedFiles'] ?? 0;
        _totalFiles = status['totalFiles'] ?? 0;
        _logMessage = status['logMessage'] ?? '';

        // 更新任务状态
        final statusStr = status['status'];
        if (statusStr != null) {
          switch (statusStr) {
            case 'PREVIEWING':
              _taskState = TaskState.previewing;
              break;
            case 'PREVIEW_COMPLETED':
              _taskState = TaskState.previewCompleted;
              break;
            case 'PREVIEW_FAILED':
              _taskState = TaskState.previewFailed;
              break;
            case 'EXECUTING':
              _taskState = TaskState.executing;
              break;
            case 'EXECUTION_COMPLETED':
              _taskState = TaskState.executionCompleted;
              break;
            case 'EXECUTION_FAILED':
              _taskState = TaskState.executionFailed;
              break;
            case 'CANCELLED':
              _taskState = TaskState.cancelled;
              break;
            default:
              _taskState = TaskState.ready;
          }
        }
      });
    } catch (e) {
      print('Error fetching status: $e');
    }
  });
}
```

## 6. 老架构与新架构对比

| 特性 | 老架构 | 新架构 | 优势 |
|------|--------|--------|------|
| 架构类型 | 桌面应用 (JavaFX) | 前后端分离 (Spring Boot + Flutter Web) | 跨平台、易于部署和维护 |
| 执行流程 | 同步执行 | 异步执行 | 更好的用户体验，支持多任务 |
| 状态管理 | 本地状态 | 服务端状态管理 | 更可靠的状态跟踪，支持任务恢复 |
| 配置持久化 | 本地文件 | 服务端文件 | 配置集中管理，多客户端共享 |
| 扩展性 | 插件系统 | 插件系统 + 策略系统 | 更灵活的扩展机制 |
| 交互方式 | 桌面GUI | Web界面 | 跨设备访问，无需安装 |
| 性能 | 本地执行 | 服务端执行 | 服务端资源更强大，支持并发 |

## 7. 核心代码实现要点

### 7.1 目录选择实现要点

- **路径验证**：确保选择的是有效目录
- **路径标准化**：统一路径格式，处理不同操作系统的路径差异
- **持久化存储**：使用文件系统存储目录配置
- **批量处理**：支持多个目录的批量处理

### 7.2 策略配置实现要点

- **参数验证**：确保必填参数已配置
- **默认值处理**：为未配置参数提供合理的默认值
- **条件参数**：根据其他参数值动态调整参数显示
- **模块化配置**：支持复杂策略的模块化配置结构

### 7.3 预览分析实现要点

- **文件扫描**：递归扫描目录下的文件，支持大目录的高效扫描
- **并行处理**：使用多线程提高扫描和分析效率
- **变更生成**：准确生成文件变更记录
- **进度反馈**：实时显示扫描和分析进度
- **内存管理**：处理大量文件时的内存优化

### 7.4 执行实现要点

- **事务管理**：确保执行过程的原子性
- **异常处理**：优雅处理执行过程中的异常
- **回滚机制**：支持执行失败时的回滚
- **进度跟踪**：实时跟踪执行进度
- **结果生成**：生成详细的执行结果报告

### 7.5 状态管理实现要点

- **状态定义**：明确定义任务的各种状态
- **状态转换**：严格控制状态之间的转换
- **状态持久化**：确保服务重启后状态不丢失
- **状态查询**：提供状态查询接口供前端使用

### 7.6 配置持久化实现要点

- **文件格式**：使用JSON格式存储配置
- **自动加载**：服务启动时自动加载配置
- **实时保存**：配置变更时实时保存
- **错误处理**：处理配置加载和保存过程中的错误
- **版本兼容**：支持不同版本配置的兼容处理

## 8. 最佳实践与设计模式

### 8.1 设计模式应用

- **策略模式**：用于实现不同的文件处理策略
- **插件模式**：用于扩展系统功能
- **观察者模式**：用于实现状态变化的通知
- **命令模式**：用于封装文件操作命令
- **工厂模式**：用于创建不同类型的任务

### 8.2 最佳实践

- **配置分离**：将配置与代码分离，便于维护
- **模块化设计**：将系统分解为可独立维护的模块
- **错误处理**：统一的错误处理机制
- **日志记录**：详细的日志记录，便于调试和监控
- **性能优化**：针对文件操作的性能优化
- **安全性**：文件操作的安全检查

### 8.3 代码规范

- **命名规范**：清晰的命名，便于理解
- **代码结构**：合理的代码结构，便于维护
- **注释规范**：详细的注释，便于理解和维护
- **异常处理**：规范的异常处理机制
- **测试覆盖**：充分的测试覆盖，确保代码质量

## 9. 总结

老架构的流程设计思路为新架构提供了重要的参考基础，新架构在保留老架构核心流程的同时，通过前后端分离、异步执行、服务端状态管理等技术手段，提供了更灵活、更高效、更可靠的文件管理和处理能力。

通过本文档的梳理，我们可以看到老架构的设计思想在新架构中的传承和发展，以及新架构如何通过现代技术手段解决老架构面临的挑战。这些设计思路和代码实现经验，对于系统的后续发展和维护具有重要的参考价值。
