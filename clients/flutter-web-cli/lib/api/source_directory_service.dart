import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class SourceDirectoryService {
  final ApiClient _apiClient;

  SourceDirectoryService(this._apiClient);

  Future<List<Map<String, dynamic>>> getSourceDirectories() async {
    try {
      final response = await _apiClient.get('/source-directories');
      if (response.statusCode == 200) {
        return List<Map<String, dynamic>>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to get source directories: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> addSourceDirectory(String path, {int threadCount = 4}) async {
    try {
      final response = await _apiClient.post('/source-directories', body: {
        'path': path,
        'threadCount': threadCount,
      });
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to add source directory: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> removeSourceDirectory(String path) async {
    try {
      final response = await _apiClient.delete('/source-directories/$path');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to remove source directory: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> clearSourceDirectories() async {
    try {
      final response = await _apiClient.delete('/source-directories');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to clear source directories: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> updateThreadCount(String path, int threadCount) async {
    try {
      final response = await _apiClient.put('/source-directories/$path/threads', body: {
        'threadCount': threadCount,
      });
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to update thread count: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }
}
