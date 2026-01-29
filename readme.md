# Music Manager Plus

## 许可证 (License)
本项目由 **hrcao** 开发，采用定制的 **GPLv3 + 非商用限制** 协议。
- **修改与分发**：必须保持开源并使用相同协议。
- **商业用途**：完全禁止任何形式的商用行为。
- **联系方式**：chrse1997@163.com

## 项目介绍
由于市面上很难找到完全符合个人工作流的音频管理工具，我便自己动手开发了 **Music Manager Plus**。它旨在解决海量音频文件管理、格式转换、备份、音频分轨处理困难的问题，让大家高效听歌，享受音乐。

## 核心亮点
- **面向大量文件的极速处理**：支持十万级音频文件的扫描与处理
- **支持事先预览**：先查看变更结果，再执行操作，避免改错文件
- **可调节的处理性能**：根据存储固件和CPU水平设置合适的并发数量，支持实时估计执行时间
- **轻量化设计**：较低的内存占用，不干扰其他工作流程
- **自带关键运行环境**：Java运行环境和ffmpeg运行环境已具备，无需额外安装
- **高度自定义的功能**：自定义条件和转换规则，灵活配置
- **性能优化**：支持在额外缓存硬盘（推荐SSD）进行中转，转换效率提升5-10倍
- **策略化架构**：采用策略模式，支持灵活组合多种处理策略
- **元数据刮削**：支持从多个数据源自动获取音频元数据、歌词、封面等信息
- **合集自动生成**：基于文件名相似度自动生成合集文件夹

## 主要功能

### 文件管理
- **批量命名修改**：支持多种重命名规则，包括正则表达式替换、模板替换等
- **批量移动或复制文件**：支持跨盘操作，可配置移动或复制模式
- **批量文件清理**：按文件大小、类型、日期等条件清理文件
- **文件归类**：基于文件名相似度自动将文件归类到合集文件夹
- **专辑目录规范化**：规范化专辑目录命名格式

### 音频处理
- **批量音频格式转换**：支持MP3、WAV、FLAC、DTS、DFF等格式互转
- **批量分轨**：自动识别CUE文件并分割音频文件
- **批量修改音频分轨文件名**：解决部分软件如Nero无法识别中文文件名问题
- **音频元数据刮削**：支持从网易云音乐、咪咕音乐、MusicBrainz、iTunes、Last.fm、Discogs等数据源自动获取音频元数据
- **歌词下载**：自动下载歌词并保存到音频文件或独立文件
- **封面下载**：自动下载专辑封面图片
- **专辑信息生成**：自动生成专辑简介文档

### 文件解压
- **批量解压**：支持多种压缩格式，内置Java解压算法，建议优先使用7zip
- **多引擎支持**：支持Java内置引擎、7zip引擎、Bandizip引擎

### 网易云音乐处理
- **NCM格式转换**：将网易云音乐的NCM格式转换为MP3/FLAC
- **NCM缓存转换**：转换网易云音乐的缓存文件
- **NCM歌词下载**：下载网易云音乐的歌词

### 重复文件处理
- **重复文件检测**：检测重复文件
- **保留最佳版本**：根据文件质量保留最佳版本
- **添加序号**：为重复文件添加序号以避免覆盖

### 其他功能
- **文件类型修复**：修复文件扩展名错误
- **文件扫描**：支持递归扫描、深度控制、文件类型过滤
- **实时预览**：预览所有变更，确认无误后再执行
- **日志记录**：详细的操作日志，便于问题排查

