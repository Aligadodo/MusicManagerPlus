# 服务接口文档

## 概述

本文档提供了FileManager Plus后端中服务接口及其实现的详细信息。这些服务构成了应用程序的核心业务逻辑层，处理文件操作、策略管理、任务执行和插件集成。

## 服务架构

后端服务架构遵循分层方法：

1. **控制器层**：处理HTTP请求和响应
2. **服务层**：实现核心业务逻辑
3. **存储库层**：管理数据访问
4. **插件层**：提供可扩展功能

## 核心服务接口

### 1. FileService

**接口**：`com.filemanager.domain.service.FileService`

**描述**：提供文件系统操作方法，如扫描、信息检索和基本文件操作。

**方法**：

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `scanDirectory(String path, int minDepth, int maxDepth, String pattern)` | `List<FileInfoDTO>` | 扫描目录中符合指定条件的文件 |
| `getFileInfo(String path)` | `FileInfoDTO` | 获取特定文件的详细信息 |
| `checkExists(List<String> paths)` | `Map<String, Boolean>` | 检查多个文件是否存在 |
| `copy(String source, String target)` | `boolean` | 将文件从源复制到目标 |
| `move(String source, String target)` | `boolean` | 将文件从源移动到目标 |
| `delete(String path)` | `boolean` | 删除文件 |
| `rename(String source, String target)` | `boolean` | 重命名文件 |
| `getFileContent(String path)` | `byte[]` | 获取文件内容 |
| `writeFileContent(String path, byte[] content)` | `boolean` | 将内容写入文件 |

**实现**：`FileServiceImpl`

该实现使用Java NIO.2进行文件系统操作，提供高效可靠的文件处理。

### 2. StrategyService

**接口**：`com.filemanager.domain.service.StrategyService`

**描述**：管理文件处理策略，包括配置、分析和执行。

**方法**：

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getAvailableStrategies()` | `List<StrategyInfoDTO>` | 获取所有可用策略 |
| `getStrategyInfo(String strategyId)` | `StrategyInfoDTO` | 获取特定策略的信息 |
| `getStrategyConfig(String strategyId)` | `StrategyConfigDTO` | 获取特定策略的配置 |
| `updateStrategyConfig(String strategyId, StrategyConfigDTO config)` | `boolean` | 更新特定策略的配置 |
| `analyzeFiles(String strategyId, List<String> filePaths, StrategyConfigDTO config)` | `List<ChangeRecord>` | 使用特定策略分析文件 |
| `executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config)` | `List<ChangeRecord>` | 在文件上执行策略 |

**实现**：`StrategyServiceImpl`

该实现与插件系统集成，提供可扩展的策略功能。当可用时，它可以委托给插件执行实际的策略。

### 3. TaskService

**接口**：`com.filemanager.domain.service.TaskService`

**描述**：管理任务创建、执行、监控和生命周期。

**方法**：

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `createTask(TaskRequestDTO request)` | `String` | 创建新任务并返回其ID |
| `getTaskStatus(String taskId)` | `TaskStatusDTO` | 获取特定任务的状态 |
| `getTasks(String status, int page, int size)` | `List<TaskStatusDTO>` | 获取任务（可选过滤和分页） |
| `executeTask(String taskId)` | `boolean` | 执行特定任务 |
| `cancelTask(String taskId)` | `boolean` | 取消运行中的任务 |
| `getTaskResults(String taskId)` | `List<ChangeRecord>` | 获取已完成任务的结果 |
| `deleteTask(String taskId)` | `boolean` | 删除任务 |

**实现**：`TaskServiceImpl`

该实现使用线程池进行异步任务执行，并在内存中维护任务状态。它与StrategyService集成以执行文件处理策略。

### 4. PluginService

**接口**：`com.filemanager.domain.service.PluginService`

**描述**：管理插件，包括发现、配置和执行。

**方法**：

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getAvailablePlugins()` | `List<PluginInfoDTO>` | 获取所有可用插件 |
| `getPluginInfo(String pluginId)` | `PluginInfoDTO` | 获取特定插件的信息 |
| `getPluginConfig(String pluginId)` | `PluginConfigDTO` | 获取特定插件的配置 |
| `updatePluginConfig(String pluginId, PluginConfigDTO config)` | `boolean` | 更新特定插件的配置 |
| `executePlugin(String pluginId, List<String> filePaths, PluginConfigDTO config)` | `List<ChangeRecord>` | 在文件上执行插件 |
| `reloadPlugins()` | `boolean` | 重新加载所有插件 |

**实现**：`PluginServiceImpl`

