import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart'; // 导入kIsWeb

/// 文件操作工具类
/// 提供文件打开、删除等操作
class FileUtils {
  /// 打开文件
  /// [filePath] 文件路径
  /// 返回是否成功打开
  static Future<bool> openFile(String filePath) async {
    try {
      final file = File(filePath);
      if (!file.existsSync()) {
        debugPrint('文件不存在: $filePath');
        return false;
      }

      // 在不同平台打开文件
      if (Platform.isMacOS) {
        await Process.run('open', [filePath]);
      } else if (Platform.isWindows) {
        await Process.run('start', ['', filePath]);
      } else if (Platform.isLinux) {
        await Process.run('xdg-open', [filePath]);
      } else if (kIsWeb) {
        // Web平台不支持直接打开文件，显示提示
        debugPrint('Web平台不支持直接打开文件');
        return false;
      } else {
        debugPrint('不支持的平台');
        return false;
      }

      return true;
    } catch (e) {
      debugPrint('打开文件失败: $e');
      return false;
    }
  }

  /// 删除文件
  /// [filePath] 文件路径
  /// 返回是否成功删除
  static Future<bool> deleteFile(String filePath) async {
    try {
      final file = File(filePath);
      if (!file.existsSync()) {
        debugPrint('文件不存在: $filePath');
        return false;
      }

      await file.delete();
      return true;
    } catch (e) {
      debugPrint('删除文件失败: $e');
      return false;
    }
  }

  /// 显示文件操作结果
  /// [context] 上下文
  /// [success] 是否成功
  /// [successMessage] 成功消息
  /// [errorMessage] 错误消息
  static void showFileOperationResult(
    BuildContext context,
    bool success,
    String successMessage,
    String errorMessage,
  ) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(success ? successMessage : errorMessage),
        backgroundColor: success ? Colors.green : Colors.red,
        duration: const Duration(seconds: 3),
      ),
    );
  }

  /// 确认删除文件
  /// [context] 上下文
  /// [filePath] 文件路径
  /// [onConfirm] 确认回调
  static Future<void> confirmDeleteFile(
    BuildContext context,
    String filePath,
    VoidCallback onConfirm,
  ) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除文件:\n$filePath'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      onConfirm();
    }
  }

  /// 打开文件所在目录
  /// [filePath] 文件路径
  /// 返回是否成功打开
  static Future<bool> openFileLocation(String filePath) async {
    try {
      final file = File(filePath);
      if (!file.existsSync()) {
        debugPrint('文件不存在: $filePath');
        return false;
      }

      final directory = file.parent.path;

      // 在不同平台打开目录
      if (Platform.isMacOS) {
        await Process.run('open', [directory]);
      } else if (Platform.isWindows) {
        await Process.run('explorer', [directory]);
      } else if (Platform.isLinux) {
        await Process.run('xdg-open', [directory]);
      } else if (kIsWeb) {
        // Web平台不支持直接打开目录，显示提示
        debugPrint('Web平台不支持直接打开目录');
        return false;
      } else {
        debugPrint('不支持的平台');
        return false;
      }

      return true;
    } catch (e) {
      debugPrint('打开文件所在目录失败: $e');
      return false;
    }
  }
}