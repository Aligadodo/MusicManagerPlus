import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../api/api_client.dart';
import '../../api/task_service.dart';
import '../../models/task_status.dart' as task_models;
import '../common/selectable_text_widget.dart';
import '../common/task_stage_indicator.dart';

class TaskListWidget extends ConsumerStatefulWidget {
  final List<task_models.TaskStatus> tasks;
  final task_models.TaskStatus? selectedTask;
  final Function(task_models.TaskStatus?) onTaskSelected;
  final Function(String) onViewModeChanged;
  final bool isLoading;
  final String errorMessage;
  final VoidCallback? onRefresh;
  final Function(String, String)? onTaskNameChanged; // 添加任务名称变更回调

  const TaskListWidget({
    super.key,
    required this.tasks,
    required this.selectedTask,
    required this.onTaskSelected,
    required this.onViewModeChanged,
    required this.isLoading,
    required this.errorMessage,
    this.onRefresh,
    this.onTaskNameChanged, // 添加可选参数
  });

  @override
  ConsumerState<TaskListWidget> createState() => _TaskListWidgetState();
}

class _TaskListWidgetState extends ConsumerState<TaskListWidget> {
  String _searchKeyword = '';
  String _statusFilter = '全部';
  DateTimeRange? _dateRange;
  Map<String, bool> _editingTasks = {}; // 跟踪正在编辑的任务
  Map<String, TextEditingController> _nameControllers = {}; // 存储每个任务的编辑控制器

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
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.blue.withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: Colors.blue.withOpacity(0.3),
              width: 1,
            ),
          ),
          child: InkWell(
            onTap: widget.onRefresh,
            borderRadius: BorderRadius.circular(8),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: const [
                Icon(Icons.refresh, size: 14, color: Colors.blue),
                SizedBox(width: 4),
                Text('刷新', style: TextStyle(fontSize: 12, color: Colors.blue)),
              ],
            ),
          ),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.orange.withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: Colors.orange.withOpacity(0.3),
              width: 1,
            ),
          ),
          child: InkWell(
            onTap: () => _cancelAllTasks(context),
            borderRadius: BorderRadius.circular(8),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: const [
                Icon(Icons.stop_circle, size: 14, color: Colors.orange),
                SizedBox(width: 4),
                Text('终止全部', style: TextStyle(fontSize: 12, color: Colors.orange)),
              ],
            ),
          ),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.red.withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: Colors.red.withOpacity(0.3),
              width: 1,
            ),
          ),
          child: InkWell(
            onTap: () => _clearAllTasks(context),
            borderRadius: BorderRadius.circular(8),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: const [
                Icon(Icons.delete_sweep, size: 14, color: Colors.red),
                SizedBox(width: 4),
                Text('删除全部', style: TextStyle(fontSize: 12, color: Colors.red)),
              ],
            ),
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
              items: <String>['全部', 'CREATED', 'SCANNING', 'PREVIEWING', 'EXECUTING', 'COMPLETED', 'FAILED', 'CANCELLED']
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

    // 不再因为全局错误消息而阻止任务列表显示
    // 单个任务的错误应该在任务卡片内部显示
    // 错误消息现在通过SnackBar显示，不影响任务列表

    final filteredTasks = _filteredTasks;
    if (filteredTasks.isEmpty) {
      // 任务列表为空时显示提示信息，不显示错误信息
      // 错误信息已通过SnackBar显示，避免重复显示
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
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // 左侧区域：任务基本信息 (30%)
            Expanded(
              flex: 3,
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () {
                  widget.onTaskSelected(task);
                  widget.onViewModeChanged('taskDetail');
                },
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // 任务名称
                    Row(
                      children: [
                        Expanded(
                          child: _editingTasks[task.taskId] ?? false
                              ? TextField(
                                  controller: _nameControllers[task.taskId] ?? TextEditingController(text: task.taskName ?? '未命名任务'),
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.black87,
                                  ),
                                  decoration: const InputDecoration(
                                    isDense: true,
                                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                    border: OutlineInputBorder(),
                                  ),
                                  onSubmitted: (_) => _saveTaskName(task.taskId!, _nameControllers[task.taskId]?.text ?? ''),
                                )
                              : Text(
                                  task.taskName ?? '未命名任务',
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w600,
                                    color: Colors.black87,
                                  ),
                                  overflow: TextOverflow.ellipsis,
                                  maxLines: 1,
                                ),
                        ),
                        IconButton(
                          icon: Icon(
                            _editingTasks[task.taskId] ?? false ? Icons.check : Icons.edit,
                            size: 16,
                            color: _editingTasks[task.taskId] ?? false ? Colors.green : Colors.blue,
                          ),
                          onPressed: () {
                            if (_editingTasks[task.taskId] ?? false) {
                              _saveTaskName(task.taskId!, _nameControllers[task.taskId]?.text ?? '');
                            } else {
                              _startEditing(task);
                            }
                          },
                          tooltip: _editingTasks[task.taskId] ?? false ? '保存' : '编辑任务名称',
                          padding: const EdgeInsets.all(4),
                          constraints: const BoxConstraints(),
                        ),
                        if (_editingTasks[task.taskId] ?? false)
                          IconButton(
                            icon: const Icon(Icons.close, size: 16, color: Colors.red),
                            onPressed: () {
                              _cancelEditing(task.taskId!);
                            },
                            tooltip: '取消',
                            padding: const EdgeInsets.all(4),
                            constraints: const BoxConstraints(),
                          ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    // 状态标签行
                    Wrap(
                      spacing: 6,
                      runSpacing: 4,
                      children: [
                        // 任务状态
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: _getStatusColor(task.status ?? 'UNKNOWN').withOpacity(0.15),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: _getStatusColor(task.status ?? 'UNKNOWN').withOpacity(0.3),
                              width: 1,
                            ),
                          ),
                          child: Text(
                            _getFriendlyStatus(task.status ?? 'UNKNOWN'),
                            style: TextStyle(
                              color: _getStatusColor(task.status ?? 'UNKNOWN'),
                              fontSize: 10,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                        // 当前阶段
                        if (task.currentStage != null)
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: Colors.blue.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              task.currentStage!,
                              style: const TextStyle(
                                fontSize: 9,
                                color: Colors.blue,
                              ),
                            ),
                          ),
                        // 进度
                        if (task.overallProgress != null && task.overallProgress! > 0)
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: Colors.green.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              '${task.overallProgress!.round()}%',
                              style: const TextStyle(
                                fontSize: 9,
                                color: Colors.green,
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    // 统计信息行
                    Wrap(
                      spacing: 10,
                      runSpacing: 4,
                      children: [
                        if (task.stages?.scan?.totalFiles != null && task.stages!.scan!.totalFiles! > 0)
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.folder_open, size: 11, color: Colors.blue),
                              const SizedBox(width: 2),
                              Text(
                                '${task.stages!.scan!.totalFiles}',
                                style: const TextStyle(
                                  fontSize: 10,
                                  color: Colors.blue,
                                ),
                              ),
                            ],
                          ),
                        if (task.stages?.preview?.totalChanges != null && task.stages!.preview!.totalChanges! > 0)
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.change_history, size: 11, color: Colors.purple),
                              const SizedBox(width: 2),
                              Text(
                                '${task.stages!.preview!.totalChanges}',
                                style: const TextStyle(
                                  fontSize: 10,
                                  color: Colors.purple,
                                ),
                              ),
                            ],
                          ),
                        if (task.stages?.execution?.successCount != null && task.stages!.execution!.successCount! > 0)
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.check_circle, size: 11, color: Colors.green),
                              const SizedBox(width: 2),
                              Text(
                                '${task.stages!.execution!.successCount}',
                                style: const TextStyle(
                                  fontSize: 10,
                                  color: Colors.green,
                                ),
                              ),
                            ],
                          ),
                        if (task.stages?.execution?.failedCount != null && task.stages!.execution!.failedCount! > 0)
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.error, size: 11, color: Colors.red),
                              const SizedBox(width: 2),
                              Text(
                                '${task.stages!.execution!.failedCount}',
                                style: const TextStyle(
                                  fontSize: 10,
                                  color: Colors.red,
                                ),
                              ),
                            ],
                          ),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            // 中间区域：阶段指示器 (35%)
            Expanded(
              flex: 4,
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: () {
                  widget.onTaskSelected(task);
                  widget.onViewModeChanged('taskDetail');
                },
                child: Center(
                  child: TaskStageIndicator(
                    task: task,
                    showLabels: false,
                  ),
                ),
              ),
            ),

            // 右侧区域：操作按钮 (25%)
            Expanded(
              flex: 3,
              child: _buildStageActionButtons(context, task),
            ),
          ],
        ),
      ),
    );
  }

  /// 构建阶段操作按钮
  Widget _buildStageActionButtons(BuildContext context, task_models.TaskStatus task) {
    final status = task.status ?? 'CREATED';

    List<Widget> buttons = [];

    switch (status) {
      case 'CREATED':
        // 已创建：显示开始扫描按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.play_arrow,
          label: '开始扫描',
          color: Colors.blue,
          onTap: () => _startScan(context, task.taskId!),
        ));
        break;

      case 'SCANNING':
        // 扫描中：显示终止按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.stop,
          label: '终止',
          color: Colors.orange,
          onTap: () => _cancelTask(context, task.taskId!),
        ));
        break;

      case 'SCANNED':
        // 已扫描：显示开始预览按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.preview,
          label: '开始预览',
          color: Colors.purple,
          onTap: () => _startPreview(context, task.taskId!),
        ));
        break;

      case 'PREVIEWING':
        // 预览中：显示终止按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.stop,
          label: '终止',
          color: Colors.orange,
          onTap: () => _cancelTask(context, task.taskId!),
        ));
        break;

      case 'PREVIEWED':
        // 已预览：显示开始执行按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.play_circle,
          label: '开始执行',
          color: Colors.green,
          onTap: () => _executeTask(context, task.taskId!),
        ));
        break;

      case 'EXECUTING':
        // 执行中：显示终止按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.stop,
          label: '终止',
          color: Colors.orange,
          onTap: () => _cancelTask(context, task.taskId!),
        ));
        break;

      case 'COMPLETED':
        // 已完成：显示重新执行按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.refresh,
          label: '重新执行',
          color: Colors.blue,
          onTap: () => _rerunTask(context, task.taskId!),
        ));
        break;

      case 'FAILED':
        // 失败：显示重试按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.refresh,
          label: '重试',
          color: Colors.blue,
          onTap: () => _rerunTask(context, task.taskId!),
        ));
        break;

      case 'CANCELLED':
        // 已取消：显示重新运行按钮
        buttons.add(_buildActionButton(
          context: context,
          icon: Icons.replay,
          label: '重新运行',
          color: Colors.blue,
          onTap: () => _rerunTask(context, task.taskId!),
        ));
        break;
    }

    // 始终显示删除按钮
    buttons.add(_buildActionButton(
      context: context,
      icon: Icons.delete,
      label: '删除',
      color: Colors.red,
      onTap: () => _deleteTask(context, task.taskId!),
    ));

    if (buttons.isEmpty) {
      return const SizedBox.shrink();
    }

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: buttons.map((button) => Padding(
        padding: const EdgeInsets.only(left: 4),
        child: button,
      )).toList(),
    );
  }

  Widget _buildActionButton({
    required BuildContext context,
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(
              color: color.withOpacity(0.3),
              width: 1,
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, size: 12, color: color),
              const SizedBox(width: 3),
              Text(
                label,
                style: TextStyle(
                  fontSize: 10,
                  color: color,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _startScan(BuildContext context, String taskId) async {
    try {
      final taskService = TaskService(ApiClient());
      await taskService.restartScan(taskId);
      _showSuccess(context, '扫描已开始');
      widget.onRefresh?.call();
    } catch (e) {
      _showError(context, '开始扫描失败: $e');
    }
  }

  Future<void> _startPreview(BuildContext context, String taskId) async {
    try {
      final taskService = TaskService(ApiClient());
      await taskService.restartPreview(taskId);
      _showSuccess(context, '预览已开始');
      widget.onRefresh?.call();
    } catch (e) {
      _showError(context, '开始预览失败: $e');
    }
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
        return '扫描完成';
      case 'PREVIEWING':
        return '预览中';
      case 'PREVIEWED':
        return '预览完成';
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
        return Colors.cyan;
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

  void _startEditing(task_models.TaskStatus task) {
    setState(() {
      _editingTasks[task.taskId!] = true;
      _nameControllers[task.taskId!] = TextEditingController(text: task.taskName ?? '未命名任务');
    });
  }

  void _saveTaskName(String taskId, String newName) {
    final trimmedName = newName.trim();
    if (trimmedName.isNotEmpty) {
      widget.onTaskNameChanged?.call(taskId, trimmedName);
    }
    _cancelEditing(taskId);
  }

  void _cancelEditing(String taskId) {
    setState(() {
      _editingTasks[taskId] = false;
      _nameControllers.remove(taskId);
    });
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

  Future<void> _cancelAllTasks(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认终止'),
        content: const Text('确定要终止全部运行中的任务吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(
              foregroundColor: Colors.orange,
            ),
            child: const Text('终止全部'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      final taskService = TaskService(ApiClient());
      final result = await taskService.cancelAllTasks();
      final cancelledCount = result['data']?['cancelledCount'] ?? 0;
      _showSuccess(context, '已终止 $cancelledCount 个运行中的任务');
      if (widget.onRefresh != null) {
        widget.onRefresh!();
      }
    } catch (e) {
      _showError(context, '终止全部任务失败: $e');
    }
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
