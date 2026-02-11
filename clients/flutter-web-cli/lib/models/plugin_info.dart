import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';

class PluginInfo {
  final String id;
  final String name;
  final String description;
  final String version;
  final bool enabled;
  final List<ConfigField>? configFields;
  final List<PreconditionGroup>? preconditionGroups;

  PluginInfo({
    required this.id,
    required this.name,
    required this.description,
    required this.version,
    this.enabled = true,
    this.configFields,
    this.preconditionGroups,
  });

  factory PluginInfo.fromJson(Map<String, dynamic> json) {
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
    }).where((field) => field != null).cast<ConfigField>().toList();

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
    }).where((group) => group != null).cast<PreconditionGroup>().toList();

    return PluginInfo(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String? ?? '',
      version: json['version'] as String? ?? '',
      enabled: json['enabled'] as bool? ?? true,
      configFields: configFields,
      preconditionGroups: preconditionGroups,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'version': version,
      'enabled': enabled,
      if (configFields != null) 'configFields': configFields!.map((field) => field.toJson()).toList(),
      if (preconditionGroups != null) 'preconditionGroups': preconditionGroups!.map((group) => group.toJson()).toList(),
    };
  }
}
