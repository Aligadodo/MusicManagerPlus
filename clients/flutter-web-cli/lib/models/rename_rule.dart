import 'package:filemanager_flutter/models/rename_condition.dart';
import 'package:filemanager_flutter/models/rename_action.dart';

class RenameRule {
  final String name;
  final bool enabled;
  final List<RenameCondition> conditions;
  final List<RenameAction> actions;

  RenameRule({
    required this.name,
    this.enabled = true,
    List<RenameCondition>? conditions,
    List<RenameAction>? actions,
  })  : conditions = conditions ?? [],
        actions = actions ?? [];

  factory RenameRule.fromJson(Map<String, dynamic> json) {
    return RenameRule(
      name: json['name'] as String? ?? '未命名规则',
      enabled: json['enabled'] as bool? ?? true,
      conditions: (json['conditions'] as List<dynamic>?)
              ?.map((e) => RenameCondition.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      actions: (json['actions'] as List<dynamic>?)
              ?.map((e) => RenameAction.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'enabled': enabled,
      'conditions': conditions.map((e) => e.toJson()).toList(),
      'actions': actions.map((e) => e.toJson()).toList(),
    };
  }

  RenameRule copyWith({
    String? name,
    bool? enabled,
    List<RenameCondition>? conditions,
    List<RenameAction>? actions,
  }) {
    return RenameRule(
      name: name ?? this.name,
      enabled: enabled ?? this.enabled,
      conditions: conditions ?? this.conditions,
      actions: actions ?? this.actions,
    );
  }
}
