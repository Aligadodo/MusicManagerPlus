# 协议接口文档

## 概述

本文档定义了FileManager Plus项目中的协议接口规范，包括前后端数据交互协议、插件接口协议等。

## 核心设计原则

### 1. 文件修改唯一性原则

**原则说明**：一个文件在一个流水线执行过程中，只能被一个策略标记为修改。

**实现方式**：
- 每个文件在流水线中只能有一个ChangeRecord标记为`changed=true`
- 如果文件已经被前面的策略修改，后续策略不能再标记该文件为修改
- 后续策略可以基于前面的修改结果继续处理，但不能产生新的修改记录

**示例**：
```
文件A.mp3
  -> 策略1（重命名）: 产生ChangeRecord，changed=true，newName=A_重命名.mp3
  -> 策略2（元数据更新）: 不能标记changed=true，只能更新extraParams
  -> 策略3（格式转换）: 不能标记changed=true，只能基于前面的结果处理
```

### 2. 单文件修改协议

**原则说明**：如果策略只是修改单个文件，只需要在ChangeRecord中标记修改，不需要返回新的ChangeRecord。

**实现方式**：
- 修改单个文件时，只更新当前ChangeRecord的属性
- 设置`changed=true`表示文件被修改
- 更新`newName`、`newPath`等属性表示修改后的状态
- 不创建新的ChangeRecord对象

**示例**：
```java
ChangeRecord record = new ChangeRecord();
record.setOriginalName("A.mp3");
record.setNewName("A_重命名.mp3");
record.setChanged(true);  // 只标记修改，不返回新记录
record.setOperationType("RENAME");
```

### 3. 新文件创建协议

**原则说明**：只有当策略通过原文件创建新文件时，才需要返回新的ChangeRecord。

**实现方式**：
- 创建新文件时，返回包含新文件信息的ChangeRecord
- 设置`isCreate=true`表示这是创建的新文件
- 新文件的ChangeRecord与原文件的ChangeRecord是独立的
- 可以在原文件的ChangeRecord中记录创建的新文件信息

**示例**：
```java
// 原文件记录
ChangeRecord originalRecord = new ChangeRecord();
originalRecord.setOriginalName("A.mp3");
originalRecord.setChanged(false);

// 新文件记录
ChangeRecord newRecord = new ChangeRecord();
newRecord.setOriginalName("A.mp3");
newRecord.setNewName("A_converted.flac");
newRecord.setChanged(true);
newRecord.setIsCreate(true);
newRecord.setOperationType("CONVERT");

// 返回两个记录
return Arrays.asList(originalRecord, newRecord);
```

### 4. 链式处理协议

**原则说明**：支持流水线中的链式处理，后续策略可以基于前面策略的结果继续处理。

**实现方式**：
- 使用`intermediateFile`字段记录中间状态文件
- 使用`getCurrentSource()`方法获取当前应该处理的源文件
- 使用`processInfo`字段记录处理过程信息
- 使用`analyzeTime`和`executeTime`记录耗时统计

**示例**：
```java
// 策略1处理
ChangeRecord record1 = new ChangeRecord();
record1.setOriginalName("A.mp3");
record1.setFileHandle(new File("/path/to/A.mp3"));
record1.setNewName("A_step1.mp3");
File intermediateFile = performStep1(record1.getFileHandle());
record1.setIntermediateFile(intermediateFile);

// 策略2处理
File currentSource = record1.getCurrentSource();  // 获取中间文件
record1.setNewName("A_step2.mp3");
File finalFile = performStep2(currentSource);
record1.setIntermediateFile(finalFile);
```

## ChangeRecord数据结构

### 字段说明

| 字段名 | 类型 | 说明 | 必填 |
|--------|------|------|------|
| id | String | 记录唯一标识符 | 是 |
| originalName | String | 原始文件名 | 是 |
| newName | String | 新文件名 | 否 |
| fileHandle | File | 原始文件句柄 | 是 |
| changed | boolean | 是否被修改 | 是 |
| newPath | String | 新文件路径 | 否 |
| operationType | String | 操作类型 | 是 |
| extraParams | Map<String, String> | 额外参数 | 否 |
| status | String | 执行状态 | 是 |
| failReason | String | 失败原因 | 否 |
| isCreate | boolean | 是否创建新文件 | 否 |
| isDeleteOrMove | boolean | 是否删除或移动 | 否 |
| selected | boolean | 是否被选中 | 否 |
| intermediateFile | File | 中间状态文件 | 否 |
| processInfo | List<String> | 处理过程信息 | 否 |
| analyzeTime | long | 分析阶段耗时（毫秒） | 否 |
| executeTime | long | 执行阶段耗时（毫秒） | 否 |
| filePath | String | 文件路径 | 是 |
| reason | String | 变更原因 | 否 |

