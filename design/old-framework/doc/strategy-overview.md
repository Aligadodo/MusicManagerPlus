# Strategy 策略模块设计大纲

## 目录结构概览

```
src/main/java/com/filemanager/strategy/
├── base/                           # 基础组件
│   ├── IConfigComponent.java       # 配置组件接口
│   └── PathSelectionComponent.java # 路径选择组件
├── cleanup/                        # 清理相关
│   ├── CleanupMode.java            # 清理模式
│   ├── DeleteMethod.java           # 删除方法
│   └── FileSizeRange.java          # 文件大小范围
├── collection/                     # 合集相关
│   ├── CollectionDeterminationAlgorithm.java  # 合集判定算法
│   ├── FileClusteringAlgorithm.java            # 文件聚类算法
│   ├── FilenameNormalizer.java                 # 文件名标准化
│   └── TextSimilarityCalculator.java           # 文本相似度计算
├── ncm/                            # NCM相关
│   ├── model/                      # 数据模型
│   ├── tool/                       # 工具类
│   ├── NcmBaseStrategy.java        # NCM基础策略
│   ├── NcmConvertStrategy.java     # NCM转换策略
│   ├── NcmCacheTransStrategy.java  # NCM缓存转换策略
│   ├── NcmLyricDownloadStrategy.java # NCM歌词下载策略
│   └── NcmIntegratedStrategy.java  # NCM集成策略
├── rename/                         # 重命名相关
│   ├── RenameActionType.java       # 重命名动作类型
│   ├── RenameMode.java             # 重命名模式
│   ├── RenameRule.java             # 重命名规则
│   └── RenameRuleListCell.java     # 重命名规则列表单元格
├── scraper/                        # 抓取相关
│   ├── LyricsManager.java          # 歌词管理器
│   ├── LyricsProvider.java         # 歌词提供者接口
│   ├── MiguLyricsProvider.java     # 咪咕歌词提供者
│   ├── NeteaseLyricsProvider.java  # 网易云歌词提供者
│   └── ScrapedResult.java          # 抓取结果
├── AbstractFfmpegStrategy.java     # FFmpeg抽象策略
├── AdvancedRenameStrategy.java     # 高级重命名策略
├── AlbumDirNormalizeStrategy.java  # 专辑目录标准化策略
├── AppStrategyFactory.java         # 策略工厂
├── AudioConverterStrategy.java     # 音频转换策略
├── CueFileRenameStrategy.java      # CUE文件重命名策略
├── CueSplitterStrategy.java        # CUE分轨策略
├── FileCleanupStrategy.java         # 文件清理策略
├── FileCollectionStrategy.java     # 文件归类策略
├── FileMigrateStrategy.java         # 文件迁移策略
├── FileTypeFixStrategy.java        # 文件类型修复策略
├── FileUnzipStrategy.java          # 文件解压策略
├── MetadataScraperStrategy.java     # 元数据抓取策略
├── NcmIntegratedStrategy.java      # NCM集成策略
└── TrackNumberStrategy.java        # 音轨编号策略
```

## 策略分类

### 1. 基础架构类

#### AbstractFfmpegStrategy
- **功能**: FFmpeg相关操作的抽象基类
- **继承**: IAppStrategy
- **子类**: AudioConverterStrategy, CueSplitterStrategy
- **核心职责**:
  - FFmpeg初始化和配置
  - 通用FFmpeg参数管理
  - 路径选择和输出控制
  - 覆盖控制

#### AppStrategyFactory
- **功能**: 策略工厂，负责创建和管理所有策略实例
- **核心职责**:
  - 策略实例化
  - 策略注册和查找
  - 策略生命周期管理

### 2. 音频处理策略

#### AudioConverterStrategy
- **功能**: 音频格式转换
- **继承**: AbstractFfmpegStrategy
- **核心功能**:
  - 支持多种音频格式转换（MP3, FLAC, WAV, AAC等）
  - CD模式支持（特殊参数配置）
  - 跳过大文件+同目录CUE的处理
  - 元数据保留
