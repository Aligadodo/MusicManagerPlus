# FileManager Plus 新架构完整设计文档

## 概述

FileManager Plus 新架构采用前后端分离的微服务架构，支持插件系统和策略系统，提供灵活的文件管理和处理能力。本文档详细描述新架构的完整设计。

## 一、架构概览

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层 (Flutter Web)                     │
├─────────────────────────────────────────────────────────────────┤
│  HomePage  │  FileBrowser  │  ComposePage  │  MonitorPage │
└─────────────────────────────────────────────────────────────────┘
                              ↓ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────────┐
│                        后端层 (Spring Boot)                   │
├─────────────────────────────────────────────────────────────────┤
│  Controller Layer  │  Service Layer  │  Plugin Layer      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        插件系统 (Plugin System)                │
├─────────────────────────────────────────────────────────────────┤
│  File Operations  │  Audio Processing  │  Metadata          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        数据层 (Data Layer)                      │
├─────────────────────────────────────────────────────────────────┤
│  File System  │  Configuration  │  Logs                 │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 技术栈

#### 前端技术栈
- **框架**: Flutter Web 3.10.8
- **语言**: Dart 3.10.8
- **状态管理**: Provider
- **HTTP客户端**: Dio
- **WebSocket客户端**: web_socket_channel

#### 后端技术栈
- **框架**: Spring Boot 2.7.18
- **Java版本**: JDK 21
- **构建工具**: Maven 3.11.0
- **插件机制**: Java ServiceLoader
- **JSON处理**: Jackson 2.15.2
- **WebSocket**: Spring WebSocket

#### 共享模块
- **框架**: Maven Module
- **Java版本**: JDK 21
- **依赖管理**: Maven

## 二、前端架构设计

### 2.1 整体架构

#### 2.1.1 分层架构
```
Presentation Layer (UI Components)
         ↓
Business Logic Layer (Providers/Services)
         ↓
Data Access Layer (API Clients)
         ↓
Network Layer (HTTP/WebSocket)
```

#### 2.1.2 核心组件

##### 1. 页面组件 (Pages)
- **HomePage**: 主页面，提供导航功能
- **FileBrowserPage**: 文件浏览器，用于浏览和选择文件
- **ComposePage**: 策略配置页面，用于配置和执行策略
- **PipelineConfigPage**: 流水线配置页面，用于配置插件和策略的流水线
- **PluginConfigPage**: 插件配置页面，用于配置插件参数
- **StrategyConfigPage**: 策略配置页面，用于配置策略参数
- **TaskMonitorPage**: 任务监控页面，用于监控任务执行状态
- **LogPage**: 日志查看页面，用于查看系统日志
- **GlobalSettingsPage**: 全局设置页面，用于配置系统全局参数

##### 2. API客户端 (API Clients)
- **ApiClient**: 基础API客户端，提供HTTP请求功能
- **StrategyService**: 策略服务客户端
- **PluginService**: 插件服务客户端
- **PipelineService**: 流水线服务客户端
- **FileService**: 文件服务客户端
- **TaskService**: 任务服务客户端
- **LogService**: 日志服务客户端
- **ConfigService**: 配置服务客户端
- **EnumService**: 枚举服务客户端
- **SourceDirectoryService**: 源目录服务客户端
- **ThreadPoolService**: 线程池服务客户端

##### 3. 数据模型 (Models)
- **StrategyInfo**: 策略信息模型
- **StrategyConfig**: 策略配置模型
- **ConfigField**: 配置字段模型
- **PluginInfo**: 插件信息模型
- **PluginConfig**: 插件配置模型
- **ChangeRecord**: 变更记录模型
- **TaskStatus**: 任务状态模型
- **FileInfo**: 文件信息模型
- **Precondition**: 前置条件模型
- **PreconditionGroup**: 前置条件组模型

##### 4. 状态管理 (Providers)
- **ConfigProvider**: 配置状态管理
- **TaskProvider**: 任务状态管理
- **FileProvider**: 文件状态管理

##### 5. 工具类 (Utils)
- **FileUtils**: 文件操作工具
- **TaskEstimator**: 任务估算工具
- **TooltipUtils**: 提示工具
- **UIUtils**: UI工具

