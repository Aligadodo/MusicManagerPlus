# ChangeRecord模型与策略接口改造方案

## 一、现状分析

### 1.1 老架构ChangeRecord模型特点

**类型系统**：
- 使用枚举类型：`OperationType`, `ExecStatus`
- 类型安全，编译时检查

**构造函数**：
- 提供多种构造函数，支持不同场景
- 便捷构造函数：`ChangeRecord(String, String, File, boolean, String, OperationType)`
- 完整构造函数：`ChangeRecord(String, String, File, boolean, String, OperationType, Map<String, String>, ExecStatus)`

**链式处理**：
- 支持`intermediateFile`字段
- 提供`getCurrentSource()`方法获取当前源文件

**过程信息**：
- 支持`processInfo`列表
- 提供`addProcessInfo(String)`方法添加过程信息

**耗时统计**：
- 支持`analyzeTime`和`executeTime`字段

### 1.2 新架构ChangeRecord模型特点

**类型系统**：
- 使用字符串类型：`operationType`, `status`
- 缺少类型安全

**构造函数**：
- 只有无参构造函数
- 缺少便捷构造函数

**链式处理**：
- 支持`intermediateFile`字段
- 提供`getCurrentSource()`方法

**过程信息**：
- 支持`processInfo`列表
- 提供`addProcessInfo(String)`方法

**耗时统计**：
- 支持`analyzeTime`和`executeTime`字段

**额外字段**：
- `filePath`: 文件路径
- `reason`: 原因

### 1.3 老架构策略接口特点

```java
public interface IAppStrategy {
    // 分析阶段：接收当前记录，返回变更记录列表
    List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs);
    
    // 执行阶段：接收变更记录，执行操作
    void execute(ChangeRecord rec) throws Exception;
    
    // 前置条件检查
    protected boolean checkConditions(ChangeRecord rec);
}
```

**关键特点**：
1. 逐个文件处理，每个文件通过所有策略
2. `analyze`方法返回列表，支持创建新文件
3. `execute`方法只接收单个ChangeRecord
4. 支持前置条件检查
5. 支持条件组（OR关系）

### 1.4 新架构策略接口特点

```java
public interface IPlugin {
    // 预览阶段：接收文件路径列表，返回变更记录列表
    List<ChangeRecord> preview(List<String> filePaths, 
        PluginConfigDTO config, 
        ExecutionContext context);
    
    // 执行阶段：接收文件路径列表，返回变更记录列表
    List<ChangeRecord> execute(List<String> filePaths, 
        PluginConfigDTO config, 
        ExecutionContext context);
}
```

**关键特点**：
1. 批量处理文件路径列表
2. `preview`和`execute`方法都返回`List<ChangeRecord>`
3. 使用`PluginConfigDTO`传递配置
4. 使用`ExecutionContext`提供执行上下文

## 二、差异分析

### 2.1 ChangeRecord模型差异

| 方面 | 老架构 | 新架构 | 影响 |
|------|----------|----------|------|
| 操作类型 | 枚举`OperationType` | 字符串`operationType` | 类型不安全 |
| 执行状态 | 枚举`ExecStatus` | 字符串`status` | 类型不安全 |
| 构造函数 | 多种便捷构造函数 | 只有无参构造函数 | 使用不便 |
| 唯一标识 | `long id` | `String id` | 类型不一致 |
| 额外字段 | 无 | `filePath`, `reason` | 功能扩展 |

### 2.2 策略接口差异

| 方面 | 老架构 | 新架构 | 影响 |
|------|----------|----------|------|
| 处理方式 | 逐个文件处理 | 批量处理文件 | 流程不同 |
| analyze参数 | `ChangeRecord` | `List<String>` | 参数类型不同 |
| execute参数 | `ChangeRecord` | `List<String>` | 参数类型不同 |
| execute返回值 | `void` | `List<ChangeRecord>` | 返回值不同 |
| 配置传递 | 通过策略内部状态 | 通过`PluginConfigDTO` | 配置方式不同 |
| 执行上下文 | 通过`IAppController` | 通过`ExecutionContext` | 上下文不同 |

### 2.3 流水线处理差异

| 方面 | 老架构 | 新架构 | 影响 |
|------|----------|----------|------|
| 文件扫描 | 先扫描所有文件 | 先扫描所有文件 | 一致 |
| 策略调用 | 逐个文件通过所有策略 | 逐个文件通过所有策略 | 一致 |
| 变更记录 | 每个文件一个ChangeRecord | 每个文件一个ChangeRecord | 一致 |
| 状态管理 | 通过ChangeRecord.status | 通过ChangeRecord.status | 一致 |

