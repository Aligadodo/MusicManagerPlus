import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/config_provider.dart';

class UiUtils {
  // 获取主题颜色
  static Color getPrimaryColor(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return _parseColor(config.appearanceConfig['accentColor'] ?? '#2196F3');
  }

  // 获取背景颜色
  static Color getBackgroundColor(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return _parseColor(config.appearanceConfig['bgColor'] ?? '#FFFFFF');
  }

  // 获取面板背景颜色
  static Color getPanelBackgroundColor(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return _parseColor(config.appearanceConfig['panelBgColor'] ?? '#FFFFFF');
  }

  // 获取主文本颜色
  static Color getTextPrimaryColor(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return _parseColor(config.appearanceConfig['textPrimaryColor'] ?? '#000000');
  }

  // 获取次要文本颜色
  static Color getTextSecondaryColor(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return _parseColor(config.appearanceConfig['textSecondaryColor'] ?? '#666666');
  }

  // 获取边框颜色
  static Color getBorderColor(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return _parseColor(config.appearanceConfig['borderColor'] ?? '#E0E0E0');
  }

  // 获取圆角半径
  static double getCornerRadius(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return (config.appearanceConfig['cornerRadius'] ?? 8.0).toDouble();
  }

  // 获取边框宽度
  static double getBorderWidth(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return (config.appearanceConfig['borderWidth'] ?? 1.0).toDouble();
  }

  // 获取大按钮尺寸
  static double getButtonLargeSize(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return (config.appearanceConfig['buttonLargeSize'] ?? 48.0).toDouble();
  }

  // 获取小按钮尺寸
  static double getButtonSmallSize(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return (config.appearanceConfig['buttonSmallSize'] ?? 32.0).toDouble();
  }

  // 获取玻璃效果透明度
  static double getGlassOpacity(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    return (config.appearanceConfig['glassOpacity'] ?? 0.9).toDouble();
  }

  // 解析颜色字符串
  static Color _parseColor(String colorString) {
    try {
      return Color(int.parse(colorString.replaceAll('#', '0xFF')));
    } catch (e) {
      return Colors.blue;
    }
  }

  // 获取按钮样式
  static ButtonStyle getPrimaryButtonStyle(BuildContext context, WidgetRef ref) {
    return ElevatedButton.styleFrom(
      backgroundColor: getPrimaryColor(context, ref),
      foregroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(getCornerRadius(context, ref)),
      ),
    );
  }

  // 获取次要按钮样式
  static ButtonStyle getSecondaryButtonStyle(BuildContext context, WidgetRef ref) {
    return ElevatedButton.styleFrom(
      backgroundColor: getTextSecondaryColor(context, ref),
      foregroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(getCornerRadius(context, ref)),
      ),
    );
  }

  // 获取危险按钮样式
  static ButtonStyle getDangerButtonStyle(BuildContext context, WidgetRef ref) {
    return ElevatedButton.styleFrom(
      backgroundColor: Colors.red,
      foregroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(getCornerRadius(context, ref)),
      ),
    );
  }

  // 获取成功按钮样式
  static ButtonStyle getSuccessButtonStyle(BuildContext context, WidgetRef ref) {
    return ElevatedButton.styleFrom(
      backgroundColor: Colors.green,
      foregroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(getCornerRadius(context, ref)),
      ),
    );
  }

  // 获取面板装饰
  static BoxDecoration getPanelDecoration(BuildContext context, WidgetRef ref) {
    return BoxDecoration(
      color: getPanelBackgroundColor(context, ref).withOpacity(getGlassOpacity(context, ref)),
      borderRadius: BorderRadius.circular(getCornerRadius(context, ref)),
      border: Border.all(
        color: getBorderColor(context, ref),
        width: getBorderWidth(context, ref),
      ),
      boxShadow: [
        BoxShadow(
          color: Colors.black.withOpacity(0.05),
          blurRadius: 4,
          offset: const Offset(0, 2),
        ),
      ],
    );
  }

  // 获取卡片装饰
  static BoxDecoration getCardDecoration(BuildContext context, WidgetRef ref) {
    return BoxDecoration(
      color: getPanelBackgroundColor(context, ref),
      borderRadius: BorderRadius.circular(getCornerRadius(context, ref)),
      border: Border.all(
        color: getBorderColor(context, ref),
        width: getBorderWidth(context, ref),
      ),
    );
  }

  // 获取列表项装饰
  static BoxDecoration getListItemDecoration(BuildContext context, WidgetRef ref, bool isSelected) {
    return BoxDecoration(
      color: isSelected ? getPrimaryColor(context, ref).withOpacity(0.1) : Colors.transparent,
      borderRadius: BorderRadius.circular(getCornerRadius(context, ref)),
      border: Border.all(
        color: isSelected ? getPrimaryColor(context, ref) : getBorderColor(context, ref),
        width: getBorderWidth(context, ref),
      ),
    );
  }

  // 获取文本样式
  static TextStyle getTextStyle(BuildContext context, WidgetRef ref, {
    double fontSize = 14,
    FontWeight fontWeight = FontWeight.normal,
    Color? color,
  }) {
    return TextStyle(
      fontSize: fontSize,
      fontWeight: fontWeight,
      color: color ?? getTextPrimaryColor(context, ref),
    );
  }

  // 获取标题文本样式
  static TextStyle getTitleTextStyle(BuildContext context, WidgetRef ref) {
    return TextStyle(
      fontSize: 18,
      fontWeight: FontWeight.bold,
      color: getTextPrimaryColor(context, ref),
    );
  }

  // 获取副标题文本样式
  static TextStyle getSubtitleTextStyle(BuildContext context, WidgetRef ref) {
    return TextStyle(
      fontSize: 14,
      fontWeight: FontWeight.normal,
      color: getTextSecondaryColor(context, ref),
    );
  }
}
