# 前端代码深度功能分析与重构方案

## 概述

本文档从功能维度深入分析了前端代码中所有未使用的文件，分析每个文件的功能价值、与现有代码的关系，并制定了基于功能分析的清理和重构方案。

**分析日期**: 2026-02-26  
**分析方法**: 功能对比、代码依赖分析、架构演进分析

---

## 核心发现

### 架构演进过程

通过代码分析，可以清晰地看到前端架构的演进过程：

```
第一阶段：独立页面模式
├── HomePage (主页)
├── TaskMonitorPage (任务监控)
├── PluginListPage (插件列表)
├── PluginConfigPage (插件配置)
├── PipelineConfigPage (流水线配置)
├── SourceDirectoriesPage (源目录配置)
├── FileBrowserPage (文件浏览器)
└── ConfigPage (配置页面)

第二阶段：Tab集成模式（当前）
├── MainLayout (主布局)
│   ├── ComposePage (任务编排) ← 集成了源目录、流水线、插件配置
│   ├── PreviewPage (预览执行) ← 集成了任务监控
│   ├── LogPage (运行日志)
│   ├── GlobalSettingsPage (全局设置)
│   └── AppearancePage (界面设置)
```

**关键变化**：
1. **从独立页面到Tab集成**：将多个独立页面整合到一个Tab界面中
2. **从页面到组件**：将页面功能拆分为可复用的组件
3. **从分散到集中**：将分散的配置功能集中到ComposePage

---

## 详细功能分析

### 一、任务监控相关文件

#### 1. task_monitor_page.dart

**文件路径**: `/pages/task_monitor_page.dart`

**功能分析**：
- ✅ 完整的任务监控页面
- ✅ 使用TaskListItem组件显示任务列表
- ✅ **包含删除全部任务功能**（这是我之前添加的）
- ✅ WebSocket实时更新任务状态
- ✅ 支持执行、取消任务操作
- ✅ 使用TaskDetailDialog显示详情

**与现有代码的关系**：

| 功能点 | task_monitor_page.dart | PreviewPage + TaskListWidget | 关系 |
|--------|----------------------|----------------------------|------|
| 任务列表显示 | TaskListItem | 内置_buildTaskCard | 功能重复 |
| 删除全部任务 | ✅ 有 | ✅ 已添加 | 功能已迁移 |
| 任务详情 | TaskDetailDialog | TaskDetailWidget | 功能重复 |
| WebSocket实时更新 | ✅ 有 | ❌ 无 | **功能缺失** |
| 执行任务 | ✅ 有 | ✅ 有 | 功能重复 |
| 取消任务 | ✅ 有 | ✅ 有 | 功能重复 |

**关键发现**：
1. **删除全部任务功能已迁移**：我已经将此功能添加到TaskListWidget中
2. **WebSocket实时更新功能缺失**：PreviewPage + TaskListWidget没有WebSocket实时更新
3. **API调用方式不同**：task_monitor_page.dart使用正确的getTaskList()，task_monitor.dart使用错误的getTasks()

**处理建议**：
- ✅ **可以删除**：删除全部任务功能已迁移到TaskListWidget
- ⚠️ **需要补充**：PreviewPage缺少WebSocket实时更新功能
- 📝 **建议**：将WebSocket实时更新功能添加到PreviewPage

---

#### 2. task_monitor.dart

**文件路径**: `/pages/task_monitor.dart`

**功能分析**：
- ✅ 完整的任务监控页面
- ✅ 使用TaskListItem组件显示任务列表
- ✅ WebSocket实时更新任务状态
- ✅ 支持执行、取消任务操作
- ❌ 使用ChangeDetailsDialog显示详情
- ❌ **调用错误的API方法**：`getTasks()` 应该是 `getTaskList()`

**与现有代码的关系**：

| 功能点 | task_monitor.dart | task_monitor_page.dart | 关系 |
|--------|----------------|----------------------|------|
| 任务列表显示 | TaskListItem | TaskListItem | 完全相同 |
| 删除全部任务 | ❌ 无 | ✅ 有 | 功能缺失 |
| 任务详情 | ChangeDetailsDialog | TaskDetailDialog | 组件不同 |
| WebSocket实时更新 | ✅ 有 | ✅ 有 | 完全相同 |
| 执行任务 | ✅ 有 | ✅ 有 | 完全相同 |
| 取消任务 | ✅ 有 | ✅ 有 | 完全相同 |
| API调用 | ❌ 错误 | ✅ 正确 | **有Bug** |

