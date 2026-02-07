import 'precondition_group.dart';

class StrategyConfig {
  final Map<String, dynamic> configValues;
  final List<PreconditionGroup>? preconditionGroups;

  StrategyConfig(this.configValues, {this.preconditionGroups});

  factory StrategyConfig.fromJson(Map<String, dynamic> json) {
    return StrategyConfig(
      json['configValues'] as Map<String, dynamic>? ?? {},
      preconditionGroups: json['preconditionGroups'] != null
          ? (json['preconditionGroups'] as List)
              .map((e) => PreconditionGroup.fromJson(e as Map<String, dynamic>))
              .toList()
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'configValues': configValues,
      if (preconditionGroups != null)
        'preconditionGroups': preconditionGroups!.map((e) => e.toJson()).toList(),
    };
  }

  dynamic getValue(String key) {
    return configValues[key];
  }

  void setValue(String key, dynamic value) {
    configValues[key] = value;
  }
}
