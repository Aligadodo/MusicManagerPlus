import 'package:flutter/material.dart';
import '../../models/rename_rule.dart';
import 'rename_rule_card.dart';

class RenameRuleEditor extends StatefulWidget {
  final List<RenameRule> rules;
  final Function(List<RenameRule>) onChanged;

  const RenameRuleEditor({
    super.key,
    required this.rules,
    required this.onChanged,
  });

  @override
  State<RenameRuleEditor> createState() => _RenameRuleEditorState();
}

class _RenameRuleEditorState extends State<RenameRuleEditor> {
  late List<RenameRule> _rules;

  @override
  void initState() {
    super.initState();
    _rules = widget.rules;
  }

  void _updateRules(List<RenameRule> newRules) {
    setState(() {
      _rules = newRules;
    });
    widget.onChanged(newRules);
  }

  void _addRule() {
    final newRule = RenameRule(
      name: '规则 ${_rules.length + 1}',
      enabled: true,
    );
    _updateRules([..._rules, newRule]);
  }

  void _removeRule(int index) {
    final newRules = <RenameRule>[];
    for (int i = 0; i < _rules.length; i++) {
      if (i != index) {
        newRules.add(_rules[i]);
      }
    }
    _updateRules(newRules);
  }

  void _updateRule(int index, RenameRule updatedRule) {
    final newRules = List<RenameRule>.from(_rules);
    newRules[index] = updatedRule;
    _updateRules(newRules);
  }

  void _moveRuleUp(int index) {
    if (index > 0) {
      final newRules = List<RenameRule>.from(_rules);
      final rule = newRules.removeAt(index);
      newRules.insert(index - 1, rule);
      _updateRules(newRules);
    }
  }

  void _moveRuleDown(int index) {
    if (index < _rules.length - 1) {
      final newRules = List<RenameRule>.from(_rules);
      final rule = newRules.removeAt(index);
      newRules.insert(index + 1, rule);
      _updateRules(newRules);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ElevatedButton.icon(
          onPressed: _addRule,
          icon: const Icon(Icons.add),
          label: const Text('添加规则'),
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF2980B9),
            foregroundColor: Colors.white,
          ),
        ),
        const SizedBox(height: 16),
        Expanded(
          child: _rules.isEmpty
              ? const Center(
                  child: Text(
                    '暂无规则，点击上方按钮添加',
                    style: TextStyle(color: Colors.grey),
                  ),
                )
              : ListView.builder(
                  itemCount: _rules.length,
                  itemBuilder: (context, index) {
                    return RenameRuleCard(
                      index: index,
                      rule: _rules[index],
                      onUpdate: (updatedRule) => _updateRule(index, updatedRule),
                      onRemove: () => _removeRule(index),
                      onMoveUp: () => _moveRuleUp(index),
                      onMoveDown: () => _moveRuleDown(index),
                    );
                  },
                ),
        ),
      ],
    );
  }
}
