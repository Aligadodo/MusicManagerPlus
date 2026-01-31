# FileManager Plus 项目架构设计文档

## 项目概述

FileManager Plus 是一个基于前后端分离架构的文件管理系统，支持插件系统和策略系统，用于管理和处理音乐文件的收集、整理、转换等操作。

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.18
- **Java版本**: JDK 21
- **构建工具**: Maven
- **主要功能模块**:
  - RESTful API服务
  - WebSocket实时通信
  - 插件系统
  - 策略系统
  - 任务管理

### 前端
- **框架**: Flutter Web
- **Dart版本**: 3.10.8
- **构建工具**: Flutter SDK
- **主要功能模块**:
  - 文件浏览器
  - 策略配置界面
  - 插件配置界面
  - 流水线配置界面
  - 任务监控界面
  - 日志查看界面

## 项目结构

```
FileManagerPlus/
├── backend/                    # 后端服务模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/filemanager/backend/
│   │   │   │       ├── config/      # 配置类
│   │   │   │       ├── controller/  # 控制器
│   │   │   │       └── service/     # 服务实现
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
├── clients/                   # 客户端模块
│   └── flutter-web-cli/       # Flutter Web客户端
│       ├── lib/
│       │   ├── api/           # API客户端
│       │   ├── models/        # 数据模型
│       │   ├── pages/         # 页面组件
│       │   └── utils/         # 工具类
│       ├── web/               # Web资源
│       └── pubspec.yaml
├── plugins/                   # 插件模块
│   ├── base/               # 插件基础框架
│   ├── file-operations/     # 文件操作插件
│   ├── file-cleanup/        # 文件清理插件
│   ├── file-collection/     # 文件收集插件
│   ├── file-rename/         # 文件重命名插件
│   ├── audio-converter/      # 音频转换插件
│   └── metadata-scraper/    # 元数据抓取插件
├── shared-domain/            # 共享领域模型
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/filemanager/domain/
│   │               ├── dto/      # 数据传输对象
│   │               ├── entity/   # 实体类
│   │               └── service/  # 服务接口
│   └── pom.xml
├── design/                   # 设计文档
├── dist/                     # 构建输出目录
└── pom.xml                   # Maven主配置文件
```

## 核心系统架构

### 1. 插件系统

#### 架构设计
插件系统采用Java的ServiceLoader机制，支持动态加载和管理插件。插件系统包括以下核心组件：

- **IPlugin接口**: 定义插件的基本功能
- **PluginRegistry**: 插件注册表，管理所有插件的加载和卸载
- **PluginLoader**: 插件加载器，负责从JAR文件中加载插件
- **PluginService**: 插件服务接口，提供插件的管理和执行功能

#### 插件接口定义
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

#### 插件加载机制
1. **内部插件**: 通过ServiceLoader从classpath中加载
2. **外部插件**: 通过PluginLoader从指定目录的JAR文件中加载
3. **插件注册**: 所有插件注册到PluginRegistry中统一管理

#### 现有插件
- FileCleanupPlugin: 文件清理插件
- FileCollectionPlugin: 文件收集插件
- AudioConverterPlugin: 音频转换插件
- FileRenamePlugin: 文件重命名插件
- MetadataScraperPlugin: 元数据抓取插件

### 2. 策略系统

#### 架构设计
策略系统提供了一种更简单的方式来定义文件处理策略，支持条件参数和模块化配置。策略系统包括以下核心组件：

- **StrategyService**: 策略服务接口，提供策略的管理和配置功能
- **StrategyServiceImpl**: 策略服务实现，管理所有策略的配置
- **StrategyInfoDTO**: 策略信息数据传输对象
- **StrategyConfigDTO**: 策略配置数据传输对象
- **ConfigFieldDTO**: 配置字段数据传输对象，支持条件参数

#### 策略配置字段
策略配置字段支持以下特性：
- **条件参数**: 通过dependsOn和dependsValue实现参数的动态显示
- **模块化配置**: 通过isModule和moduleType实现模块化配置
- **多种数据类型**: 支持string、number、boolean、select、list等类型

#### 现有策略
1. **FileMigrateStrategy**: 文件迁移策略
2. **AlbumDirNormalizeStrategy**: 专辑目录标准化策略
3. **FileUnzipStrategy**: 文件解压策略
4. **AudioFormatConvertStrategy**: 音频格式转换策略
5. **AudioTagNormalizeStrategy**: 音频标签标准化策略
6. **AudioQualityCheckStrategy**: 音频质量检查策略
7. **FileDuplicateCheckStrategy**: 文件重复检查策略
8. **FileOrganizeStrategy**: 文件整理策略
9. **FileCleanupStrategy**: 文件清理策略
10. **AudioMetadataExtractStrategy**: 音频元数据提取策略
11. **FileBackupStrategy**: 文件备份策略
12. **FileArchiveStrategy**: 文件归档策略

### 3. 前端架构

#### 页面结构
- **HomePage**: 主页面，提供导航功能
- **FileBrowserPage**: 文件浏览器，用于浏览和选择文件
- **ComposePage**: 策略配置页面，用于配置和执行策略
- **PipelineConfigPage**: 流水线配置页面，用于配置插件和策略的流水线
- **PluginConfigPage**: 插件配置页面，用于配置插件参数
- **StrategyConfigPage**: 策略配置页面，用于配置策略参数
- **TaskMonitorPage**: 任务监控页面，用于监控任务执行状态
- **LogPage**: 日志查看页面，用于查看系统日志
- **GlobalSettingsPage**: 全局设置页面，用于配置系统全局参数

