import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'dart:convert';

class TaskMonitorPage extends ConsumerStatefulWidget {
  const TaskMonitorPage({super.key});

  @override
  ConsumerState<TaskMonitorPage> createState() => _TaskMonitorPageState();
}

class _TaskMonitorPageState extends ConsumerState<TaskMonitorPage> {
  late ApiClient _apiClient;
  late TaskService _taskService;
  List<TaskStatus> _tasks = [];
  bool _isLoading = false;
  String _errorMessage = '';
  final Map<String, WebSocketChannel> _webSocketChannels = {};

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient();
    _taskService = TaskService(_apiClient);
    _loadTasks();
  }

  @override
  void dispose() {
    for (final channel in _webSocketChannels.values) {
      channel.sink.close();
    }
    super.dispose();
  }

  Future<void> _loadTasks() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final tasks = await _taskService.getTasks();
      setState(() {
        _tasks = tasks;
      });

      for (final task in tasks) {
        if (task.status == TaskStatusEnum.RUNNING) {
          _connectToTaskWebSocket(task.taskId);
        }
      }
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load tasks: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  void _connectToTaskWebSocket(String taskId) {
    if (_webSocketChannels.containsKey(taskId)) {
      return;
    }

    final channel = _taskService.connectTaskWebSocket(taskId);

    channel.stream.listen(
      (message) {
        final data = jsonDecode(message as String);
        final updatedTask = TaskStatus.fromJson(data);
        setState(() {
          final index = _tasks.indexWhere((t) => t.taskId == taskId);
          if (index != -1) {
            _tasks[index] = updatedTask;
          }

          if (updatedTask.status.isFinalState) {
            _webSocketChannels[taskId]?.sink.close();
            _webSocketChannels.remove(taskId);
          }
        });
      },
      onError: (error) {
        print('WebSocket error: $error');
        _webSocketChannels.remove(taskId);
      },
      onDone: () {
        _webSocketChannels.remove(taskId);
      },
    );

    _webSocketChannels[taskId] = channel;
  }

  Future<void> _executeTask(String taskId) async {
    try {
      await _taskService.executeTask(taskId);
      _connectToTaskWebSocket(taskId);
      await _loadTasks();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to execute task: $e';
      });
    }
  }

  Future<void> _cancelTask(String taskId) async {
    try {
      await _taskService.cancelTask(taskId);
      _webSocketChannels[taskId]?.sink.close();
      _webSocketChannels.remove(taskId);
      await _loadTasks();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to cancel task: $e';
      });
    }
  }

  void _showChangeDetails(TaskStatus task) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('任务详情: ${task.taskId}'),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('状态: ${_getStatusText(task.status)}'),
              Text('进度: ${(task.progress * 100).toStringAsFixed(1)}%'),
              Text('消息: ${task.message}'),
              const SizedBox(height: 16),
              const Text(
                '变更记录',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              if (task.changes.isEmpty)
                const Text('暂无变更记录')
              else
                Expanded(
                  child: ListView.builder(
                    shrinkWrap: true,
                    itemCount: task.changes.length,
                    itemBuilder: (context, index) {
                      final change = task.changes[index];
                      return Card(
                        margin: const EdgeInsets.symmetric(vertical: 4),
                        child: ListTile(
                          leading: Icon(
                            _getOperationIcon(change.operationType),
                            color: _getOperationColor(change.operationType),
                          ),
                          title: Text(change.originalName),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('新名称: ${change.newName}'),
                              if (change.reason != null && change.reason!.isNotEmpty)
                                Text('原因: ${change.reason}'),
                              if (change.extraParams != null && change.extraParams!.isNotEmpty)
                                Text('额外参数: ${change.extraParams}'),
                            ],
                          ),
                          trailing: SizedBox(
                            width: 100,
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                Icon(
                                  _getStatusIcon(change.status),
                                  size: 16,
                                  color: _getChangeStatusColor(change.status),
                                ),
                                const SizedBox(width: 4),
                                Text(change.status),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('任务监控'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            Navigator.pop(context);
          },
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadTasks,
          ),
        ],
      ),
      body: Container(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_errorMessage.isNotEmpty)
              Container(
                padding: const EdgeInsets.all(10),
                color: Colors.red[100],
                child: Text(
                  _errorMessage,
                  style: const TextStyle(color: Colors.red),
                ),
              ),

            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              )
            else if (_tasks.isEmpty)
              const Center(
                child: Text('暂无任务'),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: _tasks.length,
                  itemBuilder: (context, index) {
                    final task = _tasks[index];
                    return Container(
                      margin: const EdgeInsets.symmetric(vertical: 6),
                      decoration: BoxDecoration(
                        border: Border.all(color: Colors.grey.shade200),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Expanded(
                                  child: Text(
                                    task.taskId,
                                    style: const TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                                Chip(
                                  label: Text(
                                    _getStatusText(task.status),
                                    style: TextStyle(
                                      color: _getStatusColor(task.status),
                                    ),
                                  ),
                                  backgroundColor: _getStatusBackgroundColor(task.status),
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                  visualDensity: VisualDensity.compact,
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            LinearProgressIndicator(
                              value: task.progress,
                              backgroundColor: Colors.grey[200],
                              valueColor: AlwaysStoppedAnimation<Color>(
                                _getProgressColor(task.status),
                              ),
                              minHeight: 6,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '进度: ${(task.progress * 100).toStringAsFixed(1)}%',
                              style: const TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              task.message,
                              style: const TextStyle(
                                fontSize: 14,
                              ),
                            ),
                            const SizedBox(height: 8),
                            Text(
                              '变更记录: ${task.changes.length} 条',
                              style: const TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                              ),
                            ),
                            const SizedBox(height: 10),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                if (task.changes.isNotEmpty)
                                  TextButton.icon(
                                    onPressed: () => _showChangeDetails(task),
                                    icon: const Icon(Icons.list, size: 14),
                                    label: const Text('查看详情', style: TextStyle(fontSize: 12)),
                                    style: TextButton.styleFrom(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                    ),
                                  ),
                                const SizedBox(width: 8),
                                if (task.status == TaskStatusEnum.PENDING)
                                  ElevatedButton(
                                    onPressed: () {
                                      _executeTask(task.taskId);
                                    },
                                    style: ElevatedButton.styleFrom(
                                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                    ),
                                    child: const Text('执行', style: TextStyle(fontSize: 12)),
                                  ),
                                if (task.status == TaskStatusEnum.RUNNING)
                                  ElevatedButton(
                                    onPressed: () {
                                      _cancelTask(task.taskId);
                                    },
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: Colors.orange,
                                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                    ),
                                    child: const Text('取消', style: TextStyle(fontSize: 12)),
                                  ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }

  String _getStatusText(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return '等待中';
      case TaskStatusEnum.RUNNING:
        return '运行中';
      case TaskStatusEnum.SUCCESS:
        return '成功';
      case TaskStatusEnum.FAILED:
        return '失败';
      case TaskStatusEnum.CANCELLED:
        return '已取消';
    }
  }

  Color _getStatusColor(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return Colors.blue;
      case TaskStatusEnum.RUNNING:
        return Colors.green;
      case TaskStatusEnum.SUCCESS:
        return Colors.green;
      case TaskStatusEnum.FAILED:
        return Colors.red;
      case TaskStatusEnum.CANCELLED:
        return Colors.orange;
    }
  }

  Color _getStatusBackgroundColor(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return Colors.blue[100]!;
      case TaskStatusEnum.RUNNING:
        return Colors.green[100]!;
      case TaskStatusEnum.SUCCESS:
        return Colors.green[100]!;
      case TaskStatusEnum.FAILED:
        return Colors.red[100]!;
      case TaskStatusEnum.CANCELLED:
        return Colors.orange[100]!;
    }
  }

  Color _getProgressColor(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return Colors.blue;
      case TaskStatusEnum.RUNNING:
        return Colors.green;
      case TaskStatusEnum.SUCCESS:
        return Colors.green;
      case TaskStatusEnum.FAILED:
        return Colors.red;
      case TaskStatusEnum.CANCELLED:
        return Colors.orange;
    }
  }

  IconData _getStatusIcon(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return Icons.check_circle;
      case 'FAILED':
        return Icons.error;
      case 'PENDING':
        return Icons.pending;
      default:
        return Icons.help_outline;
    }
  }

  Color _getChangeStatusColor(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'PENDING':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  IconData _getOperationIcon(String? operationType) {
    switch (operationType?.toUpperCase()) {
      case 'RENAME':
        return Icons.edit;
      case 'MOVE':
        return Icons.drive_file_move;
      case 'DELETE':
        return Icons.delete;
      case 'COPY':
        return Icons.copy;
      case 'METADATA_UPDATE':
        return Icons.info;
      case 'CONVERT':
        return Icons.transform;
      case 'MERGE':
        return Icons.merge_type;
      case 'UNZIP':
        return Icons.unarchive;
      case 'FIX_TYPE':
        return Icons.build;
      case 'DEDUP':
        return Icons.content_copy;
      case 'SPLIT':
        return Icons.call_split;
      case 'ALBUM_RENAME':
        return Icons.folder;
      default:
        return Icons.description;
    }
  }

  Color _getOperationColor(String? operationType) {
    switch (operationType?.toUpperCase()) {
      case 'RENAME':
        return Colors.blue;
      case 'MOVE':
        return Colors.orange;
      case 'DELETE':
        return Colors.red;
      case 'COPY':
        return Colors.green;
      case 'METADATA_UPDATE':
        return Colors.purple;
      case 'CONVERT':
        return Colors.teal;
      case 'MERGE':
        return Colors.indigo;
      case 'UNZIP':
        return Colors.amber;
      case 'FIX_TYPE':
        return Colors.cyan;
      case 'DEDUP':
        return Colors.lime;
      case 'SPLIT':
        return Colors.pink;
      case 'ALBUM_RENAME':
        return Colors.deepOrange;
      default:
        return Colors.grey;
    }
  }
}
