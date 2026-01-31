# FileManager Plus 迁移计划

## 1. 概述

本文档详细规划了将 FileManager Plus 从 JavaFX 架构迁移到 Flutter Web + Spring Boot 架构的步骤和实施计划。迁移将保持原应用的所有核心功能，同时充分利用新架构的优势。

## 2. 功能迁移映射

### 2.1 核心功能映射

| 功能模块 | 原 JavaFX 实现 | 新架构实现 | 状态 |
|---------|--------------|-----------|------|
| 源目录管理 | `ComposeView` 左侧面板 | Flutter 源目录管理页面 | 待实现 |
| 策略流水线 | `ComposeView` 中间面板 | Flutter 策略流水线配置页面 | 待实现 |
| 预览分析 | `PreviewView` | Flutter 预览分析页面 | 待实现 |
| 执行变更 | `FileManagerPlusApp.runPipelineExecution()` | Flutter 执行页面 + 后端 API | 待实现 |
| 线程池管理 | `ThreadPoolManager` | 后端 ThreadPoolController + Flutter 配置页面 | 待实现 |
| 配置管理 | `ConfigFileManager` | 后端 ConfigController + Flutter 配置页面 | 待实现 |
| 日志管理 | `LogView` | 后端 LogController + Flutter 日志页面 | 待实现 |
| 界面设置 | `AppearanceManager` | Flutter 界面设置页面 | 待实现 |

### 2.2 插件系统迁移

| 插件 | 原实现 | 新架构实现 | 状态 |
|------|-------|-----------|------|
| 文件收集插件 | `FileCollectionStrategy` | 新插件实现 + API | 待实现 |
| 元数据抓取插件 | `MetadataScraperStrategy` | 新插件实现 + API | 待实现 |
| 文件清理插件 | `FileCleanupStrategy` | 新插件实现 + API | 待实现 |
| 其他自定义插件 | 各种策略实现 | 新插件实现 + API | 待实现 |

## 3. 后端 API 设计

### 3.1 核心 API 端点

| 功能模块 | API 路径 | 方法 | 功能描述 |
|---------|----------|------|----------|
| 源目录管理 | `/api/source-directories` | GET | 获取源目录列表 |
| 源目录管理 | `/api/source-directories` | POST | 添加源目录 |
| 源目录管理 | `/api/source-directories/{id}` | DELETE | 移除源目录 |
| 源目录管理 | `/api/source-directories` | DELETE | 清空源目录 |
| 源目录管理 | `/api/source-directories/{id}/threads` | PUT | 更新源目录线程数 |
| 策略管理 | `/api/strategies` | GET | 获取所有可用策略 |
| 策略管理 | `/api/strategies/{id}` | GET | 获取策略详情 |
| 策略管理 | `/api/strategies/{id}/config` | GET | 获取策略配置 |
| 策略管理 | `/api/strategies/{id}/config` | POST | 更新策略配置 |
| 策略管理 | `/api/strategies/{id}/analyze` | POST | 分析文件变更 |
| 策略管理 | `/api/strategies/{id}/execute` | POST | 执行策略 |
| 流水线管理 | `/api/pipeline` | GET | 获取当前流水线 |
| 流水线管理 | `/api/pipeline` | POST | 更新流水线 |
| 流水线管理 | `/api/pipeline/analyze` | POST | 分析流水线 |
| 流水线管理 | `/api/pipeline/execute` | POST | 执行流水线 |
| 线程池管理 | `/api/thread-pool` | GET | 获取线程池配置 |
| 线程池管理 | `/api/thread-pool/preview` | PUT | 更新预览线程数 |
| 线程池管理 | `/api/thread-pool/execution` | PUT | 更新执行线程数 |
| 配置管理 | `/api/config` | GET | 获取配置 |
| 配置管理 | `/api/config` | POST | 更新配置 |
| 配置管理 | `/api/config/save` | POST | 保存配置 |
| 配置管理 | `/api/config/load` | POST | 加载配置 |
| 配置管理 | `/api/config/reset` | POST | 重置配置 |
| 日志管理 | `/api/logs` | GET | 获取日志 |
| 日志管理 | `/api/logs` | DELETE | 清空日志 |
| 插件管理 | `/api/plugins` | GET | 获取所有插件 |
| 插件管理 | `/api/plugins/{id}` | GET | 获取插件详情 |
| 插件管理 | `/api/plugins/{id}/config` | GET | 获取插件配置 |
| 插件管理 | `/api/plugins/{id}/config` | POST | 更新插件配置 |
| 插件管理 | `/api/plugins/{id}/execute` | POST | 执行插件 |
| 插件管理 | `/api/plugins/reload` | POST | 重新加载插件 |

