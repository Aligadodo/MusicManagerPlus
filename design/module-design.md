# 模块设计文档

## 概述

本文档描述FileManager Plus项目的核心模块设计，包括各模块的职责、接口定义、实现方式等。

## 模块架构

```
FileManager Plus
├── 核心领域模块 (shared-domain)
│   ├── 实体模块 (entity)
│   ├── 数据传输对象模块 (dto)
│   └── 服务接口模块 (service)
├── 插件系统模块 (plugin)
│   ├── 插件接口 (IPlugin)
│   ├── 插件注册表 (PluginRegistry)
│   ├── 插件上下文 (ExecutionContext)
│   ├── 抽象插件基类 (AbstractPlugin)
│   └── 可配置策略基类 (AbstractConfigurableStrategy)
├── 策略实现模块 (plugin/impl)
│   ├── 文件操作策略
│   ├── 音频处理策略
│   ├── 元数据处理策略
│   └── NCM处理策略
├── 后端服务模块 (backend)
│   ├── 控制器模块 (controller)
│   ├── 服务实现模块 (service)
│   ├── 工具类模块 (util)
│   └── 配置模块 (config)
└── 前端客户端模块 (clients/flutter-web-cli)
    ├── API客户端模块 (api)
    ├── 数据模型模块 (models)
    ├── 页面组件模块 (pages)
    └── 工具类模块 (utils)
```

## 核心领域模块

### 1. 实体模块 (entity)

#### 模块职责
定义系统的核心数据实体，包括文件变更记录、任务信息等。

#### 主要实体类

##### ChangeRecord
**职责**: 记录文件变更的详细信息

**核心字段**:
- `id`: 记录唯一标识符
- `originalName`: 原始文件名
- `newName`: 新文件名
- `fileHandle`: 原始文件句柄
- `changed`: 是否被修改
- `newPath`: 新文件路径
- `operationType`: 操作类型（支持枚举）
- `status`: 执行状态（支持枚举）
- `extraParams`: 额外参数
- `failReason`: 失败原因

**扩展字段**:
- `isCreate`: 是否创建新文件
- `isDeleteOrMove`: 是否删除或移动
- `selected`: 是否被选中
- `intermediateFile`: 中间状态文件
- `processInfo`: 处理过程信息列表
- `analyzeTime`: 分析阶段耗时（毫秒）
- `executeTime`: 执行阶段耗时（毫秒）
- `filePath`: 文件路径
- `reason`: 变更原因

**枚举支持**:
- `OperationType`: 操作类型枚举（13种）
- `ExecStatus`: 执行状态枚举（6种）

**类型安全方法**:
- `getOperationTypeEnum()` / `setOperationType(OperationType)`
- `getStatusEnum()` / `setStatus(ExecStatus)`

**便捷构造函数**:
- 6参数构造函数：`ChangeRecord(String, String, File, boolean, String, OperationType)`
- 8参数完整构造函数：`ChangeRecord(String, String, File, boolean, String, OperationType, Map<String, String>, ExecStatus)`

**链式处理支持**:
- `getCurrentSource()`: 获取当前应该处理的源文件
- `setIntermediateFile(File)`: 设置中间状态文件

**过程信息记录**:
- `addProcessInfo(String)`: 添加过程信息
- `addProcessInfo(String, String)`: 添加键值对格式的过程信息

### 2. 数据传输对象模块 (dto)

#### 模块职责
定义前后端数据传输对象，包括配置、参数、条件等。

#### 主要DTO类

##### PluginConfigDTO
**职责**: 插件配置数据传输对象

**核心字段**:
- `configValues`: 配置值映射
- `preconditionGroups`: 前置条件组列表

**核心方法**:
- `setValue(String, Object)`: 设置配置值
- `getValue(String)`: 获取配置值

##### PluginParameterDTO
**职责**: 插件参数数据传输对象

**核心字段**:
- `name`: 参数名称
- `label`: 参数标签
- `description`: 参数描述
- `type`: 参数类型
- `defaultValue`: 默认值
- `required`: 是否必填
- `options`: 选项列表
- `enumOptions`: 枚举选项列表

##### PreconditionGroupDTO
**职责**: 前置条件组数据传输对象

**核心字段**:
- `groupId`: 条件组ID
- `groupName`: 条件组名称
- `conditions`: 条件列表
- `logicOperator`: 逻辑操作符（AND/OR）

##### ConfigFieldDTO
**职责**: 配置字段数据传输对象

