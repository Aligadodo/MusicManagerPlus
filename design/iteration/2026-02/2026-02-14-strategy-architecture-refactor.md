# 策略架构改造迭代文档

**日期**: 2026-02-14
**优先级**: 高
**状态**: 进行中

## 问题分析

### 老架构策略设计（正确的设计）

#### 核心接口设计
1. **IAppStrategy** - 策略抽象基类
   - `analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs)` - 分析方法
     - 输入：当前记录、全量记录列表、根目录列表
     - 输出：变更记录列表（可能是空列表，也可能包含新生成的记录）
     - 作用：判断文件是否需要变更，计算变更内容
   
   - `execute(ChangeRecord rec)` - 执行方法
     - 输入：单个变更记录
     - 输出：无
     - 作用：实际执行文件操作
   
   - `analyzeWithPreCheck()` - 带前置检查的分析方法
     - 检查是否已变更（避免二次变更）
     - 检查前置条件
     - 检查目标类型（文件/文件夹）
   
   - `checkConditions()` - 前置条件检查
     - 支持条件组（OR关系）
     - 组内条件（AND关系）

2. **ChangeRecord** - 变更记录
   - `originalName` - 原始文件名
   - `newName` - 新文件名
   - `fileHandle` - 文件句柄
   - `changed` - 是否需要变更
   - `newPath` - 新路径
   - `opType` - 操作类型（RENAME、MOVE、CONVERT等）
   - `extraParams` - 额外参数（change param）
   - `status` - 执行状态（PENDING、SUCCESS、FAILED等）
   - `intermediateFile` - 中间状态文件（链式处理）
   - `processInfo` - 处理过程信息

3. **业务流程**
   ```
   扫描文件 -> 生成ChangeRecord列表
   -> 遍历所有策略
   -> 对每个ChangeRecord调用analyze方法
   -> analyze检查前置条件，计算变更，标记changed=true
   -> 用户点击执行
   -> 对标记的ChangeRecord调用execute方法
   -> execute实际执行文件操作
   ```

#### 老架构AdvancedRenameStrategy的特点
1. **规则链式执行**：支持多条规则按顺序执行
2. **规则级前置条件**：每条规则可以有自己的前置条件
3. **多种操作类型**：
   - ADD_NUMBER_PREFIX - 添加序号前缀
   - REPLACE - 替换
   - PREFIX - 添加前缀
   - SUFFIX - 添加后缀
   - REMOVE_PREFIX - 移除前缀
   - REMOVE_SUFFIX - 移除后缀
   - REGEX_REPLACE - 正则替换
4. **跨盘操作**：支持移动/复制模式
5. **处理范围控制**：仅文件、仅文件夹、全部
6. **完整的规则编辑UI**：支持添加、删除、移动规则

### 新架构策略实现的问题

#### AbstractConfigurableStrategy的问题
1. **接口不一致**：
   - 使用 `executeForFile(String filePath, ...)` 而不是 `execute(ChangeRecord rec)`
   - 使用 `createPreviewRecord(String filePath, ...)` 而不是 `analyze(ChangeRecord rec, ...)`
   - 输入是文件路径字符串，而不是ChangeRecord对象

2. **业务逻辑丢失**：
   - 没有前置条件检查逻辑
   - 没有条件组支持
   - 没有链式处理支持
   - 没有目标类型检查

#### FileRenameStrategy的问题
1. **功能严重简化**：
   - 只支持简单的命名模式（{name}_{index}）
   - 没有规则链
   - 没有条件判断
   - 没有操作类型选择

2. **配置字段有限**：
   - pattern - 命名模式
   - startIndex - 起始序号
   - padZeros - 补零
   - zeroPadding - 补零长度
   - preserveExtension - 保留扩展名
   - overwriteExisting - 覆盖现有文件

3. **与老架构完全不兼容**：
   - 老架构的智能重命名支持复杂的规则系统
   - 新架构的FileRenameStrategy只是一个简单的序号重命名工具

#### AdvancedRenameStrategy的问题
1. **几乎空实现**：
   - generateNewName方法只是返回原文件名
   - 没有实际的重命名逻辑
   - rules配置字段没有被使用

2. **配置字段定义但未实现**：
   - crossDriveMode - 跨盘动作（未使用）
   - processScope - 处理范围（未使用）
   - rules - 重命名规则列表（未使用）

3. **与老架构完全不兼容**：
   - 老架构的AdvancedRenameStrategy有完整的规则系统
   - 新架构的AdvancedRenameStrategy只是一个空壳

### 核心问题总结