### 3.2 WebSocket 端点

| 端点 | 功能描述 |
|------|----------|
| `/ws/progress` | 实时任务进度更新 |
| `/ws/task` | 实时任务状态更新 |
| `/ws/file` | 实时文件操作更新 |

## 4. 前端 Flutter 实现

### 4.1 页面结构

| 页面 | 功能描述 | 对应原功能 |
|------|----------|-----------|
| 首页 | 应用入口，显示概览 | 原主界面 |
| 源目录管理页面 | 管理源目录，包括添加、移除、清空 | 原 ComposeView 左侧面板 |
| 策略流水线配置页面 | 配置策略流水线，包括添加、移除、排序策略 | 原 ComposeView 中间面板 |
| 策略参数配置页面 | 配置策略的详细参数 | 原 ComposeView 右侧面板 |
| 预览分析页面 | 显示文件变更预览 | 原 PreviewView |
| 执行页面 | 执行文件变更 | 原执行功能 |
| 线程池配置页面 | 配置线程池参数 | 原线程池管理 |
| 配置管理页面 | 管理应用配置 | 原配置管理 |
| 日志页面 | 显示系统日志 | 原 LogView |
| 界面设置页面 | 配置应用外观 | 原 AppearanceManager |

### 4.2 状态管理

使用 Riverpod 进行状态管理，创建以下主要 Provider：

| Provider | 功能 | 范围 |
|----------|------|------|
| sourceDirectoriesProvider | 源目录管理 | 全局 |
| pipelineProvider | 策略流水线管理 | 全局 |
| previewProvider | 预览分析管理 | 全局 |
| threadPoolProvider | 线程池管理 | 全局 |
| configProvider | 配置管理 | 全局 |
| logProvider | 日志管理 | 全局 |
| appearanceProvider | 界面设置 | 全局 |

### 4.3 API 客户端

创建以下 API 客户端服务：

| 服务 | 功能 | 文件路径 |
|------|------|----------|
| SourceDirectoryService | 源目录管理 API | `lib/api/source_directory_service.dart` |
| StrategyService | 策略管理 API | `lib/api/strategy_service.dart` |
| PipelineService | 流水线管理 API | `lib/api/pipeline_service.dart` |
| ThreadPoolService | 线程池管理 API | `lib/api/thread_pool_service.dart` |
| ConfigService | 配置管理 API | `lib/api/config_service.dart` |
| LogService | 日志管理 API | `lib/api/log_service.dart` |
| PluginService | 插件管理 API | `lib/api/plugin_service.dart` |

## 5. 插件系统迁移

### 5.1 插件架构

新插件系统基于 Java SPI 机制，与原系统类似，但提供更统一的 API 接口：

1. **核心接口**：`IPlugin` 接口定义插件的标准方法
2. **插件注册表**：`PluginRegistry` 管理插件的发现和生命周期
3. **执行上下文**：`ExecutionContext` 提供插件执行的运行时上下文
4. **配置管理**：`PluginConfigDTO` 表示插件配置数据

### 5.2 内置插件迁移

| 插件 | 原实现 | 新实现 |
|------|-------|--------|
| 文件收集插件 | `FileCollectionStrategy` | `FileCollectionPlugin` |
| 元数据抓取插件 | `MetadataScraperStrategy` | `MetadataScraperPlugin` |
| 文件清理插件 | `FileCleanupStrategy` | `FileCleanupPlugin` |

