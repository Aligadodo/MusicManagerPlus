import 'package:filemanager_flutter/models/change_record.dart';

class TaskStatus {
  final String? taskId;
  final String? taskName;
  final int? createdAt;
  final String? currentStage;
  final String? status;
  final double? overallProgress;
  final String? message;
  final TaskConfigSnapshot? configSnapshot;
  final TaskStages? stages;

  TaskStatus({
    this.taskId,
    this.taskName,
    this.createdAt,
    this.currentStage,
    this.status,
    this.overallProgress,
    this.message,
    this.configSnapshot,
    this.stages,
  });

  factory TaskStatus.fromJson(Map<String, dynamic> json) {
    return TaskStatus(
      taskId: json['taskId'] as String?,
      taskName: json['taskName'] as String?,
      createdAt: json['createdAt'] as int?,
      currentStage: json['currentStage'] as String?,
      status: json['status'] as String?,
      overallProgress: (json['overallProgress'] as double?) ?? (json['progress'] as double?),
      message: json['message'] as String?,
      configSnapshot: json['configSnapshot'] != null
          ? TaskConfigSnapshot.fromJson(json['configSnapshot'] as Map<String, dynamic>)
          : null,
      stages: json['stages'] != null
          ? TaskStages.fromJson(json['stages'] as Map<String, dynamic>)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'taskId': taskId,
      'taskName': taskName,
      'createdAt': createdAt,
      'currentStage': currentStage,
      'status': status,
      'overallProgress': overallProgress,
      'message': message,
      'configSnapshot': configSnapshot?.toJson(),
      'stages': stages?.toJson(),
    };
  }
}

class TaskConfigSnapshot {
  final List<SourceDirectoryConfig>? sourceDirectories;
  final String? strategyId;
  final Map<String, dynamic>? strategyConfig;
  final List<RenameRule>? renameRules;
  final List<Precondition>? preconditions;

  TaskConfigSnapshot({
    this.sourceDirectories,
    this.strategyId,
    this.strategyConfig,
    this.renameRules,
    this.preconditions,
  });

  factory TaskConfigSnapshot.fromJson(Map<String, dynamic> json) {
    return TaskConfigSnapshot(
      sourceDirectories: (json['sourceDirectories'] as List<dynamic>?)
          ?.map((e) => SourceDirectoryConfig.fromJson(e as Map<String, dynamic>))
          .toList(),
      strategyId: json['strategyId'] as String?,
      strategyConfig: json['strategyConfig'] as Map<String, dynamic>?,
      renameRules: (json['renameRules'] as List<dynamic>?)
          ?.map((e) => RenameRule.fromJson(e as Map<String, dynamic>))
          .toList(),
      preconditions: (json['preconditions'] as List<dynamic>?)
          ?.map((e) => Precondition.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'sourceDirectories': sourceDirectories?.map((e) => e.toJson()).toList(),
      'strategyId': strategyId,
      'strategyConfig': strategyConfig,
      'renameRules': renameRules?.map((e) => e.toJson()).toList(),
      'preconditions': preconditions?.map((e) => e.toJson()).toList(),
    };
  }
}

class SourceDirectoryConfig {
  final String? path;
  final int? depth;

  SourceDirectoryConfig({
    this.path,
    this.depth,
  });

  factory SourceDirectoryConfig.fromJson(Map<String, dynamic> json) {
    return SourceDirectoryConfig(
      path: json['path'] as String?,
      depth: json['depth'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'path': path,
      'depth': depth,
    };
  }
}

class RenameRule {
  final String? ruleId;
  final String? ruleName;
  final String? ruleType;
  final Map<String, dynamic>? ruleConfig;

  RenameRule({
    this.ruleId,
    this.ruleName,
    this.ruleType,
    this.ruleConfig,
  });

  factory RenameRule.fromJson(Map<String, dynamic> json) {
    return RenameRule(
      ruleId: json['ruleId'] as String?,
      ruleName: json['ruleName'] as String?,
      ruleType: json['ruleType'] as String?,
      ruleConfig: json['ruleConfig'] as Map<String, dynamic>?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'ruleId': ruleId,
      'ruleName': ruleName,
      'ruleType': ruleType,
      'ruleConfig': ruleConfig,
    };
  }
}

class Precondition {
  final String? conditionId;
  final String? conditionType;
  final Map<String, dynamic>? conditionConfig;

