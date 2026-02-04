import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/rename_rule.dart';
import 'package:filemanager_flutter/models/rename_condition.dart';
import 'package:filemanager_flutter/models/rename_action.dart';

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
                    return _buildRuleCard(index, _rules[index]);
                  },
                ),
        ),
      ],
    );
  }

  Widget _buildRuleCard(int index, RenameRule rule) {
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
                  controller: TextEditingController(text: rule.name),
                  onChanged: (value) {
                    _updateRule(index, rule.copyWith(name: value));
                  },
                ),
              ),
              const SizedBox(width: 8),
              Switch(
                value: rule.enabled,
                onChanged: (value) {
                  _updateRule(index, rule.copyWith(enabled: value));
                },
              ),
              const SizedBox(width: 8),
              IconButton(
                icon: const Icon(Icons.close, color: Colors.red),
                onPressed: () => _removeRule(index),
              ),
            ],
          ),
          const SizedBox(height: 12),
          _buildConditionsSection(index, rule),
          const SizedBox(height: 12),
          _buildActionsSection(index, rule),
        ],
      ),
    );
  }

  Widget _buildConditionsSection(int ruleIndex, RenameRule rule) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '条件',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
            color: Color(0xFF616161),
          ),
        ),
        const SizedBox(height: 8),
        ...rule.conditions.asMap().entries.map((entry) {
          final condIndex = entry.key;
          final condition = entry.value;
          return _buildConditionItem(ruleIndex, condIndex, condition);
        }),
        const SizedBox(height: 8),
        _buildAddConditionForm(ruleIndex, rule),
      ],
    );
  }

  Widget _buildConditionItem(int ruleIndex, int condIndex, RenameCondition condition) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          const Text('• ', style: TextStyle(fontSize: 16)),
          Expanded(
            child: Text(
              _formatCondition(condition),
              style: const TextStyle(color: Color(0xFF424242)),
            ),
          ),
          const SizedBox(width: 5),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) {
              switch (value) {
                case 'move_up':
                  setState(() {
                    final rule = _rules[ruleIndex];
                    if (condIndex > 0) {
                      rule.conditions.removeAt(condIndex);
                      rule.conditions.insert(condIndex - 1, condition);
                      _updateRules(_rules);
                    }
                  });
                  break;
                case 'move_down':
                  setState(() {
                    final rule = _rules[ruleIndex];
                    if (condIndex < rule.conditions.length - 1) {
                      rule.conditions.removeAt(condIndex);
                      rule.conditions.insert(condIndex + 1, condition);
                      _updateRules(_rules);
                    }
                  });
                  break;
                case 'delete':
                  setState(() {
                    final rule = _rules[ruleIndex];
                    rule.conditions.removeAt(condIndex);
                    _updateRules(_rules);
                  });
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

  String _formatCondition(RenameCondition condition) {
    final typeMap = {
      '文件名匹配': '文件名',
      '文件路径匹配': '文件路径',
      '文件大小': '文件大小',
      '文件修改日期': '文件修改日期',
      '文件扩展名': '文件扩展名',
      '正则表达式': '正则表达式',
    };
    final operatorMap = {
      '等于': '等于',
      '包含': '包含',
      '开始于': '开始于',
      '结束于': '结束于',
      '不等于': '不等于',
      '不包含': '不包含',
      '大于': '大于',
      '小于': '小于',
      '大于等于': '大于等于',
      '小于等于': '小于等于',
    };
    final typeName = typeMap[condition.type] ?? condition.type;
    final operatorName = operatorMap[condition.operator] ?? condition.operator;
    return '$typeName $operatorName ${condition.value ?? ''}';
  }

  Widget _buildAddConditionForm(int ruleIndex, RenameRule rule) {
    String selectedType = '文件名匹配';
    String selectedOperator = '包含';
    String valueText = '';

    return StatefulBuilder(
      builder: (context, setState) {
        return Row(
          children: [
            DropdownButton<String>(
              value: selectedType,
              items: const [
                DropdownMenuItem(value: '文件名匹配', child: Text('文件名匹配')),
                DropdownMenuItem(value: '文件路径匹配', child: Text('文件路径匹配')),
                DropdownMenuItem(value: '文件大小', child: Text('文件大小')),
                DropdownMenuItem(value: '文件修改日期', child: Text('文件修改日期')),
                DropdownMenuItem(value: '文件扩展名', child: Text('文件扩展名')),
                DropdownMenuItem(value: '正则表达式', child: Text('正则表达式')),
              ],
              onChanged: (value) {
                setState(() {
                  selectedType = value ?? '文件名匹配';
                });
              },
            ),
            const SizedBox(width: 5),
            DropdownButton<String>(
              value: selectedOperator,
              items: _getOperatorsForType(selectedType),
              onChanged: (value) {
                setState(() {
                  selectedOperator = value ?? '包含';
                });
              },
            ),
            const SizedBox(width: 5),
            Expanded(
              child: TextField(
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
                if (valueText.isNotEmpty) {
                  setState(() {
                    final newCondition = RenameCondition(
                      type: selectedType,
                      operator: selectedOperator,
                      value: valueText,
                    );
                    rule.conditions.add(newCondition);
                    _updateRules(_rules);
                  });
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

  List<DropdownMenuItem<String>> _getOperatorsForType(String type) {
    switch (type) {
      case '文件大小':
      case '文件修改日期':
        return const [
          DropdownMenuItem(value: '等于', child: Text('等于')),
          DropdownMenuItem(value: '大于', child: Text('大于')),
          DropdownMenuItem(value: '小于', child: Text('小于')),
          DropdownMenuItem(value: '大于等于', child: Text('大于等于')),
          DropdownMenuItem(value: '小于等于', child: Text('小于等于')),
        ];
      case '正则表达式':
        return const [
          DropdownMenuItem(value: '等于', child: Text('匹配')),
        ];
      default:
        return const [
          DropdownMenuItem(value: '等于', child: Text('等于')),
          DropdownMenuItem(value: '包含', child: Text('包含')),
          DropdownMenuItem(value: '开始于', child: Text('开始于')),
          DropdownMenuItem(value: '结束于', child: Text('结束于')),
          DropdownMenuItem(value: '不等于', child: Text('不等于')),
          DropdownMenuItem(value: '不包含', child: Text('不包含')),
        ];
    }
  }

  Widget _buildActionsSection(int ruleIndex, RenameRule rule) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '操作',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
            color: Color(0xFF616161),
          ),
        ),
        const SizedBox(height: 8),
        ...rule.actions.asMap().entries.map((entry) {
          final actionIndex = entry.key;
          final action = entry.value;
          return _buildActionItem(ruleIndex, actionIndex, action);
        }),
        const SizedBox(height: 8),
        _buildAddActionForm(ruleIndex, rule),
      ],
    );
  }

  Widget _buildActionItem(int ruleIndex, int actionIndex, RenameAction action) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          const Text('• ', style: TextStyle(fontSize: 16)),
          Expanded(
            child: Text(
              _formatAction(action),
              style: const TextStyle(color: Color(0xFF424242)),
            ),
          ),
          const SizedBox(width: 5),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) {
              switch (value) {
                case 'move_up':
                  setState(() {
                    final rule = _rules[ruleIndex];
                    if (actionIndex > 0) {
                      rule.actions.removeAt(actionIndex);
                      rule.actions.insert(actionIndex - 1, action);
                      _updateRules(_rules);
                    }
                  });
                  break;
                case 'move_down':
                  setState(() {
                    final rule = _rules[ruleIndex];
                    if (actionIndex < rule.actions.length - 1) {
                      rule.actions.removeAt(actionIndex);
                      rule.actions.insert(actionIndex + 1, action);
                      _updateRules(_rules);
                    }
                  });
                  break;
                case 'delete':
                  setState(() {
                    final rule = _rules[ruleIndex];
                    rule.actions.removeAt(actionIndex);
                    _updateRules(_rules);
                  });
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

  String _formatAction(RenameAction action) {
    final typeMap = {
      '替换文本': '替换文本',
      '添加前缀': '添加前缀',
      '添加后缀': '添加后缀',
      '删除文本': '删除文本',
      '大小写转换': '大小写转换',
      '正则替换': '正则替换',
    };
    final typeName = typeMap[action.type] ?? action.type;
    return '$typeName ${action.value ?? ''}';
  }

  Widget _buildAddActionForm(int ruleIndex, RenameRule rule) {
    String selectedType = '添加前缀';
    String valueText = '';

    return StatefulBuilder(
      builder: (context, setState) {
        return Row(
          children: [
            DropdownButton<String>(
              value: selectedType,
              items: const [
                DropdownMenuItem(value: '添加前缀', child: Text('添加前缀')),
                DropdownMenuItem(value: '添加后缀', child: Text('添加后缀')),
                DropdownMenuItem(value: '删除文本', child: Text('删除文本')),
                DropdownMenuItem(value: '大小写转换', child: Text('大小写转换')),
              ],
              onChanged: (value) {
                setState(() {
                  selectedType = value ?? '添加前缀';
                });
              },
            ),
            const SizedBox(width: 5),
            Expanded(
              child: TextField(
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
                setState(() {
                  final newAction = RenameAction(
                    type: selectedType,
                    value: valueText,
                  );
                  rule.actions.add(newAction);
                  _updateRules(_rules);
                });
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF2980B9),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              ),
              child: const Text('添加操作'),
            ),
          ],
        );
      },
    );
  }
}
