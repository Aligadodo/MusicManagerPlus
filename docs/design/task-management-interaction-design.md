# 任务管理交互设计文档

## 概述

本文档详细描述了任务管理系统的完整交互设计，包括任务层面的操作、阶段层面的操作、依赖关系管理以及不同层面的操作按钮设计。

## 核心概念

### 任务生命周期

任务的生命周期包括以下基本操作：
- **新增**：创建新任务
- **修改**：终止、重新运行等操作
- **删除**：删除任务及相关数据

### 任务阶段

每个任务包含三个阶段：
1. **扫描阶段**：扫描文件，收集文件信息
2. **分析阶段**：分析文件变更，生成预览数据
3. **执行阶段**：根据分析结果执行实际操作

### 阶段依赖关系

```
扫描阶段 → 分析阶段 → 执行阶段
```

- 分析阶段依赖扫描阶段的结果
- 执行阶段依赖分析阶段的变更数据
- 只有前置阶段完成，才能执行后续阶段

## 交互设计

### 1. 任务创建

#### 1.1 创建入口

**入口位置**：
- 主页面：点击"创建任务"按钮
- 任务列表页：点击右下角浮动按钮（+）

**创建流程**：
1. 用户点击创建任务
2. 进入任务配置页面
3. 选择策略、配置参数
4. 点击"预览"按钮（可选）
   - 自动创建预览任务
   - 执行扫描和分析阶段
   - 展示预览结果
5. 点击"创建任务"按钮
   - 创建正式任务
   - 任务状态为"已创建"（CREATED）

**UI设计**：
```dart
// 任务配置页面
Widget build(BuildContext context) {
  return Scaffold(
    appBar: AppBar(
      title: Text('创建任务'),
      actions: [
        TextButton(
          onPressed: _previewTask,
          child: Text('预览'),
        ),
      ],
    ),
    body: Column(
      children: [
        // 策略选择
        // 参数配置
        // 源目录选择
        Expanded(child: Container()),
        // 底部操作按钮
        Padding(
          padding: EdgeInsets.all(16),
          child: Row(
            children: [
              Expanded(
                child: ElevatedButton(
                  onPressed: _createTask,
                  child: Text('创建任务'),
                ),
              ),
            ],
          ),
        ),
      ],
    ),
  );
}
```

### 2. 任务列表页面

#### 2.1 任务卡片设计

每个任务卡片显示：
- 任务名称
- 任务状态（带图标和颜色）
- 当前阶段
- 进度条
- 创建时间
- 快速操作按钮

**UI设计**：
```dart
Widget _buildTaskCard(TaskStatus task) {
  return Card(
    child: InkWell(
      onTap: () => _navigateToTaskDetail(task.taskId),
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 任务头部
            Row(
              children: [
                _buildStatusIcon(task.status),
                SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        task.taskName,
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        _formatTime(task.createdAt),
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ),
                ),
                // 快速操作按钮
                _buildQuickActions(task),
              ],
            ),
            SizedBox(height: 12),
            // 任务状态和进度
            Row(
              children: [
                Text(
                  '当前阶段: ${_getStageText(task.currentStage)}',
                  style: TextStyle(fontSize: 14),
                ),
                Spacer(),
                Text(
                  '${(task.progress * 100).toInt()}%',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
            SizedBox(height: 8),
            LinearProgressIndicator(
              value: task.progress,
              backgroundColor: Colors.grey[200],
              valueColor: AlwaysStoppedAnimation<Color>(
                _getProgressColor(task.status),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

Widget _buildQuickActions(TaskStatus task) {
  return Row(
    children: [
      // 重新运行按钮
      if (_canRerun(task))
        IconButton(
          icon: Icon(Icons.refresh),
          onPressed: () => _rerunTask(task.taskId),
          tooltip: '重新运行',
        ),
      // 终止按钮
      if (_canCancel(task))
        IconButton(
          icon: Icon(Icons.stop),
          onPressed: () => _cancelTask(task.taskId),
          tooltip: '终止',
        ),
      // 删除按钮
      IconButton(
        icon: Icon(Icons.delete),
        onPressed: () => _deleteTask(task.taskId),
        tooltip: '删除',
      ),
    ],
  );
}
```

#### 2.2 批量操作

