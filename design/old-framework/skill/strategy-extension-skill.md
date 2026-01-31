# 策略扩展开发指南

## 快速开始

### 创建新策略的三步法

#### 第一步：创建策略类

```java
public class MyNewStrategy extends IAppStrategy {
    
    // 1. UI组件声明
    private final JFXComboBox<String> cbMode;
    private final TextField txtParam;
    
    // 2. 运行时参数声明（使用p前缀）
    private String pMode;
    private String pParam;
    
    public MyNewStrategy() {
        // 3. 初始化UI组件
        cbMode = new JFXComboBox<>();
        cbMode.getItems().addAll("模式1", "模式2");
        cbMode.getSelectionModel().select(0);
        
        txtParam = new TextField();
    }
    
    // 4. 实现必需的抽象方法
    @Override
    public String getName() {
        return "我的新策略";
    }
    
    @Override
    public String getDescription() {
        return "策略的功能描述";
    }
    
    @Override
    public Node getConfigNode() {
        VBox configPane = new VBox(10);
        configPane.getChildren().addAll(
            StyleFactory.createLabel("模式选择"),
            cbMode,
            StyleFactory.createLabel("参数"),
            txtParam
        );
        return configPane;
    }
    
    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY; // 或 FOLDERS_ONLY, ALL
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

#### 第二步：注册策略

在 `AppStrategyFactory.java` 中注册：

```java
public static List<IAppStrategy> getAppStrategies() {
    List<IAppStrategy> strategyPrototypes = new ArrayList<IAppStrategy>();
    // ... 其他策略 ...
    strategyPrototypes.add(new MyNewStrategy());
    return strategyPrototypes;
}
```

#### 第三步：添加操作类型（如果需要）

如果需要新的操作类型，在 `OperationType.java` 中添加：

```java
public enum OperationType {
    // ... 其他类型 ...
    MY_OPERATION("我的操作", "操作的描述");
    
    public final String name;
    public final String desc;
}
```

然后在 `AppStrategyFactory` 中添加匹配逻辑：

```java
public static IAppStrategy findStrategyForOp(OperationType op, List<IAppStrategy> pipelineStrategies) {
    for (int i = pipelineStrategies.size() - 1; i >= 0; i--) {
        IAppStrategy s = pipelineStrategies.get(i);
        // ... 其他匹配 ...
        if (op == OperationType.MY_OPERATION && s instanceof MyNewStrategy) return s;
    }
    return null;
}
```

## ChangeRecord 使用指南

### 场景1：修改现有文件（重命名/移动）

```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                   List<ChangeRecord> inputRecords, 
                                   List<File> rootDirs) {
    File file = currentRecord.getFileHandle();
    
    // 1. 计算新文件名和新路径
    String newName = "new_" + file.getName();
    File newFile = new File(file.getParent(), newName);
    
    // 2. 修改现有记录
    currentRecord.setNewName(newName);
    currentRecord.setNewPath(newFile.getAbsolutePath());
    currentRecord.setChanged(true);
    currentRecord.setOpType(OperationType.RENAME);
    currentRecord.setStatus(ExecStatus.PENDING);
    
    // 3. 返回空列表（不生成新文件）
    return Collections.emptyList();
}

@Override
public void execute(ChangeRecord rec) throws Exception {
    File source = rec.getFileHandle();
    File target = new File(rec.getNewPath());
    
    if (source.equals(target)) return;
    
    // 创建目标目录
    if (!target.getParentFile().exists()) {
        target.getParentFile().mkdirs();
    }
    
    // 执行移动/重命名
    Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
}
```

### 场景2：生成新文件（转换/提取）

```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                   List<ChangeRecord> inputRecords, 
                                   List<File> rootDirs) {
    List<ChangeRecord> result = new ArrayList<>();
    
    File sourceFile = currentRecord.getFileHandle();
    
    // 1. 计算目标文件路径
    String targetPath = sourceFile.getParent() + File.separator + 
                       sourceFile.getName().replace(".flac", ".mp3");
    
    // 2. 创建新的ChangeRecord
    ChangeRecord record = new ChangeRecord(
        sourceFile.getName(),      // originalName
        sourceFile.getName().replace(".flac", ".mp3"), // newName
        sourceFile,                // fileHandle
        true,                      // changed
        targetPath,                // newPath
        OperationType.CONVERT     // opType
    );
    
    // 3. 添加额外参数
    record.getExtraParams().put("audioFormat", "mp3");
    record.getExtraParams().put("bitrate", "320k");
    record.getExtraParams().put("overwrite", "true");
    
    // 4. 设置状态
    record.setStatus(ExecStatus.PENDING);
    
    result.add(record);
    return result;
}