**关键发现**：
1. **API调用错误**：调用了不存在的getTasks()方法
2. **缺少删除全部任务功能**
3. **与task_monitor_page.dart功能高度重复**

**处理建议**：
- ❌ **可以直接删除**：存在API错误，且功能被task_monitor_page.dart完全覆盖

---

### 二、插件配置相关文件

#### 3. plugin_config_page.dart

**文件路径**: `/pages/plugin_config_page.dart`

**功能分析**：
- ✅ 完整的插件配置页面
- ✅ 支持参数可见性控制（_isParameterVisible）
- ✅ 支持参数互斥逻辑（_handleParameterChange中的exclusiveParams）
- ✅ 支持自动检测参数（_handleAutoDetect）
- ✅ 支持前置条件配置
- ✅ 使用PluginParameterFields组件
- ✅ 使用PreconditionBuilder组件

**与现有代码的关系**：

| 功能点 | plugin_config_page.dart | ComposePage | 关系 |
|--------|----------------------|-------------|------|
| 插件参数配置 | ✅ 完整 | ✅ 完整 | 功能重复 |
| 参数可见性控制 | ✅ 有 | ❓ 需确认 | 可能缺失 |
| 参数互斥逻辑 | ✅ 有 | ❓ 需确认 | 可能缺失 |
| 自动检测参数 | ✅ 有 | ❓ 需确认 | 可能缺失 |
| 前置条件配置 | ✅ 有 | ✅ 有 | 功能重复 |
| 使用组件 | PluginParameterFields | PluginParameterFields | 相同 |

**关键发现**：
1. **功能完整性**：plugin_config_page.dart的插件配置功能非常完整
2. **高级功能**：包含参数可见性、互斥逻辑、自动检测等高级功能
3. **功能重复**：ComposePage中的ComposePipelinePanel也提供了插件配置功能
4. **需要确认**：ComposePage是否包含了所有高级功能

**处理建议**：
- ⚠️ **需要确认**：检查ComposePage中的插件配置是否包含所有高级功能
- 📝 **如果功能完整**：可以删除plugin_config_page.dart
- 📝 **如果功能不完整**：需要将高级功能迁移到ComposePage

---

#### 4. plugin_config.dart

**文件路径**: `/pages/plugin_config.dart`

**功能分析**：
- ✅ 完整的插件配置页面
- ✅ 功能与plugin_config_page.dart几乎相同
- ✅ 使用相同的组件（PluginParameterFields、PreconditionBuilder）

**与现有代码的关系**：

| 功能点 | plugin_config.dart | plugin_config_page.dart | 关系 |
|--------|------------------|----------------------|------|
| 插件参数配置 | ✅ 完整 | ✅ 完整 | 完全相同 |
| 参数可见性控制 | ✅ 有 | ✅ 有 | 完全相同 |
| 参数互斥逻辑 | ✅ 有 | ✅ 有 | 完全相同 |
| 自动检测参数 | ✅ 有 | ✅ 有 | 完全相同 |
| 前置条件配置 | ✅ 有 | ✅ 有 | 完全相同 |
| 使用组件 | PluginParameterFields | PluginParameterFields | 相同 |

**关键发现**：
1. **完全重复**：plugin_config.dart与plugin_config_page.dart功能完全相同
2. **可能是旧版本**：plugin_config.dart可能是plugin_config_page.dart的旧版本
3. **文件名不规范**：plugin_config.dart缺少_page后缀

**处理建议**：
- ❌ **可以直接删除**：与plugin_config_page.dart完全重复

---

#### 5. plugin_list.dart

**文件路径**: `/pages/plugin_list.dart`

**功能分析**：
- ✅ 插件列表页面
- ✅ 显示所有可用插件
- ✅ 支持跳转到插件配置页面（PluginConfigPage）

**与现有代码的关系**：

| 功能点 | plugin_list.dart | ComposePage | 关系 |
|--------|----------------|-------------|------|
| 插件列表显示 | ✅ 有 | ✅ 有 | 功能重复 |
| 插件选择 | ✅ 有 | ✅ 有 | 功能重复 |
| 跳转到配置 | PluginConfigPage | ComposePipelinePanel | 方式不同 |

