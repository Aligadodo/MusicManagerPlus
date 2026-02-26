# API接口设计规范

## 1. 概述

本文档定义了FileManager Plus项目的API接口设计规范，确保前后端接口路径的一致性和稳定性，避免在迭代过程中出现404等问题。

## 2. API路径规范

### 2.1 基础路径规则

所有API接口必须以`/api`为前缀，后端Controller使用`@RequestMapping("/api/xxx")`定义基础路径，前端Service调用时也必须包含`/api`前缀。

**后端示例**：
```java
@RestController
@RequestMapping("/api/logs")
public class LogController {
    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>> getLogFiles() {
        // ...
    }
}
```

**前端示例**：
```dart
class LogService {
  Future<Map<String, dynamic>> getLogFiles() async {
    final response = await _apiClient.get('/api/logs/files');
    // ...
  }
}
```

### 2.2 RESTful路径设计

API路径应遵循RESTful设计原则：

| HTTP方法 | 路径格式 | 说明 |
|---------|-----------|------|
| GET | `/api/{resource}` | 获取资源列表 |
| GET | `/api/{resource}/{id}` | 获取单个资源 |
| POST | `/api/{resource}` | 创建资源 |
| PUT | `/api/{resource}/{id}` | 更新资源 |
| DELETE | `/api/{resource}/{id}` | 删除资源 |
| POST | `/api/{resource}/{id}/{action}` | 执行资源操作 |

### 2.3 现有API路径清单

| 资源 | 基础路径 | 说明 |
|-------|-----------|------|
| 策略 | `/api/strategies` | 策略管理 |
| 管道 | `/api/pipeline` | 管道管理 |
| 插件 | `/api/plugins` | 插件管理 |
| 配置 | `/api/config` | 配置管理 |
| 源目录 | `/api/source-directories` | 源目录管理 |
| 枚举 | `/api/enums` | 枚举值管理 |
| 线程池 | `/api/thread-pool` | 线程池管理 |
| 任务 | `/api/tasks` | 任务管理 |
| 日志 | `/api/logs` | 日志管理 |
| 文件 | `/api/files` | 文件管理 |

## 3. 前后端路径一致性检查

### 3.1 检查清单

在开发新功能或修改现有功能时，必须确保：

1. **后端Controller路径**：检查`@RequestMapping`和`@GetMapping/@PostMapping`等注解的路径
2. **前端Service路径**：检查`_apiClient.get/post/put/delete`方法的路径参数
3. **路径一致性**：确保前端路径与后端路径完全一致，包括`/api`前缀

### 3.2 常见错误

**错误1：缺少/api前缀**
```dart
final response = await _apiClient.get('/logs/files');
```

**正确**：
```dart
final response = await _apiClient.get('/api/logs/files');
```

**错误2：路径大小写不一致**
```java
@GetMapping("/Files")
```

**正确**：
```java
@GetMapping("/files")
```

**错误3：路径分隔符不一致**
```dart
final response = await _apiClient.get('/api//logs/files');
```

**正确**：
```dart
final response = await _apiClient.get('/api/logs/files');
```

## 4. API版本控制

当前项目使用单一版本（v1），所有API路径不包含版本号。

如果未来需要引入版本控制，应采用以下格式：
```
/api/v1/{resource}
/api/v2/{resource}
```

## 5. 错误处理规范

### 5.1 HTTP状态码

| 状态码 | 说明 | 使用场景 |
|-------|------|---------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功 |
| 400 | Bad Request | 请求参数错误 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

### 5.2 错误响应格式

```json
{
  "success": false,
  "message": "错误描述信息"
}
```

## 6. 代码注释规范

### 6.1 后端Controller注释

每个Controller类必须添加类级别注释，说明该Controller负责的功能：

```java
@RestController
@RequestMapping("/api/logs")
public class LogController {
    
    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>> getLogFiles() {
        // API路径: GET /api/logs/files
        // 功能: 获取日志文件列表
        // 返回: 日志文件信息列表
    }
}
```

### 6.2 前端Service注释

每个Service方法必须添加注释，说明API路径和功能：

```dart
class LogService {
  final ApiClient _apiClient;

  LogService(this._apiClient);

  /// 获取日志文件列表
  /// API路径: GET /api/logs/files
  /// 返回: 日志文件信息列表
  Future<Map<String, dynamic>> getLogFiles() async {
    final response = await _apiClient.get('/api/logs/files');
    // ...
  }
}
```

## 7. 迭代规范

### 7.1 新增API

1. 在后端创建Controller或添加新的API方法
2. 在前端创建对应的Service方法
3. 确保路径完全一致，包括`/api`前缀
4. 添加必要的代码注释
5. 更新本文档的API路径清单

### 7.2 修改API

1. 检查该API是否被其他模块使用
2. 如果需要修改路径，必须同步更新前后端
3. 如果需要废弃API，保留旧API至少一个版本周期
4. 添加必要的代码注释说明修改原因

### 7.3 删除API

1. 确认该API不再被任何模块使用
2. 在文档中标记为废弃
3. 等待至少一个版本周期后删除
4. 同步删除前后端代码

## 8. 测试规范

### 8.1 API测试

在开发或修改API后，必须进行以下测试：

1. 使用curl或Postman测试API路径是否正确
2. 检查HTTP状态码是否正确
3. 检查响应数据格式是否正确
4. 检查错误处理是否正确

### 8.2 集成测试

确保前后端集成测试通过：

1. 启动后端服务
2. 启动前端服务
3. 在浏览器中访问相关页面
4. 检查浏览器控制台是否有404错误
5. 检查网络请求是否正常

## 9. 文档维护

本文档必须在以下情况下更新：

1. 新增API接口
2. 修改API接口路径
3. 废弃或删除API接口
4. 发现新的常见错误模式

## 10. 附录

### 10.1 API路径检查脚本

可以使用以下脚本检查前后端API路径是否一致：

```bash
# 检查后端Controller路径
grep -r "@RequestMapping" backend/src/main/java/com/filemanager/backend/controller/

# 检查前端Service路径
grep -r "_apiClient\\.get\\|_apiClient\\.post\\|_apiClient\\.put\\|_apiClient\\.delete" clients/flutter-web-cli/lib/api/
```

### 10.2 常见问题FAQ

**Q: 为什么会出现404错误？**
A: 最常见的原因是前端API路径缺少`/api`前缀，或者前后端路径不一致。

**Q: 如何快速定位404问题？**
A: 1. 检查浏览器控制台的网络请求；2. 检查后端日志；3. 对比前后端路径是否一致。

**Q: 修改API路径时需要注意什么？**
A: 必须同步修改前后端，并确保所有使用该API的地方都已更新。