@Override
public void execute(ChangeRecord rec) throws Exception {
    File source = rec.getFileHandle();
    File target = new File(rec.getNewPath());
    
    // 1. 检查是否覆盖
    if (!Boolean.parseBoolean(rec.getExtraParams().get("overwrite")) && target.exists()) {
        return;
    }
    
    // 2. 创建目标目录
    if (!target.getParentFile().exists()) {
        target.getParentFile().mkdirs();
    }
    
    // 3. 执行转换
    convertFile(source, target, rec.getExtraParams());
}
```

### 场景3：批量处理（归类/整理）

```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                   List<ChangeRecord> inputRecords, 
                                   List<File> rootDirs) {
    List<ChangeRecord> result = new ArrayList<>();
    
    // 1. 获取目录下的所有文件
    List<ChangeRecord> filesInDir = getFilesUnderDir(
        currentRecord.getFileHandle(), 
        inputRecords
    );
    
    // 2. 处理每个文件
    for (ChangeRecord rec : filesInDir) {
        // 3. 修改现有记录
        rec.setChanged(true);
        rec.setNewPath(newPath);
        rec.setOpType(OperationType.COLLECT);
        rec.setStatus(ExecStatus.PENDING);
        
        // 4. 添加额外参数
        rec.getExtraParams().put("collection_name", "合集名称");
        rec.getExtraParams().put("similarity", "0.85");
        
        result.add(rec);
    }
    
    return result;
}
```

## 命名规范速查表

### 运行时参数命名

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 字符串 | `p` + 驼峰 | `pMode`, `pSeparator`, `pDestDir` |
| 布尔值 | `p` + `is/has` + 驼峰 | `pIsForce`, `pHasMetadata` |
| 数值 | `p` + 驼峰 | `pThreshold`, `pMaxDepth` |

### UI组件命名

| 组件类型 | 前缀 | 示例 |
|---------|------|------|
| ComboBox | `cb` | `cbMode`, `cbFunction` |
| TextField | `txt` | `txtSeparator`, `txtDestDir` |
| CheckBox | `chk` | `chkPadZero`, `chkCleanEmpty` |
| Button | `btn` | `btnAddRule`, `btnRemoveRule` |
| ListView | `lv` | `lvRules`, `lvFiles` |
| Spinner | `sp` | `spThreadCount`, `spDepth` |

### extraParams 键命名

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 通用 | 小写 + 下划线 | `overwrite`, `action` |
| 音频 | 小写 + 下划线 | `audioFormat`, `bitrate` |
| 元数据 | 小写 + 下划线 | `songName`, `artistName` |
| 归类 | 小写 + 下划线 | `collection_name`, `similarity` |

## 常用工具方法

### 1. 获取目标文件

```java
// 获取特定文件的ChangeRecord
ChangeRecord target = getTargetFile(file, inputRecords);
```

### 2. 获取目录下的文件

```java
// 获取目录下的所有ChangeRecord
List<ChangeRecord> files = getFilesUnderDir(dir, inputRecords);

// 过滤特定类型
List<ChangeRecord> musicFiles = files.stream()
    .filter(rec -> FileTypeUtil.isMusicFile(rec.getFileHandle()))
    .collect(Collectors.toList());
```

### 3. 日志记录

```java
// 记录正常信息
log("开始处理文件: " + file.getName());

// 记录错误信息
logError("处理失败: " + e.getMessage());

