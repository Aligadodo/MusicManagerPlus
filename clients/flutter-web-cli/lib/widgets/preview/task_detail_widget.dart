import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/task_status.dart' as task_models;
import '../../api/task_service.dart';
import '../../api/api_client.dart';
import 'task_detail_header.dart';
import '../config/config_snapshot_card.dart';
import '../common/stage_result_cards.dart';
import 'task_log_panel.dart';

class TaskDetailWidget extends ConsumerStatefulWidget {
  final task_models.TaskStatus? selectedTask;
  final Function() onBack;

  const TaskDetailWidget({
    super.key,
    required this.selectedTask,
    required this.onBack,
  });

  @override
  ConsumerState<TaskDetailWidget> createState() => _TaskDetailWidgetState();
}

class _TaskDetailWidgetState extends ConsumerState<TaskDetailWidget> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  late TaskService _taskService;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 6, vsync: this);
    _taskService = TaskService(ApiClient());
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _restartScan(String taskId) async {
    try {
      await _taskService.restartScan(taskId);
      _showSuccess('重新扫描已开始');
    } catch (e) {
      _showError('重新扫描失败: $e');
    }
  }

  Future<void> _restartPreview(String taskId) async {
    try {
      await _taskService.restartPreview(taskId);
      _showSuccess('重新预览已开始');
    } catch (e) {
      _showError('重新预览失败: $e');
    }
  }

  Future<void> _restartExecution(String taskId) async {
    try {
      await _taskService.restartExecution(taskId);
      _showSuccess('重新执行已开始');
    } catch (e) {
      _showError('重新执行失败: $e');
    }
  }

  Future<void> _rerunTask(String taskId) async {
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
      await _taskService.rerunTask(taskId);
      _showSuccess('任务已重新运行');
    } catch (e) {
      _showError('重新运行任务失败: $e');
    }
  }

  Future<void> _cancelTask(String taskId) async {
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
      await _taskService.cancelTask(taskId);
      _showSuccess('任务已终止');
      // 刷新任务状态
      if (mounted) {
        setState(() {
          // 触发组件重新构建，获取最新的任务信息
        });
      }
    } catch (e) {
      _showError('终止任务失败: $e');
    }
  }

  void _showSuccess(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: Colors.green,
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  void _showError(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: Colors.red,
          duration: const Duration(seconds: 3),
        ),
      );
    }
  }

  Future<void> _updateTaskName(String newName) async {
    if (widget.selectedTask == null) return;

    try {
      final result = await _taskService.updateTaskName(widget.selectedTask!.taskId!, newName);
      if (result['success'] == true) {
        _showSuccess('任务名称已更新');
        // 刷新任务详情，确保显示更新后的名称
        if (mounted) {
          setState(() {
            // 触发组件重新构建，获取最新的任务信息
          });
        }
      } else {
        _showError('更新任务名称失败: ${result['error']['message']}');
      }
    } catch (e) {
      _showError('更新任务名称失败: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.selectedTask == null) {
      return const Center(child: Text('请选择一个任务'));
    }

    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          TaskDetailHeader(
            onBack: widget.onBack,
            selectedTask: widget.selectedTask,
            onRestartScan: _restartScan,
            onRestartPreview: _restartPreview,
            onRestartExecution: _restartExecution,
            onRerunTask: _rerunTask,
            onCancelTask: _cancelTask,
          ),
          const SizedBox(height: 12),
          
          // 添加TabBar导航条
          Container(
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
            ),
            child: TabBar(
              controller: _tabController,
              isScrollable: true,
              tabs: const [
                Tab(text: '任务信息'),
                Tab(text: '配置快照'),
                Tab(text: '扫描结果'),
                Tab(text: '预览结果'),
                Tab(text: '执行结果'),
                Tab(text: '执行日志'),
              ],
              labelColor: Colors.blue,
              unselectedLabelColor: Colors.grey,
              indicatorColor: Colors.blue,
              indicatorWeight: 2,
            ),
          ),
          
          const SizedBox(height: 12),
          
          // 添加TabBarView滑动展示
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                // 任务信息
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: TaskInfoCard(
                    selectedTask: widget.selectedTask!,
                    onTaskNameChanged: _updateTaskName,
                  ),
                ),
                
                // 配置快照
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: ConfigSnapshotCard(selectedTask: widget.selectedTask!),
                ),
                
                // 扫描结果
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: ScanResultCard(selectedTask: widget.selectedTask!),
                ),
                
                // 预览结果
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: PreviewResultCard(selectedTask: widget.selectedTask!),
                ),
                
                // 执行结果
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: ExecutionResultCard(selectedTask: widget.selectedTask!),
                ),
                
                // 执行日志
                TaskLogPanel(taskId: widget.selectedTask!.taskId ?? ''),
              ],
            ),
          ),
        ],
      ),
    );
  }
}