##### 6. 组件 (Widgets)
- **ComposeConfigPanel**: 策略配置面板
- **ComposeDirectoryPanel**: 目录配置面板
- **ComposeParameterPanel**: 参数配置面板
- **ComposePipelinePanel**: 流水线配置面板
- **ComposePreconditionPanel**: 前置条件配置面板
- **PreconditionConfigPanel**: 前置条件配置面板
- **StrategyConfigCard**: 策略配置卡片
- **RenameRuleEditor**: 重命名规则编辑器

### 2.2 状态管理

#### 2.2.1 Provider架构
```dart
class ConfigProvider extends ChangeNotifier {
  StrategyConfig? _currentConfig;
  List<StrategyInfo> _availableStrategies;
  
  StrategyConfig? get currentConfig => _currentConfig;
  List<StrategyInfo> get availableStrategies => _availableStrategies;
  
  Future<void> loadStrategies() async {
    _availableStrategies = await StrategyService.getStrategies();
    notifyListeners();
  }
  
  Future<void> loadConfig(String strategyId) async {
    _currentConfig = await StrategyService.getConfig(strategyId);
    notifyListeners();
  }
  
  Future<void> saveConfig(String strategyId, StrategyConfig config) async {
    await StrategyService.updateConfig(strategyId, config);
    _currentConfig = config;
    notifyListeners();
  }
}
```

#### 2.2.2 状态生命周期
```
初始化 → 加载数据 → 用户交互 → 更新状态 → 保存数据 → 清理资源
```

### 2.3 通信机制

#### 2.3.1 HTTP通信
```dart
class ApiClient {
  final Dio _dio = Dio(BaseOptions(
    baseUrl: 'http://localhost:8080',
    connectTimeout: Duration(seconds: 30),
    receiveTimeout: Duration(seconds: 30),
  ));
  
  Future<T> get<T>(String path) async {
    final response = await _dio.get(path);
    return response.data as T;
  }
  
  Future<T> post<T>(String path, dynamic data) async {
    final response = await _dio.post(path, data: data);
    return response.data as T;
  }
}
```

#### 2.3.2 WebSocket通信
```dart
class WebSocketClient {
  late WebSocketChannel _channel;
  final StreamController<String> _controller = StreamController<String>.broadcast();
  
  Stream<String> get stream => _controller.stream;
  
  Future<void> connect(String url) async {
    _channel = WebSocketChannel.connect(Uri.parse(url));
    _channel.stream.listen((message) {
      _controller.add(message);
    });
  }
  
  void send(String message) {
    _channel.sink.add(message);
  }
  
  Future<void> disconnect() async {
    await _channel.sink.close();
    await _controller.close();
  }
}
```

## 三、后端架构设计

### 3.1 整体架构

#### 3.1.1 分层架构
```
Controller Layer (REST API)
         ↓
Service Layer (Business Logic)
         ↓
Plugin Layer (Plugin System)
         ↓
Data Layer (File System, Configuration)
```

#### 3.1.2 核心组件

##### 1. 控制器层 (Controllers)
- **StrategyController**: 策略控制器，处理策略相关请求
- **PluginController**: 插件控制器，处理插件相关请求
- **PipelineController**: 流水线控制器，处理流水线相关请求
- **FileController**: 文件控制器，处理文件相关请求
- **TaskController**: 任务控制器，处理任务相关请求
- **LogController**: 日志控制器，处理日志相关请求
- **ConfigController**: 配置控制器，处理配置相关请求
- **EnumController**: 枚举控制器，处理枚举相关请求
- **SourceDirectoryController**: 源目录控制器，处理源目录相关请求
- **ThreadPoolController**: 线程池控制器，处理线程池相关请求

##### 2. 服务层 (Services)
- **StrategyService**: 策略服务，提供策略的管理和执行功能
- **PluginService**: 插件服务，提供插件的管理和执行功能
- **PipelineTaskManager**: 流水线任务管理器，管理流水线任务的执行
- **FileService**: 文件服务，提供文件操作功能
- **TaskService**: 任务服务，提供任务管理功能
- **LogService**: 日志服务，提供日志管理功能

##### 3. 插件系统 (Plugin System)
- **IPlugin**: 插件接口，定义插件的基本功能
- **PluginRegistry**: 插件注册表，管理所有插件的加载和卸载
- **PluginLoader**: 插件加载器，负责从JAR文件中加载插件
- **ExecutionContext**: 执行上下文，提供插件执行时的环境信息

##### 4. 配置层 (Configuration)
- **AppConfig**: 应用配置，管理应用的配置参数
- **SecurityConfig**: 安全配置，管理安全相关配置
- **WebSocketConfig**: WebSocket配置，管理WebSocket相关配置

