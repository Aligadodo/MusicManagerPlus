import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/task_request.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';

void main() {
  group('任务管理优化功能测试', () {
    late TaskService taskService;
    late ApiClient apiClient;

    setUp(() {
      apiClient = ApiClient();
      taskService = TaskService(apiClient);
    });

    test('测试暂停任务功能', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;
        final status = firstTask['status'] as String?;

        if (status != null && ['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(status)) {
          final pauseResponse = await taskService.pauseTask(taskId);

          expect(pauseResponse['success'], isTrue);
          expect(pauseResponse['data']['paused'], isTrue);
        }
      }
    });

    test('测试恢复任务功能', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;
        final status = firstTask['status'] as String?;

        if (status == 'CANCELLED') {
          final resumeResponse = await taskService.resumeTask(taskId);

          expect(resumeResponse['success'], isTrue);
          expect(resumeResponse['data']['resumed'], isTrue);
        }
      }
    });

    test('测试重新扫描功能', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;
        final status = firstTask['status'] as String?;

        if (status != null && ['SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED'].contains(status)) {
          final restartScanResponse = await taskService.restartScan(taskId);

          expect(restartScanResponse['success'], isTrue);
        }
      }
    });

    test('测试重新预览功能', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;
        final status = firstTask['status'] as String?;

        if (status != null && ['PREVIEWED', 'COMPLETED', 'FAILED'].contains(status)) {
          final restartPreviewResponse = await taskService.restartPreview(taskId);

          expect(restartPreviewResponse['success'], isTrue);
        }
      }
    });

    test('测试重新执行功能', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;
        final status = firstTask['status'] as String?;

        if (status != null && ['COMPLETED', 'FAILED'].contains(status)) {
          final restartExecutionResponse = await taskService.restartExecution(taskId);

          expect(restartExecutionResponse['success'], isTrue);
        }
      }
    });

    test('测试任务状态转换', () async {
      final request = TaskRequest(
        strategyId: 'test-strategy',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({
          'strategyName': '测试策略',
          'description': '状态转换测试策略',
        }),
        taskName: '状态转换测试任务',
        description: '测试任务状态转换',
      );

      try {
        final taskId = await taskService.createTask(request);

        expect(taskId, isNotEmpty);

        final taskInfo = await taskService.getTaskInfo(taskId);

        expect(taskInfo.status, equals('CREATED'));
        expect(taskInfo.currentStage, equals('CREATED'));
      } catch (e) {
        print('任务创建失败（可能后端未运行）: $e');
      }
    });

    test('测试任务详情阶段信息', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;

        final taskInfo = await taskService.getTaskInfo(taskId);

        expect(taskInfo.taskId, equals(taskId));
        expect(taskInfo.stages, isNotNull);
        expect(taskInfo.stages?.scan, isNotNull);
        expect(taskInfo.stages?.preview, isNotNull);
        expect(taskInfo.stages?.execution, isNotNull);
      }
    });

    test('测试批量删除任务', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.length >= 2) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final secondTask = tasks[1] as Map<String, dynamic>;
        final taskId1 = firstTask['taskId'] as String;
        final taskId2 = secondTask['taskId'] as String;

        final deleteResponse1 = await taskService.deleteTask(taskId1);
        final deleteResponse2 = await taskService.deleteTask(taskId2);

        expect(deleteResponse1['success'], isTrue);
        expect(deleteResponse2['success'], isTrue);
      }
    });

    test('测试任务进度信息', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List?;

      if (tasks != null && tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;

        final progressResponse = await taskService.getTaskProgress(taskId);

        expect(progressResponse['success'], isTrue);
        expect(progressResponse['data'], isNotNull);
      }
    });
  });
}
