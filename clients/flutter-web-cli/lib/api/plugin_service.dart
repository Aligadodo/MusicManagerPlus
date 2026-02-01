import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import '../models/plugin_info.dart';
import '../models/plugin_config.dart';
import '../models/change_record.dart';

class PluginService {
  final ApiClient _apiClient;

  PluginService(this._apiClient);

  Future<List<PluginInfo>> getPlugins() async {
    try {
      final response = await _apiClient.get('/plugins');
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((item) => PluginInfo.fromJson(item)).toList();
      } else {
        throw Exception('Failed to get plugins: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting plugins: $e');
    }
  }

  Future<PluginConfig> getPluginConfig(String pluginId) async {
    try {
      final response = await _apiClient.get('/plugins/$pluginId/config');
      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        return PluginConfig.fromJson(data);
      } else {
        throw Exception('Failed to get plugin config: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting plugin config: $e');
    }
  }

  Future<PluginConfig> savePluginConfig(String pluginId, PluginConfig config) async {
    try {
      final response = await _apiClient.post(
        '/plugins/$pluginId/config',
        body: json.encode(config.toJson()),
      );
      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        return PluginConfig.fromJson(data);
      } else {
        throw Exception('Failed to save plugin config: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error saving plugin config: $e');
    }
  }

  Future<List<ChangeRecord>> previewPlugin(String pluginId, List<String> filePaths, PluginConfig config) async {
    try {
      final response = await _apiClient.post(
        '/plugins/$pluginId/preview',
        body: json.encode({
          'filePaths': filePaths,
          'config': config.toJson(),
        }),
      );
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((item) => ChangeRecord.fromJson(item)).toList();
      } else {
        throw Exception('Failed to preview plugin: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error previewing plugin: $e');
    }
  }

  Future<List<ChangeRecord>> executePlugin(String pluginId, List<String> filePaths, PluginConfig config) async {
    try {
      final response = await _apiClient.post(
        '/plugins/$pluginId/execute',
        body: json.encode({
          'filePaths': filePaths,
          'config': config.toJson(),
        }),
      );
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((item) => ChangeRecord.fromJson(item)).toList();
      } else {
        throw Exception('Failed to execute plugin: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error executing plugin: $e');
    }
  }
}
