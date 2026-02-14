import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/source_directory.dart';

class SourceDirectoryService {
  final ApiClient _apiClient;

  SourceDirectoryService(this._apiClient);

  Future<List<SourceDirectory>> getSourceDirectories() async {
    try {
      final response = await _apiClient.get('/api/source-directories');
      print('Get source directories response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        final List<dynamic> jsonList = json.decode(response.body);
        return jsonList.map((json) => SourceDirectory.fromJson(json as Map<String, dynamic>)).toList();
      } else {
        throw Exception('Failed to get source directories: ${response.statusCode}');
      }
    } catch (e) {
      print('Error getting source directories: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> addSourceDirectory(SourceDirectory directory) async {
    try {
      final response = await _apiClient.post('/api/source-directories', body: directory.toJson());
      print('Add source directory response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to add source directory: ${response.statusCode}');
      }
    } catch (e) {
      print('Error adding source directory: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> removeSourceDirectory(String path) async {
    try {
      final response = await _apiClient.delete('/api/source-directories/$path');
      print('Remove source directory response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to remove source directory: ${response.statusCode}');
      }
    } catch (e) {
      print('Error removing source directory: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> clearSourceDirectories() async {
    try {
      final response = await _apiClient.delete('/api/source-directories');
      print('Clear source directories response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to clear source directories: ${response.statusCode}');
      }
    } catch (e) {
      print('Error clearing source directories: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> updateThreadCount(String path, int threadCount) async {
    try {
      final response = await _apiClient.put('/api/source-directories/$path/threads', body: {
        'threadCount': threadCount,
      });
      print('Update thread count response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to update thread count: ${response.statusCode}');
      }
    } catch (e) {
      print('Error updating thread count: $e');
      rethrow;
    }
  }
}
