import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/strategy_info.dart';
import '../../models/strategy_config.dart';
import '../../models/config_field.dart';
import '../../widgets/config/config_field_builder.dart';
import '../../widgets/precondition_config_panel.dart';
import '../../models/precondition_group.dart';
import '../../models/rename_rule.dart';
import '../../widgets/rename_rule_editor.dart';
import '../../providers/config_provider.dart';

class StrategyConfigPanel extends ConsumerWidget {
  final StrategyInfo? selectedStrategy;
  final StrategyConfig? strategyConfig;
  final bool isLoading;
  final String errorMessage;
  final Function(StrategyConfig) onConfigChanged;

  const StrategyConfigPanel({
    super.key,
    this.selectedStrategy,
    this.strategyConfig,
    required this.isLoading,
    required this.errorMessage,
    required this.onConfigChanged,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    final showTooltips = config.globalSettings['showTooltips'] as bool? ?? true;

    try {
      return Expanded(
        child: Container(
          padding: const EdgeInsets.only(left: 20),
          decoration: const BoxDecoration(
            border: Border(
              left: BorderSide(color: Colors.grey, width: 1),
            ),
          ),
          child: selectedStrategy == null
              ? const Center(
                  child: Text('请选择一个策略进行配置'),
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      selectedStrategy!.name,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      selectedStrategy!.description,
                      style: const TextStyle(
                        color: Colors.grey,
                      ),
                    ),
                    const SizedBox(height: 30),

                    if (errorMessage.isNotEmpty)
                      Container(
                        padding: const EdgeInsets.all(10),
                        color: Colors.red[100],
                        child: Text(
                          errorMessage,
                          style: const TextStyle(color: Colors.red),
                        ),
                      ),

                    if (isLoading && strategyConfig == null)
                      const Center(
                        child: CircularProgressIndicator(),
                      )
                    else if (strategyConfig != null && selectedStrategy != null)
                      Expanded(
                        child: ListView(
                          children: [
                            ListView.builder(
                              shrinkWrap: true,
                              physics: const NeverScrollableScrollPhysics(),
                              itemCount: selectedStrategy!.configFields.length,
                              itemBuilder: (context, index) {
                                try {
                                  final ConfigField field = selectedStrategy!.configFields[index];
                                  return _buildConfigField(field, showTooltips);
                                } catch (e) {
                                  return Card(
                                    color: Colors.red.shade50,
                                    child: Padding(
                                      padding: const EdgeInsets.all(8.0),
                                      child: Text(
                                        '字段加载失败: $e',
                                        style: const TextStyle(color: Colors.red),
                                      ),
                                    ),
                                  );
                                }
                              },
                            ),
                            const SizedBox(height: 20),
                            PreconditionConfigPanel(
                              preconditionGroups: strategyConfig?.preconditionGroups,
                              onPreconditionGroupsChanged: (groups) {
                                final copiedGroups = groups?.map((group) => group.copyWith(
                                  preconditions: group.preconditions.map((condition) => condition.copyWith()).toList(),
                                )).toList() ?? <PreconditionGroup>[];
                                
                                onConfigChanged(StrategyConfig(
                                  strategyConfig!.configValues,
                                  preconditionGroups: copiedGroups,
                                ));
                              },
                            ),
                          ],
                        ),
                      ),

                    const SizedBox(height: 20),
                  ],
                ),
        ),
      );
    } catch (e) {
      return Expanded(
        child: Container(
          padding: const EdgeInsets.only(left: 20),
          decoration: const BoxDecoration(
            border: Border(
              left: BorderSide(color: Colors.grey, width: 1),
            ),
          ),
          child: Center(
            child: Card(
              color: Colors.red.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, color: Colors.red),
                    const SizedBox(height: 8),
                    Text(
                      '配置区域加载失败: $e',
                      style: const TextStyle(color: Colors.red),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      );
    }
  }

  Widget _buildConfigField(ConfigField field, bool showTooltips) {
    try {
      print('Building config field: ${field.name}, Type: ${field.type}');
      final value = strategyConfig?.getValue(field.name);

      if (field.name == 'rules') {
        print('Building rename rules field for ${field.name}');
        return _buildRenameRulesField(field, value, showTooltips);
      }

      final builder = ConfigFieldBuilderFactory.createBuilder(field.type);
      return builder.build(field, value, (newValue) {
        if (strategyConfig != null) {
          strategyConfig?.setValue(field.name, newValue);
          onConfigChanged(strategyConfig!);
        }
      }, showTooltips);
    } catch (e) {
      print('字段 ${field.name} 加载失败: $e');
      return Card(
        color: Colors.yellow.shade50,
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Row(
            children: [
              const Icon(Icons.warning, color: Colors.orange),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '字段 ${field.label} 加载异常',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.orange),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      '该字段将使用默认值或保持为空',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
  }

  Widget _buildRenameRulesField(ConfigField field, dynamic value, bool showTooltips) {
    try {
      List<RenameRule> rules = <RenameRule>[];
      if (value != null && value is List) {
        try {
          rules = value.map((item) {
            if (item is Map<String, dynamic>) {
              return RenameRule.fromJson(item);
            } else if (item is RenameRule) {
              return item;
            }
            return RenameRule(name: '未命名规则');
          }).toList();
        } catch (e) {
          print('解析重命名规则失败: $e');
          rules = [];
        }
      }
      
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (showTooltips && (field.description?.isNotEmpty ?? false))
              Tooltip(
                message: field.description ?? '',
                child: Text(
                  field.label,
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              )
            else
              Text(
                field.label,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            const SizedBox(height: 5),
            SizedBox(
              height: 500,
              child: RenameRuleEditor(
                rules: rules,
                onChanged: (newRules) {
                  if (strategyConfig != null) {
                    strategyConfig?.setValue(field.name, newRules.map((rule) => rule.toJson()).toList());
                    onConfigChanged(strategyConfig!);
                  }
                },
              ),
            ),
          ],
        ),
      );
    } catch (e) {
      print('构建重命名规则字段 ${field.name} 失败: $e');
      return Card(
        color: Colors.yellow.shade50,
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Row(
            children: [
              const Icon(Icons.warning, color: Colors.orange),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '字段 ${field.label} 加载异常',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.orange),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      '该字段将使用默认值或保持为空',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
  }
}
