# 老架构与新架构策略实现比对文档

## 概述

本文档详细对比了老架构（JavaFX）和新架构（Flutter Web + Spring Boot）下策略实现的差异，并规划了功能迁移路径。

## 架构差异总览

| 维度 | 老架构 | 新架构 |
|------|--------|--------|
| **策略基类** | IAppStrategy抽象类 | IPlugin接口 |
| **UI框架** | JavaFX (JFX控件) | Flutter Web |
| **配置管理** | Properties文件 | PluginConfigDTO + REST API |
| **执行方式** | 直接调用 | REST API调用 |
| **状态管理** | ChangeRecord对象 | ChangeRecord + WebSocket推送 |
| **插件发现** | AppStrategyFactory | Java ServiceLoader机制 |
| **并发处理** | ThreadPoolManager | Spring线程池 + 自定义线程池 |

## 策略功能对比表

### 1. 文件管理策略

| 策略名称 | 老架构实现 | 新架构实现 | 差异说明 | 迁移优先级 |
|---------|-----------|-----------|----------|-----------|
| **文件归类** | FileCollectionStrategy | FileCollectionPlugin (待实现) | ✅ 核心算法可复用<br>❌ UI需重写<br>❌ 配置需适配 | **高** |
| **文件清理** | FileCleanupStrategy | FileCleanupPlugin (待实现) | ✅ 去重算法可复用<br>❌ UI需重写<br>❌ 删除逻辑需适配 | **高** |
| **文件迁移** | FileMigrateStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 逻辑简单，易于实现 | **中** |
| **文件解压** | FileUnzipStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 解压引擎可复用 | **中** |
| **文件类型修复** | FileTypeFixStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 逻辑简单，易于实现 | **低** |
| **高级重命名** | AdvancedRenameStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 规则引擎可复用 | **高** |
| **专辑目录标准化** | AlbumDirNormalizeStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 模板引擎可复用 | **中** |

### 2. 音频处理策略

| 策略名称 | 老架构实现 | 新架构实现 | 差异说明 | 迁移优先级 |
|---------|-----------|-----------|----------|-----------|
| **音频转换** | AudioConverterStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ FFmpeg封装可复用<br>❌ 需后端支持 | **高** |
| **音轨编号** | TrackNumberStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 编号逻辑可复用 | **中** |
| **CUE分轨** | CueSplitterStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ CUE解析可复用<br>❌ 需后端支持 | **高** |
| **CUE重命名** | CueFileRenameStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 同步逻辑可复用 | **中** |

### 3. 元数据处理策略

| 策略名称 | 老架构实现 | 新架构实现 | 差异说明 | 迁移优先级 |
|---------|-----------|-----------|----------|-----------|
| **元数据抓取** | MetadataScraperStrategy | MetadataScraperPlugin (待实现) | ✅ 数据源可复用<br>✅ 缓存机制可复用<br>❌ UI需重写<br>❌ 限流需适配 | **高** |
| **歌词下载** | NeteaseLyricsProvider/MiguLyricsProvider | ❌ 未实现 | ✅ 提供者可复用<br>❌ 需整合到插件 | **中** |

### 4. NCM相关策略

| 策略名称 | 老架构实现 | 新架构实现 | 差异说明 | 迁移优先级 |
|---------|-----------|-----------|----------|-----------|
| **NCM转换** | NcmConvertStrategy | ❌ 未实现 | ✅ 解密算法可复用<br>❌ 需后端支持 | **高** |
| **NCM缓存转换** | NcmCacheTransStrategy | ❌ 未实现 | ✅ 缓存解析可复用<br>❌ 需后端支持 | **中** |
| **NCM歌词下载** | NcmLyricDownloadStrategy | ❌ 未实现 | ✅ 歌词提取可复用<br>❌ 需后端支持 | **中** |
| **NCM集成** | NcmIntegratedStrategy | ❌ 未实现 | ❌ 完全缺失<br>✅ 可组合其他NCM策略 | **低** |

## 核心差异详细分析

### 1. 文件归类策略 (FileCollectionStrategy)

#### 老架构实现特点

