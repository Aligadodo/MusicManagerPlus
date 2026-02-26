import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../api/api_client.dart';
import '../../api/task_service.dart';
import '../../models/task_status.dart' as task_models;
import '../common/selectable_text_widget.dart';

class TaskListWidget extends ConsumerStatefulWidget {
  final List<task_models.TaskStatus> tasks;
  final task_models.TaskStatus? selectedTask;
  final Function(task_models.TaskStatus?) onTaskSelected;
  final Function(String) onViewModeChanged;
  final bool isLoading;
  final String errorMessage;
  final VoidCallback? onRefresh;

  const TaskListWidget({
    super.key,
    required this.tasks,
    required this.selectedTask,
    required this.onTaskSelected,
    required this.onViewModeChanged,
    required this.isLoading,
    required this.errorMessage,
    this.onRefresh,
  });

  @override
  ConsumerState<TaskListWidget> createState() => _TaskListWidgetState();
}

class _TaskListWidgetState extends ConsumerState<TaskListWidget> {
  String _searchKeyword = '';
  String _statusFilter = '全部';
  DateTimeRange? _dateRange;

  List<task_models.TaskStatus> get _filteredTasks {
    return widget.tasks.where((task) {
      // 搜索筛选
      if (_searchKeyword.isNotEmpty) {
        final keyword = _searchKeyword.toLowerCase();
        final taskName = (task.taskName ?? '').toLowerCase();
        final taskId = (task.taskId ?? '').toLowerCase();
        if (!taskName.contains(keyword) && !taskId.contains(keyword)) {
          return false;
        }
      }

      // 状态筛选
      if (_statusFilter != '全部' && task.status != _statusFilter) {
        return false;
      }

      // 日期范围筛选
      if (_dateRange != null && task.createdAt != null) {
        final taskDate = DateTime.fromMillisecondsSinceEpoch(task.createdAt!);
        if (taskDate.isBefore(_dateRange!.start) || taskDate.isAfter(_dateRange!.end)) {
          return false;
        }
      }

      return true;
    }).toList();
  }

  void _showDateRangePicker() async {
    final DateTime now = DateTime.now();
    final DateTime initialStart = now.subtract(const Duration(days: 7));
    final DateTime initialEnd = now;

    final DateTimeRange? picked = await showDateRangePicker(
      context: context,
      initialDateRange: DateTimeRange(start: initialStart, end: initialEnd),
      firstDate: now.subtract(const Duration(days: 365)),
      lastDate: now.add(const Duration(days: 365)),
    );

    if (picked != null) {
      setState(() {
        _dateRange = picked;
      });
    }
  }

