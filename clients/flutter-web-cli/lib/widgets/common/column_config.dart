import 'package:flutter/material.dart';

/// 列类型枚举
enum ColumnType {
  text,       // 文本类型
  number,     // 数字类型
  date,       // 日期类型
  boolean,    // 布尔类型
  enumeration, // 枚举类型（避免使用 enum 关键字）
  custom,     // 自定义类型
}

/// 列配置类
/// 用于定义表格列的显示和行为
class ColumnConfig {
  // 列标识（对应数据模型中的字段名）
  final String key;
  
  // 列标题
  final String title;
  
  // 列宽
  final double width;
  
  // 是否可排序
  final bool sortable;
  
  // 是否可筛选
  final bool filterable;
  
  // 是否默认显示
  final bool visible;
  
  // 是否可隐藏
  final bool hideable;
  
  // 自定义渲染器
  final Widget Function(dynamic value, dynamic row)? customRender;
  
  // 单元格对齐方式
  final Alignment alignment;
  
  // 列类型
  final ColumnType columnType;
  
  // 格式化函数
  final String Function(dynamic value)? formatter;
  
  // 列分组
  final String? group;

  ColumnConfig({
    required this.key,
    required this.title,
    this.width = 120,
    this.sortable = false,
    this.filterable = false,
    this.visible = true,
    this.hideable = true,
    this.customRender,
    this.alignment = Alignment.centerLeft,
    this.columnType = ColumnType.text,
    this.formatter,
    this.group,
  });

  ColumnConfig copyWith({
    String? key,
    String? title,
    double? width,
    bool? sortable,
    bool? filterable,
    bool? visible,
    bool? hideable,
    Widget Function(dynamic value, dynamic row)? customRender,
    Alignment? alignment,
    ColumnType? columnType,
    String Function(dynamic value)? formatter,
    String? group,
  }) {
    return ColumnConfig(
      key: key ?? this.key,
      title: title ?? this.title,
      width: width ?? this.width,
      sortable: sortable ?? this.sortable,
      filterable: filterable ?? this.filterable,
      visible: visible ?? this.visible,
      hideable: hideable ?? this.hideable,
      customRender: customRender ?? this.customRender,
      alignment: alignment ?? this.alignment,
      columnType: columnType ?? this.columnType,
      formatter: formatter ?? this.formatter,
      group: group ?? this.group,
    );
  }
}

/// 扫描阶段列配置
class ScanColumnConfigs {
  static List<ColumnConfig> get defaultColumns => [
    ColumnConfig(
      key: 'originalName',
      title: '文件名',
      width: 200,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'originalPath',
      title: '文件路径',
      width: 300,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'fileSize',
      title: '文件大小',
      width: 100,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: true,
      columnType: ColumnType.number,
      formatter: (value) => _formatFileSize(value),
    ),
    ColumnConfig(
      key: 'fileType',
      title: '文件类型',
      width: 80,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: true,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'lastModified',
      title: '修改时间',
      width: 150,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: true,
      columnType: ColumnType.date,
      formatter: (value) => _formatTimestamp(value),
    ),
    ColumnConfig(
      key: 'metadata',
      title: '元数据',
      width: 200,
      sortable: false,
      filterable: false,
      visible: false,
      hideable: true,
      columnType: ColumnType.custom,
      customRender: (value, row) => _renderMetadata(value),
    ),
  ];

  static String _formatFileSize(dynamic value) {
    if (value == null) return '-';
    int bytes = value is int ? value : int.tryParse(value.toString()) ?? 0;
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(2)} KB';
    if (bytes < 1024 * 1024 * 1024) return '${(bytes / (1024 * 1024)).toStringAsFixed(2)} MB';
    return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(2)} GB';
  }

  static String _formatTimestamp(dynamic value) {
    if (value == null) return '-';
    int timestamp = value is int ? value : int.tryParse(value.toString()) ?? 0;
    return DateTime.fromMillisecondsSinceEpoch(timestamp).toString().substring(0, 19);
  }

  static Widget _renderMetadata(dynamic value) {
    if (value == null) return const Text('-');
    Map<String, dynamic> metadata = value as Map<String, dynamic>;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (metadata['artist'] != null) Text('艺术家: ${metadata['artist']}', style: const TextStyle(fontSize: 12)),
        if (metadata['title'] != null) Text('标题: ${metadata['title']}', style: const TextStyle(fontSize: 12)),
        if (metadata['album'] != null) Text('专辑: ${metadata['album']}', style: const TextStyle(fontSize: 12)),
        if (metadata['duration'] != null) Text('时长: ${metadata['duration']}秒', style: const TextStyle(fontSize: 12)),
      ],
    );
  }
}

