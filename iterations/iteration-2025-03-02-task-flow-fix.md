# 迭代文档：任务流程与配置快照修复

## 1. 问题描述

### 1.1 任务流程问题
1. **自动执行模式未正确实现**：无论是否勾选"自动执行"，任务都会立即开始扫描和预览
2. **任务状态不正确**：预览完成后状态显示为"执行中"，而非"预览完成"
3. **空扫描未处理**：扫描到0个文件时仍继续执行预览，浪费资源
4. **状态枚举不完整**：缺少 SCANNED（扫描完成）和 PREVIEWED（预览完成）状态

### 1.2 配置快照问题
1. **缺少 autoExecute 参数**：配置快照未记录是否自动执行
2. **全局设置不完整**：缺少 previewLimit、executionLimit、autoRefresh 等参数
3. **前端未传递全局设置**：创建任务时未将全局设置传递给后端

### 1.3 后端参数使用问题
1. **未使用配置快照参数**：后端执行时未读取配置快照中的全局设置

## 2. 解决方案

### 2.1 任务状态枚举扩展
```java
public enum TaskStatus {
    CREATED("已创建"),
    SCANNING("正在扫描"),
    SCANNED("扫描完成"),      // 新增
    PREVIEWING("正在预览"),
    PREVIEWED("预览完成"),    // 新增
    EXECUTING("正在执行"),
    COMPLETED("执行完成"),
    FAILED("执行失败"),
    CANCELLED("已取消");
}
```

### 2.2 任务流程控制
```
创建任务 (CREATED)
    │
    ├─ autoExecute=false → 停止，等待手动触发
    │
    └─ autoExecute=true → 自动执行
           │
           ▼
       扫描文件 (SCANNING)
           │
           ├─ 扫描到0个文件 → 直接结束 (SCANNED)
           │
           └─ 扫描到文件 → (SCANNED)
                  │
                  ├─ autoExecute=false → 停止，等待手动触发预览
                  │
                  └─ autoExecute=true → 自动执行预览
                         │
                         ▼
                     预览分析 (PREVIEWING)
                         │
                         ▼
                     (PREVIEWED)
                         │
                         ├─ autoExecute=false → 停止，等待手动触发执行
                         │
                         └─ autoExecute=true → 自动执行
                                │
                                ▼
                            执行变更 (EXECUTING)
                                │
                                ▼
                            (COMPLETED)
```

### 2.3 配置快照完善
```java
public class GlobalSettings {
    // 线程配置
    private int previewThreads;
    private int executionThreads;
    private String threadPoolMode;
    
    // 扫描配置
    private int minRecursionDepth;
    private int maxRecursionDepth;
    
    // 运行配置
    private int previewLimit;
    private int executionLimit;
    private boolean autoRefresh;
    
    // 执行配置
    private boolean dryRun;
    private boolean overwrite;
    private boolean backup;
    private String backupPath;
    private int retryCount;
    private long retryInterval;
    private long timeout;
    
    // 自动执行
    private boolean autoExecute;
}
```

## 3. 实施步骤

### 步骤1：扩展任务状态枚举
- 文件：`backend/src/main/java/com/filemanager/backend/model/TaskInfo.java`
- 添加 SCANNED 和 PREVIEWED 状态

### 步骤2：修复 PipelineController.analyzePipeline
- 文件：`backend/src/main/java/com/filemanager/backend/controller/PipelineController.java`
- 只在 autoExecute=true 时自动执行扫描和预览
- 预览完成后设置状态为 PREVIEWED
- 扫描到0个文件时直接结束

### 步骤3：完善配置快照
- 文件：`backend/src/main/java/com/filemanager/backend/model/TaskConfigSnapshot.java`
- 添加 autoExecute 和其他全局设置参数

### 步骤4：修复前端请求
- 文件：`clients/flutter-web-cli/lib/api/pipeline_service.dart`
- 发送请求时包含全局设置

### 步骤5：更新前端状态显示
- 文件：`clients/flutter-web-cli/lib/widgets/preview/task_list_widget.dart`
- 正确显示 SCANNED 和 PREVIEWED 状态

## 4. 验证测试

### 测试用例1：手动执行模式
1. 不勾选"自动执行"
2. 点击"创建任务"
3. 验证任务状态为 CREATED
4. 手动点击"扫描"
5. 验证任务状态变为 SCANNED
6. 手动点击"预览"
7. 验证任务状态变为 PREVIEWED
8. 手动点击"执行"
9. 验证任务状态变为 COMPLETED

### 测试用例2：自动执行模式
1. 勾选"自动执行"
2. 点击"创建任务"
3. 验证任务自动完成所有阶段
4. 最终状态为 COMPLETED

### 测试用例3：空扫描处理
1. 配置一个空目录
2. 创建任务（自动执行）
3. 验证扫描到0个文件后任务直接结束
4. 状态为 SCANNED

### 测试用例4：配置快照验证
1. 创建任务
2. 查看配置快照
3. 验证包含所有全局设置参数
4. 验证包含 autoExecute 参数

## 5. 影响范围

### 后端
- `TaskInfo.java`：状态枚举扩展
- `PipelineController.java`：流程控制修复
- `TaskConfigSnapshot.java`：配置快照完善
- `TaskExecutionService.java`：状态流转修复

### 前端
- `pipeline_service.dart`：请求参数完善
- `task_list_widget.dart`：状态显示修复
- `task_status.dart`：状态模型更新

## 6. 风险评估

### 低风险
- 状态枚举扩展：向后兼容，不影响现有功能
- 配置快照完善：新增字段，不影响现有数据

### 中风险
- 流程控制修改：可能影响现有任务的执行流程
- 需要充分测试各种场景

## 7. 回滚方案

如发现问题，可通过以下步骤回滚：
1. 恢复 TaskInfo.java 中的状态枚举
2. 恢复 PipelineController.java 中的流程控制逻辑
3. 重新部署后端服务
