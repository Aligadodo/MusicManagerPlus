# Flutter Web 客户端功能扩展设计文档

## 1. 概述

本文档旨在详细设计 FileManager Plus Flutter Web 客户端的功能扩展，使其与原 JavaFX 应用的功能保持一致，同时充分利用新架构的优势。

## 2. 功能差距分析

### 2.1 原 JavaFX 应用核心功能

| 功能模块 | 详细功能 | 状态 |
|---------|---------|------|
| 源目录管理 | 添加、移除、清空源目录 | 缺失 |
| 策略流水线 | 策略组合、排序、配置 | 部分实现 |
| 预览分析 | 分析文件变更、显示变更预览 | 缺失 |
| 执行变更 | 实际执行文件变更 | 缺失 |
| 线程池管理 | 调整预览和执行线程数 | 缺失 |
| 配置管理 | 保存、加载、重置配置 | 缺失 |
| 日志管理 | 显示系统运行日志 | 部分实现 |
| 界面设置 | 主题、外观配置 | 缺失 |

### 2.2 当前 Flutter Web 客户端功能

| 功能模块 | 详细功能 | 状态 |
|---------|---------|------|
| 文件浏览器 | 浏览文件系统 | 基本实现 |
| 策略配置 | 查看和配置策略 | 基本实现 |
| 任务监控 | 查看任务状态 | 基本实现 |
| 系统设置 | 系统参数配置 | 缺失 |

## 3. 架构设计

### 3.1 前端架构

```
Flutter Web 客户端
├── lib/
│   ├── api/            # API 客户端
│   ├── models/         # 数据模型
│   ├── pages/          # 页面组件
│   │   ├── home_page.dart          # 首页
│   │   ├── file_browser.dart       # 文件浏览器
│   │   ├── strategy_config.dart    # 策略配置
│   │   ├── task_monitor.dart       # 任务监控
│   │   ├── pipeline_config.dart    # 策略流水线配置
│   │   ├── preview_page.dart       # 预览执行
│   │   ├── log_page.dart           # 日志页面
│   │   ├── settings_page.dart      # 系统设置
│   │   └── appearance_page.dart    # 界面设置
│   ├── providers/      # 状态管理
│   ├── widgets/        # 通用组件
│   └── main.dart       # 应用入口
├── test/               # 测试用例
└── pubspec.yaml        # 依赖配置
```

### 3.2 后端架构扩展

| 模块 | 新增/修改 | 说明 |
|------|-----------|------|
| PipelineController | 新增 | 管理策略流水线 |
| SourceDirectoryController | 新增 | 管理源目录 |
| ThreadPoolController | 新增 | 管理线程池 |
| ConfigController | 扩展 | 支持更多配置选项 |
| LogController | 扩展 | 支持更详细的日志 |

## 4. 功能模块设计

### 4.1 源目录管理

#### 4.1.1 前端实现

- **页面**：`source_directories_page.dart`
- **功能**：
  - 添加源目录
  - 移除源目录
  - 清空源目录
  - 显示源目录列表
  - 为每个源目录配置线程数

#### 4.1.2 后端实现

- **控制器**：`SourceDirectoryController.java`
- **API**：
  - `GET /api/source-directories` - 获取源目录列表
  - `POST /api/source-directories` - 添加源目录
  - `DELETE /api/source-directories/{id}` - 删除源目录
  - `DELETE /api/source-directories` - 清空源目录
  - `PUT /api/source-directories/{id}/threads` - 设置线程数

### 4.2 策略流水线

#### 4.2.1 前端实现

- **页面**：`pipeline_config.dart`
- **功能**：
  - 从可用策略中选择并添加到流水线
  - 调整流水线中策略的顺序
  - 配置每个策略的参数
  - 移除流水线中的策略

#### 4.2.2 后端实现

- **控制器**：`PipelineController.java`
- **API**：
  - `GET /api/pipeline` - 获取当前流水线
  - `POST /api/pipeline` - 更新流水线
  - `POST /api/pipeline/analyze` - 分析流水线
  - `POST /api/pipeline/execute` - 执行流水线

### 4.3 预览分析

#### 4.3.1 前端实现

- **页面**：`preview_page.dart`
- **功能**：
  - 显示文件变更预览表格
  - 显示变更统计信息
  - 支持按状态筛选变更
  - 支持查看变更详情

#### 4.3.2 后端实现

- **服务**：扩展 `StrategyServiceImpl.java`
- **API**：
  - `POST /api/strategies/{id}/analyze` - 分析文件变更

### 4.4 执行变更

#### 4.4.1 前端实现

- **功能**：
  - 执行文件变更
  - 显示执行进度
  - 支持停止执行
  - 显示执行结果

#### 4.4.2 后端实现