**批量选择模式**：
- 长按任务卡片进入选择模式
- 点击任务进行选择/取消选择
- 选中任务显示蓝色边框

**批量操作按钮**：
- 全选/取消全选
- 批量删除
- 批量终止（可选）
- 批量重新运行（可选）

**UI设计**：
```dart
// AppBar设计
Widget _buildAppBar() {
  return AppBar(
    title: Text(_isSelectionMode ? '已选择 ${_selectedTaskIds.length}' : '任务列表'),
    actions: [
      if (_isSelectionMode) ...[
        IconButton(
          icon: Icon(Icons.select_all),
          onPressed: _selectAll,
          tooltip: _selectedTaskIds.length == _tasks.length ? '取消全选' : '全选',
        ),
        IconButton(
          icon: Icon(Icons.delete),
          onPressed: _batchDelete,
          tooltip: '批量删除',
        ),
        IconButton(
          icon: Icon(Icons.close),
          onPressed: _exitSelectionMode,
          tooltip: '退出选择模式',
        ),
      ] else ...[
        IconButton(
          icon: Icon(Icons.checklist),
          onPressed: _enterSelectionMode,
          tooltip: '批量操作',
        ),
        IconButton(
          icon: Icon(Icons.delete_sweep),
          onPressed: _clearAllTasks,
          tooltip: '清空所有任务',
        ),
      ],
    ],
  );
}
```

### 3. 任务详情页面

#### 3.1 页面结构

```
┌─────────────────────────────────┐
│ 任务信息                       │
│ - 任务名称                     │
│ - 任务ID                       │
│ - 创建时间                     │
│ - 当前状态                     │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ 阶段控制                       │
│ - 扫描阶段                     │
│ - 分析阶段                     │
│ - 执行阶段                     │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ 阶段详情                       │
│ - 扫描阶段卡片                 │
│ - 分析阶段卡片                 │
│ - 执行阶段卡片                 │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│ 任务操作                       │
│ - 重新运行                     │
│ - 终止                         │
│ - 删除                         │
└─────────────────────────────────┘
```

#### 3.2 阶段控制

每个阶段都有独立的控制区域，显示：
- 阶段名称
- 阶段状态
- 阶段操作按钮

**阶段操作按钮规则**：
- **扫描阶段**：
  - 运行：任务已创建、扫描已完成、分析已完成、执行已完成、失败、已取消
  - 重新运行：扫描已完成、分析已完成、执行已完成、失败
  - 跳过：不适用（扫描是必需的）

- **分析阶段**：
  - 运行：扫描已完成、分析已完成、执行已完成、失败、已取消
  - 重新运行：分析已完成、执行已完成、失败
  - 跳过：不适用（分析是必需的）

- **执行阶段**：
  - 运行：分析已完成、执行已完成、失败、已取消
  - 重新运行：执行已完成、失败
  - 跳过：不适用（执行是必需的）

**UI设计**：
```dart
Widget _buildStageControl(StageType stageType) {
  final stage = _getStage(stageType);
  final canRun = _canRunStage(stageType);
  final canRerun = _canRerunStage(stageType);
  final isRunning = stage?.status == 'RUNNING';

  return Card(
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 阶段标题和状态
          Row(
            children: [
              Icon(_getStageIcon(stageType)),
              SizedBox(width: 8),
              Text(
                _getStageName(stageType),
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Spacer(),
              _buildStageStatusBadge(stage?.status),
            ],
          ),
          SizedBox(height: 16),
          // 阶段操作按钮
          Row(
            children: [
              if (canRun && !isRunning)
                ElevatedButton.icon(
                  icon: Icon(Icons.play_arrow),
                  label: Text('运行'),
                  onPressed: () => _runStage(stageType),
                ),
              if (canRerun && !isRunning)
                OutlinedButton.icon(
                  icon: Icon(Icons.refresh),
                  label: Text('重新运行'),
                  onPressed: () => _rerunStage(stageType),
                ),
              if (isRunning)
                ElevatedButton.icon(
                  icon: Icon(Icons.stop),
                  label: Text('终止'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: () => _stopStage(stageType),
                ),
            ],
          ),
          // 阶段进度
          if (isRunning) ...[
            SizedBox(height: 16),
            LinearProgressIndicator(
              value: stage?.progress ?? 0,
            ),
            SizedBox(height: 8),
            Text(
              '${((stage?.progress ?? 0) * 100).toInt()}%',
              style: TextStyle(fontSize: 12),
            ),
          ],
        ],
      ),
    ),
  );
}

bool _canRunStage(StageType stageType) {
  final taskStatus = _taskInfo?.status;
  final currentStage = _taskInfo?.currentStage;
  
  switch (stageType) {
    case StageType.scan:
      return ['CREATED', 'SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED']
          .contains(taskStatus);
    case StageType.preview:
      return ['SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED']
          .contains(taskStatus);
    case StageType.execution:
      return ['PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED']
          .contains(taskStatus);
  }
}

bool _canRerunStage(StageType stageType) {
  final taskStatus = _taskInfo?.status;
  
  switch (stageType) {
    case StageType.scan:
      return ['SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED'].contains(taskStatus);
    case StageType.preview:
      return ['PREVIEWED', 'COMPLETED', 'FAILED'].contains(taskStatus);
    case StageType.execution:
      return ['COMPLETED', 'FAILED'].contains(taskStatus);
  }
}
```

