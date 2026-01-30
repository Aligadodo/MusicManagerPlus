# 测试指南

## 概述

本文档提供了FileManager Plus项目的测试指南，包括测试策略、测试用例和最佳实践。

## 测试策略

### 测试层级

FileManager Plus项目采用分层测试策略：

1. **单元测试**：测试单个类和方法
2. **集成测试**：测试组件之间的交互
3. **端到端测试**：测试完整的用户流程

### 测试框架

- **后端**：JUnit 5 + Mockito
- **前端**：Flutter test framework

## 后端测试

### 测试覆盖

后端测试覆盖以下控制器：

| 控制器 | 测试类 | 测试数量 |
|--------|--------|----------|
| FileController | FileControllerTest | 9 |
| PluginController | PluginControllerTest | 10 |
| StrategyController | StrategyControllerTest | 7 |
| TaskController | TaskControllerTest | 10 |
| LogController | LogControllerTest | 8 |
| ConfigController | ConfigControllerTest | 7 |
| PipelineController | PipelineControllerTest | 8 |
| SourceDirectoryController | SourceDirectoryControllerTest | 13 |
| ThreadPoolController | ThreadPoolControllerTest | 13 |

**总计：85个测试用例**

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=FileControllerTest

# 运行特定测试方法
mvn test -Dtest=FileControllerTest#testScanDirectory_Success
```

### 测试示例

#### FileControllerTest

```java
@Test
public void testScanDirectory_Success() {
    List<FileInfoDTO> mockFiles = new ArrayList<>();
    FileInfoDTO file = new FileInfoDTO();
    file.setPath("/test/file.txt");
    file.setName("file.txt");
    file.setDirectory(false);
    file.setSize(1024);
    mockFiles.add(file);

    when(fileService.scanDirectory(eq("/test/path"), eq(0), eq(3), isNull()))
        .thenReturn(mockFiles);

    ResponseEntity<List<FileInfoDTO>> response = fileController.scanDirectory("/test/path", 0, 3, null);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("file.txt", response.getBody().get(0).getName());
}
```

#### PluginControllerTest

```java
@Test
public void testGetPlugins_Success() {
    List<PluginInfoDTO> mockPlugins = new ArrayList<>();
    PluginInfoDTO plugin = new PluginInfoDTO();
    plugin.setId("test-plugin");
    plugin.setName("Test Plugin");
    plugin.setDescription("A test plugin");
    plugin.setVersion("1.0.0");
    plugin.setEnabled(true);
    mockPlugins.add(plugin);

    when(pluginService.getAvailablePlugins()).thenReturn(mockPlugins);

    ResponseEntity<List<PluginInfoDTO>> response = pluginController.getPlugins();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals("test-plugin", response.getBody().get(0).getId());
}
```

#### TaskControllerTest

```java
@Test
public void testCreateTask_Success() {
    TaskRequestDTO request = new TaskRequestDTO();
    request.setStrategyId("test-strategy");
    request.setTaskName("Test Task");
    request.setDescription("A test task");

    when(taskService.createTask(any(TaskRequestDTO.class))).thenReturn("task-123");

    ResponseEntity<Map<String, String>> response = taskController.createTask(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("task-123", response.getBody().get("taskId"));

    verify(taskService, times(1)).createTask(any(TaskRequestDTO.class));
}
```

### 测试最佳实践

1. **使用Mock对象**：隔离被测试的组件
2. **验证行为**：验证方法调用和返回值
3. **测试边界情况**：包括空值、无效输入等
4. **清晰的测试名称**：使用描述性的测试方法名称
5. **AAA模式**：Arrange（准备）、Act（执行）、Assert（断言）

## 前端测试

### 测试覆盖

前端测试应覆盖以下页面和组件：

| 页面/组件 | 测试重点 |
|----------|----------|
| FileBrowserPage | 文件扫描、导航、选择 |
| StrategyConfigPage | 策略加载、配置更新 |
| TaskMonitorPage | 任务列表、状态更新、WebSocket连接 |
| PluginListPage | 插件列表、插件信息显示 |
| PluginConfigPage | 参数配置、保存功能 |
| PipelineConfigPage | 流水线配置、插件排序 |
| PreviewPage | 预览结果显示、变更记录 |
| LogPage | 日志加载、过滤、显示 |

### 运行测试

```bash
# 运行所有测试
flutter test

# 运行特定测试文件
flutter test test/pages/file_browser_test.dart

# 运行特定测试
flutter test test/pages/file_browser_test.dart --name="testScanDirectory"
```

### 测试示例

```dart
testWidgets('FileBrowserPage displays files correctly', (WidgetTester tester) async {
  final mockFileService = MockFileService();
  final mockFiles = [
    FileInfo(path: '/test/file1.txt', name: 'file1.txt', directory: false, size: 1024),
    FileInfo(path: '/test/file2.txt', name: 'file2.txt', directory: false, size: 2048),
  ];

  when(mockFileService.scanDirectory(any)).thenAnswer((_) async => mockFiles);

  await tester.pumpWidget(
    MaterialApp(
      home: FileBrowserPage(fileService: mockFileService),
    ),
  );

  expect(find.text('file1.txt'), findsOneWidget);
  expect(find.text('file2.txt'), findsOneWidget);
});
```

## 集成测试

### 后端-前端集成

测试后端API和前端客户端之间的集成：

1. **API端点测试**：验证所有API端点正常工作
2. **数据序列化**：验证JSON序列化和反序列化
3. **错误处理**：验证错误响应的正确处理
4. **认证**：验证认证机制正常工作

### WebSocket集成

测试WebSocket连接和实时更新：

1. **连接建立**：验证WebSocket连接成功建立
2. **消息接收**：验证实时消息正确接收
3. **连接断开**：验证连接断开时的处理
4. **重连机制**：验证自动重连功能

## 性能测试

### 后端性能

测试后端性能指标：

1. **响应时间**：API端点的响应时间
2. **并发处理**：同时处理多个请求的能力
3. **文件操作**：大文件和大量文件的处理性能
4. **内存使用**：长时间运行的内存使用情况

### 前端性能

测试前端性能指标：

1. **页面加载时间**：页面加载和渲染时间
2. **交互响应**：用户交互的响应时间
3. **内存使用**：长时间使用的内存使用情况
4. **网络请求**：API请求的效率和缓存

## 安全测试

### 认证测试

1. **有效认证**：使用有效凭据的访问
2. **无效认证**：使用无效凭据的访问
3. **认证过期**：认证过期的处理
4. **认证绕过**：尝试绕过认证机制

### 输入验证测试

1. **SQL注入**：测试SQL注入攻击
2. **XSS攻击**：测试跨站脚本攻击
3. **路径遍历**：测试路径遍历攻击
4. **文件操作**：测试恶意文件操作

## 持续集成

### CI/CD集成

将测试集成到CI/CD流程：

1. **自动测试**：每次提交自动运行测试
2. **测试报告**：生成测试覆盖率报告
3. **失败通知**：测试失败时通知团队
4. **部署门禁**：测试通过后才允许部署

### 测试覆盖率

目标测试覆盖率：

- **后端**：≥80%
- **前端**：≥70%

生成覆盖率报告：

```bash
# 后端覆盖率
mvn jacoco:report

# 前端覆盖率
flutter test --coverage
```

## 故障排除

### 常见问题

1. **测试失败**：检查测试依赖和Mock配置
2. **测试超时**：增加测试超时时间或优化测试
3. **测试不稳定**：检查测试之间的依赖关系
4. **覆盖率低**：添加更多测试用例

### 调试技巧

1. **日志输出**：使用日志输出调试信息
2. **断点调试**：使用IDE的调试功能
3. **Mock验证**：验证Mock对象的行为
4. **隔离测试**：单独运行失败的测试

## 最佳实践总结

### 测试设计

1. **独立性**：每个测试应该独立运行
2. **可重复性**：测试结果应该可重复
3. **快速执行**：测试应该快速执行
4. **清晰意图**：测试应该清楚地表达其意图

### 测试维护

1. **定期更新**：随着代码变化更新测试
2. **删除过时测试**：删除不再相关的测试
3. **重构测试代码**：保持测试代码的整洁
4. **文档化复杂测试**：为复杂测试添加文档

## 结论

测试是确保FileManager Plus项目质量和稳定性的关键部分。通过遵循本文档中概述的指南和最佳实践，您可以创建有效的测试套件，提高代码质量，并减少生产环境中的问题。

定期运行测试、维护测试覆盖率，并持续改进测试策略，以确保项目长期的成功。
