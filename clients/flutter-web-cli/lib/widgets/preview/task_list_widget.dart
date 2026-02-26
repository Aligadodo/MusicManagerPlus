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
    return Row(
      children: [
        const Text(
          '任务列表',
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
        const Spacer(),
        ElevatedButton(
          onPressed: widget.onRefresh,
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            minimumSize: Size.zero,
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
          child: const Text('刷新', style: TextStyle(fontSize: 12)),
        ),
        const SizedBox(width: 8),
        ElevatedButton.icon(
          onPressed: () => _clearAllTasks(context),
          icon: const Icon(Icons.delete_sweep, size: 16),
          label: const Text('删除全部', style: TextStyle(fontSize: 12)),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.red,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            minimumSize: Size.zero,
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
        ),
      ],
    );
  }

  Widget _buildSearchFilterSection(BuildContext context) {
    return Card(
      elevation: 1,
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                decoration: const InputDecoration(
                  labelText: '搜索',
                  prefixIcon: Icon(Icons.search, size: 18),
                  border: OutlineInputBorder(),
                  contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                  isDense: true,
                ),
                onChanged: (value) {
                  setState(() {
                    _searchKeyword = value;
                  });
                },
              ),
            ),
            const SizedBox(width: 8),
            DropdownButton<String>(
              value: _statusFilter,
              hint: const Text('状态', style: TextStyle(fontSize: 12)),
              onChanged: (String? newValue) {
                setState(() {
                  _statusFilter = newValue!;
                });
              },
              items: <String>['全部', 'CREATED', 'SCANNING', 'SCANNED', 'PREVIEWING', 'PREVIEWED', 'EXECUTING', 'COMPLETED', 'FAILED', 'CANCELLED']
                  .map<DropdownMenuItem<String>>((String value) {
                return DropdownMenuItem<String>(
                  value: value,
                  child: Text(_getFriendlyStatus(value), style: const TextStyle(fontSize: 12)),
                );
              }).toList(),
            ),
            const SizedBox(width: 4),
            IconButton(
              onPressed: _showDateRangePicker,
              icon: const Icon(Icons.calendar_today, size: 18),
              tooltip: '日期范围',
              padding: const EdgeInsets.all(4),
              constraints: const BoxConstraints(),
            ),
            if (_dateRange != null)
              IconButton(
                onPressed: _resetFilters,
                icon: const Icon(Icons.clear, size: 18),
                tooltip: '重置',
                padding: const EdgeInsets.all(4),
                constraints: const BoxConstraints(),
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
      margin: const EdgeInsets.symmetric(vertical: 6, horizontal: 0),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
      ),
      child: InkWell(
        onTap: () {
          widget.onTaskSelected(task);
          widget.onViewModeChanged('taskDetail');
        },
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              // 任务标题和状态
              Row(
                children: [
                  Expanded(
                    child: Text(
                      task.taskName ?? '未命名任务',
                      style: const TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: Colors.black87,
                      ),
                      overflow: TextOverflow.ellipsis,
                      maxLines: 1,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(task.status ?? 'UNKNOWN').withOpacity(0.15),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: _getStatusColor(task.status ?? 'UNKNOWN').withOpacity(0.3),
                        width: 1,
                      ),
                    ),
                    child: Text(
                      _getFriendlyStatus(task.status ?? 'UNKNOWN'),
                      style: TextStyle(
                        color: _getStatusColor(task.status ?? 'UNKNOWN'),
                        fontSize: 11,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
              
              const SizedBox(height: 8),
              
              // 任务基本信息
              Row(
                children: [
                  if (createdAt != null)
                    Container(
                      margin: const EdgeInsets.only(right: 12),
                      child: Row(
                        children: [
                          const Icon(Icons.access_time, size: 14, color: Colors.grey),
                          const SizedBox(width: 4),
                          Text(
                            _formatDateTime(createdAt),
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                  
                  if (task.currentStage != null)
                    Container(
                      margin: const EdgeInsets.only(right: 12),
                      child: Row(
                        children: [
                          const Icon(Icons.layers, size: 14, color: Colors.grey),
                          const SizedBox(width: 4),
                          Text(
                            task.currentStage!,
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                  
                  if (task.overallProgress != null && task.overallProgress! > 0)
                    Container(
                      margin: const EdgeInsets.only(right: 12),
                      child: Row(
                        children: [
                          const Icon(Icons.percent, size: 14, color: Colors.grey),
                          const SizedBox(width: 4),
                          Text(
                            '${(task.overallProgress! * 100).round()}%',
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              
              const SizedBox(height: 8),
              
              // 任务详情信息
              Row(
                children: [
                  if (task.stages?.scan?.totalFiles != null && task.stages!.scan!.totalFiles! > 0)
                    Container(
                      margin: const EdgeInsets.only(right: 16),
                      child: Row(
                        children: [
                          const Icon(Icons.folder_open, size: 14, color: Colors.blue),
                          const SizedBox(width: 4),
                          Text(
                            '${task.stages!.scan!.totalFiles} 文件',
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.blue,
                            ),
                          ),
                        ],
                      ),
                    ),
                  
                  if (task.stages?.preview?.totalChanges != null && task.stages!.preview!.totalChanges! > 0)
                    Container(
                      margin: const EdgeInsets.only(right: 16),
                      child: Row(
                        children: [
                          const Icon(Icons.change_history, size: 14, color: Colors.purple),
                          const SizedBox(width: 4),
                          Text(
                            '${task.stages!.preview!.totalChanges} 变更',
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.purple,
                            ),
                          ),
                        ],
                      ),
                    ),
                  
                  if (task.stages?.execution?.successCount != null && task.stages!.execution!.successCount! > 0)
                    Container(
                      margin: const EdgeInsets.only(right: 16),
                      child: Row(
                        children: [
                          const Icon(Icons.check_circle, size: 14, color: Colors.green),
                          const SizedBox(width: 4),
                          Text(
                            '${task.stages!.execution!.successCount} 成功',
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.green,
                            ),
                          ),
                        ],
                      ),
                    ),
                  
                  if (task.stages?.execution?.failedCount != null && task.stages!.execution!.failedCount! > 0)
                    Container(
                      margin: const EdgeInsets.only(right: 16),
                      child: Row(
                        children: [
                          const Icon(Icons.error, size: 14, color: Colors.red),
                          const SizedBox(width: 4),
                          Text(
                            '${task.stages!.execution!.failedCount} 失败',
                            style: const TextStyle(
                              fontSize: 11,
                              color: Colors.red,
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              
              if (task.message != null && task.message!.isNotEmpty)
                const SizedBox(height: 8),
              
              if (task.message != null && task.message!.isNotEmpty)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: Colors.grey[100],
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.info_outline, size: 14, color: Colors.grey),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          task.message!,
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey.shade700,
                            fontStyle: FontStyle.italic,
                          ),
                          overflow: TextOverflow.ellipsis,
                          maxLines: 2,
                        ),
                      ),
                    ],
                  ),
                ),
              
              const SizedBox(height: 10),
              
              // 操作按钮
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  if (task.status == 'PREVIEWED')
                    Container(
                      margin: const EdgeInsets.only(right: 8),
                      child: TextButton(
                        onPressed: () => _executeTask(context, task.taskId!),
                        style: TextButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          minimumSize: Size.zero,
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          backgroundColor: Colors.green.withOpacity(0.1),
                          foregroundColor: Colors.green,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(6),
                          ),
                        ),
                        child: const Text('执行', style: TextStyle(fontSize: 12)),
                      ),
                    ),
                  
                  if (['COMPLETED', 'FAILED', 'CANCELLED'].contains(task.status))
                    Container(
                      margin: const EdgeInsets.only(right: 8),
                      child: TextButton(
                        onPressed: () => _rerunTask(context, task.taskId!),
                        style: TextButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          minimumSize: Size.zero,
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          backgroundColor: Colors.blue.withOpacity(0.1),
                          foregroundColor: Colors.blue,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(6),
                          ),
                        ),
                        child: const Text('重新运行', style: TextStyle(fontSize: 12)),
                      ),
                    ),
                  
                  if (['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(task.status))
                    Container(
                      margin: const EdgeInsets.only(right: 8),
                      child: TextButton(
                        onPressed: () => _cancelTask(context, task.taskId!),
                        style: TextButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          minimumSize: Size.zero,
                          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          backgroundColor: Colors.orange.withOpacity(0.1),
                          foregroundColor: Colors.orange,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(6),
                          ),
                        ),
                        child: const Text('终止', style: TextStyle(fontSize: 12)),
                      ),
                    ),
                  
                  TextButton(
                    onPressed: () => _deleteTask(context, task.taskId!),
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                      minimumSize: Size.zero,
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      backgroundColor: Colors.red.withOpacity(0.1),
                      foregroundColor: Colors.red,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(6),
                      ),
                    ),
                    child: const Text('删除', style: TextStyle(fontSize: 12)),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  String _formatDateTime(DateTime dateTime) {
    final now = DateTime.now();
    final difference = now.difference(dateTime);
    
    if (difference.inMinutes < 1) {
      return '刚刚';
    } else if (difference.inMinutes < 60) {
      return '${difference.inMinutes}分钟前';
    } else if (difference.inHours < 24) {
      return '${difference.inHours}小时前';
    } else if (difference.inDays < 7) {
      return '${difference.inDays}天前';
    } else {
      return '${dateTime.month}/${dateTime.day}';
    }
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



  Future<void> _executeTask(BuildContext context, String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认执行'),
        content: const Text('确定要执行此任务吗？'),
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
      await taskService.executeTask(taskId);
      _showSuccess(context, '任务已开始执行');
    } catch (e) {
      _showError(context, '执行任务失败: $e');
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
