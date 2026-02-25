import 'package:flutter/material.dart';
import '../models/rename_condition.dart';

class ConditionBuilder extends StatefulWidget {
  final List<RenameCondition> conditions;
  final Function() onUpdate;

  const ConditionBuilder({
    super.key,
    required this.conditions,
    required this.onUpdate,
  });

  @override
  State<ConditionBuilder> createState() => _ConditionBuilderState();
}

class _ConditionBuilderState extends State<ConditionBuilder> {
  @override
  Widget build(BuildContext context) {
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
        ...widget.conditions.asMap().entries.map((entry) {
          final condIndex = entry.key;
          final condition = entry.value;
          return _buildConditionItem(condIndex, condition);
        }),
        const SizedBox(height: 8),
        _buildAddConditionForm(),
      ],
    );
  }

  Widget _buildConditionItem(int condIndex, RenameCondition condition) {
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
                  if (condIndex > 0) {
                    widget.conditions.removeAt(condIndex);
                    widget.conditions.insert(condIndex - 1, condition);
                    widget.onUpdate();
                  }
                  break;
                case 'move_down':
                  if (condIndex < widget.conditions.length - 1) {
                    widget.conditions.removeAt(condIndex);
                    widget.conditions.insert(condIndex + 1, condition);
                    widget.onUpdate();
                  }
                  break;
                case 'delete':
                  widget.conditions.removeAt(condIndex);
                  widget.onUpdate();
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

  Widget _buildAddConditionForm() {
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
                  final newCondition = RenameCondition(
                    type: selectedType,
                    operator: selectedOperator,
                    value: valueText,
                  );
                  widget.conditions.add(newCondition);
                  widget.onUpdate();
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
}