## 三、改造方案设计

### 3.1 ChangeRecord模型改造

#### 3.1.1 添加枚举类型

```java
package com.filemanager.domain.enums;

public enum OperationType {
    NONE,           // 无操作
    RENAME,         // 重命名
    MOVE,           // 移动
    DELETE,         // 删除
    COPY,           // 复制
    CONVERT,        // 转换
    SPLIT,          // 分割
    SCRAPER,        // 刮削
    MERGE,          // 合并
    CLEANUP,        // 清理
    NCM_CONVERT,    // NCM转换
    NCM_CACHE_SCAN, // NCM缓存扫描
    NCM_LYRIC_DOWNLOAD // NCM歌词下载
}

public enum ExecStatus {
    PENDING,        // 待处理
    PREVIEWING,     // 预览中
    SUCCESS,        // 成功
    FAILED,         // 失败
    SKIPPED,        // 跳过
    EXECUTING       // 执行中
}
```

#### 3.1.2 添加便捷构造函数

```java
public ChangeRecord(String originalName, String newName, File fileHandle, 
    boolean changed, String newPath, OperationType opType) {
    this.originalName = originalName;
    this.newName = newName;
    this.fileHandle = fileHandle;
    this.changed = changed && opType != null && OperationType.NONE != opType;
    this.newPath = newPath;
    this.operationType = opType != null ? opType.name() : "NONE";
}

public ChangeRecord(String originalName, String newName, File fileHandle, 
    boolean changed, String newPath, OperationType opType, 
    Map<String, String> extraParams, ExecStatus status) {
    this(originalName, newName, fileHandle, changed, newPath, opType);
    this.extraParams = extraParams != null ? extraParams : new HashMap<>();
    this.status = status != null ? status.name() : "PENDING";
}
```

#### 3.1.3 添加类型安全的getter/setter

```java
public OperationType getOperationTypeEnum() {
    if (operationType == null) {
        return OperationType.NONE;
    }
    try {
        return OperationType.valueOf(operationType);
    } catch (IllegalArgumentException e) {
        return OperationType.NONE;
    }
}

public void setOperationType(OperationType opType) {
    this.operationType = opType != null ? opType.name() : "NONE";
}

public ExecStatus getStatusEnum() {
    if (status == null) {
        return ExecStatus.PENDING;
    }
    try {
        return ExecStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
        return ExecStatus.PENDING;
    }
}

public void setStatus(ExecStatus status) {
    this.status = status != null ? status.name() : "PENDING";
}
```

#### 3.1.4 添加链式处理支持

```java
public File getCurrentSource() {
    return intermediateFile != null ? intermediateFile : fileHandle;
}

public void setIntermediateFile(File intermediateFile) {
    this.intermediateFile = intermediateFile;
}
```

#### 3.1.5 添加过程信息记录

```java
public void addProcessInfo(String info) {
    if (this.processInfo == null) {
        this.processInfo = new ArrayList<>();
    }
    this.processInfo.add(info);
}

public void addProcessInfo(String key, String value) {
    addProcessInfo(key + ": " + value);
}
```

### 3.2 策略接口改造

#### 3.2.1 修改IPlugin接口

```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    PluginConfigDTO getDefaultConfig();
    List<PluginParameterDTO> getParameters();
    List<PreconditionGroupDTO> getDefaultPreconditionGroups();
    
    // 预览阶段：接收当前记录，返回变更记录列表
    List<ChangeRecord> preview(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        PluginConfigDTO config, 
        ExecutionContext context);
    
    // 执行阶段：接收变更记录，执行操作
    void execute(ChangeRecord record, 
        PluginConfigDTO config, 
        ExecutionContext context) throws Exception;
}
```

#### 3.2.2 增强ExecutionContext

```java
public class ExecutionContext {
    // 现有字段和方法...
    
    private Map<String, Object> attributes = new HashMap<>();
    
    /**
     * 设置属性
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }
    
    /**
     * 获取属性
     * @param key 属性键
     * @return 属性值
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }
    
    /**
     * 获取属性
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    public <T> T getAttribute(String key, T defaultValue) {
        Object value = attributes.get(key);
        return value != null ? (T) value : defaultValue;
    }
    
    /**
     * 开始计时
     */
    public void startTimer() {
        setAttribute("timerStart", System.currentTimeMillis());
    }
    
    /**
     * 停止计时并返回耗时
     * @return 耗时（毫秒）
     */
    public long stopTimer() {
        Long startTime = getAttribute("timerStart", 0L);
        return System.currentTimeMillis() - startTime;
    }
}
```

