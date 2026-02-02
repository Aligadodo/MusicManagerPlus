import 'package:filemanager_flutter/models/config_field.dart';
import 'dart:core';

class StrategyInfo {
  final String id;
  final String name;
  final String description;
  final List<ConfigField> configFields;
  final bool enabled;
  final String? pipelineId; // 流水线唯一标识符

  StrategyInfo({
    required this.id,
    required this.name,
    required this.description,
    required this.configFields,
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
    return StrategyInfo(
      id: json['id'] as String? ?? 'unknown',
      name: json['name'] as String? ?? 'Unknown Strategy',
      description: json['description'] as String? ?? '',
      configFields: configFields,
      enabled: json['enabled'] as bool? ?? true,
      pipelineId: json['pipelineId'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'configFields': configFields.map((field) => field.toJson()).toList(),
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
      configFields: configFields.map((field) => field.copyWith()).toList(),
      enabled: enabled,
      pipelineId: 'pipeline_${DateTime.now().millisecondsSinceEpoch}_${id.hashCode}',
    );
  }
}
