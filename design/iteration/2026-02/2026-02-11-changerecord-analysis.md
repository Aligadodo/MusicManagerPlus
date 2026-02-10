# ChangeRecord模型深度分析与改造方案

## 一、老架构ChangeRecord模型属性分析

### 1.1 核心标识属性

| 属性名 | 类型 | 作用 | 设计用途 | 初始化时机 |
|--------|------|------|----------|------------|
| id | long | 唯一标识符 | 用于区分不同的变更记录，支持数据库存储和UI显示 | 在PipelineManager中创建记录时分配 |
| originalName | String | 原始文件名 | 用于显示原始文件名，对比变更前后 | 创建ChangeRecord时传入 |
| newName | String | 新文件名 | 用于显示变更后的文件名 | 创建ChangeRecord时传入或策略分析时设置 |
| fileHandle | File | 原始文件句柄 | 指向实际文件对象，用于文件操作 | 创建ChangeRecord时传入 |

### 1.2 状态属性

| 属性名 | 类型 | 作用 | 设计用途 | 初始化时机 |
|--------|------|------|----------|------------|
| changed | boolean | 是否被修改 | 用于判断文件是否需要处理，防止重复修改 | 创建ChangeRecord时根据opType计算 |
| status | ExecStatus | 执行状态 | 用于跟踪执行进度（PENDING/SUCCESS/FAILED/SKIPPED） | 创建ChangeRecord时默认为PENDING，执行时更新 |
| failReason | String | 失败原因 | 用于记录错误信息，便于用户排查问题 | 执行失败时设置 |

### 1.3 操作类型属性

| 属性名 | 类型 | 作用 | 设计用途 | 初始化时机 |
|--------|------|------|----------|------------|
| opType | OperationType | 操作类型 | 用于区分不同的操作（RENAME/MOVE/DELETE/COPY/CONVERT/SPLIT/SCRAPER等） | 创建ChangeRecord时传入 |
| isCreate | boolean | 是否创建新文件 | 用于区分是修改原文件还是创建新文件 | 创建ChangeRecord时传入 |
| isDeleteOrMove | boolean | 是否删除或移动 | 用于判断是否需要特殊处理（如更新文件句柄） | 创建ChangeRecord时传入 |

### 1.4 路径属性

| 属性名 | 类型 | 作用 | 设计用途 | 初始化时机 |
|--------|------|------|----------|------------|
| newPath | String | 新文件路径 | 用于记录变更后的位置 | 创建ChangeRecord时传入 |

### 1.5 链式处理属性

| 属性名 | 类型 | 作用 | 设计用途 | 初始化时机 |
|--------|------|------|----------|------------|
| intermediateFile | File | 中间状态文件 | 用于链式处理中传递中间结果 | 策略执行时设置 |

**关键方法**：
- `getCurrentSource()`: 获取当前应该处理的源文件，可能是原始文件也可能是中间文件

### 1.6 信息记录属性

| 属性名 | 类型 | 作用 | 设计用途 | 初始化时机 |
|--------|------|------|----------|------------|
| extraParams | Map<String, String> | 额外参数 | 用于存储策略特定的参数信息（如FFmpeg参数、元数据等） | 创建ChangeRecord时传入或策略分析时添加 |
| processInfo | List<String> | 处理过程信息 | 用于记录详细的处理过程，便于调试和用户查看 | 策略分析时逐步添加 |
| analyzeTime | long | 分析阶段耗时 | 用于性能统计 | 分析开始和结束时计算 |
| executeTime | long | 执行阶段耗时 | 用于性能统计 | 执行开始和结束时计算 |

### 1.7 用户交互属性

| 属性名 | 类型 | 作用 | 设计用途 | 初始化时机 |
|--------|------|------|----------|------------|
| selected | boolean | 选择状态 | 用于用户界面中的选择操作 | 用户操作时设置 |

## 二、老架构策略使用模式分析

### 2.1 直接修改模式（FileMigrateStrategy）

**特点**：只修改原文件的属性，不创建新文件

**实现方式**：
```java
// 分析阶段
ChangeRecord rec = new ChangeRecord(
    rec.getOriginalName(),  // 原始文件名
    targetFile.getName(),   // 新文件名
    rec.getFileHandle(),    // 原始文件句柄
    true,                  // changed=true，表示有变更
    targetFile.getAbsolutePath(),  // 新路径
    OperationType.MOVE,   // 操作类型
    new HashMap<>(),       // 额外参数
    ExecStatus.PENDING      // 初始状态
);

// 执行阶段
Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
rec.setStatus(ExecStatus.SUCCESS);  // 更新状态
```

