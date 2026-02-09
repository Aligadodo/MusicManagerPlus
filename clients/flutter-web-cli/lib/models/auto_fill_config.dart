class AutoFillConfig {
  final String? triggerParam;
  final String? triggerValue;
  final String? fillType;
  final String? fillValue;
  final String? detectPattern;

  AutoFillConfig({
    this.triggerParam,
    this.triggerValue,
    this.fillType,
    this.fillValue,
    this.detectPattern,
  });

  factory AutoFillConfig.fromJson(Map<String, dynamic> json) {
    return AutoFillConfig(
      triggerParam: json['triggerParam'] as String?,
      triggerValue: json['triggerValue'] as String?,
      fillType: json['fillType'] as String?,
      fillValue: json['fillValue'] as String?,
      detectPattern: json['detectPattern'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'triggerParam': triggerParam,
      'triggerValue': triggerValue,
      'fillType': fillType,
      'fillValue': fillValue,
      'detectPattern': detectPattern,
    };
  }
}