- **关键参数**:
  - 目标格式选择
  - 比特率/采样率配置
  - 覆盖控制
  - CUE文件跳过选项

#### TrackNumberStrategy
- **功能**: 音轨编号处理
- **继承**: IAppStrategy
- **核心功能**:
  - 默认排序（按文件名/拼音）
  - 文本列表匹配（.txt/.nfo）
  - 双位补零（01, 02）
  - 自定义分隔符
- **关键参数**:
  - 编号模式
  - 补零选项
  - 分隔符设置

#### CueSplitterStrategy
- **功能**: CUE文件分轨
- **继承**: AbstractFfmpegStrategy
- **核心功能**:
  - CUE文件解析
  - 音频源文件智能定位
  - 基于时间戳精确切割
  - 元数据写入
  - 分轨后处理（归档、删除等）
- **关键参数**:
  - 音频源文件路径
  - 输出格式
  - 分轨后动作
  - 归档目录

#### CueFileRenameStrategy
- **功能**: CUE文件重命名
- **继承**: IAppStrategy
- **核心功能**:
  - CUE文件名称标准化
  - 音轨文件名称同步
  - 元数据一致性检查

### 3. 文件管理策略

#### FileCollectionStrategy
- **功能**: 基于文件名相似度将文件/文件夹归类到合集文件夹
- **继承**: IAppStrategy
- **核心功能**:
  - 文件夹聚类算法
  - 文件名标准化
  - 文本相似度计算
  - 合集名称生成
  - 关键词过滤
- **关键参数**:
  - 相似度阈值
  - 合集后缀
  - 扫描目标类型
  - 必须包含/不包含的关键词
- **依赖组件**:
  - FileClusteringAlgorithm
  - FilenameNormalizer
  - TextSimilarityCalculator

#### FileCleanupStrategy
- **功能**: 文件清理与去重
- **继承**: IAppStrategy
- **核心功能**:
  - 文件去重（基于MD5/内容）
  - 文件夹去重
  - 空目录清理
  - 伪删除（归档到垃圾箱）
- **关键参数**:
  - 清理模式（文件/文件夹）
  - 删除方法（直接删除/归档）
  - 文件大小范围
  - 扫描目标类型

#### FileMigrateStrategy
- **功能**: 文件迁移
- **继承**: IAppStrategy
- **核心功能**:
  - 按路径模式迁移文件
  - 目标目录结构保持
  - 空目录清理
  - 元数据保留
- **关键参数**:
  - 目标根目录
  - 路径模式
  - 清理空目录选项

#### FileUnzipStrategy
- **功能**: 文件解压
- **继承**: IAppStrategy
- **核心功能**:
  - 支持多种压缩格式
  - 批量解压
  - 解压后处理
- **关键参数**:
  - 解压引擎选择
  - 输出目录
  - 覆盖控制

#### FileTypeFixStrategy
- **功能**: 文件类型修复
- **继承**: IAppStrategy
- **核心功能**:
  - 文件扩展名修复
  - 文件内容检测
  - 批量修复

### 4. 重命名策略

#### AdvancedRenameStrategy
- **功能**: 高级重命名
- **继承**: IAppStrategy
- **核心功能**:
  - 基于规则的重命名
  - 支持多个条件组合
  - 支持多种重命名动作
  - 规则优先级管理
- **关键参数**:
  - 重命名规则列表
  - 条件类型（文件名、大小、日期等）
  - 动作类型（替换、添加、删除等）
  - 重命名模式

#### AlbumDirNormalizeStrategy
- **功能**: 专辑目录标准化
- **继承**: IAppStrategy
- **核心功能**:
  - 基于模板重命名专辑目录
  - 支持元数据占位符（%artist%, %year%, %album%等）
  - 批量标准化