**关键点**：
- `changed=true`：表示文件被修改
- `isCreate=false`：不创建新文件
- `isDeleteOrMove=true`：删除或移动操作

### 2.2 创建新文件模式（AudioConverterStrategy）

**特点**：创建新文件，原文件保持不变

**实现方式**：
```java
// 分析阶段
Map<String, String> param = new HashMap<>();
param.put("format", "flac");  // 转换格式
param.put("bitrate", "320");   // 比特率

ChangeRecord rec = new ChangeRecord(
    rec.getOriginalName(),
    targetFile.getName(),
    rec.getFileHandle(),
    true,  // changed=true
    targetFile.getAbsolutePath(),
    OperationType.CONVERT,
    param,  // 存储转换参数
    ExecStatus.PENDING
);

// 执行阶段
executeFfmpegConversion(rec);  // 执行转换
rec.setStatus(ExecStatus.SUCCESS);
```

**关键点**：
- `changed=true`：表示有变更
- `isCreate=true`：创建新文件
- `extraParams`：存储转换参数

### 2.3 混合模式（MetadataScraperStrategy）

**特点**：既修改原文件，又创建新文件

**实现方式**：
```java
// 分析阶段
rec.addProcessInfo("开始元数据刮削分析: " + file.getName());
rec.addProcessInfo("文件路径: " + file.getAbsolutePath());

// 修改原文件
if (processor.processLyrics(...)) {
    rec.setChanged(true);
    rec.setOpType(OperationType.SCRAPER);
    rec.getExtraParams().put("scraper_active", "true");
    rec.setNewName("[更新] " + file.getName());
}

// 创建新文件
if (processor.processCover(...)) {
    Map<String, String> p = new HashMap<>();
    p.put("task_type", "DOWNLOAD_COVER");
    ChangeRecord coverRec = new ChangeRecord(
        "下载: 专辑封面",
        "cover.jpg",
        parentDir,
        true,
        targetCover.getAbsolutePath(),
        OperationType.SCRAPER,
        p,
        ExecStatus.PENDING
    );
    results.add(coverRec);  // 添加到结果列表
}

// 执行阶段
rec.setStatus(ExecStatus.SUCCESS);
for (ChangeRecord newRec : results) {
    executeRecord(newRec);  // 执行新文件记录
}
```

**关键点**：
- `processInfo`：记录详细的处理过程
- `extraParams`：存储任务类型等参数
- 返回列表包含多个ChangeRecord

### 2.4 链式处理模式（CueSplitterStrategy）

**特点**：基于中间文件继续处理

**实现方式**：
```java
// 分析阶段
ChangeRecord trackRec = new ChangeRecord(
    displayInfo,
    trackName,
    sourceAudio,
    true,
    targetFile.getAbsolutePath(),
    OperationType.SPLIT,
    params,
    ExecStatus.PENDING
);

// 执行阶段
File intermediateFile = executeSplit(trackRec);
trackRec.setIntermediateFile(intermediateFile);  // 设置中间文件

// 后续策略处理
File currentSource = trackRec.getCurrentSource();  // 获取中间文件
executeNextStep(currentSource);
```

**关键点**：
- `intermediateFile`：存储中间文件
- `getCurrentSource()`：获取当前源文件

## 三、新架构与老架构的差异分析

### 3.1 ChangeRecord模型差异

| 方面 | 老架构 | 新架构 | 差异 |
|------|----------|----------|------|
| 字段完整性 | 包含所有必要字段 | 部分字段缺失 | 新架构缺少部分字段 |
| 类型定义 | 使用枚举类型 | 使用字符串类型 | 新架构使用字符串，不够类型安全 |
| 构造函数 | 提供多种构造函数 | 只有无参构造函数 | 新架构缺少便捷构造函数 |
| 链式处理 | 支持intermediateFile | 支持 | 一致 |
| 过程信息 | 支持processInfo | 支持 | 一致 |
| 耗时统计 | 支持analyzeTime/executeTime | 支持 | 一致 |

### 3.2 策略接口差异

