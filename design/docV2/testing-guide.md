# 测试指南

## 概述

本文档提供了FileManager Plus系统的测试指南，包括测试策略、测试用例、测试方法和测试执行说明。

## 测试策略

### 测试层次

1. **单元测试**: 测试单个类和方法的功能
2. **集成测试**: 测试多个组件之间的交互
3. **端到端测试**: 测试完整的用户流程
4. **API测试**: 测试REST API端点的功能

### 测试工具

- **JUnit 5**: Java单元测试框架
- **Mockito**: Java模拟框架
- **Spring Boot Test**: Spring Boot测试支持
- **curl**: API测试工具

## 后端测试

### 控制器测试

#### FileControllerTest

测试文件操作控制器的所有端点：

```java
@Test
void testGetFiles() {
    String path = "/test/path";
    List<Map<String, Object>> expectedFiles = new ArrayList<>();
    expectedFiles.add(Map.of("name", "file1.txt", "path", "/test/path/file1.txt", "size", 1024, "isDirectory", false));

    when(fileService.getFiles(path)).thenReturn(expectedFiles);

    ResponseEntity<List<Map<String, Object>>> response = fileController.getFiles(path);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedFiles, response.getBody());
}
```

**测试用例**:
- `testGetFiles`: 测试获取文件列表
- `testScanFiles`: 测试扫描文件
- `testCreateDirectory`: 测试创建目录
- `testDeleteFile`: 测试删除文件
- `testMoveFile`: 测试移动文件
- `testCopyFile`: 测试复制文件

#### StrategyControllerTest

测试策略管理控制器的所有端点：

```java
@Test
void testGetStrategies() {
    List<StrategyInfoDTO> expectedStrategies = new ArrayList<>();
    expectedStrategies.add(new StrategyInfoDTO("strategy1", "测试策略", "测试策略描述", "1.0.0"));

    when(strategyService.getAvailableStrategies()).thenReturn(expectedStrategies);

    ResponseEntity<List<StrategyInfoDTO>> response = strategyController.getStrategies();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedStrategies, response.getBody());
}
```

**测试用例**:
- `testGetStrategies`: 测试获取所有策略
- `testGetStrategyInfo`: 测试获取策略信息
- `testGetStrategyConfig`: 测试获取策略配置
- `testUpdateStrategyConfig`: 测试更新策略配置
- `testAnalyzeFiles`: 测试分析文件
- `testExecuteStrategy`: 测试执行策略

#### TaskControllerTest

测试任务管理控制器的所有端点：

```java
@Test
void testCreateTask() {
    TaskRequestDTO request = new TaskRequestDTO();
    request.setTaskType("file-scan");
    request.setTargetPath("/test/path");
    String expectedTaskId = "task-123";

    when(taskService.createTask(request)).thenReturn(expectedTaskId);

    ResponseEntity<Map<String, String>> response = taskController.createTask(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedTaskId, response.getBody().get("taskId"));
}
```

**测试用例**:
- `testCreateTask`: 测试创建任务
- `testGetTaskStatus`: 测试获取任务状态
- `testGetTasks`: 测试获取任务列表
- `testExecuteTask`: 测试执行任务
- `testCancelTask`: 测试取消任务
- `testDeleteTask`: 测试删除任务

#### PluginControllerTest

测试插件管理控制器的所有端点：

```java
@Test
void testGetPlugins() {
    List<PluginInfoDTO> expectedPlugins = new ArrayList<>();
    expectedPlugins.add(new PluginInfoDTO("plugin1", "测试插件", "测试插件描述", "1.0.0"));

    when(pluginService.getAvailablePlugins()).thenReturn(expectedPlugins);

    ResponseEntity<List<PluginInfoDTO>> response = pluginController.getPlugins();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedPlugins, response.getBody());
}
```

**测试用例**:
- `testGetPlugins`: 测试获取所有插件
- `testGetPluginInfo`: 测试获取插件信息
- `testGetPluginConfig`: 测试获取插件配置
- `testUpdatePluginConfig`: 测试更新插件配置
- `testExecutePlugin`: 测试执行插件
- `testReloadPlugins`: 测试重新加载插件

#### ConfigControllerTest

测试配置管理控制器的所有端点：

```java
@Test
void testGetConfig() {
    Map<String, Object> expectedConfig = new HashMap<>();
    expectedConfig.put("key1", "value1");
    expectedConfig.put("key2", "value2");

    when(configService.getConfig()).thenReturn(expectedConfig);

    ResponseEntity<Map<String, Object>> response = configController.getConfig();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedConfig, response.getBody());
}
```

