import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/file_info.dart';

class FileService {
  final ApiClient _apiClient;

  FileService(this._apiClient);

  Future<List<FileInfo>> scanDirectory(String path, {
    int minDepth = 0,
    int maxDepth = 3,
    String? pattern,
  }) async {
    final response = await _apiClient.get(
      '/files/scan?path=$path&minDepth=$minDepth&maxDepth=$maxDepth${pattern != null ? '&pattern=$pattern' : ''}',
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((item) => FileInfo.fromJson(item)).toList();
    } else {
      throw Exception('Failed to scan directory: ${response.statusCode}');
    }
  }

  Future<FileInfo> getFileInfo(String path) async {
    final response = await _apiClient.get(
      '/files/info?path=$path',
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return FileInfo.fromJson(data);
    } else {
      throw Exception('Failed to get file info: ${response.statusCode}');
    }
  }

  Future<Map<String, bool>> checkExists(List<String> paths) async {
    final response = await _apiClient.post(
      '/files/exists',
      body: {'paths': paths},
    );

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return data.map((key, value) => MapEntry(key, value as bool));
    } else {
      throw Exception('Failed to check exists: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> fileOperation(String operation, String source, String target) async {
    final response = await _apiClient.post(
      '/files/operation',
      body: {
        'operation': operation,
        'source': source,
        'target': target,
      },
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to perform file operation: ${response.statusCode}');
    }
  }
}
