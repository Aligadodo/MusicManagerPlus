import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
import 'package:filemanager_flutter/widgets/parameter_field.dart';
import 'package:filemanager_flutter/widgets/compose_precondition_panel.dart';

class ComposeConfigPanel extends StatefulWidget {
  final StrategyInfo? strategyInfo;
  final StrategyConfig? strategyConfig;
  final List<PreconditionGroup> preconditionGroups;
  final Function(StrategyConfig?) onConfigChanged;
  final Function(List<PreconditionGroup>) onPreconditionGroupsChanged;

  const ComposeConfigPanel({
    super.key,
    required this.strategyInfo,
    required this.strategyConfig,
    required this.preconditionGroups,
    required this.onConfigChanged,
    required this.onPreconditionGroupsChanged,
  });

  @override
  State<ComposeConfigPanel> createState() => _ComposeConfigPanelState();
}

class _ComposeConfigPanelState extends State<ComposeConfigPanel> {
  void _updateConfigValue(String fieldName, dynamic value) {
    if (widget.strategyConfig == null) return;
    
    final newConfig = StrategyConfig(
      Map<String, dynamic>.from(widget.strategyConfig!.configValues),
    );
    newConfig.setValue(fieldName, value);
    widget.onConfigChanged(newConfig);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: widget.strategyConfig == null
          ? Center(
              child: Text(
                '请选择一个步骤以查看配置',
                style: TextStyle(
                  color: Color(0xFFBDBDBD),
                  fontSize: 13,
                ),
              ),
            )
          : SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ComposePreconditionPanel(
                    preconditionGroups: widget.preconditionGroups,
                    onPreconditionGroupsChanged: widget.onPreconditionGroupsChanged,
                  ),
                  const SizedBox(height: 15),
                  _buildParametersUI(),
                ],
              ),
            ),
    );
  }

  Widget _buildParametersUI() {
    if (widget.strategyInfo == null || widget.strategyInfo!.configFields.isEmpty) {
      return const Text('无', style: TextStyle(fontSize: 12));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(Icons.settings, color: Colors.blue.shade700, size: 18),
            const SizedBox(width: 8),
            const Text(
              '参数配置',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 15,
                color: Colors.black87,
              ),
            ),
            const SizedBox(width: 8),
            Tooltip(
              message: '配置当前步骤的参数',
              child: Icon(Icons.help_outline, color: Colors.grey.shade600, size: 16),
            ),
          ],
        ),
        const SizedBox(height: 12),
        ...widget.strategyInfo!.configFields.where((field) {
          if (field.dependsOn != null && field.dependsValue != null) {
            try {
              final dependentValue = widget.strategyConfig?.getValue(field.dependsOn!);
              return dependentValue?.toString() == field.dependsValue;
            } catch (e) {
              return true;
            }
          }
          return true;
        }).map((field) {
          try {
            return ParameterField(
              field: field,
              strategyConfig: widget.strategyConfig,
              onValueChanged: _updateConfigValue,
            );
          } catch (e) {
            return Container(
              margin: const EdgeInsets.only(bottom: 10),
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.red.shade50,
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: Colors.red.shade200),
              ),
              child: Text(
                '构建参数字段失败: $e',
                style: const TextStyle(color: Colors.red, fontSize: 12),
              ),
            );
          }
        }).toList(),
      ],
    );
  }
}