### 3.2 REST API设计

#### 3.2.1 API设计原则
- **RESTful风格**: 遵循RESTful API设计规范
- **统一响应格式**: 使用统一的响应格式
- **错误处理**: 统一的错误处理机制
- **版本控制**: 支持API版本控制
- **认证授权**: 支持认证和授权

#### 3.2.2 统一响应格式
```json
{
  "success": true,
  "data": {},
  "message": "操作成功",
  "timestamp": "2026-02-08T22:00:00Z"
}
```

#### 3.2.3 错误响应格式
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "错误描述",
    "details": {}
  },
  "timestamp": "2026-02-08T22:00:00Z"
}
```

### 3.3 WebSocket设计

#### 3.3.1 WebSocket端点
- **/ws/file-operation**: 文件操作WebSocket，用于推送文件操作进度
- **/ws/progress**: 进度更新WebSocket，用于推送任务执行进度
- **/ws/task**: 任务状态WebSocket，用于推送任务状态变化

#### 3.3.2 WebSocket消息格式
```json
{
  "type": "progress",
  "data": {
    "taskId": "task-id",
    "progress": 50,
    "message": "处理中..."
  }
}
```

## 四、插件系统设计

### 4.1 插件接口定义

#### 4.1.1 IPlugin接口
```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    PluginConfigDTO getDefaultConfig();
    List<PluginParameterDTO> getParameters();
    List<PreconditionGroupDTO> getDefaultPreconditionGroups();
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
}
```

#### 4.1.2 插件生命周期
```
加载 → 初始化 → 配置 → 执行 → 清理 → 卸载
```

### 4.2 插件发现机制

#### 4.2.1 ServiceLoader机制
```java
public class PluginLoader {
    public static List<IPlugin> loadPlugins() {
        ServiceLoader<IPlugin> loader = ServiceLoader.load(IPlugin.class);
        List<IPlugin> plugins = new ArrayList<>();
        for (IPlugin plugin : loader) {
            plugins.add(plugin);
        }
        return plugins;
    }
}
```

#### 4.2.2 插件注册
```java
public class PluginRegistry {
    private final Map<String, IPlugin> plugins = new ConcurrentHashMap<>();
    
    public void register(IPlugin plugin) {
        plugins.put(plugin.getId(), plugin);
    }
    
    public IPlugin getPlugin(String id) {
        return plugins.get(id);
    }
    
    public List<IPlugin> getAllPlugins() {
        return new ArrayList<>(plugins.values());
    }
}
```

### 4.3 插件执行环境

#### 4.3.1 ExecutionContext
```java
public class ExecutionContext {
    private final String taskId;
    private final ProgressCallback progressCallback;
    private final LogCallback logCallback;
    
    public void reportProgress(int progress, String message) {
        progressCallback.onProgress(taskId, progress, message);
    }
    
    public void log(String level, String message) {
        logCallback.onLog(taskId, level, message);
    }
}
```

## 五、策略系统设计

### 5.1 策略接口定义

#### 5.1.1 StrategyConfigurable接口
```java
public interface StrategyConfigurable {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    List<ConfigFieldDTO> getConfigFields();
    StrategyConfigDTO initializeDefaultConfig();
    boolean validateConfig(StrategyConfigDTO config);
}
```

### 5.2 策略配置设计

#### 5.2.1 ConfigFieldDTO
```java
public class ConfigFieldDTO {
    private String name;
    private String label;
    private String type;
    private Object defaultValue;
    private String description;
    private boolean required;
    private List<String> options;
    private List<EnumOptionDTO> enumOptions;
    private String dependsOn;
    private Object dependsValue;
    private boolean isModule;
    private String moduleType;
}
```

#### 5.2.2 条件参数
```java
public class ConfigFieldDTO {
    // 当dependsOn字段的值等于dependsValue时，此字段才显示
    private String dependsOn;
    private Object dependsValue;
}
```

#### 5.2.3 模块化配置
```java
public class ConfigFieldDTO {
    // 模块化配置字段
    private boolean isModule;
    private String moduleType;
}
```

## 六、数据流设计

### 6.1 策略执行流程
```
用户选择策略
  ↓
前端加载策略配置
  ↓
用户配置策略参数
  ↓
前端发送执行请求
  ↓
后端接收请求
  ↓
后端验证配置
  ↓
后端执行策略
  ↓
后端返回执行结果
  ↓
