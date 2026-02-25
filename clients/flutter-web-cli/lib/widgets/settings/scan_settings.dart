import 'package:flutter/material.dart';

class ScanSettings extends StatelessWidget {
  final String recursionMode;
  final int recursionDepth;
  final int minRecursionDepth;
  final int maxRecursionDepth;
  final Function(String) onRecursionModeChanged;
  final Function(int) onRecursionDepthChanged;
  final Function(int) onMinRecursionDepthChanged;
  final Function(int) onMaxRecursionDepthChanged;
  final ThemeData theme;

  const ScanSettings({
    super.key,
    required this.recursionMode,
    required this.recursionDepth,
    required this.minRecursionDepth,
    required this.maxRecursionDepth,
    required this.onRecursionModeChanged,
    required this.onRecursionDepthChanged,
    required this.onMinRecursionDepthChanged,
    required this.onMaxRecursionDepthChanged,
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
              key: const ValueKey('scan_mode_row'),
              children: [
                Text('扫描模式:', style: theme.textTheme.bodyMedium),
                const SizedBox(width: 20),
                DropdownButton<String>(
                  value: recursionMode,
                  items: const [
                    DropdownMenuItem(value: 'ALL', child: Text('全部文件')),
                    DropdownMenuItem(value: 'CURRENT', child: Text('当前目录')),
                    DropdownMenuItem(value: 'SPECIFIC', child: Text('指定目录层级')),
                    DropdownMenuItem(value: 'RANGE', child: Text('目录层级范围')),
                  ],
                  onChanged: (value) {
                    onRecursionModeChanged(value ?? 'ALL');
                  },
                  dropdownColor: theme.colorScheme.surface,
                  style: theme.textTheme.bodyMedium,
                ),
              ],
            ),
            if (recursionMode == 'SPECIFIC')
              Padding(
                padding: const EdgeInsets.only(left: 120, top: 16),
                child: Row(
                  children: [
                    Text('扫描层级:', style: theme.textTheme.bodyMedium),
                    const SizedBox(width: 20),
                    Expanded(
                      child: Slider(
                        min: 1,
                        max: 10,
                        value: recursionDepth.toDouble(),
                        onChanged: (value) {
                          onRecursionDepthChanged(value.toInt());
                        },
                        divisions: 9,
                        label: '$recursionDepth',
                        activeColor: theme.primaryColor,
                      ),
                    ),
                    Text('$recursionDepth', style: theme.textTheme.bodyMedium),
                  ],
                ),
              ),
            if (recursionMode == 'RANGE')
              Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(left: 120, top: 16),
                    child: Row(
                      key: const ValueKey('recursion_depth_row'),
                      children: [
                        Text('最小层级:', style: theme.textTheme.bodyMedium),
                        const SizedBox(width: 20),
                        Expanded(
                          child: Slider(
                            min: 1,
                            max: 10,
                            value: minRecursionDepth.toDouble(),
                            onChanged: (value) {
                              onMinRecursionDepthChanged(value.toInt());
                            },
                            divisions: 9,
                            label: '$minRecursionDepth',
                            activeColor: theme.primaryColor,
                          ),
                        ),
                        Text('$minRecursionDepth', style: theme.textTheme.bodyMedium),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(left: 120, top: 16),
                    child: Row(
                      key: const ValueKey('max_recursion_depth_row'),
                      children: [
                        Text('最大层级:', style: theme.textTheme.bodyMedium),
                        const SizedBox(width: 20),
                        Expanded(
                          child: Slider(
                            min: minRecursionDepth.toDouble(),
                            max: 10,
                            value: maxRecursionDepth.toDouble(),
                            onChanged: (value) {
                              onMaxRecursionDepthChanged(value.toInt());
                            },
                            divisions: 10 - minRecursionDepth,
                            label: '$maxRecursionDepth',
                            activeColor: theme.primaryColor,
                          ),
                        ),
                        Text('$maxRecursionDepth', style: theme.textTheme.bodyMedium),
                      ],
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
