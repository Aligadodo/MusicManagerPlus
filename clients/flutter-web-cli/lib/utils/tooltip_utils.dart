import 'package:flutter/material.dart';

/// 提示信息工具类
class TooltipUtils {
  /// 创建带提示的文本
  static Widget textWithTooltip({
    required String text,
    required String tooltip,
    TextStyle? style,
  }) {
    return Tooltip(
      message: tooltip,
      child: Text(text, style: style),
    );
  }

  /// 创建带提示的图标
  static Widget iconWithTooltip({
    required IconData icon,
    required String tooltip,
    Color? color,
    double? size,
  }) {
    return Tooltip(
      message: tooltip,
      child: Icon(icon, color: color, size: size),
    );
  }

  /// 创建带提示的按钮
  static Widget buttonWithTooltip({
    required Widget child,
    required String tooltip,
    required VoidCallback onPressed,
    ButtonStyle? style,
  }) {
    return Tooltip(
      message: tooltip,
      child: ElevatedButton(
        onPressed: onPressed,
        style: style,
        child: child,
      ),
    );
  }

  /// 创建带提示的复选框
  static Widget checkboxWithTooltip({
    required bool value,
    required ValueChanged<bool?> onChanged,
    required String label,
    required String tooltip,
  }) {
    return Tooltip(
      message: tooltip,
      child: Row(
        children: [
          Checkbox(
            value: value,
            onChanged: onChanged,
          ),
          Text(label),
        ],
      ),
    );
  }

  /// 创建带提示的下拉菜单
  static Widget dropdownWithTooltip<T>({
    required T value,
    required List<DropdownMenuItem<T>> items,
    required ValueChanged<T?> onChanged,
    required String tooltip,
    String? hint,
  }) {
    return Tooltip(
      message: tooltip,
      child: DropdownButton<T>(
        value: value,
        items: items,
        onChanged: onChanged,
        hint: hint != null ? Text(hint) : null,
      ),
    );
  }

  /// 创建带提示的文本输入框
  static Widget textFieldWithTooltip({
    required TextEditingController controller,
    required String labelText,
    required String tooltip,
    String? hintText,
    TextInputType? keyboardType,
    bool obscureText = false,
  }) {
    return Tooltip(
      message: tooltip,
      child: TextField(
        controller: controller,
        decoration: InputDecoration(
          labelText: labelText,
          hintText: hintText,
          border: const OutlineInputBorder(),
        ),
        keyboardType: keyboardType,
        obscureText: obscureText,
      ),
    );
  }

  /// 创建带提示的容器
  static Widget containerWithTooltip({
    required Widget child,
    required String tooltip,
    Decoration? decoration,
    EdgeInsetsGeometry? padding,
    EdgeInsetsGeometry? margin,
  }) {
    return Tooltip(
      message: tooltip,
      child: Container(
        decoration: decoration,
        padding: padding,
        margin: margin,
        child: child,
      ),
    );
  }
}

/// 详细的参数使用说明
class ParameterDescriptions {
  // 预览执行页面参数说明
  static const Map<String, String> previewPage = {
    'search': '输入关键词搜索文件名称和路径',
    'statusFilter': '按执行状态筛选记录',
    'operationTypeFilter': '按操作类型筛选记录',
    'hideUnchanged': '仅显示有变更的文件',
    'autoRefresh': '自动刷新变更记录',
    'pageSize': '设置每页显示的记录数量',
    'refresh': '手动刷新变更记录',
    'analyze': '分析流水线配置对文件的影响',
    'execute': '执行流水线配置',
    'stop': '停止正在执行的任务',
  };

  // 任务编排页面参数说明
  static const Map<String, String> pipelinePage = {
    'addPlugin': '添加插件到流水线',
    'removePlugin': '从流水线中移除插件',
    'moveUp': '向上移动插件位置',
    'moveDown': '向下移动插件位置',
    'configurePlugin': '配置插件参数',
    'enablePlugin': '启用/禁用插件',
  };

  // 策略配置页面参数说明
  static const Map<String, String> strategyPage = {
    'strategyName': '策略名称，用于标识不同的策略配置',
    'strategyDescription': '策略描述，说明策略的用途和适用场景',
    'saveStrategy': '保存策略配置',
    'deleteStrategy': '删除策略配置',
    'applyStrategy': '应用策略配置到当前任务',
  };

  // 外观设置页面参数说明
  static const Map<String, String> appearancePage = {
    'theme': '选择应用主题',
    'fontSize': '设置字体大小',
    'language': '选择应用语言',
    'saveConfig': '保存外观配置',
    'applyPreset': '应用预设主题配置',
  };

  // 源目录管理页面参数说明
  static const Map<String, String> sourceDirPage = {
    'addDirectory': '添加源目录',
    'removeDirectory': '移除源目录',
    'scanDirectory': '扫描目录文件',
    'excludePattern': '设置排除文件的模式',
  };
}
