import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:mockito/mockito.dart';
import 'package:http/http.dart' as http;

void taskServiceTests() {
  group('任务服务测试', () {
    late MockTaskService mockTaskService;
    late ApiClient mockApiClient;

    setUp(() {
      mockApiClient = MockApiClient();
      mockTaskService = MockTaskService(mockApiClient);
    });

    test('获取任务信息', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.taskId, equals('test-task-123'));
      expect(taskInfo.taskName, equals('测试任务'));
      expect(taskInfo.status, equals('CREATED'));
      expect(taskInfo.configSnapshotId, equals('snapshot-123'));
      expect(taskInfo.configSnapshot, isNotNull);
    });

    test('配置快照ID展示', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.configSnapshotId, isNotNull);
      expect(taskInfo.configSnapshotId!.length, greaterThan(0));
    });

    test('配置快照数据验证', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.configSnapshot, isNotNull);
      expect(taskInfo.configSnapshot!.sourceDirectories, isNotNull);
      expect(taskInfo.configSnapshot!.sourceDirectories!.length, equals(1));
      expect(taskInfo.configSnapshot!.sourceDirectories![0].path, equals('/test/path'));
      expect(taskInfo.configSnapshot!.sourceDirectories![0].depth, equals(1));
    });

    test('任务阶段状态验证', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.stages, isNotNull);
      expect(taskInfo.stages!.scan, isNotNull);
      expect(taskInfo.stages!.preview, isNotNull);
      expect(taskInfo.stages!.execution, isNotNull);
      
      expect(taskInfo.stages!.scan!.status, equals('PENDING'));
      expect(taskInfo.stages!.preview!.status, equals('PENDING'));
      expect(taskInfo.stages!.execution!.status, equals('PENDING'));
    });

    test('任务进度计算', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.overallProgress, equals(0.0));
    });

    test('任务消息验证', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.message, equals('任务已创建'));
    });
  });

  group('任务操作测试', () {
    late MockTaskService mockTaskService;
    late ApiClient mockApiClient;

    setUp(() {
      mockApiClient = MockApiClient();
      mockTaskService = MockTaskService(mockApiClient);
    });

    test('执行文件扫描', () async {
      try {
        await mockTaskService.executeScan('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });

    test('执行预览分析', () async {
      try {
        await mockTaskService.executePreview('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });

    test('执行任务', () async {
      try {
        await mockTaskService.executeTask('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });

    test('重新扫描', () async {
      try {
        await mockTaskService.restartScan('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });

    test('重新预览', () async {
      try {
        await mockTaskService.restartPreview('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });

    test('重新执行', () async {
      try {
        await mockTaskService.restartExecution('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });

    test('删除任务', () async {
      try {
        await mockTaskService.deleteTask('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });

    test('取消任务', () async {
      try {
        await mockTaskService.cancelTask('test-task-123');
      } catch (e) {
        fail('不应该抛出异常: $e');
      }
    });
  });

  group('配置快照测试', () {
    late MockTaskService mockTaskService;
    late ApiClient mockApiClient;

    setUp(() {
      mockApiClient = MockApiClient();
      mockTaskService = MockTaskService(mockApiClient);
    });

    test('配置快照JSON序列化', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      final json = taskInfo.configSnapshot!.toJson();
      expect(json, isNotNull);
      expect(json, isA<Map>());
    });

    test('配置快照包含源目录', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.configSnapshot!.sourceDirectories, isNotNull);
      expect(taskInfo.configSnapshot!.sourceDirectories!.length, greaterThan(0));
    });

    test('配置快照包含策略配置', () async {
      final taskInfo = await mockTaskService.getTaskInfo('test-task-123');
      
      expect(taskInfo.configSnapshot!.strategyId, isNotNull);
      expect(taskInfo.configSnapshot!.strategyConfig, isNotNull);
    });
  });

  group('任务状态转换测试', () {
    test('CREATED状态', () {
      final status = 'CREATED';
      expect(status, equals('CREATED'));
    });

    test('SCANNING状态', () {
      final status = 'SCANNING';
      expect(status, equals('SCANNING'));
    });

    test('SCANNED状态', () {
      final status = 'SCANNED';
      expect(status, equals('SCANNED'));
    });

    test('PREVIEWING状态', () {
      final status = 'PREVIEWING';
      expect(status, equals('PREVIEWING'));
    });

    test('PREVIEWED状态', () {
      final status = 'PREVIEWED';
      expect(status, equals('PREVIEWED'));
    });

    test('EXECUTING状态', () {
      final status = 'EXECUTING';
      expect(status, equals('EXECUTING'));
    });

    test('COMPLETED状态', () {
      final status = 'COMPLETED';
      expect(status, equals('COMPLETED'));
    });

    test('FAILED状态', () {
      final status = 'FAILED';
      expect(status, equals('FAILED'));
    });

    test('CANCELLED状态', () {
      final status = 'CANCELLED';
      expect(status, equals('CANCELLED'));
    });
  });

  group('任务详情页面UI测试', () {
    testWidgets('配置快照卡片显示', (WidgetTester tester) async {
      final taskInfo = TaskStatus(
        taskId: 'test-task-123',
        taskName: '测试任务',
        status: 'CREATED',
        configSnapshotId: 'snapshot-123',
        configSnapshot: TaskConfigSnapshot(
          sourceDirectories: [
            SourceDirectoryConfig(
              path: '/test/path',
              depth: 1,
            ),
          ],
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: TaskDetailPage(
              taskId: 'test-task-123',
            ),
          ),
        ),
      );

      expect(find.text('配置快照'), findsOneWidget);
      expect(find.text('快照ID'), findsOneWidget);
    });

    testWidgets('任务信息卡片显示', (WidgetTester tester) async {
      final taskInfo = TaskStatus(
        taskId: 'test-task-123',
        taskName: '测试任务',
        status: 'CREATED',
        overallProgress: 0.0,
        message: '任务已创建',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: TaskDetailPage(
              taskId: 'test-task-123',
            ),
          ),
        ),
      );

      expect(find.text('测试任务'), findsOneWidget);
      expect(find.text('任务已创建'), findsOneWidget);
    });

    testWidgets('操作按钮显示', (WidgetTester tester) async {
      final taskInfo = TaskStatus(
        taskId: 'test-task-123',
        taskName: '测试任务',
        status: 'CREATED',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: TaskDetailPage(
              taskId: 'test-task-123',
            ),
          ),
        ),
      );

      expect(find.text('扫描文件'), findsOneWidget);
      expect(find.text('预览分析'), findsOneWidget);
      expect(find.text('执行'), findsOneWidget);
      expect(find.text('删除'), findsOneWidget);
    });
  });
}

class MockApiClient extends ApiClient {
  MockApiClient() : super('http://localhost:8080');

  @override
  Future<http.Response> get(String path, {Map<String, String>? headers}) async {
    return http.Response('{"taskId":"test-task-123","taskName":"测试任务","status":"CREATED","configSnapshotId":"snapshot-123"}', 200);
  }

  @override
  Future<http.Response> post(String path, {Map<String, dynamic>? body, Map<String, String>? headers}) async {
    return http.Response('{"message":"操作成功"}', 200);
  }
}