// 使预览失效
invalidatePreview("配置已更改");
```

### 4. 文件操作

```java
// 移动文件
Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

// 复制文件
Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

// 创建目录
if (!target.getParentFile().exists()) {
    target.getParentFile().mkdirs();
}

// 检查文件是否存在
if (target.exists()) {
    // 处理已存在的情况
}
```

## 常见问题快速解决

### Q1: analyze() 应该返回什么？

**规则**:
- 修改现有文件 → 返回 `Collections.emptyList()`
- 生成新文件 → 返回包含新ChangeRecord的列表
- 不需要处理 → 返回 `Collections.emptyList()`

### Q2: 如何判断文件是否需要处理？

**规则**:
```java
// 检查是否已变更
if (currentRecord.isChanged()) {
    return Collections.emptyList();
}

// 检查前置条件
if (!checkConditions(currentRecord)) {
    return Collections.emptyList();
}

// 检查文件类型
if (ScanTarget.FILES_ONLY == getTargetType() && currentRecord.getFileHandle().isDirectory()) {
    return Collections.emptyList();
}
```

### Q3: execute() 中如何获取实际源文件？

**规则**:
```java
// 使用 getCurrentSource() 获取实际源文件
File source = rec.getCurrentSource();

// 或者直接使用 fileHandle
File source = rec.getFileHandle();
```

### Q4: 如何传递额外参数？

**规则**:
```java
// 在 analyze() 中设置参数
rec.getExtraParams().put("key", "value");

// 在 execute() 中获取参数
String value = rec.getExtraParams().get("key");
```

### Q5: 如何处理异常？

**规则**:
```java
@Override
public void execute(ChangeRecord rec) throws Exception {
    try {
        // 执行操作
        doSomething();
        
        // 设置成功状态
        rec.setStatus(ExecStatus.SUCCESS);
    } catch (Exception e) {
        // 设置失败状态和原因
        rec.setStatus(ExecStatus.FAILED);
        rec.setFailReason(e.getMessage());
        
        // 记录错误
        logError("执行失败: " + e.getMessage());
        
        // 重新抛出异常
        throw e;
    }
}
```

## 最佳实践检查清单

### analyze() 方法

- [ ] 检查 `currentRecord.isChanged()`
- [ ] 检查前置条件 `checkConditions()`
- [ ] 检查文件类型 `getTargetType()`
- [ ] 正确设置 `newName` 和 `newPath`
- [ ] 正确设置 `opType`
- [ ] 正确设置 `changed` 标志
- [ ] 正确设置 `status`
- [ ] 合理使用 `extraParams`
- [ ] 返回正确的列表

### execute() 方法

- [ ] 检查源文件和目标文件是否相同
- [ ] 创建目标目录
- [ ] 检查是否覆盖
- [ ] 使用 `getCurrentSource()` 获取源文件
- [ ] 处理异常
- [ ] 更新执行状态
- [ ] 记录日志

### 参数管理

- [ ] 使用 `p` 前缀命名运行时参数
- [ ] 在 `captureParams()` 中捕获参数
- [ ] 在 `saveConfig()` 中保存参数
- [ ] 在 `loadConfig()` 中加载参数
- [ ] 使用 `extraParams` 传递策略特定参数

### 日志记录

- [ ] 使用 `log()` 记录正常信息
- [ ] 使用 `logError()` 记录错误信息
- [ ] 记录关键操作
- [ ] 记录状态变化
- [ ] 提供有意义的日志信息

## 扩展示例

### 示例1：文件重命名策略

```java
public class FileRenameStrategy extends IAppStrategy {
    private final TextField txtPrefix;
    private final TextField txtSuffix;
    private String pPrefix;
    private String pSuffix;
    
    public FileRenameStrategy() {
        txtPrefix = new TextField();
        txtSuffix = new TextField();
    }
    
    @Override
    public String getName() {
        return "文件重命名";
    }
    
    @Override
    public String getDescription() {
        return "为文件添加前缀和后缀";
    }
    
