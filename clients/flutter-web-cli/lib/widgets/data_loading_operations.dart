import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/pipeline_service.dart';
import '../api/source_directory_service.dart';
import '../api/task_service.dart';
import '../models/change_record.dart';
import '../models/source_directory.dart';
import '../models/strategy_info.dart';
import '../models/task_status.dart' as task_models;
import '../models/local_task_state.dart';

class DataLoadingOperations {
  final BuildContext context;
  final WidgetRef ref;
  final PipelineService pipelineService;
  final SourceDirectoryService sourceDirectoryService;
  final TaskService taskService;
  final Function(List<SourceDirectory>) onSourceDirectoriesLoaded;
  final Function(List<StrategyInfo>) onPipelineLoaded;
  final Function(List<task_models.TaskStatus>) onTasksLoaded;
  final Function(List<ChangeRecord>) onChangesLoaded;
  final Function(bool) onLoadingChanged;
  final Function(String) onErrorMessageChanged;

  DataLoadingOperations({
    required this.context,
    required this.ref,
    required this.pipelineService,
    required this.sourceDirectoryService,
    required this.taskService,
    required this.onSourceDirectoriesLoaded,
    required this.onPipelineLoaded,
    required this.onTasksLoaded,
    required this.onChangesLoaded,
    required this.onLoadingChanged,
    required this.onErrorMessageChanged,
  });

  Future<void> loadData() async {
    onLoadingChanged(true);
    onErrorMessageChanged('');

    try {
      await Future.wait([
        loadSourceDirectories(),
        loadPipeline(),
        loadTasks(null),
      ]);
    } catch (e) {
      onErrorMessageChanged('加载数据失败: $e');
    } finally {
      onLoadingChanged(false);
    }
  }

  Future<void> loadSourceDirectories() async {
    try {
      final directories = await sourceDirectoryService.getSourceDirectories();
      onSourceDirectoriesLoaded(directories);
    } catch (e) {
      onErrorMessageChanged('加载源目录失败: $e');
    }
  }

  Future<void> loadPipeline() async {
    try {
      final pipeline = await pipelineService.getPipeline();
      onPipelineLoaded(pipeline);
    } catch (e) {
      onErrorMessageChanged('加载流水线失败: $e');
    }
  }

  Future<void> loadTasks(String? statusFilter) async {
    onLoadingChanged(true);
    try {
      final result = await taskService.getTaskList(
        page: 1,
        size: 20,
        status: _mapStatusToApi(statusFilter),
      );

      final data = result['data'] as Map<String, dynamic>?;
      final tasks = (data?['list'] as List<dynamic>?)?.map((json) => task_models.TaskStatus.fromJson(json as Map<String, dynamic>)).toList() ?? [];
      onTasksLoaded(tasks);
    } catch (e) {
      onErrorMessageChanged('加载任务失败: $e');
    } finally {
      onLoadingChanged(false);
    }
  }

  String? _mapStatusToApi(String? status) {
    switch (status) {
      case '等待中':
        return 'PENDING';
      case '进行中':
        return 'SCANNING,SCANNED,PREVIEWING,EXECUTING';
      case '已完成':
        return 'SCANNED,PREVIEWED,EXECUTED';
      case '失败':
        return 'FAILED';
      case '已取消':
        return 'CANCELLED';
      default:
        return null;
    }
  }

  Future<void> refreshTaskDetail(String taskId) async {
    onLoadingChanged(true);
    try {
      final task = await taskService.getTaskInfo(taskId);
      // 这里可以添加回调来更新单个任务的详情
    } catch (e) {
      onErrorMessageChanged('刷新任务详情失败: $e');
    } finally {
      onLoadingChanged(false);
    }
  }

  Future<void> refreshTaskDetailSafe(String taskId) async {
    try {
      await refreshTaskDetail(taskId);
    } catch (e) {
      // 安全模式：即使失败也不显示错误
      print('刷新任务详情失败: $e');
    }
  }

  Future<void> fetchChanges(String taskId, int page, int pageSize, String filter) async {
    onLoadingChanged(true);
    try {
      final result = await pipelineService.getChanges(
        searchFilter: filter,
        page: page,
        size: pageSize,
      );
      final changes = result['data']?['list'] ?? [];
      final changeRecords = changes.map((json) => ChangeRecord.fromJson(json as Map<String, dynamic>)).toList();
      onChangesLoaded(changeRecords);
    } catch (e) {
      onErrorMessageChanged('获取变更记录失败: $e');
    } finally {
      onLoadingChanged(false);
    }
  }
}