- **服务**：扩展 `TaskServiceImpl.java`
- **API**：
  - `POST /api/tasks/{id}/execute` - 执行任务
  - `POST /api/tasks/{id}/cancel` - 取消任务

### 4.5 线程池管理

#### 4.5.1 前端实现

- **功能**：
  - 调整全局预览线程数
  - 调整全局执行线程数
  - 为每个源目录配置线程数

#### 4.5.2 后端实现

- **控制器**：`ThreadPoolController.java`
- **API**：
  - `GET /api/thread-pool` - 获取线程池配置
  - `PUT /api/thread-pool/preview` - 设置预览线程数
  - `PUT /api/thread-pool/execution` - 设置执行线程数

### 4.6 配置管理

#### 4.6.1 前端实现

- **页面**：`config_page.dart`
- **功能**：
  - 保存配置到文件
  - 从文件加载配置
  - 重置配置为默认值

#### 4.6.2 后端实现

- **服务**：扩展 `ConfigServiceImpl.java`
- **API**：
  - `GET /api/config` - 获取配置
  - `POST /api/config` - 更新配置
  - `POST /api/config/save` - 保存配置
  - `POST /api/config/load` - 加载配置
  - `POST /api/config/reset` - 重置配置

### 4.7 日志管理

#### 4.7.1 前端实现

- **页面**：`log_page.dart`
- **功能**：
  - 显示系统运行日志
  - 支持按级别筛选日志
  - 支持清空日志

#### 4.7.2 后端实现

- **服务**：扩展 `LogServiceImpl.java`
- **API**：
  - `GET /api/logs` - 获取日志
  - `DELETE /api/logs` - 清空日志

### 4.8 界面设置

#### 4.8.1 前端实现

- **页面**：`appearance_page.dart`
- **功能**：
  - 主题选择（亮色/暗色）
  - 颜色配置
  - 字体配置
  - 背景设置

#### 4.8.2 后端实现

- **API**：
  - `GET /api/appearance` - 获取外观配置
  - `POST /api/appearance` - 更新外观配置

## 5. 技术实现细节

### 5.1 状态管理

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

### 5.2 数据模型

扩展现有模型，添加以下新模型：

| 模型 | 功能 | 文件 |
|------|------|------|
| SourceDirectory | 源目录信息 | `models/source_directory.dart` |
| PipelineStrategy | 流水线策略 | `models/pipeline_strategy.dart` |
| ChangeRecord | 文件变更记录 | `models/change_record.dart` |
| ThreadPoolConfig | 线程池配置 | `models/thread_pool_config.dart` |
| AppConfig | 应用配置 | `models/app_config.dart` |
| LogEntry | 日志条目 | `models/log_entry.dart` |
| AppearanceConfig | 外观配置 | `models/appearance_config.dart` |

### 5.3 API 客户端扩展

扩展现有 API 客户端，添加以下新服务：

| 服务 | 功能 | 文件 |
|------|------|------|
| SourceDirectoryService | 源目录管理 API | `api/source_directory_service.dart` |
| PipelineService | 策略流水线 API | `api/pipeline_service.dart` |
| ThreadPoolService | 线程池管理 API | `api/thread_pool_service.dart` |
| ConfigService | 配置管理 API | `api/config_service.dart` |
| LogService | 日志管理 API | `api/log_service.dart` |
| AppearanceService | 界面设置 API | `api/appearance_service.dart` |

### 5.4 界面组件

创建以下通用组件：

| 组件 | 功能 | 文件 |
|------|------|------|
| SourceDirectoryList | 源目录列表 | `widgets/source_directory_list.dart` |
| PipelineStrategyList | 策略流水线列表 | `widgets/pipeline_strategy_list.dart` |
| ChangeRecordTable | 变更记录表格 | `widgets/change_record_table.dart` |
| ThreadPoolSettings | 线程池设置 | `widgets/thread_pool_settings.dart` |
| LogView | 日志视图 | `widgets/log_view.dart` |
| AppearanceSettings | 外观设置 | `widgets/appearance_settings.dart` |

## 6. 后端 API 设计

### 6.1 源目录管理 API

| 方法 | 路径 | 功能 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/source-directories` | 获取源目录列表 | N/A | `[{"path": "...", "threadCount": 4}]` |
| POST | `/api/source-directories` | 添加源目录 | `{"path": "...", "threadCount": 4}` | `{"success": true}` |
| DELETE | `/api/source-directories/{id}` | 删除源目录 | N/A | `{"success": true}` |
| DELETE | `/api/source-directories` | 清空源目录 | N/A | `{"success": true}` |
| PUT | `/api/source-directories/{id}/threads` | 设置线程数 | `{"threadCount": 4}` | `{"success": true}` |

### 6.2 策略流水线 API

| 方法 | 路径 | 功能 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/pipeline` | 获取流水线 | N/A | `[{"strategyId": "...", "config": {...}}]` |
| POST | `/api/pipeline` | 更新流水线 | `[{"strategyId": "...", "config": {...}}]` | `{"success": true}` |
| POST | `/api/pipeline/analyze` | 分析流水线 | `{"sourceDirectories": [...], "pipeline": [...]}` | `[{"id": "...", "originalName": "...", "newName": "..."}]` |
| POST | `/api/pipeline/execute` | 执行流水线 | `{"sourceDirectories": [...], "pipeline": [...]}` | `{"taskId": "..."}` |

