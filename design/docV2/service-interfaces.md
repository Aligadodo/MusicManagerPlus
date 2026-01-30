# Service Interfaces Documentation

## Overview

This document provides detailed information about the service interfaces and their implementations in the FileManager Plus backend. These services form the core business logic layer of the application, handling file operations, strategy management, task execution, and plugin integration.

## Service Architecture

The backend service architecture follows a layered approach:

1. **Controller Layer**: Handles HTTP requests and responses
2. **Service Layer**: Implements core business logic
3. **Repository Layer**: Manages data access
4. **Plugin Layer**: Provides extensible functionality

## Core Service Interfaces

### 1. FileService

**Interface**: `com.filemanager.domain.service.FileService`

**Description**: Provides methods for file system operations such as scanning, information retrieval, and basic file operations.

**Methods**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `scanDirectory(String path, int minDepth, int maxDepth, String pattern)` | `List<FileInfoDTO>` | Scans a directory for files matching the specified criteria |
| `getFileInfo(String path)` | `FileInfoDTO` | Gets detailed information about a specific file |
| `checkExists(List<String> paths)` | `Map<String, Boolean>` | Checks if multiple files exist |
| `copy(String source, String target)` | `boolean` | Copies a file from source to target |
| `move(String source, String target)` | `boolean` | Moves a file from source to target |
| `delete(String path)` | `boolean` | Deletes a file |
| `rename(String source, String target)` | `boolean` | Renames a file |
| `getFileContent(String path)` | `byte[]` | Gets the content of a file |
| `writeFileContent(String path, byte[] content)` | `boolean` | Writes content to a file |

**Implementation**: `FileServiceImpl`

The implementation uses Java NIO.2 for file system operations, providing efficient and reliable file handling.

### 2. StrategyService

**Interface**: `com.filemanager.domain.service.StrategyService`

**Description**: Manages file processing strategies, including configuration, analysis, and execution.

**Methods**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAvailableStrategies()` | `List<StrategyInfoDTO>` | Gets all available strategies |
| `getStrategyInfo(String strategyId)` | `StrategyInfoDTO` | Gets information about a specific strategy |
| `getStrategyConfig(String strategyId)` | `StrategyConfigDTO` | Gets configuration for a specific strategy |
| `updateStrategyConfig(String strategyId, StrategyConfigDTO config)` | `boolean` | Updates configuration for a specific strategy |
| `analyzeFiles(String strategyId, List<String> filePaths, StrategyConfigDTO config)` | `List<ChangeRecord>` | Analyzes files using a specific strategy |
| `executeStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config)` | `List<ChangeRecord>` | Executes a strategy on files |

**Implementation**: `StrategyServiceImpl`

The implementation integrates with the plugin system to provide extensible strategy functionality. It can delegate to plugins for actual strategy execution when available.

### 3. TaskService

**Interface**: `com.filemanager.domain.service.TaskService`

**Description**: Manages task creation, execution, monitoring, and lifecycle.

**Methods**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `createTask(TaskRequestDTO request)` | `String` | Creates a new task and returns its ID |
| `getTaskStatus(String taskId)` | `TaskStatusDTO` | Gets the status of a specific task |
| `getTasks(String status, int page, int size)` | `List<TaskStatusDTO>` | Gets tasks with optional filtering and pagination |
| `executeTask(String taskId)` | `boolean` | Executes a specific task |
| `cancelTask(String taskId)` | `boolean` | Cancels a running task |
| `getTaskResults(String taskId)` | `List<ChangeRecord>` | Gets the results of a completed task |
| `deleteTask(String taskId)` | `boolean` | Deletes a task |

**Implementation**: `TaskServiceImpl`

The implementation uses a thread pool for asynchronous task execution and maintains task state in memory. It integrates with StrategyService to execute file processing strategies.

### 4. PluginService

**Interface**: `com.filemanager.domain.service.PluginService`

**Description**: Manages plugins, including discovery, configuration, and execution.

**Methods**:

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getAvailablePlugins()` | `List<PluginInfoDTO>` | Gets all available plugins |
| `getPluginInfo(String pluginId)` | `PluginInfoDTO` | Gets information about a specific plugin |
| `getPluginConfig(String pluginId)` | `PluginConfigDTO` | Gets configuration for a specific plugin |
| `updatePluginConfig(String pluginId, PluginConfigDTO config)` | `boolean` | Updates configuration for a specific plugin |
| `executePlugin(String pluginId, List<String> filePaths, PluginConfigDTO config)` | `List<ChangeRecord>` | Executes a plugin on files |
| `reloadPlugins()` | `boolean` | Reloads all plugins |

**Implementation**: `PluginServiceImpl`

The implementation uses the Java ServiceLoader mechanism to discover and load plugins. It maintains plugin configurations in memory and delegates execution to the appropriate plugin instances.

## Service Dependencies

The services have the following dependencies:

| Service | Dependencies | Description |
|---------|--------------|-------------|
| `FileServiceImpl` | None | Standalone service for file operations |
| `StrategyServiceImpl` | `PluginRegistry` | Uses plugins for strategy execution |
| `TaskServiceImpl` | `StrategyService` | Uses strategies to execute tasks |
| `PluginServiceImpl` | `PluginRegistry` | Uses the plugin registry to manage plugins |

## Configuration

Services are configured through Spring Boot's dependency injection system. Configuration classes are located in the `com.filemanager.backend.config` package.

### Key Configuration Classes

| Class | Description |
|-------|-------------|
| `AppConfig` | General application configuration |
| `SecurityConfig` | Security and authentication configuration |
| `WebSocketConfig` | WebSocket connection configuration |
| `PluginConfig` | Plugin system configuration |

## Error Handling

Services handle errors through exception propagation and logging. Exceptions are caught at the controller layer and converted to appropriate HTTP status codes.

## Performance Considerations

- **FileService**: Uses Java NIO.2 for efficient file operations
- **TaskService**: Uses thread pooling for asynchronous execution
- **StrategyService**: Caches strategy configurations for quick access
- **PluginService**: Lazy-loads plugin configurations

## Extensibility

The service layer is designed for extensibility through:

1. **Interface-based design**: All services are defined through interfaces
2. **Plugin system**: Allows third-party functionality extension
3. **Spring dependency injection**: Facilitates component replacement

## Testing

Services can be tested using standard JUnit and Mockito frameworks. Integration tests can be performed using Spring Boot's test support.

## Conclusion

The service interfaces form the core business logic layer of the FileManager Plus application, providing a clean separation between API handling and actual functionality. By following interface-based design principles and integrating with a plugin system, the services are both flexible and extensible, allowing for easy addition of new features and capabilities.
