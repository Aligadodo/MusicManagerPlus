import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class TaskDetailPage extends StatefulWidget {
  final String taskId;

  const TaskDetailPage({
    Key? key,
    required this.taskId,
  }) : super(key: key);

  @override
  State<TaskDetailPage> createState() => _TaskDetailPageState();
}

class _TaskDetailPageState extends State<TaskDetailPage>
    with SingleTickerProviderStateMixin {
  late final TaskService _taskService;
  TaskStatus? _taskInfo;
  bool _isLoading = true;
  WebSocketChannel? _webSocketChannel;
  StreamSubscription? _webSocketSubscription;
  Timer? _refreshTimer;

  @override
  void initState() {
    super.initState();
    _taskService = TaskService(ApiClient());
    _loadTaskInfo();
    _connectWebSocket();
    _startAutoRefresh();
  }

  @override
  void dispose() {
    _webSocketSubscription?.cancel();
    _webSocketChannel?.sink.close();
    _refreshTimer?.cancel();
    super.dispose();
  }

  void _startAutoRefresh() {
    _refreshTimer = Timer.periodic(const Duration(seconds: 3), (_) {
      _loadTaskInfo();
    });
  }

  Future<void> _loadTaskInfo() async {
    try {
      final taskInfo = await _taskService.getTaskInfo(widget.taskId);
      if (mounted) {
        setState(() {
          _taskInfo = taskInfo;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
        _showErrorSnackBar('加载任务信息失败: $e');
      }
    }
  }

  void _connectWebSocket() {
    try {
      _webSocketChannel = _taskService.connectTaskWebSocket(widget.taskId);
      _webSocketSubscription = _webSocketChannel!.stream.listen(
        (message) {
          _handleWebSocketMessage(message);
        },
        onError: (error) {
          print('WebSocket error: $error');
        },
        onDone: () {
          print('WebSocket connection closed');
        },
      );
    } catch (e) {
      print('Failed to connect WebSocket: $e');
    }
  }

  void _handleWebSocketMessage(dynamic message) {
    if (!mounted) return;

    try {
      final data = message as Map<String, dynamic>;
      final messageType = data['type'] as String?;
      final messageData = data['data'] as Map<String, dynamic>?;

      if (messageType == 'TASK_INFO_UPDATE' && messageData != null) {
        final updatedTask = TaskStatus.fromJson(messageData);
        if (updatedTask.taskId == widget.taskId) {
          setState(() {
            _taskInfo = updatedTask;
          });
        }
      } else if (messageType == 'TASK_PROGRESS' && messageData != null) {
        final progress = messageData['progress'] as double?;
        final currentStage = messageData['currentStage'] as String?;
        final msg = messageData['message'] as String?;
        
        if (_taskInfo != null) {
          setState(() {
            _taskInfo = TaskStatus(
              taskId: _taskInfo!.taskId,
              taskName: _taskInfo!.taskName,
              createdAt: _taskInfo!.createdAt,
              currentStage: currentStage ?? _taskInfo!.currentStage,
              status: _taskInfo!.status,
              overallProgress: progress ?? _taskInfo!.overallProgress,
              message: msg ?? _taskInfo!.message,
              configSnapshotId: _taskInfo!.configSnapshotId,
              configSnapshot: _taskInfo!.configSnapshot,
              stages: _taskInfo!.stages,
            );
          });
        }
      } else if (messageType == 'TASK_COMPLETED' && messageData != null) {
        final msg = messageData['message'] as String?;
        if (_taskInfo != null) {
          setState(() {
            _taskInfo = TaskStatus(
              taskId: _taskInfo!.taskId,
              taskName: _taskInfo!.taskName,
              createdAt: _taskInfo!.createdAt,
              currentStage: _taskInfo!.currentStage,
              status: 'COMPLETED',
              overallProgress: 100.0,
              message: msg ?? _taskInfo!.message,
              configSnapshotId: _taskInfo!.configSnapshotId,
              configSnapshot: _taskInfo!.configSnapshot,
              stages: _taskInfo!.stages,
            );
          });
        }
        _showSuccessSnackBar('任务已完成');
      } else if (messageType == 'TASK_FAILED' && messageData != null) {
        final msg = messageData['message'] as String?;
        final error = messageData['error'] as Map<String, dynamic>?;
        final errorMessage = error?['message'] as String?;
        
        if (_taskInfo != null) {
          setState(() {
            _taskInfo = TaskStatus(
              taskId: _taskInfo!.taskId,
              taskName: _taskInfo!.taskName,
              createdAt: _taskInfo!.createdAt,
              currentStage: _taskInfo!.currentStage,
              status: 'FAILED',
              overallProgress: _taskInfo!.overallProgress,
              message: msg ?? errorMessage ?? _taskInfo!.message,
              configSnapshotId: _taskInfo!.configSnapshotId,
              configSnapshot: _taskInfo!.configSnapshot,
              stages: _taskInfo!.stages,
            );
          });
        }
        _showErrorSnackBar('任务失败: ${errorMessage ?? msg ?? "未知错误"}');
      } else if (messageType == 'TASK_CANCELLED' && messageData != null) {
        final msg = messageData['message'] as String?;
        
        if (_taskInfo != null) {
          setState(() {
            _taskInfo = TaskStatus(
              taskId: _taskInfo!.taskId,
              taskName: _taskInfo!.taskName,
              createdAt: _taskInfo!.createdAt,
              currentStage: _taskInfo!.currentStage,
              status: 'CANCELLED',
              overallProgress: _taskInfo!.overallProgress,
              message: msg ?? _taskInfo!.message,
              configSnapshotId: _taskInfo!.configSnapshotId,
              configSnapshot: _taskInfo!.configSnapshot,
              stages: _taskInfo!.stages,
            );
          });
        }
        _showSuccessSnackBar('任务已取消');
      } else if (messageType == 'STAGE_STATUS' && messageData != null) {
        final stage = messageData['stage'] as String?;
        final status = messageData['status'] as String?;
        final processedCount = messageData['processedCount'] as int?;
        final totalCount = messageData['totalCount'] as int?;
        
        if (stage != null && status != null && _taskInfo != null) {
          double progress = _taskInfo!.overallProgress ?? 0.0;
          if (processedCount != null && totalCount != null && totalCount > 0) {
            progress = (processedCount / totalCount) * 100;
          }
          
          setState(() {
            _taskInfo = TaskStatus(
              taskId: _taskInfo!.taskId,
              taskName: _taskInfo!.taskName,
              createdAt: _taskInfo!.createdAt,
              currentStage: stage,
              status: _taskInfo!.status,
              overallProgress: progress,
              message: _taskInfo!.message,
              configSnapshotId: _taskInfo!.configSnapshotId,
              configSnapshot: _taskInfo!.configSnapshot,
              stages: _taskInfo!.stages,
            );
          });
        }
      }
    } catch (e) {
      print('Failed to parse WebSocket message: $e');
    }
  }

  Future<void> _executeScan() async {
    try {
      await _taskService.executeScan(widget.taskId);
      _showSuccessSnackBar('文件扫描已开始');
    } catch (e) {
      _showErrorSnackBar('启动扫描失败: $e');
    }
  }

  Future<void> _executePreview() async {
    try {
      await _taskService.executePreview(widget.taskId);
      _showSuccessSnackBar('预览分析已开始');
    } catch (e) {
      _showErrorSnackBar('启动预览失败: $e');
    }
  }

  Future<void> _executeTask() async {
    try {
      await _taskService.executeTask(widget.taskId);
      _showSuccessSnackBar('任务执行已开始');
    } catch (e) {
      _showErrorSnackBar('启动执行失败: $e');
    }
  }

  Future<void> _restartScan() async {
    final confirmed = await _showConfirmDialog('确认重新扫描', '确定要重新扫描文件吗？这将清空扫描数据。');
    if (!confirmed) return;

    try {
      await _taskService.restartScan(widget.taskId);
      _showSuccessSnackBar('重新扫描已开始');
      _loadTaskInfo();
    } catch (e) {
      _showErrorSnackBar('重新扫描失败: $e');
    }
  }

  Future<void> _restartPreview() async {
    final confirmed = await _showConfirmDialog('确认重新分析', '确定要重新分析文件吗？这将清空预览数据。');
    if (!confirmed) return;

    try {
      await _taskService.restartPreview(widget.taskId);
      _showSuccessSnackBar('重新分析已开始');
      _loadTaskInfo();
    } catch (e) {
      _showErrorSnackBar('重新分析失败: $e');
    }
  }

  Future<void> _restartExecution() async {
    final confirmed = await _showConfirmDialog('确认重新执行', '确定要重新执行任务吗？这将清空执行数据。');
    if (!confirmed) return;

    try {
      await _taskService.restartExecution(widget.taskId);
      _showSuccessSnackBar('重新执行已开始');
      _loadTaskInfo();
    } catch (e) {
      _showErrorSnackBar('重新执行失败: $e');
    }
  }

  Future<void> _deleteTask() async {
    final confirmed = await _showConfirmDialog('确认删除', '确定要删除此任务吗？此操作不可恢复。');
    if (!confirmed) return;

    try {
      await _taskService.deleteTask(widget.taskId);
      _showSuccessSnackBar('任务已删除');
      if (mounted) {
        Navigator.of(context).pop();
      }
    } catch (e) {
      _showErrorSnackBar('删除任务失败: $e');
    }
  }

  Future<void> _cancelTask() async {
    final confirmed = await _showConfirmDialog('确认取消', '确定要取消此任务吗？');
    if (!confirmed) return;

    try {
      await _taskService.cancelTask(widget.taskId);
      _showSuccessSnackBar('任务已取消');
      _loadTaskInfo();
    } catch (e) {
      _showErrorSnackBar('取消任务失败: $e');
    }
  }

  Future<void> _pauseTask() async {
    try {
      await _taskService.pauseTask(widget.taskId);
      _showSuccessSnackBar('任务已暂停');
      _loadTaskInfo();
    } catch (e) {
      _showErrorSnackBar('暂停任务失败: $e');
    }
  }

  Future<void> _resumeTask() async {
    try {
      await _taskService.resumeTask(widget.taskId);
      _showSuccessSnackBar('任务已恢复');
      _loadTaskInfo();
    } catch (e) {
      _showErrorSnackBar('恢复任务失败: $e');
    }
  }

  Future<void> _retryFailed() async {
    final confirmed = await _showConfirmDialog('确认重试', '确定要重试失败的任务吗？');
    if (!confirmed) return;

    try {
      await _taskService.retryFailed(widget.taskId);
      _showSuccessSnackBar('重试已开始');
      _loadTaskInfo();
    } catch (e) {
      _showErrorSnackBar('重试失败: $e');
    }
  }

  void _showErrorSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  void _showSuccessSnackBar(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
      ),
    );
  }

  Future<bool> _showConfirmDialog(String title, String content) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(content),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确定'),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_taskInfo?.taskName ?? '任务详情'),
        actions: [
          if (_canCancel())
            IconButton(
              icon: const Icon(Icons.cancel_outlined),
              onPressed: _cancelTask,
              tooltip: '取消任务',
            ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _taskInfo == null
              ? const Center(child: Text('任务不存在'))
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _buildTaskInfoCard(),
                      const SizedBox(height: 16),
                      _buildStageControlsCard(),
                      const SizedBox(height: 16),
                      _buildConfigSnapshotCard(),
                      const SizedBox(height: 16),
                      _buildScanResultCard(),
                      const SizedBox(height: 16),
                      _buildPreviewResultCard(),
                      const SizedBox(height: 16),
                      _buildExecutionResultCard(),
                    ],
                  ),
                ),
    );
  }

  Widget _buildTaskInfoCard() {
    final task = _taskInfo!;
    final createdAt = task.createdAt != null
        ? DateTime.fromMillisecondsSinceEpoch(task.createdAt!)
        : null;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        task.taskName ?? '未命名任务',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        task.taskId ?? '',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey[600],
                        ),
                      ),
                    ],
                  ),
                ),
                _buildStatusIcon(),
              ],
            ),
            const SizedBox(height: 16),
            _buildProgressBar(),
            const SizedBox(height: 12),
            Row(
              children: [
                Icon(Icons.access_time, size: 14, color: Colors.grey[600]),
                const SizedBox(width: 4),
                Text(
                  createdAt != null ? _formatDateTime(createdAt) : '未知时间',
                  style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(
                    task.message ?? '',
                    style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            _buildActionButtons(),
          ],
        ),
      ),
    );
  }

  Widget _buildStageControlsCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '阶段控制',
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            _buildScanStageControl(),
            const SizedBox(height: 12),
            _buildPreviewStageControl(),
            const SizedBox(height: 12),
            _buildExecutionStageControl(),
          ],
        ),
      ),
    );
  }

  Widget _buildScanStageControl() {
    final scanStage = _taskInfo?.stages?.scan;
    final canRun = _canRunScanStage();
    final canRerun = _canRerunScanStage();
    final isRunning = scanStage?.status == 'RUNNING';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.scanner, size: 20),
                const SizedBox(width: 8),
                Text(
                  '扫描阶段',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                _buildStageStatusBadge(scanStage?.status),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: [
                if (canRun && !isRunning)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.play_arrow, size: 16),
                    label: const Text('运行'),
                    onPressed: () => _runScanStage(),
                  ),
                if (canRerun && !isRunning)
                  OutlinedButton.icon(
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('重新运行'),
                    onPressed: () => _rerunScanStage(),
                  ),
                if (isRunning)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.stop, size: 16),
                    label: const Text('终止'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: () => _stopScanStage(),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPreviewStageControl() {
    final previewStage = _taskInfo?.stages?.preview;
    final canRun = _canRunPreviewStage();
    final canRerun = _canRerunPreviewStage();
    final isRunning = previewStage?.status == 'RUNNING';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.preview, size: 20),
                const SizedBox(width: 8),
                Text(
                  '分析阶段',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                _buildStageStatusBadge(previewStage?.status),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: [
                if (canRun && !isRunning)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.play_arrow, size: 16),
                    label: const Text('运行'),
                    onPressed: () => _runPreviewStage(),
                  ),
                if (canRerun && !isRunning)
                  OutlinedButton.icon(
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('重新运行'),
                    onPressed: () => _rerunPreviewStage(),
                  ),
                if (isRunning)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.stop, size: 16),
                    label: const Text('终止'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: () => _stopPreviewStage(),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildExecutionStageControl() {
    final executionStage = _taskInfo?.stages?.execution;
    final canRun = _canRunExecutionStage();
    final canRerun = _canRerunExecutionStage();
    final isRunning = executionStage?.status == 'RUNNING';

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.play_circle_outline, size: 20),
                const SizedBox(width: 8),
                Text(
                  '执行阶段',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                _buildStageStatusBadge(executionStage?.status),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              children: [
                if (canRun && !isRunning)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.play_arrow, size: 16),
                    label: const Text('运行'),
                    onPressed: () => _runExecutionStage(),
                  ),
                if (canRerun && !isRunning)
                  OutlinedButton.icon(
                    icon: const Icon(Icons.refresh, size: 16),
                    label: const Text('重新运行'),
                    onPressed: () => _rerunExecutionStage(),
                  ),
                if (isRunning)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.stop, size: 16),
                    label: const Text('终止'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: () => _stopExecutionStage(),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStageStatusBadge(String? status) {
    Color color;
    String text;

    switch (status) {
      case 'PENDING':
        color = Colors.grey;
        text = '等待中';
        break;
      case 'RUNNING':
        color = Colors.blue;
        text = '进行中';
        break;
      case 'COMPLETED':
        color = Colors.green;
        text = '已完成';
        break;
      case 'FAILED':
        color = Colors.red;
        text = '失败';
        break;
      default:
        color = Colors.grey;
        text = status ?? '未知';
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        text,
        style: const TextStyle(color: Colors.white, fontSize: 12),
      ),
    );
  }

  bool _canRunScanStage() {
    final status = _taskInfo?.status ?? '';
    return ['CREATED', 'SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED'].contains(status);
  }

  bool _canRerunScanStage() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED'].contains(status);
  }

  bool _canRunPreviewStage() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED'].contains(status);
  }

  bool _canRerunPreviewStage() {
    final status = _taskInfo?.status ?? '';
    return ['PREVIEWED', 'COMPLETED', 'FAILED'].contains(status);
  }

  bool _canRunExecutionStage() {
    final status = _taskInfo?.status ?? '';
    return ['PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED'].contains(status);
  }

  bool _canRerunExecutionStage() {
    final status = _taskInfo?.status ?? '';
    return ['COMPLETED', 'FAILED'].contains(status);
  }

  Future<void> _runScanStage() async {
    if (!_checkDependency('SCAN')) {
      _showErrorSnackBar('前置阶段未完成，无法执行扫描阶段');
      return;
    }

    try {
      await _taskService.executeScan(widget.taskId);
      _showSuccessSnackBar('扫描已开始');
    } catch (e) {
      _showErrorSnackBar('启动扫描失败: $e');
    }
  }

  Future<void> _rerunScanStage() async {
    final confirmed = await _showConfirmDialog('确认重新运行', '重新运行扫描阶段将清空后续阶段的数据，确定继续吗？');
    if (!confirmed) return;

    try {
      await _clearSubsequentStages('SCAN');
      await _taskService.restartScan(widget.taskId);
      _showSuccessSnackBar('扫描已重新开始');
    } catch (e) {
      _showErrorSnackBar('重新运行扫描失败: $e');
    }
  }

  Future<void> _stopScanStage() async {
    try {
      await _taskService.cancelStage(widget.taskId, 'scan');
      _showSuccessSnackBar('扫描已终止');
    } catch (e) {
      _showErrorSnackBar('终止扫描失败: $e');
    }
  }

  Future<void> _runPreviewStage() async {
    if (!_checkDependency('PREVIEW')) {
      _showErrorSnackBar('扫描阶段未完成，无法执行分析阶段');
      return;
    }

    try {
      await _taskService.executePreview(widget.taskId);
      _showSuccessSnackBar('分析已开始');
    } catch (e) {
      _showErrorSnackBar('启动分析失败: $e');
    }
  }

  Future<void> _rerunPreviewStage() async {
    final confirmed = await _showConfirmDialog('确认重新运行', '重新运行分析阶段将清空执行阶段的数据，确定继续吗？');
    if (!confirmed) return;

    try {
      await _clearSubsequentStages('PREVIEW');
      await _taskService.restartPreview(widget.taskId);
      _showSuccessSnackBar('分析已重新开始');
    } catch (e) {
      _showErrorSnackBar('重新运行分析失败: $e');
    }
  }

  Future<void> _stopPreviewStage() async {
    try {
      await _taskService.cancelStage(widget.taskId, 'preview');
      _showSuccessSnackBar('分析已终止');
    } catch (e) {
      _showErrorSnackBar('终止分析失败: $e');
    }
  }

  Future<void> _runExecutionStage() async {
    if (!_checkDependency('EXECUTION')) {
      _showErrorSnackBar('分析阶段未完成，无法执行执行阶段');
      return;
    }

    try {
      await _taskService.executeTask(widget.taskId);
      _showSuccessSnackBar('执行已开始');
    } catch (e) {
      _showErrorSnackBar('启动执行失败: $e');
    }
  }

  Future<void> _rerunExecutionStage() async {
    final confirmed = await _showConfirmDialog('确认重新运行', '重新运行执行阶段将清空执行结果，确定继续吗？');
    if (!confirmed) return;

    try {
      await _taskService.restartExecution(widget.taskId);
      _showSuccessSnackBar('执行已重新开始');
    } catch (e) {
      _showErrorSnackBar('重新运行执行失败: $e');
    }
  }

  Future<void> _stopExecutionStage() async {
    try {
      await _taskService.cancelStage(widget.taskId, 'execution');
      _showSuccessSnackBar('执行已终止');
    } catch (e) {
      _showErrorSnackBar('终止执行失败: $e');
    }
  }

  bool _checkDependency(String stageType) {
    switch (stageType) {
      case 'SCAN':
        return true;
      case 'PREVIEW':
        final scanStage = _taskInfo?.stages?.scan;
        return scanStage?.status == 'COMPLETED';
      case 'EXECUTION':
        final previewStage = _taskInfo?.stages?.preview;
        return previewStage?.status == 'COMPLETED';
      default:
        return false;
    }
  }

  Future<void> _clearSubsequentStages(String stageType) async {
    switch (stageType) {
      case 'SCAN':
        await _taskService.clearStageData(widget.taskId, 'preview');
        await _taskService.clearStageData(widget.taskId, 'execution');
        break;
      case 'PREVIEW':
        await _taskService.clearStageData(widget.taskId, 'execution');
        break;
      case 'EXECUTION':
        break;
    }
  }

  Widget _buildStatusIcon() {
    final status = _taskInfo!.status ?? '';
    IconData iconData;
    Color iconColor;
    String statusText;

    switch (status) {
      case 'CREATED':
        iconData = Icons.folder_open;
        iconColor = Colors.blue;
        statusText = '已创建';
        break;
      case 'SCANNING':
        iconData = Icons.scanner;
        iconColor = Colors.orange;
        statusText = '扫描中';
        break;
      case 'SCANNED':
        iconData = Icons.check_circle_outline;
        iconColor = Colors.green;
        statusText = '扫描完成';
        break;
      case 'PREVIEWING':
        iconData = Icons.preview;
        iconColor = Colors.orange;
        statusText = '预览中';
        break;
      case 'PREVIEWED':
        iconData = Icons.check_circle_outline;
        iconColor = Colors.green;
        statusText = '预览完成';
        break;
      case 'EXECUTING':
        iconData = Icons.play_circle_outline;
        iconColor = Colors.orange;
        statusText = '执行中';
        break;
      case 'COMPLETED':
        iconData = Icons.check_circle;
        iconColor = Colors.green;
        statusText = '已完成';
        break;
      case 'FAILED':
        iconData = Icons.error;
        iconColor = Colors.red;
        statusText = '失败';
        break;
      case 'CANCELLED':
        iconData = Icons.cancel;
        iconColor = Colors.grey;
        statusText = '已取消';
        break;
      default:
        iconData = Icons.help_outline;
        iconColor = Colors.grey;
        statusText = '未知';
    }

    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: iconColor.withOpacity(0.1),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Icon(iconData, color: iconColor, size: 48),
        ),
        const SizedBox(height: 8),
        Text(
          statusText,
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: iconColor,
          ),
        ),
      ],
    );
  }

  Widget _buildProgressBar() {
    final progress = _taskInfo!.overallProgress ?? 0.0;
    final currentStage = _taskInfo!.currentStage ?? '';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              currentStage,
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
            ),
            Text(
              '${progress.toStringAsFixed(1)}%',
              style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
            ),
          ],
        ),
        const SizedBox(height: 4),
        LinearProgressIndicator(
          value: progress / 100,
          backgroundColor: Colors.grey[300],
          valueColor: AlwaysStoppedAnimation<Color>(
            _getProgressColor(progress),
          ),
        ),
      ],
    );
  }

  Color _getProgressColor(double progress) {
    if (progress >= 100) return Colors.green;
    if (progress >= 50) return Colors.blue;
    return Colors.orange;
  }

  Widget _buildActionButtons() {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        if (_canExecuteScan())
          ElevatedButton.icon(
            onPressed: _executeScan,
            icon: const Icon(Icons.scanner, size: 18),
            label: const Text('扫描'),
          ),
        if (_canExecutePreview())
          ElevatedButton.icon(
            onPressed: _executePreview,
            icon: const Icon(Icons.preview, size: 18),
            label: const Text('预览'),
          ),
        if (_canExecuteTask())
          ElevatedButton.icon(
            onPressed: _executeTask,
            icon: const Icon(Icons.play_arrow, size: 18),
            label: const Text('执行'),
          ),
        if (_canPause())
          ElevatedButton.icon(
            onPressed: _pauseTask,
            icon: const Icon(Icons.pause, size: 18),
            label: const Text('暂停'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.orange,
              foregroundColor: Colors.white,
            ),
          ),
        if (_canResume())
          ElevatedButton.icon(
            onPressed: _resumeTask,
            icon: const Icon(Icons.play_arrow, size: 18),
            label: const Text('恢复'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
            ),
          ),
        if (_canRestartScan())
          ElevatedButton.icon(
            onPressed: () => _restartScan(),
            icon: const Icon(Icons.refresh, size: 18),
            label: const Text('重新扫描'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
        if (_canRestartPreview())
          ElevatedButton.icon(
            onPressed: () => _restartPreview(),
            icon: const Icon(Icons.refresh, size: 18),
            label: const Text('重新分析'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
        if (_canRestartExecution())
          ElevatedButton.icon(
            onPressed: () => _restartExecution(),
            icon: const Icon(Icons.refresh, size: 18),
            label: const Text('重新执行'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
        if (_canRetry())
          ElevatedButton.icon(
            onPressed: _retryFailed,
            icon: const Icon(Icons.refresh, size: 18),
            label: const Text('重试'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.orange,
              foregroundColor: Colors.white,
            ),
          ),
        if (_canCancel())
          ElevatedButton.icon(
            onPressed: _cancelTask,
            icon: const Icon(Icons.cancel, size: 18),
            label: const Text('取消'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
              foregroundColor: Colors.white,
            ),
          ),
        ElevatedButton.icon(
          onPressed: _deleteTask,
          icon: const Icon(Icons.delete, size: 18),
          label: const Text('删除'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.grey,
            foregroundColor: Colors.white,
          ),
        ),
      ],
    );
  }

  bool _canCancel() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(status);
  }

  bool _canPause() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(status);
  }

  bool _canResume() {
    final status = _taskInfo?.status ?? '';
    return ['CANCELLED'].contains(status);
  }

  bool _canRetry() {
    final status = _taskInfo?.status ?? '';
    return ['FAILED', 'COMPLETED'].contains(status);
  }

  bool _canExecuteScan() {
    final status = _taskInfo?.status ?? '';
    return ['CREATED', 'SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED'].contains(status);
  }

  bool _canExecutePreview() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED'].contains(status);
  }

  bool _canExecuteTask() {
    final status = _taskInfo?.status ?? '';
    return ['PREVIEWED', 'COMPLETED', 'FAILED'].contains(status);
  }

  bool _canRestartScan() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED'].contains(status);
  }

  bool _canRestartPreview() {
    final status = _taskInfo?.status ?? '';
    return ['PREVIEWED', 'COMPLETED', 'FAILED'].contains(status);
  }

  bool _canRestartExecution() {
    final status = _taskInfo?.status ?? '';
    return ['COMPLETED', 'FAILED'].contains(status);
  }

  Widget _buildConfigSnapshotCard() {
    return _CollapsibleCard(
      title: '配置快照',
      icon: Icons.settings,
      initiallyExpanded: false,
      child: _buildConfigContent(),
    );
  }

  Widget _buildConfigContent() {
    final configSnapshot = _taskInfo?.configSnapshot;
    if (configSnapshot == null) {
      return const Center(child: Text('配置快照不存在'));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildSectionTitle('快照信息'),
        const SizedBox(height: 8),
        _buildSnapshotInfo(),
        const SizedBox(height: 16),
        _buildSectionTitle('全局设置'),
        const SizedBox(height: 8),
        _buildGlobalSettings(configSnapshot.globalSettings),
        const SizedBox(height: 16),
        _buildSectionTitle('源目录配置'),
        const SizedBox(height: 8),
        _buildSourceDirectoriesList(configSnapshot.sourceDirectories),
        const SizedBox(height: 16),
        _buildSectionTitle('流水线配置'),
        const SizedBox(height: 8),
        _buildPipelineConfig(configSnapshot.pipelineConfig),
        const SizedBox(height: 16),
        _buildSectionTitle('重命名规则'),
        const SizedBox(height: 8),
        _buildRenameRules(configSnapshot.renameRules),
        const SizedBox(height: 16),
        _buildSectionTitle('前置条件'),
        const SizedBox(height: 8),
        _buildPreconditions(configSnapshot.preconditions),
        const SizedBox(height: 16),
        _buildSectionTitle('完整配置JSON'),
        const SizedBox(height: 8),
        _buildConfigJsonViewer(),
      ],
    );
  }

  Widget _buildSnapshotInfo() {
    final configSnapshotId = _taskInfo?.configSnapshotId;
    if (configSnapshotId == null || configSnapshotId.isEmpty) {
      return const Text('快照ID: 无', style: TextStyle(color: Colors.grey));
    }

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.blue[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.blue[200]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.fingerprint, size: 16, color: Colors.blue),
              const SizedBox(width: 8),
              const Text(
                '快照ID',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                  color: Colors.blue,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          SelectableText(
            configSnapshotId,
            style: const TextStyle(
              fontSize: 12,
              fontFamily: 'monospace',
              color: Colors.black87,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConfigJsonViewer() {
    final configSnapshot = _taskInfo?.configSnapshot;
    if (configSnapshot == null) {
      return const SizedBox.shrink();
    }

    final configJson = configSnapshot.toJson();
    final formattedJson = const JsonEncoder.withIndent('  ').convert(configJson);

    return _JsonViewer(
      title: '完整配置JSON',
      jsonData: formattedJson,
    );
  }

  Widget _buildStrategyConfig(String? strategyId, Map<String, dynamic>? strategyConfig) {
    if (strategyId == null) {
      return const Text('无策略配置', style: TextStyle(color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('策略ID: $strategyId',
            style: const TextStyle(fontSize: 13)),
        if (strategyConfig != null)
          Text('配置参数: ${strategyConfig.length} 项',
              style: const TextStyle(fontSize: 13)),
      ],
    );
  }

  Widget _buildSourceDirectoriesList(List<dynamic>? sourceDirs) {
    if (sourceDirs == null || sourceDirs.isEmpty) {
      return const Text('无源目录配置', style: TextStyle(color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: sourceDirs.map((dir) {
        final dirMap = dir as Map<String, dynamic>;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              const Icon(Icons.folder, size: 16, color: Colors.blue),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  dirMap['path'] ?? '',
                  style: const TextStyle(fontSize: 13),
                ),
              ),
              if (dirMap['depth'] != null)
                Text(
                  '深度: ${dirMap['depth']}',
                  style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                ),
            ],
          ),
        );
      }).toList(),
    );
  }

  Widget _buildGlobalSettings(Map<String, dynamic>? globalSettings) {
    if (globalSettings == null || globalSettings.isEmpty) {
      return const Text('无全局设置', style: TextStyle(color: Colors.grey));
    }

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey[200]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('全局设置详情', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          const SizedBox(height: 8),
          ...globalSettings.entries.map((entry) {
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 150,
                    child: Text(
                      '${entry.key}:',
                      style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 12),
                    ),
                  ),
                  Expanded(
                    child: Text(
                      entry.value?.toString() ?? 'N/A',
                      style: const TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
        ],
      ),
    );
  }

  Widget _buildPipelineConfig(dynamic pipelineConfig) {
    if (pipelineConfig == null) {
      return const Text('无流水线配置', style: TextStyle(color: Colors.grey));
    }

    final pipelineMap = pipelineConfig as Map<String, dynamic>;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('流水线ID: ${pipelineMap['pipelineId'] ?? 'N/A'}',
            style: const TextStyle(fontSize: 13)),
        Text('名称: ${pipelineMap['name'] ?? 'N/A'}',
            style: const TextStyle(fontSize: 13)),
        if (pipelineMap['description'] != null)
          Text('描述: ${pipelineMap['description']}',
              style: const TextStyle(fontSize: 13)),
      ],
    );
  }

  Widget _buildRenameRules(List<dynamic>? renameRules) {
    if (renameRules == null || renameRules.isEmpty) {
      return const Text('无重命名规则', style: TextStyle(color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: renameRules.map((rule) {
        final ruleMap = rule as Map<String, dynamic>;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              const Icon(Icons.rule, size: 16, color: Colors.purple),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      ruleMap['ruleName'] ?? '未命名规则',
                      style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
                    ),
                    Text(
                      '类型: ${ruleMap['ruleType'] ?? 'N/A'}',
                      style: TextStyle(fontSize: 12, color: Colors.grey[600]),
                    ),
                  ],
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }

  Widget _buildPreconditions(List<dynamic>? preconditions) {
    if (preconditions == null || preconditions.isEmpty) {
      return const Text('无前置条件', style: TextStyle(color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: preconditions.map((condition) {
        final conditionMap = condition as Map<String, dynamic>;
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              const Icon(Icons.check_circle_outline, size: 16, color: Colors.green),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  '条件类型: ${conditionMap['conditionType'] ?? 'N/A'}',
                  style: const TextStyle(fontSize: 13),
                ),
              ),
            ],
          ),
        );
      }).toList(),
    );
  }

  Widget _buildScanResultCard() {
    return _CollapsibleCard(
      title: '文件扫描结果',
      icon: Icons.scanner,
      initiallyExpanded: false,
      child: _buildScanContent(),
    );
  }

  Widget _buildScanContent() {
    final scanStage = _taskInfo?.stages?.scan;
    if (scanStage == null) {
      return const Center(child: Text('扫描结果不存在'));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildSectionTitle('统计信息'),
        const SizedBox(height: 8),
        _buildScanStatistics(scanStage),
        const SizedBox(height: 16),
        _buildSectionTitle('文件列表'),
        const SizedBox(height: 8),
        _buildScanFileList(scanStage),
      ],
    );
  }

  Widget _buildScanStatistics(dynamic scanStage) {
    final scanMap = scanStage as Map<String, dynamic>;
    final status = scanMap['status'] as String?;
    
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.blue[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.blue[200]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.info_outline, size: 16, color: Colors.blue[700]),
              const SizedBox(width: 8),
              Text(
                '扫描状态: ${_formatStatus(status)}',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Colors.blue[700],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: [
              _buildStatItem('总文件数', scanMap['totalFiles']?.toString() ?? '0'),
              _buildStatItem('扫描开始时间', _formatTimestamp(scanMap['scanStartTime'])),
              _buildStatItem('扫描结束时间', _formatTimestamp(scanMap['scanEndTime'])),
              _buildStatItem('扫描耗时', '${scanMap['scanDuration'] ?? 0}ms'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildScanFileList(dynamic scanStage) {
    final scanMap = scanStage as Map<String, dynamic>;
    final status = scanMap['status'] as String?;

    if (status != 'COMPLETED') {
      return Center(
        child: Column(
          children: [
            Icon(Icons.hourglass_empty, size: 48, color: Colors.grey[400]),
            const SizedBox(height: 8),
            Text(
              '扫描未完成',
              style: TextStyle(color: Colors.grey[600]),
            ),
          ],
        ),
      );
    }

    return Container(
      height: 200,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey[300]!),
        borderRadius: BorderRadius.circular(4),
      ),
      child: const Center(
        child: Text('文件列表功能待实现'),
      ),
    );
  }

  Widget _buildPreviewResultCard() {
    return _CollapsibleCard(
      title: '预览分析结果',
      icon: Icons.preview,
      initiallyExpanded: false,
      child: _buildPreviewContent(),
    );
  }

  Widget _buildPreviewContent() {
    final previewStage = _taskInfo?.stages?.preview;
    if (previewStage == null) {
      return const Center(child: Text('预览结果不存在'));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildSectionTitle('统计信息'),
        const SizedBox(height: 8),
        _buildPreviewStatistics(previewStage),
        const SizedBox(height: 16),
        _buildSectionTitle('变更记录列表'),
        const SizedBox(height: 8),
        _buildPreviewRecordList(previewStage),
      ],
    );
  }

  Widget _buildPreviewStatistics(dynamic previewStage) {
    final previewMap = previewStage as Map<String, dynamic>;
    final status = previewMap['status'] as String?;
    
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.orange[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.orange[200]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.info_outline, size: 16, color: Colors.orange[700]),
              const SizedBox(width: 8),
              Text(
                '预览状态: ${_formatStatus(status)}',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Colors.orange[700],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: [
              _buildStatItem('总文件数', previewMap['totalFiles']?.toString() ?? '0'),
              _buildStatItem('已处理文件数', previewMap['processedFiles']?.toString() ?? '0'),
              _buildStatItem('变更文件数', previewMap['changedFiles']?.toString() ?? '0'),
              _buildStatItem('未变更文件数', previewMap['unchangedFiles']?.toString() ?? '0'),
              _buildStatItem('预览开始时间', _formatTimestamp(previewMap['previewStartTime'])),
              _buildStatItem('预览结束时间', _formatTimestamp(previewMap['previewEndTime'])),
              _buildStatItem('预览耗时', '${previewMap['previewDuration'] ?? 0}ms'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPreviewRecordList(dynamic previewStage) {
    final previewMap = previewStage as Map<String, dynamic>;
    final status = previewMap['status'] as String?;

    if (status != 'COMPLETED') {
      return Center(
        child: Column(
          children: [
            Icon(Icons.hourglass_empty, size: 48, color: Colors.grey[400]),
            const SizedBox(height: 8),
            Text(
              '预览未完成',
              style: TextStyle(color: Colors.grey[600]),
            ),
          ],
        ),
      );
    }

    return Container(
      height: 200,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey[300]!),
        borderRadius: BorderRadius.circular(4),
      ),
      child: const Center(
        child: Text('变更记录列表功能待实现'),
      ),
    );
  }

  Widget _buildExecutionResultCard() {
    return _CollapsibleCard(
      title: '执行结果',
      icon: Icons.play_circle,
      initiallyExpanded: false,
      child: _buildExecutionContent(),
    );
  }

  Widget _buildExecutionContent() {
    final executionStage = _taskInfo?.stages?.execution;
    if (executionStage == null) {
      return const Center(child: Text('执行结果不存在'));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildSectionTitle('执行历史'),
        const SizedBox(height: 8),
        _buildExecutionHistory(executionStage),
        const SizedBox(height: 16),
        _buildSectionTitle('当前执行结果'),
        const SizedBox(height: 8),
        _buildExecutionResultList(executionStage),
      ],
    );
  }

  Widget _buildExecutionHistory(dynamic executionStage) {
    final executionMap = executionStage as Map<String, dynamic>;
    final executionCount = executionMap['executionCount'] as int? ?? 0;
    final currentExecution = executionMap['currentExecution'] as String?;

    if (executionCount == 0) {
      return const Center(
        child: Text('暂无执行历史', style: TextStyle(color: Colors.grey)),
      );
    }

    return DefaultTabController(
      length: executionCount,
      child: Column(
        children: [
          TabBar(
            isScrollable: true,
            tabs: List.generate(executionCount, (index) {
              final executionNum = 'execution_${(index + 1).toString().padLeft(3, '0')}';
              return Tab(text: '第${index + 1}次执行');
            }),
          ),
          Container(
            height: 150,
            child: TabBarView(
              children: List.generate(executionCount, (index) {
                return Center(
                  child: Text('执行历史详情待实现\n第${index + 1}次执行'),
                );
              }),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildExecutionResultList(dynamic executionStage) {
    final executionMap = executionStage as Map<String, dynamic>;
    final status = executionMap['status'] as String?;
    
    if (status != 'COMPLETED' && status != 'RUNNING') {
      return Center(
        child: Column(
          children: [
            Icon(Icons.hourglass_empty, size: 48, color: Colors.grey[400]),
            const SizedBox(height: 8),
            Text(
              '执行未开始',
              style: TextStyle(color: Colors.grey[600]),
            ),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.green[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.green[200]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.info_outline, size: 16, color: Colors.green[700]),
              const SizedBox(width: 8),
              Text(
                '执行状态: ${_formatStatus(status)}',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Colors.green[700],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: [
              _buildStatItem('执行次数', executionMap['executionCount']?.toString() ?? '0'),
              _buildStatItem('已执行文件数', executionMap['executedFiles']?.toString() ?? '0'),
              _buildStatItem('成功数', executionMap['successCount']?.toString() ?? '0'),
              _buildStatItem('失败数', executionMap['failedCount']?.toString() ?? '0'),
              _buildStatItem('执行开始时间', _formatTimestamp(executionMap['executionStartTime'])),
              _buildStatItem('执行结束时间', _formatTimestamp(executionMap['executionEndTime'])),
              _buildStatItem('执行耗时', '${executionMap['executionDuration'] ?? 0}ms'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSectionTitle(String title) {
    return Text(
      title,
      style: const TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.bold,
      ),
    );
  }

  Widget _buildStatItem(String label, String value) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text('$label: ', style: const TextStyle(fontSize: 12, color: Colors.grey)),
        Text(value, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w500)),
      ],
    );
  }

  String _formatDateTime(DateTime dateTime) {
    return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} '
        '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
  }

  String _formatTimestamp(dynamic timestamp) {
    if (timestamp == null) return 'N/A';
    final time = timestamp as int;
    if (time == 0) return 'N/A';
    return _formatDateTime(DateTime.fromMillisecondsSinceEpoch(time));
  }

  String _formatStatus(dynamic status) {
    if (status == null) return 'N/A';
    final statusStr = status as String;
    switch (statusStr) {
      case 'PENDING':
        return '等待中';
      case 'RUNNING':
        return '运行中';
      case 'COMPLETED':
        return '已完成';
      case 'FAILED':
        return '失败';
      case 'SKIPPED':
        return '已跳过';
      default:
        return statusStr;
    }
  }
}

class _CollapsibleCard extends StatefulWidget {
  final String title;
  final IconData icon;
  final Widget child;
  final bool initiallyExpanded;

  const _CollapsibleCard({
    required this.title,
    required this.icon,
    required this.child,
    this.initiallyExpanded = false,
  });

  @override
  State<_CollapsibleCard> createState() => _CollapsibleCardState();
}

class _CollapsibleCardState extends State<_CollapsibleCard> {
  late bool _isExpanded;

  @override
  void initState() {
    super.initState();
    _isExpanded = widget.initiallyExpanded;
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        children: [
          InkWell(
            onTap: () {
              setState(() {
                _isExpanded = !_isExpanded;
              });
            },
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  Icon(widget.icon, size: 20, color: Colors.blue),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      widget.title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  Icon(
                    _isExpanded ? Icons.expand_less : Icons.expand_more,
                    color: Colors.grey,
                  ),
                ],
              ),
            ),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 300),
            child: _isExpanded
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: widget.child,
                  )
                : const SizedBox.shrink(),
          ),
        ],
      ),
    );
  }
}

class _JsonViewer extends StatefulWidget {
  final String title;
  final String jsonData;

  const _JsonViewer({
    Key? key,
    required this.title,
    required this.jsonData,
  }) : super(key: key);

  @override
  State<_JsonViewer> createState() => _JsonViewerState();
}

class _JsonViewerState extends State<_JsonViewer> {
  bool _isExpanded = false;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        children: [
          InkWell(
            onTap: () {
              setState(() {
                _isExpanded = !_isExpanded;
              });
            },
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  const Icon(Icons.code, size: 20, color: Colors.blue),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      widget.title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  Icon(
                    _isExpanded ? Icons.expand_less : Icons.expand_more,
                    color: Colors.grey,
                  ),
                ],
              ),
            ),
          ),
          AnimatedSize(
            duration: const Duration(milliseconds: 300),
            child: _isExpanded
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            ElevatedButton.icon(
                              onPressed: () {
                                _copyJsonToClipboard();
                              },
                              icon: const Icon(Icons.copy, size: 16),
                              label: const Text('复制'),
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.blue,
                                foregroundColor: Colors.white,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 16),
                        Container(
                          width: double.infinity,
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.grey[100],
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            widget.jsonData,
                            style: const TextStyle(
                              fontFamily: 'monospace',
                              fontSize: 12,
                            ),
                          ),
                        ),
                      ],
                    ),
                  )
                : const SizedBox.shrink(),
          ),
        ],
      ),
    );
  }

  void _copyJsonToClipboard() {
    // TODO: 实现复制到剪贴板功能
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('已复制到剪贴板'),
        backgroundColor: Colors.green,
      ),
    );
  }
}
