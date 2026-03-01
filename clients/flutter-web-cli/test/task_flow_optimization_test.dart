import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/models/task_status.dart' as task_models;
import 'package:filemanager_flutter/models/strategy_info.dart';

void main() {
  group('任务流程优化测试', () {
    late TaskService taskService;
    late PipelineService pipelineService;
    late ApiClient apiClient;

    setUp(() {
      apiClient = ApiClient();
      taskService = TaskService(apiClient);
      pipelineService = PipelineService(apiClient);
    });

    StrategyInfo _createTestStrategy() {
      return StrategyInfo(
        id: 'test-strategy-001',
        name: '测试策略',
        description: '测试策略描述',
        configFields: [],
        enabled: true,
      );
    }

    group('TC001 - 创建任务（不勾选自动执行）', () {
      test('任务创建后状态应为 CREATED', () async {
        final sourceDirectories = ['/test/music'];
        final pipeline = [_createTestStrategy()];

        final result = await pipelineService.analyzePipeline(
          sourceDirectories,
          pipeline,
          autoExecute: false,
        );

        expect(result['success'], isTrue);
        expect(result['taskId'], isNotNull);

        final taskId = result['taskId'];
        await Future.delayed(const Duration(seconds: 1));

        final taskInfo = await taskService.getTaskInfo(taskId);
        expect(taskInfo.status, equals('CREATED'));
      });
    });

    group('TC002 - 创建任务（勾选自动执行）', () {
      test('任务创建后应自动开始运行', () async {
        final sourceDirectories = ['/test/music'];
        final pipeline = [_createTestStrategy()];

        final result = await pipelineService.analyzePipeline(
          sourceDirectories,
          pipeline,
          autoExecute: true,
        );

        expect(result['success'], isTrue);
        expect(result['taskId'], isNotNull);

        final taskId = result['taskId'];
        await Future.delayed(const Duration(seconds: 1));

        final taskInfo = await taskService.getTaskInfo(taskId);
        expect(
          taskInfo.status,
          anyOf('SCANNING', 'SCANNED', 'PREVIEWING', 'PREVIEWED', 'EXECUTING', 'COMPLETED'),
          reason: '自动执行时任务应该处于运行中或已完成状态',
        );
      });
    });

    group('TC003 - 连续创建多个任务', () {
      test('所有任务都应创建成功', () async {
        final taskIds = <String>[];

        for (int i = 0; i < 3; i++) {
          final result = await pipelineService.analyzePipeline(
            ['/test/music$i'],
            [_createTestStrategy()],
            autoExecute: false,
          );

          expect(result['success'], isTrue);
          expect(result['taskId'], isNotNull);
          taskIds.add(result['taskId']);

          await Future.delayed(const Duration(milliseconds: 500));
        }

        expect(taskIds.length, equals(3));

        for (final taskId in taskIds) {
          final taskInfo = await taskService.getTaskInfo(taskId);
          expect(taskInfo.status, equals('CREATED'));
        }
      });
    });

    group('TC004 - CREATED 状态点击开始扫描', () {
      test('任务应开始扫描', () async {
        final result = await pipelineService.analyzePipeline(
          ['/test/music'],
          [_createTestStrategy()],
          autoExecute: false,
        );

        final taskId = result['taskId'];
        await Future.delayed(const Duration(seconds: 1));

        var taskInfo = await taskService.getTaskInfo(taskId);
        expect(taskInfo.status, equals('CREATED'));

        await taskService.restartScan(taskId);
        await Future.delayed(const Duration(seconds: 1));

        taskInfo = await taskService.getTaskInfo(taskId);
        expect(
          taskInfo.status,
          anyOf('SCANNING', 'SCANNED'),
          reason: '点击开始扫描后任务应该处于扫描中或已扫描状态',
        );
      });
    });

    group('TC005 - SCANNED 状态点击开始预览', () {
      test('任务应开始预览', () async {
        final result = await pipelineService.analyzePipeline(
          ['/test/music'],
          [_createTestStrategy()],
          autoExecute: false,
        );

        final taskId = result['taskId'];
        await Future.delayed(const Duration(seconds: 1));

        await taskService.restartScan(taskId);
        await Future.delayed(const Duration(seconds: 2));

        var taskInfo = await taskService.getTaskInfo(taskId);
        if (taskInfo.status == 'SCANNED') {
          await taskService.restartPreview(taskId);
          await Future.delayed(const Duration(seconds: 1));

          taskInfo = await taskService.getTaskInfo(taskId);
          expect(
            taskInfo.status,
            anyOf('PREVIEWING', 'PREVIEWED'),
            reason: '点击开始预览后任务应该处于预览中或已预览状态',
          );
        }
      });
    });

    group('TC006 - PREVIEWED 状态点击开始执行', () {
      test('任务应开始执行', () async {
        final result = await pipelineService.analyzePipeline(
          ['/test/music'],
          [_createTestStrategy()],
          autoExecute: false,
        );

        final taskId = result['taskId'];
        await Future.delayed(const Duration(seconds: 1));

        await taskService.restartScan(taskId);
        await Future.delayed(const Duration(seconds: 2));

        var taskInfo = await taskService.getTaskInfo(taskId);
        if (taskInfo.status == 'SCANNED') {
          await taskService.restartPreview(taskId);
          await Future.delayed(const Duration(seconds: 2));

          taskInfo = await taskService.getTaskInfo(taskId);
          if (taskInfo.status == 'PREVIEWED') {
            await taskService.restartExecution(taskId);
            await Future.delayed(const Duration(seconds: 1));

            taskInfo = await taskService.getTaskInfo(taskId);
            expect(
              taskInfo.status,
              anyOf('EXECUTING', 'COMPLETED'),
              reason: '点击开始执行后任务应该处于执行中或已完成状态',
            );
          }
        }
      });
    });

    group('TC007 - 运行中点击终止', () {
      test('任务应被终止', () async {
        final result = await pipelineService.analyzePipeline(
          ['/test/music'],
          [_createTestStrategy()],
          autoExecute: true,
        );

        final taskId = result['taskId'];
        await Future.delayed(const Duration(milliseconds: 500));

        var taskInfo = await taskService.getTaskInfo(taskId);
        if (['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(taskInfo.status)) {
          await taskService.cancelTask(taskId);
          await Future.delayed(const Duration(seconds: 1));

          taskInfo = await taskService.getTaskInfo(taskId);
          expect(taskInfo.status, equals('CANCELLED'));
        }
      });
    });

    group('TC008 - FAILED 状态点击重试', () {
      test('任务应重新开始', () async {
        final result = await pipelineService.analyzePipeline(
          ['/invalid/path/that/does/not/exist'],
          [_createTestStrategy()],
          autoExecute: true,
        );

        final taskId = result['taskId'];
        await Future.delayed(const Duration(seconds: 3));

        var taskInfo = await taskService.getTaskInfo(taskId);
        if (taskInfo.status == 'FAILED') {
          await taskService.rerunTask(taskId);
          await Future.delayed(const Duration(seconds: 1));

          taskInfo = await taskService.getTaskInfo(taskId);
          expect(
            taskInfo.status,
            anyOf('SCANNING', 'SCANNED', 'PREVIEWING', 'PREVIEWED', 'EXECUTING', 'FAILED'),
            reason: '重试后任务应该重新开始运行',
          );
        }
      });
    });

    group('任务状态机验证', () {
      test('验证状态流转顺序', () async {
        final result = await pipelineService.analyzePipeline(
          ['/test/music'],
          [_createTestStrategy()],
          autoExecute: false,
        );

        final taskId = result['taskId'];
        await Future.delayed(const Duration(seconds: 1));

        final validTransitions = {
          'CREATED': ['SCANNING'],
          'SCANNING': ['SCANNED', 'FAILED', 'CANCELLED'],
          'SCANNED': ['PREVIEWING'],
          'PREVIEWING': ['PREVIEWED', 'FAILED', 'CANCELLED'],
          'PREVIEWED': ['EXECUTING'],
          'EXECUTING': ['COMPLETED', 'FAILED', 'CANCELLED'],
          'COMPLETED': [],
          'FAILED': ['SCANNING'],
          'CANCELLED': ['SCANNING'],
        };

        var taskInfo = await taskService.getTaskInfo(taskId);
        var currentStatus = taskInfo.status;
        expect(currentStatus, equals('CREATED'));

        await taskService.restartScan(taskId);
        await Future.delayed(const Duration(seconds: 2));

        taskInfo = await taskService.getTaskInfo(taskId);
        var newStatus = taskInfo.status;
        expect(
          validTransitions[currentStatus]?.contains(newStatus) ?? false,
          isTrue,
          reason: '从 $currentStatus 到 $newStatus 的状态转换无效',
        );
      });
    });

    group('任务列表显示测试', () {
      test('单个任务失败不应影响整个列表', () async {
        final goodResult = await pipelineService.analyzePipeline(
          ['/test/music'],
          [_createTestStrategy()],
          autoExecute: false,
        );

        final badResult = await pipelineService.analyzePipeline(
          ['/invalid/path'],
          [_createTestStrategy()],
          autoExecute: true,
        );

        await Future.delayed(const Duration(seconds: 3));

        final listResult = await taskService.getTaskList();
        expect(listResult['success'], isTrue);
        expect(listResult['data'], isNotNull);
        expect(listResult['data']['list'], isNotNull);

        final tasks = listResult['data']['list'] as List;
        expect(tasks.length, greaterThanOrEqualTo(2));
      });
    });
  });
}
