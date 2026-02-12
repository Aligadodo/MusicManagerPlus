import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../providers/config_provider.dart';
import '../utils/ui_utils.dart';

// 主题样式提供器
class ThemeProvider extends ConsumerWidget {
  final Widget child;

  const ThemeProvider({super.key, required this.child});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    final appearanceConfig = config.appearanceConfig;

    return Theme(
      data: _buildThemeData(appearanceConfig),
      child: child,
    );
  }

  ThemeData _buildThemeData(Map<String, dynamic> appearanceConfig) {
    final isDark = appearanceConfig['darkBackground'] as bool? ?? false;
    final primaryColor = UiUtils.parseColor(appearanceConfig['accentColor'] as String? ?? '#2196F3');
    final backgroundColor = UiUtils.parseColor(appearanceConfig['bgColor'] as String? ?? '#FFFFFF');
    final textPrimaryColor = UiUtils.parseColor(appearanceConfig['textPrimaryColor'] as String? ?? '#000000');
    final textSecondaryColor = UiUtils.parseColor(appearanceConfig['textSecondaryColor'] as String? ?? '#666666');
    final borderColor = UiUtils.parseColor(appearanceConfig['borderColor'] as String? ?? '#E0E0E0');

    return ThemeData(
      useMaterial3: true,
      brightness: isDark ? Brightness.dark : Brightness.light,
      primaryColor: primaryColor,
      scaffoldBackgroundColor: backgroundColor,
      colorScheme: ColorScheme.fromSeed(
        seedColor: primaryColor,
        brightness: isDark ? Brightness.dark : Brightness.light,
        primary: primaryColor,
        onPrimary: Colors.white,
        secondary: primaryColor.withOpacity(0.8),
        onSecondary: Colors.white,
        background: backgroundColor,
        onBackground: textPrimaryColor,
        surface: backgroundColor,
        onSurface: textPrimaryColor,
      ),
      textTheme: TextTheme(
        displayLarge: TextStyle(color: textPrimaryColor),
        displayMedium: TextStyle(color: textPrimaryColor),
        displaySmall: TextStyle(color: textPrimaryColor),
        headlineLarge: TextStyle(color: textPrimaryColor),
        headlineMedium: TextStyle(color: textPrimaryColor),
        headlineSmall: TextStyle(color: textPrimaryColor),
        titleLarge: TextStyle(color: textPrimaryColor),
        titleMedium: TextStyle(color: textPrimaryColor),
        titleSmall: TextStyle(color: textPrimaryColor),
        bodyLarge: TextStyle(color: textPrimaryColor),
        bodyMedium: TextStyle(color: textPrimaryColor),
        bodySmall: TextStyle(color: textSecondaryColor),
        labelLarge: TextStyle(color: textPrimaryColor),
        labelMedium: TextStyle(color: textSecondaryColor),
        labelSmall: TextStyle(color: textSecondaryColor),
      ),
      buttonTheme: ButtonThemeData(
        buttonColor: primaryColor,
        textTheme: ButtonTextTheme.primary,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primaryColor,
          foregroundColor: Colors.white,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: primaryColor,
          side: BorderSide(color: primaryColor),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: primaryColor,
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: OutlineInputBorder(
          borderSide: BorderSide(color: borderColor),
        ),
        focusedBorder: OutlineInputBorder(
          borderSide: BorderSide(color: primaryColor),
        ),
        enabledBorder: OutlineInputBorder(
          borderSide: BorderSide(color: borderColor),
        ),
        disabledBorder: OutlineInputBorder(
          borderSide: BorderSide(color: UiUtils.parseColor(appearanceConfig['textDisabledColor'] as String? ?? '#CCCCCC')),
        ),
        hintStyle: TextStyle(color: textSecondaryColor),
        labelStyle: TextStyle(color: textPrimaryColor),
      ),
      dividerTheme: DividerThemeData(
        color: borderColor,
        thickness: 1,
      ),
      expansionTileTheme: ExpansionTileThemeData(
        backgroundColor: backgroundColor,
        collapsedBackgroundColor: backgroundColor,
        textColor: textPrimaryColor,
        iconColor: textPrimaryColor,
      ),
      listTileTheme: ListTileThemeData(
        tileColor: UiUtils.parseColor(appearanceConfig['listBgColor'] as String? ?? '#FFFFFF'),
        textColor: textPrimaryColor,
        selectedTileColor: UiUtils.parseColor(appearanceConfig['listRowSelectedBgColor'] as String? ?? '#E3F2FD'),
        selectedColor: UiUtils.parseColor(appearanceConfig['listRowSelectedTextColor'] as String? ?? '#2196F3'),
        iconColor: textPrimaryColor,
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: primaryColor,
        contentTextStyle: TextStyle(color: Colors.white),
      ),
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: primaryColor,
        foregroundColor: Colors.white,
      ),
    );
  }
}

// 主题扩展
extension ThemeExtension on BuildContext {
  // 获取主题颜色
  Color get primaryColor => Theme.of(this).primaryColor;
  
  // 获取背景颜色
  Color get backgroundColor => Theme.of(this).scaffoldBackgroundColor;
  
  // 获取文本颜色
  Color get textPrimaryColor => Theme.of(this).textTheme.bodyLarge?.color ?? Colors.black;
  
  // 获取次要文本颜色
  Color get textSecondaryColor => Theme.of(this).textTheme.bodySmall?.color ?? Colors.grey;
  
  // 获取边框颜色
  Color get borderColor => Theme.of(this).inputDecorationTheme.border?.borderSide.color ?? Colors.grey;
  
  // 获取卡片颜色
  Color get cardColor => Theme.of(this).cardColor;
  
  // 获取按钮样式
  ButtonStyle get primaryButtonStyle => Theme.of(this).elevatedButtonTheme.style ?? ElevatedButton.styleFrom();
  
  // 获取次要按钮样式
  ButtonStyle get secondaryButtonStyle => Theme.of(this).outlinedButtonTheme.style ?? OutlinedButton.styleFrom();
  
  // 获取文本按钮样式
  ButtonStyle get textButtonStyle => Theme.of(this).textButtonTheme.style ?? TextButton.styleFrom();
}