### 操作类型（operationType）

| 值 | 说明 |
|----|------|
| NONE | 无操作 |
| RENAME | 重命名 |
| MOVE | 移动 |
| DELETE | 删除 |
| COPY | 复制 |
| CONVERT | 格式转换 |
| SPLIT | 分割 |
| SCRAPER | 刮削 |
| MERGE | 合并 |
| CLEANUP | 清理 |
| NCM_CONVERT | NCM转换 |
| NCM_CACHE_SCAN | NCM缓存扫描 |
| NCM_LYRIC_DOWNLOAD | NCM歌词下载 |

**枚举类型**: `com.filemanager.domain.enums.OperationType`

**类型安全方法**:
- `getOperationTypeEnum()`: 获取枚举类型的操作类型
- `setOperationType(OperationType)`: 设置枚举类型的操作类型

### 执行状态（status）

| 值 | 说明 |
|----|------|
| PENDING | 待处理 |
| PREVIEWING | 预览中 |
| SUCCESS | 成功 |
| FAILED | 失败 |
| SKIPPED | 跳过 |
| EXECUTING | 执行中 |

**枚举类型**: `com.filemanager.domain.enums.ExecStatus`

**类型安全方法**:
- `getStatusEnum()`: 获取枚举类型的执行状态
- `setStatus(ExecStatus)`: 设置枚举类型的执行状态

## 前后端交互协议

### 预览流水线接口

**接口路径**: `POST /api/pipeline/analyze`

**请求参数**:
```json
{
  "sourceDirectories": ["/path/to/source1", "/path/to/source2"],
  "pipeline": [
    {
      "pluginId": "file-rename",
      "config": {
        "pattern": "{name}_{date}{ext}",
        "dateFormat": "yyyyMMdd"
      },
      "preconditionGroups": []
    },
    {
      "pluginId": "metadata-scraper",
      "config": {
        "source": "musicbrainz"
      },
      "preconditionGroups": []
    }
  ]
}
```

**响应参数**:
```json
{
  "success": true,
  "taskId": "task-123456",
  "message": "预览任务已开始执行"
}
```

### 执行流水线接口

**接口路径**: `POST /api/pipeline/execute`

**请求参数**: 同预览接口

**响应参数**:
```json
{
  "success": true,
  "taskId": "task-123456",
  "message": "执行任务已开始执行"
}
```

### 获取变更记录接口

**接口路径**: `GET /api/pipeline/changes`

**请求参数**:
- `searchFilter`: 搜索过滤关键词
- `statusFilter`: 状态过滤
- `operationTypeFilter`: 操作类型过滤
- `hideUnchanged`: 是否隐藏未变更记录
- `page`: 页码
- `size`: 每页大小
- `sortBy`: 排序字段
- `sortDirection`: 排序方向

**响应参数**:
```json
{
  "records": [
    {
      "id": "1",
      "originalName": "A.mp3",
      "newName": "A_重命名.mp3",
      "filePath": "/path/to/A.mp3",
      "status": "SUCCESS",
      "operationType": "RENAME",
      "changed": true,
      "reason": "文件名规范化"
    }
  ],
  "total": 100,
  "page": 1,
  "size": 20
}
```

### 停止任务接口

**接口路径**: `POST /api/pipeline/stop`

**响应参数**:
```json
{
  "success": true,
  "message": "任务已成功中止"
}
```

### 获取任务状态接口

**接口路径**: `GET /api/pipeline/status`

**响应参数**:
```json
{
  "taskId": "task-123456",
  "taskName": "preview",
  "status": "PREVIEWING",
  "step": "扫描文件",
  "message": "正在扫描文件...",
  "progress": 50,
  "total": 1000,
  "processed": 500,
  "hasChanges": true,
  "changesCount": 50
}
```

## 插件接口协议

### IPlugin接口

```java
public interface IPlugin {
    String getId();
    String getName();
    String getDescription();
    String getVersion();
    PluginConfigDTO getDefaultConfig();
    List<PluginParameterDTO> getParameters();
    List<PreconditionGroupDTO> getDefaultPreconditionGroups();
    
    // 批量处理方法（兼容旧接口）
    List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context);
    
    // 逐个文件处理方法（新接口，推荐使用）
    List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        PluginConfigDTO config, 
        ExecutionContext context);
    
    void execute(ChangeRecord record, 
        PluginConfigDTO config, 
        ExecutionContext context) throws Exception;
}
```

### 插件实现规范

#### 1. 预览方法（preview）

**用途**: 批量预览文件处理结果

**实现规范**:
- 返回所有可能的变更记录
- 不实际修改文件
- 使用`status="PENDING"`标记待处理的记录
- 支持批量处理提高效率

#### 2. 执行方法（execute - 批量）

**用途**: 批量执行文件操作