**测试用例**:
- `testGetConfig`: 测试获取配置
- `testUpdateConfig`: 测试更新配置
- `testResetConfig`: 测试重置配置

#### LogControllerTest

测试日志管理控制器的所有端点：

```java
@Test
void testGetLogs() {
    String level = "INFO";
    String source = "api";
    int page = 1;
    int size = 50;
    List<Map<String, Object>> expectedLogs = new ArrayList<>();
    Map<String, Object> log1 = new HashMap<>();
    log1.put("timestamp", System.currentTimeMillis());
    log1.put("level", "INFO");
    log1.put("source", "api");
    log1.put("message", "测试日志消息");
    expectedLogs.add(log1);

    when(logService.getLogs(level, source, page, size)).thenReturn(expectedLogs);

    ResponseEntity<List<Map<String, Object>>> response = logController.getLogs(level, source, page, size);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedLogs, response.getBody());
}
```

**测试用例**:
- `testGetLogs`: 测试获取日志
- `testAddLog`: 测试添加日志
- `testClearLogs`: 测试清除日志

#### SourceDirectoryControllerTest

测试源目录管理控制器的所有端点：

```java
@Test
void testGetSourceDirectories() {
    List<SourceDirectory> expectedDirectories = new ArrayList<>();
    expectedDirectories.add(new SourceDirectory("/path/to/source", 4));

    when(sourceDirectoryService.getSourceDirectories()).thenReturn(expectedDirectories);

    ResponseEntity<List<SourceDirectory>> response = sourceDirectoryController.getSourceDirectories();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedDirectories, response.getBody());
}
```

**测试用例**:
- `testGetSourceDirectories`: 测试获取源目录列表
- `testAddSourceDirectory`: 测试添加源目录
- `testRemoveSourceDirectory`: 测试移除源目录
- `testClearSourceDirectories`: 测试清除所有源目录
- `testUpdateThreadCount`: 测试更新线程数

#### PipelineControllerTest

测试流水线管理控制器的所有端点：

```java
@Test
void testGetPipeline() {
    List<StrategyConfigDTO> expectedPipeline = new ArrayList<>();
    StrategyConfigDTO strategy = new StrategyConfigDTO();
    strategy.setStrategyId("rename");
    expectedPipeline.add(strategy);

    when(pipelineService.getPipeline()).thenReturn(expectedPipeline);

    ResponseEntity<List<StrategyConfigDTO>> response = pipelineController.getPipeline();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedPipeline, response.getBody());
}
```

**测试用例**:
- `testGetPipeline`: 测试获取流水线
- `testUpdatePipeline`: 测试更新流水线
- `testAnalyzePipeline`: 测试分析流水线
- `testExecutePipeline`: 测试执行流水线

#### ThreadPoolControllerTest

测试线程池管理控制器的所有端点：

```java
@Test
void testGetThreadPoolConfig() {
    ThreadPoolConfig expectedConfig = new ThreadPoolConfig(4, 8);

    when(threadPoolService.getThreadPoolConfig()).thenReturn(expectedConfig);

    ResponseEntity<ThreadPoolConfig> response = threadPoolController.getThreadPoolConfig();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expectedConfig, response.getBody());
}
```

**测试用例**:
- `testGetThreadPoolConfig`: 测试获取线程池配置
- `testUpdatePreviewThreads`: 测试更新预览线程数
- `testUpdateExecutionThreads`: 测试更新执行线程数

### 运行测试

#### 运行所有测试

```bash
cd backend
mvn test
```

#### 运行特定测试类

```bash
cd backend
mvn test -Dtest=FileControllerTest
```

#### 运行特定测试方法

```bash
cd backend
mvn test -Dtest=FileControllerTest#testGetFiles
```

## API测试

### 文件操作API测试

#### 扫描目录

```bash
curl -X GET 'http://localhost:8080/api/files/scan?path=/tmp&minDepth=0&maxDepth=3&pattern=*.txt'
```

#### 获取文件信息

```bash
curl -X GET 'http://localhost:8080/api/files/info?path=/tmp/test.txt'
```

#### 检查文件是否存在

```bash
curl -X POST http://localhost:8080/api/files/exists \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/tmp/file1.txt", "/tmp/file2.txt"]
  }'
```

#### 文件操作

```bash
curl -X POST http://localhost:8080/api/files/operation \
  -H "Content-Type: application/json" \
  -d '{
    "operation": "copy",
    "source": "/tmp/source.txt",
    "target": "/tmp/target.txt"
  }'
```

### 策略管理API测试

#### 获取所有策略

```bash
curl -X GET http://localhost:8080/api/strategies
```

#### 获取策略信息

```bash
curl -X GET http://localhost:8080/api/strategies/file-collection
```