- **关键参数**:
  - 目录命名模板
  - 占位符解析
  - 元数据提取

### 5. 元数据处理策略

#### MetadataScraperStrategy
- **功能**: 元数据抓取
- **继承**: IAppStrategy
- **核心功能**:
  - 从在线源抓取元数据
  - 支持网易云音乐、咪咕音乐等
  - 歌词下载
  - 封面图片下载
  - 元数据写入音频文件
- **关键参数**:
  - 数据源选择
  - 抓取模式
  - 覆盖控制
  - 歌词下载选项

### 6. NCM相关策略

#### NcmBaseStrategy
- **功能**: NCM基础策略
- **继承**: IAppStrategy, IConfigComponent
- **核心功能**:
  - 提供通用的NCM处理功能
  - 路径选择组件集成
  - 输出路径管理
- **子类**: NcmConvertStrategy, NcmCacheTransStrategy, NcmLyricDownloadStrategy

#### NcmConvertStrategy
- **功能**: NCM格式转换
- **继承**: NcmBaseStrategy
- **核心功能**:
  - NCM文件解密
  - 转换为标准音频格式
  - 元数据提取和保留

#### NcmCacheTransStrategy
- **功能**: NCM缓存转换
- **继承**: NcmBaseStrategy
- **核心功能**:
  - NCM缓存文件处理
  - 批量转换
  - 缓存文件清理

#### NcmLyricDownloadStrategy
- **功能**: NCM歌词下载
- **继承**: NcmBaseStrategy
- **核心功能**:
  - 从NCM文件提取歌词信息
  - 下载对应歌词文件
  - 歌词格式转换

#### NcmIntegratedStrategy
- **功能**: NCM集成策略
- **继承**: IAppStrategy
- **核心功能**:
  - 集成多种NCM处理功能
  - 一键转换、下载歌词等

## 核心设计模式

### 1. 策略模式 (Strategy Pattern)
- 所有策略继承自IAppStrategy
- 统一的接口定义（getName(), getConfigNode(), execute()等）
- 策略工厂负责创建和管理策略实例

### 2. 模板方法模式 (Template Method Pattern)
- AbstractFfmpegStrategy定义FFmpeg操作的模板
- 子类实现具体的处理逻辑

### 3. 工厂模式 (Factory Pattern)
- AppStrategyFactory负责策略实例化
- 支持策略注册和动态加载

### 4. 组件模式 (Component Pattern)
- IConfigComponent定义配置组件接口
- PathSelectionComponent提供路径选择功能
- 各策略通过组合使用这些组件

## 核心组件

### 1. IConfigComponent
- **功能**: 配置组件接口
- **方法**:
  - getConfigNode(): 获取配置UI节点
  - captureParams(): 捕获配置参数

### 2. PathSelectionComponent
- **功能**: 路径选择组件
- **功能**:
  - 源路径选择
  - 输出路径选择
  - 路径模式选择（根目录、子目录等）
  - 参数持久化

### 3. CleanupUIConfig
- **功能**: 清理UI配置
- **职责**: 提供清理策略的UI配置界面

### 4. CleanupParams
- **功能**: 清理参数
- **职责**: 存储清理策略的运行参数

## 辅助模块

### 1. collection模块
- **FileClusteringAlgorithm**: 文件聚类算法
- **FilenameNormalizer**: 文件名标准化
- **TextSimilarityCalculator**: 文本相似度计算
- **CollectionDeterminationAlgorithm**: 合集判定算法

### 2. rename模块
- **RenameRule**: 重命名规则
- **RenameMode**: 重命名模式
- **RenameActionType**: 重命名动作类型
- **RenameRuleListCell**: 重命名规则列表单元格

### 3. scraper模块
- **LyricsManager**: 歌词管理器
- **LyricsProvider**: 歌词提供者接口
- **NeteaseLyricsProvider**: 网易云歌词提供者
- **MiguLyricsProvider**: 咪咕歌词提供者
- **ScrapedResult**: 抓取结果