**核心组件**：
- `FilenameNormalizer` - 文件名标准化
- `TextSimilarityCalculator` - 文本相似度计算
- `FileClusteringAlgorithm` - 文件聚类算法
- `CollectionDeterminationAlgorithm` - 合集判定算法
- `ICollectionNamingStrategy` - 合集命名策略（多种实现）

**配置参数**：
```java
- 相似度阈值 (0.0-1.0)
- 合集文件夹格式 (如：【合集】)
- 目标类型 (文件/文件夹/全部)
- 命名策略 (精确/简洁/模板/通用)
- 必须包含关键词
- 不能包含关键词
```

**UI组件**：
- Slider - 相似度阈值滑块
- TextField - 合集格式、关键词输入
- JFXComboBox - 目标类型、命名策略选择
- TitledPane - 分组显示配置项

**执行流程**：
1. 扫描源目录获取文件/文件夹列表
2. 标准化文件名
3. 计算相似度矩阵
4. 基于阈值进行聚类
5. 生成合集名称
6. 创建合集目录并移动文件

#### 新架构迁移方案

**可复用组件**：
- ✅ `FilenameNormalizer` - 无需修改
- ✅ `TextSimilarityCalculator` - 无需修改
- ✅ `FileClusteringAlgorithm` - 无需修改
- ✅ `CollectionDeterminationAlgorithm` - 无需修改
- ✅ `ICollectionNamingStrategy` - 无需修改

**需要重构的部分**：
- ❌ UI组件 - 需用Flutter重写
- ❌ 配置管理 - 需适配PluginConfigDTO
- ❌ 文件操作 - 需通过REST API调用
- ❌ 进度反馈 - 需通过WebSocket推送

**迁移步骤**：
1. 创建`FileCollectionPlugin`实现`IPlugin`接口
2. 将老架构的核心算法代码迁移到插件中
3. 实现REST API端点：
   - `POST /api/plugins/file-collection/analyze`
   - `POST /api/plugins/file-collection/execute`
4. 创建Flutter UI配置界面
5. 实现配置参数的序列化/反序列化

**配置DTO示例**：
```json
{
  "threshold": 0.9,
  "collectionSuffix": "【合集】",
  "targetType": "FOLDERS_ONLY",
  "namingStrategy": "PRECISE",
  "mustContainKeywords": ["CD", "系列", "合集"],
  "mustNotContainKeywords": ["下载", "Album", "群星"]
}
```

### 2. 元数据抓取策略 (MetadataScraperStrategy)

#### 老架构实现特点

**核心组件**：
- `MetadataSource` - 数据源接口
  - `LocalInferenceSource` - 本地推断
  - `NeteaseMusicSource` - 网易云音乐
  - `MiguMusicSource` - 咪咕音乐
  - `MusicBrainzSource` - MusicBrainz
  - `ITunesSource` - iTunes
  - `LastFmSource` - Last.fm
  - `DiscogsSource` - Discogs
- `MetadataScraperProcessor` - 处理器
- `MetadataCacheManager` - 缓存管理器
- `RateLimiter` - 限流器

**模块化配置**：
- `LyricsModuleConfig` - 歌词模块配置
- `CoverModuleConfig` - 封面模块配置
- `AlbumInfoModuleConfig` - 专辑信息模块配置

**配置参数**：
```java
- 数据源选择
- 网络并发线程数 (1-8)
- 歌词匹配配置 (启用/覆盖/格式)
- 封面匹配配置 (启用/覆盖/尺寸)
- 专辑信息配置 (启用/覆盖/字段)
- API限流配置 (最大请求数/时间窗口)
```

**UI组件**：
- JFXComboBox - 数据源选择
- Spinner - 线程数选择
- ModuleConfigUI - 模块化配置UI
- TitledPane - 分组显示配置项
- FloatingTooltip - 参数说明提示

**执行流程**：
1. 扫描音频文件
2. 提取文件元数据（歌手、专辑、标题）
3. 根据配置的数据源查询在线数据库
4. 应用限流策略避免API滥用
5. 缓存查询结果
6. 根据模块配置更新音频文件
7. 下载歌词、封面、专辑简介

