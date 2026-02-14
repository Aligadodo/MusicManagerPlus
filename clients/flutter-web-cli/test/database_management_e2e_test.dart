import 'package:flutter_test/flutter_test.dart';
import 'package:filemanager_flutter/api/database_task_service.dart';
import 'package:filemanager_flutter/api/database_config_service.dart';
import 'package:filemanager_flutter/api/api_client.dart';

void main() {
  group('数据库任务管理端到端测试', () {
    late DatabaseTaskService databaseTaskService;
    late DatabaseConfigService databaseConfigService;
    late ApiClient apiClient;

    setUp(() {
      apiClient = ApiClient();
      databaseTaskService = DatabaseTaskService(apiClient);
      databaseConfigService = DatabaseConfigService(apiClient);
    });

    test('1. 验证数据库任务服务连接', () async {
      try {
        final statistics = await databaseTaskService.getStatistics();
        expect(statistics['success'], isTrue);
        expect(statistics['data'], isNotNull);
        expect(statistics['data']['totalTasks'], isNotNull);
        print('✓ 数据库任务服务连接成功');
      } catch (e) {
        fail('数据库任务服务连接失败: $e');
      }
    });

    test('2. 验证数据库配置服务连接', () async {
      try {
        final snapshots = await databaseConfigService.getSnapshots();
        expect(snapshots['success'], isTrue);
        expect(snapshots['data'], isNotNull);
        print('✓ 数据库配置服务连接成功');
      } catch (e) {
        fail('数据库配置服务连接失败: $e');
      }
    });

    test('3. 验证任务列表分页功能', () async {
      try {
        final page1 = await databaseTaskService.getTasks(page: 1, size: 10);
        expect(page1['success'], isTrue);
        expect(page1['data'], isNotNull);
        expect(page1['total'], isNotNull);
        expect(page1['page'], equals(1));
        expect(page1['size'], equals(10));
        print('✓ 任务列表分页功能正常');
      } catch (e) {
        fail('任务列表分页功能失败: $e');
      }
    });

    test('4. 验证任务状态筛选功能', () async {
      try {
        final createdTasks = await databaseTaskService.getTasks(
          page: 1,
          size: 20,
          status: 'CREATED',
        );
        expect(createdTasks['success'], isTrue);
        expect(createdTasks['data'], isNotNull);
        print('✓ 任务状态筛选功能正常');
      } catch (e) {
        fail('任务状态筛选功能失败: $e');
      }
    });

    test('5. 验证任务关键词搜索功能', () async {
      try {
        final searchResults = await databaseTaskService.getTasks(
          page: 1,
          size: 20,
          keyword: 'test',
        );
        expect(searchResults['success'], isTrue);
        expect(searchResults['data'], isNotNull);
        print('✓ 任务关键词搜索功能正常');
      } catch (e) {
        fail('任务关键词搜索功能失败: $e');
      }
    });

    test('6. 验证任务详情查询功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final taskDetail = await databaseTaskService.getTask(taskId);
          expect(taskDetail['success'], isTrue);
          expect(taskDetail['data'], isNotNull);
          expect(taskDetail['data']['taskId'], equals(taskId));
          print('✓ 任务详情查询功能正常');
        } else {
          print('⚠ 没有可用的任务进行详情查询测试');
        }
      } catch (e) {
        fail('任务详情查询功能失败: $e');
      }
    });

    test('7. 验证任务阶段查询功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final taskStages = await databaseTaskService.getTaskStages(taskId);
          expect(taskStages['success'], isTrue);
          expect(taskStages['data'], isNotNull);
          print('✓ 任务阶段查询功能正常');
        } else {
          print('⚠ 没有可用的任务进行阶段查询测试');
        }
      } catch (e) {
        fail('任务阶段查询功能失败: $e');
      }
    });

    test('8. 验证变更记录查询功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final changes = await databaseTaskService.getTaskChanges(
            taskId,
            page: 1,
            size: 20,
          );
          expect(changes['success'], isTrue);
          expect(changes['data'], isNotNull);
          expect(changes['total'], isNotNull);
          print('✓ 变更记录查询功能正常');
        } else {
          print('⚠ 没有可用的任务进行变更记录查询测试');
        }
      } catch (e) {
        fail('变更记录查询功能失败: $e');
      }
    });

    test('9. 验证变更记录状态筛选功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final changes = await databaseTaskService.getTaskChanges(
            taskId,
            page: 1,
            size: 20,
            status: 'SUCCESS',
          );
          expect(changes['success'], isTrue);
          expect(changes['data'], isNotNull);
          print('✓ 变更记录状态筛选功能正常');
        } else {
          print('⚠ 没有可用的任务进行变更记录状态筛选测试');
        }
      } catch (e) {
        fail('变更记录状态筛选功能失败: $e');
      }
    });

    test('10. 验证变更记录操作类型筛选功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final changes = await databaseTaskService.getTaskChanges(
            taskId,
            page: 1,
            size: 20,
            operationType: 'RENAME',
          );
          expect(changes['success'], isTrue);
          expect(changes['data'], isNotNull);
          print('✓ 变更记录操作类型筛选功能正常');
        } else {
          print('⚠ 没有可用的任务进行变更记录操作类型筛选测试');
        }
      } catch (e) {
        fail('变更记录操作类型筛选功能失败: $e');
      }
    });

    test('11. 验证变更记录关键词搜索功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final changes = await databaseTaskService.getTaskChanges(
            taskId,
            page: 1,
            size: 20,
            keyword: 'test',
          );
          expect(changes['success'], isTrue);
          expect(changes['data'], isNotNull);
          print('✓ 变更记录关键词搜索功能正常');
        } else {
          print('⚠ 没有可用的任务进行变更记录关键词搜索测试');
        }
      } catch (e) {
        fail('变更记录关键词搜索功能失败: $e');
      }
    });

    test('12. 验证任务日志查询功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final logs = await databaseTaskService.getTaskLogs(
            taskId,
            page: 1,
            size: 20,
          );
          expect(logs['success'], isTrue);
          expect(logs['data'], isNotNull);
          expect(logs['total'], isNotNull);
          print('✓ 任务日志查询功能正常');
        } else {
          print('⚠ 没有可用的任务进行日志查询测试');
        }
      } catch (e) {
        fail('任务日志查询功能失败: $e');
      }
    });

    test('13. 验证配置快照列表查询功能', () async {
      try {
        final snapshots = await databaseConfigService.getSnapshots(
          page: 1,
          size: 20,
        );
        expect(snapshots['success'], isTrue);
        expect(snapshots['data'], isNotNull);
        expect(snapshots['total'], isNotNull);
        print('✓ 配置快照列表查询功能正常');
      } catch (e) {
        fail('配置快照列表查询功能失败: $e');
      }
    });

    test('14. 验证配置快照类型筛选功能', () async {
      try {
        final snapshots = await databaseConfigService.getSnapshots(
          page: 1,
          size: 20,
          type: 'PIPELINE',
        );
        expect(snapshots['success'], isTrue);
        expect(snapshots['data'], isNotNull);
        print('✓ 配置快照类型筛选功能正常');
      } catch (e) {
        fail('配置快照类型筛选功能失败: $e');
      }
    });

    test('15. 验证配置模板列表查询功能', () async {
      try {
        final templates = await databaseConfigService.getTemplates(
          page: 1,
          size: 20,
        );
        expect(templates['success'], isTrue);
        expect(templates['data'], isNotNull);
        expect(templates['total'], isNotNull);
        print('✓ 配置模板列表查询功能正常');
      } catch (e) {
        fail('配置模板列表查询功能失败: $e');
      }
    });

    test('16. 验证配置模板分类筛选功能', () async {
      try {
        final templates = await databaseConfigService.getTemplates(
          page: 1,
          size: 20,
          category: 'default',
        );
        expect(templates['success'], isTrue);
        expect(templates['data'], isNotNull);
        print('✓ 配置模板分类筛选功能正常');
      } catch (e) {
        fail('配置模板分类筛选功能失败: $e');
      }
    });

    test('17. 验证系统配置列表查询功能', () async {
      try {
        final configs = await databaseConfigService.getSystemConfigs(
          page: 1,
          size: 20,
        );
        expect(configs['success'], isTrue);
        expect(configs['data'], isNotNull);
        expect(configs['total'], isNotNull);
        print('✓ 系统配置列表查询功能正常');
      } catch (e) {
        fail('系统配置列表查询功能失败: $e');
      }
    });

    test('18. 验证系统配置分类筛选功能', () async {
      try {
        final configs = await databaseConfigService.getSystemConfigs(
          page: 1,
          size: 20,
          category: 'database',
        );
        expect(configs['success'], isTrue);
        expect(configs['data'], isNotNull);
        print('✓ 系统配置分类筛选功能正常');
      } catch (e) {
        fail('系统配置分类筛选功能失败: $e');
      }
    });

    test('19. 验证统计信息查询功能', () async {
      try {
        final statistics = await databaseTaskService.getStatistics();
        expect(statistics['success'], isTrue);
        expect(statistics['data'], isNotNull);
        expect(statistics['data']['totalTasks'], isNotNull);
        expect(statistics['data']['totalChanges'], isNotNull);
        expect(statistics['data']['totalLogs'], isNotNull);
        print('✓ 统计信息查询功能正常');
      } catch (e) {
        fail('统计信息查询功能失败: $e');
      }
    });

    test('20. 验证组合查询功能（状态 + 关键词）', () async {
      try {
        final tasks = await databaseTaskService.getTasks(
          page: 1,
          size: 20,
          status: 'COMPLETED',
          keyword: 'test',
        );
        expect(tasks['success'], isTrue);
        expect(tasks['data'], isNotNull);
        print('✓ 组合查询功能（状态 + 关键词）正常');
      } catch (e) {
        fail('组合查询功能失败: $e');
      }
    });

    test('21. 验证变更记录组合查询功能', () async {
      try {
        final tasks = await databaseTaskService.getTasks(page: 1, size: 1);
        if (tasks['data'].isNotEmpty) {
          final taskId = tasks['data'][0]['taskId'];
          final changes = await databaseTaskService.getTaskChanges(
            taskId,
            page: 1,
            size: 20,
            status: 'SUCCESS',
            operationType: 'RENAME',
            keyword: 'test',
          );
          expect(changes['success'], isTrue);
          expect(changes['data'], isNotNull);
          print('✓ 变更记录组合查询功能正常');
        } else {
          print('⚠ 没有可用的任务进行变更记录组合查询测试');
        }
      } catch (e) {
        fail('变更记录组合查询功能失败: $e');
      }
    });

    test('22. 验证分页边界条件', () async {
      try {
        final page1 = await databaseTaskService.getTasks(page: 1, size: 5);
        expect(page1['success'], isTrue);
        
        final page999 = await databaseTaskService.getTasks(page: 999, size: 5);
        expect(page999['success'], isTrue);
        expect((page999['data'] as List).isEmpty, isTrue);
        
        print('✓ 分页边界条件测试正常');
      } catch (e) {
        fail('分页边界条件测试失败: $e');
      }
    });

    test('23. 验证空结果处理', () async {
      try {
        final tasks = await databaseTaskService.getTasks(
          page: 1,
          size: 20,
          keyword: 'nonexistent_task_id_123456789',
        );
        expect(tasks['success'], isTrue);
        expect(tasks['data'], isNotNull);
        expect((tasks['data'] as List).isEmpty, isTrue);
        
        print('✓ 空结果处理测试正常');
      } catch (e) {
        fail('空结果处理测试失败: $e');
      }
    });

    test('24. 验证API响应格式一致性', () async {
      try {
        final statistics = await databaseTaskService.getStatistics();
        expect(statistics['success'], isTrue);
        expect(statistics.containsKey('data'), isTrue);
        
        final tasks = await databaseTaskService.getTasks();
        expect(tasks['success'], isTrue);
        expect(tasks.containsKey('data'), isTrue);
        expect(tasks.containsKey('total'), isTrue);
        expect(tasks.containsKey('page'), isTrue);
        expect(tasks.containsKey('size'), isTrue);
        
        print('✓ API响应格式一致性测试正常');
      } catch (e) {
        fail('API响应格式一致性测试失败: $e');
      }
    });

    test('25. 验证错误处理机制', () async {
      try {
        await databaseTaskService.getTask('invalid_task_id');
        fail('应该抛出异常');
      } catch (e) {
        expect(e, isA<Exception>());
        print('✓ 错误处理机制测试正常');
      }
    });

    test('26. 验证并发查询稳定性', () async {
      try {
        final futures = <Future>[];
        
        for (int i = 0; i < 10; i++) {
          futures.add(databaseTaskService.getTasks(page: 1, size: 20));
        }
        
        final results = await Future.wait(futures);
        
        for (final result in results) {
          expect(result['success'], isTrue);
        }
        
        print('✓ 并发查询稳定性测试正常');
      } catch (e) {
        fail('并发查询稳定性测试失败: $e');
      }
    });

    test('27. 验证大数据量查询性能', () async {
      try {
        final stopwatch = Stopwatch()..start();
        
        final tasks = await databaseTaskService.getTasks(
          page: 1,
          size: 100,
        );
        
        stopwatch.stop();
        
        expect(tasks['success'], isTrue);
        expect(stopwatch.elapsedMilliseconds, lessThan(5000));
        
        print('✓ 大数据量查询性能测试正常 (耗时: ${stopwatch.elapsedMilliseconds}ms)');
      } catch (e) {
        fail('大数据量查询性能测试失败: $e');
      }
    });

    test('28. 验证配置快照创建功能', () async {
      try {
        final snapshotData = {
          'snapshotId': 'test_snapshot_${DateTime.now().millisecondsSinceEpoch}',
          'snapshotName': '测试快照',
          'snapshotType': 'PIPELINE',
          'configData': '{"test": "data"}',
          'description': '这是一个测试快照',
          'isTemplate': false,
        };
        
        final result = await databaseConfigService.createSnapshot(snapshotData);
        expect(result['success'], isTrue);
        expect(result['data'], isNotNull);
        
        print('✓ 配置快照创建功能正常');
      } catch (e) {
        fail('配置快照创建功能失败: $e');
      }
    });

    test('29. 验证配置模板创建功能', () async {
      try {
        final templateData = {
          'templateId': 'test_template_${DateTime.now().millisecondsSinceEpoch}',
          'templateName': '测试模板',
          'templateType': 'PIPELINE',
          'snapshotId': 'test_snapshot',
          'category': 'test',
          'description': '这是一个测试模板',
          'isDefault': false,
        };
        
        final result = await databaseConfigService.createTemplate(templateData);
        expect(result['success'], isTrue);
        expect(result['data'], isNotNull);
        
        print('✓ 配置模板创建功能正常');
      } catch (e) {
        fail('配置模板创建功能失败: $e');
      }
    });

    test('30. 验证系统配置创建功能', () async {
      try {
        final configData = {
          'configKey': 'test_config_${DateTime.now().millisecondsSinceEpoch}',
          'configValue': 'test_value',
          'configType': 'STRING',
          'description': '这是一个测试配置',
          'category': 'test',
          'isEncrypted': false,
        };
        
        final result = await databaseConfigService.createSystemConfig(configData);
        expect(result['success'], isTrue);
        expect(result['data'], isNotNull);
        
        print('✓ 系统配置创建功能正常');
      } catch (e) {
        fail('系统配置创建功能失败: $e');
      }
    });
  });
}
