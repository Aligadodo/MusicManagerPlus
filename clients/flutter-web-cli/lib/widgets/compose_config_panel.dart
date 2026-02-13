import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/models/precondition_group.dart';
import 'package:filemanager_flutter/widgets/parameter_field.dart';
import 'package:filemanager_flutter/widgets/compose_precondition_panel.dart';
import 'package:filemanager_flutter/utils/theme_utils.dart';

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
      decoration: ThemeUtils.getCardDecoration(context),
      child: widget.strategyConfig == null
          ? Center(
              child: Text(
                '请选择一个步骤以查看配置',
                style: TextStyle(
                  color: ThemeUtils.getTextSecondaryColor(context),
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

    // 过滤可见的参数
    List<ConfigField> visibleFields = _filterVisibleFields(widget.strategyInfo!.configFields);
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(Icons.settings, color: ThemeUtils.getPrimaryColor(context), size: 18),
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
              child: Icon(Icons.help_outline, color: ThemeUtils.getTextSecondaryColor(context), size: 16),
            ),
          ],
        ),
        const SizedBox(height: 12),
        ...visibleFields.map((field) {
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
                color: ThemeUtils.getErrorColor(context).withOpacity(0.1),
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: ThemeUtils.getErrorColor(context).withOpacity(0.3)),
              ),
              child: Text(
                '构建参数字段失败: $e',
                style: TextStyle(color: ThemeUtils.getErrorColor(context), fontSize: 12),
              ),
            );
          }
        }).toList(),
      ],
    );
  }
  
  /**
   * 过滤可见的参数
   * @param allFields 所有参数
   * @return 可见的参数列表
   */
  List<ConfigField> _filterVisibleFields(List<ConfigField> allFields) {
    List<ConfigField> visibleFields = [];
    
    for (ConfigField field in allFields) {
      if (_isFieldVisible(field)) {
        visibleFields.add(field);
      }
    }
    
    return visibleFields;
  }
  
  /**
   * 判断参数是否可见
   * @param field 参数
   * @return 是否可见
   */
  bool _isFieldVisible(ConfigField field) {
    // 检查阻止条件
    if (field.blockConditions != null && field.blockConditions!.isNotEmpty) {
      for (Map<String, dynamic> condition in field.blockConditions!) {
        if (_isConditionMet(condition)) {
          return false;
        }
      }
    }
    
    // 检查依赖条件
    if (field.dependsOn != null && field.dependsValue != null) {
      try {
        final dependentValue = widget.strategyConfig?.getValue(field.dependsOn!);
        if (dependentValue == null || dependentValue.toString() != field.dependsValue) {
          return false;
        }
      } catch (e) {
        return false;
      }
    }
    
    // 检查父参数是否可见（对于子参数）
    if (field.dependsOn != null) {
      ConfigField? parentField = _findFieldByName(field.dependsOn!);
      if (parentField != null && !_isFieldVisible(parentField)) {
        return false;
      }
    }
    
    return true;
  }
  
  /**
   * 判断条件是否满足
   * @param condition 条件
   * @return 是否满足
   */
  bool _isConditionMet(Map<String, dynamic> condition) {
    String? dependentParam = condition['dependentParam'] as String?;
    dynamic expectedValue = condition['expectedValue'];
    
    if (dependentParam == null || expectedValue == null) {
      return false;
    }
    
    dynamic actualValue = widget.strategyConfig?.getValue(dependentParam);
    return expectedValue.toString() == actualValue?.toString();
  }
  
  /**
   * 根据名称查找参数
   * @param name 参数名
   * @return 参数
   */
  ConfigField? _findFieldByName(String name) {
    try {
      return widget.strategyInfo!.configFields.firstWhere(
        (field) => field.name == name,
      );
    } catch (e) {
      return null;
    }
  }
}
