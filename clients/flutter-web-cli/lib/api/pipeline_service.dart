import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/change_record.dart';

class PipelineService {
  final ApiClient _apiClient;

  PipelineService(this._apiClient);

  Future<List<Map<String, dynamic>>> getPipeline() async {
    try {
      final response = await _apiClient.get('/pipeline');
      if (response.statusCode == 200) {
        return List<Map<String, dynamic>>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to get pipeline: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> updatePipeline(List<Map<String, dynamic>> pipeline) async {
    try {
      final response = await _apiClient.post('/pipeline', body: pipeline);
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to update pipeline: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<List<ChangeRecord>> analyzePipeline(List<String> sourceDirectories, List<Map<String, dynamic>> pipeline) async {
    try {
      final response = await _apiClient.post('/pipeline/analyze', body: {
        'sourceDirectories': sourceDirectories,
        'pipeline': pipeline,
      });
      if (response.statusCode == 200) {
        final List<dynamic> changesJson = json.decode(response.body);
        return changesJson.map((json) => ChangeRecord.fromJson(json)).toList();
      } else {
        throw Exception('Failed to analyze pipeline: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> executePipeline(List<String> sourceDirectories, List<Map<String, dynamic>> pipeline) async {
    try {
      final response = await _apiClient.post('/pipeline/execute', body: {
        'sourceDirectories': sourceDirectories,
        'pipeline': pipeline,
      });
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to execute pipeline: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }
}
