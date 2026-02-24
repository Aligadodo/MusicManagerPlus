import 'package:flutter/material.dart';

class ThreadPoolSettings extends StatelessWidget {
  final int previewThreads;
  final int executionThreads;
  final String threadPoolMode;
  final Function(int) onPreviewThreadsChanged;
  final Function(int) onExecutionThreadsChanged;
  final Function(String) onThreadPoolModeChanged;
  final ThemeData theme;

  const ThreadPoolSettings({
    super.key,
    required this.previewThreads,
    required this.executionThreads,
    required this.threadPoolMode,
    required this.onPreviewThreadsChanged,
    required this.onExecutionThreadsChanged,
    required this.onThreadPoolModeChanged,
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
              key: const ValueKey('preview_threads_row'),
              children: [
                Text('预览线程数:', style: theme.textTheme.bodyMedium),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 1,
                    max: 16,
                    value: previewThreads.toDouble(),
                    onChanged: (value) {
                      onPreviewThreadsChanged(value.toInt());
                    },
                    divisions: 15,
                    label: '$previewThreads',
                    activeColor: theme.primaryColor,
                  ),
                ),
                Text('$previewThreads', style: theme.textTheme.bodyMedium),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('execution_threads_row'),
              children: [
                Text('执行线程数:', style: theme.textTheme.bodyMedium),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 1,
                    max: 12,
                    value: executionThreads.toDouble(),
                    onChanged: (value) {
                      onExecutionThreadsChanged(value.toInt());
                    },
                    divisions: 11,
                    label: '$executionThreads',
                    activeColor: theme.primaryColor,
                  ),
                ),
                Text('$executionThreads', style: theme.textTheme.bodyMedium),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('thread_pool_mode_row'),
              children: [
                Text('线程池模式:', style: theme.textTheme.bodyMedium),
                const SizedBox(width: 20),
                DropdownButton<String>(
                  value: threadPoolMode,
                  items: const [
                    DropdownMenuItem(value: 'GLOBAL', child: Text('全局统一配置')),
                    DropdownMenuItem(value: 'ROOT_PATH', child: Text('根路径独立配置')),
                  ],
                  onChanged: (value) {
                    onThreadPoolModeChanged(value ?? 'GLOBAL');
                  },
                  dropdownColor: theme.colorScheme.surface,
                  style: theme.textTheme.bodyMedium,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
