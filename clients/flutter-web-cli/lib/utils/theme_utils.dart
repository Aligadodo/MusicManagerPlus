import 'package:flutter/material.dart';

/// 主题工具类，提供统一的主题颜色和样式访问
class ThemeUtils {
  /// 获取主题数据
  static ThemeData getTheme(BuildContext context) {
    return Theme.of(context);
  }

  /// 获取主颜色
  static Color getPrimaryColor(BuildContext context) {
    return Theme.of(context).primaryColor;
  }

  /// 获取背景颜色
  static Color getBackgroundColor(BuildContext context) {
    return Theme.of(context).scaffoldBackgroundColor;
  }

  /// 获取卡片颜色
  static Color getCardColor(BuildContext context) {
    return Theme.of(context).cardColor;
  }

  /// 获取边框颜色
  static Color getBorderColor(BuildContext context) {
    return Theme.of(context).dividerColor;
  }

  /// 获取主要文本颜色
  static Color getTextPrimaryColor(BuildContext context) {
    return Theme.of(context).textTheme.bodyLarge?.color ?? Colors.black;
  }

  /// 获取次要文本颜色
  static Color getTextSecondaryColor(BuildContext context) {
    return Theme.of(context).textTheme.bodySmall?.color ?? Colors.grey;
  }

  /// 获取错误颜色
  static Color getErrorColor(BuildContext context) {
    return Theme.of(context).colorScheme.error;
  }

  /// 获取警告颜色
  static Color getWarningColor(BuildContext context) {
    return Colors.orange; // 警告颜色通常固定为橙色
  }

  /// 获取成功颜色
  static Color getSuccessColor(BuildContext context) {
    return Colors.green; // 成功颜色通常固定为绿色
  }

  /// 获取信息颜色
  static Color getInfoColor(BuildContext context) {
    return Theme.of(context).primaryColor;
  }

  /// 获取主要按钮样式
  static ButtonStyle getPrimaryButtonStyle(BuildContext context) {
    return Theme.of(context).elevatedButtonTheme.style ?? ElevatedButton.styleFrom(
      backgroundColor: getPrimaryColor(context),
      foregroundColor: Colors.white,
    );
  }

  /// 获取次要按钮样式
  static ButtonStyle getSecondaryButtonStyle(BuildContext context) {
    return Theme.of(context).outlinedButtonTheme.style ?? OutlinedButton.styleFrom(
      side: BorderSide(color: getPrimaryColor(context)),
      foregroundColor: getPrimaryColor(context),
    );
  }

  /// 获取文本按钮样式
  static ButtonStyle getTextButtonStyle(BuildContext context) {
    return Theme.of(context).textButtonTheme.style ?? TextButton.styleFrom(
      foregroundColor: getPrimaryColor(context),
    );
  }

  /// 获取输入框装饰
  static InputDecoration getInputDecoration(BuildContext context, {
    String? labelText,
    String? hintText,
    Widget? suffixIcon,
  }) {
    return InputDecoration(
      labelText: labelText,
      hintText: hintText,
      suffixIcon: suffixIcon,
      border: OutlineInputBorder(
        borderSide: BorderSide(color: getBorderColor(context)),
      ),
      enabledBorder: OutlineInputBorder(
        borderSide: BorderSide(color: getBorderColor(context)),
      ),
      focusedBorder: OutlineInputBorder(
        borderSide: BorderSide(color: getPrimaryColor(context), width: 2),
      ),
      errorBorder: OutlineInputBorder(
        borderSide: BorderSide(color: getErrorColor(context)),
      ),
      labelStyle: TextStyle(color: getTextSecondaryColor(context)),
      hintStyle: TextStyle(color: getTextSecondaryColor(context)),
    );
  }

  /// 获取卡片装饰
  static BoxDecoration getCardDecoration(BuildContext context, {
    bool withBorder = true,
    bool withShadow = false,
  }) {
    return BoxDecoration(
      color: getCardColor(context),
      border: withBorder ? Border.all(color: getBorderColor(context)) : null,
      boxShadow: withShadow ? [
        BoxShadow(
          color: Colors.black.withOpacity(0.1),
          spreadRadius: 1,
          blurRadius: 3,
          offset: const Offset(0, 2),
        ),
      ] : null,
      borderRadius: BorderRadius.circular(8),
    );
  }

  /// 获取错误卡片装饰
  static BoxDecoration getErrorCardDecoration(BuildContext context) {
    return BoxDecoration(
      color: getErrorColor(context).withOpacity(0.1),
      border: Border.all(color: getErrorColor(context).withOpacity(0.3)),
      borderRadius: BorderRadius.circular(8),
    );
  }

  /// 获取警告卡片装饰
  static BoxDecoration getWarningCardDecoration(BuildContext context) {
    return BoxDecoration(
      color: getWarningColor(context).withOpacity(0.1),
      border: Border.all(color: getWarningColor(context).withOpacity(0.3)),
      borderRadius: BorderRadius.circular(8),
    );
  }

  /// 获取信息卡片装饰
  static BoxDecoration getInfoCardDecoration(BuildContext context) {
    return BoxDecoration(
      color: getInfoColor(context).withOpacity(0.1),
      border: Border.all(color: getInfoColor(context).withOpacity(0.3)),
      borderRadius: BorderRadius.circular(8),
    );
  }
}