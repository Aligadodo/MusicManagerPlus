# 任务管理系统开发指南

## 1. 系统概述

### 1.1 系统架构

任务管理系统采用分层架构设计，主要包含以下层次：

```
┌─────────────────────────────────────────────────┐
│           前端层 (Flutter Web)            │
├─────────────────────────────────────────────────┤
│           API层 (REST API)                │
├─────────────────────────────────────────────────┤
│         服务层 (Service Layer)              │
│  - TaskRegistry                          │
│  - OptimizedTaskStorageService             │
│  - OptimizedTaskExecutionService          │
├─────────────────────────────────────────────────┤
│        数据层 (Data Layer)                 │
│  - 文件系统存储                          │
└─────────────────────────────────────────────────┘
```

### 1.2 核心组件

#### 1.2.1 TaskRegistry (任务注册中心)

**位置**: `com.filemanager.backend.service.TaskRegistry`

**职责**:
- 管理所有任务的生命周期
- 提供任务注册、查询、更新、删除功能
- 维护任务状态和进度信息
- 提供任务统计信息

**主要方法**:

```java
public String registerTask(TaskInfo task)
public TaskInfo getTask(String taskId)
public List<TaskInfo> getAllTasks()
public void updateTaskStatus(String taskId, TaskStatus status)
public void updateTaskProgress(String taskId, double progress)
public boolean restartTask(String taskId, String fromStage)
public boolean deleteTask(String taskId)
public Map<String, Object> getTaskStatistics()
```

**使用示例**:

```java
@Autowired
private TaskRegistry taskRegistry;

public void createTask() {
    TaskInfo task = new TaskInfo();
    task.setTaskName("音乐文件整理");
    task.setStatus(TaskInfo.TaskStatus.CREATED);
    
    String taskId = taskRegistry.registerTask(task);
    System.out.println("任务已创建: " + taskId);
}
```

#### 1.2.2 OptimizedTaskStorageService (任务存储服务)

**位置**: `com.filemanager.backend.service.OptimizedTaskStorageService`

**职责**:
- 负责任务的持久化存储
- 提供任务的CRUD操作
- 管理任务相关的文件和目录

**主要方法**:

```java
public String createTask(TaskRequestDTO request)
public TaskInfo loadTaskInfo(String taskId)
public List<String> getAllTaskIds()
public boolean saveTask(TaskInfo task)
public boolean deleteTask(String taskId)
```

#### 1.2.3 OptimizedTaskExecutionService (任务执行服务)

**位置**: `com.filemanager.backend.service.OptimizedTaskExecutionService`

**职责**:
- 负责任务的执行控制
- 管理任务的扫描、预览、执行阶段
- 处理任务取消和重试

**主要方法**:

```java
public void executeScan(String taskId)
public void executePreview(String taskId)
public void executeTask(String taskId)
public void executeSelected(String taskId, List<String> selectedRecordIds)
public boolean cancelTask(String taskId)
```

## 2. 开发环境配置

### 2.1 后端环境

**技术栈**:
- Java 8
- Spring Boot 2.7.18
- Maven 3.x

**配置文件**: `backend/src/main/resources/application.yml`

**关键配置**:

```yaml
server:
  port: 8080

spring:
  application:
    name: backend

logging:
  level:
    com.filemanager: DEBUG
```

**启动命令**:

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

### 2.2 前端环境

**技术栈**:
- Flutter 3.x
- Dart 3.x
- Web平台

**配置文件**: `clients/flutter-web-cli/lib/api/api_client.dart`

**关键配置**:

```dart
class ApiClient {
  static const String baseUrl = 'http://localhost:8080';
  static const String wsBaseUrl = 'ws://localhost:8080';
}
```

**启动命令**:

```bash
cd clients/flutter-web-cli
flutter pub get
flutter run -d chrome --web-port=8081
```

## 3. 开发规范

### 3.1 代码规范

#### 3.1.1 Java代码规范

1. **命名规范**:
   - 类名：大驼峰命名法（PascalCase）
   - 方法名：小驼峰命名法（camelCase）
   - 常量：全大写，下划线分隔（UPPER_SNAKE_CASE）
   - 变量：小驼峰命名法（camelCase）

2. **注释规范**:
   - 类级别注释：描述类的职责和用途
   - 方法级别注释：描述方法的功能、参数、返回值
   - 复杂逻辑注释：解释复杂的业务逻辑

3. **异常处理**:
   - 使用具体的异常类型
   - 提供有意义的错误信息
   - 记录异常日志

**示例**:

