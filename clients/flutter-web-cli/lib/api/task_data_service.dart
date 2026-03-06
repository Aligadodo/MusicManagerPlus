import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/task_record.dart';
import 'api_client.dart';

/// 任务数据查询服务
/// 提供扫描/预览/执行三个阶段的数据分页查询功能
class TaskDataService {
  static final TaskDataService _instance = TaskDataService._internal();
  factory TaskDataService() => _instance;
  TaskDataService._internal();

  final ApiClient _apiClient = ApiClient();

  /// 创建空响应
  PaginatedResponse<TaskRecord> _emptyResponse(PaginationParams params) {
    return PaginatedResponse<TaskRecord>(
      list: [],
      total: 0,
      page: params.page,
      pageSize: params.pageSize,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
    );
  }

  /// 查询扫描记录
  /// 
  /// [taskId] 任务ID
  /// [params] 分页查询参数
  /// 
  /// 返回分页响应，包含扫描记录列表
  Future<PaginatedResponse<TaskRecord>> queryScanRecords(
    String taskId,
    PaginationParams params,
  ) async {
    try {
      final queryParams = params.toQueryParams();
      final queryString = _buildQueryString(queryParams);
      
      final response = await _apiClient.get(
        '/api/task-data/$taskId/scan/records$queryString',
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true && data['data'] != null) {
          return PaginatedResponse<TaskRecord>.fromJson(
            data['data'],
            (json) => TaskRecord.fromJson(json),
          );
        }
      }

      return _emptyResponse(params);
    } catch (e) {
      print('[TaskDataService] 查询扫描记录失败: $e');
      return _emptyResponse(params);
    }
  }

  /// 查询预览记录
  /// 
  /// [taskId] 任务ID
  /// [params] 分页查询参数
  /// 
  /// 返回分页响应，包含预览记录列表
  Future<PaginatedResponse<TaskRecord>> queryPreviewRecords(
    String taskId,
    PaginationParams params,
  ) async {
    try {
      final queryParams = params.toQueryParams();
      final queryString = _buildQueryString(queryParams);
      
      final response = await _apiClient.get(
        '/api/task-data/$taskId/preview/records$queryString',
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true && data['data'] != null) {
          return PaginatedResponse<TaskRecord>.fromJson(
            data['data'],
            (json) => TaskRecord.fromJson(json),
          );
        }
      }

      return _emptyResponse(params);
    } catch (e) {
      print('[TaskDataService] 查询预览记录失败: $e');
      return _emptyResponse(params);
    }
  }

  /// 查询执行记录
  /// 
  /// [taskId] 任务ID
  /// [params] 分页查询参数
  /// 
  /// 返回分页响应，包含执行记录列表
  Future<PaginatedResponse<TaskRecord>> queryExecutionRecords(
    String taskId,
    PaginationParams params,
  ) async {
    try {
      final queryParams = params.toQueryParams();
      final queryString = _buildQueryString(queryParams);
      
      final response = await _apiClient.get(
        '/api/task-data/$taskId/execution/records$queryString',
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true && data['data'] != null) {
          return PaginatedResponse<TaskRecord>.fromJson(
            data['data'],
            (json) => TaskRecord.fromJson(json),
          );
        }
      }

      return _emptyResponse(params);
    } catch (e) {
      print('[TaskDataService] 查询执行记录失败: $e');
      return _emptyResponse(params);
    }
  }

  /// 获取单条扫描记录详情
  /// 
  /// [taskId] 任务ID
  /// [recordId] 记录ID
  Future<TaskRecord?> getScanRecordDetail(String taskId, String recordId) async {
    try {
      final response = await _apiClient.get(
        '/api/task-data/$taskId/scan/records/$recordId',
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true && data['data'] != null) {
          return TaskRecord.fromJson(data['data']);
        }
      }

      return null;
    } catch (e) {
      print('[TaskDataService] 获取扫描记录详情失败: $e');
      return null;
    }
  }

  /// 获取单条预览记录详情
  /// 
  /// [taskId] 任务ID
  /// [recordId] 记录ID
  Future<TaskRecord?> getPreviewRecordDetail(String taskId, String recordId) async {
    try {
      final response = await _apiClient.get(
        '/api/task-data/$taskId/preview/records/$recordId',
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true && data['data'] != null) {
          return TaskRecord.fromJson(data['data']);
        }
      }

      return null;
    } catch (e) {
      print('[TaskDataService] 获取预览记录详情失败: $e');
      return null;
    }
  }

  /// 获取单条执行记录详情
  /// 
  /// [taskId] 任务ID
  /// [recordId] 记录ID
  Future<TaskRecord?> getExecutionRecordDetail(String taskId, String recordId) async {
    try {
      final response = await _apiClient.get(
        '/api/task-data/$taskId/execution/records/$recordId',
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true && data['data'] != null) {
          return TaskRecord.fromJson(data['data']);
        }
      }

      return null;
    } catch (e) {
      print('[TaskDataService] 获取执行记录详情失败: $e');
      return null;
    }
  }

  /// 构建查询字符串
  String _buildQueryString(Map<String, dynamic> params) {
    if (params.isEmpty) return '';
    
    final pairs = <String>[];
    params.forEach((key, value) {
      if (value != null) {
        pairs.add('$key=${Uri.encodeComponent(value.toString())}');
      }
    });
    
    return pairs.isNotEmpty ? '?${pairs.join('&')}' : '';
  }
}