1. **接口设计错误**：
   - 新架构使用文件路径字符串作为输入
   - 老架构使用ChangeRecord对象作为输入
   - 导致无法实现链式处理和状态传递

2. **业务逻辑丢失**：
   - 前置条件检查逻辑丢失
   - 条件组支持丢失
   - 链式处理支持丢失
   - 目标类型检查丢失

3. **规则系统缺失**：
   - 老架构有完整的规则系统（RenameRule、RenameActionType、RenameMode）
   - 新架构没有规则系统
   - 导致高级重命名策略无法实现

## 改造方案

### 设计原则

1. **保留DTO设计**：
   - 为了通用性和前后端解耦，DTO设计是好的
   - 可以接受后端配置管理的额外复杂度
   - 前端可以选择性展示部分参数，后端存储全部参数

2. **策略只关心两件事**：
   - 分析（analyze）- 判断文件是否需要变更，计算变更内容
   - 执行（execute）- 实际执行文件操作

3. **驱动流程设计**：
   - 扫描文件 -> 生成ChangeRecord列表
   - 遍历策略 -> 对每个ChangeRecord调用analyze方法
   - analyze检查前置条件，计算变更，标记changed=true
   - 用户点击执行 -> 对标记的ChangeRecord调用execute方法
   - execute实际执行文件操作

4. **ChangeRecord标记**：
   - 基于ChangeRecord标记操作和参数（opType、extraParams）
   - 记录处理过程信息（processInfo）
   - 支持链式处理（intermediateFile）

5. **模块拆分**：
   - 文件扫描过滤模块
   - 分析ChangeRecord模块
   - 执行ChangeRecord模块

6. **快捷操作**：
   - 筛选失败记录
   - 单独执行失败记录
   - 删除原始文件
   - 删除目标文件

7. **任务统计信息**：
   - 总文件数
   - 过滤数
   - 变更数
   - 变更类型统计

8. **详细ChangeRecord列表**：
   - 包含详细变更信息
   - 支持筛选
   - 支持分页查询（大数据量场景）

### 改造目标

1. **恢复老架构的策略接口设计**：
   - analyze方法输入ChangeRecord对象
   - execute方法输入ChangeRecord对象
   - 支持前置条件检查
   - 支持条件组
   - 支持目标类型检查

2. **保留新架构的DTO设计**：
   - 保留StrategyConfigDTO用于配置管理
   - 保留PluginConfigDTO用于前后端交互
   - 前端可以选择性展示参数

3. **迁移所有策略的业务逻辑**：
   - 确保所有策略的analyze方法与老架构逻辑一致
   - 确保所有策略的execute方法与老架构逻辑一致
   - 支持策略链式处理

4. **改造后端调度逻辑**：
   - 实现扫描->分析->执行流程
   - 支持策略链式处理
   - 支持前置条件过滤
   - 支持目标类型过滤

5. **改造任务管理**：
   - 提供任务统计信息
   - 支持分页查询ChangeRecord列表
   - 支持快捷操作（筛选失败、单独执行、删除文件）

6. **改造前端适配**：
   - 更新前端适配新的策略接口
   - 更新预览和执行逻辑
   - 更新配置管理

7. **清理老架构代码**：
   - 迭代完成后清理老架构的不通用接口代码
   - 避免遗留垃圾代码

## 完整策略清单

### 需要迁移的策略（14个）

1. **FileRenameStrategy（智能重命名）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/filerename/FileRenameStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/FileRenameStrategy.java`
   - 问题：功能严重简化，只支持简单的序号重命名
   - 改造：恢复老架构的智能重命名逻辑

2. **AdvancedRenameStrategy（高级重命名）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/advancedrename/AdvancedRenameStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/AdvancedRenameStrategy.java`
   - 问题：几乎空实现，没有实际的重命名逻辑
   - 改造：恢复老架构的高级重命名逻辑，包括规则系统

3. **AudioConverterStrategy（音频转换）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/audioconverter/AudioConverterStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/AudioConverterStrategy.java`
   - 改造：恢复老架构的音频转换逻辑

4. **CueSplitterStrategy（CUE分轨）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/cuesplitter/CueSplitterStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/CueSplitterStrategy.java`
   - 改造：恢复老架构的CUE分轨逻辑

5. **CueFileRenameStrategy（CUE文件重命名）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/cuefilerename/CueFileRenameStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/CueFileRenameStrategy.java`
   - 改造：恢复老架构的CUE文件重命名逻辑

6. **FileCleanupStrategy（文件清理）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/filecleanup/FileCleanupStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/FileCleanupStrategy.java`
   - 改造：恢复老架构的文件清理逻辑