```java
/**
 * 任务注册中心
 * 统一管理所有任务的注册、状态和生命周期
 */
@Service
public class TaskRegistry {
    
    /**
     * 注册新任务
     * 
     * @param task 任务信息
     * @return 任务ID
     * @throws IllegalArgumentException 如果任务信息无效
     */
    public String registerTask(TaskInfo task) {
        if (task == null) {
            throw new IllegalArgumentException("任务信息不能为空");
        }
        
        String taskId = generateTaskId();
        registeredTasks.put(taskId, task);
        
        logger.info("任务已注册: {}", taskId);
        return taskId;
    }
}
```

#### 3.1.2 Dart代码规范

1. **命名规范**:
   - 类名：大驼峰命名法（PascalCase）
   - 方法名：小驼峰命名法（camelCase）
   - 常量：全小写，下划线分隔（lower_snake_case）
   - 变量：小驼峰命名法（camelCase）

2. **注释规范**:
   - 类级别注释：描述类的职责和用途
   - 方法级别注释：描述方法的功能、参数、返回值
   - 复杂逻辑注释：解释复杂的业务逻辑

3. **异步处理**:
   - 使用async/await处理异步操作
   - 正确处理Future和Stream
   - 添加适当的错误处理

**示例**:

```dart
/// 任务服务
/// 提供任务管理的API调用
class TaskService {
  final ApiClient _apiClient;

  TaskService(this._apiClient);

  /// 创建任务
  /// 
  /// [request] 任务请求信息
  /// 返回任务ID
  Future<String> createTask(TaskRequest request) async {
    try {
      final response = await _apiClient.post(
        '/api/tasks',
        body: request.toJson(),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        return data['taskId'] as String;
      } else {
        throw Exception('Failed to create task: ${response.statusCode}');
      }
    } catch (e) {
      logger.error('创建任务失败', e);
      rethrow;
    }
  }
}
```

### 3.2 API设计规范

#### 3.2.1 RESTful API设计

1. **URL设计**:
   - 使用名词复数形式：`/tasks`
   - 使用层级结构：`/tasks/{taskId}/scan`
   - 使用查询参数：`/tasks?page=1&size=20`

2. **HTTP方法**:
   - GET：查询资源
   - POST：创建资源
   - PUT：更新资源
   - DELETE：删除资源

3. **响应格式**:
   - 统一的成功响应格式
   - 统一的失败响应格式
   - 包含时间戳

**示例**:

```java
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(
            @RequestBody TaskRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String taskId = taskService.createTask(request);
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "任务已创建");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("任务创建失败", e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "CREATE_TASK_FAILED");
            error.put("message", "任务创建失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(response);
        }
    }
}
```

### 3.3 数据库规范

#### 3.3.1 文件存储规范

1. **目录结构**:
   ```
   tasks/
   ├── {taskId}/
   │   ├── task.json
   │   ├── scan/
   │   ├── preview/
   │   └── execution/
   ```

2. **文件命名**:
   - 任务信息：`task.json`
   - 扫描结果：`scan_results.json`
   - 预览结果：`preview_results.json`
   - 执行结果：`execution_{num}_results.json`

3. **数据格式**:
   - 使用JSON格式存储数据
   - 使用UTF-8编码
   - 保持数据的一致性

## 4. 测试指南

### 4.1 单元测试

**测试框架**: JUnit 5

**测试位置**: `backend/src/test/java/com/filemanager/backend/`

**测试命名规范**: `{ClassName}Test.java`

**示例**:

```java
@SpringBootTest
class TaskRegistryTest {
    
    @Autowired
    private TaskRegistry taskRegistry;
    
    @Test
    void testRegisterTask() {
        TaskInfo task = new TaskInfo();
        task.setTaskName("测试任务");
        
        String taskId = taskRegistry.registerTask(task);
        
        assertNotNull(taskId);
        assertTrue(taskId.startsWith("task-"));
    }
}
```

### 4.2 集成测试

**测试框架**: Spring Boot Test

**测试位置**: `backend/src/test/java/com/filemanager/backend/integration/`

**测试命名规范**: `{ClassName}IntegrationTest.java`

**示例**:

```java
@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testCreateTask() throws Exception {
        String requestJson = """
            {
                "taskName": "测试任务",
                "sourceDirectories": [
                    {"path": "/test/path"}
                ]
            }
            """;
        
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.success").value(true))
               .andExpect(jsonPath("$.data.taskId").exists());
    }
}
```

### 4.3 端到端测试

**测试框架**: Flutter Test

**测试位置**: `clients/flutter-web-cli/test/`

