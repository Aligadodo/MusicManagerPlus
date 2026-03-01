import 'dart:convert';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
import 'package:filemanager_flutter/models/config_field.dart';

class PipelineService {
  final ApiClient _apiClient;

  PipelineService(this._apiClient);

  Future<List<StrategyInfo>> getPipeline() async {
    try {
      // 1. 先获取所有可用的策略列表，包含中文名和配置字段
      List<StrategyInfo> availableStrategies = [];
      try {
        print('正在获取所有可用策略...');
        final response = await _apiClient.get('/api/strategies');
        if (response.statusCode == 200) {
          final List<dynamic> data = json.decode(response.body);
          availableStrategies = data.map((item) => StrategyInfo.fromJson(item)).toList();
          print('获取到可用策略数量: ${availableStrategies.length}');
        }
      } catch (e) {
        print('获取可用策略失败: $e');
        availableStrategies = [];
      }
      
      // 2. 获取流水线配置
      final response = await _apiClient.get('/api/pipeline');
      print('Get pipeline response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        final List<dynamic> jsonList = json.decode(response.body);
        print('Pipeline items count: ${jsonList.length}');
        
        final List<StrategyInfo> result = [];
        for (int i = 0; i < jsonList.length; i++) {
          try {
            final json = jsonList[i];
            print('Processing pipeline item $i: $json');
            
            if (json == null) {
              print('Skipping null pipeline item $i');
              continue;
            }
            
            final map = json as Map<String, dynamic>;
            print('Pipeline item $i map: $map');
            
            // 确保pluginId不为null，同时支持旧的"id"字段和新的"pluginId"字段
            final pluginId = (map['pluginId'] as String?) ?? (map['id'] as String?) ?? 'unknown';
            print('Pipeline item $i pluginId: $pluginId');
            
            // 处理preconditionGroups
            List<PreconditionGroup> preconditionGroups = [];
            try {
              if (map.containsKey('preconditionGroups')) {
                final preconditionGroupsData = map['preconditionGroups'];
                print('Pipeline item $i preconditionGroupsData: $preconditionGroupsData');
                
                if (preconditionGroupsData is List) {
                  preconditionGroups = preconditionGroupsData.map((group) {
                    try {
                      if (group == null) {
                        print('Skipping null precondition group in item $i');
                        return null;
                      }
                      return PreconditionGroup.fromJson(group as Map<String, dynamic>);
                    } catch (e) {
                      print('Failed to parse precondition group in item $i: $group, error: $e');
                      return null;
                    }
                  }).where((group) => group != null).cast<PreconditionGroup>().toList();
                }
              }
            } catch (e) {
              print('Error processing preconditionGroups in item $i: $e');
              preconditionGroups = [];
            }
            
            // 处理enabled字段
            bool enabled = true;
            try {
              enabled = map['enabled'] as bool? ?? true;
            } catch (e) {
              print('Error processing enabled field in item $i: $e');
              enabled = true;
            }
            
            // 处理pipelineId字段
            String? pipelineId;
            try {
              pipelineId = map['pipelineId'] as String?;
            } catch (e) {
              print('Error processing pipelineId field in item $i: $e');
              pipelineId = null;
            }
            
            // 3. 查找对应的可用策略，获取中文名和配置字段
            StrategyInfo matchedStrategy = availableStrategies.firstWhere(
              (strategy) => strategy.id == pluginId,
              orElse: () => StrategyInfo(
                id: pluginId,
                name: pluginId, // 默认使用pluginId
                description: '',
                configFields: [],
                enabled: true,
                preconditionGroups: [],
              ),
            );
            
            // 4. 处理配置字段
            List<ConfigField> configFields = matchedStrategy.configFields;
            
            // 5. 创建StrategyInfo对象
            final strategyInfo = StrategyInfo(
              id: pluginId,
              name: matchedStrategy.name, // 使用匹配到的中文名
              description: matchedStrategy.description,
              configFields: configFields,
              preconditionGroups: preconditionGroups,
              enabled: enabled,
              pipelineId: pipelineId,
            );
            
            result.add(strategyInfo);
            print('Added pipeline item $i: ${matchedStrategy.name} (ID: $pluginId)');
          } catch (e) {
            print('Error processing pipeline item $i: $e');
            // 跳过有错误的项目，继续处理其他项目
            continue;
          }
        }
        
        print('Processed ${result.length} pipeline items');
        return result;
      } else {
        throw Exception('Failed to get pipeline: ${response.statusCode}');
      }
    } catch (e) {
      print('Error getting pipeline: $e');
      // 返回空列表而不是抛出异常，确保应用能够正常启动
      return [];
    }
  }

