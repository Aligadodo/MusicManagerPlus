import 'package:flutter/material.dart';
import '../../models/rename_rule.dart';
import '../../models/rename_condition.dart';
import '../../models/rename_action.dart';
import 'condition_builder.dart';
import 'action_builder.dart';

class RenameRuleCard extends StatefulWidget {
  final int index;
  final RenameRule rule;
  final Function(RenameRule) onUpdate;
  final Function() onRemove;
  final Function() onMoveUp;
  final Function() onMoveDown;

  const RenameRuleCard({
    super.key,
    required this.index,
    required this.rule,
    required this.onUpdate,
    required this.onRemove,
    required this.onMoveUp,
    required this.onMoveDown,
  });

  @override
  State<RenameRuleCard> createState() => _RenameRuleCardState();
}

class _RenameRuleCardState extends State<RenameRuleCard> {
  late RenameRule _rule;

  @override
  void initState() {
    super.initState();
    _rule = widget.rule;
  }

  void _updateRule(RenameRule updatedRule) {
    setState(() {
      _rule = updatedRule;
    });
    widget.onUpdate(updatedRule);
  }

  @override
  Widget build(BuildContext context) {
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
              Expanded(
                child: TextField(
                  decoration: const InputDecoration(
                    labelText: '规则名称',
                    border: OutlineInputBorder(),
                    isDense: true,
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                  ),
                  controller: TextEditingController(text: _rule.name),
                  onChanged: (value) {
                    _updateRule(_rule.copyWith(name: value));
                  },
                ),
              ),
              const SizedBox(width: 8),
              Switch(
                value: _rule.enabled,
                onChanged: (value) {
                  _updateRule(_rule.copyWith(enabled: value));
                },
              ),
              const SizedBox(width: 8),
              IconButton(
                icon: const Icon(Icons.arrow_up, color: Colors.blue),
                onPressed: widget.onMoveUp,
                tooltip: '上移规则',
              ),
              IconButton(
                icon: const Icon(Icons.arrow_down, color: Colors.blue),
                onPressed: widget.onMoveDown,
                tooltip: '下移规则',
              ),
              IconButton(
                icon: const Icon(Icons.close, color: Colors.red),
                onPressed: widget.onRemove,
                tooltip: '删除规则',
              ),
            ],
          ),
          const SizedBox(height: 12),
          ConditionBuilder(
            conditions: _rule.conditions,
            onUpdate: () {
              _updateRule(_rule);
            },
          ),
          const SizedBox(height: 12),
          ActionBuilder(
            actions: _rule.actions,
            onUpdate: () {
              _updateRule(_rule);
            },
          ),
        ],
      ),
    );
  }
}
