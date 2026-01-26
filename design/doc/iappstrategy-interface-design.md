# IAppStrategy 接口设计文档

## 概述

**接口**: `com.filemanager.app.base.IAppStrategy`

**设计模式**: 策略模式 + 模板方法模式

**核心职责**: 定义文件处理策略的统一接口，支持流水线式文件处理

## 接口定义

### 核心抽象方法

```java
public abstract class IAppStrategy implements IConfigComponent {
    
    // 1. 策略标识
    public abstract String getName();
    public abstract String getDescription();
    
    // 2. UI配置
    public abstract Node getConfigNode();
    
    // 3. 核心处理逻辑
    public abstract List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                                List<ChangeRecord> inputRecords, 
                                                List<File> rootDirs);
    
    public abstract void execute(ChangeRecord rec) throws Exception;
    
    // 4. 类型定义
    public abstract ScanTarget getTargetType();
}
```

## 核心方法详解

### 1. analyze() 方法

**方法签名**:
```java
public abstract List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                           List<ChangeRecord> inputRecords, 
                                           List<File> rootDirs)
```

**参数说明**:
- `currentRecord`: 当前正在处理的文件/文件夹记录
- `inputRecords`: 扫描范围内的全量文件记录列表
- `rootDirs`: 根目录列表

**返回值**:
- 返回新增的ChangeRecord列表
- 如果只是对已有文件进行操作，返回空列表即可

**处理流程**:
1. 分析当前文件是否需要处理
2. 如果需要处理，修改currentRecord的属性（newName、newPath、opType等）
3. 如果需要生成新文件，创建新的ChangeRecord并返回
4. 如果不需要处理，返回空列表

**使用场景**:

**场景1: 文件重命名**（TrackNumberStrategy）
```java
@Override
public List<ChangeRecord> analyze(ChangeRecord change, List<ChangeRecord> inputRecords, List<File> rootDirs) {
    // 1. 分析目录下的文件
    List<ChangeRecord> group = getFilesUnderDir(f, inputRecords).stream()
            .filter(rec -> FileTypeUtil.isMusicFile(rec.getFileHandle()))
            .collect(Collectors.toList());
    
    // 2. 修改现有记录的属性
    for (int i = 0; i < group.size(); i++) {
        ChangeRecord rec = group.get(i);
        String newName = num + pSeparator + baseName + ext;
        File target = new File(vFile.getParent(), newName);
        
        // 修改记录属性
        rec.setNewName(newName);
        rec.setNewPath(target.getAbsolutePath());
        rec.setChanged(true);
        rec.setOpType(OperationType.RENAME);
    }
    
    // 3. 返回空列表（不生成新文件）
    return Collections.emptyList();
}
```

**场景2: 文件归类**（FileCollectionStrategy）
```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
    // 1. 分析文件是否需要归类
    if (shouldAddToExistingCollection(record.getFileHandle(), collectionDir, record)) {
        // 2. 修改现有记录的属性
        record.setChanged(true);
        record.setNewPath(collectionDir.toPath().resolve(record.getFileHandle().getName()).toString());
        record.setOpType(OperationType.COLLECT);
        record.setStatus(ExecStatus.PENDING);
        
        // 3. 添加到结果列表
        changeRecords.add(record);
    }
    
    return changeRecords;
}
```

**场景3: 文件转换**（AudioConverterStrategy）
```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
    // 1. 分析文件是否需要转换
    if (needsConversion(currentRecord.getFileHandle())) {
        // 2. 修改现有记录的属性
        String targetPath = getOutputPath(currentRecord.getFileHandle());
        currentRecord.setNewPath(targetPath);
        currentRecord.setChanged(true);
        currentRecord.setOpType(OperationType.CONVERT);
        
        // 3. 添加额外参数
        currentRecord.getExtraParams().put("overwrite", "true");
        currentRecord.getExtraParams().put("audioFormat", "mp3");
    }
    
    return Collections.emptyList();
}
```