  Future<Map<String, dynamic>> updatePipeline(List<StrategyInfo> pipeline) async {
    try {
      // 转换为后端期望的数据结构
      final body = pipeline.map((s) {
        final map = <String, dynamic>{
          'pluginId': s.id,
          'enabled': s.enabled,
          'config': s.configFields.fold<Map<String, dynamic>>({}, (acc, field) {
            acc[field.name] = field.defaultValue;
            return acc;
          }),
          'preconditionGroups': s.preconditionGroups.map((group) => group.toJson()).toList(),
        };
        if (s.pipelineId != null) {
          map['pipelineId'] = s.pipelineId;
        }
        return map;
      }).toList();
      
      final response = await _apiClient.post('/api/pipeline', body: body);
      print('Update pipeline response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to update pipeline: ${response.statusCode}');
      }
    } catch (e) {
      print('Error updating pipeline: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> resetPipeline() async {
    try {
      print('发送重置流水线请求...');
      final response = await _apiClient.post('/api/pipeline/reset');
      print('重置流水线请求响应: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to reset pipeline: ${response.statusCode}');
      }
    } catch (e) {
      print('重置流水线请求失败: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> analyzePipeline(List<String> sourceDirectories, List<StrategyInfo> pipeline, {bool autoExecute = false}) async {
    try {
      // 转换为后端期望的数据结构
      final pipelineData = pipeline.map((s) {
        final map = <String, dynamic>{
          'pluginId': s.id,
          'enabled': s.enabled,
          'config': s.configFields.fold<Map<String, dynamic>>({}, (acc, field) {
            acc[field.name] = field.defaultValue;
            return acc;
          }),
          'preconditionGroups': s.preconditionGroups.map((group) => group.toJson()).toList(),
        };
        if (s.pipelineId != null) {
          map['pipelineId'] = s.pipelineId;
        }
        return map;
      }).toList();
      
      final response = await _apiClient.post('/api/pipeline/analyze', body: {
        'sourceDirectories': sourceDirectories,
        'pipeline': pipelineData,
        'autoExecute': autoExecute,
      });
      print('Analyze pipeline response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to analyze pipeline: ${response.statusCode}');
      }
    } catch (e) {
      print('Error analyzing pipeline: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> executePipeline(List<String> sourceDirectories, List<StrategyInfo> pipeline) async {
    try {
      // 转换为后端期望的数据结构
      final pipelineData = pipeline.map((s) {
        final map = <String, dynamic>{
          'pluginId': s.id,
          'enabled': s.enabled,
          'config': s.configFields.fold<Map<String, dynamic>>({}, (acc, field) {
            acc[field.name] = field.defaultValue;
            return acc;
          }),
          'preconditionGroups': s.preconditionGroups.map((group) => group.toJson()).toList(),
        };
        if (s.pipelineId != null) {
          map['pipelineId'] = s.pipelineId;
        }
        return map;
      }).toList();
      
      final response = await _apiClient.post('/api/pipeline/execute', body: {
        'sourceDirectories': sourceDirectories,
        'pipeline': pipelineData,
      });
      print('Execute pipeline response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to execute pipeline: ${response.statusCode}');
      }
    } catch (e) {
      print('Error executing pipeline: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> getPipelineStatus() async {
    try {
      final response = await _apiClient.get('/api/pipeline/status');
      print('Get pipeline status response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to get pipeline status: ${response.statusCode}');
      }
    } catch (e) {
      print('Error getting pipeline status: $e');
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
      final response = await _apiClient.get('/api/pipeline/changes?$queryString');
      print('Get changes response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to get changes: ${response.statusCode}');
      }
    } catch (e) {
      print('Error getting changes: $e');
      rethrow;
    }
  }

  Future<Map<String, dynamic>> stopPipeline() async {
    try {
      final response = await _apiClient.post('/api/pipeline/stop');
      print('Stop pipeline response: ${response.statusCode}, ${response.body}');
      if (response.statusCode == 200) {
        return Map<String, dynamic>.from(json.decode(response.body));
      } else {
        throw Exception('Failed to stop pipeline: ${response.statusCode}');
      }
    } catch (e) {
      print('Error stopping pipeline: $e');
      rethrow;
    }
  }
}
