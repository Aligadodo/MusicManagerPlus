import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class DatabaseTaskService {
  final ApiClient _apiClient;

  DatabaseTaskService(this._apiClient);

  Future<Map<String, dynamic>> getTasks({
    int page = 1,
    int size = 20,
    String? status,
    String? keyword,
  }) async {
    String queryParams = 'page=$page&size=$size';
    if (status != null && status.isNotEmpty) {
      queryParams += '&status=$status';
    }
    if (keyword != null && keyword.isNotEmpty) {
      queryParams += '&keyword=$keyword';
    }

    final response = await _apiClient.get('/api/database/tasks?$queryParams');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get tasks: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTask(String taskId) async {
    final response = await _apiClient.get('/api/database/tasks/$taskId');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTaskStages(String taskId) async {
    final response = await _apiClient.get('/api/database/tasks/$taskId/stages');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get task stages: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTaskChanges(
    String taskId, {
    int page = 1,
    int size = 20,
    String? status,
    String? operationType,
    String? keyword,
  }) async {
    String queryParams = 'page=$page&size=$size';
    if (status != null && status.isNotEmpty) {
      queryParams += '&status=$status';
    }
    if (operationType != null && operationType.isNotEmpty) {
      queryParams += '&operationType=$operationType';
    }
    if (keyword != null && keyword.isNotEmpty) {
      queryParams += '&keyword=$keyword';
    }

    final response = await _apiClient.get(
      '/api/database/tasks/$taskId/changes?$queryParams',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get task changes: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTaskLogs(
    String taskId, {
    int page = 1,
    int size = 20,
  }) async {
    String queryParams = 'page=$page&size=$size';

    final response = await _apiClient.get(
      '/api/database/tasks/$taskId/logs?$queryParams',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get task logs: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> deleteTask(String taskId) async {
    final response = await _apiClient.delete('/api/database/tasks/$taskId');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to delete task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getStatistics() async {
    final response = await _apiClient.get('/api/database/tasks/statistics');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get statistics: ${response.statusCode}');
    }
  }
}
