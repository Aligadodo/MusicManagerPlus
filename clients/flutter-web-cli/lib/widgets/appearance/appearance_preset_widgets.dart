import 'package:flutter/material.dart';

class AppearancePresetWidgets {
  static Widget buildPresetCard(Map<String, dynamic> preset, int index, ThemeData theme, int selectedPresetIndex, Function(Map<String, dynamic>) onApply, Function(String?) onDelete) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        preset['name'] ?? '未命名主题',
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        preset['description'] ?? '',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.textTheme.bodySmall?.color?.withOpacity(0.7),
                        ),
                      ),
                    ],
                  ),
                ),
                Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.edit),
                      onPressed: () => onApply(preset),
                      tooltip: '编辑',
                    ),
                    if (preset['themeId'] != null && preset['themeId'] != 'default')
                      IconButton(
                        icon: const Icon(Icons.delete),
                        onPressed: () => onDelete(preset['themeId']),
                        tooltip: '删除',
                      ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 12),
            Container(
              height: 60,
              decoration: BoxDecoration(
                color: (preset['config'] as Map<String, dynamic>?)?['bgColor'] as Color? ?? Colors.white,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: theme.dividerColor),
              ),
              child: Center(
                child: Text(
                  '预览',
                  style: TextStyle(
                    color: (preset['config'] as Map<String, dynamic>?)?['textPrimaryColor'] as Color? ?? Colors.black,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  static Widget buildPresetTabContent(List<Map<String, dynamic>> presets, ThemeData theme, int selectedPresetIndex, Function(Map<String, dynamic>) onApply, Function(String?) onDelete) {
    return GridView.builder(
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        childAspectRatio: 1.5,
        crossAxisSpacing: 12,
        mainAxisSpacing: 12,
      ),
      itemCount: presets.length,
      itemBuilder: (context, index) {
        final preset = presets[index];
        return buildPresetCard(preset, index, theme, selectedPresetIndex, onApply, onDelete);
      },
    );
  }
}