7. **FileCollectionStrategy（文件收集）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/filecollection/FileCollectionStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/FileCollectionStrategy.java`
   - 改造：恢复老架构的文件收集逻辑

8. **FileMigrateStrategy（文件迁移）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/filemigrate/FileMigrateStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/FileMigrateStrategy.java`
   - 改造：恢复老架构的文件迁移逻辑

9. **FileTypeFixStrategy（文件类型修复）**
   - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/filetypefix/FileTypeFixStrategy.java`
   - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/FileTypeFixStrategy.java`
   - 改造：恢复老架构的文件类型修复逻辑

10. **FileUnzipStrategy（文件解压）**
    - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/fileunzip/FileUnzipStrategy.java`
    - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/FileUnzipStrategy.java`
    - 改造：恢复老架构的文件解压逻辑

11. **MetadataScraperStrategy（元数据抓取）**
    - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/metadatascraper/MetadataScraperStrategy.java`
    - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/MetadataScraperStrategy.java`
    - 改造：恢复老架构的元数据抓取逻辑

12. **AlbumDirNormalizeStrategy（专辑目录标准化）**
    - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/albumdirnormalize/AlbumDirNormalizeStrategy.java`
    - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/AlbumDirNormalizeStrategy.java`
    - 改造：恢复老架构的专辑目录标准化逻辑

13. **TrackNumberStrategy（音轨编号）**
    - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/tracknumber/TrackNumberStrategy.java`
    - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/TrackNumberStrategy.java`
    - 改造：恢复老架构的音轨编号逻辑

14. **NcmIntegratedStrategy（网易云音乐集成）**
    - 路径：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/impl/ncmintegrated/NcmIntegratedStrategy.java`
    - 老架构路径：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/NcmIntegratedStrategy.java`
    - 改造：恢复老架构的网易云音乐集成逻辑

## 改造步骤

### 第一步：改造策略抽象基类
1. **改造AbstractConfigurableStrategy**：
   - 添加 `analyze(ChangeRecord currentRecord, List<ChangeRecord> inputRecords, List<File> rootDirs, StrategyConfigDTO config, ExecutionContext context)` 方法
   - 添加 `execute(ChangeRecord rec, StrategyConfigDTO config, ExecutionContext context)` 方法
   - 实现前置条件检查逻辑
   - 实现条件组支持
   - 实现目标类型检查
   - 保留DTO设计（StrategyConfigDTO、PluginConfigDTO）

2. **保留现有接口**：
   - 保留 `executeForFile(String filePath, ...)` 方法（向后兼容）
   - 保留 `createPreviewRecord(String filePath, ...)` 方法（向后兼容）
   - 新方法优先，旧方法逐步废弃

### 第二步：迁移ChangeRecord
1. **统一使用老架构的ChangeRecord**：
   - 使用老架构的ChangeRecord（`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/model/ChangeRecord.java`）
   - 移除新架构中的ChangeRecord实体类（`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/domain/entity/ChangeRecord.java`）
   - 更新所有DTO使用老架构的ChangeRecord

2. **更新ChangeRecord字段**：
   - 确保所有字段与老架构一致
   - originalName、newName、fileHandle、changed、newPath、opType、extraParams、status、intermediateFile、processInfo等

### 第三步：迁移策略实现（14个策略）

#### 3.1 迁移FileRenameStrategy（智能重命名）
1. 分析老架构的FileRenameStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.2 迁移AdvancedRenameStrategy（高级重命名）
1. 分析老架构的AdvancedRenameStrategy实现
2. 迁移规则系统（RenameRule、RenameActionType、RenameMode）
3. 迁移analyze方法逻辑（规则链式执行）
4. 迁移execute方法逻辑
5. 更新配置字段定义
6. 测试验证

#### 3.3 迁移AudioConverterStrategy（音频转换）
1. 分析老架构的AudioConverterStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.4 迁移CueSplitterStrategy（CUE分轨）
1. 分析老架构的CueSplitterStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.5 迁移CueFileRenameStrategy（CUE文件重命名）
1. 分析老架构的CueFileRenameStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.6 迁移FileCleanupStrategy（文件清理）
1. 分析老架构的FileCleanupStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.7 迁移FileCollectionStrategy（文件收集）
1. 分析老架构的FileCollectionStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.8 迁移FileMigrateStrategy（文件迁移）
1. 分析老架构的FileMigrateStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.9 迁移FileTypeFixStrategy（文件类型修复）
1. 分析老架构的FileTypeFixStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.10 迁移FileUnzipStrategy（文件解压）
1. 分析老架构的FileUnzipStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.11 迁移MetadataScraperStrategy（元数据抓取）
1. 分析老架构的MetadataScraperStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.12 迁移AlbumDirNormalizeStrategy（专辑目录标准化）
1. 分析老架构的AlbumDirNormalizeStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.13 迁移TrackNumberStrategy（音轨编号）
1. 分析老架构的TrackNumberStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