### 3.3 PipelineController改造

#### 3.3.1 修改文件处理逻辑

```java
// 扫描文件
List<String> allFilePaths = new ArrayList<>();
for (String directory : sourceDirectories) {
    File dir = new File(directory);
    if (dir.exists() && dir.isDirectory()) {
        List<File> files = fileScanner.scanFilesRobust(dir, minDepth, maxDepth, globalLimit, currentDirLimit, msg -> {
            UnifiedLogger.backendOperation("Pipeline", msg);
            taskManager.updateTaskMessage(taskId, msg);
        });
        for (File file : files) {
            allFilePaths.add(file.getAbsolutePath());
        }
    }
}

// 创建初始ChangeRecord列表
List<ChangeRecord> allRecords = new ArrayList<>();
for (String filePath : allFilePaths) {
    File file = new File(filePath);
    ChangeRecord record = new ChangeRecord(
        file.getName(),
        file.getName(),
        file,
        false,
        file.getAbsolutePath(),
        OperationType.NONE
    );
    allRecords.add(record);
}

// 逐个文件通过所有策略
List<ChangeRecord> allChanges = new ArrayList<>();
for (ChangeRecord currentRecord : allRecords) {
    // 检查是否已取消
    if (!isTaskRunning.get()) {
        break;
    }
    
    // 检查是否已变更
    if (currentRecord.isChanged()) {
        allChanges.add(currentRecord);
        continue;
    }
    
    // 逐个策略处理
    for (Map<String, Object> pluginConfig : pipeline) {
        String pluginId = (String) pluginConfig.get("pluginId");
        
        // 获取插件配置
        PluginConfigDTO config = pluginService.getPluginConfig(pluginId);
        
        // 创建执行上下文
        ExecutionContext context = new ExecutionContext(pluginId);
        
        // 调用策略预览
        List<ChangeRecord> changes = pluginService.previewPlugin(pluginId, currentRecord, allRecords, rootDirs, config, context);
        
        // 处理变更记录
        if (!changes.isEmpty()) {
            ChangeRecord change = changes.get(0);
            if (change.isChanged()) {
                // 更新当前记录
                currentRecord = change;
            } else {
                // 添加新文件记录
                allChanges.add(change);
            }
        }
    }
    
    allChanges.add(currentRecord);
}
```

### 3.4 策略适配改造

#### 3.4.1 FileMigratePlugin适配

```java
@Override
public List<ChangeRecord> preview(ChangeRecord currentRecord, 
    List<ChangeRecord> inputRecords, 
    List<File> rootDirs, 
    PluginConfigDTO config, 
    ExecutionContext context) {
    
    File sourceFile = currentRecord.getCurrentSource();
    String targetPath = buildTargetPath(sourceFile, config);
    
    if (targetPath == null) {
        return Collections.emptyList();
    }
    
    ChangeRecord record = new ChangeRecord(
        currentRecord.getOriginalName(),
        new File(targetPath).getName(),
        sourceFile,
        true,
        targetPath,
        OperationType.MOVE,
        new HashMap<>(),
        ExecStatus.PENDING
    );
    
    return Collections.singletonList(record);
}

@Override
public void execute(ChangeRecord record, 
    PluginConfigDTO config, 
    ExecutionContext context) throws Exception {
    
    File source = record.getCurrentSource();
    File target = new File(record.getNewPath());
    
    if (!target.getParentFile().exists()) {
        target.getParentFile().mkdirs();
    }
    
    String operationMode = config.getParameter("operationMode", "MOVE");
    
    if ("MOVE".equals(operationMode)) {
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
    } else {
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }
    
    record.setStatus(ExecStatus.SUCCESS);
    context.logInfo("文件移动完成: " + source.getName() + " -> " + target.getName());
}
```

#### 3.4.2 AudioConverterPlugin适配