#### 获取策略配置

```bash
curl -X GET http://localhost:8080/api/strategies/file-collection/config
```

#### 更新策略配置

```bash
curl -X POST http://localhost:8080/api/strategies/file-collection/config \
  -H "Content-Type: application/json" \
  -d '{
    "values": {
      "targetDirectory": "/path/to/target",
      "recursive": true
    }
  }'
```

#### 分析文件

```bash
curl -X POST http://localhost:8080/api/strategies/file-collection/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "files": ["/tmp/file1.mp3", "/tmp/file2.mp3"],
    "config": {
      "values": {
        "targetDirectory": "/path/to/target"
      }
    }
  }'
```

#### 执行策略

```bash
curl -X POST http://localhost:8080/api/strategies/file-collection/execute \
  -H "Content-Type: application/json" \
  -d '{
    "files": ["/tmp/file1.mp3", "/tmp/file2.mp3"],
    "config": {
      "values": {
        "targetDirectory": "/path/to/target"
      }
    }
  }'
```

### 任务管理API测试

#### 创建任务

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": "file-collection",
    "filePaths": ["/tmp/file1.mp3", "/tmp/file2.mp3"],
    "strategyConfig": {
      "values": {
        "targetDirectory": "/path/to/target"
      }
    }
  }'
```

#### 获取任务状态

```bash
curl -X GET http://localhost:8080/api/tasks/task-1234567890
```

#### 获取所有任务

```bash
curl -X GET 'http://localhost:8080/api/tasks?status=RUNNING&page=1&size=20'
```

#### 执行任务

```bash
curl -X POST http://localhost:8080/api/tasks/task-1234567890/execute
```

#### 取消任务

```bash
curl -X POST http://localhost:8080/api/tasks/task-1234567890/cancel
```

#### 删除任务

```bash
curl -X DELETE http://localhost:8080/api/tasks/task-1234567890
```

### 插件管理API测试

#### 获取所有插件

```bash
curl -X GET http://localhost:8080/api/plugins
```

#### 获取插件信息

```bash
curl -X GET http://localhost:8080/api/plugins/file-cleanup
```

#### 获取插件配置

```bash
curl -X GET http://localhost:8080/api/plugins/file-cleanup/config
```

#### 更新插件配置

```bash
curl -X POST http://localhost:8080/api/plugins/file-cleanup/config \
  -H "Content-Type: application/json" \
  -d '{
    "values": {
      "maxFileAgeDays": 30,
      "deleteEmptyDirectories": true
    }
  }'
```

#### 执行插件

```bash
curl -X POST http://localhost:8080/api/plugins/file-cleanup/execute \
  -H "Content-Type: application/json" \
  -d '{
    "files": ["/tmp/file1.tmp", "/tmp/file2.log"],
    "config": {
      "values": {
        "maxFileAgeDays": 30
      }
    }
  }'
```

#### 重新加载插件

```bash
curl -X POST http://localhost:8080/api/plugins/reload
```

### 配置管理API测试

#### 获取所有配置

```bash
curl -X GET http://localhost:8080/api/config
```

#### 获取特定配置

```bash
curl -X GET http://localhost:8080/api/config/maxConcurrentTasks
```

#### 更新多个配置

```bash
curl -X POST http://localhost:8080/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "maxConcurrentTasks": 5,
    "defaultScanDepth": 3
  }'
```

#### 更新特定配置

```bash
curl -X POST http://localhost:8080/api/config/maxConcurrentTasks \
  -H "Content-Type: application/json" \
  -d '10'
```

#### 删除特定配置

```bash
curl -X DELETE http://localhost:8080/api/config/oldSetting
```

#### 清除所有配置

```bash
curl -X DELETE http://localhost:8080/api/config
```

### 日志管理API测试

#### 获取日志

```bash
curl -X GET 'http://localhost:8080/api/logs?level=ERROR&source=plugin&page=1&size=50'
```

#### 添加日志条目

```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "level": "INFO",
    "message": "任务执行成功",
    "source": "task-service"
  }'
```

#### 清除日志

```bash
curl -X DELETE http://localhost:8080/api/logs
```

### 源目录管理API测试

#### 获取所有源目录

```bash
curl -X GET http://localhost:8080/api/source-directories
```

#### 添加源目录

```bash
curl -X POST http://localhost:8080/api/source-directories \
  -H "Content-Type: application/json" \
  -d '{
    "path": "/path/to/source",
    "threadCount": 4
  }'