### 4. ncm模块
- **model**: NCM数据模型
- **tool**: NCM工具类（文件解析、API调用等）

## 设计原则

### 1. 单一职责原则
- 每个策略只负责一种特定的文件操作
- 辅助模块提供特定的功能支持

### 2. 开闭原则
- 通过继承和组合扩展功能
- 新增策略无需修改现有代码

### 3. 依赖倒置原则
- 依赖抽象接口（IAppStrategy, IConfigComponent）
- 不依赖具体实现

### 4. 接口隔离原则
- IConfigComponent提供最小化的配置接口
- 各策略只实现需要的方法

## 交互设计

### 1. UI组件统一性
- 使用JFX控件（JFXButton, JFXComboBox等）
- 统一的样式和主题
- 悬浮提示（FloatingTooltip）

### 2. 配置管理
- 参数持久化到Properties
- 默认值设置
- 参数验证

### 3. 执行流程
1. 用户选择策略
2. 配置参数
3. 选择目标路径
4. 执行操作
5. 查看结果

### 4. 结果反馈
- ChangeRecord记录操作结果
- 执行状态（ExecStatus）
- 操作类型（OperationType）

## 注意事项

### 1. FFmpeg依赖
- 需要正确配置FFmpeg环境
- 版本兼容性考虑
- 参数验证

### 2. 文件操作安全
- 覆盖控制
- 文件存在检查
- 异常处理

### 3. 性能优化
- 大文件处理
- 批量操作优化
- 多线程支持

### 4. 元数据处理
- 编码问题（UTF-8）
- 特殊字符处理
- 元数据完整性

### 5. NCM处理
- 加密算法兼容性
- 缓存文件管理
- 版权合规性

## 优化思路

### 1. 算法优化
- 文件聚类算法优化
- 文本相似度计算优化
- 去重算法优化

### 2. 性能优化
- 多线程处理
- 内存优化
- I/O优化

### 3. 功能扩展
- 支持更多音频格式
- 支持更多数据源
- 支持更多压缩格式

### 4. 用户体验
- 更友好的UI
- 更详细的进度反馈
- 更好的错误提示

## 文档清单

### 已完成文档
- [合集自动化测试开发指南](../skill/readme-collection-develop.md)

### 待编写文档
- [AbstractFfmpegStrategy设计文档](AbstractFfmpegStrategy.md)
- [AudioConverterStrategy设计文档](AudioConverterStrategy.md)
- [TrackNumberStrategy设计文档](TrackNumberStrategy.md)
- [CueSplitterStrategy设计文档](CueSplitterStrategy.md)
- [CueFileRenameStrategy设计文档](CueFileRenameStrategy.md)
- [FileCollectionStrategy设计文档](FileCollectionStrategy.md)
- [FileCleanupStrategy设计文档](FileCleanupStrategy.md)
- [FileMigrateStrategy设计文档](FileMigrateStrategy.md)
- [FileUnzipStrategy设计文档](FileUnzipStrategy.md)
- [FileTypeFixStrategy设计文档](FileTypeFixStrategy.md)
- [AdvancedRenameStrategy设计文档](AdvancedRenameStrategy.md)
- [AlbumDirNormalizeStrategy设计文档](AlbumDirNormalizeStrategy.md)
- [MetadataScraperStrategy设计文档](MetadataScraperStrategy.md)
- [NcmBaseStrategy设计文档](NcmBaseStrategy.md)
- [NcmConvertStrategy设计文档](NcmConvertStrategy.md)
- [NcmCacheTransStrategy设计文档](NcmCacheTransStrategy.md)
- [NcmLyricDownloadStrategy设计文档](NcmLyricDownloadStrategy.md)
- [NcmIntegratedStrategy设计文档](NcmIntegratedStrategy.md)

---

**文档版本**: 1.0  
**最后更新**: 2026-01-27  
**维护者**: FileEditTools Team
