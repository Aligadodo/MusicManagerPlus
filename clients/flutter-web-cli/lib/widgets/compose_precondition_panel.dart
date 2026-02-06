import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/precondition.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
import 'package:filemanager_flutter/models/precondition_field_config.dart';
import 'package:filemanager_flutter/widgets/precondition_group_item.dart';

class ComposePreconditionPanel extends StatefulWidget {
  final List<PreconditionGroup> preconditionGroups;
  final Function(List<PreconditionGroup>) onPreconditionGroupsChanged;

  const ComposePreconditionPanel({
    super.key,
    required this.preconditionGroups,
    required this.onPreconditionGroupsChanged,
  });

  @override
  State<ComposePreconditionPanel> createState() => _ComposePreconditionPanelState();
}

class _ComposePreconditionPanelState extends State<ComposePreconditionPanel> {
  void _addPreconditionGroup() {
    final newGroups = List<PreconditionGroup>.from(widget.preconditionGroups);
    newGroups.add(PreconditionGroup(
      id: 'group_${DateTime.now().millisecondsSinceEpoch}',
      name: '条件组 ${newGroups.length + 1}',
      description: '条件组描述',
      logicType: 'AND',
      preconditions: [],
    ));
    widget.onPreconditionGroupsChanged(newGroups);
  }

  void _removePreconditionGroup(int index) {
    final newGroups = List<PreconditionGroup>.from(widget.preconditionGroups);
    newGroups.removeAt(index);
    widget.onPreconditionGroupsChanged(newGroups);
  }

  void _addPrecondition(int groupIndex) {
    final newGroups = List<PreconditionGroup>.from(widget.preconditionGroups);
    final group = newGroups[groupIndex];
    final newPreconditions = List<Precondition>.from(group.preconditions);
    String newConditionId = 'condition_${DateTime.now().millisecondsSinceEpoch}';
    PreconditionFieldConfig defaultField = PreconditionFieldConfigs.fields.first;
    String defaultOperator = defaultField.operators.first.code;
    newPreconditions.add(Precondition(
      id: newConditionId,
      field: defaultField.code,
      operator: defaultOperator,
      value: '',
      description: '条件描述',
    ));
    newGroups[groupIndex] = PreconditionGroup(
      id: group.id,
      name: group.name,
      description: group.description,
      logicType: group.logicType,
      preconditions: newPreconditions,
    );
    widget.onPreconditionGroupsChanged(newGroups);
  }

  void _removePrecondition(int groupIndex, int conditionIndex) {
    final newGroups = List<PreconditionGroup>.from(widget.preconditionGroups);
    final group = newGroups[groupIndex];
    final newPreconditions = List<Precondition>.from(group.preconditions);
    newPreconditions.removeAt(conditionIndex);
    newGroups[groupIndex] = PreconditionGroup(
      id: group.id,
      name: group.name,
      description: group.description,
      logicType: group.logicType,
      preconditions: newPreconditions,
    );
    widget.onPreconditionGroupsChanged(newGroups);
  }

  void _updatePreconditionGroup(int index, PreconditionGroup group) {
    final newGroups = List<PreconditionGroup>.from(widget.preconditionGroups);
    newGroups[index] = group;
    widget.onPreconditionGroupsChanged(newGroups);
  }

  void _updatePrecondition(int groupIndex, int conditionIndex, Precondition condition) {
    final newGroups = List<PreconditionGroup>.from(widget.preconditionGroups);
    final group = newGroups[groupIndex];
    final newPreconditions = List<Precondition>.from(group.preconditions);
    newPreconditions[conditionIndex] = condition;
    newGroups[groupIndex] = PreconditionGroup(
      id: group.id,
      name: group.name,
      description: group.description,
      logicType: group.logicType,
      preconditions: newPreconditions,
    );
    widget.onPreconditionGroupsChanged(newGroups);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(),
          const SizedBox(height: 12),
          if (widget.preconditionGroups.isEmpty) _buildEmptyState(),
          if (widget.preconditionGroups.isNotEmpty) _buildGroups(),
          const SizedBox(height: 12),
          _buildAddButton(),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Icon(Icons.filter_list, color: Colors.blue.shade700, size: 18),
        const SizedBox(width: 8),
        const Text(
          '前置条件配置',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 15,
            color: Colors.black87,
          ),
        ),
        const SizedBox(width: 8),
        Tooltip(
          message: '设置文件处理的前置条件，只有满足条件的文件才会被处理',
          child: Icon(Icons.help_outline, color: Colors.grey.shade600, size: 16),
        ),
      ],
    );
  }

  Widget _buildEmptyState() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300, style: BorderStyle.solid),
      ),
      child: Column(
        children: [
          Icon(Icons.inbox, color: Colors.grey.shade400, size: 40),
          const SizedBox(height: 10),
          Text(
            '暂无前置条件',
            style: TextStyle(
              color: Colors.grey.shade600,
              fontSize: 13,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            '点击下方按钮添加条件组开始配置',
            style: TextStyle(
              color: Colors.grey.shade500,
              fontSize: 11,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGroups() {
    return Column(
      children: widget.preconditionGroups.asMap().entries.map((entry) {
        int index = entry.key;
        PreconditionGroup group = entry.value;
        return PreconditionGroupItem(
          index: index,
          group: group,
          onUpdate: _updatePreconditionGroup,
          onDelete: _removePreconditionGroup,
          onAddCondition: _addPrecondition,
          onUpdateCondition: _updatePrecondition,
          onDeleteCondition: _removePrecondition,
        );
      }).toList(),
    );
  }

  Widget _buildAddButton() {
    return ElevatedButton.icon(
      onPressed: _addPreconditionGroup,
      icon: const Icon(Icons.add_circle_outline, size: 18),
      label: const Text('添加条件组', style: TextStyle(fontSize: 13)),
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.blue.shade700,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(6),
        ),
      ),
    );
  }
}