#### 新架构迁移方案

**可复用组件**：
- ✅ 所有`MetadataSource`实现 - 无需修改
- ✅ `MetadataScraperProcessor` - 无需修改
- ✅ `MetadataCacheManager` - 无需修改
- ✅ `RateLimiter` - 无需修改
- ✅ 所有`ModuleConfig` - 无需修改

**需要重构的部分**：
- ❌ UI组件 - 需用Flutter重写
- ❌ 配置管理 - 需适配PluginConfigDTO
- ❌ 文件操作 - 需通过REST API调用
- ❌ 进度反馈 - 需通过WebSocket推送

**迁移步骤**：
1. 创建`MetadataScraperPlugin`实现`IPlugin`接口
2. 将老架构的核心代码迁移到插件中
3. 实现REST API端点：
   - `POST /api/plugins/metadata-scraper/analyze`
   - `POST /api/plugins/metadata-scraper/execute`
4. 创建Flutter UI配置界面
5. 实现配置参数的序列化/反序列化
6. 集成WebSocket进度推送

**配置DTO示例**：
```json
{
  "source": "MusicBrainz",
  "threads": 4,
  "lyricsConfig": {
    "enabled": true,
    "overwrite": false,
    "format": "lrc"
  },
  "coverConfig": {
    "enabled": true,
    "overwrite": false,
    "maxSize": 1200
  },
  "albumInfoConfig": {
    "enabled": true,
    "overwrite": false,
    "fields": ["artist", "album", "year", "genre"]
  },
  "rateLimiterConfig": {
    "maxRequests": 10,
    "periodMs": 1000
  }
}
```

### 3. 文件清理策略 (FileCleanupStrategy)

#### 老架构实现特点

**核心组件**：
- `DuplicateAnalyzer` - 重复文件分析器
- `DeleteExecutor` - 删除执行器
- `DuplicateStrategyManager` - 去重策略管理器
  - `KeepBestVersionStrategy` - 保留最佳版本
  - `AddSequenceStrategy` - 添加序号

**配置参数**：
```java
- 清理模式 (文件/文件夹/全部)
- 删除方法 (直接删除/归档到垃圾箱)
- 文件大小范围
- 扫描目标类型
- 去重策略 (保留最大/保留最早/保留最新)
- 音频文件特殊处理
- 保留扩展名
```

**UI组件**：
- CleanupUIConfig - 清理UI配置组件
- TitledPane - 分组显示配置项

**执行流程**：
1. 扫描文件/文件夹
2. 计算文件哈希值
3. 识别重复文件
4. 根据去重策略选择保留版本
5. 执行删除或归档操作
6. 清理空目录

#### 新架构迁移方案

**可复用组件**：
- ✅ `DuplicateAnalyzer` - 无需修改
- ✅ `DeleteExecutor` - 需适配REST API
- ✅ `DuplicateStrategyManager` - 无需修改
- ✅ 所有去重策略 - 无需修改

**需要重构的部分**：
- ❌ UI组件 - 需用Flutter重写
- ❌ 配置管理 - 需适配PluginConfigDTO
- ❌ 文件操作 - 需通过REST API调用
- ❌ 进度反馈 - 需通过WebSocket推送

**迁移步骤**：
1. 创建`FileCleanupPlugin`实现`IPlugin`接口
2. 将老架构的核心代码迁移到插件中
3. 实现REST API端点：
   - `POST /api/plugins/file-cleanup/analyze`
   - `POST /api/plugins/file-cleanup/execute`
4. 创建Flutter UI配置界面
5. 实现配置参数的序列化/反序列化

**配置DTO示例**：
```json
{
  "cleanupMode": "FILES_ONLY",
  "deleteMethod": "ARCHIVE",
  "fileSizeRange": {
    "min": 0,
    "max": 1073741824
  },
  "targetType": "FILES_ONLY",
  "duplicateStrategy": "KEEP_LARGEST",
  "audioSpecial": true,
  "keepExtensions": ["flac", "mp3"]
}
```

### 4. 音频转换策略 (AudioConverterStrategy)

