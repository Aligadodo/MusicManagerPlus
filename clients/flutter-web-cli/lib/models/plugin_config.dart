import 'plugin_parameter.dart';
import 'rule_condition_group.dart';

class PluginConfig {
  final Map<String, dynamic> configValues;
  final List<PluginParameter> parameters;
  final List<RuleConditionGroup> preconditionGroups;

  PluginConfig({
    required this.configValues,
    required this.parameters,
    required this.preconditionGroups,
  });

  factory PluginConfig.fromJson(Map<String, dynamic> json) {
    // 处理后端返回的格式 (只包含configMap)
    if (json.containsKey('configMap')) {
      return PluginConfig(
        configValues: Map<String, dynamic>.from(json['configMap'] as Map),
        parameters: [],
        preconditionGroups: [],
      );
    }
    
    // 处理前端期望的格式
    return PluginConfig(
      configValues: json['configValues'] != null ? Map<String, dynamic>.from(json['configValues']) : {},
      parameters: json['parameters'] != null 
          ? List<PluginParameter>.from(json['parameters'].map((x) => PluginParameter.fromJson(x)))
          : [],
      preconditionGroups: json['preconditionGroups'] != null 
          ? List<RuleConditionGroup>.from(json['preconditionGroups'].map((x) => RuleConditionGroup.fromJson(x)))
          : [],
    );
  }

  Map<String, dynamic> toJson() {
    // 转换为后端期望的格式
    return {
      'configMap': configValues,
    };
  }
}