/// 预览阶段列配置
class PreviewColumnConfigs {
  static List<ColumnConfig> get defaultColumns => [
    ColumnConfig(
      key: 'originalName',
      title: '原文件名',
      width: 180,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'newName',
      title: '新文件名',
      width: 180,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.text,
      customRender: (value, row) => _renderNewName(value, row),
    ),
    ColumnConfig(
      key: 'originalPath',
      title: '原路径',
      width: 250,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: true,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'newPath',
      title: '新路径',
      width: 250,
      sortable: true,
      filterable: true,
      visible: false,
      hideable: true,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'operationType',
      title: '操作类型',
      width: 100,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.enumeration,
      customRender: (value, row) => _renderOperationType(value),
    ),
    ColumnConfig(
      key: 'status',
      title: '状态',
      width: 100,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.enumeration,
      customRender: (value, row) => _renderStatus(value),
    ),
    ColumnConfig(
      key: 'changed',
      title: '是否变更',
      width: 80,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: true,
      columnType: ColumnType.boolean,
      customRender: (value, row) => _renderChanged(value),
    ),
    ColumnConfig(
      key: 'reason',
      title: '变更原因',
      width: 200,
      sortable: false,
      filterable: false,
      visible: false,
      hideable: true,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'extraParams',
      title: '额外参数',
      width: 150,
      sortable: false,
      filterable: false,
      visible: false,
      hideable: true,
      columnType: ColumnType.custom,
      customRender: (value, row) => _renderExtraParams(value),
    ),
    ColumnConfig(
      key: 'analyzeTime',
      title: '分析时间',
      width: 150,
      sortable: true,
      filterable: true,
      visible: false,
      hideable: true,
      columnType: ColumnType.date,
      formatter: (value) => _formatTimestamp(value),
    ),
  ];

  static Widget _renderNewName(dynamic value, dynamic row) {
    bool changed = row['changed'] ?? false;
    return Text(
      value?.toString() ?? '-',
      style: TextStyle(
        color: changed ? Colors.green : Colors.grey,
        fontWeight: changed ? FontWeight.bold : FontWeight.normal,
      ),
    );
  }

  static Widget _renderOperationType(dynamic value) {
    String type = value?.toString() ?? 'UNKNOWN';
    Color color;
    String label;

    switch (type) {
      case 'RENAME':
        color = Colors.blue;
        label = '重命名';
        break;
      case 'MOVE':
        color = Colors.orange;
        label = '移动';
        break;
      case 'DELETE':
        color = Colors.red;
        label = '删除';
        break;
      case 'COPY':
        color = Colors.green;
        label = '复制';
        break;
      default:
        color = Colors.grey;
        label = type;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12),
      ),
    );
  }

  static Widget _renderStatus(dynamic value) {
    String status = value?.toString() ?? 'UNKNOWN';
    Color color;
    String label;

    switch (status) {
      case 'CHANGED':
        color = Colors.green;
        label = '已变更';
        break;
      case 'UNCHANGED':
        color = Colors.grey;
        label = '未变更';
        break;
      case 'PENDING':
        color = Colors.blue;
        label = '待处理';
        break;
      case 'ERROR':
        color = Colors.red;
        label = '错误';
        break;
      default:
        color = Colors.grey;
        label = status;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontSize: 12),
      ),
    );
  }

  static Widget _renderChanged(dynamic value) {
    bool changed = value ?? false;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: changed ? Colors.green.withOpacity(0.1) : Colors.grey.withOpacity(0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        changed ? '是' : '否',
        style: TextStyle(
          color: changed ? Colors.green : Colors.grey,
          fontSize: 12,
        ),
      ),
    );
  }

  static Widget _renderExtraParams(dynamic value) {
    if (value == null) return const Text('-');
    Map<String, dynamic> params = value as Map<String, dynamic>;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: params.entries
          .take(3)
          .map((e) => Text('${e.key}: ${e.value}', style: const TextStyle(fontSize: 12)))
          .toList(),
    );
  }

  static String _formatTimestamp(dynamic value) {
    if (value == null) return '-';
    int timestamp = value is int ? value : int.tryParse(value.toString()) ?? 0;
    return DateTime.fromMillisecondsSinceEpoch(timestamp).toString().substring(0, 19);
  }
}