    @Override
    public Node getConfigNode() {
        VBox configPane = new VBox(10);
        configPane.getChildren().addAll(
            StyleFactory.createLabel("前缀"),
            txtPrefix,
            StyleFactory.createLabel("后缀"),
            txtSuffix
        );
        return configPane;
    }
    
    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }
    
    @Override
    public void captureParams() {
        pPrefix = txtPrefix.getText();
        pSuffix = txtSuffix.getText();
    }
    
    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                       List<ChangeRecord> inputRecords, 
                                       List<File> rootDirs) {
        File file = currentRecord.getFileHandle();
        String fileName = file.getName();
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            ext = fileName.substring(dot);
            fileName = fileName.substring(0, dot);
        }
        
        String newName = pPrefix + fileName + pSuffix + ext;
        File newFile = new File(file.getParent(), newName);
        
        currentRecord.setNewName(newName);
        currentRecord.setNewPath(newFile.getAbsolutePath());
        currentRecord.setChanged(true);
        currentRecord.setOpType(OperationType.RENAME);
        currentRecord.setStatus(ExecStatus.PENDING);
        
        return Collections.emptyList();
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        
        if (source.equals(target)) return;
        if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
        
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
```

### 示例2：文件分类策略

```java
public class FileClassifyStrategy extends IAppStrategy {
    private final TextField txtKeyword;
    private final TextField txtTargetDir;
    private String pKeyword;
    private String pTargetDir;
    
    public FileClassifyStrategy() {
        txtKeyword = new TextField();
        txtTargetDir = new TextField();
    }
    
    @Override
    public String getName() {
        return "文件分类";
    }
    
    @Override
    public String getDescription() {
        return "根据关键词将文件分类到指定目录";
    }
    
    @Override
    public Node getConfigNode() {
        VBox configPane = new VBox(10);
        configPane.getChildren().addAll(
            StyleFactory.createLabel("关键词"),
            txtKeyword,
            StyleFactory.createLabel("目标目录"),
            txtTargetDir
        );
        return configPane;
    }
    
    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
    }
    
    @Override
    public void captureParams() {
        pKeyword = txtKeyword.getText();
        pTargetDir = txtTargetDir.getText();
    }
    
    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
                                       List<ChangeRecord> inputRecords, 
                                       List<File> rootDirs) {
        File file = currentRecord.getFileHandle();
        
        // 检查文件名是否包含关键词
        if (!file.getName().contains(pKeyword)) {
            return Collections.emptyList();
        }
        
        // 计算目标路径
        File targetDir = new File(pTargetDir);
        File targetFile = new File(targetDir, file.getName());
        
        currentRecord.setNewPath(targetFile.getAbsolutePath());
        currentRecord.setChanged(true);
        currentRecord.setOpType(OperationType.MOVE);
        currentRecord.setStatus(ExecStatus.PENDING);
        
        return Collections.emptyList();
    }
    
    @Override
    public void execute(ChangeRecord rec) throws Exception {
        File source = rec.getFileHandle();
        File target = new File(rec.getNewPath());
        
        if (source.equals(target)) return;
        if (!target.getParentFile().exists()) target.getParentFile().mkdirs();
        
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
```

## 注意事项

### 1. 线程安全

- 不要在 `analyze()` 中执行文件操作
- 不要在 `execute()` 中修改UI
- 使用 `synchronized` 保护共享资源

### 2. 性能优化

- 避免在 `analyze()` 中进行重复计算
- 使用缓存提高性能
- 合理使用流式操作

### 3. 错误处理

- 始终处理异常
- 记录错误信息
- 更新执行状态

### 4. 资源管理

- 及时关闭文件流
- 释放系统资源
- 避免内存泄漏

## 参考资源

- [IAppStrategy接口设计文档](../doc/iappstrategy-interface-design.md)
- [策略总览文档](../doc/strategy-overview.md)
- [ChangeRecord数据结构](../doc/iappstrategy-interface-design.md#changerecord-数据结构)

---

**文档版本**: 1.0  
**最后更新**: 2026-01-27  
**维护者**: FileEditTools Team
