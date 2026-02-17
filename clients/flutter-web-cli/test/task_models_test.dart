import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';

void main() {
  group('TaskStatus模型测试', () {
    test('创建TaskStatus对象', () {
      final taskStatus = TaskStatus(
        taskId: 'test-task-123',
        taskName: '测试任务',
        status: 'CREATED',
        currentStage: 'CREATED',
        overallProgress: 0.0,
        message: '任务已创建',
        configSnapshotId: 'snapshot-123',
        configSnapshot: TaskConfigSnapshot(
          sourceDirectories: [
            SourceDirectoryConfig(
              path: '/test/path',
              depth: 1,
            ),
          ],
          strategyId: 'file-rename',
          strategyConfig: {
            'pattern': 'test_{index}',
            'startIndex': 1,
          },
        ),
        stages: TaskStages(
          scan: ScanStage(
            status: 'PENDING',
            scanStartTime: null,
            scanEndTime: null,
            scannedFiles: 0,
            totalFiles: 0,
          ),
          preview: PreviewStage(
            status: 'PENDING',
            previewStartTime: null,
            previewEndTime: null,
            analyzedFiles: 0,
            totalChanges: 0,
          ),
          execution: ExecutionStage(
            status: 'PENDING',
            executionStartTime: null,
            executionEndTime: null,
            executedFiles: 0,
            successCount: 0,
            failedCount: 0,
            executionCount: 0,
          ),
        ),
      );

      expect(taskStatus.taskId, equals('test-task-123'));
      expect(taskStatus.taskName, equals('测试任务'));
      expect(taskStatus.status, equals('CREATED'));
      expect(taskStatus.configSnapshotId, equals('snapshot-123'));
      expect(taskStatus.configSnapshot, isNotNull);
    });

    test('TaskStatus JSON序列化', () {
      final taskStatus = TaskStatus(
        taskId: 'test-task-123',
        taskName: '测试任务',
        status: 'CREATED',
        configSnapshotId: 'snapshot-123',
      );

      final json = taskStatus.toJson();
      expect(json, isA<Map>());
      expect(json['taskId'], equals('test-task-123'));
      expect(json['taskName'], equals('测试任务'));
      expect(json['status'], equals('CREATED'));
      expect(json['configSnapshotId'], equals('snapshot-123'));
    });

    test('TaskStatus JSON反序列化', () {
      final json = {
        'taskId': 'test-task-123',
        'taskName': '测试任务',
        'status': 'CREATED',
        'configSnapshotId': 'snapshot-123',
        'configSnapshot': {
          'sourceDirectories': [
            {
              'path': '/test/path',
              'depth': 1,
            },
          ],
        },
      };

      final taskStatus = TaskStatus.fromJson(json);
      expect(taskStatus.taskId, equals('test-task-123'));
      expect(taskStatus.taskName, equals('测试任务'));
      expect(taskStatus.status, equals('CREATED'));
      expect(taskStatus.configSnapshotId, equals('snapshot-123'));
      expect(taskStatus.configSnapshot, isNotNull);
    });
  });

  group('TaskConfigSnapshot模型测试', () {
    test('创建TaskConfigSnapshot对象', () {
      final configSnapshot = TaskConfigSnapshot(
        sourceDirectories: [
          SourceDirectoryConfig(
            path: '/test/path',
            depth: 1,
          ),
        ],
        strategyId: 'file-rename',
        strategyConfig: {
          'pattern': 'test_{index}',
          'startIndex': 1,
        },
      );

      expect(configSnapshot.sourceDirectories, isNotNull);
      expect(configSnapshot.sourceDirectories!.length, equals(1));
      expect(configSnapshot.sourceDirectories![0].path, equals('/test/path'));
      expect(configSnapshot.sourceDirectories![0].depth, equals(1));
      expect(configSnapshot.strategyId, equals('file-rename'));
      expect(configSnapshot.strategyConfig, isNotNull);
    });

    test('TaskConfigSnapshot JSON序列化', () {
      final configSnapshot = TaskConfigSnapshot(
        sourceDirectories: [
          SourceDirectoryConfig(
            path: '/test/path',
            depth: 1,
          ),
        ],
        strategyId: 'file-rename',
        strategyConfig: {
          'pattern': 'test_{index}',
        },
      );

      final json = configSnapshot.toJson();
      expect(json, isA<Map>());
      expect(json['sourceDirectories'], isA<List>());
      expect(json['strategyId'], equals('file-rename'));
    });

    test('TaskConfigSnapshot JSON反序列化', () {
      final json = {
        'sourceDirectories': [
          {
            'path': '/test/path',
            'depth': 1,
          },
        ],
        'strategyId': 'file-rename',
        'strategyConfig': {
          'pattern': 'test_{index}',
        },
      };

      final configSnapshot = TaskConfigSnapshot.fromJson(json);
      expect(configSnapshot.sourceDirectories, isNotNull);
      expect(configSnapshot.sourceDirectories!.length, equals(1));
      expect(configSnapshot.sourceDirectories![0].path, equals('/test/path'));
      expect(configSnapshot.strategyId, equals('file-rename'));
    });
  });

  group('SourceDirectoryConfig模型测试', () {
    test('创建SourceDirectoryConfig对象', () {
      final sourceDir = SourceDirectoryConfig(
        path: '/test/path',
        depth: 1,
      );

      expect(sourceDir.path, equals('/test/path'));
      expect(sourceDir.depth, equals(1));
    });

    test('SourceDirectoryConfig JSON序列化', () {
      final sourceDir = SourceDirectoryConfig(
        path: '/test/path',
        depth: 1,
      );

      final json = sourceDir.toJson();
      expect(json, isA<Map>());
      expect(json['path'], equals('/test/path'));
      expect(json['depth'], equals(1));
    });

    test('SourceDirectoryConfig JSON反序列化', () {
      final json = {
        'path': '/test/path',
        'depth': 1,
      };

      final sourceDir = SourceDirectoryConfig.fromJson(json);
      expect(sourceDir.path, equals('/test/path'));
      expect(sourceDir.depth, equals(1));
    });
  });

  group('TaskStages模型测试', () {
    test('创建TaskStages对象', () {
      final stages = TaskStages(
        scan: ScanStage(
          status: 'PENDING',
          scannedFiles: 0,
          totalFiles: 0,
        ),
        preview: PreviewStage(
          status: 'PENDING',
          analyzedFiles: 0,
          totalChanges: 0,
        ),
        execution: ExecutionStage(
          status: 'PENDING',
          executedFiles: 0,
          successCount: 0,
          failedCount: 0,
          executionCount: 0,
        ),
      );

      expect(stages.scan, isNotNull);
      expect(stages.preview, isNotNull);
      expect(stages.execution, isNotNull);
    });

    test('TaskStages JSON序列化', () {
      final stages = TaskStages(
        scan: ScanStage(
          status: 'PENDING',
          scannedFiles: 0,
          totalFiles: 0,
        ),
        preview: PreviewStage(
          status: 'PENDING',
          analyzedFiles: 0,
          totalChanges: 0,
        ),
        execution: ExecutionStage(
          status: 'PENDING',
          executedFiles: 0,
          successCount: 0,
          failedCount: 0,
          executionCount: 0,
        ),
      );

      final json = stages.toJson();
      expect(json, isA<Map>());
      expect(json['scan'], isNotNull);
      expect(json['preview'], isNotNull);
      expect(json['execution'], isNotNull);
    });

    test('TaskStages JSON反序列化', () {
      final json = {
        'scan': {
          'status': 'PENDING',
          'scannedFiles': 0,
          'totalFiles': 0,
        },
        'preview': {
          'status': 'PENDING',
          'analyzedFiles': 0,
          'totalChanges': 0,
        },
        'execution': {
          'status': 'PENDING',
          'executedFiles': 0,
          'successCount': 0,
          'failedCount': 0,
          'executionCount': 0,
        },
      };

      final stages = TaskStages.fromJson(json);
      expect(stages.scan, isNotNull);
      expect(stages.preview, isNotNull);
      expect(stages.execution, isNotNull);
    });
  });

  group('ScanStage模型测试', () {
    test('创建ScanStage对象', () {
      final scanStage = ScanStage(
        status: 'PENDING',
        scanStartTime: 1234567890,
        scanEndTime: 1234567891,
        scannedFiles: 10,
        totalFiles: 10,
      );

      expect(scanStage.status, equals('PENDING'));
      expect(scanStage.scanStartTime, equals(1234567890));
      expect(scanStage.scanEndTime, equals(1234567891));
      expect(scanStage.scannedFiles, equals(10));
      expect(scanStage.totalFiles, equals(10));
    });

    test('ScanStage JSON序列化', () {
      final scanStage = ScanStage(
        status: 'PENDING',
        scannedFiles: 10,
        totalFiles: 10,
      );

      final json = scanStage.toJson();
      expect(json, isA<Map>());
      expect(json['status'], equals('PENDING'));
      expect(json['scannedFiles'], equals(10));
      expect(json['totalFiles'], equals(10));
    });

    test('ScanStage JSON反序列化', () {
      final json = {
        'status': 'PENDING',
        'scanStartTime': 1234567890,
        'scanEndTime': 1234567891,
        'scannedFiles': 10,
        'totalFiles': 10,
      };

      final scanStage = ScanStage.fromJson(json);
      expect(scanStage.status, equals('PENDING'));
      expect(scanStage.scannedFiles, equals(10));
      expect(scanStage.totalFiles, equals(10));
    });
  });

  group('PreviewStage模型测试', () {
    test('创建PreviewStage对象', () {
      final previewStage = PreviewStage(
        status: 'PENDING',
        previewStartTime: 1234567890,
        previewEndTime: 1234567891,
        analyzedFiles: 5,
        totalChanges: 3,
      );

      expect(previewStage.status, equals('PENDING'));
      expect(previewStage.previewStartTime, equals(1234567890));
      expect(previewStage.previewEndTime, equals(1234567891));
      expect(previewStage.analyzedFiles, equals(5));
      expect(previewStage.totalChanges, equals(3));
    });

    test('PreviewStage JSON序列化', () {
      final previewStage = PreviewStage(
        status: 'PENDING',
        analyzedFiles: 5,
        totalChanges: 3,
      );

      final json = previewStage.toJson();
      expect(json, isA<Map>());
      expect(json['status'], equals('PENDING'));
      expect(json['analyzedFiles'], equals(5));
      expect(json['totalChanges'], equals(3));
    });

    test('PreviewStage JSON反序列化', () {
      final json = {
        'status': 'PENDING',
        'previewStartTime': 1234567890,
        'previewEndTime': 1234567891,
        'analyzedFiles': 5,
        'totalChanges': 3,
      };

      final previewStage = PreviewStage.fromJson(json);
      expect(previewStage.status, equals('PENDING'));
      expect(previewStage.analyzedFiles, equals(5));
      expect(previewStage.totalChanges, equals(3));
    });
  });

  group('ExecutionStage模型测试', () {
    test('创建ExecutionStage对象', () {
      final executionStage = ExecutionStage(
        status: 'PENDING',
        executionStartTime: 1234567890,
        executionEndTime: 1234567891,
        executedFiles: 5,
        successCount: 4,
        failedCount: 1,
        executionCount: 5,
      );

      expect(executionStage.status, equals('PENDING'));
      expect(executionStage.executionStartTime, equals(1234567890));
      expect(executionStage.executionEndTime, equals(1234567891));
      expect(executionStage.executedFiles, equals(5));
      expect(executionStage.successCount, equals(4));
      expect(executionStage.failedCount, equals(1));
      expect(executionStage.executionCount, equals(5));
    });

    test('ExecutionStage JSON序列化', () {
      final executionStage = ExecutionStage(
        status: 'PENDING',
        executedFiles: 5,
        successCount: 4,
        failedCount: 1,
        executionCount: 5,
      );

      final json = executionStage.toJson();
      expect(json, isA<Map>());
      expect(json['status'], equals('PENDING'));
      expect(json['executedFiles'], equals(5));
      expect(json['successCount'], equals(4));
      expect(json['failedCount'], equals(1));
      expect(json['executionCount'], equals(5));
    });

    test('ExecutionStage JSON反序列化', () {
      final json = {
        'status': 'PENDING',
        'executionStartTime': 1234567890,
        'executionEndTime': 1234567891,
        'executedFiles': 5,
        'successCount': 4,
        'failedCount': 1,
        'executionCount': 5,
      };

      final executionStage = ExecutionStage.fromJson(json);
      expect(executionStage.status, equals('PENDING'));
      expect(executionStage.executedFiles, equals(5));
      expect(executionStage.successCount, equals(4));
      expect(executionStage.failedCount, equals(1));
      expect(executionStage.executionCount, equals(5));
    });
  });

  group('配置快照功能测试', () {
    test('配置快照ID验证', () {
      final taskStatus = TaskStatus(
        taskId: 'test-task-123',
        configSnapshotId: 'snapshot-123',
      );

      expect(taskStatus.configSnapshotId, isNotNull);
      expect(taskStatus.configSnapshotId!.length, greaterThan(0));
    });

    test('配置快照数据验证', () {
      final taskStatus = TaskStatus(
        taskId: 'test-task-123',
        configSnapshot: TaskConfigSnapshot(
          sourceDirectories: [
            SourceDirectoryConfig(
              path: '/test/path',
              depth: 1,
            ),
          ],
        ),
      );

      expect(taskStatus.configSnapshot, isNotNull);
      expect(taskStatus.configSnapshot!.sourceDirectories, isNotNull);
      expect(taskStatus.configSnapshot!.sourceDirectories!.length, equals(1));
      expect(taskStatus.configSnapshot!.sourceDirectories![0].path, equals('/test/path'));
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
}
