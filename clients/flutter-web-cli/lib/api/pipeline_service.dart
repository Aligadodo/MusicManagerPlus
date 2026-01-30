import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/change_record.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';

class PipelineService {
  final ApiClient _apiClient;

  PipelineService(this._apiClient);

  Future<List<StrategyInfo>> getPipeline() async {
    try {
      final response = await _apiClient.get('/pipeline');
      if (response.statusCode == 200) {
        final List<dynamic> jsonList = json.decode(response.body);
        return jsonList.map((json) => StrategyInfo.fromJson(json as Map<String, dynamic>)).toList();
      } else {
        throw Exception('Failed to get pipeline: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> updatePipeline(List<StrategyInfo> pipeline) async {
    try {
      final body = pipeline.map((s) => s.toJson()).toList();
      final response = await _apiClient.post('/pipeline', body: body);
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to update pipeline: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<List<ChangeRecord>> analyzePipeline(List<String> sourceDirectories, List<StrategyInfo> pipeline) async {
    try {
      final response = await _apiClient.post('/pipeline/analyze', body: {
        'sourceDirectories': sourceDirectories,
        'pipeline': pipeline.map((s) => s.toJson()).toList(),
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

  Future<Map<String, dynamic>> executePipeline(List<String> sourceDirectories, List<StrategyInfo> pipeline) async {
    try {
      final response = await _apiClient.post('/pipeline/execute', body: {
        'sourceDirectories': sourceDirectories,
        'pipeline': pipeline.map((s) => s.toJson()).toList(),
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
