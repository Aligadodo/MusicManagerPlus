import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class ConfigService {
  final ApiClient _apiClient;

  ConfigService(this._apiClient);

  Future<dynamic> getConfig() async {
    try {
      final response = await _apiClient.get('/config');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      return {};
    } catch (e) {
      print('获取配置失败: $e');
      return {};
    }
  }

  Future<dynamic> saveConfig(Map<String, dynamic> config) async {
    try {
      final response = await _apiClient.post('/config', body: config);
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      throw Exception('保存配置失败');
    } catch (e) {
      print('保存配置失败: $e');
      rethrow;
    }
  }

  Future<List<Map<String, dynamic>>> getThemePresets() async {
    // 这里可以实现获取主题预设的逻辑
    // 暂时返回默认的主题预设
    return [
      {
        'name': '默认主题',
        'description': '系统默认主题',
        'config': {
          'theme': 'light',
          'bgColor': '#FFFFFF',
          'accentColor': '#2196F3',
          'textPrimaryColor': '#000000',
          'textSecondaryColor': '#666666',
          'textTertiaryColor': '#999999',
          'textDisabledColor': '#CCCCCC',
          'glassOpacity': 0.9,
          'darkBackground': false,
          'panelBgColor': '#FFFFFF',
          'fontFamily': 'Roboto',
          'fontSize': 14,
          'cornerRadius': 8.0,
          'borderWidth': 1.0,
          'borderColor': '#E0E0E0',
          'listBgColor': '#FFFFFF',
          'listRowEvenBgColor': '#FFFFFF',
          'listRowOddBgColor': '#F5F5F5',
          'listRowSelectedBgColor': '#2196F3',
          'listRowSelectedTextColor': '#FFFFFF',
          'listRowHoverBgColor': '#E3F2FD',
          'listBorderColor': '#E0E0E0',
          'listHeaderBgColor': '#F5F5F5',
          'listHeaderTextColor': '#000000',
          'buttonLargeSize': 48.0,
          'buttonSmallSize': 32.0,
        }
      },
      {
        'name': '深色主题',
        'description': '深色模式主题',
        'config': {
          'theme': 'dark',
          'bgColor': '#121212',
          'accentColor': '#2196F3',
          'textPrimaryColor': '#FFFFFF',
          'textSecondaryColor': '#B0B0B0',
          'textTertiaryColor': '#808080',
          'textDisabledColor': '#606060',
          'glassOpacity': 0.9,
          'darkBackground': true,
          'panelBgColor': '#1E1E1E',
          'fontFamily': 'Roboto',
          'fontSize': 14,
          'cornerRadius': 8.0,
          'borderWidth': 1.0,
          'borderColor': '#303030',
          'listBgColor': '#1E1E1E',
          'listRowEvenBgColor': '#1E1E1E',
          'listRowOddBgColor': '#252525',
          'listRowSelectedBgColor': '#2196F3',
          'listRowSelectedTextColor': '#FFFFFF',
          'listRowHoverBgColor': '#303030',
          'listBorderColor': '#303030',
          'listHeaderBgColor': '#252525',
          'listHeaderTextColor': '#FFFFFF',
          'buttonLargeSize': 48.0,
          'buttonSmallSize': 32.0,
        }
      }
    ];
  }

  Future<dynamic> saveThemePreset(Map<String, dynamic> preset) async {
    // 这里可以实现保存主题预设的逻辑
    // 暂时返回成功
    return {"success": true};
  }
}
