import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
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

  Future<Map<String, dynamic>> analyzePipeline(List<String> sourceDirectories, List<StrategyInfo> pipeline) async {
    try {
      final response = await _apiClient.post('/pipeline/analyze', body: {
        'sourceDirectories': sourceDirectories,
        'pipeline': pipeline.map((s) => s.toJson()).toList(),
      });
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
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

  Future<Map<String, dynamic>> getPipelineStatus() async {
    try {
      final response = await _apiClient.get('/pipeline/status');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to get pipeline status: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> getChanges({
    String? searchFilter,
    String? statusFilter,
    String? operationTypeFilter,
    bool hideUnchanged = true,
    int page = 1,
    int size = 20,
    String sortBy = 'id',
    String sortDirection = 'ASC',
  }) async {
    try {
      final queryParams = <String, dynamic>{
        if (searchFilter != null) 'searchFilter': searchFilter,
        if (statusFilter != null) 'statusFilter': statusFilter,
        if (operationTypeFilter != null) 'operationTypeFilter': operationTypeFilter,
        'hideUnchanged': hideUnchanged,
        'page': page,
        'size': size,
        'sortBy': sortBy,
        'sortDirection': sortDirection,
      };

      final queryString = Uri(queryParameters: queryParams).query;
      final response = await _apiClient.get('/pipeline/changes?$queryString');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to get changes: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }

  Future<Map<String, dynamic>> stopPipeline() async {
    try {
      final response = await _apiClient.post('/pipeline/stop');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to stop pipeline: ${response.statusCode}');
      }
    } catch (e) {
      rethrow;
    }
  }
}