**核心字段**:
- `name`: 字段名称
- `label`: 字段标签
- `type`: 字段类型
- `defaultValue`: 默认值
- `description`: 字段描述
- `required`: 是否必填
- `options`: 选项列表
- `enumOptions`: 枚举选项列表
- `isModule`: 是否为模块
- `moduleType`: 模块类型
- `blockConditions`: 阻塞条件列表
- `autoFillConfig`: 自动填充配置

##### StrategyConfigDTO
**职责**: 策略配置数据传输对象

**核心字段**:
- `configValues`: 配置值映射
- `preconditionGroups`: 前置条件组列表

##### EnumOptionDTO
**职责**: 枚举选项数据传输对象

**核心字段**:
- `code`: 选项代码
- `label`: 选项标签（中文）
- `labelEn`: 选项标签（英文）

### 3. 服务接口模块 (service)

#### 模块职责
定义系统的核心服务接口，包括插件服务、任务服务等。

#### 主要服务接口

##### PluginService
**职责**: 插件管理服务接口

**核心方法**:
- `getAvailablePlugins()`: 获取所有可用插件
- `getPluginInfo(String)`: 获取指定插件信息
- `getPluginConfig(String)`: 获取插件配置
- `updatePluginConfig(String, PluginConfigDTO)`: 更新插件配置
- `previewPlugin(String, List<String>, PluginConfigDTO)`: 预览插件执行结果
- `previewPlugin(String, List<String>, PluginConfigDTO, List<PreconditionGroupDTO>)`: 预览插件执行结果（带前置条件）
- `analyzePlugin(String, ChangeRecord, List<ChangeRecord>, List<File>, PluginConfigDTO, List<PreconditionGroupDTO>)`: 分析单个文件（新接口）
- `executePlugin(String, List<String>, PluginConfigDTO)`: 执行插件
- `executePlugin(String, List<String>, PluginConfigDTO, List<PreconditionGroupDTO>)`: 执行插件（带前置条件）
- `reloadPlugins()`: 重新加载插件
- `getInternalPlugins()`: 获取内部插件
- `getExternalPlugins()`: 获取外部插件
- `scanExternalPlugins(String)`: 扫描外部插件
- `loadExternalPlugins(String)`: 加载外部插件
- `reloadExternalPlugins()`: 重新加载外部插件

##### TaskService
**职责**: 任务管理服务接口

**核心方法**:
- `createTask(String)`: 创建任务
- `getTask(String)`: 获取指定任务
- `getAllTasks()`: 获取所有任务
- `cancelTask(String)`: 取消任务
- `executeTask(String)`: 执行任务

##### PipelineTaskManager
**职责**: 流水线任务管理器

**核心方法**:
- `createTask(String)`: 创建任务
- `updateTaskStatus(String, TaskStatus)`: 更新任务状态
- `updateTaskMessage(String, String)`: 更新任务消息
- `updateTaskStep(String, String)`: 更新任务步骤
- `updateTaskProgress(String, int, int)`: 更新任务进度
- `updateTaskChanges(String, boolean, int)`: 更新任务变更信息
- `updateTaskScanningInfo(String, String, int, int)`: 更新任务扫描信息
- `updateTaskLogMessage(String, String)`: 更新任务日志消息
- `isTaskRunning()`: 检查任务是否运行
- `setCurrentTaskRunning(boolean)`: 设置当前任务运行状态

##### FileFilterService
**职责**: 文件过滤服务接口

**核心方法**:
- `isFileIncluded(File)`: 检查文件是否包含
- `isFileFiltered(File)`: 检查文件是否过滤
- `getScanFilterList()`: 获取扫描过滤列表
- `addScanFilter(String)`: 添加扫描过滤
- `removeScanFilter(String)`: 移除扫描过滤
- `clearScanFilters()`: 清除扫描过滤

##### PreviewLimitService
**职责**: 预览和执行数量限制服务接口

**核心方法**:
- `getGlobalPreviewLimit()`: 获取全局预览限制
- `setGlobalPreviewLimit(int)`: 设置全局预览限制
- `isGlobalPreviewUnlimited()`: 检查全局预览是否无限制
- `setGlobalPreviewUnlimited(boolean)`: 设置全局预览无限制
- `getGlobalExecutionLimit()`: 获取全局执行限制
- `setGlobalExecutionLimit(int)`: 设置全局执行限制
- `isGlobalExecutionUnlimited()`: 检查全局执行是否无限制
- `setGlobalExecutionUnlimited(boolean)`: 设置全局执行无限制
- `getRootPathPreviewLimit(String)`: 获取根路径预览限制
- `setRootPathPreviewLimit(String, int)`: 设置根路径预览限制
- `isRootPathPreviewUnlimited(String)`: 检查根路径预览是否无限制
- `setRootPathPreviewUnlimited(String, boolean)`: 设置根路径预览无限制
- `getRootPathExecutionLimit(String)`: 获取根路径执行限制
- `setRootPathExecutionLimit(String, int)`: 设置根路径执行限制
- `isRootPathExecutionUnlimited(String)`: 检查根路径执行是否无限制
- `setRootPathExecutionUnlimited(String, boolean)`: 设置根路径执行无限制
- `getAllRootPathPreviewLimits()`: 获取所有根路径预览限制
- `getAllRootPathExecutionLimits()`: 获取所有根路径执行限制
- `clearAllRootPathLimits()`: 清除所有根路径限制

