# 策略实现文档

## 概述

本文档详细描述了FileManager Plus系统中的策略实现，包括策略架构、已实现的策略列表、策略配置和使用方法。系统采用统一的插件-策略架构，策略类实现了IPlugin接口，同时具备参数配置和功能执行能力。

## 策略架构

### 核心接口

#### IPlugin接口

所有策略必须实现`IPlugin`接口，该接口定义了策略的基本行为：

```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    List<PluginParameterDTO> getParameters();
    PluginConfigDTO getDefaultConfig();
    List<PreconditionGroupDTO> getDefaultPreconditionGroups();
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
}
```

#### StrategyConfigurable接口

策略配置接口继承自IPlugin，定义了策略配置方法：

```java
public interface StrategyConfigurable extends IPlugin {
    List<ConfigFieldDTO> getConfigFields();
    StrategyConfigDTO initializeDefaultConfig();
    boolean validateConfig(StrategyConfigDTO config);
    <T> T getConfigValue(StrategyConfigDTO config, String key, T defaultValue);
    void setConfigValue(StrategyConfigDTO config, String key, Object value);
}
```

#### AbstractConfigurableStrategy

策略抽象基类实现了IPlugin接口，提供了策略实现的模板：

```java
public abstract class AbstractConfigurableStrategy implements IPlugin {
    protected List<ConfigFieldDTO> configFields;
    
    public AbstractConfigurableStrategy();
    protected abstract void initConfigFields();
    protected abstract void initDefaultConfigValues(StrategyConfigDTO config);
    protected abstract ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context);
    protected abstract ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context);
    
    protected void addConfigField(String name, String label, String type, Object defaultValue, String description, boolean required);
    protected ChangeRecord createChangeRecord(String originalPath, String newPath, String status);
}
```

#### PluginRegistry

插件注册表负责管理所有已加载的策略：

```java
public class PluginRegistry {
    private static PluginRegistry instance;
    private final Map<String, IPlugin> plugins = new HashMap<>();
    private final PluginLoader pluginLoader = new PluginLoader();
    private String externalPluginDir;

    public static synchronized PluginRegistry getInstance();
    public void loadExternalPlugins(String pluginDirPath);
    public void reloadPlugins();
    public void reloadExternalPlugins();
    public List<String> scanExternalPluginDirectory(String pluginDirPath);
    public IPlugin getPlugin(String pluginId);
    public List<IPlugin> getAllPlugins();
    public List<IPlugin> getInternalPlugins();
    public List<IPlugin> getExternalPlugins();
    public void registerPlugin(IPlugin plugin);
    public void unregisterPlugin(String pluginId);
}
```

#### PluginLoader

插件加载器支持从外部JAR文件加载策略：

```java
public class PluginLoader {
    public List<IPlugin> loadPluginsFromDirectory(String pluginDirPath);
    public List<IPlugin> loadPluginFromJar(File jarFile);
    public boolean isPluginJar(File jarFile);
    public List<String> scanPluginDirectory(String pluginDirPath);
    public void unloadExternalPlugins();
    public List<IPlugin> getExternalPlugins();
    public void reloadExternalPlugins(String pluginDirPath);
}
```

#### ExecutionContext

执行上下文提供策略执行时的环境信息：

```java
public class ExecutionContext {
    private String taskId;
    private String userId;
    private Map<String, Object> contextData;

    public void setContextData(String key, Object value);
    public Object getContextData(String key);
}
```

### 策略配置

策略配置使用`StrategyConfigDTO`类来管理：

```java
public class StrategyConfigDTO {
    private Map<String, Object> configValues;
    private List<ConfigFieldDTO> configFields;
    private List<PreconditionGroupDTO> preconditionGroups;

    public void setValue(String key, Object value);
    public Object getValue(String key);
    public Object getValue(String key, Object defaultValue);
    public Map<String, Object> getConfigValues();
}
```

## 插件分类

### 内置插件

内置插件位于`plugins/file-operations`模块中，随主程序一起发布。

#### 1. 文件清理插件 (file-cleanup)

**插件ID**: `file-cleanup`

**描述**: 支持文件去重、文件夹去重、空目录清理、文件夹合并等多种清理模式

**默认配置**:
```json
{
  "cleanupMode": "dedup_files",
  "deleteMethod": "pseudo_delete",
  "trashPath": ".EchoTrash",
  "keepLargest": true,
  "keepEarliest": true,
  "keepExt": "wav",
  "preprocessLower": true,
  "preprocessUpper": false,
  "preprocessSimplified": false,
  "audioSpecial": true,
  "minFileSizeKB": 0,
  "maxFileSizeKB": 10240
}
```

