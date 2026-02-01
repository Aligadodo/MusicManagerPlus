class ConfigField {
  final String name;
  final String label;
  final String type;
  final dynamic defaultValue;
  final String description;
  final bool required;
  
  // 条件参数支持
  final String? dependsOn;
  final String? dependsValue;
  final List<String>? options;
  final Map<String, dynamic>? subFields;
  
  // 模块化配置支持
  final bool isModule;
  final String? moduleType;

  ConfigField({
    required this.name,
    required this.label,
    required this.type,
    required this.defaultValue,
    required this.description,
    required this.required,
    this.dependsOn,
    this.dependsValue,
    this.options,
    this.subFields,
    this.isModule = false,
    this.moduleType,
  });

  factory ConfigField.fromJson(Map<String, dynamic> json) {
    return ConfigField(
      name: json['name'] as String,
      label: json['label'] as String,
      type: json['type'] as String,
      defaultValue: json['defaultValue'],
      description: json['description'] as String? ?? '',
      required: json['required'] as bool? ?? false,
      dependsOn: json['dependsOn'] as String?,
      dependsValue: json['dependsValue'] as String?,
      options: json['options'] != null 
          ? List<String>.from(json['options'] as List) 
          : null,
      subFields: json['subFields'] as Map<String, dynamic>?,
      isModule: json['isModule'] as bool? ?? json['module'] as bool? ?? false,
      moduleType: json['moduleType'] as String?,
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
      'dependsOn': dependsOn,
      'dependsValue': dependsValue,
      'options': options,
      'subFields': subFields,
      'isModule': isModule,
      'moduleType': moduleType,
    };
  }
}
