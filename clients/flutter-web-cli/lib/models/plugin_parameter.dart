class PluginParameter {
  final String name;
  final String label;
  final String description;
  final String type;
  final dynamic defaultValue;
  final bool required;
  final List<String>? options;
  final String? validationPattern;
  final int? minValue;
  final int? maxValue;

  PluginParameter({
    required this.name,
    required this.label,
    required this.description,
    required this.type,
    required this.defaultValue,
    required this.required,
    this.options,
    this.validationPattern,
    this.minValue,
    this.maxValue,
  });

  factory PluginParameter.fromJson(Map<String, dynamic> json) {
    return PluginParameter(
      name: json['name'] ?? '',
      label: json['label'] ?? '',
      description: json['description'] ?? '',
      type: json['type'] ?? '',
      defaultValue: json['defaultValue'],
      required: json['required'] ?? false,
      options: json['options'] != null ? List<String>.from(json['options']) : null,
      validationPattern: json['validationPattern'],
      minValue: json['minValue'],
      maxValue: json['maxValue'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'label': label,
      'description': description,
      'type': type,
      'defaultValue': defaultValue,
      'required': required,
      'options': options,
      'validationPattern': validationPattern,
      'minValue': minValue,
      'maxValue': maxValue,
    };
  }
}