**配置参数**:
- `cleanupMode`: 清理模式（dedup_files, dedup_folders, remove_empty_dirs, direct_cleanup, merge_same_name, merge_nested）
- `deleteMethod`: 删除方法（direct_delete, pseudo_delete）
- `trashPath`: 伪删除时的回收站路径
- `keepLargest`: 去重时保留最大的文件
- `keepEarliest`: 去重时保留最早的文件
- `keepExt`: 去重时保留的扩展名
- `preprocessLower`: 去重前将文件名转为小写
- `preprocessUpper`: 去重前将文件名转为大写
- `preprocessSimplified`: 去重前将文件名转为简体中文
- `audioSpecial`: 对音频文件进行特殊处理
- `minFileSizeKB`: 小于此大小的文件将被清理
- `maxFileSizeKB`: 大于此大小的文件将被清理

**使用示例**:
```http
POST /api/plugins/file-cleanup/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "cleanupMode": "dedup_files",
      "deleteMethod": "pseudo_delete",
      "keepLargest": true
    }
  }
}
```

#### 2. 文件收集插件 (file-collection)

**插件ID**: `file-collection`

**描述**: 根据配置规则收集和整理文件

**默认配置**:
```json
{
  "targetDirectory": "/tmp/collected",
  "recursive": true,
  "includePatterns": ["*.mp3", "*.wav", "*.flac"],
  "excludePatterns": ["*.tmp", "*.log"]
}
```

**配置参数**:
- `targetDirectory`: 目标目录路径
- `recursive`: 是否递归扫描子目录
- `includePatterns`: 包含的文件模式列表
- `excludePatterns`: 排除的文件模式列表

**使用示例**:
```http
POST /api/plugins/file-collection/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "targetDirectory": "/path/to/target",
      "recursive": true
    }
  }
}
```

#### 3. 文件重命名插件 (file-rename)

**插件ID**: `file-rename`

**描述**: 根据规则批量重命名文件

**默认配置**:
```json
{
  "pattern": "{name}_{index}",
  "startIndex": 1,
  "padZeros": true,
  "zeroPadding": 3,
  "preserveExtension": true,
  "overwriteExisting": false
}
```

**配置参数**:
- `pattern`: 重命名模式（支持{name}和{index}占位符）
- `startIndex`: 起始索引
- `padZeros`: 是否用零填充索引
- `zeroPadding`: 零填充位数
- `preserveExtension`: 是否保留文件扩展名
- `overwriteExisting`: 是否覆盖已存在的文件

**使用示例**:
```http
POST /api/plugins/file-rename/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.txt", "/path/to/file2.txt"],
  "config": {
    "values": {
      "pattern": "{name}_{index}",
      "startIndex": 1,
      "padZeros": true,
      "zeroPadding": 3,
      "preserveExtension": true
    }
  }
}
```

#### 4. 音频转换插件 (audio-converter)

**插件ID**: `audio-converter`

**描述**: 将音频文件转换为不同格式

**默认配置**:
```json
{
  "targetFormat": "mp3",
  "bitrate": "320k",
  "sampleRate": 44100,
  "channels": 2,
  "outputDirectory": "",
  "overwriteExisting": false
}
```

**配置参数**:
- `targetFormat`: 目标格式（mp3, wav, flac, aac等）
- `bitrate`: 比特率（如320k, 256k, 128k）
- `sampleRate`: 采样率（如44100, 48000）
- `channels`: 声道数（1=单声道, 2=立体声）
- `outputDirectory`: 输出目录（空表示原文件所在目录）
- `overwriteExisting`: 是否覆盖已存在的文件

**使用示例**:
```http
POST /api/plugins/audio-converter/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.wav", "/path/to/file2.flac"],
  "config": {
    "values": {
      "targetFormat": "mp3",
      "bitrate": "320k",
      "sampleRate": 44100,
      "channels": 2,
      "outputDirectory": "/path/to/output"
    }
  }
}
```

#### 5. 元数据抓取插件 (metadata-scraper)

**插件ID**: `metadata-scraper`

**描述**: 从网络或本地抓取并更新文件的元数据信息

**默认配置**:
```json
{
  "sources": ["discogs", "musicbrainz", "local"],
  "updateTags": true,
  "updateCoverArt": true,
  "forceUpdate": false
}
```