#### 3.3 阶段详情卡片

每个阶段的详细信息卡片显示：
- 阶段状态
- 开始/结束时间
- 耗时
- 统计信息
- 错误信息（如果有）

**UI设计**：
```dart
Widget _buildStageDetailCard(StageType stageType) {
  final stage = _getStage(stageType);
  
  if (stage == null) {
    return Card(
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Text(
          '该阶段尚未执行',
          style: TextStyle(color: Colors.grey),
        ),
      ),
    );
  }

  return Card(
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 阶段标题
          Text(
            '${_getStageName(stageType)}详情',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          SizedBox(height: 16),
          // 阶段状态
          _buildStageStatusInfo(stage),
          SizedBox(height: 16),
          // 阶段统计
          _buildStageStatistics(stageType, stage),
          SizedBox(height: 16),
          // 错误信息
          if (stage.errorMessage != null)
            _buildErrorMessage(stage.errorMessage!),
        ],
      ),
    ),
  );
}
```

#### 3.4 任务操作

任务级别的操作按钮：
- **重新运行**：适用于已完成、失败、已取消的任务
- **终止**：适用于正在运行的任务
- **删除**：适用于所有任务

**UI设计**：
```dart
Widget _buildTaskActions() {
  return Card(
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '任务操作',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
          SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              if (_canRerunTask())
                ElevatedButton.icon(
                  icon: Icon(Icons.refresh),
                  label: Text('重新运行'),
                  onPressed: _rerunTask,
                ),
              if (_canCancelTask())
                ElevatedButton.icon(
                  icon: Icon(Icons.stop),
                  label: Text('终止'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: _cancelTask,
                ),
              OutlinedButton.icon(
                icon: Icon(Icons.delete),
                label: Text('删除'),
                onPressed: _deleteTask,
              ),
            ],
          ),
        ],
      ),
    ),
  );
}
```

### 4. 阶段依赖管理

#### 4.1 依赖检查

在执行某个阶段前，检查前置阶段是否完成：

```dart
bool _checkDependency(StageType stageType) {
  switch (stageType) {
    case StageType.scan:
      return true; // 扫描阶段无依赖
    case StageType.preview:
      final scanStage = _getStage(StageType.scan);
      return scanStage?.status == 'COMPLETED';
    case StageType.execution:
      final previewStage = _getStage(StageType.preview);
      return previewStage?.status == 'COMPLETED';
  }
}

Future<void> _runStage(StageType stageType) async {
  if (!_checkDependency(stageType)) {
    _showErrorSnackBar('前置阶段未完成，无法执行此阶段');
    return;
  }

  try {
    switch (stageType) {
      case StageType.scan:
        await _taskService.executeScan(widget.taskId);
        break;
      case StageType.preview:
        await _taskService.executePreview(widget.taskId);
        break;
      case StageType.execution:
        await _taskService.executeTask(widget.taskId);
        break;
    }
    _showSuccessSnackBar('${_getStageName(stageType)}已开始');
  } catch (e) {
    _showErrorSnackBar('启动${_getStageName(stageType)}失败: $e');
  }
}
```

#### 4.2 阶段数据清理

