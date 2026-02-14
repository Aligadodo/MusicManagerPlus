import 'dart:async';
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
  int _selectedTab = 0;
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
              : Column(
                  children: [
                    _buildTaskHeader(),
                    _buildTabBar(),
                    Expanded(
                      child: _buildTabContent(),
                    ),
                  ],
                ),
    );
  }

  Widget _buildTaskHeader() {
    final task = _taskInfo!;
    final createdAt = task.createdAt != null
        ? DateTime.fromMillisecondsSinceEpoch(task.createdAt!)
        : null;

    return Container(
      padding: const EdgeInsets.all(16),
      color: Colors.grey[100],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              _buildStatusIcon(),
              const SizedBox(width: 12),
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
            ],
          ),
          const SizedBox(height: 12),
          _buildProgressBar(),
          const SizedBox(height: 8),
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
          const SizedBox(height: 12),
          _buildActionButtons(),
        ],
      ),
    );
  }

  Widget _buildStatusIcon() {
    final status = _taskInfo!.status ?? '';
    IconData iconData;
    Color iconColor;

    switch (status) {
      case 'CREATED':
        iconData = Icons.folder_open;
        iconColor = Colors.blue;
        break;
      case 'SCANNING':
        iconData = Icons.scanner;
        iconColor = Colors.orange;
        break;
      case 'SCANNED':
        iconData = Icons.check_circle_outline;
        iconColor = Colors.green;
        break;
      case 'PREVIEWING':
        iconData = Icons.preview;
        iconColor = Colors.orange;
        break;
      case 'PREVIEWED':
        iconData = Icons.check_circle_outline;
        iconColor = Colors.green;
        break;
      case 'EXECUTING':
        iconData = Icons.play_circle_outline;
        iconColor = Colors.orange;
        break;
      case 'COMPLETED':
        iconData = Icons.check_circle;
        iconColor = Colors.green;
        break;
      case 'FAILED':
        iconData = Icons.error;
        iconColor = Colors.red;
        break;
      case 'CANCELLED':
        iconData = Icons.cancel;
        iconColor = Colors.grey;
        break;
      default:
        iconData = Icons.help_outline;
        iconColor = Colors.grey;
    }

    return Icon(iconData, color: iconColor, size: 40);
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
      ],
    );
  }

  bool _canCancel() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(status);
  }

  bool _canExecuteScan() {
    final status = _taskInfo?.status ?? '';
    return ['CREATED', 'SCANNED'].contains(status);
  }

  bool _canExecutePreview() {
    final status = _taskInfo?.status ?? '';
    return ['SCANNED', 'PREVIEWED'].contains(status);
  }

  bool _canExecuteTask() {
    final status = _taskInfo?.status ?? '';
    return ['PREVIEWED'].contains(status);
  }

  Widget _buildTabBar() {
    return TabBar(
      controller: TabController(length: 4, vsync: this),
      onTap: (index) {
        setState(() {
          _selectedTab = index;
        });
      },
      tabs: const [
        Tab(text: '配置'),
        Tab(text: '扫描'),
        Tab(text: '预览'),
        Tab(text: '执行'),
      ],
    );
  }

  Widget _buildTabContent() {
    switch (_selectedTab) {
      case 0:
        return _buildConfigTab();
      case 1:
        return _buildScanTab();
      case 2:
        return _buildPreviewTab();
      case 3:
        return _buildExecutionTab();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildConfigTab() {
    return Center(
      child: Text('配置信息 - ${_taskInfo?.configSnapshot?.toString() ?? "无"}'),
    );
  }

  Widget _buildScanTab() {
    return Center(
      child: Text('扫描结果 - ${_taskInfo?.stages?.scan?.toString() ?? "无"}'),
    );
  }

  Widget _buildPreviewTab() {
    return Center(
      child: Text('预览结果 - ${_taskInfo?.stages?.preview?.toString() ?? "无"}'),
    );
  }

  Widget _buildExecutionTab() {
    return Center(
      child: Text('执行结果 - ${_taskInfo?.stages?.execution?.toString() ?? "无"}'),
    );
  }

  String _formatDateTime(DateTime dateTime) {
    return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} '
        '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
  }
}
