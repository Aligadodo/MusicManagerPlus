import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/task_request.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';

void main() {
  group('任务管理完整端到端测试', () {
    late TaskService taskService;
    late ApiClient apiClient;

    setUp(() {
      apiClient = ApiClient();
      taskService = TaskService(apiClient);
    });

    test('1. 创建任务并验证基本字段', () async {
      final request = TaskRequest(
        strategyId: 'test-strategy-1',
        filePaths: ['/test/path1', '/test/path2'],
        strategyConfig: StrategyConfig({
          'strategyName': '完整测试策略',
          'description': '端到端完整测试',
        }),
        taskName: '完整测试任务',
      );

      final taskId = await taskService.createTask(request);

      expect(taskId, isNotEmpty);
      expect(taskId, startsWith('task-'));

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.taskId, equals(taskId));
      expect(taskInfo.taskName, equals('完整测试任务'));
      expect(taskInfo.status, isNotEmpty);
      expect(taskInfo.createdAt, isNotNull);
      expect(taskInfo.createdAt, greaterThan(0));
    });

    test('2. 验证任务列表分页功能', () async {
      final taskIds = <String>[];

      for (int i = 0; i < 25; i++) {
        final request = TaskRequest(
          strategyId: 'test-strategy-page-$i',
          filePaths: ['/test/path$i'],
          strategyConfig: StrategyConfig({
            'strategyName': '分页测试$i',
          }),
          taskName: '分页测试任务$i',
        );

        final taskId = await taskService.createTask(request);
        taskIds.add(taskId);
      }

      await Future.delayed(const Duration(seconds: 1));

      final page1 = await taskService.getTaskList(page: 1, size: 10);
      expect(page1['success'], isTrue);
      expect((page1['tasks'] as List).length, lessThanOrEqualTo(10));

      final page2 = await taskService.getTaskList(page: 2, size: 10);
      expect(page2['success'], isTrue);
      expect((page2['tasks'] as List).length, lessThanOrEqualTo(10));

      final page3 = await taskService.getTaskList(page: 3, size: 10);
      expect(page3['success'], isTrue);
      expect((page3['tasks'] as List).length, lessThanOrEqualTo(10));
    });

    test('3. 验证任务状态筛选功能', () async {
      final request1 = TaskRequest(
        strategyId: 'test-status-1',
        filePaths: ['/test/path1'],
        strategyConfig: StrategyConfig({}),
        taskName: '状态测试任务1',
      );

      final request2 = TaskRequest(
        strategyId: 'test-status-2',
        filePaths: ['/test/path2'],
        strategyConfig: StrategyConfig({}),
        taskName: '状态测试任务2',
      );

      final taskId1 = await taskService.createTask(request1);
      final taskId2 = await taskService.createTask(request2);

      await Future.delayed(const Duration(milliseconds: 500));

      final allTasks = await taskService.getTaskList();
      expect(allTasks['success'], isTrue);

      final createdTasks = await taskService.getTaskList(status: 'CREATED');
      expect(createdTasks['success'], isTrue);

      final taskList = createdTasks['tasks'] as List;
      expect(taskList.length, greaterThan(0));

      for (var task in taskList) {
        final taskMap = task as Map<String, dynamic>;
        expect(taskMap['status'], equals('CREATED'));
      }
    });

    test('4. 验证任务详情完整性', () async {
      final request = TaskRequest(
        strategyId: 'test-detail',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({
          'strategyName': '详情测试策略',
        }),
        taskName: '详情测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.taskId, isNotNull);
      expect(taskInfo.taskName, isNotNull);
      expect(taskInfo.status, isNotNull);
      expect(taskInfo.currentStage, isNotNull);
      expect(taskInfo.overallProgress, isNotNull);
      expect(taskInfo.message, isNotNull);
      expect(taskInfo.createdAt, isNotNull);
      expect(taskInfo.configSnapshot, isNotNull);
      expect(taskInfo.stages, isNotNull);
    });

    test('5. 验证任务配置快照', () async {
      final request = TaskRequest(
        strategyId: 'test-snapshot',
        filePaths: ['/test/path1', '/test/path2'],
        strategyConfig: StrategyConfig({
          'strategyName': '快照测试策略',
          'description': '测试配置快照',
        }),
        taskName: '快照测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.configSnapshot, isNotNull);
      expect(taskInfo.configSnapshot!.sourceDirectories, isNotNull);
      expect(taskInfo.configSnapshot!.sourceDirectories!.length, greaterThan(0));
    });

    test('6. 验证任务阶段信息', () async {
      final request = TaskRequest(
        strategyId: 'test-stages',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '阶段测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.stages, isNotNull);
      expect(taskInfo.stages!.scan, isNotNull);
      expect(taskInfo.stages!.preview, isNotNull);
      expect(taskInfo.stages!.execution, isNotNull);
    });

    test('7. 验证多任务并发创建', () async {
      final futures = <Future<String>>[];

      for (int i = 0; i < 10; i++) {
        final request = TaskRequest(
          strategyId: 'test-concurrent-$i',
          filePaths: ['/test/path$i'],
          strategyConfig: StrategyConfig({}),
          taskName: '并发测试任务$i',
        );

        futures.add(taskService.createTask(request));
      }

      final taskIds = await Future.wait(futures);

      expect(taskIds.length, equals(10));

      for (var taskId in taskIds) {
        expect(taskId, isNotEmpty);
        expect(taskId, startsWith('task-'));
      }

      await Future.delayed(const Duration(seconds: 1));

      final response = await taskService.getTaskList();
      expect(response['success'], isTrue);

      final tasks = response['tasks'] as List;
      for (var taskId in taskIds) {
        final taskExists = tasks.any((task) {
          final taskMap = task as Map<String, dynamic>;
          return taskMap['taskId'] == taskId;
        });
        expect(taskExists, isTrue, reason: '任务 $taskId 应该存在于任务列表中');
      }
    });

    test('8. 验证任务ID唯一性', () async {
      final request = TaskRequest(
        strategyId: 'test-unique',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '唯一性测试任务',
      );

      final taskIds = <String>[];

      for (int i = 0; i < 5; i++) {
        final taskId = await taskService.createTask(request);
        taskIds.add(taskId);
      }

      expect(taskIds.toSet().length, equals(taskIds.length),
          reason: '所有任务ID应该是唯一的');
    });

    test('9. 验证任务时间戳', () async {
      final request = TaskRequest(
        strategyId: 'test-timestamp',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '时间戳测试任务',
      );

      final beforeCreate = DateTime.now().millisecondsSinceEpoch;

      final taskId = await taskService.createTask(request);

      final afterCreate = DateTime.now().millisecondsSinceEpoch;

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.createdAt, greaterThanOrEqualTo(beforeCreate));
      expect(taskInfo.createdAt, lessThanOrEqualTo(afterCreate));
    });

    test('10. 验证空任务列表处理', () async {
      final response = await taskService.getTaskList();

      expect(response['success'], isTrue);
      expect(response['tasks'], isNotNull);

      final tasks = response['tasks'] as List;
      expect(tasks, isA<List>());
    });

    test('11. 验证不存在的任务ID', () async {
      try {
        await taskService.getTaskInfo('non-existent-task-id');
        fail('应该抛出异常');
      } catch (e) {
        expect(e, isNotNull);
      }
    });

    test('12. 验证任务状态枚举值', () async {
      final request = TaskRequest(
        strategyId: 'test-enum',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '枚举测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      final validStatuses = [
        'CREATED',
        'SCANNING',
        'SCANNED',
        'PREVIEWING',
        'PREVIEWED',
        'EXECUTING',
        'COMPLETED',
        'FAILED',
        'CANCELLED',
      ];

      expect(validStatuses.contains(taskInfo.status), isTrue);
    });

    test('13. 验证任务进度范围', () async {
      final request = TaskRequest(
        strategyId: 'test-progress',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '进度测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      if (taskInfo.overallProgress != null) {
        expect(taskInfo.overallProgress, greaterThanOrEqualTo(0.0));
        expect(taskInfo.overallProgress, lessThanOrEqualTo(100.0));
      }
    });

    test('14. 验证任务消息字段', () async {
      final request = TaskRequest(
        strategyId: 'test-message',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '消息测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.message, isNotNull);
      expect(taskInfo.message, isA<String>());
    });

    test('15. 验证任务当前阶段字段', () async {
      final request = TaskRequest(
        strategyId: 'test-stage',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: '阶段字段测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.currentStage, isNotNull);
      expect(taskInfo.currentStage, isA<String>());
    });

    test('16. 验证任务名称字段', () async {
      final taskName = '特殊字符任务名称测试_123!@#\$%';

      final request = TaskRequest(
        strategyId: 'test-name',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({}),
        taskName: taskName,
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.taskName, equals(taskName));
    });

    test('17. 验证文件路径列表', () async {
      final filePaths = [
        '/test/path1',
        '/test/path2',
        '/test/path3',
      ];

      final request = TaskRequest(
        strategyId: 'test-paths',
        filePaths: filePaths,
        strategyConfig: StrategyConfig({}),
        taskName: '路径测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.configSnapshot, isNotNull);
      expect(taskInfo.configSnapshot!.sourceDirectories, isNotNull);
      expect(taskInfo.configSnapshot!.sourceDirectories!.length, equals(filePaths.length));
    });

    test('18. 验证策略配置', () async {
      final strategyConfig = StrategyConfig({
        'strategyName': '测试策略',
        'description': '策略描述',
        'param1': 'value1',
        'param2': 123,
        'param3': true,
      });

      final request = TaskRequest(
        strategyId: 'test-config',
        filePaths: ['/test/path'],
        strategyConfig: strategyConfig,
        taskName: '配置测试任务',
      );

      final taskId = await taskService.createTask(request);

      await Future.delayed(const Duration(milliseconds: 500));

      final taskInfo = await taskService.getTaskInfo(taskId);

      expect(taskInfo.configSnapshot, isNotNull);
    });
  });
}
