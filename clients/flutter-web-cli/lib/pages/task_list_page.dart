import 'dart:async';
import 'package:flutter/material.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:filemanager_flutter/widgets/task_card.dart';

class TaskListPage extends StatefulWidget {
  const TaskListPage({Key? key}) : super(key: key);

  @override
  State<TaskListPage> createState() => _TaskListPageState();
}

class _TaskListPageState extends State<TaskListPage> {
  late final TaskService _taskService;
  List<TaskStatus> _tasks = [];
  bool _isLoading = false;
  String? _selectedStatus;
  int _currentPage = 1;
  int _totalPages = 1;
  final ScrollController _scrollController = ScrollController();
  Timer? _refreshTimer;

  @override
  void initState() {
    super.initState();
    _taskService = TaskService(ApiClient());
    _loadTasks();
    _scrollController.addListener(_onScroll);
    _startAutoRefresh();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _refreshTimer?.cancel();
    super.dispose();
  }

  void _startAutoRefresh() {
    _refreshTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      _refreshTasks();
    });
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent * 0.8) {
      _loadMoreTasks();
    }
  }

  Future<void> _loadTasks() async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
      _currentPage = 1;
    });

    try {
      final result = await _taskService.getTaskList(
        page: _currentPage,
        size: 20,
        status: _selectedStatus,
      );

      final tasks = (result['tasks'] as List<dynamic>)
          .map((json) => TaskStatus.fromJson(json as Map<String, dynamic>))
          .toList();

      setState(() {
        _tasks = tasks;
        final total = result['total'] as int?;
        _totalPages = total != null ? (total / 20).ceil() : 1;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      _showErrorSnackBar('加载任务列表失败: $e');
    }
  }

  Future<void> _loadMoreTasks() async {
    if (_isLoading || _currentPage >= _totalPages) return;

    setState(() {
      _isLoading = true;
      _currentPage++;
    });

    try {
      final result = await _taskService.getTaskList(
        page: _currentPage,
        size: 20,
        status: _selectedStatus,
      );

      final newTasks = (result['tasks'] as List<dynamic>)
          .map((json) => TaskStatus.fromJson(json as Map<String, dynamic>))
          .toList();

      setState(() {
        _tasks.addAll(newTasks);
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _currentPage--;
      });
      _showErrorSnackBar('加载更多任务失败: $e');
    }
  }

  Future<void> _refreshTasks() async {
    try {
      final result = await _taskService.getTaskList(
        page: 1,
        size: _tasks.length,
        status: _selectedStatus,
      );

      final tasks = (result['tasks'] as List<dynamic>)
          .map((json) => TaskStatus.fromJson(json as Map<String, dynamic>))
          .toList();

      if (mounted) {
        setState(() {
          _tasks = tasks;
        });
      }
    } catch (e) {
      if (mounted) {
        _showErrorSnackBar('刷新任务列表失败: $e');
      }
    }
  }

  Future<void> _cancelTask(String taskId) async {
    try {
      await _taskService.cancelTask(taskId);
      _showSuccessSnackBar('任务已取消');
      _refreshTasks();
    } catch (e) {
      _showErrorSnackBar('取消任务失败: $e');
    }
  }

  Future<void> _deleteTask(String taskId) async {
    final confirmed = await _showConfirmDialog('确认删除', '确定要删除此任务吗？');
    if (!confirmed) return;

    try {
      await _taskService.deleteTask(taskId);
      _showSuccessSnackBar('任务已删除');
      _refreshTasks();
    } catch (e) {
      _showErrorSnackBar('删除任务失败: $e');
    }
  }

  void _navigateToTaskDetail(TaskStatus task) {
    Navigator.pushNamed(
      context,
      '/task-detail',
      arguments: {'taskId': task.taskId},
    );
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
        title: const Text('任务列表'),
        actions: [
          PopupMenuButton<String>(
            icon: const Icon(Icons.filter_list),
            onSelected: (status) {
              setState(() {
                _selectedStatus = status == '全部' ? null : status;
              });
              _loadTasks();
            },
            itemBuilder: (context) => [
              const PopupMenuItem(
                value: '全部',
                child: Text('全部'),
              ),
              const PopupMenuItem(
                value: 'CREATED',
                child: Text('已创建'),
              ),
              const PopupMenuItem(
                value: 'SCANNING',
                child: Text('扫描中'),
              ),
              const PopupMenuItem(
                value: 'SCANNED',
                child: Text('已扫描'),
              ),
              const PopupMenuItem(
                value: 'PREVIEWING',
                child: Text('预览中'),
              ),
              const PopupMenuItem(
                value: 'PREVIEWED',
                child: Text('已预览'),
              ),
              const PopupMenuItem(
                value: 'EXECUTING',
                child: Text('执行中'),
              ),
              const PopupMenuItem(
                value: 'COMPLETED',
                child: Text('已完成'),
              ),
              const PopupMenuItem(
                value: 'FAILED',
                child: Text('失败'),
              ),
              const PopupMenuItem(
                value: 'CANCELLED',
                child: Text('已取消'),
              ),
            ],
          ),
        ],
      ),
      body: _buildBody(),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.pushNamed(context, '/compose');
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading && _tasks.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_tasks.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.inbox_outlined, size: 64, color: Colors.grey[400]),
            const SizedBox(height: 16),
            Text(
              '暂无任务',
              style: TextStyle(fontSize: 16, color: Colors.grey[600]),
            ),
            const SizedBox(height: 8),
            Text(
              '点击右下角按钮创建新任务',
              style: TextStyle(fontSize: 14, color: Colors.grey[500]),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _refreshTasks,
      child: ListView.builder(
        controller: _scrollController,
        itemCount: _tasks.length + (_isLoading ? 1 : 0),
        itemBuilder: (context, index) {
          if (index >= _tasks.length) {
            return const Padding(
              padding: EdgeInsets.all(16),
              child: Center(child: CircularProgressIndicator()),
            );
          }

          final task = _tasks[index];
          return TaskCard(
            task: task,
            onTap: () => _navigateToTaskDetail(task),
            onCancel: () => _cancelTask(task.taskId!),
            onDelete: () => _deleteTask(task.taskId!),
          );
        },
      ),
    );
  }
}