**场景4: 生成新文件**（NcmCacheTransStrategy）
```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs) {
    List<ChangeRecord> result = new ArrayList<>();
    
    // 1. 分析缓存文件
    for (File cacheFile : cacheFiles) {
        // 2. 创建新的ChangeRecord（生成新文件）
        String targetPath = targetDir + File.separator + displayName;
        ChangeRecord record = new ChangeRecord(
            cacheFile.getName(),
            displayName,
            cacheFile,
            true,
            targetPath,
            OperationType.NCM_CACHE_SCAN
        );
        
        // 3. 添加额外参数
        record.getExtraParams().put("audioFormat", cacheInfo.getAudioFormat());
        record.getExtraParams().put("songName", cacheInfo.getSongName());
        record.getExtraParams().put("artistName", cacheInfo.getArtistName());
        record.getExtraParams().put("songId", songId);
        
        result.add(record);
    }
    
    return result;
}
```

### 2. execute() 方法

**方法签名**:
```java
public abstract void execute(ChangeRecord rec) throws Exception
```

**参数说明**:
- `rec`: 要执行的变更记录

**处理流程**:
1. 获取源文件路径（rec.getFileHandle() 或 rec.getCurrentSource()）
2. 获取目标文件路径（rec.getNewPath()）
3. 获取额外参数（rec.getExtraParams()）
4. 执行文件操作
5. 更新执行状态（rec.setStatus()）

**使用场景**:

**场景1: 文件重命名**（TrackNumberStrategy）
```java
@Override
public void execute(ChangeRecord rec) throws Exception {
    if (rec.getOpType() != OperationType.RENAME) return;
    
    File s = rec.getFileHandle();
    File t = new File(rec.getNewPath());
    
    if (s.equals(t)) return;
    if (!t.getParentFile().exists()) t.getParentFile().mkdirs();
    
    // 执行重命名
    Files.move(s.toPath(), t.toPath(), StandardCopyOption.REPLACE_EXISTING);
}
```

**场景2: 文件转换**（AudioConverterStrategy）
```java
@Override
public void execute(ChangeRecord rec) throws Exception {
    File source = rec.getFileHandle();
    File target = new File(rec.getNewPath());
    
    // 检查是否覆盖
    if (!Boolean.parseBoolean(rec.getExtraParams().get("overwrite")) && target.exists()) {
        return;
    }
    
    // 创建目标目录
    if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
    
    // 执行转换
    convertAudioFile(source, target, rec.getExtraParams());
}
```

**场景3: 缓存文件转换**（NcmCacheTransStrategy）
```java
@Override
public void execute(ChangeRecord rec) throws Exception {
    File cacheFile = rec.getFileHandle();
    
    // 从ChangeRecord中获取缓存文件信息
    Map<String, String> extraParams = rec.getExtraParams();
    String audioFormat = extraParams.get("audioFormat");
    String songName = extraParams.get("songName");
    String artistName = extraParams.get("artistName");
    
    // 直接使用ChangeRecord中的目标文件路径
    File targetFile = new File(rec.getNewPath());
    
    // 执行缓存转换
    executeCacheScan(cacheFile, rec);
}
```

### 3. getTargetType() 方法

**方法签名**:
```java
public abstract ScanTarget getTargetType()
```

**返回值**:
- `ScanTarget.FILES_ONLY`: 只处理文件
- `ScanTarget.FOLDERS_ONLY`: 只处理文件夹
- `ScanTarget.ALL`: 处理文件和文件夹

**使用场景**:

```java
@Override
public ScanTarget getTargetType() {
    return ScanTarget.FILES_ONLY; // 只处理音频文件
}
```

```java
@Override
public ScanTarget getTargetType() {
    return ScanTarget.FOLDERS_ONLY; // 只处理文件夹
}
```

```java
@Override
public ScanTarget getTargetType() {
    return ScanTarget.ALL; // 处理文件和文件夹
}
```

### 4. getConfigNode() 方法

**方法签名**:
```java
public abstract Node getConfigNode()
```

**返回值**:
- 返回策略的配置UI节点

**使用场景**:

