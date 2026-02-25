import 'package:flutter/material.dart';
import '../utils/ui_utils.dart';

class PresetManager {
  static bool isPresetApplied(Map<String, dynamic> preset, Map<String, dynamic> currentConfig) {
    final config = preset['config'] as Map<String, dynamic>;
    return config['bgColor'] == currentConfig['bgColor'] &&
           config['accentColor'] == currentConfig['accentColor'] &&
           config['theme'] == currentConfig['theme'];
  }

  static Widget buildPresetCard(
    Map<String, dynamic> preset,
    int index,
    ThemeData theme,
    Map<String, dynamic> currentConfig,
    Function(Map<String, dynamic>) onApply,
    Function(Map<String, dynamic>) onEdit,
    Function(String) onDelete,
  ) {
    final config = preset['config'] as Map<String, dynamic>;
    final isApplied = isPresetApplied(preset, currentConfig);
    final accentColor = UiUtils.parseColor(config['accentColor'] ?? '#2196F3');
    final isDefault = preset['type'] == 'default';
    final themeId = preset['id'];
    
    return Card(
      elevation: isApplied ? 6 : 2,
      borderOnForeground: true,
      shape: RoundedRectangleBorder(
        side: BorderSide(
          color: isApplied ? accentColor : theme.dividerColor,
          width: isApplied ? 2 : 1,
        ),
        borderRadius: BorderRadius.circular(10),
      ),
      child: InkWell(
        onTap: () => onApply(preset),
        hoverColor: UiUtils.parseColor(config['listRowHoverBgColor'] ?? '#F0F8FF'),
        splashColor: accentColor.withOpacity(0.3),
        child: Container(
          padding: const EdgeInsets.all(15),
          decoration: BoxDecoration(
            color: UiUtils.parseColor(config['bgColor'] ?? '#FFFFFF'),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Stack(
            children: [
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    preset['name'] ?? '未命名主题',
                    style: TextStyle(
                      color: UiUtils.parseColor(config['textPrimaryColor'] ?? '#000000'),
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    preset['description'] ?? '',
                    style: TextStyle(
                      color: UiUtils.parseColor(config['textSecondaryColor'] ?? '#666666'),
                      fontSize: 12,
                    ),
                    textAlign: TextAlign.center,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 8),
                  Container(
                    height: 40,
                    decoration: BoxDecoration(
                      color: UiUtils.parseColor(config['panelBgColor'] ?? '#F5F5F5'),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(
                        color: UiUtils.parseColor(config['borderColor'] ?? '#E0E0E0'),
                      ),
                    ),
                    child: Center(
                      child: Text(
                        '预览',
                        style: TextStyle(
                          color: UiUtils.parseColor(config['textPrimaryColor'] ?? '#000000'),
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              if (isApplied)
                Positioned(
                  top: 5,
                  right: 5,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: accentColor,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      '已应用',
                      style: TextStyle(
                        color: UiUtils.parseColor(config['bgColor'] ?? '#FFFFFF'),
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ),
              Positioned(
                top: 5,
                left: 5,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.edit, size: 18),
                      onPressed: () => onEdit(preset),
                      tooltip: '编辑',
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(
                        minWidth: 30,
                        minHeight: 30,
                      ),
                    ),
                    if (!isDefault)
                      IconButton(
                        icon: const Icon(Icons.delete, size: 18),
                        onPressed: () => onDelete(themeId),
                        tooltip: '删除',
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(
                          minWidth: 30,
                          minHeight: 30,
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  static Widget buildPresetTabContent(
    List<Map<String, dynamic>> presets,
    ThemeData theme,
    Map<String, dynamic> currentConfig,
    Function(Map<String, dynamic>) onApply,
    Function(Map<String, dynamic>) onEdit,
    Function(String) onDelete,
    Function() onSaveAsPreset,
  ) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (presets.isEmpty)
            Center(
              child: Padding(
                padding: const EdgeInsets.all(32),
                child: Text('暂无主题预设', style: theme.textTheme.bodyMedium),
              ),
            )
          else
            GridView.builder(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 2,
              ),
              itemCount: presets.length,
              itemBuilder: (context, index) {
                final preset = presets[index];
                return buildPresetCard(
                  preset,
                  index,
                  theme,
                  currentConfig,
                  onApply,
                  onEdit,
                  onDelete,
                );
              },
            ),
          const SizedBox(height: 20),
          Center(
            child: ElevatedButton.icon(
              onPressed: onSaveAsPreset,
              icon: const Icon(Icons.add),
              label: const Text('保存当前主题为预设'),
              style: ElevatedButton.styleFrom(
                backgroundColor: theme.colorScheme.primary,
                foregroundColor: theme.colorScheme.onPrimary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