#### 3.14 迁移NcmIntegratedStrategy（网易云音乐集成）
1. 分析老架构的NcmIntegratedStrategy实现
2. 迁移analyze方法逻辑
3. 迁移execute方法逻辑
4. 更新配置字段定义
5. 测试验证

### 第四步：改造后端调度逻辑

#### 4.1 文件扫描过滤模块
1. 实现文件扫描功能
2. 实现文件过滤功能
3. 生成ChangeRecord列表

#### 4.2 分析ChangeRecord模块
1. 遍历所有策略
2. 对每个ChangeRecord调用analyze方法
3. 检查前置条件
4. 计算变更内容
5. 标记changed=true
6. 支持策略链式处理

#### 4.3 执行ChangeRecord模块
1. 遍历标记的ChangeRecord
2. 调用execute方法
3. 实际执行文件操作
4. 更新执行状态

#### 4.4 驱动流程整合
1. 实现扫描->分析->执行流程
2. 支持流程控制（暂停、取消）
3. 支持进度报告

### 第五步：改造任务管理

#### 5.1 任务统计信息
1. **总文件数**：扫描到的文件总数
2. **过滤数**：被前置条件过滤掉的文件数
3. **变更数**：需要变更的文件数
4. **变更类型统计**：按操作类型统计（RENAME、MOVE、CONVERT等）
5. **执行状态统计**：按执行状态统计（PENDING、SUCCESS、FAILED等）

#### 5.2 分页查询ChangeRecord列表
1. **支持分页参数**：page、size
2. **支持筛选参数**：status、opType、changed等
3. **支持排序参数**：sortBy、sortOrder
4. **大数据量优化**：避免一次性传输大量数据

#### 5.3 快捷操作
1. **筛选失败记录**：只显示执行失败的ChangeRecord
2. **单独执行失败记录**：只重新执行失败的ChangeRecord
3. **删除原始文件**：删除ChangeRecord的原始文件（fileHandle）
4. **删除目标文件**：删除ChangeRecord的目标文件（newPath）

#### 5.4 接口设计
1. **GET /api/tasks/{id}/records** - 获取任务的ChangeRecord列表（支持分页和筛选）
2. **GET /api/tasks/{id}/statistics** - 获取任务的统计信息
3. **POST /api/tasks/{id}/records/execute-failed** - 单独执行失败的ChangeRecord
4. **POST /api/tasks/{id}/records/delete-source** - 删除原始文件
5. **POST /api/tasks/{id}/records/delete-target** - 删除目标文件

### 第六步：改造前端适配

#### 6.1 更新策略接口调用
1. 更新预览逻辑，使用新的analyze方法
2. 更新执行逻辑，使用新的execute方法
3. 更新配置管理，使用DTO设计

#### 6.2 更新任务管理界面
1. 显示任务统计信息
2. 显示ChangeRecord列表（支持分页和筛选）
3. 支持快捷操作（筛选失败、单独执行、删除文件）

#### 6.3 更新配置管理界面
1. 前端选择性展示参数
2. 后端存储全部参数
3. 支持参数验证

### 第七步：测试验证

#### 7.1 测试所有策略的analyze逻辑
1. 测试FileRenameStrategy的analyze逻辑
2. 测试AdvancedRenameStrategy的analyze逻辑
3. 测试AudioConverterStrategy的analyze逻辑
4. 测试CueSplitterStrategy的analyze逻辑
5. 测试CueFileRenameStrategy的analyze逻辑
6. 测试FileCleanupStrategy的analyze逻辑
7. 测试FileCollectionStrategy的analyze逻辑
8. 测试FileMigrateStrategy的analyze逻辑
9. 测试FileTypeFixStrategy的analyze逻辑
10. 测试FileUnzipStrategy的analyze逻辑
11. 测试MetadataScraperStrategy的analyze逻辑
12. 测试AlbumDirNormalizeStrategy的analyze逻辑
13. 测试TrackNumberStrategy的analyze逻辑
14. 测试NcmIntegratedStrategy的analyze逻辑