**配置参数**:
- `sources`: 数据源列表（discogs, musicbrainz, local）
- `updateTags`: 是否更新标签
- `updateCoverArt`: 是否更新封面艺术
- `forceUpdate`: 是否强制更新

**使用示例**:
```http
POST /api/plugins/metadata-scraper/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "sources": ["discogs", "musicbrainz"],
      "updateTags": true,
      "updateCoverArt": true
    }
  }
}
```

#### 6. CUE分轨插件 (cue-splitter)

**插件ID**: `cue-splitter`

**描述**: 解析.cue索引文件，将整轨音频无损切割为单曲。支持预览详细的歌曲清单与时长信息。只需要扫描cue文件。

**默认配置**:
```json
{
  "afterSplitAction": "do_nothing",
  "enableArchive": false,
  "archiveDir": "",
  "outputDirPrefix": "Split",
  "overwrite": false,
  "format": "%artist% - %album% - %track% - %title%",
  "autoFormatFilename": true,
  "useCacheDir": false,
  "cacheDir": "",
  "mirrorDir": ""
}
```

**配置参数**:
- `afterSplitAction`: 切分后操作（do_nothing, delete_original, archive_original）
- `enableArchive`: 是否启用归档目录
- `archiveDir`: 归档目录路径
- `outputDirPrefix`: 输出目录前缀
- `overwrite`: 是否覆盖已存在文件
- `format`: 文件名格式
- `autoFormatFilename`: 是否自动格式化文件名
- `useCacheDir`: 是否使用缓存目录
- `cacheDir`: 缓存目录路径
- `mirrorDir`: 镜像目录路径

#### 7. 专辑目录标准化插件 (album-dir-normalize)

**插件ID**: `album-dir-normalize`

**描述**: 智能规范化专辑目录名称，支持多种命名模板、元数据提取、特殊字符清理等功能。

**默认配置**:
```json
{
  "template": "%artist% - %year% - %album%",
  "customTemplate": "",
  "cleanSpecialChars": true,
  "removeYearPrefix": true,
  "useConsensusMetadata": true,
  "preserveOriginalName": false,
  "validateAlbumInfo": true
}
```

**配置参数**:
- `template`: 命名模板
- `customTemplate`: 自定义模板
- `cleanSpecialChars`: 是否清理特殊字符
- `removeYearPrefix`: 是否移除年份前缀
- `useConsensusMetadata`: 是否使用共识元数据
- `preserveOriginalName`: 是否保留原始目录名
- `validateAlbumInfo`: 是否验证专辑信息

#### 8. 文件类型修复插件 (file-type-fix)

**插件ID**: `file-type-fix`

**描述**: 一些网上下载的音频文件类型和实际类型不符，可以通过该工具智能进行修复。

**默认配置**:
```json
{
  "force": false
}
```

**配置参数**:
- `force`: 是否强制文件类型识别

#### 9. 文件解压插件 (file-unzip)

**插件ID**: `file-unzip`

**描述**: 批量智能解压文件，支持多种压缩格式、密码管理、智能目录等功能。

**默认配置**:
```json
{
  "engine": "java",
  "exePath": "",
  "outputMode": "same_dir",
  "customPath": "",
  "smartFolder": true,
  "mergeSameName": false,
  "deleteSource": false,
  "overwrite": false,
  "deleteOnFail": false,
  "nestedFolderMerge": false,
  "passwords": []
}
```

**配置参数**:
- `engine`: 解压引擎（java, 7zip, bandizip）
- `exePath`: 可执行文件路径
- `outputMode`: 输出模式（same_dir, custom_dir, parent_dir）
- `customPath`: 自定义路径
- `smartFolder`: 是否智能文件夹
- `mergeSameName`: 是否合并同名文件夹
- `deleteSource`: 是否解压后删除源文件
- `overwrite`: 是否覆盖已存在文件
- `deleteOnFail`: 是否解压失败后删除
- `nestedFolderMerge`: 是否嵌套文件夹合并
- `passwords`: 密码列表

#### 10. 文件迁移插件 (file-migrate)

**插件ID**: `file-migrate`

**描述**: 文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择。

**默认配置**:
```json
{
  "operationMode": "MOVE",
  "targetPath": "",
  "pathMode": "absolute",
  "scope": "all",
  "duplicateStrategy": "skip",
  "overwrite": false,
  "preserveStructure": true
}
```

