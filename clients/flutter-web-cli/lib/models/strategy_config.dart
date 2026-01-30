class StrategyConfig {
  final Map<String, dynamic> configValues;

  StrategyConfig(this.configValues);

  factory StrategyConfig.fromJson(Map<String, dynamic> json) {
    return StrategyConfig(
      json['configValues'] as Map<String, dynamic>? ?? {},
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'configValues': configValues,
    };
  }

  dynamic getValue(String key) {
    return configValues[key];
  }

  void setValue(String key, dynamic value) {
    configValues[key] = value;
  }
}
