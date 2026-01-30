import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/pages/home_page.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'dart:convert';

class TaskMonitorPage extends ConsumerStatefulWidget {
  const TaskMonitorPage({super.key});

  @override
  ConsumerState<TaskMonitorPage> createState() => _TaskMonitorPageState();
}

class _TaskMonitorPageState extends ConsumerState<TaskMonitorPage> {
  List<TaskStatus> _tasks = [];
  bool _isLoading = false;
  String _errorMessage = '';
  Map<String, WebSocketChannel> _webSocketChannels = {};

  @override
  void initState() {
    super.initState();
    _loadTasks();
  }

  @override
  void dispose() {
    // 关闭所有WebSocket连接
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
      final taskService = ref.read(taskServiceProvider);
      final tasks = await taskService.getTasks();
      setState(() {
        _tasks = tasks;
      });

      // 为每个运行中的任务建立WebSocket连接
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

    final taskService = ref.read(taskServiceProvider);
    final channel = taskService.connectTaskWebSocket('/tasks/$taskId');

    channel.stream.listen(
      (message) {
        final data = jsonDecode(message as String);
        final updatedTask = TaskStatus.fromJson(data);
        setState(() {
          final index = _tasks.indexWhere((t) => t.taskId == taskId);
          if (index != -1) {
            _tasks[index] = updatedTask;
          }

          // 如果任务完成，关闭WebSocket连接
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
      final taskService = ref.read(taskServiceProvider);
      await taskService.executeTask(taskId);
      _connectToTaskWebSocket(taskId);
      // 重新加载任务列表
      await _loadTasks();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to execute task: $e';
      });
    }
  }

  Future<void> _cancelTask(String taskId) async {
    try {
      final taskService = ref.read(taskServiceProvider);
      await taskService.cancelTask(taskId);
      // 关闭WebSocket连接
      _webSocketChannels[taskId]?.sink.close();
      _webSocketChannels.remove(taskId);
      // 重新加载任务列表
      await _loadTasks();
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to cancel task: $e';
      });
    }
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
        padding: const EdgeInsets.all(20.0),
        child:
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children:
              [
                const Text(
                  '任务列表',
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 20),

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
                    child:
                      ListView.builder(
                        itemCount: _tasks.length,
                        itemBuilder: (context, index) {
                          final task = _tasks[index];
                          return Card(
                            elevation: 2,
                            margin: const EdgeInsets.symmetric(vertical: 10),
                            child:
                              Padding(
                                padding: const EdgeInsets.all(15.0),
                                child:
                                  Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children:
                                      [
                                        Row(
                                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                          children:
                                            [
                                              Text(
                                                task.taskId,
                                                style: const TextStyle(
                                                  fontSize: 16,
                                                  fontWeight: FontWeight.bold,
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
                                              ),
                                            ],
                                        ),
                                        const SizedBox(height: 10),
                                        LinearProgressIndicator(
                                          value: task.progress,
                                          backgroundColor: Colors.grey[200],
                                          valueColor: AlwaysStoppedAnimation<Color>(
                                            _getProgressColor(task.status),
                                          ),
                                        ),
                                        const SizedBox(height: 5),
                                        Text(
                                          '进度: ${(task.progress * 100).toStringAsFixed(1)}%',
                                          style: const TextStyle(
                                            fontSize: 12,
                                            color: Colors.grey,
                                          ),
                                        ),
                                        const SizedBox(height: 10),
                                        Text(
                                          task.message,
                                          style: const TextStyle(
                                            fontSize: 14,
                                          ),
                                        ),
                                        const SizedBox(height: 15),
                                        Row(
                                          mainAxisAlignment: MainAxisAlignment.end,
                                          children:
                                            [
                                              if (task.status == TaskStatusEnum.PENDING)
                                                ElevatedButton(
                                                  onPressed: () {
                                                    _executeTask(task.taskId);
                                                  },
                                                  child: const Text('执行'),
                                                ),
                                              if (task.status == TaskStatusEnum.RUNNING)
                                                ElevatedButton(
                                                  onPressed: () {
                                                    _cancelTask(task.taskId);
                                                  },
                                                  child: const Text('取消'),
                                                  style: ElevatedButton.styleFrom(
                                                    backgroundColor: Colors.orange,
                                                  ),
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
}