**配置参数**:
- `operationMode`: 操作模式（MOVE, COPY）
- `targetPath`: 目标路径
- `pathMode`: 路径模式（absolute, relative, flat）
- `scope`: 生效范围（all, selected, matched）
- `duplicateStrategy`: 去重策略（skip, overwrite, rename, keep_both）
- `overwrite`: 是否覆盖已存在文件
- `preserveStructure`: 是否保留目录结构

#### 11. CUE文件重命名插件 (cue-file-rename)

**插件ID**: `cue-file-rename`

**描述**: 为了解决cue文件在部分软件下，由于中文命名导致的无法加载的问题，支持统一调整cue及对应的音频文件命名。

**默认配置**:
```json
{
  "mode": "auto",
  "fileName": "album",
  "overwrite": false
}
```

**配置参数**:
- `mode`: 修改模式（auto）
- `fileName`: 文件名前缀
- `overwrite`: 是否覆盖已存在文件

#### 12. 网易云音乐工具集插件 (ncm-integrated)

**插件ID**: `ncm-integrated`

**描述**: 网易云音乐工具集，包含NCM转换、缓存扫描、歌词下载等功能。

**默认配置**:
```json
{
  "function": "convert",
  "outputFormat": "mp3",
  "bitrate": "320k",
  "cacheDir": "",
  "outputDir": "",
  "downloadLyric": true,
  "lyricFormat": "lrc",
  "overwrite": false
}
```

**配置参数**:
- `function`: 功能选择（convert, cache_scan, lyric_download）
- `outputFormat`: 输出格式
- `bitrate`: 比特率
- `cacheDir`: 缓存目录
- `outputDir`: 输出目录
- `downloadLyric`: 是否下载歌词
- `lyricFormat`: 歌词格式
- `overwrite`: 是否覆盖已存在文件

#### 13. 高级重命名插件 (advanced-rename)

**插件ID**: `advanced-rename`

**描述**: 支持规则列表、正则表达式、元数据提取等多种重命名方式的高级重命名工具。

**默认配置**:
```json
{
  "crossDriveMode": "move",
  "processScope": "all",
  "rules": [],
  "caseSensitive": false,
  "useRegex": false,
  "preserveExtension": true,
  "overwrite": false
}
```

**配置参数**:
- `crossDriveMode`: 跨盘动作（move, copy）
- `processScope`: 处理范围（files_only, folders_only, all）
- `rules`: 重命名规则列表
- `caseSensitive`: 是否区分大小写
- `useRegex`: 是否使用正则表达式
- `preserveExtension`: 是否保留文件扩展名
- `overwrite`: 是否覆盖已存在文件

#### 14. 合集命名插件 (collection-naming)

**插件ID**: `collection-naming`

**描述**: 支持多种合集命名策略，包括简洁风格、精确风格、选取模板等。

**默认配置**:
```json
{
  "strategy": "concise",
  "removeYear": true,
  "removeFormat": true,
  "removeCDNumber": true,
  "removeDiscNumber": true,
  "removeVolNumber": true,
  "removeParentheses": false,
  "removeBrackets": false,
  "keepTemplate": false,
  "overwrite": false
}
```

**配置参数**:
- `strategy`: 命名策略（concise, precise, template）
- `removeYear`: 是否移除年份
- `removeFormat`: 是否移除格式
- `removeCDNumber`: 是否移除CD序号
- `removeDiscNumber`: 是否移除Disc序号
- `removeVolNumber`: 是否移除Vol序号
- `removeParentheses`: 是否移除括号内容
- `removeBrackets`: 是否移除方括号内容
- `keepTemplate`: 是否保留模板
- `overwrite`: 是否覆盖已存在文件

#### 15. 文件去重插件 (duplicate)

**插件ID**: `duplicate`

**描述**: 支持多种去重策略，包括保留最佳版本、添加序号、保留最早/最新文件等。

**默认配置**:
```json
{
  "strategy": "keep_best",
  "comparisonMethod": "md5",
  "caseInsensitive": true,
  "ignoreWhitespace": true,
  "ignoreSpecialChars": true,
  "keepLargest": true,
  "keepEarliest": true,
  "keepLatest": false,
  "addSequence": false,
  "sequenceFormat": "({index})",
  "moveToTrash": false,
  "trashPath": ".EchoTrash"
}
```