/// 执行阶段列配置
class ExecutionColumnConfigs {
  static List<ColumnConfig> get defaultColumns => [
    ColumnConfig(
      key: 'originalName',
      title: '原文件名',
      width: 180,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'newName',
      title: '目标文件名',
      width: 180,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'operationType',
      title: '操作类型',
      width: 100,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.enumeration,
      customRender: (value, row) => PreviewColumnConfigs._renderOperationType(value),
    ),
    ColumnConfig(
      key: 'status',
      title: '执行状态',
      width: 100,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: false,
      columnType: ColumnType.enumeration,
      customRender: (value, row) => _renderExecutionStatus(value),
    ),
    ColumnConfig(
      key: 'failReason',
      title: '错误信息',
      width: 250,
      sortable: false,
      filterable: false,
      visible: false,
      hideable: true,
      columnType: ColumnType.text,
      customRender: (value, row) => _renderErrorMessage(value, row),
    ),
    ColumnConfig(
      key: 'executeTime',
      title: '执行时间',
      width: 150,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: true,
      columnType: ColumnType.date,
      formatter: (value) => _formatTimestamp(value),
    ),
    ColumnConfig(
      key: 'duration',
      title: '耗时',
      width: 80,
      sortable: true,
      filterable: true,
      visible: true,
      hideable: true,
      columnType: ColumnType.number,
      formatter: (value) => _formatDuration(value),
    ),
    ColumnConfig(
      key: 'retryCount',
      title: '重试次数',
      width: 80,
      sortable: true,
      filterable: true,
      visible: false,
      hideable: true,
      columnType: ColumnType.number,
    ),
    ColumnConfig(
      key: 'originalPath',
      title: '原路径',
      width: 250,
      sortable: true,
      filterable: true,
      visible: false,
      hideable: true,
      columnType: ColumnType.text,
    ),
    ColumnConfig(
      key: 'newPath',
      title: '目标路径',
      width: 250,
      sortable: true,
      filterable: true,
      visible: false,
      hideable: true,
      columnType: ColumnType.text,
    ),
  ];

  static Widget _renderExecutionStatus(dynamic value) {
    String status = value?.toString() ?? 'UNKNOWN';
    Color color;
    String label;

    switch (status) {
      case 'SUCCESS':
        color = Colors.green;
        label = '成功';
        break;
      case 'FAILED':
        color = Colors.red;
        label = '失败';
        break;
      case 'SKIPPED':
        color = Colors.orange;
        label = '跳过';
        break;
      case 'PENDING':
        color = Colors.blue;
        label = '待执行';
        break;
      default:
        color = Colors.grey;
        label = status;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12),
      ),
    );
  }

  static Widget _renderErrorMessage(dynamic value, dynamic row) {
    String? message = value?.toString();
    if (message == null || message.isEmpty) {
      return const Text('-', style: TextStyle(color: Colors.grey));
    }

    return Tooltip(
      message: message,
      child: Text(
        message,
        maxLines: 2,
        overflow: TextOverflow.ellipsis,
        style: const TextStyle(color: Colors.red, fontSize: 12),
      ),
    );
  }

  static String _formatTimestamp(dynamic value) {
    if (value == null) return '-';
    int timestamp = value is int ? value : int.tryParse(value.toString()) ?? 0;
    return DateTime.fromMillisecondsSinceEpoch(timestamp).toString().substring(0, 19);
  }

  static String _formatDuration(dynamic value) {
    if (value == null) return '-';
    int ms = value is int ? value : int.tryParse(value.toString()) ?? 0;
    if (ms < 1000) return '${ms}ms';
    return '${(ms / 1000).toStringAsFixed(2)}s';
  }
}
