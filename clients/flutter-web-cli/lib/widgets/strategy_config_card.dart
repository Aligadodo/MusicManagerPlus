import 'package:flutter/material.dart';
import '../models/strategy_info.dart';
import '../models/config_field.dart';
import '../models/precondition_group.dart';
import '../widgets/precondition_config_panel.dart';

class StrategyConfigCard extends StatefulWidget {
  final StrategyInfo strategy;
  final int index;
  final VoidCallback onDelete;
  final VoidCallback onMoveUp;
  final VoidCallback onMoveDown;
  final Function(StrategyInfo) onStrategyChanged;

  const StrategyConfigCard({
    super.key,
    required this.strategy,
    required this.index,
    required this.onDelete,
    required this.onMoveUp,
    required this.onMoveDown,
    required this.onStrategyChanged,
  });

  @override
  State<StrategyConfigCard> createState() => _StrategyConfigCardState();
}

class _StrategyConfigCardState extends State<StrategyConfigCard> {
  late StrategyInfo _currentStrategy;
  bool _isExpanded = true;

  @override
  void initState() {
    super.initState();
    // 创建策略的深拷贝，确保每个卡片都有独立的配置
    _currentStrategy = _createDeepCopy(widget.strategy);
  }

  @override
  void didUpdateWidget(covariant StrategyConfigCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    // 当外部策略变化时，更新内部状态
    if (widget.strategy.id != oldWidget.strategy.id ||
        widget.strategy.preconditionGroups.length != oldWidget.strategy.preconditionGroups.length) {
      setState(() {
        _currentStrategy = _createDeepCopy(widget.strategy);
      });
    }
  }

  StrategyInfo _createDeepCopy(StrategyInfo strategy) {
    // 创建策略的深拷贝，确保每个策略都有独立的配置
    final copiedConfigFields = strategy.configFields.map((field) => ConfigField(
      name: field.name,
      label: field.label,
      type: field.type,
      defaultValue: field.defaultValue,
      description: field.description,
      required: field.required,
      dependsOn: field.dependsOn,
      dependsValue: field.dependsValue,
      options: field.options,
      enumOptions: field.enumOptions,
      subFields: field.subFields,
      isModule: field.isModule,
      moduleType: field.moduleType,
    )).toList();
    
    final copiedPreconditionGroups = strategy.preconditionGroups.map((group) => group.copyWith(
      preconditions: group.preconditions.map((condition) => condition.copyWith()).toList(),
    )).toList();

    return StrategyInfo(
      id: strategy.id,
      name: strategy.name,
      description: strategy.description,
      configFields: copiedConfigFields,
      preconditionGroups: copiedPreconditionGroups,
      enabled: strategy.enabled,
      pipelineId: strategy.pipelineId,
    );
  }

  void _handlePreconditionGroupsChanged(List<PreconditionGroup> groups) {
    setState(() {
      // 创建前置条件组的深拷贝
      final copiedGroups = groups.map((group) => group.copyWith(
        preconditions: group.preconditions.map((condition) => condition.copyWith()).toList(),
      )).toList();
      
      final copiedConfigFields = _currentStrategy.configFields.map((field) => ConfigField(
        name: field.name,
        label: field.label,
        type: field.type,
        defaultValue: field.defaultValue,
        description: field.description,
        required: field.required,
        dependsOn: field.dependsOn,
        dependsValue: field.dependsValue,
        options: field.options,
        enumOptions: field.enumOptions,
        subFields: field.subFields,
        isModule: field.isModule,
        moduleType: field.moduleType,
      )).toList();
      
      _currentStrategy = StrategyInfo(
        id: _currentStrategy.id,
        name: _currentStrategy.name,
        description: _currentStrategy.description,
        configFields: copiedConfigFields,
        preconditionGroups: copiedGroups,
        enabled: _currentStrategy.enabled,
        pipelineId: _currentStrategy.pipelineId,
      );
    });
    
    // 通知父组件策略已变化
    widget.onStrategyChanged(_currentStrategy);
  }