```java
@Override
public Node getConfigNode() {
    VBox configPane = new VBox(10);
    configPane.getChildren().addAll(
        createModeSelector(),
        createSeparatorField(),
        createPadZeroCheckbox()
    );
    return configPane;
}
```

### 5. captureParams() 方法

**方法签名**:
```java
public void captureParams()
```

**功能**:
- 从UI组件中捕获参数
- 保存到策略的运行时变量中

**使用场景**:

```java
@Override
public void captureParams() {
    pMode = cbMode.getValue();
    pPadZero = chkPadZero.isSelected();
    pSeparator = txtSeparator.getText();
}
```

## ChangeRecord 数据结构

### 核心属性

```java
public class ChangeRecord {
    // 基本信息
    private String originalName;        // 原始文件名
    private String newName;              // 新文件名
    private File fileHandle;             // 原始文件句柄
    private String newPath;              // 新文件路径
    private OperationType opType;         // 操作类型
    private Map<String, String> extraParams; // 额外参数
    
    // 状态信息
    private boolean changed;              // 是否已变更
    private ExecStatus status;           // 执行状态
    private String failReason;           // 失败原因
    
    // 特殊标记
    private boolean isCreate;             // 是否创建新文件
    private boolean isDeleteOrMove;      // 是否删除或移动
    private boolean selected;             // 是否选中
    private long id;                      // 唯一标识符
    
    // 链式处理
    private File intermediateFile;       // 中间状态文件
}
```

### 数据协议

#### 1. 基本字段协议

| 字段 | 类型 | 说明 | 必填 | 示例 |
|------|------|------|------|------|
| originalName | String | 原始文件名 | 是 | "song.flac" |
| newName | String | 新文件名 | 否 | "01. song.flac" |
| fileHandle | File | 原始文件句柄 | 是 | new File("/path/to/song.flac") |
| newPath | String | 新文件路径 | 否 | "/path/to/01. song.flac" |
| opType | OperationType | 操作类型 | 否 | OperationType.RENAME |
| changed | boolean | 是否已变更 | 否 | true |
| status | ExecStatus | 执行状态 | 否 | ExecStatus.PENDING |

#### 2. extraParams 协议

**通用参数**:
```java
// 覆盖标志
record.getExtraParams().put("overwrite", "true");

// 操作类型
record.getExtraParams().put("action", "copy");

// 来源说明
record.getExtraParams().put("来源", "文件类型修复");
```

**音频转换参数**:
```java
// 音频格式
record.getExtraParams().put("audioFormat", "mp3");

// 比特率
record.getExtraParams().put("bitrate", "320k");

// 采样率
record.getExtraParams().put("sampleRate", "44100");
```

**NCM缓存参数**:
```java
// 音频格式
record.getExtraParams().put("audioFormat", "flac");

// 歌曲名称
record.getExtraParams().put("songName", "歌曲名");

// 歌手名称
record.getExtraParams().put("artistName", "歌手名");

// 歌曲ID
record.getExtraParams().put("songId", "123456");
```

**文件归类参数**:
```java
// 合并策略
record.getExtraParams().put("merge_strategy", "创建新合集");

// 合集名称
record.getExtraParams().put("collection_name", "滚石唱片");

// 相似度
record.getExtraParams().put("similarity_to_collection", "0.850");
```

#### 3. 状态流转协议

```
PENDING → RUNNING → SUCCESS
                     ↓
                   FAILED
                     ↓
               SKIPPED
```

### 使用场景

#### 场景1: 文件重命名

```java
// 创建记录
ChangeRecord rec = new ChangeRecord(
    "song.flac",           // originalName
    "01. song.flac",        // newName
    file,                   // fileHandle
    true,                   // changed
    "/path/to/01. song.flac", // newPath
    OperationType.RENAME    // opType
);

// 设置状态
rec.setStatus(ExecStatus.PENDING);
```

#### 场景2: 文件转换