**关键发现**：
1. **功能重复**：ComposePage中的ComposePipelinePanel已经提供了插件列表和选择功能
2. **集成方式不同**：plugin_list.dart是独立页面，ComposePage是集成在Tab中
3. **是否需要独立页面**：取决于是否需要单独管理所有插件

**处理建议**：
- ⚠️ **需要确认**：是否需要独立的插件列表页面
- 📝 **如果不需要**：可以删除plugin_list.dart
- 📝 **如果需要**：保留作为插件管理入口

---

### 三、配置相关文件

#### 6. pipeline_config.dart

**文件路径**: `/pages/pipeline_config.dart`

**功能分析**：
- ✅ 流水线配置页面
- ✅ 显示流水线中的所有策略
- ✅ 支持添加、删除、移动策略
- ✅ 支持加载可用插件和策略
- ✅ 使用StrategyConfigCard组件

**与现有代码的关系**：

| 功能点 | pipeline_config.dart | ComposePage | 关系 |
|--------|-------------------|-------------|------|
| 流水线配置 | ✅ 完整 | ✅ 完整 | 功能重复 |
| 添加策略 | ✅ 有 | ✅ 有 | 功能重复 |
| 删除策略 | ✅ 有 | ✅ 有 | 功能重复 |
| 移动策略 | ✅ 有 | ✅ 有 | 功能重复 |
| 使用组件 | StrategyConfigCard | PipelineListItem | 组件不同 |

**关键发现**：
1. **功能重复**：ComposePage中的ComposePipelinePanel提供了完整的流水线配置功能
2. **组件不同**：使用不同的组件（StrategyConfigCard vs PipelineListItem）
3. **可能是早期版本**：pipeline_config.dart可能是ComposePipelinePanel的早期版本

**处理建议**：
- ❌ **可以直接删除**：功能被ComposePage完全覆盖

---

#### 7. source_directories.dart

**文件路径**: `/pages/source_directories.dart`

**功能分析**：
- ✅ 源目录配置页面
- ✅ 显示所有源目录
- ✅ 支持添加、删除源目录
- ✅ 支持配置线程数
- ✅ 使用DirectoryListItem组件

**与现有代码的关系**：

| 功能点 | source_directories.dart | ComposePage | 关系 |
|--------|----------------------|-------------|------|
| 源目录配置 | ✅ 完整 | ✅ 完整 | 功能重复 |
| 添加目录 | ✅ 有 | ✅ 有 | 功能重复 |
| 删除目录 | ✅ 有 | ✅ 有 | 功能重复 |
| 线程数配置 | ✅ 有 | ❓ 需确认 | 可能缺失 |
| 使用组件 | DirectoryListItem | DirectoryListItem | 相同 |

**关键发现**：
1. **功能重复**：ComposePage中的ComposeDirectoryPanel提供了完整的源目录配置功能
2. **线程数配置**：需要确认ComposePage是否包含线程数配置

**处理建议**：
- ⚠️ **需要确认**：检查ComposePage是否包含线程数配置
- 📝 **如果包含**：可以删除source_directories.dart
- 📝 **如果不包含**：需要将线程数配置迁移到ComposePage

---

#### 8. config_page.dart

**文件路径**: `/pages/config_page.dart`

**功能分析**：
- ⚠️ 需要进一步分析具体功能

**与现有代码的关系**：

| 功能点 | config_page.dart | GlobalSettingsPage | 关系 |
|--------|----------------|-------------------|------|
| 全局配置 | ⚠️ 待确认 | ✅ 完整 | 可能重复 |

**处理建议**：
- ⚠️ **需要分析**：需要进一步分析config_page.dart的具体功能
- 📝 **如果功能重复**：可以删除

---

### 四、其他页面文件

#### 9. home_page.dart

**文件路径**: `/pages/home_page.dart`

**功能分析**：
- ✅ 应用主页
- ✅ 提供服务提供者（Provider）
- ✅ 显示功能入口

**与现有代码的关系**：

| 功能点 | home_page.dart | MainLayout | 关系 |
|--------|--------------|------------|------|
| 应用主页 | ✅ 有 | ✅ 有 | 功能重复 |
| 服务提供者 | ✅ 有 | ✅ 有 | 功能重复 |
| 功能入口 | ✅ 有 | ✅ 有 | 功能重复 |