重新运行某个阶段时，清理后续阶段的数据：

```dart
Future<void> _rerunStage(StageType stageType) async {
  final confirmed = await _showConfirmDialog(
    '确认重新运行',
    '重新运行${_getStageName(stageType)}将清空后续阶段的数据，确定继续吗？',
  );
  
  if (!confirmed) return;

  try {
    // 清理后续阶段数据
    await _clearSubsequentStages(stageType);
    
    // 重新运行阶段
    switch (stageType) {
      case StageType.scan:
        await _taskService.restartScan(widget.taskId);
        break;
      case StageType.preview:
        await _taskService.restartPreview(widget.taskId);
        break;
      case StageType.execution:
        await _taskService.restartExecution(widget.taskId);
        break;
    }
    _showSuccessSnackBar('${_getStageName(stageType)}已重新开始');
  } catch (e) {
    _showErrorSnackBar('重新运行${_getStageName(stageType)}失败: $e');
  }
}

Future<void> _clearSubsequentStages(StageType stageType) async {
  switch (stageType) {
    case StageType.scan:
      // 清理分析和执行阶段数据
      await _taskService.clearStageData(widget.taskId, 'preview');
      await _taskService.clearStageData(widget.taskId, 'execution');
      break;
    case StageType.preview:
      // 清理执行阶段数据
      await _taskService.clearStageData(widget.taskId, 'execution');
      break;
    case StageType.execution:
      // 无需清理
      break;
  }
}
```

### 5. 任务状态设计

#### 5.1 任务状态枚举

```dart
enum TaskStatus {
  CREATED,      // 已创建
  SCANNING,     // 扫描中
  SCANNED,      // 已扫描
  PREVIEWING,   // 分析中
  PREVIEWED,    // 已分析
  EXECUTING,    // 执行中
  COMPLETED,    // 已完成
  FAILED,       // 失败
  CANCELLED,    // 已取消
}
```

#### 5.2 阶段状态枚举

```dart
enum StageStatus {
  PENDING,      // 等待中
  RUNNING,      // 运行中
  COMPLETED,    // 已完成
  FAILED,       // 失败
  CANCELLED,    // 已取消
}
```

#### 5.3 状态转换规则

```
CREATED → SCANNING → SCANNED → PREVIEWING → PREVIEWED → EXECUTING → COMPLETED
                    ↓            ↓              ↓
                  FAILED        FAILED         FAILED
                    ↓            ↓              ↓
                  CANCELLED    CANCELLED      CANCELLED
```

### 6. 后端接口设计

#### 6.1 任务管理接口

**创建任务**
```
POST /api/tasks
Request Body:
{
  "strategyId": "strategy-123",
  "filePaths": ["/path/to/files"],
  "strategyConfig": {...},
  "taskName": "任务名称",
  "description": "任务描述"
}

Response:
{
  "success": true,
  "data": {
    "taskId": "task-123",
    "taskName": "任务名称",
    "status": "CREATED",
    ...
  }
}
```

**删除任务**
```
DELETE /api/tasks/{taskId}

Response:
{
  "success": true,
  "message": "任务已删除"
}
```

**批量删除任务**
```
DELETE /api/tasks/batch
Request Body:
{
  "taskIds": ["task-1", "task-2", "task-3"]
}

Response:
{
  "success": true,
  "data": {
    "successCount": 2,
    "failCount": 1,
    "failedTasks": ["task-3"]
  }
}
```

**清空所有任务**
```
DELETE /api/tasks/clear

Response:
{
  "success": true,
  "data": {
    "deletedCount": 100
  }
}
```

**终止任务**
```
POST /api/tasks/{taskId}/cancel

Response:
{
  "success": true,
  "message": "任务已终止"
}
```

**重新运行任务**
```
POST /api/tasks/{taskId}/rerun

Response:
{
  "success": true,
  "message": "任务已重新开始"
}
```

#### 6.2 阶段管理接口

**运行扫描阶段**
```
POST /api/tasks/{taskId}/scan

Response:
{
  "success": true,
  "message": "扫描已开始"
}
```

**运行分析阶段**
```
POST /api/tasks/{taskId}/preview

Response:
{
  "success": true,
  "message": "分析已开始"
}
```