该实现使用Java ServiceLoader机制来发现和加载插件。它在内存中维护插件配置，并将执行委托给适当的插件实例。

### 5. SourceDirectoryService

**接口**：`com.filemanager.backend.controller.SourceDirectoryController`

**描述**：管理文件处理的源目录，包括添加、删除和配置线程数。

**方法**：

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getSourceDirectories()` | `List<Map<String, Object>>` | 获取所有配置的源目录 |
| `addSourceDirectory(Map<String, Object> request)` | `Map<String, Object>` | 添加带有可选线程数的新源目录 |
| `removeSourceDirectory(String id)` | `Map<String, Object>` | 移除特定源目录 |
| `clearSourceDirectories()` | `Map<String, Object>` | 清除所有源目录 |
| `updateThreadCount(String id, Map<String, Object> request)` | `Map<String, Object>` | 更新特定源目录的线程数 |

**实现**：`SourceDirectoryController`

该实现在内存中维护带有关联线程数的源目录。每个目录可以配置特定数量的线程用于并行文件处理。

### 6. PipelineService

**接口**：`com.filemanager.backend.controller.PipelineController`

**描述**：管理复杂文件处理工作流的策略流水线，包括配置、分析和执行。

**方法**：

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getPipeline()` | `List<Map<String, Object>>` | 获取当前流水线配置 |
| `updatePipeline(List<Map<String, Object>> pipeline)` | `Map<String, Object>` | 更新流水线配置 |
| `analyzePipeline(Map<String, Object> request)` | `List<ChangeRecord>` | 分析流水线以预览文件更改 |
| `executePipeline(Map<String, Object> request)` | `Map<String, Object>` | 在源目录上执行流水线 |

**实现**：`PipelineController`

该实现与StrategyService集成以分析和执行流水线。它允许用户将多个策略链接在一起，用于复杂的文件处理工作流。

### 7. ThreadPoolService

**接口**：`com.filemanager.backend.controller.ThreadPoolController`

**描述**：管理文件处理的线程池配置，包括预览和执行线程数。

**方法**：

| 方法 | 返回类型 | 描述 |
|------|----------|------|
| `getThreadPoolConfig()` | `Map<String, Object>` | 获取当前线程池配置 |
| `setPreviewThreads(Map<String, Object> request)` | `Map<String, Object>` | 更新预览线程数 |
| `setExecutionThreads(Map<String, Object> request)` | `Map<String, Object>` | 更新执行线程数 |

**实现**：`ThreadPoolController`

该实现在内存中维护线程池配置，允许用户调整用于预览和执行操作的线程数。

## 服务依赖关系

服务具有以下依赖关系：

| 服务 | 依赖项 | 描述 |
|------|--------|------|
| `FileServiceImpl` | 无 | 文件操作的独立服务 |
| `StrategyServiceImpl` | `PluginRegistry` | 使用插件执行策略 |
| `TaskServiceImpl` | `StrategyService` | 使用策略执行任务 |
| `PluginServiceImpl` | `PluginRegistry` | 使用插件注册表管理插件 |
| `SourceDirectoryController` | 无 | 源目录管理的独立控制器 |
| `PipelineController` | `StrategyService`, `TaskService` | 使用策略和任务执行流水线 |
| `ThreadPoolController` | 无 | 线程池管理的独立控制器 |

## 配置

服务通过Spring Boot的依赖注入系统进行配置。配置类位于`com.filemanager.backend.config`包中。

### 关键配置类

| 类 | 描述 |
|-----|------|
| `AppConfig` | 一般应用程序配置 |
| `SecurityConfig` | 安全和认证配置 |
| `WebSocketConfig` | WebSocket连接配置 |
| `PluginConfig` | 插件系统配置 |

## 错误处理

服务通过异常传播和日志记录处理错误。异常在控制器层被捕获并转换为适当的HTTP状态码。

## 性能考虑

- **FileService**：使用Java NIO.2进行高效文件操作
- **TaskService**：使用线程池进行异步执行
- **StrategyService**：缓存策略配置以快速访问
- **PluginService**：延迟加载插件配置

## 可扩展性

服务层设计为通过以下方式可扩展：

1. **基于接口的设计**：所有服务通过接口定义
2. **插件系统**：允许第三方功能扩展
3. **Spring依赖注入**：促进组件替换

## 测试

可以使用标准的JUnit和Mockito框架测试服务。可以使用Spring Boot的测试支持执行集成测试。

## 结论

服务接口构成了FileManager Plus应用程序的核心业务逻辑层，提供了API处理和实际功能之间的清晰分离。通过遵循基于接口的设计原则并与插件系统集成，服务既灵活又可扩展，允许轻松添加新功能和能力。