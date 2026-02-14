import 'enum_option.dart';
import 'auto_fill_config.dart';

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
  final List<EnumOption>? enumOptions;
  final Map<String, dynamic>? subFields;
  
  // 模块化配置支持
  final bool isModule;
  final String? moduleType;
  
  // 参数关系定义
  final String? exclusiveGroup;
  final List<Map<String, dynamic>>? blockConditions;
  final AutoFillConfig? autoFillConfig;
  final List<ConfigField>? childFields;

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
    this.enumOptions,
    this.subFields,
    this.isModule = false,
    this.moduleType,
    this.exclusiveGroup,
    this.blockConditions,
    this.autoFillConfig,
    this.childFields,
  });

  factory ConfigField.fromJson(Map<String, dynamic> json) {
    List<String>? parsedOptions;
    if (json['options'] != null) {
      try {
        final optionsList = json['options'] as List<dynamic>?;
        if (optionsList != null) {
          parsedOptions = optionsList
              .where((option) => option != null && option is String)
              .cast<String>()
              .toList();
        }
      } catch (e) {
        print('Failed to parse options: ${json['options']}, error: $e');
      }
    }

    List<EnumOption>? parsedEnumOptions;
    if (json['enumOptions'] != null) {
      try {
        final enumOptionsList = json['enumOptions'] as List<dynamic>?;
        if (enumOptionsList != null) {
          parsedEnumOptions = enumOptionsList
              .where((option) => option != null && option is Map<String, dynamic>)
              .map((option) => EnumOption.fromJson(option as Map<String, dynamic>))
              .toList();
        }
      } catch (e) {
        print('Failed to parse enumOptions: ${json['enumOptions']}, error: $e');
      }
    }

    AutoFillConfig? parsedAutoFillConfig;
    if (json['autoFillConfig'] != null) {
      try {
        parsedAutoFillConfig = AutoFillConfig.fromJson(
            json['autoFillConfig'] as Map<String, dynamic>);
      } catch (e) {
        print('Failed to parse autoFillConfig: ${json['autoFillConfig']}, error: $e');
      }
    }

    List<ConfigField>? parsedChildFields;
    if (json['childFields'] != null) {
      try {
        final childFieldsList = json['childFields'] as List<dynamic>?;
        if (childFieldsList != null) {
          parsedChildFields = childFieldsList
              .where((field) => field != null && field is Map<String, dynamic>)
              .map((field) => ConfigField.fromJson(field as Map<String, dynamic>))
              .toList();
        }
      } catch (e) {
        print('Failed to parse childFields: ${json['childFields']}, error: $e');
      }
    }

    List<Map<String, dynamic>>? parsedBlockConditions;
    if (json['blockConditions'] != null) {
      try {
        final blockConditionsList = json['blockConditions'] as List<dynamic>?;
        if (blockConditionsList != null) {
          parsedBlockConditions = blockConditionsList
              .where((condition) => condition != null && condition is Map<String, dynamic>)
              .map((condition) => condition as Map<String, dynamic>)
              .toList();
        }
      } catch (e) {
        print('Failed to parse blockConditions: ${json['blockConditions']}, error: $e');
      }
    }

    return ConfigField(
      name: json['name'] as String? ?? 'unknown',
      label: json['label'] as String? ?? 'Unknown Field',
      type: json['type'] as String? ?? 'string',
      defaultValue: json['defaultValue'],
      description: json['description'] as String? ?? '',
      required: json['required'] as bool? ?? false,
      dependsOn: json['dependsOn'] as String?,
      dependsValue: json['dependsValue'] as String?,
      options: parsedOptions,
      enumOptions: parsedEnumOptions,
      subFields: json['subFields'] as Map<String, dynamic>?,
      isModule: json['isModule'] as bool? ?? json['module'] as bool? ?? false,
      moduleType: json['moduleType'] as String?,
      exclusiveGroup: json['exclusiveGroup'] as String?,
      blockConditions: parsedBlockConditions,
      autoFillConfig: parsedAutoFillConfig,
      childFields: parsedChildFields,
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
      'enumOptions': enumOptions?.map((e) => e.toJson()).toList(),
      'subFields': subFields,
      'isModule': isModule,
      'moduleType': moduleType,
      'exclusiveGroup': exclusiveGroup,
      'blockConditions': blockConditions,
      'autoFillConfig': autoFillConfig?.toJson(),
      'childFields': childFields?.map((e) => e.toJson()).toList(),
    };
  }

  // 创建一个新的ConfigField实例
  ConfigField copyWith() {
    return ConfigField(
      name: name,
      label: label,
      type: type,
      defaultValue: defaultValue,
      description: description,
      required: required,
      dependsOn: dependsOn,
      dependsValue: dependsValue,
      options: options,
      enumOptions: enumOptions,
      subFields: subFields,
      isModule: isModule,
      moduleType: moduleType,
      exclusiveGroup: exclusiveGroup,
      blockConditions: blockConditions,
      autoFillConfig: autoFillConfig,
      childFields: childFields,
    );
  }
}