## 项目结构
```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── filemanager/
│   │           ├── app/                    # 应用核心框架
│   │           │   ├── base/             # 基础接口定义
│   │           │   │   ├── IAppController.java
│   │           │   │   ├── IAppStrategy.java
│   │           │   │   ├── IConfigManager.java
│   │           │   │   ├── IDataProvider.java
│   │           │   │   ├── ITaskManager.java
│   │           │   │   ├── IUIElementProvider.java
│   │           │   │   ├── IViewManager.java
│   │           │   │   └── ...
│   │           │   ├── components/       # 核心组件
│   │           │   │   ├── CleanupUIConfig.java
│   │           │   │   ├── FileScanner.java
│   │           │   │   └── PipelineManager.java
│   │           │   ├── tools/           # 工具类
│   │           │   │   ├── display/     # UI显示工具
│   │           │   │   │   ├── styles/  # 样式管理
│   │           │   │   │   ├── StyleFactory.java
│   │           │   │   │   ├── ComponentFactory.java
│   │           │   │   │   └── ...
│   │           │   │   ├── AdvancedFileTypeManager.java
│   │           │   │   ├── ConfigFileManager.java
│   │           │   │   └── MultiThreadTaskEstimator.java
│   │           │   ├── ui/              # 用户界面
│   │           │   │   ├── AppearanceManager.java
│   │           │   │   ├── ComposeView.java
│   │           │   │   ├── GlobalSettingsView.java
│   │           │   │   ├── LogView.java
│   │           │   │   └── PreviewView.java
│   │           │   └── versions/        # 版本管理
│   │           │       ├── FileManagerAppV14_Stable.java
│   │           │       ├── FileManagerAppV15.java
│   │           │       ├── FileManagerAppV16.java
│   │           │       ├── FileManagerAppV17.java
│   │           │       └── FileManagerAppV20_Stable.java
│   │           ├── model/               # 数据模型
│   │           │   ├── ChangeRecord.java
│   │           │   ├── CleanupParams.java
│   │           │   ├── CueSheet.java
│   │           │   ├── FileStatisticInfo.java
│   │           │   ├── MusicInfo.java
│   │           │   ├── RuleCondition.java
│   │           │   └── RuleConditionGroup.java
│   │           ├── rule/                # 规则定义
│   │           ├── strategy/            # 处理策略
│   │           │   ├── base/            # 策略基础组件
│   │           │   ├── cleanup/         # 清理策略
│   │           │   ├── collection/      # 归类策略
│   │           │   │   ├── FileClusteringAlgorithm.java
│   │           │   │   ├── FilenameNormalizer.java
│   │           │   │   ├── CollectionNamingStrategy.java
│   │           │   │   ├── TextSimilarityCalculator.java
│   │           │   │   └── ...
│   │           │   ├── duplicate/       # 重复文件策略
│   │           │   ├── ncm/            # 网易云音乐策略
│   │           │   │   ├── model/       # NCM数据模型
│   │           │   │   ├── tool/        # NCM工具
│   │           │   │   ├── NcmBaseStrategy.java
│   │           │   │   ├── NcmConvertStrategy.java
│   │           │   │   ├── NcmCacheTransStrategy.java
│   │           │   │   └── NcmLyricDownloadStrategy.java
│   │           │   ├── rename/          # 重命名策略
│   │           │   ├── scraper/         # 元数据刮削策略
│   │           │   │   ├── cache/       # 缓存管理
│   │           │   │   ├── config/      # 配置管理
│   │           │   │   ├── model/       # 数据模型
│   │           │   │   ├── processor/   # 处理器
│   │           │   │   ├── source/      # 数据源
│   │           │   │   │   ├── impl/   # 数据源实现
│   │           │   │   │   │   ├── ITunesSource.java
│   │           │   │   │   │   ├── NeteaseMusicSource.java
│   │           │   │   │   │   ├── MiguMusicSource.java
│   │           │   │   │   │   ├── MusicBrainzSource.java
│   │           │   │   │   │   ├── LastFmSource.java
│   │           │   │   │   │   ├── DiscogsSource.java
│   │           │   │   │   │   └── LocalInferenceSource.java
│   │           │   │   └── ui/          # UI组件
│   │           │   ├── AbstractFfmpegStrategy.java
│   │           │   ├── AdvancedRenameStrategy.java
│   │           │   ├── AlbumDirNormalizeStrategy.java
│   │           │   ├── AudioConverterStrategy.java
│   │           │   ├── CueFileRenameStrategy.java
│   │           │   ├── CueSplitterStrategy.java
│   │           │   ├── FileCleanupStrategy.java
│   │           │   ├── FileCollectionStrategy.java
│   │           │   ├── FileMigrateStrategy.java
│   │           │   ├── FileTypeFixStrategy.java
│   │           │   ├── FileUnzipStrategy.java
│   │           │   ├── MetadataScraperStrategy.java
│   │           │   └── NcmIntegratedStrategy.java
│   │           ├── tool/                # 工具集合
│   │           │   ├── backup/         # 文件处理工具
│   │           │   ├── file/           # 文件操作工具
│   │           │   ├── log/            # 日志工具
│   │           │   ├── unzip/          # 解压工具
│   │           │   ├── ThreadPoolManager.java
│   │           │   └── RetryableThreadPool.java
│   │           ├── type/                # 类型定义
│   │           │   ├── ActionType.java
│   │           │   ├── ConditionType.java
│   │           │   ├── ExecStatus.java
│   │           │   ├── OperationType.java
│   │           │   ├── ScanTarget.java
│   │           │   └── TaskStatus.java
│   │           └── util/                # 通用工具
│   │               ├── file/           # 文件工具
│   │               ├── MetadataHelper.java
│   │               ├── StringUtils.java
│   │               ├── FileUtil.java
│   │               └── ...
│   └── resources/                      # 资源文件
│       ├── jdk17/                      # JDK17环境配置
│       ├── jdk8/                       # JDK8环境配置
│       └── style.css                   # 样式文件
├── design/                            # 设计文档
│   ├── doc/                           # 策略类文档
│   ├── review/                        # 迭代总结
│   ├── skill/                         # 实践技巧
│   ├── problem/                       # 问题记录
│   └── command/                       # 命令说明
├── metadata-test/                      # 元数据测试项目
├── .gitignore                         # Git忽略文件
├── build_dist.bat                      # 构建脚本
├── pom.xml                            # Maven配置
└── readme.md                          # 项目说明
```