| 方面 | 老架构 | 新架构 | 差异 |
|------|----------|----------|------|
| analyze方法 | 接收ChangeRecord，返回List<ChangeRecord> | 接收List<String>，返回List<ChangeRecord> | 参数类型不同 |
| execute方法 | 接收ChangeRecord，无返回值 | 接收List<String>，返回List<ChangeRecord> | 参数和返回值不同 |
| 前置条件 | 支持条件组 | 支持前置条件组 | 一致 |
| 链式处理 | 通过intermediateFile实现 | 未明确支持 | 新架构未明确支持 |

### 3.3 流水线处理差异

| 方面 | 老架构 | 新架构 | 差异 |
|------|----------|----------|------|
| 文件扫描 | 先扫描所有文件，再逐个处理 | 先扫描所有文件，再逐个处理 | 一致 |
| 策略调用 | 逐个文件通过所有策略 | 逐个文件通过所有策略 | 一致 |
| 变更记录 | 每个文件一个ChangeRecord | 每个文件一个ChangeRecord | 一致 |
| 状态管理 | 通过ChangeRecord.status管理 | 通过ChangeRecord.status管理 | 一致 |

## 四、改造方案设计

### 4.1 ChangeRecord模型改造

#### 4.1.1 添加枚举类型支持

```java
public enum OperationType {
    NONE, RENAME, MOVE, DELETE, COPY, CONVERT, SPLIT, SCRAPER, MERGE, CLEANUP
}

public enum ExecStatus {
    PENDING, SUCCESS, FAILED, SKIPPED, PREVIEWING, EXECUTING
}
```

#### 4.1.2 添加便捷构造函数

```java
public ChangeRecord(String originalName, String newName, File fileHandle, 
    boolean changed, String newPath, OperationType opType, 
    Map<String, String> extraParams, ExecStatus status) {
    this.originalName = originalName;
    this.newName = newName;
    this.fileHandle = fileHandle;
    this.changed = changed && opType != null && OperationType.NONE != opType;
    this.newPath = newPath;
    this.opType = opType;
    this.extraParams = extraParams != null ? extraParams : new HashMap<>();
    this.status = status != null ? status : ExecStatus.PENDING;
}
```

#### 4.1.3 添加链式处理支持

```java
public File getCurrentSource() {
    return intermediateFile != null ? intermediateFile : fileHandle;
}

public void setIntermediateFile(File intermediateFile) {
    this.intermediateFile = intermediateFile;
}
```

#### 4.1.4 添加过程信息记录

```java
public void addProcessInfo(String info) {
    if (this.processInfo == null) {
        this.processInfo = new ArrayList<>();
    }
    this.processInfo.add(info);
}
```

### 4.2 策略接口改造

#### 4.2.1 修改analyze方法签名

```java
public interface IPlugin {
    List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs);
    
    void execute(ChangeRecord record) throws Exception;
}
```

#### 4.2.2 添加ExecutionContext支持

```java
public class ExecutionContext {
    private List<String> processInfo = new ArrayList<>();
    private long startTime;
    
    public void log(String message) {
        processInfo.add(message);
    }
    
    public void startTimer() {
        startTime = System.currentTimeMillis();
    }
    
    public long stopTimer() {
        return System.currentTimeMillis() - startTime;
    }
}
```

### 4.3 PipelineController改造

#### 4.3.1 修改文件处理逻辑

```java
for (String filePath : allFilePaths) {
    File file = new File(filePath);
    
    // 创建初始ChangeRecord
    ChangeRecord record = new ChangeRecord(
        file.getName(),
        file.getName(),
        file,
        false,
        file.getAbsolutePath(),
        OperationType.NONE,
        new HashMap<>(),
        ExecStatus.PENDING
    );
    
    // 逐个策略处理
    for (Map<String, Object> pluginConfig : pipeline) {
        String pluginId = (String) pluginConfig.get("pluginId");
        
        // 检查是否已变更
        if (record.isChanged()) {
            break;  // 已变更的文件不支持二次变更
        }
        
        // 调用策略分析
        List<ChangeRecord> changes = pluginService.analyzePlugin(pluginId, record, allRecords, rootDirs);
        
        // 处理变更记录
        if (!changes.isEmpty()) {
            ChangeRecord change = changes.get(0);
            if (change.isChanged()) {
                record = change;  // 更新当前记录
            } else {
                allChanges.add(change);  // 添加新文件记录
            }
        }
    }
    
    allChanges.add(record);
}
```

### 4.4 策略适配改造

#### 4.4.1 FileMigrateStrategy适配

