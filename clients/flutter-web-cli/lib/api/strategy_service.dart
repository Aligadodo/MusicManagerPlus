import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/change_record.dart';

class StrategyService {
  final ApiClient _apiClient;

  StrategyService(this._apiClient);

  Future<List<StrategyInfo>> getAvailableStrategies() async {
    final response = await _apiClient.get('/strategies');

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((item) => StrategyInfo.fromJson(item)).toList();
    } else {
      throw Exception('Failed to get strategies: ${response.statusCode}');
    }
  }

  Future<StrategyInfo> getStrategyInfo(String strategyId) async {
    final response = await _apiClient.get('/strategies/$strategyId');

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return StrategyInfo.fromJson(data);
    } else {
      throw Exception('Failed to get strategy info: ${response.statusCode}');
    }
  }

  Future<StrategyConfig> getStrategyConfig(String strategyId) async {
    final response = await _apiClient.get('/strategies/$strategyId/config');

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return StrategyConfig.fromJson(data);
    } else {
      throw Exception('Failed to get strategy config: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> updateStrategyConfig(String strategyId, StrategyConfig config) async {
    final response = await _apiClient.post(
      '/strategies/$strategyId/config',
      body: config.toJson(),
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to update strategy config: ${response.statusCode}');
    }
  }

  Future<List<ChangeRecord>> analyzeFiles(String strategyId, List<String> filePaths, StrategyConfig config) async {
    final response = await _apiClient.post(
      '/strategies/$strategyId/analyze',
      body: {
        'files': filePaths,
        'config': config.toJson(),
      },
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((item) => ChangeRecord.fromJson(item)).toList();
    } else {
      throw Exception('Failed to analyze files: ${response.statusCode}');
    }
  }

  Future<List<ChangeRecord>> executeStrategy(String strategyId, List<String> filePaths, StrategyConfig config) async {
    final response = await _apiClient.post(
      '/strategies/$strategyId/execute',
      body: {
        'files': filePaths,
        'config': config.toJson(),
      },
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((item) => ChangeRecord.fromJson(item)).toList();
    } else {
      throw Exception('Failed to execute strategy: ${response.statusCode}');
    }
  }
}
