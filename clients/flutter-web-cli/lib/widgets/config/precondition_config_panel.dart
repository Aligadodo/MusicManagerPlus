import 'package:flutter/material.dart';
import '../../models/precondition.dart';
import '../../models/precondition_group.dart';
import '../../models/precondition_field_config.dart';
import 'precondition_group_item.dart';
import '../common/selectable_text_widget.dart';

class PreconditionConfigPanel extends StatefulWidget {
  final List<PreconditionGroup>? preconditionGroups;
  final Function(List<PreconditionGroup>?) onPreconditionGroupsChanged;

  const PreconditionConfigPanel({
    super.key,
    this.preconditionGroups,
    required this.onPreconditionGroupsChanged,
  });

  @override
  State<PreconditionConfigPanel> createState() => _PreconditionConfigPanelState();
}

class _PreconditionConfigPanelState extends State<PreconditionConfigPanel> {
  late List<PreconditionGroup> _preconditionGroups;

  @override
  void initState() {
    super.initState();
    _preconditionGroups = widget.preconditionGroups ?? [];
  }

  @override
  void didUpdateWidget(covariant PreconditionConfigPanel oldWidget) {
    super.didUpdateWidget(oldWidget);
    // 当父组件传递新的 preconditionGroups 时，更新内部状态
    // 使用深拷贝确保每个策略都有独立的前置条件配置
    if (widget.preconditionGroups != oldWidget.preconditionGroups) {
      final newGroups = widget.preconditionGroups?.map((group) => group.copyWith(
        preconditions: group.preconditions.map((condition) => condition.copyWith()).toList(),
      )).toList() ?? [];
      
      setState(() {
        _preconditionGroups = newGroups;
      });
    }
  }

  void _addPreconditionGroup() {
    final newGroups = List<PreconditionGroup>.from(_preconditionGroups);
    newGroups.add(PreconditionGroup(
      id: 'group_${DateTime.now().millisecondsSinceEpoch}',
      name: '条件组 ${newGroups.length + 1}',
      description: '条件组描述',
      logicType: 'AND',
      preconditions: [],
    ));
    _updatePreconditionGroups(newGroups);
  }

  void _removePreconditionGroup(int index) {
    final newGroups = List<PreconditionGroup>.from(_preconditionGroups);
    newGroups.removeAt(index);
    _updatePreconditionGroups(newGroups);
  }

  void _addPrecondition(int groupIndex) {
    final newGroups = List<PreconditionGroup>.from(_preconditionGroups);
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
      description: '新条件',
    ));
    newGroups[groupIndex] = PreconditionGroup(
      id: group.id,
      name: group.name,
      description: group.description,
      logicType: group.logicType,
      preconditions: newPreconditions,
    );
    _updatePreconditionGroups(newGroups);
  }

  void _removePrecondition(int groupIndex, int conditionIndex) {
    final newGroups = List<PreconditionGroup>.from(_preconditionGroups);
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
    _updatePreconditionGroups(newGroups);
  }

  void _updatePrecondition(int groupIndex, Precondition updatedCondition) {
    final newGroups = List<PreconditionGroup>.from(_preconditionGroups);
    final group = newGroups[groupIndex];
    final newPreconditions = List<Precondition>.from(group.preconditions);
    final conditionIndex = newPreconditions.indexWhere((c) => c.id == updatedCondition.id);
    if (conditionIndex != -1) {
      newPreconditions[conditionIndex] = updatedCondition;
    }
    newGroups[groupIndex] = PreconditionGroup(
      id: group.id,
      name: group.name,
      description: group.description,
      logicType: group.logicType,
      preconditions: newPreconditions,
    );
    _updatePreconditionGroups(newGroups);
  }

  void _updatePreconditionGroups(List<PreconditionGroup> newGroups) {
    setState(() {
      _preconditionGroups = newGroups;
    });
    widget.onPreconditionGroupsChanged(newGroups);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.blue.shade50,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(8),
                topRight: Radius.circular(8),
              ),
              border: Border(
                bottom: BorderSide(color: Colors.grey.shade300),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(Icons.filter_list, color: Colors.blue.shade700, size: 20),
                    const SizedBox(width: 8),
                    SelectableTextWidget(
                      text: '前置条件配置',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Colors.blue.shade900,
                      ),
                    ),
                  ],
                ),
                ElevatedButton.icon(
                  onPressed: _addPreconditionGroup,
                  icon: const Icon(Icons.add, size: 16),
                  label: const Text('添加条件组', style: TextStyle(fontSize: 12)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blue.shade600,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    minimumSize: const Size(0, 0),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(4),
                    ),
                  ),
                ),
              ],
            ),
          ),
          if (_preconditionGroups.isEmpty)
            Padding(
              padding: const EdgeInsets.all(20),
              child: Center(
                child: Column(
                  children: [
                    Icon(Icons.filter_list_outlined, color: Colors.grey.shade400, size: 48),
                    const SizedBox(height: 8),
                    SelectableTextWidget(
                      text: '暂无前置条件',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey.shade600,
                      ),
                    ),
                    const SizedBox(height: 4),
                    SelectableTextWidget(
                      text: '点击上方按钮添加条件组',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey.shade500,
                      ),
                    ),
                  ],
                ),
              ),
            )
          else
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.blue.shade50,
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Row(
                      children: [
                        Icon(Icons.info_outline, color: Colors.blue.shade700, size: 16),
                        const SizedBox(width: 8),
                        Expanded(
                          child: SelectableTextWidget(
                            text: '前置条件用于在策略执行前过滤文件，只有符合前置条件的文件才会被处理。',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.blue.shade900,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  ...List.generate(_preconditionGroups.length, (groupIndex) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: PreconditionGroupItem(
                        key: ValueKey(_preconditionGroups[groupIndex].id),
                        index: groupIndex,
                        group: _preconditionGroups[groupIndex],
                        onUpdate: (index, updatedGroup) {
                          final newGroups = List<PreconditionGroup>.from(_preconditionGroups);
                          newGroups[index] = updatedGroup;
                          _updatePreconditionGroups(newGroups);
                        },
                        onDelete: (index) => _removePreconditionGroup(index),
                        onAddCondition: (index) => _addPrecondition(index),
                        onUpdateCondition: (groupIndex, conditionIndex, updatedCondition) {
                          _updatePrecondition(groupIndex, updatedCondition);
                        },
                        onDeleteCondition: (groupIndex, conditionIndex) {
                          _removePrecondition(groupIndex, conditionIndex);
                        },
                      ),
                    );
                  }),
                ],
              ),
            ),
        ],
      ),
    );
  }
}