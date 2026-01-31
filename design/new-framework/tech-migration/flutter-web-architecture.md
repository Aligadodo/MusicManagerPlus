# Flutter Web + 后端 API 架构设计

## 1. 整体架构概述

### 1.1 架构目标
- **解耦底层能力与上层交互**：核心业务逻辑与 UI 层完全分离
- **支持多端并行**：JavaFX 客户端与 Flutter Web 客户端并行运行
- **可扩展性**：业务逻辑通过插件机制扩展
- **维护性**：清晰的模块划分，便于独立开发和测试

### 1.2 模块划分

```
┌─────────────────────────────────────────────────────┐
│                      项目根目录                       │
├─────────────────────────────────────────────────────┤
│ ┌───────────────────┐  ┌───────────────────┐       │
│ │                   │  │                   │       │
│ │  shared-domain    │  │    plugins        │       │
│ │  (核心实体与接口)  │  │  (业务插件模块)    │       │
│ │                   │  │                   │       │
│ └───────────────────┘  └───────────────────┘       │
│          ▲                      ▲                  │
│          │                      │                  │
│          ▼                      ▼                  │
│ ┌─────────────────────────────────────────────┐    │
│ │                 backend                     │    │
│ │           (服务端模块)                       │    │
│ └─────────────────────────────────────────────┘    │
│                        ▲                          │
│                        │                          │
│                        ▼                          │
│ ┌─────────────────────────────────────────────┐    │
│ │                  clients                    │    │
│ │ ┌─────────────┐  ┌────────────────────────┐ │    │
│ │ │ javafx-cli  │  │  flutter-web-cli      │ │    │
│ │ │ (旧客户端)  │  │  (新Web客户端)         │ │    │
│ │ └─────────────┘  └────────────────────────┘ │    │
│ └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

## 2. 核心模块设计

### 2.1 shared-domain 模块

#### 2.1.1 模块职责
- 定义核心实体类和数据模型
- 定义业务接口和服务契约
- 提供公共工具类和常量
- 不依赖任何具体实现

#### 2.1.2 包结构
```
shared-domain/
├── src/main/java/com/filemanager/domain/
│   ├── entity/            # 核心实体类
│   │   ├── ChangeRecord.java
│   │   ├── RuleCondition.java
│   │   ├── RuleConditionGroup.java
│   │   └── ...
│   ├── service/           # 服务接口定义
│   │   ├── FileService.java
│   │   ├── StrategyService.java
│   │   ├── TaskService.java
│   │   └── ...
│   ├── dto/               # 数据传输对象
│   │   ├── FileInfoDTO.java
│   │   ├── TaskProgressDTO.java
│   │   ├── StrategyConfigDTO.java
│   │   └── ...
│   ├── exception/         # 异常定义
│   │   ├── DomainException.java
│   │   └── ...
│   └── util/              # 公共工具
│       ├── FileUtil.java
│       ├── StringUtil.java
│       └── ...
└── pom.xml                # Maven 配置
```

#### 2.1.3 核心接口设计

##### FileService 接口
```java
public interface FileService {
    List<FileInfoDTO> scanDirectory(String path, int minDepth, int maxDepth);
    FileInfoDTO getFileInfo(String path);
    boolean exists(String path);
    boolean isDirectory(String path);
    String getParentPath(String path);
}
```

##### StrategyService 接口
```java
public interface StrategyService {
    List<StrategyInfoDTO> getAvailableStrategies();
    StrategyConfigDTO getStrategyConfig(String strategyId);
    void updateStrategyConfig(String strategyId, StrategyConfigDTO config);
    List<ChangeRecord> analyzeWithStrategy(String strategyId, List<String> filePaths, StrategyConfigDTO config);
}
```

##### TaskService 接口
```java
public interface TaskService {
    String createTask(TaskRequestDTO request);
    TaskStatusDTO getTaskStatus(String taskId);
    List<TaskStatusDTO> getTasks();
    void cancelTask(String taskId);
    void executeTask(String taskId);
}
```

### 2.2 plugins 模块

#### 2.2.1 模块职责
- 实现具体的业务策略插件
- 提供策略配置和执行逻辑
- 通过 SPI 机制集成到服务端

#### 2.2.2 包结构
```
plugins/
├── base/                  # 插件基础定义
│   ├── src/main/java/com/filemanager/plugin/
│   │   ├── IPlugin.java
│   │   ├── PluginRegistry.java
│   │   └── ...
├── file-collection/       # 文件收集插件
│   ├── src/main/java/com/filemanager/plugin/collection/
│   │   ├── FileCollectionPlugin.java
│   │   ├── FileCollectionConfig.java
│   │   └── ...
├── metadata-scraper/      # 元数据抓取插件
│   ├── src/main/java/com/filemanager/plugin/scraper/
│   │   ├── MetadataScraperPlugin.java
│   │   ├── MetadataScraperConfig.java
│   │   └── ...
├── file-cleanup/          # 文件清理插件
│   ├── src/main/java/com/filemanager/plugin/cleanup/
│   │   ├── FileCleanupPlugin.java
│   │   ├── FileCleanupConfig.java
│   │   └── ...
└── pom.xml                # Maven 配置
```

#### 2.2.3 插件接口设计

##### IPlugin 接口
```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    PluginConfigDTO getDefaultConfig();
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
}
```

### 2.3 backend 模块

#### 2.3.1 模块职责
- 实现服务端核心逻辑
- 提供 RESTful API 接口
- 集成业务插件
- 管理任务执行和线程池

#### 2.3.2 包结构
```
backend/
├── src/main/java/com/filemanager/backend/
│   ├── config/            # 配置类
│   │   ├── AppConfig.java
│   │   ├── SecurityConfig.java
│   │   └── ...
│   ├── controller/        # API 控制器
│   │   ├── FileController.java
│   │   ├── StrategyController.java
│   │   ├── TaskController.java
│   │   └── ...
│   ├── service/           # 服务实现
│   │   ├── impl/          # 具体实现
│   │   │   ├── FileServiceImpl.java
│   │   │   ├── StrategyServiceImpl.java
│   │   │   ├── TaskServiceImpl.java
│   │   │   └── ...
│   │   └── ...
│   ├── repository/        # 数据访问
│   │   ├── ConfigRepository.java
│   │   ├── TaskRepository.java
│   │   └── ...
│   ├── util/              # 工具类
│   │   ├── ThreadPoolUtil.java
│   │   ├── FileOperationUtil.java
│   │   └── ...
│   └── Application.java    # 应用入口
├── src/main/resources/
│   ├── application.yml
│   └── ...
└── pom.xml                # Maven 配置
```

#### 2.3.3 API 接口设计

##### File API
- `GET /api/files/scan` - 扫描目录
- `GET /api/files/info` - 获取文件信息
- `POST /api/files/exists` - 检查文件是否存在

##### Strategy API
- `GET /api/strategies` - 获取可用策略列表
- `GET /api/strategies/{id}/config` - 获取策略配置
- `POST /api/strategies/{id}/config` - 更新策略配置
- `POST /api/strategies/{id}/analyze` - 分析文件

##### Task API
- `POST /api/tasks` - 创建任务
- `GET /api/tasks/{id}` - 获取任务状态
- `GET /api/tasks` - 获取任务列表
- `POST /api/tasks/{id}/execute` - 执行任务
- `POST /api/tasks/{id}/cancel` - 取消任务

### 2.4 clients 模块

#### 2.4.1 javafx-cli 模块

##### 模块职责
- 保留现有的 JavaFX 客户端
- 通过新的 API 客户端与后端通信
- 支持平滑过渡到新架构

##### 包结构
```
clients/javafx-cli/
├── src/main/java/com/filemanager/client/javafx/
│   ├── controller/        # 控制器
│   │   ├── MainController.java
│   │   ├── FileBrowserController.java
│   │   └── ...
│   ├── view/              # 视图
│   │   ├── MainView.java
│   │   ├── FileBrowserView.java
│   │   └── ...
│   ├── service/           # 客户端服务
│   │   ├── ApiClient.java
│   │   ├── FileServiceClient.java
│   │   └── ...
│   └── MainApp.java       # 应用入口
└── pom.xml                # Maven 配置
```

#### 2.4.2 flutter-web-cli 模块

##### 模块职责
- 实现 Flutter Web 客户端
- 提供现代化的 Web 界面
- 通过 API 与后端通信

##### 目录结构
```
clients/flutter-web-cli/
├── lib/
│   ├── main.dart          # 应用入口
│   ├── api/               # API 客户端
│   │   ├── api_client.dart
│   │   ├── file_service.dart
│   │   ├── strategy_service.dart
│   │   └── task_service.dart
│   ├── models/            # 数据模型
│   │   ├── file_info.dart
│   │   ├── strategy_info.dart
│   │   └── task_status.dart
│   ├── pages/             # 页面
│   │   ├── home_page.dart
│   │   ├── file_browser.dart
│   │   ├── strategy_config.dart
│   │   └── task_monitor.dart
│   ├── widgets/           # 组件
│   │   ├── file_item.dart
│   │   ├── strategy_card.dart
│   │   └── progress_bar.dart
│   └── utils/             # 工具类
│       ├── file_utils.dart
│       └── ui_utils.dart
├── web/
│   ├── index.html
│   └── ...
├── pubspec.yaml           # 依赖配置
└── ...
```

## 3. 核心实体和接口设计

### 3.1 核心实体

#### ChangeRecord
```java
public class ChangeRecord {
    private String id;
    private String originalName;
    private String newName;
    private String filePath;
    private boolean changed;
    private OperationType operationType;
    private ExecStatus status;
    private String failReason;
    // getters, setters, constructors
}
```

#### StrategyInfoDTO
```java
public class StrategyInfoDTO {
    private String id;
    private String name;
    private String description;
    private List<ConfigFieldDTO> configFields;
    // getters, setters, constructors
}
```

#### TaskStatusDTO
```java
public class TaskStatusDTO {
    private String taskId;
    private TaskStatus status;
    private double progress;
    private String message;
    private long startTime;
    private Long endTime;
    private List<ChangeRecord> changes;
    // getters, setters, constructors
}
```

### 3.2 接口设计

#### 服务端接口
- **FileController** - 处理文件相关 API 请求
- **StrategyController** - 处理策略相关 API 请求
- **TaskController** - 处理任务相关 API 请求
- **WebSocketController** - 处理实时任务状态更新

#### 客户端接口
- **ApiClient** - 基础 API 客户端
- **FileServiceClient** - 文件服务客户端
- **StrategyServiceClient** - 策略服务客户端
- **TaskServiceClient** - 任务服务客户端

## 4. 业务插件模块设计

### 4.1 插件加载机制
- 使用 Java SPI 机制加载插件
- 插件实现 IPlugin 接口
- 服务启动时自动发现和注册插件

### 4.2 插件配置管理
- 每个插件维护自己的配置结构
- 配置通过 API 暴露给客户端
- 配置持久化到文件系统或数据库

### 4.3 插件执行流程
1. 客户端选择插件并配置参数
2. 服务端验证配置并创建任务
3. 服务端执行插件逻辑
4. 插件生成变更记录
5. 服务端保存执行结果
6. 客户端获取执行状态和结果

## 5. 客户端模块设计

### 5.1 JavaFX 客户端
- 保留现有界面和功能
- 替换内部实现为 API 调用
- 支持与新架构并行运行
- 提供平滑过渡方案

### 5.2 Flutter Web 客户端
- 现代化的响应式界面
- 支持实时任务状态更新
- 提供文件浏览器功能
- 支持策略配置和执行
- 响应式设计，适配不同设备

## 6. 服务端模块设计

### 6.1 核心功能
- 文件系统操作
- 策略管理和执行
- 任务调度和监控
- 配置管理
- 安全控制

### 6.2 技术选型
- **框架**：Spring Boot 3.x
- **语言**：Java 21+
- **API**：RESTful + WebSocket
- **持久化**：文件系统 + 可选数据库
- **安全**：JWT 认证

### 6.3 部署方案
- **本地部署**：可执行 JAR 文件
- **容器部署**：Docker 容器
- **云部署**：支持各种云平台

## 7. 实施路径

### 7.1 阶段一：核心模块拆分
1. 提取 shared-domain 模块
2. 重构现有代码到新模块结构
3. 定义服务接口和数据模型

### 7.2 阶段二：服务端实现
1. 实现 backend 模块
2. 集成业务插件
3. 实现 API 接口
4. 测试服务端功能

### 7.3 阶段三：客户端迁移
1. 改造 JavaFX 客户端
2. 实现 Flutter Web 客户端
3. 测试多端并行运行

### 7.4 阶段四：集成测试
1. 端到端测试
2. 性能测试
3. 安全测试
4. 用户验收测试

### 7.5 阶段五：部署和切换
1. 部署服务端
2. 部署 Flutter Web 客户端
3. 逐步迁移用户到新客户端
4. 保留 JavaFX 客户端作为备份

## 8. 关键技术挑战与解决方案

### 8.1 文件系统访问
- **挑战**：Web 客户端无法直接访问本地文件系统
- **解决方案**：
  - 服务端提供文件浏览和操作 API
  - 实现文件上传/下载功能
  - 支持文件路径映射配置

### 8.2 实时任务状态
- **挑战**：Web 客户端需要实时获取任务状态
- **解决方案**：
  - 使用 WebSocket 进行实时通信
  - 实现任务状态推送机制
  - 提供轮询作为降级方案

### 8.3 插件配置界面
- **挑战**：不同插件有不同的配置需求
- **解决方案**：
  - 实现动态表单生成
  - 插件提供配置元数据
  - 前端根据元数据渲染表单

### 8.4 性能优化
- **挑战**：大文件处理和并发任务执行
- **解决方案**：
  - 服务端实现多线程处理
  - 使用文件流处理大文件
  - 实现任务队列和优先级管理

## 9. 技术栈选择

### 9.1 服务端
- **框架**：Spring Boot 3.2+
- **语言**：Java 21+
- **构建工具**：Maven 3.9+
- **依赖管理**：Maven

### 9.2 Flutter Web 客户端
- **框架**：Flutter 3.16+
- **语言**：Dart 3.2+
- **状态管理**：Riverpod / Provider
- **UI 组件**：Flutter Material 3 / Cupertino
- **网络**：Dio / http
- **构建工具**：Flutter CLI

### 9.3 开发工具
- **IDE**：IntelliJ IDEA / Android Studio
- **版本控制**：Git
- **CI/CD**：GitHub Actions / Jenkins

## 10. 结论与建议

### 10.1 架构优势
- **解耦程度高**：核心业务逻辑与 UI 完全分离
- **多端支持**：同时支持 JavaFX 和 Flutter Web 客户端
- **可扩展性强**：通过插件机制扩展业务功能
- **维护性好**：清晰的模块划分，便于独立开发和测试

### 10.2 实施建议
1. **优先拆分核心模块**：先提取 shared-domain 和 plugins 模块
2. **服务端先行**：优先实现服务端 API，为客户端提供基础
3. **并行开发**：JavaFX 改造和 Flutter 开发可并行进行
4. **增量迁移**：逐步将功能从 JavaFX 迁移到 Flutter Web
5. **充分测试**：每个阶段都要进行充分的测试，确保功能正常

### 10.3 预期收益
- **开发效率提升**：模块化设计，便于团队协作
- **部署灵活性**：服务端可独立部署，客户端可通过浏览器访问
- **用户体验改善**：现代化的 Flutter Web 界面
- **维护成本降低**：清晰的代码结构，便于问题定位和修复
- **技术栈现代化**：采用最新的 Spring Boot 和 Flutter 技术

---

**架构设计文档**：Flutter Web + 后端 API 架构设计  
**版本**：1.0  
**日期**：2026-01-30  
**适用范围**：FileManager Plus 项目技术迁移