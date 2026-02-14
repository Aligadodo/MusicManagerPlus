import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/task_request.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';

/**
 * 任务管理端到端测试
 * 验证前后端任务交互的完整功能
 */
void main() {
  group('任务管理端到端测试', () {
    late TaskService taskService;
    late ApiClient apiClient;

    setUp(() {
    apiClient = ApiClient();
    taskService = TaskService(apiClient);
  });

    test('创建任务并验证返回', () async {
      final request = TaskRequest(
        strategyId: 'test-strategy',
        filePaths: ['/test/path'],
        strategyConfig: StrategyConfig({
          'strategyName': '测试策略',
          'description': '端到端测试策略',
        }),
        taskName: '测试任务',
        description: '端到端测试任务',
      );

      final taskId = await taskService.createTask(request);

      expect(taskId, isNotEmpty);
      expect(taskId, startsWith('task-'));
    });

    test('获取任务列表', () async {
      final response = await taskService.getTaskList();

      expect(response['success'], isTrue);
      expect(response['tasks'], isNotNull);
      expect(response['count'], greaterThanOrEqualTo(0));
    });

    test('获取任务详情', () async {
      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List;

      if (tasks.isNotEmpty) {
        final firstTask = tasks.first as Map<String, dynamic>;
        final taskId = firstTask['taskId'] as String;

        final taskInfo = await taskService.getTaskInfo(taskId);

        expect(taskInfo.taskId, equals(taskId));
        expect(taskInfo.taskName, isNotNull);
        expect(taskInfo.status, isNotNull);
        expect(taskInfo.createdAt, isNotNull);
      }
    });

    test('按状态筛选任务', () async {
      final response = await taskService.getTaskList(status: 'COMPLETED');

      expect(response['success'], isTrue);
      expect(response['tasks'], isNotNull);

      final tasks = response['tasks'] as List;
      for (var task in tasks) {
        final taskMap = task as Map<String, dynamic>;
        expect(taskMap['status'], equals('COMPLETED'));
      }
    });

    test('验证任务状态枚举', () {
      final statuses = [
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

      for (var status in statuses) {
        final task = TaskStatus(
          taskId: 'test-task-id',
          status: status,
          createdAt: DateTime.now().millisecondsSinceEpoch,
        );

        expect(task.status, equals(status));
      }
    });

    test('验证任务进度字段兼容性', () {
      final taskWithOverallProgress = TaskStatus(
        taskId: 'test-task-1',
        status: 'PREVIEWING',
        overallProgress: 50.0,
        createdAt: DateTime.now().millisecondsSinceEpoch,
      );

      final taskWithProgress = TaskStatus(
        taskId: 'test-task-2',
        status: 'PREVIEWING',
        overallProgress: 75.0,
        createdAt: DateTime.now().millisecondsSinceEpoch,
      );

      expect(taskWithOverallProgress.overallProgress, equals(50.0));
      expect(taskWithProgress.overallProgress, equals(75.0));
    });

    test('验证任务详情JSON序列化', () {
      final task = TaskStatus(
        taskId: 'test-task-id',
        taskName: '测试任务',
        status: 'PREVIEWING',
        currentStage: 'PREVIEW',
        overallProgress: 50.0,
        message: '正在处理',
        createdAt: DateTime.now().millisecondsSinceEpoch,
      );

      final json = task.toJson();

      expect(json['taskId'], equals('test-task-id'));
      expect(json['taskName'], equals('测试任务'));
      expect(json['status'], equals('PREVIEWING'));
      expect(json['currentStage'], equals('PREVIEW'));
      expect(json['overallProgress'], equals(50.0));
      expect(json['message'], equals('正在处理'));
    });

    test('验证任务详情JSON反序列化', () {
      final json = {
        'taskId': 'test-task-id',
        'taskName': '测试任务',
        'status': 'PREVIEWING',
        'currentStage': 'PREVIEW',
        'overallProgress': 50.0,
        'message': '正在处理',
        'createdAt': 1234567890,
      };

      final task = TaskStatus.fromJson(json);

      expect(task.taskId, equals('test-task-id'));
      expect(task.taskName, equals('测试任务'));
      expect(task.status, equals('PREVIEWING'));
      expect(task.currentStage, equals('PREVIEW'));
      expect(task.overallProgress, equals(50.0));
      expect(task.message, equals('正在处理'));
      expect(task.createdAt, equals(1234567890));
    });

    test('验证进度字段兼容性（overallProgress和progress）', () {
      final jsonWithOverallProgress = {
        'taskId': 'test-task-1',
        'status': 'PREVIEWING',
        'overallProgress': 60.0,
        'createdAt': 1234567890,
      };

      final jsonWithProgress = {
        'taskId': 'test-task-2',
        'status': 'PREVIEWING',
        'progress': 80.0,
        'createdAt': 1234567890,
      };

      final task1 = TaskStatus.fromJson(jsonWithOverallProgress);
      final task2 = TaskStatus.fromJson(jsonWithProgress);

      expect(task1.overallProgress, equals(60.0));
      expect(task2.overallProgress, equals(80.0));
    });

    test('验证空值处理', () {
      final json = {
        'taskId': 'test-task-id',
        'status': 'CREATED',
        'createdAt': 1234567890,
      };

      final task = TaskStatus.fromJson(json);

      expect(task.taskName, isNull);
      expect(task.currentStage, isNull);
      expect(task.message, isNull);
      expect(task.overallProgress, isNull);
      expect(task.configSnapshot, isNull);
      expect(task.stages, isNull);
    });

    test('验证任务配置快照序列化', () {
      final task = TaskStatus(
        taskId: 'test-task-id',
        status: 'PREVIEWING',
        createdAt: DateTime.now().millisecondsSinceEpoch,
        configSnapshot: TaskConfigSnapshot(
          sourceDirectories: [
            SourceDirectoryConfig(
              path: '/test/path',
            ),
          ],
        ),
      );

      final json = task.toJson();

      expect(json['configSnapshot'], isNotNull);
      final configSnapshot = json['configSnapshot'] as Map<String, dynamic>;
      expect(configSnapshot['sourceDirectories'], isNotNull);
    });

    test('验证任务阶段信息', () {
      final task = TaskStatus(
        taskId: 'test-task-id',
        status: 'PREVIEWING',
        createdAt: DateTime.now().millisecondsSinceEpoch,
        stages: TaskStages(
          scan: ScanStage(
            status: 'COMPLETED',
            totalFiles: 100,
          ),
          preview: PreviewStage(
            status: 'IN_PROGRESS',
            analyzedFiles: 50,
            totalChanges: 100,
          ),
          execution: ExecutionStage(
            status: 'PENDING',
          ),
        ),
      );

      final json = task.toJson();

      expect(json['stages'], isNotNull);
      final stages = json['stages'] as Map<String, dynamic>;
      expect(stages['scan'], isNotNull);
      expect(stages['preview'], isNotNull);
      expect(stages['execution'], isNotNull);
    });

    test('验证多任务场景', () async {
      final taskIds = <String>[];

      for (int i = 0; i < 5; i++) {
        final request = TaskRequest(
          strategyId: 'test-strategy-$i',
          filePaths: ['/test/path${i + 1}'],
          strategyConfig: StrategyConfig({
            'strategyName': '多任务测试${i + 1}',
            'description': '测试多任务创建',
          }),
          taskName: '多任务测试${i + 1}',
          description: '测试多任务创建',
        );

        final taskId = await taskService.createTask(request);
        taskIds.add(taskId);
      }

      expect(taskIds.length, equals(5));

      final response = await taskService.getTaskList();
      final tasks = response['tasks'] as List;

      for (var taskId in taskIds) {
        final taskExists = tasks.any((task) {
          final taskMap = task as Map<String, dynamic>;
          return taskMap['taskId'] == taskId;
        });
        expect(taskExists, isTrue, reason: '任务 $taskId 应该存在于任务列表中');
      }
    });

    test('验证任务状态转换', () {
      final validTransitions = {
        'CREATED': ['SCANNING', 'PREVIEWING', 'CANCELLED'],
        'SCANNING': ['SCANNED', 'FAILED', 'CANCELLED'],
        'SCANNED': ['PREVIEWING', 'CANCELLED'],
        'PREVIEWING': ['PREVIEWED', 'FAILED', 'CANCELLED'],
        'PREVIEWED': ['EXECUTING', 'CANCELLED'],
        'EXECUTING': ['COMPLETED', 'FAILED', 'CANCELLED'],
        'COMPLETED': ['SCANNING', 'PREVIEWING', 'CANCELLED'],
        'FAILED': ['SCANNING', 'PREVIEWING', 'CANCELLED'],
        'CANCELLED': ['SCANNING', 'PREVIEWING'],
      };

      validTransitions.forEach((fromStatus, toStatuses) {
        for (var toStatus in toStatuses) {
          final task = TaskStatus(
            taskId: 'test-task-id',
            status: fromStatus,
            createdAt: DateTime.now().millisecondsSinceEpoch,
          );

          final json = task.toJson();
          json['status'] = toStatus;

          final updatedTask = TaskStatus.fromJson(json);
          expect(updatedTask.status, equals(toStatus));
        }
      });
    });
  });
}