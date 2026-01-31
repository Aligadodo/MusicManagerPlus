import 'package:filemanager_flutter/api/api_client.dart';

class ConfigService {
  final ApiClient _apiClient;

  ConfigService(this._apiClient);

  Future<dynamic> getConfig() async {
    // 这里可以实现获取配置的逻辑
    // 暂时返回空的配置
    return {};
  }

  Future<dynamic> saveConfig(Map<String, dynamic> config) async {
    // 这里可以实现保存配置的逻辑
    // 暂时返回成功
    return {"success": true};
  }
}
