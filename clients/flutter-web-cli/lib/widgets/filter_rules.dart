import 'package:flutter/material.dart';

class FilterRules extends StatelessWidget {
  final List<String> filterList;
  final String newFilterRule;
  final Function(String) onNewFilterRuleChanged;
  final Function() onAddFilterRule;
  final Function(int) onRemoveFilterRule;
  final ThemeData theme;

  const FilterRules({
    super.key,
    required this.filterList,
    required this.newFilterRule,
    required this.onNewFilterRuleChanged,
    required this.onAddFilterRule,
    required this.onRemoveFilterRule,
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
              key: const ValueKey('add_filter_rule_row'),
              children: [
                Expanded(
                  child: TextField(
                    controller: TextEditingController(text: newFilterRule),
                    onChanged: onNewFilterRuleChanged,
                    decoration: InputDecoration(
                      labelText: '添加过滤规则',
                      hintText: '例如：*Convert*',
                      border: const OutlineInputBorder(),
                      labelStyle: theme.textTheme.bodyMedium,
                      hintStyle: theme.textTheme.bodySmall?.copyWith(color: theme.hintColor),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: onAddFilterRule,
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (filterList.isNotEmpty)
              Column(
                children: [
                  Text('当前过滤规则:', style: theme.textTheme.bodyMedium),
                  const SizedBox(height: 8),
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: theme.dividerColor),
                      borderRadius: BorderRadius.circular(8),
                      color: theme.colorScheme.surfaceContainer,
                    ),
                    child: ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: filterList.length,
                      itemBuilder: (context, index) {
                        String rule = filterList[index];
                        return Row(
                          key: ValueKey('scan_filter_rule_row_$index'),
                          children: [
                            Expanded(
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Text(rule, style: theme.textTheme.bodyMedium),
                              ),
                            ),
                            IconButton(
                              icon: Icon(Icons.delete, color: theme.colorScheme.error),
                              onPressed: () => onRemoveFilterRule(index),
                            ),
                          ],
                        );
                      },
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
