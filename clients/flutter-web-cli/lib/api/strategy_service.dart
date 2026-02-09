import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/change_record.dart';

class StrategyService {
  final ApiClient _apiClient;

  StrategyService(this._apiClient);

  Future<List<StrategyInfo>> getAvailableStrategies() async {
    try {
      print('正在请求策略列表...');
      print('请求URL: http://localhost:8080/api/strategies');
      final response = await _apiClient.get('/api/strategies');
      print('响应状态码: ${response.statusCode}');
      
      if (response.statusCode == 200) {
        print('响应体长度: ${response.body.length}');
        final List<dynamic> data = jsonDecode(response.body);
        print('策略数量: ${data.length}');
        return data.map((item) => StrategyInfo.fromJson(item)).toList();
      } else {
        print('获取策略失败: ${response.statusCode}');
        print('响应体: ${response.body}');
        throw Exception('Failed to get strategies: ${response.statusCode}');
      }
    } catch (e) {
      print('获取策略列表异常: $e');
      rethrow;
    }
  }

  Future<StrategyInfo> getStrategyInfo(String strategyId) async {
    final response = await _apiClient.get('/api/strategies/$strategyId');

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return StrategyInfo.fromJson(data);
    } else {
      throw Exception('Failed to get strategy info: ${response.statusCode}');
    }
  }

  Future<StrategyConfig> getStrategyConfig(String strategyId) async {
    final response = await _apiClient.get('/api/strategies/$strategyId/config');

    if (response.statusCode == 200) {
      final Map<String, dynamic> data = jsonDecode(response.body);
      return StrategyConfig.fromJson(data);
    } else {
      throw Exception('Failed to get strategy config: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> updateStrategyConfig(String strategyId, StrategyConfig config) async {
    final response = await _apiClient.post(
      '/api/strategies/$strategyId/config',
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
      '/api/strategies/$strategyId/analyze',
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
      '/api/strategies/$strategyId/execute',
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
