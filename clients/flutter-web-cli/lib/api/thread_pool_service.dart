import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class ThreadPoolService {
  final ApiClient _apiClient;

  ThreadPoolService(this._apiClient);

  Future<Map<String, dynamic>> getThreadPoolConfig() async {
    try {
      final response = await _apiClient.get('/api/thread-pool');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to get thread pool config: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> setPreviewThreads(int threads) async {
    try {
      final response = await _apiClient.put('/api/thread-pool/preview', body: {
        'threads': threads,
      });
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to set preview threads: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> setExecutionThreads(int threads) async {
    try {
      final response = await _apiClient.put('/api/thread-pool/execution', body: {
        'threads': threads,
      });
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to set execution threads: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }
}
