import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/task_service.dart';
import '../models/task_status.dart';
import '../widgets/task_list_item.dart';
import '../widgets/task_detail_dialog.dart';
import '../widgets/task_status_helpers.dart';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';

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
    TaskDetailDialog.show(context, task);
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
                    return TaskListItem(
                      task: task,
                      onShowDetails: () => _showChangeDetails(task),
                      onExecute: () => _executeTask(task.taskId),
                      onCancel: () => _cancelTask(task.taskId),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }
}