#### 7.2 测试所有策略的execute逻辑
1. 测试FileRenameStrategy的execute逻辑
2. 测试AdvancedRenameStrategy的execute逻辑
3. 测试AudioConverterStrategy的execute逻辑
4. 测试CueSplitterStrategy的execute逻辑
5. 测试CueFileRenameStrategy的execute逻辑
6. 测试FileCleanupStrategy的execute逻辑
7. 测试FileCollectionStrategy的execute逻辑
8. 测试FileMigrateStrategy的execute逻辑
9. 测试FileTypeFixStrategy的execute逻辑
10. 测试FileUnzipStrategy的execute逻辑
11. 测试MetadataScraperStrategy的execute逻辑
12. 测试AlbumDirNormalizeStrategy的execute逻辑
13. 测试TrackNumberStrategy的execute逻辑
14. 测试NcmIntegratedStrategy的execute逻辑

#### 7.3 测试策略链式处理
1. 测试多个策略链式执行
2. 测试中间状态文件传递
3. 测试intermediateFile字段

#### 7.4 测试前置条件过滤
1. 测试条件组支持
2. 测试目标类型过滤
3. 测试已变更文件过滤

#### 7.5 测试任务管理
1. 测试任务统计信息
2. 测试分页查询ChangeRecord列表
3. 测试快捷操作（筛选失败、单独执行、删除文件）

### 第八步：清理老架构代码

#### 8.1 清理不通用接口代码
1. 删除老架构的IAppStrategy（`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/base/IAppStrategy.java`）
2. 删除老架构的ChangeRecord（如果已迁移到新架构）
3. 删除老架构的策略实现（如果已迁移到新架构）

#### 8.2 清理废弃方法
1. 删除AbstractConfigurableStrategy中的废弃方法（executeForFile、createPreviewRecord）
2. 更新所有调用点使用新方法

#### 8.3 清理临时文件
1. 删除迁移过程中的临时文件
2. 删除测试文件

## 验收标准

1. **所有策略的analyze方法与老架构逻辑一致**
2. **所有策略的execute方法与老架构逻辑一致**
3. **支持策略链式处理**
4. **支持前置条件过滤**
5. **支持目标类型过滤**
6. **前端能够正常预览和执行**
7. **任务统计信息准确**
8. **分页查询ChangeRecord列表正常**
9. **快捷操作正常**
10. **所有测试用例通过**
11. **老架构的不通用接口代码已清理**

## 风险评估

1. **高风险**：
   - 策略接口改造影响范围大
   - 需要迁移所有策略实现（14个）
   - 前端需要适配新的接口
   - 需要清理老架构代码

2. **缓解措施**：
   - 分步骤实施，每步充分测试
   - 保留新架构的DTO设计
   - 保留向后兼容的接口
   - 前端逐步迁移，保持向后兼容
   - 最后清理老架构代码

## 时间估算

1. 改造策略抽象基类：2小时
2. 迁移ChangeRecord：1小时
3. 迁移FileRenameStrategy：2小时
4. 迁移AdvancedRenameStrategy：4小时
5. 迁移AudioConverterStrategy：2小时
6. 迁移CueSplitterStrategy：2小时
7. 迁移CueFileRenameStrategy：2小时
8. 迁移FileCleanupStrategy：2小时
9. 迁移FileCollectionStrategy：2小时
10. 迁移FileMigrateStrategy：2小时
11. 迁移FileTypeFixStrategy：2小时
12. 迁移FileUnzipStrategy：2小时
13. 迁移MetadataScraperStrategy：2小时
14. 迁移AlbumDirNormalizeStrategy：2小时
15. 迁移TrackNumberStrategy：2小时
16. 迁移NcmIntegratedStrategy：2小时
17. 改造后端调度逻辑：4小时
18. 改造任务管理：4小时
19. 改造前端适配：4小时
20. 测试验证：6小时
21. 清理老架构代码：2小时

**总计**：约54小时

## 参考资料

- 老架构IAppStrategy：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/app/base/IAppStrategy.java`
- 老架构ChangeRecord：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/model/ChangeRecord.java`
- 老架构AdvancedRenameStrategy：`/Users/hrcao/Documents/MusicManagerPlus/src/main/java/com/filemanager/strategy/AdvancedRenameStrategy.java`
- 新架构AbstractConfigurableStrategy：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/plugin/AbstractConfigurableStrategy.java`
- 新架构TaskController：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/backend/controller/TaskController.java`
- 新架构TaskServiceImpl：`/Users/hrcao/Documents/MusicManagerPlus/backend/src/main/java/com/filemanager/backend/service/impl/TaskServiceImpl.java`
