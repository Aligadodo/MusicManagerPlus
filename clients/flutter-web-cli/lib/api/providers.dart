import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/file_service.dart';
import 'package:filemanager_flutter/api/strategy_service.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/api/thread_pool_service.dart';
import 'package:filemanager_flutter/api/log_service.dart';
import 'package:filemanager_flutter/api/config_service.dart';
import 'package:filemanager_flutter/api/database_task_service.dart';
import 'package:filemanager_flutter/api/database_config_service.dart';

// 创建 ApiClient 实例的 provider
final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient();
});

// 创建 FileService 实例的 provider
final fileServiceProvider = Provider<FileService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return FileService(apiClient);
});

// 创建 StrategyService 实例的 provider
final strategyServiceProvider = Provider<StrategyService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return StrategyService(apiClient);
});

// 创建 TaskService 实例的 provider
final taskServiceProvider = Provider<TaskService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return TaskService(apiClient);
});

// 创建 SourceDirectoryService 实例的 provider
final sourceDirectoryServiceProvider = Provider<SourceDirectoryService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return SourceDirectoryService(apiClient);
});

// 创建 PipelineService 实例的 provider
final pipelineServiceProvider = Provider<PipelineService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return PipelineService(apiClient);
});

// 创建 ThreadPoolService 实例的 provider
final threadPoolServiceProvider = Provider<ThreadPoolService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ThreadPoolService(apiClient);
});

// 创建 LogService 实例的 provider
final logServiceProvider = Provider<LogService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return LogService(apiClient);
});

// 创建 ConfigService 实例的 provider
final configServiceProvider = Provider<ConfigService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return ConfigService(apiClient);
});

// 创建 DatabaseTaskService 实例的 provider
final databaseTaskServiceProvider = Provider<DatabaseTaskService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DatabaseTaskService(apiClient);
});

// 创建 DatabaseConfigService 实例的 provider
final databaseConfigServiceProvider = Provider<DatabaseConfigService>((ref) {
  final apiClient = ref.watch(apiClientProvider);
  return DatabaseConfigService(apiClient);
});
