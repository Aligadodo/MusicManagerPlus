import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class ConfigService {
  final ApiClient _apiClient;

  ConfigService(this._apiClient);

  Future<dynamic> getConfig() async {
    try {
      final response = await _apiClient.get('/api/config');
      print('Get config response: ${response.statusCode}, ${response.body}');
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
      final response = await _apiClient.post('/api/config', body: config);
      print('Save config response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      throw Exception('保存配置失败');
    } catch (e) {
      print('保存配置失败: $e');
      rethrow;
    }
  }

  // 主题管理接口
  Future<List<Map<String, dynamic>>> getThemes() async {
    try {
      final response = await _apiClient.get('/api/themes');
      print('Get themes response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data is List) {
          return List<Map<String, dynamic>>.from(data);
        }
      }
      return [];
    } catch (e) {
      print('获取主题列表失败: $e');
      return [];
    }
  }

  Future<Map<String, dynamic>> getTheme(String id) async {
    try {
      final response = await _apiClient.get('/api/themes/$id');
      print('Get theme response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      return {};
    } catch (e) {
      print('获取主题详情失败: $e');
      return {};
    }
  }

  Future<Map<String, dynamic>> createTheme(Map<String, dynamic> theme) async {
    try {
      final response = await _apiClient.post('/api/themes', body: theme);
      print('Create theme response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 201) {
        return jsonDecode(response.body);
      }
      throw Exception('创建主题失败');
    } catch (e) {
      print('创建主题失败: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> updateTheme(String id, Map<String, dynamic> theme) async {
    try {
      final response = await _apiClient.put('/api/themes/$id', body: theme);
      print('Update theme response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      throw Exception('更新主题失败');
    } catch (e) {
      print('更新主题失败: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> deleteTheme(String id) async {
    try {
      final response = await _apiClient.delete('/api/themes/$id');
      print('Delete theme response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      throw Exception('删除主题失败');
    } catch (e) {
      print('删除主题失败: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> getDefaultTheme() async {
    try {
      final response = await _apiClient.get('/api/themes/default');
      print('Get default theme response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      return {};
    } catch (e) {
      print('获取默认主题失败: $e');
      return {};
    }
  }

  Future<Map<String, dynamic>> setDefaultTheme(String themeId) async {
    try {
      final response = await _apiClient.put('/api/themes/default', body: {'themeId': themeId});
      print('Set default theme response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      }
      throw Exception('设置默认主题失败');
    } catch (e) {
      print('设置默认主题失败: $e');
      rethrow;
    }
  }

  // 兼容旧接口
  Future<List<Map<String, dynamic>>> getThemePresets() async {
    try {
      // 优先使用新的主题接口
      final themes = await getThemes();
      if (themes.isNotEmpty) {
        return themes.map((theme) => {
          'name': theme['name'],
          'description': theme['description'],
          'config': theme['config']
        }).toList();
      }
      
      //  fallback 到旧接口
      final response = await _apiClient.get('/api/config/themePresets');
      print('Get theme presets response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data is List) {
          return List<Map<String, dynamic>>.from(data);
        }
      }
      // 返回默认的主题预设作为 fallback
      return _getDefaultThemePresets();
    } catch (e) {
      print('获取主题预设失败: $e');
      // 返回默认的主题预设作为 fallback
      return _getDefaultThemePresets();
    }
  }

  List<Map<String, dynamic>> _getDefaultThemePresets() {
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
}
