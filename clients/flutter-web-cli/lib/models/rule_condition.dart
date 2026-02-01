import 'package:filemanager_flutter/models/condition_type.dart';

class RuleCondition {
  final ConditionType type;
  final String value;

  RuleCondition({
    required this.type,
    required this.value,
  });

  factory RuleCondition.fromJson(Map<String, dynamic> json) {
    return RuleCondition(
      type: ConditionType.values.firstWhere(
        (e) => e.name == json['type'],
        orElse: () => ConditionType.contains,
      ),
      value: json['value'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'type': type.name,
      'value': value,
    };
  }

  @override
  String toString() {
    if (!type.needsValue()) return type.description;
    return '${type.description} [$value]';
  }
}
