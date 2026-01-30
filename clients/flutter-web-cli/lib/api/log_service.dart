import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class LogService {
  final ApiClient _apiClient;

  LogService(this._apiClient);

  Future<List<Map<String, dynamic>>> getLogs() async {
    try {
      final response = await _apiClient.get('/logs');
      if (response.statusCode == 200) {
        final List<dynamic> jsonList = json.decode(response.body);
        return jsonList.map((json) => json as Map<String, dynamic>).toList();
      } else {
        throw Exception('Failed to get logs: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> clearLogs() async {
    try {
      final response = await _apiClient.delete('/logs');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to clear logs: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }
}
