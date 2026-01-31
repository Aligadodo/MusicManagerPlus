import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class LogService {
  final ApiClient _apiClient;

  LogService(this._apiClient);

  Future<Map<String, dynamic>> getLogFiles() async {
    try {
      final response = await _apiClient.get('/api/logs/files');
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data is Map) {
          return data as Map<String, dynamic>;
        } else if (data is List) {
          return {'files': data};
        }
        return {'files': data};
      } else {
        throw Exception('获取日志文件列表失败: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('获取日志文件列表失败: $e');
    }
  }

  Future<Map<String, dynamic>> getLogEntries({
    required String fileName,
    String? keyword,
    int page = 1,
    int size = 100,
  }) async {
    try {
      final queryParams = <String, String>{
        'fileName': fileName,
        'page': page.toString(),
        'size': size.toString(),
      };

      if (keyword != null && keyword.isNotEmpty) {
        queryParams['keyword'] = keyword;
      }

      final response = await _apiClient.get('/api/logs/entries', queryParams: queryParams);
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return data as Map<String, dynamic>;
      } else {
        throw Exception('获取日志内容失败: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('获取日志内容失败: $e');
    }
  }

  Future<Map<String, dynamic>> downloadLogFile(String fileName) async {
    try {
      final response = await _apiClient.get('/api/logs/download/$fileName');
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return data as Map<String, dynamic>;
      } else {
        throw Exception('下载日志文件失败: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('下载日志文件失败: $e');
    }
  }

  Future<Map<String, dynamic>> clearOldLogs({int days = 7}) async {
    try {
      final response = await _apiClient.post('/api/logs/clear', body: {'days': days});
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        return data as Map<String, dynamic>;
      } else {
        throw Exception('清理旧日志失败: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('清理旧日志失败: $e');
    }
  }
}
