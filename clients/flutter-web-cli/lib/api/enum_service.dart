import 'api_client.dart';
import '../models/enum_option.dart';

class EnumService {
  final ApiClient _apiClient;

  EnumService(this._apiClient);

  Future<List<EnumOption>> getAudioFormats() async {
    final response = await _apiClient.get('/api/enums/audio-formats');
    return (response as List)
        .map((json) => EnumOption.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<List<EnumOption>> getOutputDirModes() async {
    final response = await _apiClient.get('/api/enums/output-dir-modes');
    return (response as List)
        .map((json) => EnumOption.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<List<EnumOption>> getSampleRates() async {
    final response = await _apiClient.get('/api/enums/sample-rates');
    return (response as List)
        .map((json) => EnumOption.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<List<EnumOption>> getChannels() async {
    final response = await _apiClient.get('/api/enums/channels');
    return (response as List)
        .map((json) => EnumOption.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<List<EnumOption>> getCrossDriveModes() async {
    final response = await _apiClient.get('/api/enums/cross-drive-modes');
    return (response as List)
        .map((json) => EnumOption.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<List<EnumOption>> getProcessScopes() async {
    final response = await _apiClient.get('/api/enums/process-scopes');
    return (response as List)
        .map((json) => EnumOption.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  Future<List<EnumOption>> getEnumOptions(String enumType) async {
    final response = await _apiClient.get('/api/enums/$enumType');
    return (response as List)
        .map((json) => EnumOption.fromJson(json as Map<String, dynamic>))
        .toList();
  }
}