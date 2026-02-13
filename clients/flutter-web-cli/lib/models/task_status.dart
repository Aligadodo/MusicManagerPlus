import 'package:filemanager_flutter/models/change_record.dart';

class TaskStatus {
  final String taskId;
  final TaskStatusEnum status;
  final double progress;
  final String message;
  final int startTime;
  final int? endTime;
  final List<ChangeRecord> changes;
  
  // 统计信息
  final int totalFiles;
  final int processedFiles;
  final int successCount;
  final int failedCount;
  final int skippedCount;
  final Map<String, int>? operationStats;

  TaskStatus({
    required this.taskId,
    required this.status,
    required this.progress,
    required this.message,
    required this.startTime,
    this.endTime,
    required this.changes,
    this.totalFiles = 0,
    this.processedFiles = 0,
    this.successCount = 0,
    this.failedCount = 0,
    this.skippedCount = 0,
    this.operationStats,
  });

  factory TaskStatus.fromJson(Map<String, dynamic> json) {
    final changes = (json['changes'] as List<dynamic>?)?.map((change) => ChangeRecord.fromJson(change as Map<String, dynamic>)).toList() ?? [];
    
    final operationStatsJson = json['operationStats'] as Map<String, dynamic>?;
    final operationStats = operationStatsJson != null 
        ? operationStatsJson.map((key, value) => MapEntry(key, value as int))
        : <String, int>{};
    
    return TaskStatus(
      taskId: json['taskId'] as String,
      status: TaskStatusEnum.values.firstWhere((e) => e.name == json['status']),
      progress: json['progress'] as double,
      message: json['message'] as String,
      startTime: json['startTime'] as int,
      endTime: json['endTime'] as int?,
      changes: changes,
      totalFiles: json['totalFiles'] as int? ?? 0,
      processedFiles: json['processedFiles'] as int? ?? 0,
      successCount: json['successCount'] as int? ?? 0,
      failedCount: json['failedCount'] as int? ?? 0,
      skippedCount: json['skippedCount'] as int? ?? 0,
      operationStats: operationStats,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'taskId': taskId,
      'status': status.name,
      'progress': progress,
      'message': message,
      'startTime': startTime,
      'endTime': endTime,
      'changes': changes.map((change) => change.toJson()).toList(),
      'totalFiles': totalFiles,
      'processedFiles': processedFiles,
      'successCount': successCount,
      'failedCount': failedCount,
      'skippedCount': skippedCount,
      'operationStats': operationStats,
    };
  }
}

enum TaskStatusEnum {
  PENDING,
  RUNNING,
  SUCCESS,
  FAILED,
  CANCELLED;

  bool get isFinalState {
    return this == SUCCESS || this == FAILED || this == CANCELLED;
  }
}
