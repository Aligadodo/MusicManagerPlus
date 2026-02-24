import 'package:flutter/material.dart';

class RunSettings extends StatelessWidget {
  final bool autoRefresh;
  final int previewLimit;
  final int executionLimit;
  final Function(bool) onAutoRefreshChanged;
  final Function(int) onPreviewLimitChanged;
  final Function(int) onExecutionLimitChanged;
  final ThemeData theme;

  const RunSettings({
    super.key,
    required this.autoRefresh,
    required this.previewLimit,
    required this.executionLimit,
    required this.onAutoRefreshChanged,
    required this.onPreviewLimitChanged,
    required this.onExecutionLimitChanged,
    required this.theme,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              key: const ValueKey('auto_refresh_row'),
              children: [
                Checkbox(
                  value: autoRefresh,
                  onChanged: (value) => onAutoRefreshChanged(value ?? true),
                  fillColor: MaterialStateProperty.resolveWith((states) {
                    if (states.contains(MaterialState.selected)) {
                      return theme.primaryColor;
                    }
                    return null;
                  }),
                ),
                Text('自动刷新', style: theme.textTheme.bodyMedium),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('preview_limit_row'),
              children: [
                Text('预览数量限制:', style: theme.textTheme.bodyMedium),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 50,
                    max: 1000,
                    value: previewLimit.toDouble(),
                    onChanged: (value) => onPreviewLimitChanged(value.toInt()),
                    divisions: 19,
                    label: '$previewLimit',
                    activeColor: theme.primaryColor,
                    inactiveColor: theme.dividerColor,
                  ),
                ),
                Text('$previewLimit', style: theme.textTheme.bodyMedium),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('execution_limit_row'),
              children: [
                Text('执行数量限制:', style: theme.textTheme.bodyMedium),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 100,
                    max: 5000,
                    value: executionLimit.toDouble(),
                    onChanged: (value) => onExecutionLimitChanged(value.toInt()),
                    divisions: 49,
                    label: '$executionLimit',
                    activeColor: theme.primaryColor,
                    inactiveColor: theme.dividerColor,
                  ),
                ),
                Text('$executionLimit', style: theme.textTheme.bodyMedium),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
