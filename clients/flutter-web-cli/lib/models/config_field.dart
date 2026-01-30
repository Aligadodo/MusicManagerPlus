class ConfigField {
  final String name;
  final String label;
  final String type;
  final dynamic defaultValue;
  final String description;
  final bool required;

  ConfigField({
    required this.name,
    required this.label,
    required this.type,
    required this.defaultValue,
    required this.description,
    required this.required,
  });

  factory ConfigField.fromJson(Map<String, dynamic> json) {
    return ConfigField(
      name: json['name'] as String,
      label: json['label'] as String,
      type: json['type'] as String,
      defaultValue: json['defaultValue'],
      description: json['description'] as String,
      required: json['required'] as bool,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'label': label,
      'type': type,
      'defaultValue': defaultValue,
      'description': description,
      'required': required,
    };
  }
}