```java
// 创建记录
ChangeRecord rec = new ChangeRecord(
    "song.flac",           // originalName
    "song.mp3",            // newName
    file,                  // fileHandle
    true,                  // changed
    "/path/to/song.mp3",   // newPath
    OperationType.CONVERT  // opType
);

// 添加额外参数
rec.getExtraParams().put("audioFormat", "mp3");
rec.getExtraParams().put("bitrate", "320k");
rec.getExtraParams().put("overwrite", "true");

// 设置状态
rec.setStatus(ExecStatus.PENDING);
```

#### 场景3: 文件归类

```java
// 修改现有记录
record.setChanged(true);
record.setNewPath(collectionDir.toPath().resolve(record.getFileHandle().getName()).toString());
record.setOpType(OperationType.COLLECT);
record.setStatus(ExecStatus.PENDING);

// 添加额外参数
record.getExtraParams().put("merge_strategy", "创建新合集");
record.getExtraParams().put("collection_name", "滚石唱片");
record.getExtraParams().put("similarity_to_collection", "0.850");
```

#### 场景4: 生成新文件

```java
// 创建新文件记录
ChangeRecord record = new ChangeRecord(
    cacheFile.getName(),   // originalName
    displayName,          // newName
    cacheFile,             // fileHandle
    true,                  // changed
    targetPath,            // newPath
    OperationType.NCM_CACHE_SCAN // opType
);

// 添加额外参数
record.getExtraParams().put("audioFormat", "flac");
record.getExtraParams().put("songName", "歌曲名");
record.getExtraParams().put("artistName", "歌手名");
record.getExtraParams().put("songId", "123456");

// 设置状态
record.setStatus(ExecStatus.PENDING);
```

## 枚举类设计

### 1. OperationType

**位置**: `com.filemanager.type.OperationType`

**枚举值**:
```java
public enum OperationType {
    NONE("无", "未指定任何操作"),
    RENAME("重命名", "对文件进行简单的重命名操作"),
    ALBUM_RENAME("专辑重命名", "根据元数据对整个专辑文件夹进行重命名"),
    CUE_RENAME("CUE重命名", "修改CUE索引文件中的文件名"),
    MOVE("移动", "将文件移动到新的目录结构中"),
    COLLECT("归类", "基于文件名相似度将文件/文件夹归类到合集文件夹中"),
    CONVERT("转换", "转换文件编码或多媒体格式（如FLAC转MP3）"),
    SCRAPER("刮削", "从互联网获取并更新文件的元数据信息"),
    SPLIT("分割", "将整轨文件（如APE/FLAC）按CUE索引切分为单曲"),
    DELETE("删除", "将文件从磁盘中永久删除"),
    UNZIP("解压", "对压缩包文件进行解解压操作"),
    FIX_TYPE("修复类型", "修复文件的真正类型"),
    NCM_CONVERT("NCM转换", "将网易云.ncm格式文件转换为常规音频文件"),
    NCM_CACHE_SCAN("NCM缓存扫描", "自动扫描网易云音乐缓存文件并转换为正常音频格式"),
    NCM_LYRIC_DOWNLOAD("NCM歌词下载", "根据音频文件信息下载网易云平台对应歌词");
    
    public final String name;
    public final String desc;
}
```

**维护规则**:
1. 新增操作类型时，添加到枚举末尾
2. 提供清晰的中文名称和描述
3. 在AppStrategyFactory中添加对应的策略匹配逻辑
4. 在PreviewView中添加对应的显示逻辑

**扩展示例**:
```java
// 1. 添加枚举值
NEW_OPERATION("新操作", "新操作的描述"),

// 2. 在AppStrategyFactory中添加匹配
if (op == OperationType.NEW_OPERATION && s instanceof NewOperationStrategy) return s;
```

### 2. ExecStatus

**位置**: `com.filemanager.type.ExecStatus`

**枚举值**:
```java
public enum ExecStatus {
    PENDING,        // 待执行
    RUNNING,        // 执行中
    SUCCESS,        // 执行成功
    FAILED,         // 执行失败
    ANALYZE_FAILED, // 分析失败
    SKIPPED         // 已跳过
}
```