**关键发现**：
1. **功能完全重复**：MainLayout已经提供了完整的应用主页功能
2. **服务提供者重复**：两个文件都定义了相同的服务提供者
3. **可能是早期版本**：home_page.dart可能是MainLayout的早期版本

**处理建议**：
- ❌ **可以直接删除**：功能被MainLayout完全覆盖

---

#### 10. file_browser.dart

**文件路径**: `/pages/file_browser.dart`

**功能分析**：
- ✅ 文件浏览器页面
- ✅ 显示指定目录下的文件
- ✅ 支持导航到子目录

**与现有代码的关系**：

| 功能点 | file_browser.dart | ComposePage | 关系 |
|--------|----------------|-------------|------|
| 文件浏览 | ✅ 有 | ✅ 有 | 功能重复 |
| 目录选择 | ✅ 有 | ✅ 有 | 功能重复 |

**关键发现**：
1. **功能重复**：ComposePage中的ComposeDirectoryPanel已经提供了文件选择功能
2. **是否需要独立浏览器**：取决于是否需要单独浏览文件

**处理建议**：
- ⚠️ **需要确认**：是否需要独立的文件浏览器页面
- 📝 **如果不需要**：可以删除file_browser.dart
- 📝 **如果需要**：保留作为文件浏览入口

---

### 五、未使用的组件文件

#### 11. task_card.dart

**文件路径**: `/widgets/task_card.dart`

**功能分析**：
- ✅ 任务卡片组件
- ✅ 显示任务状态图标
- ✅ 显示任务进度条
- ✅ 显示任务信息
- ✅ 提供操作按钮（重新运行、终止、删除）

**与现有代码的关系**：

| 功能点 | task_card.dart | TaskListWidget._buildTaskCard | 关系 |
|--------|--------------|-------------------------------|------|
| 任务卡片显示 | ✅ 完整 | ✅ 完整 | 功能重复 |
| 状态图标 | ✅ 有 | ❌ 无 | 功能差异 |
| 进度条 | ✅ 有 | ❌ 无 | 功能差异 |
| 操作按钮 | ✅ 有 | ✅ 有 | 功能重复 |

**关键发现**：
1. **功能重复**：TaskListWidget中的_buildTaskCard提供了类似的任务卡片功能
2. **UI差异**：task_card.dart有状态图标和进度条，TaskListWidget的卡片更简洁
3. **可能是早期版本**：task_card.dart可能是早期版本的任务卡片组件

**处理建议**：
- ⚠️ **需要确认**：是否需要状态图标和进度条
- 📝 **如果不需要**：可以删除task_card.dart
- 📝 **如果需要**：将状态图标和进度条功能迁移到TaskListWidget

---

#### 12. task_list_item.dart

**文件路径**: `/widgets/task_list_item.dart`

**功能分析**：
- ✅ 任务列表项组件
- ✅ 显示任务状态
- ✅ 显示任务进度
- ✅ 显示任务统计信息
- ✅ 提供操作按钮

**与现有代码的关系**：

| 功能点 | task_list_item.dart | TaskListWidget._buildTaskCard | 关系 |
|--------|------------------|-------------------------------|------|
| 任务列表项 | ✅ 完整 | ✅ 完整 | 功能重复 |
| 状态显示 | ✅ 有 | ✅ 有 | 功能重复 |
| 进度显示 | ✅ 有 | ❌ 无 | 功能差异 |
| 统计信息 | ✅ 有 | ❌ 无 | 功能差异 |
| 操作按钮 | ✅ 有 | ✅ 有 | 功能重复 |

**关键发现**：
1. **功能重复**：TaskListWidget中的_buildTaskCard提供了类似的功能
2. **UI差异**：task_list_item.dart有进度条和统计信息，TaskListWidget的卡片更简洁
3. **可能是早期版本**：task_list_item.dart可能是早期版本的任务列表项组件

**处理建议**：
- ⚠️ **需要确认**：是否需要进度条和统计信息
- 📝 **如果不需要**：可以删除task_list_item.dart
- 📝 **如果需要**：将进度条和统计信息功能迁移到TaskListWidget

---

#### 13. change_details_dialog.dart

**文件路径**: `/widgets/change_details_dialog.dart`