```java
@Override
public List<ChangeRecord> preview(ChangeRecord currentRecord, 
    List<ChangeRecord> inputRecords, 
    List<File> rootDirs, 
    PluginConfigDTO config, 
    ExecutionContext context) {
    
    File sourceFile = currentRecord.getCurrentSource();
    
    // 检查文件类型
    if (!isAudioFile(sourceFile)) {
        return Collections.emptyList();
    }
    
    // 构建目标路径
    String targetPath = buildTargetPath(sourceFile, config);
    if (targetPath == null) {
        return Collections.emptyList();
    }
    
    // 检查目标文件是否存在
    File targetFile = new File(targetPath);
    if (targetFile.exists() && !config.getParameter("overwrite", false)) {
        return Collections.emptyList();
    }
    
    // 构建参数
    Map<String, String> params = new HashMap<>();
    params.put("format", config.getParameter("format", "mp3"));
    params.put("bitrate", config.getParameter("bitrate", "320"));
    params.put("codec", config.getParameter("codec", "libmp3lame"));
    
    ChangeRecord record = new ChangeRecord(
        currentRecord.getOriginalName(),
        targetFile.getName(),
        sourceFile,
        true,
        targetPath,
        OperationType.CONVERT,
        params,
        ExecStatus.PENDING
    );
    
    return Collections.singletonList(record);
}

@Override
public void execute(ChangeRecord record, 
    PluginConfigDTO config, 
    ExecutionContext context) throws Exception {
    
    File source = record.getCurrentSource();
    File target = new File(record.getNewPath());
    
    // 确保目标目录存在
    if (!target.getParentFile().exists()) {
        target.getParentFile().mkdirs();
    }
    
    // 执行转换
    executeFfmpegConversion(source, target, record.getExtraParams(), context);
    
    record.setStatus(ExecStatus.SUCCESS);
    context.logInfo("音频转换完成: " + source.getName() + " -> " + target.getName());
}
```

## 四、改造清单

### 4.1 ChangeRecord模型改造

- [ ] 添加`OperationType`枚举类型
- [ ] 添加`ExecStatus`枚举类型
- [ ] 添加便捷构造函数
- [ ] 添加类型安全的getter/setter方法
- [ ] 增强`addProcessInfo`方法
- [ ] 添加链式处理支持（已存在）
- [ ] 添加耗时统计字段（已存在）

### 4.2 策略接口改造

- [ ] 修改`IPlugin`接口的`preview`方法签名
- [ ] 修改`IPlugin`接口的`execute`方法签名
- [ ] 增强`ExecutionContext`类
- [ ] 更新`PluginService`实现

### 4.3 PipelineController改造

- [ ] 修改文件处理逻辑，使用ChangeRecord
- [ ] 实现逐个文件通过所有策略的处理模式
- [ ] 添加变更记录合并逻辑
- [ ] 添加链式处理支持

### 4.4 策略适配改造

- [ ] FileMigratePlugin适配
- [ ] AudioConverterPlugin适配
- [ ] MetadataScraperPlugin适配
- [ ] CueSplitterPlugin适配
- [ ] NcmConvertPlugin适配
- [ ] NcmCacheTransPlugin适配
- [ ] NcmLyricDownloadPlugin适配

### 4.5 文档更新

- [ ] 更新协议接口文档
- [ ] 更新前端交互流程文档
- [ ] 更新策略开发指南
- [ ] 添加迁移指南

### 4.6 测试用例更新

- [ ] 添加ChangeRecord模型测试
- [ ] 添加策略接口测试
- [ ] 添加PipelineController测试
- [ ] 添加策略适配测试
- [ ] 添加端到端测试

## 五、实施计划

### 5.1 第一阶段：ChangeRecord模型改造

**目标**：完善ChangeRecord模型，支持老架构的所有功能

**任务**：
1. 添加OperationType枚举类型
2. 添加ExecStatus枚举类型
3. 添加便捷构造函数
4. 添加类型安全的getter/setter方法
5. 增强addProcessInfo方法

**预计时间**：2小时

### 5.2 第二阶段：策略接口改造

**目标**：修改策略接口，支持老架构的调用方式

**任务**：
1. 修改IPlugin接口
2. 增强ExecutionContext类
3. 更新PluginService实现

**预计时间**：3小时

### 5.3 第三阶段：PipelineController改造

**目标**：修改流水线处理逻辑，支持逐个文件通过所有策略

**任务**：
1. 修改文件处理逻辑
2. 实现变更记录合并
3. 添加链式处理支持

**预计时间**：2小时

### 5.4 第四阶段：策略适配改造

**目标**：适配所有策略到新接口

**任务**：
1. 适配核心策略（FileMigrate, AudioConverter）
2. 适配复杂策略（MetadataScraper, CueSplitter）
3. 适配NCM策略（NcmConvert, NcmCacheTrans, NcmLyricDownload）
4. 测试策略功能

**预计时间**：4小时

### 5.5 第五阶段：文档和测试

**目标**：更新文档和测试用例

**任务**：
1. 更新协议接口文档
2. 更新前端交互流程文档
3. 添加测试用例
4. 执行端到端测试

**预计时间**：2小时

---

**文档版本**: 1.0  
**最后更新**: 2026-02-11  
**维护者**: FileManager Plus Team
