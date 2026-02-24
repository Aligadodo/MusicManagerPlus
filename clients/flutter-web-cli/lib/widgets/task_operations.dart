import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dart:convert';
import '../api/api_client.dart';
import '../api/pipeline_service.dart';
import '../api/task_service.dart';
import '../models/source_directory.dart';
import '../models/strategy_info.dart';
import '../models/local_task_state.dart';

class TaskOperations {
  final BuildContext context;
  final WidgetRef ref;
  final ApiClient apiClient;
  final PipelineService pipelineService;
  final TaskService taskService;
  final List<SourceDirectory> sourceDirectories;
  final List<StrategyInfo> pipeline;
  final String taskId;
  final Function(LocalTaskState) onTaskStateChanged;
  final Function(String) onMessageChanged;
  final Function(String) onErrorMessageChanged;
  final Function(String) onLogMessageChanged;
  final Function(double) onProgressChanged;
  final Function(String) onRemainingTimeChanged;
  final Function(String) onCurrentStepChanged;
  final Function() onRefreshTasks;
  final Function() onFetchChanges;

  TaskOperations({
    required this.context,
    required this.ref,
    required this.apiClient,
    required this.pipelineService,
    required this.taskService,
    required this.sourceDirectories,
    required this.pipeline,
    required this.taskId,
    required this.onTaskStateChanged,
    required this.onMessageChanged,
    required this.onErrorMessageChanged,
    required this.onLogMessageChanged,
    required this.onProgressChanged,
    required this.onRemainingTimeChanged,
    required this.onCurrentStepChanged,
    required this.onRefreshTasks,
    required this.onFetchChanges,
  });

  Future<void> analyzePipeline() async {
    if (!_validateConfiguration()) {
      return;
    }

    if (!_validatePipelineParameters()) {
      return;
    }

    onTaskStateChanged(LocalTaskState.previewing);
    onErrorMessageChanged('');
    onProgressChanged(0);
    onRemainingTimeChanged('计算中...');
    onCurrentStepChanged('初始化预览任务');
    onMessageChanged('开始分析流水线...');
    onLogMessageChanged('');

    try {
      final sourceDirs = sourceDirectories.map((d) => d.path).toList();
      final result = await pipelineService.analyzePipeline(sourceDirs, pipeline);

      if (result['success'] == true) {
        _showSuccess(result['message'] ?? '分析任务已开始');
        await onRefreshTasks();
      } else {
        _showError(result['message'] ?? '分析失败');
        await onRefreshTasks();
        onTaskStateChanged(LocalTaskState.previewFailed);
        onErrorMessageChanged(result['message'] ?? '分析失败');
      }
    } catch (e) {
      print('分析流水线失败: $e');
      _showError('分析流水线失败: $e');
      await onRefreshTasks();
      onTaskStateChanged(LocalTaskState.previewFailed);
      onErrorMessageChanged('分析流水线失败: $e');
    }
  }

  Future<void> executeTask() async {
    if (!_validateConfiguration()) {
      return;
    }

    onTaskStateChanged(LocalTaskState.executing);
    onErrorMessageChanged('');
    onProgressChanged(0);
    onRemainingTimeChanged('计算中...');
    onCurrentStepChanged('初始化执行任务');
    onMessageChanged('开始执行流水线...');
    onLogMessageChanged('');

    try {
      final sourceDirs = sourceDirectories.map((d) => d.path).toList();
      final result = await pipelineService.executePipeline(sourceDirs, pipeline);

      if (result['success'] == true) {
        _showSuccess(result['message'] ?? '执行任务已开始');
        await onFetchChanges();
        onTaskStateChanged(LocalTaskState.executionCompleted);
        onMessageChanged('执行完成');
      } else {
        _showError(result['message'] ?? '执行失败');
        onTaskStateChanged(LocalTaskState.executionFailed);
        onErrorMessageChanged(result['message'] ?? '执行失败');
      }
    } catch (e) {
      print('执行流水线失败: $e');
      _showError('执行流水线失败: $e');
      onTaskStateChanged(LocalTaskState.executionFailed);
      onErrorMessageChanged('执行流水线失败: $e');
    }
  }

  Future<void> stopTask() async {
    onMessageChanged('正在停止任务...');

    try {
      final result = await apiClient.post('/api/tasks/stop');

      if (result.statusCode == 200) {
        final data = jsonDecode(result.body);
        if (data['success'] == true) {
          _showSuccess(data['message'] ?? '任务已停止');
          onTaskStateChanged(LocalTaskState.ready);
          onMessageChanged('任务已停止');
        } else {
          _showError(data['message'] ?? '停止任务失败');
        }
      } else {
        _showError('停止任务失败: ${result.statusCode}');
      }
    } catch (e) {
      print('停止任务失败: $e');
      _showError('停止任务失败: $e');
    }
  }

  Future<void> rerunTask(String taskId) async {
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
      await taskService.rerunTask(taskId);
      _showSuccess('任务已重新运行');
      await onRefreshTasks();
    } catch (e) {
      _showError('重新运行任务失败: $e');
    }
  }

  Future<void> cancelTask(String taskId) async {
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
      await taskService.cancelTask(taskId);
      _showSuccess('任务已终止');
      await onRefreshTasks();
    } catch (e) {
      _showError('终止任务失败: $e');
    }
  }

  Future<void> deleteTask(String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除此任务吗？删除后无法恢复。'),
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
      await taskService.deleteTask(taskId);
      _showSuccess('任务已删除');
      await onRefreshTasks();
    } catch (e) {
      _showError('删除任务失败: $e');
    }
  }

  bool _validateConfiguration() {
    if (sourceDirectories.isEmpty) {
      _showError('请先添加源目录');
      return false;
    }

    if (pipeline.isEmpty) {
      _showError('请先选择流水线策略');
      return false;
    }

    return true;
  }

  bool _validatePipelineParameters() {
    if (pipeline.isEmpty) {
      _showError('流水线配置无效');
      return false;
    }

    return true;
  }

  void _showSuccess(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
        duration: const Duration(seconds: 3),
      ),
    );
  }
}
