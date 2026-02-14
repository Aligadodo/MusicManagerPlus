import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/task_request.dart';
import 'package:filemanager_flutter/models/task_status.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

class TaskService {
  final ApiClient _apiClient;

  TaskService(this._apiClient);

  Future<String> createTask(TaskRequest request) async {
    final response = await _apiClient.post(
      '/api/tasks',
      body: request.toJson(),
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return data['taskId'] as String;
    } else {
      throw Exception('Failed to create task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTaskList({
    int page = 1,
    int size = 20,
    String? status,
  }) async {
    String queryParams = 'page=$page&size=$size';
    if (status != null) {
      queryParams += '&status=$status';
    }

    final response = await _apiClient.get('/api/tasks?$queryParams');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get task list: ${response.statusCode}');
    }
  }

  Future<TaskStatus> getTaskInfo(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId');

    if (response.statusCode == 200) {
      final Map<String, dynamic> responseData = jsonDecode(response.body);
      if (responseData['success'] == true && responseData['data'] != null) {
        return TaskStatus.fromJson(responseData['data'] as Map<String, dynamic>);
      } else {
        throw Exception('Failed to get task info: ${responseData['message'] ?? 'Unknown error'}');
      }
    } else {
      throw Exception('Failed to get task info: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTaskProgress(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId/progress');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get task progress: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> cancelTask(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/cancel');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to cancel task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> deleteTask(String taskId) async {
    final response = await _apiClient.delete('/api/tasks/$taskId');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to delete task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> executeScan(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/scan');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to execute scan: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> executePreview(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/preview');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to execute preview: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> executeTask(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/execute');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to execute task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> executeSelected(String taskId, List<String> selectedRecordIds) async {
    final response = await _apiClient.post(
      '/api/tasks/$taskId/execute/selected',
      body: jsonEncode(selectedRecordIds),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to execute selected: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> retryFailed(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/retry');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to retry failed: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> restartScan(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/restart/scan');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to restart scan: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> restartPreview(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/restart/preview');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to restart preview: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> restartExecution(String taskId) async {
    final response = await _apiClient.post('/api/tasks/$taskId/restart/execution');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to restart execution: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getScanResults(
    String taskId, {
    int page = 1,
    int size = 20,
  }) async {
    final response = await _apiClient.get(
      '/api/tasks/$taskId/scan/results?page=$page&size=$size',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get scan results: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getScanStatistics(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId/scan/statistics');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get scan statistics: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getPreviewResults(
    String taskId, {
    int page = 1,
    int size = 20,
  }) async {
    final response = await _apiClient.get(
      '/api/tasks/$taskId/preview/results?page=$page&size=$size',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get preview results: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getPreviewStatistics(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId/preview/statistics');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get preview statistics: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getExecutionHistory(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId/execution/history');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get execution history: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getExecutionResults(
    String taskId, {
    int executionNum = 1,
    int page = 1,
    int size = 20,
  }) async {
    final response = await _apiClient.get(
      '/api/tasks/$taskId/execution/results?executionNum=$executionNum&page=$page&size=$size',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get execution results: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getExecutionStatistics(
    String taskId, {
    int executionNum = 1,
  }) async {
    final response = await _apiClient.get(
      '/api/tasks/$taskId/execution/statistics?executionNum=$executionNum',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get execution statistics: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTaskLogs(
    String taskId, {
    int page = 1,
    int size = 20,
  }) async {
    final response = await _apiClient.get(
      '/api/tasks/$taskId/logs?page=$page&size=$size',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get task logs: ${response.statusCode}');
    }
  }

  Future<String> exportScanResults(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId/scan/export');

    if (response.statusCode == 200) {
      return response.body;
    } else {
      throw Exception('Failed to export scan results: ${response.statusCode}');
    }
  }

  Future<String> exportPreviewResults(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId/preview/export');

    if (response.statusCode == 200) {
      return response.body;
    } else {
      throw Exception('Failed to export preview results: ${response.statusCode}');
    }
  }

  Future<String> exportExecutionResults(String taskId, {int executionNum = 1}) async {
    final response = await _apiClient.get(
      '/api/tasks/$taskId/execution/export?executionNum=$executionNum',
    );

    if (response.statusCode == 200) {
      return response.body;
    } else {
      throw Exception('Failed to export execution results: ${response.statusCode}');
    }
  }

  Future<String> exportTaskLogs(String taskId) async {
    final response = await _apiClient.get('/api/tasks/$taskId/logs/export');

    if (response.statusCode == 200) {
      return response.body;
    } else {
      throw Exception('Failed to export task logs: ${response.statusCode}');
    }
  }

  WebSocketChannel connectTaskWebSocket(String taskId) {
    return _apiClient.connectWebSocket('/ws/tasks/$taskId');
  }
}