### 6.3 线程池管理 API

| 方法 | 路径 | 功能 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/thread-pool` | 获取线程池配置 | N/A | `{"previewThreads": 4, "executionThreads": 8}` |
| PUT | `/api/thread-pool/preview` | 设置预览线程数 | `{"threads": 4}` | `{"success": true}` |
| PUT | `/api/thread-pool/execution` | 设置执行线程数 | `{"threads": 8}` | `{"success": true}` |

### 6.4 配置管理 API

| 方法 | 路径 | 功能 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/config` | 获取配置 | N/A | `{"key": "value", ...}` |
| POST | `/api/config` | 更新配置 | `{"key": "value", ...}` | `{"success": true}` |
| POST | `/api/config/save` | 保存配置 | `{"fileName": "config.json"}` | `{"success": true}` |
| POST | `/api/config/load` | 加载配置 | `{"fileName": "config.json"}` | `{"success": true, "config": {...}}` |
| POST | `/api/config/reset` | 重置配置 | N/A | `{"success": true}` |

### 6.5 日志管理 API

| 方法 | 路径 | 功能 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/logs` | 获取日志 | N/A | `[{"timestamp": 1234567890, "level": "INFO", "message": "..."}]` |
| DELETE | `/api/logs` | 清空日志 | N/A | `{"success": true}` |

### 6.6 界面设置 API

| 方法 | 路径 | 功能 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/appearance` | 获取外观配置 | N/A | `{"theme": "light", "primaryColor": "#2196F3"}` |
| POST | `/api/appearance` | 更新外观配置 | `{"theme": "light", "primaryColor": "#2196F3"}` | `{"success": true}` |

## 7. 测试策略

### 7.1 单元测试

| 模块 | 测试文件 | 测试内容 |
|------|----------|----------|
| API 客户端 | `test/api/` | API 调用测试 |
| 数据模型 | `test/models/` | 模型序列化/反序列化测试 |
| 状态管理 | `test/providers/` | Provider 状态管理测试 |
| 业务逻辑 | `test/services/` | 业务逻辑测试 |

### 7.2 集成测试

| 测试文件 | 测试内容 |
|----------|----------|
| `test/integration/app_test.dart` | 应用整体功能测试 |
| `test/integration/api_integration_test.dart` | API 集成测试 |

### 7.3 端到端测试

| 测试文件 | 测试内容 |
|----------|----------|
| `test/e2e/pipeline_test.dart` | 策略流水线端到端测试 |
| `test/e2e/file_operation_test.dart` | 文件操作端到端测试 |

## 8. 实施计划

### 8.1 阶段一：核心功能实现

1. **源目录管理**：实现源目录的添加、移除、清空功能
2. **策略流水线**：完善策略组合、排序、配置功能
3. **预览分析**：实现文件变更分析和预览功能
4. **执行变更**：实现文件变更执行功能

### 8.2 阶段二：高级功能实现

1. **线程池管理**：实现线程池配置功能
2. **配置管理**：实现配置的保存、加载、重置功能
3. **日志管理**：完善日志显示功能
4. **界面设置**：实现主题、外观配置功能

### 8.3 阶段三：测试和优化

1. **单元测试**：为每个模块添加单元测试
2. **集成测试**：测试模块间集成
3. **端到端测试**：测试完整业务流程
4. **性能优化**：优化应用性能

## 9. 技术风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| API 兼容性 | 前端功能异常 | 低 | 严格的 API 版本控制 |
| 性能问题 | 用户体验下降 | 中 | 实现分页加载和懒加载 |
| 并发操作 | 数据不一致 | 低 | 实现适当的并发控制 |
| 浏览器兼容性 | 部分功能不可用 | 低 | 测试主流浏览器 |
| 网络延迟 | 操作响应缓慢 | 中 | 实现异步操作和进度显示 |

## 10. 结论

通过本设计文档的实施，FileManager Plus Flutter Web 客户端将能够提供与原 JavaFX 应用相同的核心功能，同时充分利用新架构的优势，如跨平台支持、现代化 UI、更好的可扩展性等。

本设计文档详细规划了功能扩展的各个方面，包括架构设计、功能模块设计、API 设计、测试策略等，为开发团队提供了清晰的实施指南。