**实现规范**:
- 实际执行文件操作
- 更新`status`为最终状态（SUCCESS/FAILED/SKIPPED）
- 记录执行耗时
- 支持批量处理提高效率

#### 3. 分析方法（analyze - 推荐）

**用途**: 逐个文件分析处理结果

**实现规范**:
- 接收当前文件的ChangeRecord
- 返回变更记录列表
- 如果只是修改当前文件，返回包含当前文件的列表
- 如果创建新文件，返回包含新文件的列表
- 使用枚举类型设置操作类型和状态

**示例**:
```java
@Override
public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
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
```

#### 4. 执行方法（execute - 单个，推荐）

**用途**: 执行单个文件的变更操作

**实现规范**:
- 实际执行文件操作
- 使用枚举类型设置状态
- 记录执行耗时
- 捕获异常并记录到failReason

**示例**:
```java
@Override
public void execute(ChangeRecord record, 
    PluginConfigDTO config, 
    ExecutionContext context) throws Exception {
    
    File source = record.getCurrentSource();
    File target = new File(record.getNewPath());
    
    if (!target.getParentFile().exists()) {
        target.getParentFile().mkdirs();
    }
    
    Files.move(source.toPath(), target.toPath());
    
    record.setStatus(ExecStatus.SUCCESS);
    context.logInfo("文件移动完成: " + source.getName() + " -> " + target.getName());
}
```

#### 5. 错误处理

**实现规范**:
- 捕获所有异常并记录到`failReason`
- 设置`status=ExecStatus.FAILED`
- 返回包含错误信息的ChangeRecord
- 使用`ExecutionContext`记录错误日志

**示例**:
```java
try {
    executeOperation(record);
    record.setStatus(ExecStatus.SUCCESS);
} catch (Exception e) {
    record.setStatus(ExecStatus.FAILED);
    record.setFailReason(e.getMessage());
    context.logError("执行失败: " + e.getMessage());
    throw e;
}
```

#### 6. 进度反馈

**实现规范**:
- 使用`ExecutionContext`记录处理进度
- 定期更新进度信息
- 提供详细的处理日志
- 使用`processInfo`记录处理过程

**示例**:
```java
context.startTimer();
context.logInfo("开始处理文件: " + record.getOriginalName());
record.addProcessInfo("开始时间: " + new Date());

executeOperation(record);

long elapsed = context.stopTimer();
record.setExecuteTime(elapsed);
record.addProcessInfo("完成时间: " + new Date());
record.addProcessInfo("耗时: " + elapsed + "ms");

context.logInfo("文件处理完成，耗时: " + elapsed + "ms");
```

## 文件扫描协议

### FileScanner接口

```java
public interface FileScanner {
    List<File> scanFilesRobust(File root, int minDepth, int maxDepth, 
        AtomicInteger globalLimit, AtomicInteger dirLimit, Consumer<String> msg);
}
```

### 扫描参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| root | File | 扫描根目录 |
| minDepth | int | 最小扫描深度 |
| maxDepth | int | 最大扫描深度 |
| globalLimit | AtomicInteger | 全局文件数量限制 |
| dirLimit | AtomicInteger | 单目录文件数量限制 |
| msg | Consumer<String> | 进度消息回调 |

### 文件过滤协议

### FileFilterService接口

```java
public interface FileFilterService {
    boolean isFileIncluded(File file);
    boolean isFileFiltered(File file);
    List<String> getScanFilterList();
    void addScanFilter(String filter);
    void removeScanFilter(String filter);
    void clearScanFilters();
}
```

### 过滤规则说明

- 支持通配符匹配：`*`匹配任意字符，`?`匹配单个字符
- 支持正则表达式
- 支持路径过滤
- 支持文件名过滤

## 数据一致性保证

### 事务处理

- 所有文件操作在事务中执行
- 失败时回滚所有变更
- 支持断点续传

### 并发控制

- 使用线程池控制并发度
- 使用原子变量保证线程安全
- 支持任务取消

### 错误恢复

- 记录详细的错误日志
- 支持重试机制
- 提供错误恢复建议

## 性能优化

### 文件扫描优化

- 使用并行扫描提高效率
- 支持深度控制减少不必要的扫描
- 支持数量限制控制内存使用

### 流水线执行优化

- 逐个文件通过所有策略处理
- 减少重复扫描
- 支持中间结果缓存

### 内存管理

- 控制单次处理的文件数量
- 及时释放不再使用的资源
- 使用流式处理减少内存占用

## 安全考虑

### 文件操作安全

- 验证文件路径合法性
- 防止路径遍历攻击
- 限制文件操作权限

### 数据传输安全

- 使用HTTPS加密传输
- 验证请求来源
- 防止CSRF攻击

---

**文档版本**: 1.0  
**最后更新**: 2026-02-11  
**维护者**: FileManager Plus Team