## 技术架构

### 核心设计模式
- **策略模式**：所有文件处理操作都实现为策略，支持灵活组合
- **工厂模式**：策略工厂负责创建和管理策略实例
- **观察者模式**：UI组件监听数据变化并实时更新
- **建造者模式**：复杂对象的创建使用建造者模式

### 技术栈
- **Java**: 核心开发语言
- **JavaFX**: 图形用户界面
- **Maven**: 项目构建和依赖管理
- **jaudiotagger**: 音频元数据处理
- **ffmpeg**: 音频格式转换
- **FastJSON**: JSON序列化

### 关键特性
- **多线程处理**：支持并发处理，提高处理效率
- **缓存机制**：元数据缓存，减少网络请求
- **配置持久化**：支持配置保存和加载
- **实时预览**：所有操作支持预览，避免误操作
- **日志记录**：详细的操作日志，便于问题排查

## 部署与使用

### 运行方式
直接将 `dist` 文件夹压缩成 ZIP 发送给用户。用户解压后，双击 `EchoMusicManager.bat` 即可运行，无需安装任何 Java 环境。

### 目录结构
```
dist/
├── EchoMusicManager.bat  (双击运行)
├── ffmpeg.exe            (内置工具)
├── bin/
│   └── EchoMusicManager.jar (核心程序)
└── jre/                  (内置Java环境)
    └── bin/
    └── lib/
```

### 开发环境
- **JDK**: JDK 8 或 JDK 17
- **构建工具**: Maven 3.6+
- **IDE**: IntelliJ IDEA 或 Eclipse

### 构建项目
```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 打包项目
mvn clean package

# 构建发布包
build_dist.bat
```

## 文档

### 设计文档
- [策略总览](design/doc/strategy-overview.md)
- [IAppStrategy接口设计](design/doc/iappstrategy-interface-design.md)
- [UI系统设计](design/doc/ui-overview.md)

### 策略文档
- [AudioConverterStrategy](design/doc/AudioConverterStrategy.md)
- [MetadataScraperStrategy](design/doc/MetadataScraperStrategy.md)
- [FileCollectionStrategy](design/doc/FileCollectionStrategy.md)
- [AdvancedRenameStrategy](design/doc/AdvancedRenameStrategy.md)
- [CueSplitterStrategy](design/doc/CueSplitterStrategy.md)
- [FileCleanupStrategy](design/doc/FileCleanupStrategy.md)
- [NcmConvertStrategy](design/doc/NcmConvertStrategy.md)

### 技巧文档
- [迭代文档维护指南](design/skill/iteration-documentation-maintenance-guide.md)
- [元数据提取算法优化技巧](design/skill/metadata-extraction-optimization-skill.md)
- [策略扩展开发指南](design/skill/strategy-extension-skill.md)
- [合集自动化测试开发指南](design/skill/readme-collection-develop.md)

### 迭代总结
- [元数据提取迭代总结](design/review/metadata-extraction-iteration-review.md)
- [合集命名策略迭代总结](design/review/collection-naming-strategy-iteration-review.md)
- [策略重构迭代总结](design/review/strategy-refactoring-iteration-review.md)

### 问题记录
- [设计问题记录](design/problem/design-problems.md)
- [实现问题记录](design/problem/implementation-problems.md)

### 命令说明
- [Windows命令使用说明](design/command/windows-commands.md)

## 免责声明
该软件不需要付费，也不承担额外的后果，不能保证没有bug，大家对自己的文件操作需要谨慎~

## 公测与开源
目前软件处于 **公测开源** 阶段，非常欢迎大家试用并提出宝贵意见！

## 联系方式
群里私聊或发邮件至hrcao97@163.com，平时上班忙，有空会回。

## 贡献指南
欢迎贡献代码、报告问题或提出建议。请遵循以下步骤：
1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证
本项目采用 GPLv3 + 非商用限制 协议。详见 [LICENSE](license.txt) 文件。

---

**项目版本**: 2.0  
**最后更新**: 2026-01-30  
**维护者**: hrcao
