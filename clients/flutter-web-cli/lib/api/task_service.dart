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
      '/tasks',
      body: request.toJson(),
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return data['taskId'] as String;
    } else {
      throw Exception('Failed to create task: ${response.statusCode}');
    }
  }

  Future<TaskStatus> getTaskStatus(String taskId) async {
    final response = await _apiClient.get('/tasks/$taskId');

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return TaskStatus.fromJson(data);
    } else {
      throw Exception('Failed to get task status: ${response.statusCode}');
    }
  }

  Future<List<TaskStatus>> getTasks({String? status, int page = 1, int size = 20}) async {
    final response = await _apiClient.get(
      '/tasks?${status != null ? 'status=$status&' : ''}page=$page&size=$size',
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((item) => TaskStatus.fromJson(item)).toList();
    } else {
      throw Exception('Failed to get tasks: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> executeTask(String taskId) async {
    final response = await _apiClient.post('/tasks/$taskId/execute');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to execute task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> cancelTask(String taskId) async {
    final response = await _apiClient.post('/tasks/$taskId/cancel');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to cancel task: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> deleteTask(String taskId) async {
    final response = await _apiClient.delete('/tasks/$taskId');

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to delete task: ${response.statusCode}');
    }
  }

  WebSocketChannel connectTaskWebSocket(String taskId) {
    return _apiClient.connectWebSocket('/ws/tasks/$taskId');
  }
}
