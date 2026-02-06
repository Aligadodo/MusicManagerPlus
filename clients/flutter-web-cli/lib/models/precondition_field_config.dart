import 'precondition.dart';

enum PreconditionFieldType {
  text,
  number,
  boolean,
  fileType,
  date,
}

enum PreconditionInputType {
  none,
  single,
  range,
  list,
  multiSelect,
}

class PreconditionOperatorConfig {
  final String code;
  final String name;
  final String description;
  final String valuePlaceholder;
  final PreconditionInputType inputType;
  final String? endValuePlaceholder;

  PreconditionOperatorConfig({
    required this.code,
    required this.name,
    required this.description,
    required this.valuePlaceholder,
    this.inputType = PreconditionInputType.single,
    this.endValuePlaceholder,
  });
}

class PreconditionFieldConfig {
  final String code;
  final String name;
  final String description;
  final List<PreconditionOperatorConfig> operators;
  final PreconditionFieldType type;

  PreconditionFieldConfig({
    required this.code,
    required this.name,
    required this.description,
    required this.operators,
    required this.type,
  });

  PreconditionOperatorConfig? getOperatorConfig(String operatorCode) {
    try {
      return operators.firstWhere((op) => op.code == operatorCode);
    } catch (e) {
      return null;
    }
  }

  PreconditionInputType getOperatorInputType(String operatorCode) {
    final operatorConfig = getOperatorConfig(operatorCode);
    return operatorConfig?.inputType ?? PreconditionInputType.single;
  }

  bool operatorRequiresValue(String operatorCode) {
    final operatorConfig = getOperatorConfig(operatorCode);
    return operatorConfig?.inputType != PreconditionInputType.none;
  }
}

