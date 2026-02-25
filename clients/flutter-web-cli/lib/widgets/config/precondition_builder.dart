import 'package:flutter/material.dart';
import '../../models/rule_condition_group.dart';
import '../../models/rule_condition.dart';
import '../../models/condition_type.dart';

class PreconditionBuilder {
  static Widget buildPreconditionGroup(
    RuleConditionGroup group,
    int index,
    Function(RuleConditionGroup) onRemoveGroup,
    Function(RuleConditionGroup) onAddCondition
  ) {
    try {
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          border: Border.all(color: const Color(0xFFBDBDBD)),
          borderRadius: BorderRadius.circular(4),
          color: Colors.white.withOpacity(0.4),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  '条件组 $index (一组条件内为且)',
                  style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF616161)),
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close, color: Colors.red),
                  onPressed: () => onRemoveGroup(group),
                ),
              ],
            ),
            const SizedBox(height: 8),
            ...group.conditions.map((condition) => _buildConditionItem(group, condition)),
            const SizedBox(height: 8),
            _buildAddConditionForm(group, onAddCondition),
          ],
        ),
      );
    } catch (e) {
      return Card(
        color: Colors.red.shade50,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.red),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '条件组 $index 加载失败',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red),
                    ),
                    Text(
                      '错误: $e',
                      style: const TextStyle(fontSize: 12, color: Colors.red),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
  }

  static Widget _buildConditionItem(RuleConditionGroup group, RuleCondition condition) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          const Text('• ', style: TextStyle(fontSize: 16)),
          Expanded(
            child: Text(
              condition.toString(),
              style: const TextStyle(color: Color(0xFF424242)),
            ),
          ),
          const SizedBox(width: 5),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) {
              switch (value) {
                case 'move_up':
                  _moveConditionUp(group, condition);
                  break;
                case 'move_down':
                  _moveConditionDown(group, condition);
                  break;
                case 'delete':
                  _deleteCondition(group, condition);
                  break;
              }
            },
            itemBuilder: (context) => [
              const PopupMenuItem(
                value: 'move_up',
                child: Text('上移'),
              ),
              const PopupMenuItem(
                value: 'move_down',
                child: Text('下移'),
              ),
              const PopupMenuItem(
                value: 'delete',
                child: Text('删除'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  static Widget _buildAddConditionForm(
    RuleConditionGroup group,
    Function(RuleConditionGroup) onAddCondition
  ) {
    ConditionType selectedType = ConditionType.contains;
    String valueText = '';

    return StatefulBuilder(
      builder: (context, setState) {
        return Row(
          children: [
            DropdownButton<ConditionType>(
              value: selectedType,
              items: ConditionType.values.map((type) {
                return DropdownMenuItem<ConditionType>(
                  value: type,
                  child: Text(type.description),
                );
              }).toList(),
              onChanged: (type) {
                setState(() {
                  selectedType = type ?? ConditionType.contains;
                });
              },
            ),
            const SizedBox(width: 5),
            Expanded(
              child: TextField(
                enabled: selectedType.needsValue(),
                decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  hintText: '值',
                  contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                ),
                onChanged: (v) {
                  valueText = v;
                },
              ),
            ),
            const SizedBox(width: 5),
            ElevatedButton(
              onPressed: () {
                if (!selectedType.needsValue() || valueText.isNotEmpty) {
                  final newCondition = RuleCondition(
                    type: selectedType,
                    value: selectedType.needsValue() ? valueText : '',
                  );
                  group.conditions.add(newCondition);
                  onAddCondition(group);
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF2980B9),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              ),
              child: const Text('添加条件'),
            ),
          ],
        );
      },
    );
  }

  static void _moveConditionUp(RuleConditionGroup group, RuleCondition condition) {
    final index = group.conditions.indexOf(condition);
    if (index > 0) {
      group.conditions.removeAt(index);
      group.conditions.insert(index - 1, condition);
    }
  }

  static void _moveConditionDown(RuleConditionGroup group, RuleCondition condition) {
    final index = group.conditions.indexOf(condition);
    if (index < group.conditions.length - 1) {
      group.conditions.removeAt(index);
      group.conditions.insert(index + 1, condition);
    }
  }

  static void _deleteCondition(RuleConditionGroup group, RuleCondition condition) {
    group.conditions.remove(condition);
  }
}