  Precondition({
    this.conditionId,
    this.conditionType,
    this.conditionConfig,
  });

  factory Precondition.fromJson(Map<String, dynamic> json) {
    return Precondition(
      conditionId: json['conditionId'] as String?,
      conditionType: json['conditionType'] as String?,
      conditionConfig: json['conditionConfig'] as Map<String, dynamic>?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'conditionId': conditionId,
      'conditionType': conditionType,
      'conditionConfig': conditionConfig,
    };
  }
}

class TaskStages {
  final ScanStage? scan;
  final PreviewStage? preview;
  final ExecutionStage? execution;

  TaskStages({
    this.scan,
    this.preview,
    this.execution,
  });

  factory TaskStages.fromJson(Map<String, dynamic> json) {
    return TaskStages(
      scan: json['scan'] != null
          ? ScanStage.fromJson(json['scan'] as Map<String, dynamic>)
          : null,
      preview: json['preview'] != null
          ? PreviewStage.fromJson(json['preview'] as Map<String, dynamic>)
          : null,
      execution: json['execution'] != null
          ? ExecutionStage.fromJson(json['execution'] as Map<String, dynamic>)
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'scan': scan?.toJson(),
      'preview': preview?.toJson(),
      'execution': execution?.toJson(),
    };
  }
}

class ScanStage {
  final String? status;
  final int? scanStartTime;
  final int? scanEndTime;
  final int? scannedFiles;
  final int? totalFiles;

  ScanStage({
    this.status,
    this.scanStartTime,
    this.scanEndTime,
    this.scannedFiles,
    this.totalFiles,
  });

  factory ScanStage.fromJson(Map<String, dynamic> json) {
    return ScanStage(
      status: json['status'] as String?,
      scanStartTime: json['scanStartTime'] as int?,
      scanEndTime: json['scanEndTime'] as int?,
      scannedFiles: json['scannedFiles'] as int?,
      totalFiles: json['totalFiles'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'status': status,
      'scanStartTime': scanStartTime,
      'scanEndTime': scanEndTime,
      'scannedFiles': scannedFiles,
      'totalFiles': totalFiles,
    };
  }
}

class PreviewStage {
  final String? status;
  final int? previewStartTime;
  final int? previewEndTime;
  final int? analyzedFiles;
  final int? totalChanges;

  PreviewStage({
    this.status,
    this.previewStartTime,
    this.previewEndTime,
    this.analyzedFiles,
    this.totalChanges,
  });

  factory PreviewStage.fromJson(Map<String, dynamic> json) {
    return PreviewStage(
      status: json['status'] as String?,
      previewStartTime: json['previewStartTime'] as int?,
      previewEndTime: json['previewEndTime'] as int?,
      analyzedFiles: json['analyzedFiles'] as int?,
      totalChanges: json['totalChanges'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'status': status,
      'previewStartTime': previewStartTime,
      'previewEndTime': previewEndTime,
      'analyzedFiles': analyzedFiles,
      'totalChanges': totalChanges,
    };
  }
}

class ExecutionStage {
  final String? status;
  final int? executionStartTime;
  final int? executionEndTime;
  final int? executedFiles;
  final int? successCount;
  final int? failedCount;
  final int? executionCount;

  ExecutionStage({
    this.status,
    this.executionStartTime,
    this.executionEndTime,
    this.executedFiles,
    this.successCount,
    this.failedCount,
    this.executionCount,
  });

  factory ExecutionStage.fromJson(Map<String, dynamic> json) {
    return ExecutionStage(
      status: json['status'] as String?,
      executionStartTime: json['executionStartTime'] as int?,
      executionEndTime: json['executionEndTime'] as int?,
      executedFiles: json['executedFiles'] as int?,
      successCount: json['successCount'] as int?,
      failedCount: json['failedCount'] as int?,
      executionCount: json['executionCount'] as int?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'status': status,
      'executionStartTime': executionStartTime,
      'executionEndTime': executionEndTime,
      'executedFiles': executedFiles,
      'successCount': successCount,
      'failedCount': failedCount,
      'executionCount': executionCount,
    };
  }
}