**测试命名规范**: `{feature}_e2e_test.dart`

**示例**:

```dart
void main() {
  group('任务管理端到端测试', () {
    late TaskService taskService;
    late ApiClient apiClient;

    setUp(() {
      apiClient = ApiClient();
      taskService = TaskService(apiClient);
    });

    test('创建任务并验证返回', () async {
      final request = TaskRequest(
        strategyId: 'test-strategy',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '测试任务',
      );

      final taskId = await taskService.createTask(request);

      expect(taskId, isNotEmpty);
      expect(taskId, startsWith('task-'));
    });
  });
}
```

### 4.4 运行测试

**后端测试**:

```bash
cd backend
mvn test
```

**前端测试**:

```bash
cd clients/flutter-web-cli
flutter test
```

**特定测试**:

```bash
flutter test test/task_management_comprehensive_e2e_test.dart
```

## 5. 部署指南

### 5.1 后端部署

**构建**:

```bash
cd backend
mvn clean package
```

**运行**:

```bash
java -jar target/backend-1.0.0.jar
```

**Docker部署**:

```dockerfile
FROM openjdk:8-jre-alpine
COPY target/backend-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### 5.2 前端部署

**构建**:

```bash
cd clients/flutter-web-cli
flutter build web
```

**部署**:

将 `build/web` 目录部署到Web服务器

## 6. 故障排查

### 6.1 常见问题

#### 问题1: 任务列表只能显示一个任务

**原因**: PipelineController在每次预览前调用clearAllTasks()

**解决方案**:
```java
// 移除这行代码
taskManager.clearAllTasks();

// 使用TaskRegistry管理多任务
taskRegistry.registerTask(taskInfo);
```

#### 问题2: 任务详情刷新失败

**原因**: TaskController.taskInfoToDetailMap方法中缺少null检查

**解决方案**:
```java
// 添加null检查
if (taskInfo.getStages() != null) {
    Map<String, Object> stages = new HashMap<>();
    stages.put("scan", scanStageToMap(taskInfo.getStages().getScan()));
    stages.put("preview", previewStageToMap(taskInfo.getStages().getPreview()));
    stages.put("execution", executionStageToMap(taskInfo.getStages().getExecution()));
    map.put("stages", stages);
}
```

#### 问题3: 预览后任务不显示在列表中

**原因**: PipelineController未正确创建持久化任务记录

**解决方案**:
```java
// 集成TaskService创建持久化任务
String taskId = taskService.createTask(taskRequest);
taskManager.createTaskWithId(taskId, "preview");

// 更新任务状态
taskService.updateTaskStatus(taskId, TaskStatus.PREVIEWING);
```

### 6.2 日志查看

**后端日志**:

```bash
# 查看实时日志
tail -f backend/logs/application.log

# 搜索错误日志
grep "ERROR" backend/logs/application.log

# 搜索特定任务日志
grep "task-1234567890" backend/logs/application.log
```

**前端日志**:

```bash
# 查看浏览器控制台
# Chrome: F12 -> Console
# Firefox: F12 -> Console
```

## 7. 最佳实践

### 7.1 性能优化

1. **使用缓存**:
   - 缓存频繁访问的数据
   - 使用适当的缓存策略

2. **异步处理**:
   - 使用异步方法处理耗时操作
   - 避免阻塞主线程

3. **批量操作**:
   - 批量处理数据减少IO操作
   - 使用批量API接口

### 7.2 安全性

1. **输入验证**:
   - 验证所有输入参数
   - 防止注入攻击

2. **错误处理**:
   - 不暴露敏感信息
   - 提供友好的错误消息

3. **访问控制**:
   - 实现适当的权限控制
   - 使用HTTPS传输数据

### 7.3 可维护性

1. **代码组织**:
   - 按功能模块组织代码
   - 保持单一职责原则

2. **文档维护**:
   - 保持文档与代码同步
   - 更新API文档和测试文档

3. **版本控制**:
   - 使用Git进行版本控制
   - 编写有意义的提交消息

## 8. 附录

### 8.1 相关文档

- [API文档](./task_management_api.md)
- [测试报告](./task_management_test_report.md)
- [架构文档](./task_management_architecture.md)

### 8.2 更新日志

#### 2026-02-14

- 创建任务注册中心
- 优化多任务管理
- 修复任务列表显示问题
- 修复任务详情刷新问题
- 添加完整的测试用例
- 更新开发文档

---

**文档版本**: 1.0.0
**最后更新**: 2026-02-14
**维护者**: 开发团队