**维护规则**:
1. 保持状态流转的清晰性
2. 新增状态时考虑UI显示逻辑
3. 确保状态流转的合理性

### 3. ScanTarget

**位置**: `com.filemanager.type.ScanTarget`

**枚举值**:
```java
public enum ScanTarget {
    FILES_ONLY,     // 只处理文件
    FOLDERS_ONLY,   // 只处理文件夹
    ALL             // 处理文件和文件夹
}
```

**维护规则**:
1. 保持枚举值的简洁性
2. 新增类型时考虑analyzeWithPreCheck方法的逻辑

### 4. RenameActionType

**位置**: `com.filemanager.strategy.rename.RenameActionType`

**枚举值**:
```java
public enum RenameActionType {
    REPLACE_TEXT("文本替换"),
    REPLACE_REGEX("正则替换"),
    PREPEND("前缀添加"),
    APPEND("后缀添加"),
    TO_LOWER("转小写"),
    TO_UPPER("转大写"),
    TRIM("去前后空格"),
    ADD_LETTER_PREFIX("首字母前缀"),
    CLEAN_NOISE("智能清理"),
    BATCH_REMOVE("批量移除"),
    CUT_PREFIX("截取前N位"),
    CUT_SUFFIX("截取后N位"),
    KEEP_PREFIX("保留前N位"),
    KEEP_SUFFIX("保留后N位"),
    REMOVE_PREFIX("移除前缀"),
    REMOVE_SUFFIX("移除后缀"),
    TRADITIONAL_TO_SIMPLIFIED("繁体转简体");
}
```

**维护规则**:
1. 新增操作类型时，添加到枚举末尾
2. 提供清晰的中文描述
3. 在RenameRule中添加对应的处理逻辑

## 参数命名风格

### 1. 运行时参数命名

**命名规范**:
- 使用 `p` 前缀表示运行时参数
- 使用驼峰命名法
- 布尔值使用 `is` 或 `has` 前缀

**示例**:
```java
// 字符串参数
private String pMode;
private String pSeparator;
private String pDestDir;

// 布尔参数
private boolean pPadZero;
private boolean pClean;
private boolean pForce;

// 数值参数
private int pProcessScopeIndex;
private double pThreshold;
```

### 2. UI组件命名

**命名规范**:
- 使用组件类型缩写作为前缀
- 使用驼峰命名法
- 描述性命名

**示例**:
```java
// ComboBox
private JFXComboBox<String> cbMode;
private JFXComboBox<String> cbFunction;
private JFXComboBox<String> cbCrossDriveMode;

// TextField
private TextField txtSeparator;
private TextField txtDestDir;
private TextField txtPathPattern;

// CheckBox
private CheckBox chkPadZero;
private CheckBox chkCleanEmpty;
private CheckBox chkSelectAll;

// Button
private JFXButton btnAddRule;
private JFXButton btnRemoveRule;
private JFXButton btnMoveUp;
private JFXButton btnMoveDown;

// ListView
private ListView<RenameRule> lvRules;
```

### 3. 方法命名

**命名规范**:
- 使用动词开头
- 使用驼峰命名法
- 描述性命名

**示例**:
```java
// 分析方法
public List<ChangeRecord> analyze(...)
public List<ChangeRecord> analyzeWithPreCheck(...)

// 执行方法
public void execute(ChangeRecord rec) throws Exception

// 配置方法
public Node getConfigNode()
public void captureParams()
public void saveConfig(Properties props)
public void loadConfig(Properties props)

// 工具方法
protected void log(String msg)
protected void logError(String msg)
protected void invalidatePreview()
protected boolean checkConditions(ChangeRecord rec)
```

### 4. extraParams 键命名

**命名规范**:
- 使用小写字母和下划线
- 描述性命名
- 统一格式

**示例**:
```java
// 通用参数
"overwrite"
"action"
"来源"

// 音频参数
"audioFormat"
"bitrate"
"sampleRate"

// NCM参数
"songName"
"artistName"
"songId"

// 归类参数
"merge_strategy"
"collection_name"
"similarity_to_collection"
```

## 策略实现规范

