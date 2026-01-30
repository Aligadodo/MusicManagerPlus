import 'package:filemanager_flutter/models/config_field.dart';

class StrategyInfo {
  final String id;
  final String name;
  final String description;
  final List<ConfigField> configFields;
  final bool enabled;

  StrategyInfo({
    required this.id,
    required this.name,
    required this.description,
    required this.configFields,
    required this.enabled,
  });

  factory StrategyInfo.fromJson(Map<String, dynamic> json) {
    final configFields = (json['configFields'] as List<dynamic>?)?.map((field) => ConfigField.fromJson(field as Map<String, dynamic>)).toList() ?? [];
    return StrategyInfo(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      configFields: configFields,
      enabled: json['enabled'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'configFields': configFields.map((field) => field.toJson()).toList(),
      'enabled': enabled,
    };
  }
}