#### 老架构实现特点

**核心组件**：
- `AbstractFfmpegStrategy` - FFmpeg抽象策略
- FFmpeg命令封装
- 音频格式支持

**配置参数**：
```java
- 目标格式 (MP3/FLAC/WAV/AAC等)
- 比特率 (128k/192k/320k等)
- 采样率 (44100/48000等)
- 覆盖控制
- CD模式支持
- 跳过大文件+同目录CUE的处理
```

**UI组件**：
- JFXComboBox - 格式、比特率、采样率选择
- CheckBox - 覆盖控制、CD模式、跳过CUE

**执行流程**：
1. 扫描音频文件
2. 检查文件格式
3. 生成FFmpeg转换命令
4. 执行转换
5. 保留元数据

#### 新架构迁移方案

**可复用组件**：
- ✅ FFmpeg命令封装 - 无需修改
- ✅ 格式转换逻辑 - 无需修改

**需要重构的部分**：
- ❌ 完全未实现
- ❌ UI组件 - 需用Flutter重写
- ❌ 配置管理 - 需适配PluginConfigDTO
- ❌ 文件操作 - 需通过REST API调用
- ❌ FFmpeg执行 - 需后端支持

**迁移步骤**：
1. 创建`AudioConverterPlugin`实现`IPlugin`接口
2. 将老架构的FFmpeg封装代码迁移到插件中
3. 实现REST API端点：
   - `POST /api/plugins/audio-converter/analyze`
   - `POST /api/plugins/audio-converter/execute`
4. 创建Flutter UI配置界面
5. 实现配置参数的序列化/反序列化
6. 在后端集成FFmpeg

**配置DTO示例**：
```json
{
  "targetFormat": "mp3",
  "bitrate": "320k",
  "sampleRate": 44100,
  "overwrite": true,
  "cdMode": false,
  "skipCueFiles": true
}
```

### 5. CUE分轨策略 (CueSplitterStrategy)

#### 老架构实现特点

**核心组件**：
- `AbstractFfmpegStrategy` - FFmpeg抽象策略
- `CueParserUtil` - CUE文件解析工具
- FFmpeg分轨命令封装

**配置参数**：
```java
- 音频源文件路径
- 输出格式
- 分轨后动作 (归档/删除/保留)
- 归档目录
- 覆盖控制
```

**UI组件**：
- PathSelectionComponent - 路径选择组件
- JFXComboBox - 格式、动作选择
- TextField - 归档目录

**执行流程**：
1. 解析CUE文件
2. 定位音频源文件
3. 基于时间戳切割音频
4. 写入元数据
5. 执行分轨后动作

#### 新架构迁移方案

**可复用组件**：
- ✅ `CueParserUtil` - 无需修改
- ✅ FFmpeg分轨命令封装 - 无需修改

**需要重构的部分**：
- ❌ 完全未实现
- ❌ UI组件 - 需用Flutter重写
- ❌ 配置管理 - 需适配PluginConfigDTO
- ❌ 文件操作 - 需通过REST API调用
- ❌ FFmpeg执行 - 需后端支持

**迁移步骤**：
1. 创建`CueSplitterPlugin`实现`IPlugin`接口
2. 将老架构的CUE解析和FFmpeg代码迁移到插件中
3. 实现REST API端点：
   - `POST /api/plugins/cue-splitter/analyze`
   - `POST /api/plugins/cue-splitter/execute`
4. 创建Flutter UI配置界面
5. 实现配置参数的序列化/反序列化
6. 在后端集成FFmpeg

**配置DTO示例**：
```json
{
  "audioSourcePath": "/path/to/audio.flac",
  "outputFormat": "flac",
  "postSplitAction": "ARCHIVE",
  "archiveDirectory": "/path/to/archive",
  "overwrite": false
}
```

## 迁移优先级规划

### 第一阶段：核心文件管理策略 (2-3周)

**目标**：实现最常用的文件管理功能

| 策略 | 预计工作量 | 依赖项 |
|------|-----------|--------|
| FileCollectionPlugin | 5天 | 无 |
| FileCleanupPlugin | 4天 | 无 |
| FileMigratePlugin | 2天 | 无 |
| AdvancedRenamePlugin | 3天 | 无 |