**功能分析**：
- ✅ 变更详情对话框
- ✅ 显示变更记录的详细信息

**与现有代码的关系**：

| 功能点 | change_details_dialog.dart | TaskDetailWidget | 关系 |
|--------|------------------------|-----------------|------|
| 变更详情显示 | ✅ 有 | ✅ 有 | 功能重复 |

**关键发现**：
1. **功能重复**：TaskDetailWidget已经提供了任务详情显示功能
2. **可能是早期版本**：change_details_dialog.dart可能是早期版本的对话框

**处理建议**：
- ❌ **可以直接删除**：功能被TaskDetailWidget完全覆盖

---

#### 14. task_detail_dialog.dart

**文件路径**: `/widgets/task_detail_dialog.dart`

**功能分析**：
- ✅ 任务详情对话框
- ✅ 显示任务的详细信息

**与现有代码的关系**：

| 功能点 | task_detail_dialog.dart | TaskDetailWidget | 关系 |
|--------|----------------------|-----------------|------|
| 任务详情显示 | ✅ 有 | ✅ 有 | 功能重复 |

**关键发现**：
1. **功能重复**：TaskDetailWidget已经提供了任务详情显示功能
2. **可能是早期版本**：task_detail_dialog.dart可能是早期版本的对话框

**处理建议**：
- ❌ **可以直接删除**：功能被TaskDetailWidget完全覆盖

---

## 清理和重构方案

### 阶段1：确认功能完整性

**目标**：确认ComposePage和PreviewPage是否包含了所有必要的功能

**需要确认的问题**：
1. ComposePage的插件配置是否包含参数可见性、互斥逻辑、自动检测等高级功能？
2. ComposePage的源目录配置是否包含线程数配置？
3. PreviewPage是否需要WebSocket实时更新功能？
4. TaskListWidget是否需要状态图标、进度条、统计信息等UI元素？

**确认方法**：
- 阅读ComposePage和PreviewPage的代码
- 对比未使用文件的功能
- 列出缺失的功能点

---

### 阶段2：功能迁移

**目标**：将未使用文件中的有价值功能迁移到现有代码中

#### 2.1 迁移WebSocket实时更新功能

**源文件**：task_monitor_page.dart  
**目标文件**：PreviewPage

**需要迁移的功能**：
- WebSocket连接管理
- 实时任务状态更新
- 自动连接到运行中的任务

**迁移步骤**：
1. 在PreviewPage中添加WebSocket连接管理
2. 为运行中的任务建立WebSocket连接
3. 监听WebSocket消息并更新任务状态
4. 任务完成后自动关闭WebSocket连接

---

#### 2.2 迁移高级插件配置功能（如果需要）

**源文件**：plugin_config_page.dart  
**目标文件**：ComposePage

**需要迁移的功能**（如果缺失）：
- 参数可见性控制
- 参数互斥逻辑
- 自动检测参数

**迁移步骤**：
1. 检查ComposePage是否已包含这些功能
2. 如果缺失，从plugin_config_page.dart中提取相关代码
3. 集成到ComposePage的插件配置逻辑中

---

#### 2.3 迁移线程数配置（如果需要）

**源文件**：source_directories.dart  
**目标文件**：ComposePage

**需要迁移的功能**（如果缺失）：
- 线程数配置界面
- 线程数保存逻辑

**迁移步骤**：
1. 检查ComposePage是否已包含线程数配置
2. 如果缺失，从source_directories.dart中提取相关代码
3. 集成到ComposePage的源目录配置逻辑中

---

#### 2.4 迁移任务卡片UI元素（如果需要）

**源文件**：task_card.dart、task_list_item.dart  
**目标文件**：TaskListWidget

**需要迁移的功能**（如果需要）：
- 状态图标
- 进度条
- 统计信息

**迁移步骤**：
1. 检查TaskListWidget是否需要这些UI元素
2. 如果需要，从task_card.dart和task_list_item.dart中提取相关代码
3. 集成到TaskListWidget的_buildTaskCard方法中

---

### 阶段3：删除未使用文件

**目标**：删除所有功能已迁移或功能重复的文件

#### 3.1 可以直接删除的文件

