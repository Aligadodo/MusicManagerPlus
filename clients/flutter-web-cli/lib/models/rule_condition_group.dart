import 'package:filemanager_flutter/models/rule_condition.dart';

class RuleConditionGroup {
  final String id;
  final List<RuleCondition> conditions;

  RuleConditionGroup({
    String? id,
    List<RuleCondition>? conditions,
  })  : id = id ?? 'group_${DateTime.now().millisecondsSinceEpoch}',
        conditions = conditions ?? [];

  factory RuleConditionGroup.fromJson(Map<String, dynamic> json) {
    final conditionsList = (json['conditions'] as List<dynamic>?)
            ?.map((c) => RuleCondition.fromJson(c as Map<String, dynamic>))
            .toList() ??
        [];
    return RuleConditionGroup(
      id: json['id'] ?? 'group_${DateTime.now().millisecondsSinceEpoch}',
      conditions: conditionsList,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'conditions': conditions.map((c) => c.toJson()).toList(),
    };
  }

  void add(RuleCondition condition) {
    conditions.add(condition);
  }

  void remove(RuleCondition condition) {
    conditions.remove(condition);
  }

  void removeAt(int index) {
    conditions.removeAt(index);
  }

  void move(int oldIndex, int newIndex) {
    if (oldIndex < 0 || oldIndex >= conditions.length) return;
    if (newIndex < 0 || newIndex >= conditions.length) return;
    
    final condition = conditions.removeAt(oldIndex);
    conditions.insert(newIndex, condition);
  }
}