### 第二阶段：音频处理策略 (3-4周)

**目标**：实现音频转换和处理功能

| 策略 | 预计工作量 | 依赖项 |
|------|-----------|--------|
| AudioConverterPlugin | 5天 | FFmpeg集成 |
| CueSplitterPlugin | 4天 | FFmpeg集成 |
| TrackNumberPlugin | 2天 | 无 |
| CueFileRenamePlugin | 2天 | 无 |
| AlbumDirNormalizePlugin | 2天 | 无 |

### 第三阶段：元数据处理策略 (2-3周)

**目标**：实现元数据抓取和管理功能

| 策略 | 预计工作量 | 依赖项 |
|------|-----------|--------|
| MetadataScraperPlugin | 5天 | 无 |
| LyricsDownloadPlugin | 2天 | MetadataScraperPlugin |

### 第四阶段：NCM相关策略 (2-3周)

**目标**：实现NCM格式处理功能

| 策略 | 预计工作量 | 依赖项 |
|------|-----------|--------|
| NcmConvertPlugin | 4天 | 无 |
| NcmCacheTransPlugin | 3天 | 无 |
| NcmLyricDownloadPlugin | 2天 | 无 |

### 第五阶段：辅助功能策略 (1-2周)

**目标**：实现辅助功能

| 策略 | 预计工作量 | 依赖项 |
|------|-----------|--------|
| FileUnzipPlugin | 2天 | 无 |
| FileTypeFixPlugin | 1天 | 无 |

## 技术债务和风险

### 技术债务

1. **配置兼容性**：老架构使用Properties文件，新架构使用JSON，需要转换工具
2. **UI提示信息**：老架构有详细的FloatingTooltip，新架构需要重新实现
3. **参数验证**：老架构有完善的参数验证，新架构需要重新实现
4. **错误处理**：老架构有详细的错误处理和日志，新架构需要重新实现

### 风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| FFmpeg集成复杂 | 高 | 中 | 提前进行技术验证 |
| 数据源API变更 | 中 | 中 | 使用缓存和降级策略 |
| 性能问题 | 中 | 低 | 使用线程池和异步处理 |
| 用户体验下降 | 高 | 低 | 充分的用户测试 |

## 测试策略

### 单元测试

- 为每个插件编写单元测试
- 测试核心算法的正确性
- 测试边界情况和异常处理

### 集成测试

- 测试插件与REST API的集成
- 测试插件与WebSocket的集成
- 测试插件之间的协作

### 端到端测试

- 测试完整的业务流程
- 测试用户操作场景
- 测试性能和稳定性

### 回归测试

- 使用老架构的测试用例
- 确保功能一致性
- 验证性能不下降

## 文档更新

### 需要更新的文档

1. **插件开发指南** - 更新插件开发流程
2. **API文档** - 更新所有插件API端点
3. **用户手册** - 更新用户操作指南
4. **测试用例文档** - 更新功能测试用例

### 需要创建的文档

1. **插件迁移指南** - 指导如何从老架构迁移插件
2. **配置迁移工具** - Properties到JSON的转换工具
3. **性能优化指南** - 插件性能优化建议

## 总结

老架构和新架构在策略实现上有显著差异，主要体现在：

1. **架构模式**：从直接调用到REST API调用
2. **UI框架**：从JavaFX到Flutter Web
3. **配置管理**：从Properties到JSON
4. **状态管理**：从直接对象到WebSocket推送

迁移的核心原则是：
- ✅ **复用核心算法**：最大化复用老架构的核心算法和业务逻辑
- ✅ **适配新架构**：将核心逻辑适配到新的插件架构
- ✅ **保持功能一致**：确保迁移后的功能与老架构一致
- ✅ **优化用户体验**：利用新架构的优势优化用户体验

通过分阶段、有计划的迁移，可以在保持功能完整性的同时，逐步完成从老架构到新架构的过渡。

---

**文档版本**: 1.0  
**创建日期**: 2026-02-03  
**维护者**: FileManager Plus Team
