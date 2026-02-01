import 'rule_condition.dart';

class RuleConditionGroup {
  final String id;
  final List<RuleCondition> conditions;

  RuleConditionGroup({
    String? id,
    List<RuleCondition>? conditions,
  })  : id = id ?? '',
        conditions = conditions ?? [];

  factory RuleConditionGroup.fromJson(Map<String, dynamic> json) {
    final conditionsList = json['conditions'] as List<dynamic>?;
    return RuleConditionGroup(
      id: json['id'] as String?,
      conditions: conditionsList != null
          ? conditionsList.map((x) => RuleCondition.fromJson(x as Map<String, dynamic>)).toList()
          : [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'conditions': conditions.map((x) => x.toJson()).toList(),
    };
  }

  @override
  String toString() {
    if (conditions.isEmpty) return '无限制 (总是通过)';
    return conditions.map((c) => c.toString()).join(' 且 ');
  }
}
