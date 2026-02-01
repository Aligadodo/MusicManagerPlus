import 'condition_type.dart';

class RuleCondition {
  final ConditionType type;
  final String? value;

  RuleCondition({
    required this.type,
    this.value,
  });

  factory RuleCondition.fromJson(Map<String, dynamic> json) {
    final typeStr = json['type'] as String?;
    return RuleCondition(
      type: typeStr != null ? ConditionType.fromString(typeStr!) : ConditionType.contains,
      value: json['value'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'type': type.description,
      'value': value,
    };
  }

  @override
  String toString() {
    if (!type.needsValue()) return type.description;
    return '${type.description} [${value ?? ""}]';
  }
}
