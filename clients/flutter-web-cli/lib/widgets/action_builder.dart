import 'package:flutter/material.dart';
import '../models/rename_action.dart';

class ActionBuilder extends StatefulWidget {
  final List<RenameAction> actions;
  final Function() onUpdate;

  const ActionBuilder({
    super.key,
    required this.actions,
    required this.onUpdate,
  });

  @override
  State<ActionBuilder> createState() => _ActionBuilderState();
}

class _ActionBuilderState extends State<ActionBuilder> {
  @override
  Widget build(BuildContext context) {
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
        ...widget.actions.asMap().entries.map((entry) {
          final actionIndex = entry.key;
          final action = entry.value;
          return _buildActionItem(actionIndex, action);
        }),
        const SizedBox(height: 8),
        _buildAddActionForm(),
      ],
    );
  }

  Widget _buildActionItem(int actionIndex, RenameAction action) {
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
                  if (actionIndex > 0) {
                    widget.actions.removeAt(actionIndex);
                    widget.actions.insert(actionIndex - 1, action);
                    widget.onUpdate();
                  }
                  break;
                case 'move_down':
                  if (actionIndex < widget.actions.length - 1) {
                    widget.actions.removeAt(actionIndex);
                    widget.actions.insert(actionIndex + 1, action);
                    widget.onUpdate();
                  }
                  break;
                case 'delete':
                  widget.actions.removeAt(actionIndex);
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

  Widget _buildAddActionForm() {
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
                final newAction = RenameAction(
                  type: selectedType,
                  value: valueText,
                );
                widget.actions.add(newAction);
                widget.onUpdate();
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