**运行执行阶段**
```
POST /api/tasks/{taskId}/execute

Response:
{
  "success": true,
  "message": "执行已开始"
}
```

**重新运行扫描阶段**
```
POST /api/tasks/{taskId}/restart/scan

Response:
{
  "success": true,
  "message": "扫描已重新开始"
}
```

**重新运行分析阶段**
```
POST /api/tasks/{taskId}/restart/preview

Response:
{
  "success": true,
  "message": "分析已重新开始"
}
```

**重新运行执行阶段**
```
POST /api/tasks/{taskId}/restart/execution

Response:
{
  "success": true,
  "message": "执行已重新开始"
}
```

**终止阶段**
```
POST /api/tasks/{taskId}/stage/{stageType}/cancel

Response:
{
  "success": true,
  "message": "阶段已终止"
}
```

**清理阶段数据**
```
DELETE /api/tasks/{taskId}/stage/{stageType}/data

Response:
{
  "success": true,
  "message": "阶段数据已清理"
}
```

#### 6.3 任务查询接口

**获取任务列表**
```
GET /api/tasks?page=1&pageSize=20&status=COMPLETED

Response:
{
  "success": true,
  "data": {
    "total": 100,
    "totalPages": 5,
    "page": 1,
    "pageSize": 20,
    "list": [...]
  }
}
```

**获取任务详情**
```
GET /api/tasks/{taskId}

Response:
{
  "success": true,
  "data": {
    "taskId": "task-123",
    "taskName": "任务名称",
    "status": "COMPLETED",
    "currentStage": "EXECUTION",
    "progress": 1.0,
    "createdAt": 1234567890,
    "updatedAt": 1234567890,
    "stages": {
      "scan": {...},
      "preview": {...},
      "execution": {...}
    }
  }
}
```

**获取任务进度**
```
GET /api/tasks/{taskId}/progress

Response:
{
  "success": true,
  "data": {
    "taskId": "task-123",
    "currentStage": "EXECUTION",
    "overallProgress": 0.75,
    "stageProgress": {
      "scan": 1.0,
      "preview": 1.0,
      "execution": 0.75
    }
  }
}
```

### 7. WebSocket实时通知

#### 7.1 任务状态更新

```json
{
  "type": "TASK_STATUS_UPDATE",
  "data": {
    "taskId": "task-123",
    "status": "SCANNING",
    "currentStage": "SCAN",
    "timestamp": 1234567890
  }
}
```

#### 7.2 阶段状态更新

```json
{
  "type": "STAGE_STATUS_UPDATE",
  "data": {
    "taskId": "task-123",
    "stageType": "SCAN",
    "status": "RUNNING",
    "progress": 0.5,
    "timestamp": 1234567890
  }
}
```

#### 7.3 任务进度更新

```json
{
  "type": "TASK_PROGRESS",
  "data": {
    "taskId": "task-123",
    "currentStage": "EXECUTION",
    "overallProgress": 0.75,
    "message": "正在处理文件...",
    "timestamp": 1234567890
  }
}
```

#### 7.4 任务完成通知

```json
{
  "type": "TASK_COMPLETED",
  "data": {
    "taskId": "task-123",
    "status": "COMPLETED",
    "message": "任务已完成",
    "timestamp": 1234567890
  }
}
```

#### 7.5 任务失败通知

```json
{
  "type": "TASK_FAILED",
  "data": {
    "taskId": "task-123",
    "status": "FAILED",
    "errorMessage": "执行失败：文件不存在",
    "timestamp": 1234567890
  }
}
```

## 总结

本设计文档详细描述了任务管理系统的完整交互设计，包括：

1. **任务层面的操作**：新增、修改（终止、重新运行）、删除
2. **阶段层面的操作**：每个阶段独立运行、重新运行、终止
3. **阶段依赖管理**：扫描→分析→执行的依赖关系
4. **不同层面的操作按钮设计**：任务列表、任务详情、阶段控制
5. **后端接口设计**：完整的REST API接口
6. **实时通知机制**：WebSocket推送任务状态和进度

通过这个设计，用户可以：
- 灵活地创建和管理任务
- 在任意阶段重新执行任务
- 查看详细的任务执行信息
- 批量管理历史任务
- 实时了解任务进度和状态

系统提供了完整的任务生命周期管理，满足用户的各种操作需求。