### 1. 基本结构

```java
public class MyStrategy extends IAppStrategy {
    
    // UI组件
    private final JFXComboBox<String> cbMode;
    private final TextField txtParam;
    
    // 运行时参数
    private String pMode;
    private String pParam;
    
    public MyStrategy() {
        // 初始化UI组件
        cbMode = new JFXComboBox<>();
        txtParam = new TextField();
    }
    
    @Override
    public String getName() {
        return "策略名称";
    }
    
    @Override
    public String getDescription() {
        return "策略描述";
    }
    
    @Override
    public Node getConfigNode() {
        VBox configPane = new VBox(10);
        configPane.getChildren().addAll(cbMode, txtParam);
        return configPane;
    }
    
    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }
    
    @Override
    public void captureParams() {
        pMode = cbMode.getValue();
        pParam = txtParam.getText();
    }
    
    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                       List<ChangeRecord> inputRecords, 
                                       List<File> rootDirs) {
        // 分析逻辑
        return Collections.emptyList();
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        // 执行逻辑
    }
}
```

### 2. 注册策略

在 `AppStrategyFactory` 中注册新策略:

```java
public static List<IAppStrategy> getAppStrategies() {
    List<IAppStrategy> strategyPrototypes = new ArrayList<IAppStrategy>();
    strategyPrototypes.add(new MyStrategy());
    return strategyPrototypes;
}
```

### 3. 匹配策略

在 `AppStrategyFactory` 中添加策略匹配逻辑:

```java
public static IAppStrategy findStrategyForOp(OperationType op, List<IAppStrategy> pipelineStrategies) {
    for (int i = pipelineStrategies.size() - 1; i >= 0; i--) {
        IAppStrategy s = pipelineStrategies.get(i);
        if (op == OperationType.MY_OPERATION && s instanceof MyStrategy) return s;
    }
    return null;
}
```

## 最佳实践

### 1. analyze() 方法

**推荐做法**:
- 优先修改现有记录，而不是创建新记录
- 只在需要生成新文件时才返回非空列表
- 使用 `getTargetFile()` 和 `getFilesUnderDir()` 辅助方法
- 合理使用 `extraParams` 传递额外信息

**避免做法**:
- 在analyze()中执行文件操作
- 返回大量不必要的ChangeRecord
- 忽略前置条件检查

### 2. execute() 方法

**推荐做法**:
- 检查源文件和目标文件是否相同
- 创建目标目录
- 处理异常并更新状态
- 使用 `getCurrentSource()` 获取实际源文件

**避免做法**:
- 忽略异常处理
- 不检查文件是否存在
- 不更新执行状态

### 3. 参数管理

**推荐做法**:
- 使用 `p` 前缀表示运行时参数
- 在 `captureParams()` 中捕获参数
- 在 `saveConfig()` 和 `loadConfig()` 中持久化参数
- 使用 `extraParams` 传递策略特定参数

**避免做法**:
- 直接在analyze()中读取UI组件
- 不持久化参数
- 混用不同类型的参数

### 4. 日志记录

**推荐做法**:
- 使用 `log()` 记录正常信息
- 使用 `logError()` 记录错误信息
- 记录关键操作和状态变化
- 提供有意义的日志信息

**避免做法**:
- 记录过多调试信息
- 不记录错误信息
- 记录敏感信息

## 总结

IAppStrategy接口提供了统一的策略处理框架，通过ChangeRecord传递文件操作信息，支持流水线式文件处理。开发新策略时，需要：

1. 继承IAppStrategy类
2. 实现核心抽象方法
3. 遵循命名规范
4. 正确使用ChangeRecord
5. 合理管理参数
6. 记录日志信息
7. 注册策略到工厂

通过遵循本文档的规范，可以确保策略的一致性、可维护性和可扩展性。

---

**相关文档**:
- [strategy-extension-skill.md](../skill/strategy-extension-skill.md)
- [strategy-overview.md](strategy-overview.md)

**文档版本**: 1.0  
**最后更新**: 2026-01-27  
**维护者**: FileEditTools Team