**配置参数**:
- `strategy`: 去重策略（keep_best, keep_largest, keep_earliest, keep_latest, add_sequence）
- `comparisonMethod`: 比较方法（md5, sha1, sha256, size, name）
- `caseInsensitive`: 是否忽略大小写
- `ignoreWhitespace`: 是否忽略空白字符
- `ignoreSpecialChars`: 是否忽略特殊字符
- `keepLargest`: 是否保留最大文件
- `keepEarliest`: 是否保留最早文件
- `keepLatest`: 是否保留最新文件
- `addSequence`: 是否添加序号
- `sequenceFormat`: 序号格式
- `moveToTrash`: 是否移动到回收站
- `trashPath`: 回收站路径

### 外部插件

外部插件是用户或第三方开发者开发的插件，可以动态加载到系统中。

#### 创建外部插件

1. 创建新的Maven项目
2. 添加对`plugin-base`和`domain`模块的依赖
3. 实现`IPlugin`接口
4. 创建`META-INF/services/com.filemanager.plugin.IPlugin`文件
5. 打包为JAR文件

#### 加载外部插件

1. 将插件JAR文件放入插件目录
2. 调用`/api/plugins/load-external` API
3. 插件自动加载并可用

## 策略开发指南

### 创建新策略

1. 创建新的策略类并继承AbstractConfigurableStrategy类：
```java
package com.filemanager.plugin.operations;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;

import java.util.ArrayList;
import java.util.List;

public class YourStrategy extends AbstractConfigurableStrategy {
    
    public YourStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "your-strategy-id";
    }

    @Override
    public String getName() {
        return "你的策略名称";
    }

    @Override
    public String getDescription() {
        return "策略描述";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addConfigField("key1", "配置项1", "string", "default-value1", 
            "配置项1的描述", false);
        addConfigField("key2", "配置项2", "boolean", true, 
            "配置项2的描述", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "key1", "default-value1");
        setConfigValue(config, "key2", true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String value1 = getConfigValue(config, "key1", "default-value1");
        boolean value2 = getConfigValue(config, "key2", false);
        
        return createChangeRecord(filePath, filePath, "PENDING");
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String value1 = getConfigValue(config, "key1", "default-value1");
        boolean value2 = getConfigValue(config, "key2", false);
        
        try {
            context.logInfo("Processing file: " + filePath);
            return createChangeRecord(filePath, filePath, "SUCCESS");
        } catch (Exception e) {
            context.logError("Error processing file: " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }
}
```

2. 在`backend/src/main/resources/META-INF/services/com.filemanager.plugin.IPlugin`文件中注册策略：
```
com.filemanager.plugin.operations.YourStrategy
```

## 策略管理API

### 获取所有策略

```http
GET /api/plugins
```

### 获取内置策略

```http
GET /api/plugins/internal
```

### 获取外部策略

```http
GET /api/plugins/external
```

### 获取策略信息

```http
GET /api/plugins/{strategyId}
```

### 获取策略配置

```http
GET /api/plugins/{strategyId}/config
```

### 更新策略配置
```http
POST /api/plugins/{strategyId}/config
Content-Type: application/json

{
  "values": {
    "key1": "value1",
    "key2": "value2"
  }
}
```

### 执行策略

```http
POST /api/plugins/{strategyId}/execute
Content-Type: application/json

{
  "files": ["/path/to/file1.mp3", "/path/to/file2.mp3"],
  "config": {
    "values": {
      "key1": "value1"
    }
  }
}
```

### 扫描外部策略目录

```http
POST /api/plugins/scan
Content-Type: application/json

{
  "pluginDir": "/path/to/plugins"
}
```

### 加载外部策略

```http
POST /api/plugins/load-external
Content-Type: application/json

{
  "pluginDir": "/path/to/plugins"
}
```

### 重载外部插件

```http
POST /api/plugins/reload-external
```

### 重载所有插件

```http
POST /api/plugins/reload
```

## 插件最佳实践

1. **配置验证**：在execute方法中验证配置参数的有效性
2. **错误处理**：使用try-catch块处理可能的异常
3. **日志记录**：使用适当的日志级别记录插件执行过程
4. **性能优化**：对于大量文件，考虑使用批处理或并行处理
5. **资源清理**：确保在插件执行完成后清理所有打开的资源
6. **文档完善**：为插件提供清晰的配置说明和使用示例

## 总结

FileManager Plus的插件系统提供了灵活的扩展机制，允许开发者轻松添加新的文件处理功能。通过实现IPlugin接口并遵循插件开发指南，可以创建功能强大的插件来满足各种文件管理需求。

系统支持内置插件和外部插件两种方式，内置插件随主程序发布，外部插件可以动态加载，提供了极大的灵活性和可扩展性。