### 5.3 插件开发指南

1. **实现 IPlugin 接口**：创建插件类并实现所有必要方法
2. **注册插件**：在 `META-INF/services` 中创建服务提供者配置文件
3. **配置插件**：通过 `PluginConfigDTO` 管理插件配置
4. **执行插件**：使用 `execute` 方法执行插件逻辑

## 6. 实施计划

### 6.1 阶段一：后端 API 实现

1. **源目录管理 API**：实现 SourceDirectoryController
2. **策略管理 API**：实现 StrategyController
3. **流水线管理 API**：实现 PipelineController
4. **线程池管理 API**：实现 ThreadPoolController
5. **配置管理 API**：实现 ConfigController
6. **日志管理 API**：实现 LogController
7. **插件管理 API**：实现 PluginController

### 6.2 阶段二：前端 Flutter 实现

1. **基础框架搭建**：创建 Flutter 项目结构
2. **API 客户端实现**：创建各种 API 服务
3. **状态管理实现**：创建 Riverpod Provider
4. **页面实现**：
   - 首页
   - 源目录管理页面
   - 策略流水线配置页面
   - 预览分析页面
   - 执行页面
   - 线程池配置页面
   - 配置管理页面
   - 日志页面
   - 界面设置页面

### 6.3 阶段三：插件系统迁移

1. **插件架构实现**：实现核心插件接口和注册表
2. **内置插件迁移**：迁移所有内置插件
3. **插件 API 实现**：实现插件管理 API

### 6.4 阶段四：测试和优化

1. **单元测试**：为所有模块编写单元测试
2. **集成测试**：测试模块间集成
3. **端到端测试**：测试完整业务流程
4. **性能优化**：优化应用性能
5. **用户体验优化**：优化界面和交互

## 7. 技术风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| API 兼容性问题 | 前端功能异常 | 低 | 严格的 API 版本控制和测试 |
| 插件系统迁移复杂性 | 插件功能缺失 | 中 | 逐个插件迁移，确保功能完整 |
| 性能问题 | 用户体验下降 | 中 | 实现分页加载和懒加载，优化 API 响应 |
| 浏览器兼容性 | 部分功能不可用 | 低 | 测试主流浏览器，使用兼容的 Flutter 特性 |
| 网络延迟 | 操作响应缓慢 | 中 | 实现异步操作和进度显示，优化网络请求 |

## 8. 资源需求

### 8.1 人员需求

| 角色 | 职责 | 数量 |
|------|------|------|
| 后端开发 | 实现后端 API 和插件系统 | 1-2 |
| 前端开发 | 实现 Flutter Web 界面 | 1-2 |
| 测试工程师 | 测试系统功能和性能 | 1 |

### 8.2 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 后端 | Spring Boot | 3.2+ |
| 后端 | Java | 21+ |
| 前端 | Flutter | 3.16+ |
| 前端 | Dart | 3.2+ |
| 构建工具 | Maven | 3.9+ |
| 构建工具 | Flutter CLI | 最新 |
| 部署 | Docker | 最新 |

## 9. 时间估计

| 阶段 | 时间估计 | 开始日期 | 结束日期 |
|------|----------|----------|----------|
| 阶段一：后端 API 实现 | 2-3 周 | TBD | TBD |
| 阶段二：前端 Flutter 实现 | 3-4 周 | TBD | TBD |
| 阶段三：插件系统迁移 | 1-2 周 | TBD | TBD |
| 阶段四：测试和优化 | 1-2 周 | TBD | TBD |
| 总计 | 7-11 周 | TBD | TBD |

## 10. 结论

通过本迁移计划的实施，FileManager Plus 将从 JavaFX 架构成功迁移到 Flutter Web + Spring Boot 架构，保持所有核心功能的同时，获得更好的跨平台支持、现代化的 UI 体验和更灵活的插件系统。

本计划详细规划了迁移的各个方面，包括功能映射、API 设计、前端实现、插件系统迁移和实施步骤，为开发团队提供了清晰的指导。