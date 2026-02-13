import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/precondition.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
import 'package:filemanager_flutter/widgets/precondition_item.dart';
import 'package:filemanager_flutter/widgets/selectable_text_widget.dart';
import 'package:filemanager_flutter/utils/theme_utils.dart';

class PreconditionGroupItem extends StatelessWidget {
  final int index;
  final PreconditionGroup group;
  final Function(int, PreconditionGroup) onUpdate;
  final Function(int) onDelete;
  final Function(int) onAddCondition;
  final Function(int, int, Precondition) onUpdateCondition;
  final Function(int, int) onDeleteCondition;

  const PreconditionGroupItem({
    super.key,
    required this.index,
    required this.group,
    required this.onUpdate,
    required this.onDelete,
    required this.onAddCondition,
    required this.onUpdateCondition,
    required this.onDeleteCondition,
  });

  void _updateLogicType(String logicType) {
    onUpdate(index, PreconditionGroup(
      id: group.id,
      name: group.name,
      description: group.description,
      logicType: logicType,
      preconditions: group.preconditions,
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: ThemeUtils.getPrimaryColor(context).withOpacity(0.3)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.03),
            blurRadius: 3,
            offset: const Offset(0, 1),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(context),
          if (group.preconditions.isEmpty) _buildEmptyState(context),
          if (group.preconditions.isNotEmpty) _buildConditions(context),
          _buildAddButton(context),
        ],
      ),
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: ThemeUtils.getBackgroundColor(context).withOpacity(0.5),
        borderRadius: const BorderRadius.only(
          topLeft: Radius.circular(8),
          topRight: Radius.circular(8),
        ),
        border: Border(
          bottom: BorderSide(color: ThemeUtils.getBorderColor(context)),
        ),
      ),
      child: Row(
        children: [
          Icon(Icons.folder_open, color: ThemeUtils.getPrimaryColor(context), size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: SelectableTextWidget(
              text: group.name,
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 14,
                color: Colors.black87,
              ),
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: ThemeUtils.getBorderColor(context)),
            ),
            child: DropdownButton<String>(
              value: group.logicType,
              items: const [
                DropdownMenuItem(value: 'AND', child: Text('AND', style: TextStyle(fontSize: 12))),
                DropdownMenuItem(value: 'OR', child: Text('OR', style: TextStyle(fontSize: 12))),
              ],
              onChanged: (value) => _updateLogicType(value ?? 'AND'),
              style: const TextStyle(fontSize: 12),
              dropdownColor: Colors.white,
              underline: const SizedBox.shrink(),
              icon: Icon(Icons.arrow_drop_down, color: ThemeUtils.getTextSecondaryColor(context), size: 18),
              isDense: true,
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: Icon(Icons.delete_outline, color: ThemeUtils.getErrorColor(context).withOpacity(0.7), size: 18),
            onPressed: () => onDelete(index),
            tooltip: '删除条件组',
            padding: EdgeInsets.zero,
            constraints: const BoxConstraints(),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 20),
      child: Center(
        child: Column(
          children: [
            Icon(Icons.rule, color: ThemeUtils.getTextSecondaryColor(context), size: 28),
            const SizedBox(height: 8),
            Text(
              '暂无条件',
              style: TextStyle(
                color: ThemeUtils.getTextSecondaryColor(context),
                fontSize: 12,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConditions(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8),
      child: Column(
        children: group.preconditions.asMap().entries.map((entry) {
          int conditionIndex = entry.key;
          Precondition condition = entry.value;
          return PreconditionItem(
            groupIndex: index,
            conditionIndex: conditionIndex,
            condition: condition,
            onUpdate: onUpdateCondition,
            onDelete: onDeleteCondition,
          );
        }).toList(),
      ),
    );
  }

  Widget _buildAddButton(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: ThemeUtils.getBackgroundColor(context).withOpacity(0.5),
        borderRadius: const BorderRadius.only(
          bottomLeft: Radius.circular(8),
          bottomRight: Radius.circular(8),
        ),
        border: Border(
          top: BorderSide(color: ThemeUtils.getBorderColor(context)),
        ),
      ),
      child: SizedBox(
        width: double.infinity,
        child: OutlinedButton.icon(
          onPressed: () => onAddCondition(index),
          icon: Icon(Icons.add, size: 16, color: ThemeUtils.getSuccessColor(context)),
          label: Text('添加条件', style: TextStyle(color: ThemeUtils.getSuccessColor(context), fontSize: 12)),
          style: OutlinedButton.styleFrom(
            side: BorderSide(color: ThemeUtils.getSuccessColor(context).withOpacity(0.5)),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ),
      ),
    );
  }
}