```

#### 移除源目录

```bash
curl -X DELETE http://localhost:8080/api/source-directories/path/to/source
```

#### 清除所有源目录

```bash
curl -X DELETE http://localhost:8080/api/source-directories
```

#### 更新线程数

```bash
curl -X PUT http://localhost:8080/api/source-directories/path/to/source/threads \
  -H "Content-Type: application/json" \
  -d '{
    "threadCount": 8
  }'
```

### 流水线管理API测试

#### 获取流水线

```bash
curl -X GET http://localhost:8080/api/pipeline
```

#### 更新流水线

```bash
curl -X POST http://localhost:8080/api/pipeline \
  -H "Content-Type: application/json" \
  -d '[
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
  ]'
```

#### 分析流水线

```bash
curl -X POST http://localhost:8080/api/pipeline/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "sourceDirectories": ["/path/to/source1", "/path/to/source2"],
    "pipeline": [
      {
        "strategyId": "rename",
        "config": {}
      }
    ]
  }'
```

#### 执行流水线

```bash
curl -X POST http://localhost:8080/api/pipeline/execute \
  -H "Content-Type: application/json" \
  -d '{
    "sourceDirectories": ["/path/to/source1", "/path/to/source2"],
    "pipeline": [
      {
        "strategyId": "rename",
        "config": {}
      }
    ]
  }'
```

### 线程池管理API测试

#### 获取线程池配置

```bash
curl -X GET http://localhost:8080/api/thread-pool
```

#### 更新预览线程数

```bash
curl -X PUT http://localhost:8080/api/thread-pool/preview \
  -H "Content-Type: application/json" \
  -d '{
    "threads": 8
  }'
```

#### 更新执行线程数

```bash
curl -X PUT http://localhost:8080/api/thread-pool/execution \
  -H "Content-Type: application/json" \
  -d '{
    "threads": 16
  }'
```

## 前端测试

### Flutter测试

#### 运行所有测试

```bash
cd clients/flutter-web-cli
flutter test
```

#### 运行特定测试文件

```bash
cd clients/flutter-web-cli
flutter test test/pages/source_directories_test.dart
```

#### 运行特定测试

```bash
cd clients/flutter-web-cli
flutter test --name "testGetSourceDirectories"
```

## 集成测试

### 启动测试环境

1. 启动后端服务：
```bash
cd backend
mvn spring-boot:run
```

2. 启动前端服务：
```bash
cd clients/flutter-web-cli
flutter build web
python3 -m http.server 8081
```

3. 执行集成测试脚本

### 测试脚本示例

```bash
#!/bin/bash

BASE_URL="http://localhost:8080/api"

echo "测试文件操作API..."
curl -X GET "$BASE_URL/files/scan?path=/tmp"

echo "测试策略管理API..."
curl -X GET "$BASE_URL/strategies"

echo "测试任务管理API..."
curl -X POST "$BASE_URL/tasks" \
  -H "Content-Type: application/json" \
  -d '{
    "strategyId": "file-collection",
    "filePaths": ["/tmp/file1.mp3"],
    "strategyConfig": {
      "values": {
        "targetDirectory": "/path/to/target"
      }
    }
  }'

echo "测试插件管理API..."
curl -X GET "$BASE_URL/plugins"

echo "测试配置管理API..."
curl -X GET "$BASE_URL/config"

echo "测试日志管理API..."
curl -X GET "$BASE_URL/logs"

echo "测试源目录管理API..."
curl -X GET "$BASE_URL/source-directories"

echo "测试流水线管理API..."
curl -X GET "$BASE_URL/pipeline"

echo "测试线程池管理API..."
curl -X GET "$BASE_URL/thread-pool"

echo "所有测试完成！"
```

## 测试覆盖率

### 生成测试覆盖率报告

```bash
cd backend
mvn clean test jacoco:report
```

测试覆盖率报告将生成在`target/site/jacoco/index.html`。

### 查看测试覆盖率

打开浏览器访问`target/site/jacoco/index.html`查看详细的测试覆盖率报告。

## 测试最佳实践

1. **测试命名**: 使用描述性的测试方法名，如`testGetFilesReturnsFileList`
2. **测试隔离**: 每个测试应该独立运行，不依赖其他测试
3. **测试数据**: 使用测试数据构建器或工厂方法创建测试数据
4. **Mock使用**: 适当使用Mock对象隔离被测试的组件
5. **断言清晰**: 使用清晰的断言消息，便于理解测试意图
6. **测试清理**: 在测试后清理测试数据和资源
7. **测试覆盖**: 确保测试覆盖正常流程和异常情况
8. **性能测试**: 对于关键功能，添加性能测试

## 总结

本测试指南提供了FileManager Plus系统的完整测试策略和测试方法。通过遵循这些指南，可以确保系统的质量和稳定性。