## 插件系统模块

### 1. 插件接口 (IPlugin)

#### 模块职责
定义插件的基本功能接口，所有插件必须实现此接口。

#### 核心方法

##### 基础方法
- `String getId()`: 获取插件ID
- `String getName()`: 获取插件名称
- `String getDescription()`: 获取插件描述
- `String getVersion()`: 获取插件版本
- `PluginConfigDTO getDefaultConfig()`: 获取默认配置
- `List<PluginParameterDTO> getParameters()`: 获取参数列表
- `List<PreconditionGroupDTO> getDefaultPreconditionGroups()`: 获取默认前置条件组

##### 批量处理方法（兼容旧接口）
- `List<ChangeRecord> execute(List<String>, PluginConfigDTO, ExecutionContext)`: 批量执行文件处理
- `List<ChangeRecord> preview(List<String>, PluginConfigDTO, ExecutionContext)`: 批量预览文件处理

##### 逐个文件处理方法（新接口，推荐使用）
- `List<ChangeRecord> analyze(ChangeRecord, List<ChangeRecord>, List<File>, PluginConfigDTO, ExecutionContext)`: 分析单个文件
- `void execute(ChangeRecord, PluginConfigDTO, ExecutionContext)`: 执行单个文件变更

### 2. 插件注册表 (PluginRegistry)

#### 模块职责
管理所有插件的注册、加载、卸载等操作。

#### 核心方法
- `registerPlugin(IPlugin)`: 注册插件
- `unregisterPlugin(String)`: 注销插件
- `getPlugin(String)`: 获取指定插件
- `getAvailablePlugins()`: 获取所有可用插件
- `getInternalPlugins()`: 获取内部插件
- `getExternalPlugins()`: 获取外部插件
- `reloadPlugins()`: 重新加载插件
- `scanExternalPluginDirectory(String)`: 扫描外部插件目录
- `loadExternalPlugins(String)`: 加载外部插件
- `reloadExternalPlugins()`: 重新加载外部插件

### 3. 插件上下文 (ExecutionContext)

#### 模块职责
提供插件执行过程中的环境支持，包括日志记录、进度跟踪、属性存储等。

#### 核心方法

##### 基本功能
- `logInfo(String)`: 记录信息日志
- `logError(String)`: 记录错误日志
- `logWarn(String)`: 记录警告日志
- `logDebug(String)`: 记录调试日志
- `updateProgress(long, long)`: 更新进度
- `getProgress()`: 获取当前进度
- `cancel()`: 取消执行
- `isCancelled()`: 检查是否已取消
- `getExecutionTime()`: 获取执行时间

##### 扩展功能
- `setAttribute(String, Object)`: 设置属性
- `getAttribute(String)`: 获取属性
- `getAttribute(String, T)`: 获取属性（带默认值）
- `startTimer()`: 开始计时
- `stopTimer()`: 停止计时并返回耗时

### 4. 抽象插件基类 (AbstractPlugin)

#### 模块职责
提供插件的基础实现，简化插件开发。

#### 核心方法
- `preview(List<String>, PluginConfigDTO, ExecutionContext)`: 批量预览实现
- `execute(List<String>, PluginConfigDTO, ExecutionContext)`: 批量执行实现
- `analyze(ChangeRecord, List<ChangeRecord>, List<File>, PluginConfigDTO, ExecutionContext)`: 逐个文件分析实现
- `execute(ChangeRecord, PluginConfigDTO, ExecutionContext)`: 单个文件执行实现

#### 抽象方法
- `createPreviewRecord(String, PluginConfigDTO, ExecutionContext)`: 创建预览记录（子类实现）
- `executeForFile(String, PluginConfigDTO, ExecutionContext)`: 执行文件处理（子类实现）

### 5. 可配置策略基类 (AbstractConfigurableStrategy)

#### 模块职责
提供可配置策略的基础实现，简化策略开发。