  void _handleConfigFieldChanged(int fieldIndex, dynamic value) {
    setState(() {
      final updatedFields = List<ConfigField>.from(_currentStrategy.configFields);
      // 直接修改字段的defaultValue
      updatedFields[fieldIndex] = ConfigField(
        name: updatedFields[fieldIndex].name,
        label: updatedFields[fieldIndex].label,
        type: updatedFields[fieldIndex].type,
        defaultValue: value,
        description: updatedFields[fieldIndex].description,
        required: updatedFields[fieldIndex].required,
        dependsOn: updatedFields[fieldIndex].dependsOn,
        dependsValue: updatedFields[fieldIndex].dependsValue,
        options: updatedFields[fieldIndex].options,
        enumOptions: updatedFields[fieldIndex].enumOptions,
        subFields: updatedFields[fieldIndex].subFields,
        isModule: updatedFields[fieldIndex].isModule,
        moduleType: updatedFields[fieldIndex].moduleType,
      );
      
      final copiedPreconditionGroups = _currentStrategy.preconditionGroups.map((group) => group.copyWith(
        preconditions: group.preconditions.map((condition) => condition.copyWith()).toList(),
      )).toList();
      
      _currentStrategy = StrategyInfo(
        id: _currentStrategy.id,
        name: _currentStrategy.name,
        description: _currentStrategy.description,
        configFields: updatedFields,
        preconditionGroups: copiedPreconditionGroups,
        enabled: _currentStrategy.enabled,
        pipelineId: _currentStrategy.pipelineId,
      );
    });
    
    // 通知父组件策略已变化
    widget.onStrategyChanged(_currentStrategy);
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 10),
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _isExpanded ? Colors.blue.shade50 : Colors.white,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(8),
                topRight: Radius.circular(8),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _currentStrategy.name,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height:4),
                      Text(
                        'ID: ${_currentStrategy.id}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                        ),
                      ),
                    ],
                  ),
                ),
                Row(
                  children: [
                    IconButton(
                      icon: Icon(
                        _isExpanded ? Icons.expand_less : Icons.expand_more,
                        color: Colors.blue,
                      ),
                      onPressed: () {
                        setState(() {
                          _isExpanded = !_isExpanded;
                        });
                      },
                    ),
                    IconButton(
                      icon: const Icon(Icons.delete, color: Colors.red),
                      onPressed: widget.onDelete,
                    ),
                  ],
                ),
              ],
            ),
          ),
          if (_isExpanded)
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                borderRadius: const BorderRadius.only(
                  bottomLeft: Radius.circular(8),
                  bottomRight: Radius.circular(8),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 前置条件配置区域
                  _buildPreconditionSection(),
                  const SizedBox(height: 20),
                  // 业务参数配置区域
                  _buildConfigFieldsSection(),
                  const SizedBox(height: 16),
                  // 移动控制按钮
                  _buildMoveControls(),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildPreconditionSection() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.blue.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.filter_list, color: Colors.blue.shade700, size: 20),
              const SizedBox(width: 8),
              const Text(
                '前置条件配置',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Colors.blue,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          PreconditionConfigPanel(
            preconditionGroups: _currentStrategy.preconditionGroups,
            onPreconditionGroupsChanged: _handlePreconditionGroupsChanged,
          ),
        ],
      ),
    );
  }

  Widget _buildConfigFieldsSection() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.green.shade200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.settings, color: Colors.green.shade700, size: 20),
              const SizedBox(width: 8),
              const Text(
                '业务参数配置',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Colors.green,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (_currentStrategy.configFields.isEmpty)
            const Text(
              '此策略暂无业务参数配置项',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
            )
          else
            ..._currentStrategy.configFields.asMap().entries.map((entry) {
              return _buildConfigField(entry.key, entry.value);
            }),
        ],
      ),
    );
  }

  Widget _buildConfigField(int index, ConfigField field) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            field.label,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 4),
          _buildFieldInput(field, index),
        ],
      ),
    );
  }

  Widget _buildFieldInput(ConfigField field, int index) {
    switch (field.type) {
      case 'text':
        return TextField(
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            hintText: field.description,
            contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          ),
          controller: TextEditingController(
            text: field.defaultValue?.toString() ?? '',
          ),
          onChanged: (value) => _handleConfigFieldChanged(index, value),
        );
      case 'number':
        return TextField(
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            hintText: field.description,
            contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          ),
          controller: TextEditingController(
            text: field.defaultValue?.toString() ?? '',
          ),
          keyboardType: TextInputType.number,
          onChanged: (value) => _handleConfigFieldChanged(index, value),
        );
      case 'boolean':
        return Row(
          children: [
            Checkbox(
              value: field.defaultValue ?? false,
              onChanged: (value) => _handleConfigFieldChanged(index, value),
            ),
            const SizedBox(width: 8),
            Text(
              field.description ?? '',
              style: const TextStyle(fontSize: 12),
            ),
          ],
        );
      case 'select':
        return DropdownButtonFormField<String>(
          decoration: const InputDecoration(
            border: OutlineInputBorder(),
            contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          ),
          value: field.defaultValue?.toString(),
          items: field.options?.map((option) {
            return DropdownMenuItem<String>(
              value: option,
              child: Text(option),
            );
          }).toList() ?? [],
          onChanged: (value) => _handleConfigFieldChanged(index, value),
        );
      default:
        return TextField(
          decoration: InputDecoration(
            border: const OutlineInputBorder(),
            hintText: field.description,
            contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          ),
          controller: TextEditingController(
            text: field.defaultValue?.toString() ?? '',
          ),
          onChanged: (value) => _handleConfigFieldChanged(index, value),
        );
    }
  }

  Widget _buildMoveControls() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        IconButton(
          icon: const Icon(Icons.arrow_upward),
          onPressed: widget.index > 0 ? widget.onMoveUp : null,
          disabledColor: Colors.grey,
        ),
        IconButton(
          icon: const Icon(Icons.arrow_downward),
          onPressed: widget.onMoveDown,
          disabledColor: Colors.grey,
        ),
      ],
    );
  }
}