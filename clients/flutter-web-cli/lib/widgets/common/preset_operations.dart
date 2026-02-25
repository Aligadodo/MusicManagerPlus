import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../api/config_service.dart';
import '../../providers/config_provider.dart';
import 'selectable_text_widget.dart';

class PresetOperations {
  final BuildContext context;
  final WidgetRef ref;
  final ConfigService configService;
  final Map<String, dynamic> appearanceConfig;
  final Function(bool) onLoadingChanged;
  final Function() onRefreshPresets;

  const PresetOperations({
    required this.context,
    required this.ref,
    required this.configService,
    required this.appearanceConfig,
    required this.onLoadingChanged,
    required this.onRefreshPresets,
  });

  Future<void> applyPreset(Map<String, dynamic> preset) async {
    try {
      final config = preset['config'];
      if (config == null) {
        throw Exception('主题配置为空');
      }

      if (config is! Map<String, dynamic>) {
        throw Exception('主题配置格式错误');
      }

      onLoadingChanged(true);

      final configNotifier = ref.read(configProvider.notifier);
      configNotifier.updateAppearanceConfig(Map<String, dynamic>.from(config));
      await configNotifier.saveConfig();
      
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('主题预设应用成功')),
        );
      }
    } catch (e) {
      print('应用主题预设失败: $e');
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: SelectableTextWidget(
              text: '应用主题预设失败: $e',
              style: const TextStyle(color: Colors.white),
              maxLines: 5,
            ),
            backgroundColor: Colors.red,
            duration: const Duration(seconds: 5),
          ),
        );
      }
    } finally {
      onLoadingChanged(false);
    }
  }

  Future<void> saveAsPreset(Map<String, dynamic> currentConfig) async {
    final nameController = TextEditingController();
    final descriptionController = TextEditingController();

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('保存主题预设'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameController,
              decoration: const InputDecoration(
                labelText: '主题名称',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: descriptionController,
              decoration: const InputDecoration(
                labelText: '主题描述',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('保存'),
          ),
        ],
      ),
    );

    if (result == true && nameController.text.isNotEmpty) {
      try {
        onLoadingChanged(true);

        final newPreset = {
          'id': DateTime.now().millisecondsSinceEpoch.toString(),
          'name': nameController.text,
          'description': descriptionController.text,
          'config': Map<String, dynamic>.from(currentConfig),
          'type': 'custom',
          'createdAt': DateTime.now().toIso8601String(),
        };

        await configService.createTheme(newPreset);
        await onRefreshPresets();

        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('主题预设保存成功')),
          );
        }
      } catch (e) {
        print('保存主题预设失败: $e');
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: SelectableTextWidget(
                text: '保存主题预设失败: $e',
                style: const TextStyle(color: Colors.white),
                maxLines: 5,
              ),
              backgroundColor: Colors.red,
              duration: const Duration(seconds: 5),
            ),
          );
        }
      } finally {
        onLoadingChanged(false);
      }
    }

    nameController.dispose();
    descriptionController.dispose();
  }

  Future<void> editTheme(Map<String, dynamic> preset) async {
    final nameController = TextEditingController(text: preset['name'] ?? '');
    final descriptionController = TextEditingController(text: preset['description'] ?? '');
    final themeId = preset['id'];

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('编辑主题预设'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameController,
              decoration: const InputDecoration(
                labelText: '主题名称',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: descriptionController,
              decoration: const InputDecoration(
                labelText: '主题描述',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('保存'),
          ),
        ],
      ),
    );

    if (result == true && nameController.text.isNotEmpty) {
      try {
        onLoadingChanged(true);

        final updatedPreset = {
          'id': themeId,
          'name': nameController.text,
          'description': descriptionController.text,
          'config': Map.from(preset['config'] ?? {}),
          'type': preset['type'] ?? 'custom',
          'createdAt': preset['createdAt'] ?? DateTime.now().toIso8601String(),
          'updatedAt': DateTime.now().toIso8601String(),
        };

        await configService.updateTheme(themeId, updatedPreset);
        await onRefreshPresets();

        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('主题预设更新成功')),
          );
        }
      } catch (e) {
        print('更新主题预设失败: $e');
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: SelectableTextWidget(
                text: '更新主题预设失败: $e',
                style: const TextStyle(color: Colors.white),
                maxLines: 5,
              ),
              backgroundColor: Colors.red,
              duration: const Duration(seconds: 5),
            ),
          );
        }
      } finally {
        onLoadingChanged(false);
      }
    }

    nameController.dispose();
    descriptionController.dispose();
  }

  Future<void> deleteTheme(String? themeId) async {
    if (themeId == null) return;

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除这个主题预设吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        onLoadingChanged(true);
        await configService.deleteTheme(themeId);
        await onRefreshPresets();

        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('主题预设删除成功')),
          );
        }
      } catch (e) {
        print('删除主题预设失败: $e');
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: SelectableTextWidget(
                text: '删除主题预设失败: $e',
                style: const TextStyle(color: Colors.white),
                maxLines: 5,
              ),
              backgroundColor: Colors.red,
              duration: const Duration(seconds: 5),
            ),
          );
        }
      } finally {
        onLoadingChanged(false);
      }
    }
  }
}