#### 核心方法
- `getConfigFields()`: 获取配置字段列表
- `initializeDefaultConfig()`: 初始化默认配置
- `validateConfig(StrategyConfigDTO)`: 验证配置
- `getConfigValue(StrategyConfigDTO, String, T)`: 获取配置值
- `setConfigValue(StrategyConfigDTO, String, Object)`: 设置配置值

#### 配置字段管理
- `addConfigField(String, String, String, Object, String, boolean)`: 添加配置字段
- `addConfigField(String, String, String, Object, String, boolean, List<String>)`: 添加带选项的配置字段
- `addEnumConfigField(String, String, String, Object, String, boolean, List<EnumOptionDTO>)`: 添加带枚举选项的配置字段
- `getConfigField(String)`: 获取配置字段

#### 抽象方法
- `initConfigFields()`: 初始化配置字段（子类实现）
- `initDefaultConfigValues(StrategyConfigDTO)`: 初始化默认配置值（子类实现）
- `createPreviewRecord(String, StrategyConfigDTO, ExecutionContext)`: 创建预览记录（子类实现）
- `executeForFile(String, StrategyConfigDTO, ExecutionContext)`: 执行文件处理（子类实现）

## 策略实现模块

### 1. 文件操作策略

#### FileMigrateStrategy
**职责**: 文件批量归档和移动

**核心功能**:
- 支持移动、复制操作
- 支持多种路径模式（子目录、指定目录、根目录）
- 支持文件去重
- 支持生效范围控制

**主要参数**:
- `operationMode`: 操作模式（MOVE/COPY）
- `outputDirMode`: 输出目录模式（SUBDIRECTORY/SPECIFIED_DIR/ROOT）
- `outputPath`: 输出路径
- `scope`: 生效范围（ALL/SPECIFIED_DEPTH）
- `depth`: 深度值
- `keepLargest`: 保留最大文件
- `keepEarliest`: 保留最早文件
- `keepExt`: 优先后缀
- `audioSpecial`: 音频特殊处理

#### FileRenameStrategy
**职责**: 文件重命名

**核心功能**:
- 支持多种重命名模式
- 支持大小写转换
- 支持特殊字符处理

**主要参数**:
- `renameMode`: 重命名模式
- `caseMode`: 大小写模式
- `specialCharMode`: 特殊字符模式

#### FileTypeFixStrategy
**职责**: 文件类型修复

**核心功能**:
- 支持多种目标格式
- 支持文件类型检测
- 支持批量修复

**主要参数**:
- `targetFormat`: 目标格式
- `detectMode`: 检测模式

### 2. 音频处理策略

#### AudioConverterStrategy
**职责**: 音频格式转换

**核心功能**:
- 支持多种音频格式转换
- 支持音频质量设置
- 支持采样率设置
- 支持声道数设置

**主要参数**:
- `targetFormat`: 目标格式（MP3/FLAC/WAV/AAC/OGG）
- `bitrate`: 比特率
- `sampleRate`: 采样率
- `channels`: 声道数
- `codec`: 编码器

#### CueSplitterStrategy
**职责**: CUE分轨

**核心功能**:
- 支持CUE文件解析
- 支持音频文件分割
- 支持分轨后处理

**主要参数**:
- `afterSplitAction`: 分割后操作（DELETE_ORIGINAL/KEEP_ORIGINAL/MOVE_ORIGINAL）

### 3. 元数据处理策略

#### MetadataScraperStrategy
**职责**: 元数据抓取

**核心功能**:
- 支持多种数据源（MusicBrainz/Last.fm/Discogs）
- 支持并发抓取
- 支持标签更新

**主要参数**:
- `dataSource`: 数据源
- `updateTags`: 更新标签
- `threadCount`: 线程数

#### AdvancedRenameStrategy
**职责**: 高级重命名

**核心功能**:
- 支持复杂重命名规则
- 支持条件重命名
- 支持批量重命名

**主要参数**:
- `renameRules`: 重命名规则列表
- `processScope`: 处理范围

### 4. NCM处理策略

#### NcmIntegratedStrategy
**职责**: 网易云音乐工具集成

**核心功能**:
- 支持NCM文件转换
- 支持缓存文件扫描
- 支持歌词下载

**主要参数**:
- `operationMode`: 操作模式（CONVERT/CACHE_SCAN/LYRIC_DOWNLOAD）
- `outputFormat`: 输出格式
- `downloadLyric`: 下载歌词

## 后端服务模块

### 1. 控制器模块 (controller)

#### PipelineController
**职责**: 流水线管理控制器