前端显示执行结果
```

### 6.2 插件执行流程
```
用户选择插件
  ↓
前端加载插件配置
  ↓
用户配置插件参数
  ↓
前端发送执行请求
  ↓
后端接收请求
  ↓
后端从PluginRegistry获取插件
  ↓
后端创建ExecutionContext
  ↓
后端执行插件
  ↓
后端返回执行结果
  ↓
前端显示执行结果
```

### 6.3 流水线执行流程
```
用户配置流水线
  ↓
前端发送流水线配置
  ↓
后端保存流水线配置
  ↓
用户执行流水线
  ↓
后端按顺序执行流水线中的插件/策略
  ↓
后端返回执行结果
  ↓
前端显示执行结果
```

## 七、配置管理

### 7.1 后端配置

#### 7.1.1 application.yml
```yaml
server:
  port: 8080

spring:
  application:
    name: FileManager Plus Backend
  
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

logging:
  level:
    root: INFO
    com.filemanager: DEBUG
  file:
    name: logs/application.log
```

### 7.2 前端配置

#### 7.2.1 pubspec.yaml
```yaml
name: music_manager_plus
description: FileManager Plus Flutter Web Client
version: 1.0.0

environment:
  sdk: '>=3.10.0 <4.0.0'

dependencies:
  flutter:
    sdk: flutter
  provider: ^6.1.1
  dio: ^5.4.0
  web_socket_channel: ^2.4.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.0
```

## 八、安全设计

### 8.1 认证授权

#### 8.1.1 Spring Security配置
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/**").permitAll()
                .antMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            .and()
            .csrf().disable()
            .cors();
    }
}
```

### 8.2 数据保护

#### 8.2.1 文件操作安全
- 文件路径验证
- 文件权限检查
- 文件大小限制
- 文件类型验证

#### 8.2.2 配置数据保护
- 配置数据加密
- 配置数据备份
- 配置数据验证

## 九、性能优化

### 9.1 前端性能优化

#### 9.1.1 代码优化
- 使用const构造函数
- 避免不必要的重建
- 使用ListView.builder
- 使用FutureBuilder

#### 9.1.2 资源优化
- 图片压缩
- 代码分割
- 懒加载

### 9.2 后端性能优化

#### 9.2.1 并发处理
- 使用线程池
- 异步处理
- 批量操作

#### 9.2.2 缓存机制
- 插件缓存
- 配置缓存
- 文件缓存

## 十、监控和日志

### 10.1 日志系统

#### 10.1.1 日志级别
- ERROR: 错误信息
- WARN: 警告信息
- INFO: 一般信息
- DEBUG: 调试信息

#### 10.1.2 日志格式
```
[时间戳] [级别] [类名] 消息内容
```

### 10.2 监控系统

#### 10.2.1 性能监控
- 响应时间监控
- 吞吐量监控
- 错误率监控

#### 10.2.2 资源监控
- CPU使用率
- 内存使用率
- 磁盘使用率

## 十一、部署架构

### 11.1 开发环境
- 前端：http://localhost:8081
- 后端：http://localhost:8080
- 数据库：H2内存数据库

### 11.2 生产环境
- 前端：Nginx反向代理
- 后端：Spring Boot内嵌Tomcat
- 数据库：PostgreSQL/MySQL

### 11.3 部署方式
- Docker容器化部署
- Kubernetes集群部署
- CI/CD自动化部署

## 十二、扩展性设计

### 12.1 插件扩展
- 支持外部插件加载
- 支持插件热加载
- 支持插件依赖管理

### 12.2 策略扩展
- 支持动态策略注册
- 支持策略组合
- 支持策略优先级

### 12.3 API扩展
- 支持API版本控制
- 支持API文档自动生成
- 支持API测试工具

## 十三、测试架构

### 13.1 单元测试
- 前端：Flutter Test
- 后端：JUnit + Mockito

### 13.2 集成测试
- 前后端集成测试
- 插件集成测试
- API集成测试

### 13.3 端到端测试
- 用户流程测试
- 功能完整性测试
- 性能测试

## 十四、总结

FileManager Plus 新架构采用前后端分离的微服务架构，支持插件系统和策略系统，提供了灵活的文件管理和处理能力。架构设计清晰，易于扩展和维护，为后续的功能扩展和性能优化提供了良好的基础。

---

**文档版本**: 1.0  
**创建日期**: 2026-02-08  
**维护者**: FileManager Plus Team