class PreconditionFieldConfigs {
  static final List<PreconditionFieldConfig> fields = [
    PreconditionFieldConfig(
      code: 'file',
      name: '文件',
      description: '判断是否为文件',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是文件',
          description: '判断是否为文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是文件',
          description: '判断是否不是文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'directory',
      name: '文件夹',
      description: '判断是否为文件夹',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是文件夹',
          description: '判断是否为文件夹',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是文件夹',
          description: '判断是否不是文件夹',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isEmpty',
          name: '是空文件夹',
          description: '判断文件夹是否为空',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNotEmpty',
          name: '不是空文件夹',
          description: '判断文件夹是否不为空',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'hasSubdirectories',
          name: '有子文件夹',
          description: '判断文件夹是否包含子文件夹',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'hasNoSubdirectories',
          name: '没有子文件夹',
          description: '判断文件夹是否不包含子文件夹',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'depthGreaterThan',
          name: '深度大于',
          description: '判断文件夹深度是否大于指定值',
          valuePlaceholder: '请输入深度值',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'depthLessThan',
          name: '深度小于',
          description: '判断文件夹深度是否小于指定值',
          valuePlaceholder: '请输入深度值',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'fileCountGreaterThan',
          name: '文件数量大于',
          description: '判断文件夹内文件数量是否大于指定值',
          valuePlaceholder: '请输入文件数量',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'fileCountLessThan',
          name: '文件数量小于',
          description: '判断文件夹内文件数量是否小于指定值',
          valuePlaceholder: '请输入文件数量',
          inputType: PreconditionInputType.single,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'audioFile',
      name: '音频文件',
      description: '判断是否为音频文件',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是音频文件',
          description: '判断是否为音频文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是音频文件',
          description: '判断是否不是音频文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'formatIn',
          name: '格式在列表中',
          description: '判断音频格式是否在指定列表中',
          valuePlaceholder: '请输入格式列表（如mp3,wav,flac）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'formatNotIn',
          name: '格式不在列表中',
          description: '判断音频格式是否不在指定列表中',
          valuePlaceholder: '请输入格式列表（如mp3,wav,flac）',
          inputType: PreconditionInputType.list,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'videoFile',
      name: '视频文件',
      description: '判断是否为视频文件',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是视频文件',
          description: '判断是否为视频文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是视频文件',
          description: '判断是否不是视频文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'formatIn',
          name: '格式在列表中',
          description: '判断视频格式是否在指定列表中',
          valuePlaceholder: '请输入格式列表（如mp4,mkv,avi）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'formatNotIn',
          name: '格式不在列表中',
          description: '判断视频格式是否不在指定列表中',
          valuePlaceholder: '请输入格式列表（如mp4,mkv,avi）',
          inputType: PreconditionInputType.list,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'imageFile',
      name: '图片文件',
      description: '判断是否为图片文件',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是图片文件',
          description: '判断是否为图片文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是图片文件',
          description: '判断是否不是图片文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'formatIn',
          name: '格式在列表中',
          description: '判断图片格式是否在指定列表中',
          valuePlaceholder: '请输入格式列表（如jpg,png,gif）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'formatNotIn',
          name: '格式不在列表中',
          description: '判断图片格式是否不在指定列表中',
          valuePlaceholder: '请输入格式列表（如jpg,png,gif）',
          inputType: PreconditionInputType.list,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'textFile',
      name: '文本文件',
      description: '判断是否为文本文件',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是文本文件',
          description: '判断是否为文本文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是文本文件',
          description: '判断是否不是文本文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'formatIn',
          name: '格式在列表中',
          description: '判断文本格式是否在指定列表中',
          valuePlaceholder: '请输入格式列表（如txt,csv,md）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'formatNotIn',
          name: '格式不在列表中',
          description: '判断文本格式是否不在指定列表中',
          valuePlaceholder: '请输入格式列表（如txt,csv,md）',
          inputType: PreconditionInputType.list,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'documentFile',
      name: '文档文件',
      description: '判断是否为文档文件',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是文档文件',
          description: '判断是否为文档文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是文档文件',
          description: '判断是否不是文档文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'formatIn',
          name: '格式在列表中',
          description: '判断文档格式是否在指定列表中',
          valuePlaceholder: '请输入格式列表（如pdf,docx,xlsx）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'formatNotIn',
          name: '格式不在列表中',
          description: '判断文档格式是否不在指定列表中',
          valuePlaceholder: '请输入格式列表（如pdf,docx,xlsx）',
          inputType: PreconditionInputType.list,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'archiveFile',
      name: '压缩文件',
      description: '判断是否为压缩文件',
      type: PreconditionFieldType.fileType,
      operators: [
        PreconditionOperatorConfig(
          code: 'is',
          name: '是压缩文件',
          description: '判断是否为压缩文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'isNot',
          name: '不是压缩文件',
          description: '判断是否不是压缩文件',
          valuePlaceholder: '',
          inputType: PreconditionInputType.none,
        ),
        PreconditionOperatorConfig(
          code: 'formatIn',
          name: '格式在列表中',
          description: '判断压缩格式是否在指定列表中',
          valuePlaceholder: '请输入格式列表（如zip,rar,7z）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'formatNotIn',
          name: '格式不在列表中',
          description: '判断压缩格式是否不在指定列表中',
          valuePlaceholder: '请输入格式列表（如zip,rar,7z）',
          inputType: PreconditionInputType.list,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'name',
      name: '文件名',
      description: '文件或文件夹的名称',
      type: PreconditionFieldType.text,
      operators: [
        PreconditionOperatorConfig(
          code: 'contains',
          name: '包含',
          description: '文件名包含指定文本',
          valuePlaceholder: '请输入要包含的文本',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'equals',
          name: '等于',
          description: '文件名等于指定文本',
          valuePlaceholder: '请输入文件名',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'startsWith',
          name: '以...开头',
          description: '文件名以指定文本开头',
          valuePlaceholder: '请输入开头的文本',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'endsWith',
          name: '以...结尾',
          description: '文件名以指定文本结尾',
          valuePlaceholder: '请输入结尾的文本',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'notContains',
          name: '不包含',
          description: '文件名不包含指定文本',
          valuePlaceholder: '请输入要排除的文本',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'regex',
          name: '匹配正则',
          description: '文件名匹配正则表达式',
          valuePlaceholder: '请输入正则表达式',
          inputType: PreconditionInputType.single,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'extension',
      name: '扩展名',
      description: '文件的扩展名（如.mp3, .wav）',
      type: PreconditionFieldType.text,
      operators: [
        PreconditionOperatorConfig(
          code: 'equals',
          name: '等于',
          description: '扩展名等于指定值',
          valuePlaceholder: '请输入扩展名（如.mp3）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'in',
          name: '在列表中',
          description: '扩展名在指定列表中',
          valuePlaceholder: '请输入扩展名列表（如.mp3,.wav）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'notIn',
          name: '不在列表中',
          description: '扩展名不在指定列表中',
          valuePlaceholder: '请输入扩展名列表（如.mp3,.wav）',
          inputType: PreconditionInputType.list,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'size',
      name: '文件大小',
      description: '文件的大小（字节）',
      type: PreconditionFieldType.number,
      operators: [
        PreconditionOperatorConfig(
          code: 'greaterThan',
          name: '大于',
          description: '文件大小大于指定值',
          valuePlaceholder: '请输入大小（字节）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'lessThan',
          name: '小于',
          description: '文件大小小于指定值',
          valuePlaceholder: '请输入大小（字节）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'equals',
          name: '等于',
          description: '文件大小等于指定值',
          valuePlaceholder: '请输入大小（字节）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'between',
          name: '介于',
          description: '文件大小介于两个值之间',
          valuePlaceholder: '请输入最小值（字节）',
          endValuePlaceholder: '请输入最大值（字节）',
          inputType: PreconditionInputType.range,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'modified',
      name: '修改时间',
      description: '文件最后修改时间',
      type: PreconditionFieldType.date,
      operators: [
        PreconditionOperatorConfig(
          code: 'greaterThan',
          name: '晚于',
          description: '修改时间晚于指定时间',
          valuePlaceholder: '请输入时间戳',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'lessThan',
          name: '早于',
          description: '修改时间早于指定时间',
          valuePlaceholder: '请输入时间戳',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'between',
          name: '介于',
          description: '修改时间介于两个时间之间',
          valuePlaceholder: '请输入开始时间戳',
          endValuePlaceholder: '请输入结束时间戳',
          inputType: PreconditionInputType.range,
        ),
        PreconditionOperatorConfig(
          code: 'lastDays',
          name: '最近N天',
          description: '修改时间在最近N天内',
          valuePlaceholder: '请输入天数',
          inputType: PreconditionInputType.single,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'path',
      name: '文件路径',
      description: '文件的完整路径',
      type: PreconditionFieldType.text,
      operators: [
        PreconditionOperatorConfig(
          code: 'contains',
          name: '包含',
          description: '路径包含指定文本',
          valuePlaceholder: '请输入路径片段',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'startsWith',
          name: '以...开头',
          description: '路径以指定文本开头',
          valuePlaceholder: '请输入开头的路径',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'endsWith',
          name: '以...结尾',
          description: '路径以指定文本结尾',
          valuePlaceholder: '请输入结尾的路径',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'regex',
          name: '匹配正则',
          description: '路径匹配正则表达式',
          valuePlaceholder: '请输入正则表达式',
          inputType: PreconditionInputType.single,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'duration',
      name: '音频时长',
      description: '音频文件的时长（秒）',
      type: PreconditionFieldType.number,
      operators: [
        PreconditionOperatorConfig(
          code: 'greaterThan',
          name: '大于',
          description: '时长大于指定值（秒）',
          valuePlaceholder: '请输入时长（秒）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'lessThan',
          name: '小于',
          description: '时长小于指定值（秒）',
          valuePlaceholder: '请输入时长（秒）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'between',
          name: '介于',
          description: '时长介于两个值之间（秒）',
          valuePlaceholder: '请输入最小时长（秒）',
          endValuePlaceholder: '请输入最大时长（秒）',
          inputType: PreconditionInputType.range,
        ),
      ],
    ),
    PreconditionFieldConfig(
      code: 'bitrate',
      name: '比特率',
      description: '音频文件的比特率（kbps）',
      type: PreconditionFieldType.number,
      operators: [
        PreconditionOperatorConfig(
          code: 'greaterThan',
          name: '大于',
          description: '比特率大于指定值（kbps）',
          valuePlaceholder: '请输入比特率（kbps）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'lessThan',
          name: '小于',
          description: '比特率小于指定值（kbps）',
          valuePlaceholder: '请输入比特率（kbps）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'equals',
          name: '等于',
          description: '比特率等于指定值（kbps）',
          valuePlaceholder: '请输入比特率（kbps）',
          inputType: PreconditionInputType.single,
        ),
        PreconditionOperatorConfig(
          code: 'in',
          name: '在列表中',
          description: '比特率在指定列表中（kbps）',
          valuePlaceholder: '请输入比特率列表（如128,192,320）',
          inputType: PreconditionInputType.list,
        ),
        PreconditionOperatorConfig(
          code: 'notIn',
          name: '不在列表中',
          description: '比特率不在指定列表中（kbps）',
          valuePlaceholder: '请输入比特率列表（如128,192,320）',
          inputType: PreconditionInputType.list,
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

    if (operatorConfig.inputType == PreconditionInputType.none) {
      return '${fieldConfig.name}${operatorConfig.name}';
    }

    if (operatorConfig.inputType == PreconditionInputType.range) {
      final values = parseRangeValue(condition.value);
      return '${fieldConfig.name}${operatorConfig.name} ${values['start']} - ${values['end']}';
    }

    if (operatorConfig.inputType == PreconditionInputType.list) {
      return '${fieldConfig.name}${operatorConfig.name} [${condition.value}]';
    }

    return '${fieldConfig.name}${operatorConfig.name} ${condition.value}';
  }

  static Map<String, String> parseRangeValue(String value) {
    final parts = value.split('|');
    if (parts.length == 2) {
      return {'start': parts[0], 'end': parts[1]};
    }
    return {'start': value, 'end': value};
  }

  static String formatRangeValue(String start, String end) {
    return '$start|$end';
  }
}
