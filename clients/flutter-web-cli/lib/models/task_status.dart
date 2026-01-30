import 'package:filemanager_flutter/models/change_record.dart';

class TaskStatus {
  final String taskId;
  final TaskStatusEnum status;
  final double progress;
  final String message;
  final int startTime;
  final int? endTime;
  final List<ChangeRecord> changes;

  TaskStatus({
    required this.taskId,
    required this.status,
    required this.progress,
    required this.message,
    required this.startTime,
    this.endTime,
    required this.changes,
  });

  factory TaskStatus.fromJson(Map<String, dynamic> json) {
    final changes = (json['changes'] as List<dynamic>?)?.map((change) => ChangeRecord.fromJson(change as Map<String, dynamic>)).toList() ?? [];
    return TaskStatus(
      taskId: json['taskId'] as String,
      status: TaskStatusEnum.values.firstWhere((e) => e.name == json['status']),
      progress: json['progress'] as double,
      message: json['message'] as String,
      startTime: json['startTime'] as int,
      endTime: json['endTime'] as int?,
      changes: changes,
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