#### API客户端
- **ApiClient**: 基础API客户端，提供HTTP请求功能
- **StrategyService**: 策略服务客户端
- **PluginService**: 插件服务客户端
- **PipelineService**: 流水线服务客户端
- **FileService**: 文件服务客户端
- **TaskService**: 任务服务客户端
- **LogService**: 日志服务客户端

#### 数据模型
- **StrategyInfo**: 策略信息模型
- **StrategyConfig**: 策略配置模型
- **ConfigField**: 配置字段模型
- **PluginInfo**: 插件信息模型
- **PluginConfig**: 插件配置模型
- **ChangeRecord**: 变更记录模型

### 4. 后端API设计

#### RESTful API端点

##### 策略相关API
- `GET /api/strategies`: 获取所有策略
- `GET /api/strategies/{id}`: 获取指定策略信息
- `GET /api/strategies/{id}/config`: 获取策略配置
- `POST /api/strategies/{id}/config`: 更新策略配置
- `POST /api/strategies/{id}/execute`: 执行策略
- `POST /api/strategies/{id}/preview`: 预览策略执行结果

##### 插件相关API
- `GET /api/plugins`: 获取所有插件
- `GET /api/plugins/{id}`: 获取指定插件信息
- `GET /api/plugins/{id}/config`: 获取插件配置
- `POST /api/plugins/{id}/config`: 更新插件配置
- `POST /api/plugins/{id}/execute`: 执行插件
- `POST /api/plugins/{id}/preview`: 预览插件执行结果
- `POST /api/plugins/reload`: 重新加载插件
- `GET /api/plugins/internal`: 获取内部插件
- `GET /api/plugins/external`: 获取外部插件
- `POST /api/plugins/scan`: 扫描外部插件
- `POST /api/plugins/load-external`: 加载外部插件
- `POST /api/plugins/reload-external`: 重新加载外部插件

##### 流水线相关API
- `GET /api/pipeline`: 获取流水线配置
- `POST /api/pipeline`: 更新流水线配置

##### 文件相关API
- `GET /api/files`: 获取文件列表
- `POST /api/files/scan`: 扫描文件
- `POST /api/files/move`: 移动文件
- `POST /api/files/copy`: 复制文件
- `POST /api/files/delete`: 删除文件

##### 任务相关API
- `GET /api/tasks`: 获取任务列表
- `GET /api/tasks/{id}`: 获取指定任务信息
- `POST /api/tasks`: 创建任务
- `POST /api/tasks/{id}/cancel`: 取消任务

##### 日志相关API
- `GET /api/logs`: 获取日志列表
- `GET /api/logs/{id}`: 获取指定日志信息

#### WebSocket端点
- `/ws/file-operation`: 文件操作WebSocket
- `/ws/progress`: 进度更新WebSocket
- `/ws/task`: 任务状态WebSocket

## 数据流设计

### 1. 策略执行流程
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
后端执行策略
  ↓
后端返回执行结果
  ↓
前端显示执行结果
```

### 2. 插件执行流程
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
后端执行插件
  ↓
后端返回执行结果
  ↓
前端显示执行结果
```

### 3. 流水线执行流程
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

## 配置管理

### 后端配置
- **application.yml**: Spring Boot应用配置文件
  - 服务器端口配置
  - 数据库配置
  - 日志配置
  - WebSocket配置

### 前端配置
- **pubspec.yaml**: Flutter项目配置文件
  - 依赖管理
  - 资源配置
  - 构建配置

## 构建和部署

### 后端构建
```bash
mvn clean package -DskipTests
```

### 前端构建
```bash
flutter build web
```

### 一键构建
```bash
./build_dist_jdk21_macos.sh
```

### 服务启动
```bash
# 启动后端
./dist/start-backend.sh

# 启动前端
./dist/start-frontend.sh

# 一键启动
./dist/start-all.sh
```

### 服务停止
```bash
./dist/stop-all.sh
```

### 服务重启
```bash
./dist/restart-all.sh
```

## 访问地址

- **后端服务**: http://localhost:8080
- **前端服务**: http://localhost:8081

## 技术特点

### 1. 前后端分离
- 前端和后端独立开发和部署
- 通过RESTful API进行通信
- 支持WebSocket实时通信

### 2. 插件化架构
- 支持动态加载插件
- 插件可独立开发和部署
- 插件之间相互独立

### 3. 策略系统
- 提供简单的策略配置方式
- 支持条件参数和模块化配置
- 易于扩展新的策略

### 4. 实时通信
- 使用WebSocket实现实时通信
- 支持文件操作进度更新
- 支持任务状态实时更新

### 5. 跨平台
- 后端支持Java跨平台
- 前端支持Web跨平台
- 支持Windows、macOS、Linux

## 未来规划

1. **性能优化**: 优化文件处理性能，支持并发处理
2. **功能扩展**: 增加更多的插件和策略
3. **用户体验**: 优化前端界面，提升用户体验
4. **安全性**: 加强系统安全性，保护用户数据
5. **国际化**: 支持多语言，方便国际化使用

## 总结

FileManager Plus是一个功能完善的文件管理系统，采用前后端分离架构，支持插件系统和策略系统，提供了灵活的文件处理能力。系统设计清晰，易于扩展和维护。