  void _resetFilters() {
    setState(() {
      _searchKeyword = '';
      _statusFilter = '全部';
      _dateRange = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Consumer(
      builder: (context, ref, child) {
        return Container(
          padding: const EdgeInsets.all(12.0),
          child: Column(
            children: [
              _buildTaskListHeader(context),
              const SizedBox(height: 12),
              _buildSearchFilterSection(context),
              const SizedBox(height: 12),
              Expanded(
                child: _buildTaskList(context),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildTaskListHeader(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '任务列表',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            ElevatedButton(
              onPressed: widget.onRefresh,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
              child: const Text('刷新'),
            ),
            const SizedBox(width: 8),
            ElevatedButton.icon(
              onPressed: () => _clearAllTasks(context),
              icon: const Icon(Icons.delete_sweep, size: 18),
              label: const Text('删除全部'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildSearchFilterSection(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '搜索与筛选',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: const InputDecoration(
                      labelText: '搜索关键词',
                      prefixIcon: Icon(Icons.search),
                      border: OutlineInputBorder(),
                    ),
                    onChanged: (value) {
                      setState(() {
                        _searchKeyword = value;
                      });
                    },
                  ),
                ),
                const SizedBox(width: 12),
                DropdownButton<String>(
                  value: _statusFilter,
                  hint: const Text('状态'),
                  onChanged: (String? newValue) {
                    setState(() {
                      _statusFilter = newValue!;
                    });
                  },
                  items: <String>['全部', 'CREATED', 'SCANNING', 'SCANNED', 'PREVIEWING', 'PREVIEWED', 'EXECUTING', 'COMPLETED', 'FAILED', 'CANCELLED']
                      .map<DropdownMenuItem<String>>((String value) {
                    return DropdownMenuItem<String>(
                      value: value,
                      child: Text(_getFriendlyStatus(value)),
                    );
                  }).toList(),
                ),
                const SizedBox(width: 8),
                ElevatedButton.icon(
                  onPressed: _showDateRangePicker,
                  icon: const Icon(Icons.calendar_today, size: 18),
                  label: const Text('日期范围'),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _resetFilters,
                  child: const Text('重置'),
                ),
              ],
            ),
            if (_dateRange != null)
              Padding(
                padding: const EdgeInsets.only(top: 8.0),
                child: Text(
                  '日期范围: ${_dateRange!.start.toString().split(' ')[0]} - ${_dateRange!.end.toString().split(' ')[0]}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildTaskList(BuildContext context) {
    if (widget.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (widget.errorMessage.isNotEmpty) {
      return Center(
        child: Text(
          widget.errorMessage,
          style: const TextStyle(color: Colors.red),
        ),
      );
    }

    final filteredTasks = _filteredTasks;
    if (filteredTasks.isEmpty) {
      return const Center(
        child: Text('暂无任务记录'),
      );
    }

    return ListView.builder(
      itemCount: filteredTasks.length,
      itemBuilder: (context, index) {
        final task = filteredTasks[index];
        return _buildTaskCard(context, task);
      },
    );
  }

  Widget _buildTaskCard(BuildContext context, task_models.TaskStatus task) {
    final createdAt = task.createdAt != null
        ? DateTime.fromMillisecondsSinceEpoch(task.createdAt!)
        : null;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    task.taskName ?? '未命名任务',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Chip(
                  label: Text(
                    _getFriendlyStatus(task.status ?? 'UNKNOWN'),
                    style: TextStyle(
                      color: _getStatusColor(task.status ?? 'UNKNOWN'),
                    ),
                  ),
                  backgroundColor: _getStatusColor(task.status ?? 'UNKNOWN').withOpacity(0.1),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (createdAt != null)
              Text(
                '创建时间: ${createdAt.toString()}',
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.grey,
                ),
              ),
            if (task.currentStage != null)
              Text(
                '当前阶段: ${task.currentStage}',
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.grey,
                ),
              ),
            if (task.message != null)
              Text(
                '消息: ${task.message}',
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.grey,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                if (['COMPLETED', 'FAILED', 'CANCELLED'].contains(task.status))
                  IconButton(
                    icon: const Icon(Icons.refresh, color: Colors.green, size: 20),
                    onPressed: () => _rerunTask(context, task.taskId!),
                    tooltip: '重新运行',
                  ),
                if (['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(task.status))
                  IconButton(
                    icon: const Icon(Icons.stop, color: Colors.orange, size: 20),
                    onPressed: () => _cancelTask(context, task.taskId!),
                    tooltip: '终止',
                  ),
                IconButton(
                  icon: const Icon(Icons.delete, color: Colors.red, size: 20),
                  onPressed: () => _deleteTask(context, task.taskId!),
                  tooltip: '删除',
                ),
                TextButton(
                  onPressed: () {
                    widget.onTaskSelected(task);
                    widget.onViewModeChanged('taskDetail');
                  },
                  child: const Text('查看详情'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _getFriendlyStatus(String status) {
    if (status == '全部') {
      return '全部';
    }
    switch (status) {
      case 'CREATED':
        return '已创建';
      case 'PENDING':
        return '等待中';
      case 'SCANNING':
        return '扫描中';
      case 'SCANNED':
        return '已扫描';
      case 'PREVIEWING':
        return '预览中';
      case 'PREVIEWED':
        return '已预览';
      case 'EXECUTING':
        return '执行中';
      case 'COMPLETED':
        return '已完成';
      case 'FAILED':
        return '失败';
      case 'CANCELLED':
        return '已取消';
      default:
        return '未知状态';
    }
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'CREATED':
      case 'PENDING':
        return Colors.yellow;
      case 'SCANNING':
      case 'PREVIEWING':
      case 'EXECUTING':
        return Colors.blue;
      case 'SCANNED':
      case 'PREVIEWED':
      case 'COMPLETED':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'CANCELLED':
        return Colors.grey;
      default:
        return Colors.grey;
    }
  }

  Future<void> _rerunTask(BuildContext context, String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认重新运行'),
        content: const Text('确定要重新运行此任务吗？'),
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

    if (confirmed != true) return;

    try {
      final taskService = TaskService(ApiClient());
      await taskService.rerunTask(taskId);
      _showSuccess(context, '任务已重新运行');
    } catch (e) {
      _showError(context, '重新运行任务失败: $e');
    }
  }

  Future<void> _cancelTask(BuildContext context, String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认终止'),
        content: const Text('确定要终止此任务吗？'),
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

    if (confirmed != true) return;

    try {
      final taskService = TaskService(ApiClient());
      await taskService.cancelTask(taskId);
      _showSuccess(context, '任务已终止');
    } catch (e) {
      _showError(context, '终止任务失败: $e');
    }
  }

  Future<void> _deleteTask(BuildContext context, String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除此任务吗？此操作不可恢复！'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(
              foregroundColor: Colors.red,
            ),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      final taskService = TaskService(ApiClient());
      await taskService.deleteTask(taskId);
      _showSuccess(context, '任务已删除');
    } catch (e) {
      _showError(context, '删除任务失败: $e');
    }
  }

  void _showError(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  void _showSuccess(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
      ),
    );
  }

  Future<void> _clearAllTasks(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除全部任务吗？此操作不可恢复。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(
              foregroundColor: Colors.red,
            ),
            child: const Text('确定'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      final taskService = TaskService(ApiClient());
      await taskService.clearAllTasks();
      _showSuccess(context, '全部任务已删除');
      if (widget.onRefresh != null) {
        widget.onRefresh!();
      }
    } catch (e) {
      _showError(context, '删除全部任务失败: $e');
    }
  }
}
