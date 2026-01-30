import 'plugin_parameter.dart';
import 'precondition_group.dart';

class PluginConfig {
  final Map<String, dynamic> configValues;
  final List<PluginParameter> parameters;
  final List<PreconditionGroup> preconditionGroups;

  PluginConfig({
    required this.configValues,
    required this.parameters,
    required this.preconditionGroups,
  });

  factory PluginConfig.fromJson(Map<String, dynamic> json) {
    return PluginConfig(
      configValues: json['configValues'] != null ? Map<String, dynamic>.from(json['configValues']) : {},
      parameters: json['parameters'] != null 
          ? List<PluginParameter>.from(json['parameters'].map((x) => PluginParameter.fromJson(x)))
          : [],
      preconditionGroups: json['preconditionGroups'] != null 
          ? List<PreconditionGroup>.from(json['preconditionGroups'].map((x) => PreconditionGroup.fromJson(x)))
          : [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'configValues': configValues,
      'parameters': parameters.map((x) => x.toJson()).toList(),
      'preconditionGroups': preconditionGroups.map((x) => x.toJson()).toList(),
    };
  }
}