```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
    List<ChangeRecord> inputRecords, List<File> rootDirs) {
    
    File sourceFile = currentRecord.getCurrentSource();
    String targetPath = buildTargetPath(sourceFile);
    
    return Collections.singletonList(new ChangeRecord(
        currentRecord.getOriginalName(),
        new File(targetPath).getName(),
        sourceFile,
        true,
        targetPath,
        OperationType.MOVE,
        new HashMap<>(),
        ExecStatus.PENDING
    ));
}

@Override
public void execute(ChangeRecord record) throws Exception {
    File source = record.getCurrentSource();
    File target = new File(record.getNewPath());
    
    Files.move(source.toPath(), target.toPath());
    record.setStatus(ExecStatus.SUCCESS);
}
```

#### 4.4.2 AudioConverterStrategy适配

```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
    List<ChangeRecord> inputRecords, List<File> rootDirs) {
    
    File sourceFile = currentRecord.getCurrentSource();
    String targetPath = buildTargetPath(sourceFile);
    
    Map<String, String> params = new HashMap<>();
    params.put("format", "flac");
    params.put("bitrate", "320");
    
    return Collections.singletonList(new ChangeRecord(
        currentRecord.getOriginalName(),
        new File(targetPath).getName(),
        sourceFile,
        true,
        targetPath,
        OperationType.CONVERT,
        params,
        ExecStatus.PENDING
    ));
}

@Override
public void execute(ChangeRecord record) throws Exception {
    File source = record.getCurrentSource();
    File target = new File(record.getNewPath());
    
    executeFfmpegConversion(source, target, record.getExtraParams());
    record.setStatus(ExecStatus.SUCCESS);
}
```

## 五、改造清单

### 5.1 ChangeRecord模型改造

- [ ] 添加OperationType枚举类型
- [ ] 添加ExecStatus枚举类型
- [ ] 添加便捷构造函数
- [ ] 添加getCurrentSource()方法
- [ ] 添加addProcessInfo()方法
- [ ] 添加耗时统计字段

### 5.2 策略接口改造

- [ ] 修改IPlugin接口的analyze方法签名
- [ ] 修改IPlugin接口的execute方法签名
- [ ] 添加ExecutionContext类
- [ ] 更新PluginService实现

### 5.3 PipelineController改造

- [ ] 修改文件处理逻辑，使用ChangeRecord
- [ ] 实现逐个文件通过所有策略的处理模式
- [ ] 添加变更记录合并逻辑
- [ ] 添加链式处理支持

### 5.4 策略适配改造

- [ ] FileMigrateStrategy适配
- [ ] AudioConverterStrategy适配
- [ ] MetadataScraperStrategy适配
- [ ] CueSplitterStrategy适配
- [ ] 其他策略适配

### 5.5 文档更新

- [ ] 更新协议接口文档
- [ ] 更新前端交互流程文档
- [ ] 更新策略开发指南

### 5.6 测试用例更新

- [ ] 添加ChangeRecord模型测试
- [ ] 添加策略接口测试
- [ ] 添加PipelineController测试
- [ ] 添加策略适配测试

## 六、实施计划

### 6.1 第一阶段：ChangeRecord模型改造

**目标**：完善ChangeRecord模型，支持老架构的所有功能

**任务**：
1. 添加枚举类型
2. 添加便捷构造函数
3. 添加链式处理支持
4. 添加过程信息记录

**预计时间**：2小时

### 6.2 第二阶段：策略接口改造

**目标**：修改策略接口，支持老架构的调用方式

**任务**：
1. 修改IPlugin接口
2. 添加ExecutionContext类
3. 更新PluginService实现

**预计时间**：3小时

### 6.3 第三阶段：PipelineController改造

**目标**：修改流水线处理逻辑，支持逐个文件通过所有策略

**任务**：
1. 修改文件处理逻辑
2. 实现变更记录合并
3. 添加链式处理支持

**预计时间**：2小时

### 6.4 第四阶段：策略适配改造

**目标**：适配所有策略到新接口

**任务**：
1. 适配核心策略
2. 适配其他策略
3. 测试策略功能

**预计时间**：4小时

### 6.5 第五阶段：文档和测试

**目标**：更新文档和测试用例

**任务**：
1. 更新协议接口文档
2. 更新前端交互流程文档
3. 添加测试用例

**预计时间**：2小时

---

**文档版本**: 1.0  
**最后更新**: 2026-02-11  
**维护者**: FileManager Plus Team
