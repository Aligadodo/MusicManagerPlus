import 'precondition.dart';

class PreconditionFieldConfig {
  final String code;
  final String name;
  final String description;
  final List<PreconditionOperatorConfig> operators;
  final bool requiresValue;
  final PreconditionFieldType type;

  PreconditionFieldConfig({
    required this.code,
    required this.name,
    required this.description,
    required this.operators,
    required this.requiresValue,
    required this.type,
  });
}

enum PreconditionFieldType {
  text,
  number,
  boolean,
  fileType,
}

class PreconditionOperatorConfig {
  final String code;
  final String name;
  final String description;
  final String valuePlaceholder;

  PreconditionOperatorConfig({
    required this.code,
    required this.name,
    required this.description,
    required this.valuePlaceholder,
  });
}

class PreconditionFieldConfigs {
  static final List<PreconditionFieldConfig> fields = [
    PreconditionFieldConfig(
      code: 'file',
      name: '文件',
      description: '判断是否为文件',
      type: PreconditionFieldType.fileType,
      requiresValue: false,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是文件',
          description: '判断是否为文件',
          valuePlaceholder: '',
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'directory',
      name: '文件夹',
      description: '判断是否为文件夹',
      type: PreconditionFieldType.fileType,
      requiresValue: false,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是文件夹',
          description: '判断是否为文件夹',
          valuePlaceholder: '',
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'name',
      name: '文件名',
      description: '文件或文件夹的名称',
      type: PreconditionFieldType.text,
      requiresValue: true,
      operators: [
        PreconditionOperatorConfig(
          code: 'contains',
          name: '包含',
          description: '文件名包含指定文本',
          valuePlaceholder: '请输入要包含的文本',
        ),
        PreconditionOperatorConfig(
          code: 'equals',
          name: '等于',
          description: '文件名等于指定文本',
          valuePlaceholder: '请输入文件名',
        ),
        PreconditionOperatorConfig(
          code: 'startsWith',
          name: '以...开头',
          description: '文件名以指定文本开头',
          valuePlaceholder: '请输入开头的文本',
        ),
        PreconditionOperatorConfig(
          code: 'endsWith',
          name: '以...结尾',
          description: '文件名以指定文本结尾',
          valuePlaceholder: '请输入结尾的文本',
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'extension',
      name: '扩展名',
      description: '文件的扩展名（如.mp3, .wav）',
      type: PreconditionFieldType.text,
      requiresValue: true,
      operators: [
        PreconditionOperatorConfig(
          code: 'equals',
          name: '等于',
          description: '扩展名等于指定值',
          valuePlaceholder: '请输入扩展名（如.mp3）',
        ),
        PreconditionOperatorConfig(
          code: 'in',
          name: '在列表中',
          description: '扩展名在指定列表中',
          valuePlaceholder: '请输入扩展名列表（如.mp3,.wav）',
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'size',
      name: '文件大小',
      description: '文件的大小（字节）',
      type: PreconditionFieldType.number,
      requiresValue: true,
      operators: [
        PreconditionOperatorConfig(
          code: 'greaterThan',
          name: '大于',
          description: '文件大小大于指定值',
          valuePlaceholder: '请输入大小（字节）',
        ),
        PreconditionOperatorConfig(
          code: 'lessThan',
          name: '小于',
          description: '文件大小小于指定值',
          valuePlaceholder: '请输入大小（字节）',
        ),
        PreconditionOperatorConfig(
          code: 'greaterThanOrEqual',
          name: '大于等于',
          description: '文件大小大于等于指定值',
          valuePlaceholder: '请输入大小（字节）',
        ),
        PreconditionOperatorConfig(
          code: 'lessThanOrEqual',
          name: '小于等于',
          description: '文件大小小于等于指定值',
          valuePlaceholder: '请输入大小（字节）',
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'modified',
      name: '修改时间',
      description: '文件最后修改时间',
      type: PreconditionFieldType.number,
      requiresValue: true,
      operators: [
        PreconditionOperatorConfig(
          code: 'greaterThan',
          name: '晚于',
          description: '修改时间晚于指定时间',
          valuePlaceholder: '请输入时间戳',
        ),
        PreconditionOperatorConfig(
          code: 'lessThan',
          name: '早于',
          description: '修改时间早于指定时间',
          valuePlaceholder: '请输入时间戳',
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'path',
      name: '文件路径',
      description: '文件的完整路径',
      type: PreconditionFieldType.text,
      requiresValue: true,
      operators: [
        PreconditionOperatorConfig(
          code: 'contains',
          name: '包含',
          description: '路径包含指定文本',
          valuePlaceholder: '请输入路径片段',
        ),
        PreconditionOperatorConfig(
          code: 'startsWith',
          name: '以...开头',
          description: '路径以指定文本开头',
          valuePlaceholder: '请输入开头的路径',
        ),
      ],
    ),
  ];

  static PreconditionFieldConfig? getFieldConfig(String fieldCode) {
    try {
      return fields.firstWhere((field) => field.code == fieldCode);
    } catch (e) {
      return null;
    }
  }

  static String getConditionDescription(Precondition condition) {
    PreconditionFieldConfig? fieldConfig = getFieldConfig(condition.field);
    if (fieldConfig == null) return '';

    PreconditionOperatorConfig? operatorConfig;
    try {
      operatorConfig = fieldConfig.operators.firstWhere((op) => op.code == condition.operator);
    } catch (e) {
      return '';
    }

    if (!fieldConfig.requiresValue) {
      return '${fieldConfig.name}${operatorConfig.name}';
    }

    return '${fieldConfig.name}${operatorConfig.name} ${condition.value}';
  }
}