| 文件 | 原因 | 风险 |
|------|------|------|
| task_monitor.dart | API错误，功能被task_monitor_page.dart覆盖 | 低 |
| plugin_config.dart | 与plugin_config_page.dart完全重复 | 低 |
| pipeline_config.dart | 功能被ComposePage完全覆盖 | 低 |
| home_page.dart | 功能被MainLayout完全覆盖 | 低 |
| change_details_dialog.dart | 功能被TaskDetailWidget完全覆盖 | 低 |
| task_detail_dialog.dart | 功能被TaskDetailWidget完全覆盖 | 低 |

#### 3.2 需要确认后删除的文件

| 文件 | 需要确认的问题 | 风险 |
|------|--------------|------|
| task_monitor_page.dart | 删除全部任务功能已迁移，WebSocket功能已迁移 | 低 |
| plugin_config_page.dart | 高级功能是否已迁移到ComposePage | 中 |
| plugin_list.dart | 是否需要独立的插件列表页面 | 中 |
| source_directories.dart | 线程数配置是否已迁移到ComposePage | 中 |
| config_page.dart | 具体功能是否被GlobalSettingsPage覆盖 | 中 |
| file_browser.dart | 是否需要独立的文件浏览器页面 | 中 |
| task_card.dart | 是否需要状态图标和进度条 | 中 |
| task_list_item.dart | 是否需要进度条和统计信息 | 中 |

---

### 阶段4：验证功能完整性

**目标**：确保删除文件后所有功能正常

**验证步骤**：
1. 重新构建前端项目
2. 测试所有核心功能
3. 确认没有遗漏的功能

---

## 执行计划

### 第一步：功能确认（预计时间：30分钟）

- [ ] 检查ComposePage的插件配置功能
- [ ] 检查ComposePage的线程数配置功能
- [ ] 检查PreviewPage的WebSocket实时更新需求
- [ ] 检查TaskListWidget的UI元素需求

### 第二步：功能迁移（预计时间：60分钟）

- [ ] 迁移WebSocket实时更新功能到PreviewPage
- [ ] 迁移高级插件配置功能（如果需要）
- [ ] 迁移线程数配置功能（如果需要）
- [ ] 迁移任务卡片UI元素（如果需要）

### 第三步：删除文件（预计时间：10分钟）

- [ ] 删除可以直接删除的文件（6个）
- [ ] 删除需要确认后删除的文件（8个）

### 第四步：验证测试（预计时间：30分钟）

- [ ] 重新构建前端项目
- [ ] 测试所有核心功能
- [ ] 确认没有遗漏的功能

---

## 风险评估

### 高风险项

1. **删除plugin_config_page.dart**
   - **风险**：可能丢失高级插件配置功能
   - **缓解措施**：先确认ComposePage是否包含所有高级功能

2. **删除source_directories.dart**
   - **风险**：可能丢失线程数配置功能
   - **缓解措施**：先确认ComposePage是否包含线程数配置

### 中风险项

1. **删除task_monitor_page.dart**
   - **风险**：可能丢失WebSocket实时更新功能
   - **缓解措施**：先迁移WebSocket功能到PreviewPage

2. **删除task_card.dart和task_list_item.dart**
   - **风险**：可能丢失重要的UI元素
   - **缓解措施**：先确认是否需要这些UI元素

### 低风险项

1. **删除task_monitor.dart、plugin_config.dart等重复文件**
   - **风险**：极低
   - **缓解措施**：这些文件功能完全重复或存在错误

---

## 总结

### 关键发现

1. **架构演进清晰**：从独立页面模式演进到Tab集成模式
2. **功能迁移不完整**：部分高级功能可能未完全迁移
3. **删除全部任务功能已迁移**：已成功添加到TaskListWidget
4. **WebSocket实时更新功能缺失**：PreviewPage需要添加此功能

### 建议的清理策略

1. **先确认后删除**：对于可能包含有价值功能的文件，先确认功能是否已迁移
2. **分阶段执行**：先迁移功能，再删除文件，最后验证
3. **保留备份**：删除前创建Git分支，以便回滚

### 预期效果

1. **代码库更简洁**：删除14个未使用的文件
2. **功能更完整**：迁移缺失的功能（如WebSocket实时更新）
3. **架构更清晰**：统一使用Tab集成模式
4. **维护成本降低**：减少重复代码和混淆

---

**文档版本**: 1.0  
**最后更新**: 2026-02-26  
**维护者**: FileManager Plus Team
