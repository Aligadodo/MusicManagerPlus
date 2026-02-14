import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';

class DatabaseConfigService {
  final ApiClient _apiClient;

  DatabaseConfigService(this._apiClient);

  Future<Map<String, dynamic>> getSnapshots({
    int page = 1,
    int size = 20,
    String? type,
  }) async {
    String queryParams = 'page=$page&size=$size';
    if (type != null && type.isNotEmpty) {
      queryParams += '&type=$type';
    }

    final response = await _apiClient.get(
      '/api/database/config/snapshots?$queryParams',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get snapshots: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getSnapshot(String snapshotId) async {
    final response = await _apiClient.get(
      '/api/database/config/snapshots/$snapshotId',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get snapshot: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> createSnapshot(
    Map<String, dynamic> snapshotData,
  ) async {
    final response = await _apiClient.post(
      '/api/database/config/snapshots',
      body: snapshotData,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to create snapshot: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> updateSnapshot(
    String snapshotId,
    Map<String, dynamic> snapshotData,
  ) async {
    final response = await _apiClient.put(
      '/api/database/config/snapshots/$snapshotId',
      body: snapshotData,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to update snapshot: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> deleteSnapshot(String snapshotId) async {
    final response = await _apiClient.delete(
      '/api/database/config/snapshots/$snapshotId',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to delete snapshot: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTemplates({
    int page = 1,
    int size = 20,
    String? category,
  }) async {
    String queryParams = 'page=$page&size=$size';
    if (category != null && category.isNotEmpty) {
      queryParams += '&category=$category';
    }

    final response = await _apiClient.get(
      '/api/database/config/templates?$queryParams',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get templates: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getTemplate(String templateId) async {
    final response = await _apiClient.get(
      '/api/database/config/templates/$templateId',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get template: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> createTemplate(
    Map<String, dynamic> templateData,
  ) async {
    final response = await _apiClient.post(
      '/api/database/config/templates',
      body: templateData,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to create template: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> updateTemplate(
    String templateId,
    Map<String, dynamic> templateData,
  ) async {
    final response = await _apiClient.put(
      '/api/database/config/templates/$templateId',
      body: templateData,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to update template: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> deleteTemplate(String templateId) async {
    final response = await _apiClient.delete(
      '/api/database/config/templates/$templateId',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to delete template: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getSystemConfigs({
    int page = 1,
    int size = 20,
    String? category,
  }) async {
    String queryParams = 'page=$page&size=$size';
    if (category != null && category.isNotEmpty) {
      queryParams += '&category=$category';
    }

    final response = await _apiClient.get(
      '/api/database/config/system?$queryParams',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get system configs: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getSystemConfig(String configKey) async {
    final response = await _apiClient.get(
      '/api/database/config/system/$configKey',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get system config: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> createSystemConfig(
    Map<String, dynamic> configData,
  ) async {
    final response = await _apiClient.post(
      '/api/database/config/system',
      body: configData,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to create system config: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> updateSystemConfig(
    String configKey,
    Map<String, dynamic> configData,
  ) async {
    final response = await _apiClient.put(
      '/api/database/config/system/$configKey',
      body: configData,
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to update system config: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> deleteSystemConfig(String configKey) async {
    final response = await _apiClient.delete(
      '/api/database/config/system/$configKey',
    );

    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to delete system config: ${response.statusCode}');
    }
  }
}
