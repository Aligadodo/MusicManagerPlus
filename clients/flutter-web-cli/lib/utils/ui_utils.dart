import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class UIUtils {
  /// 显示成功消息
  static void showSuccessMessage(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: _CopyableText(message),
        backgroundColor: Colors.green,
      ),
    );
  }

  /// 显示错误消息
  static void showErrorMessage(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: _CopyableText(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  /// 显示警告消息
  static void showWarningMessage(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: _CopyableText(message),
        backgroundColor: Colors.orange,
      ),
    );
  }

  /// 可双击复制的文本组件
  static Widget _CopyableText(String text) {
    return GestureDetector(
      onDoubleTap: () async {
        await Clipboard.setData(ClipboardData(text: text));
      },
      child: Text(text),
    );
  }

  /// 显示确认对话框
  static Future<bool> showConfirmationDialog(
    BuildContext context,
    String title,
    String message,
  ) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: _CopyableText(title),
          content: _CopyableText(message),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(false);
              },
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(true);
              },
              style: TextButton.styleFrom(
                foregroundColor: Colors.red,
              ),
              child: const Text('确认'),
            ),
          ],
        );
      },
    );
    return result ?? false;
  }

  /// 显示加载对话框
  static Future<void> showLoadingDialog(
    BuildContext context,
    String message,
  ) async {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) {
        return AlertDialog(
          content: Row(
            children: [
              const CircularProgressIndicator(),
              const SizedBox(width: 20),
              _CopyableText(message),
            ],
          ),
        );
      },
    );
  }

  /// 关闭加载对话框
  static void hideLoadingDialog(BuildContext context) {
    Navigator.of(context).pop();
  }

  /// 构建状态标签
  static Widget buildStatusBadge(String status, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        status,
        style: TextStyle(
          fontSize: 12,
          color: color,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  /// 构建进度条
  static Widget buildProgressBar(double progress, Color color) {
    return LinearProgressIndicator(
      value: progress,
      backgroundColor: Colors.grey[200],
      valueColor: AlwaysStoppedAnimation<Color>(color),
      minHeight: 8,
    );
  }
}
