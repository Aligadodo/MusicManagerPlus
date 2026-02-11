import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
import 'dart:core';

class StrategyInfo {
  final String id;
  final String name;
  final String description;
  final String? version;
  final List<ConfigField> configFields;
  final List<PreconditionGroup> preconditionGroups;
  final bool enabled;
  final String? pipelineId; // 流水线唯一标识符

  StrategyInfo({
    required this.id,
    required this.name,
    required this.description,
    this.version,
    required this.configFields,
    this.preconditionGroups = const [],
    required this.enabled,
    this.pipelineId,
  });

  factory StrategyInfo.fromJson(Map<String, dynamic> json) {
    final configFields = (json['configFields'] as List<dynamic>?)?.map((field) {
      try {
        if (field == null) {
          print('Skipping null config field');
          return null;
        }
        return ConfigField.fromJson(field as Map<String, dynamic>);
      } catch (e) {
        print('Failed to parse config field: $field, error: $e');
        return null;
      }
    }).where((field) => field != null).cast<ConfigField>().toList() ?? [];

    final preconditionGroups = (json['preconditionGroups'] as List<dynamic>?)?.map((group) {
      try {
        if (group == null) {
          print('Skipping null precondition group');
          return null;
        }
        return PreconditionGroup.fromJson(group as Map<String, dynamic>);
      } catch (e) {
        print('Failed to parse precondition group: $group, error: $e');
        return null;
      }
    }).where((group) => group != null).cast<PreconditionGroup>().toList() ?? [];

    return StrategyInfo(
      id: json['id'] as String? ?? 'unknown',
      name: json['name'] as String? ?? 'Unknown Strategy',
      description: json['description'] as String? ?? '',
      version: json['version'] as String?,
      configFields: configFields,
      preconditionGroups: preconditionGroups,
      enabled: json['enabled'] as bool? ?? true,
      pipelineId: json['pipelineId'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      if (version != null) 'version': version,
      'configFields': configFields.map((field) => field.toJson()).toList(),
      'preconditionGroups': preconditionGroups.map((group) => group.toJson()).toList(),
      'enabled': enabled,
      if (pipelineId != null) 'pipelineId': pipelineId,
    };
  }

  // 创建一个新的策略实例，带有唯一的流水线ID
  StrategyInfo copyWithPipelineId() {
    return StrategyInfo(
      id: id,
      name: name,
      description: description,
      version: version,
      configFields: configFields.map((field) => field.copyWith()).toList(),
      preconditionGroups: preconditionGroups.map((group) => group.copyWith()).cast<PreconditionGroup>().toList(),
      enabled: enabled,
      pipelineId: 'pipeline_${DateTime.now().millisecondsSinceEpoch}_${id.hashCode}',
    );
  }

  // 复制策略信息，更新前置条件组
  StrategyInfo copyWithPreconditionGroups(List<PreconditionGroup> groups) {
    // 创建前置条件组的深拷贝，确保每个策略都有独立的前置条件配置
    final copiedGroups = groups.map((group) => group.copyWith(
      preconditions: group.preconditions.map((condition) => condition.copyWith()).toList(),
    )).toList();
    
    return StrategyInfo(
      id: id,
      name: name,
      description: description,
      version: version,
      configFields: configFields.map((field) => field.copyWith()).toList(),
      preconditionGroups: copiedGroups,
      enabled: enabled,
      pipelineId: pipelineId,
    );
  }
}