**核心方法**:
- `analyzePipeline(@RequestBody Map)`: 分析流水线
- `executePipeline(@RequestBody Map)`: 执行流水线
- `getChanges(@RequestParam Map)`: 获取变更记录
- `stopPipeline(@RequestBody Map)`: 停止流水线
- `getTaskStatus(@RequestParam String)`: 获取任务状态

#### PluginController
**职责**: 插件管理控制器

**核心方法**:
- `getPlugins()`: 获取所有插件
- `getPlugin(@PathVariable String)`: 获取指定插件
- `updatePluginConfig(@PathVariable String, @RequestBody PluginConfigDTO)`: 更新插件配置
- `reloadPlugins()`: 重新加载插件

#### StrategyController
**职责**: 策略管理控制器

**核心方法**:
- `getStrategies()`: 获取所有策略
- `getStrategy(@PathVariable String)`: 获取指定策略
- `updateStrategyConfig(@PathVariable String, @RequestBody StrategyConfigDTO)`: 更新策略配置

### 2. 服务实现模块 (service)

#### PluginServiceImpl
**职责**: 插件服务实现

**核心功能**:
- 插件注册管理
- 插件配置管理
- 插件执行调用
- 前置条件评估

#### FileFilterServiceImpl
**职责**: 文件过滤服务实现

**核心功能**:
- 文件过滤规则管理
- 文件包含/排除判断
- 默认过滤规则初始化

#### PreviewLimitServiceImpl
**职责**: 预览和执行数量限制服务实现

**核心功能**:
- 全局限制管理
- 根路径限制管理
- 限制配置持久化

### 3. 工具类模块 (util)

#### FileScanner
**职责**: 文件扫描工具

**核心功能**:
- 并行文件扫描
- 深度控制
- 数量限制
- 进度回调

#### ParallelStreamWalker
**职责**: 并行流式文件遍历器

**核心功能**:
- 高效文件遍历
- 并行处理
- 限制控制

#### UnifiedLogger
**职责**: 统一日志工具

**核心功能**:
- 后端操作日志
- 后端错误日志
- 后端警告日志

## 前端客户端模块

### 1. API客户端模块 (api)

#### ApiClient
**职责**: 基础API客户端

**核心功能**:
- HTTP请求封装
- 错误处理
- 响应解析

#### StrategyService
**职责**: 策略服务客户端

**核心功能**:
- 策略列表获取
- 策略配置获取
- 策略执行调用

#### PluginService
**职责**: 插件服务客户端

**核心功能**:
- 插件列表获取
- 插件配置获取
- 插件执行调用

#### PipelineService
**职责**: 流水线服务客户端

**核心功能**:
- 流水线配置获取
- 流水线执行调用
- 变更记录获取

### 2. 数据模型模块 (models)

#### StrategyInfo
**职责**: 策略信息模型

**核心字段**:
- `id`: 策略ID
- `name`: 策略名称
- `description`: 策略描述
- `version`: 策略版本

#### StrategyConfig
**职责**: 策略配置模型

**核心字段**:
- `configValues`: 配置值映射
- `preconditionGroups`: 前置条件组列表

#### ChangeRecord
**职责**: 变更记录模型

**核心字段**: 与后端ChangeRecord实体对应

### 3. 页面组件模块 (pages)

#### ComposePage
**职责**: 策略配置页面

**核心功能**:
- 策略选择
- 策略参数配置
- 预览结果展示

#### PipelineConfigPage
**职责**: 流水线配置页面

**核心功能**:
- 流水线节点配置
- 节点顺序调整
- 流水线保存

#### TaskMonitorPage
**职责**: 任务监控页面

**核心功能**:
- 任务状态展示
- 任务进度展示
- 任务日志展示

## 模块依赖关系

```
前端页面
  ↓
API客户端
  ↓
RESTful API
  ↓
控制器
  ↓
服务层
  ↓
插件系统
  ↓
策略实现
```

## 模块设计原则

### 1. 单一职责原则
每个模块只负责一个功能领域，保持模块职责清晰。

### 2. 接口隔离原则
模块之间通过接口进行通信，降低模块间的耦合度。

### 3. 依赖倒置原则
高层模块不依赖低层模块，都依赖于抽象接口。

### 4. 开闭原则
模块对扩展开放，对修改关闭。

### 5. 里氏替换原则
子类可以替换父类，不影响系统的正确性。

## 总结

FileManager Plus采用模块化设计，各模块职责清晰，接口定义明确，易于扩展和维护。插件系统提供了灵活的扩展能力，策略系统提供了丰富的文件处理功能。
