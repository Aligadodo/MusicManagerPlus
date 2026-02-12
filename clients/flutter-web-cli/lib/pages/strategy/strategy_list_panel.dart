import 'package:flutter/material.dart';
import '../../models/strategy_info.dart';
import '../../utils/theme_utils.dart';

class StrategyListPanel extends StatelessWidget {
  final List<StrategyInfo> strategies;
  final bool isLoading;
  final StrategyInfo? selectedStrategy;
  final Function(String) onStrategySelected;

  const StrategyListPanel({
    super.key,
    required this.strategies,
    required this.isLoading,
    this.selectedStrategy,
    required this.onStrategySelected,
  });

  @override
  Widget build(BuildContext context) {
    try {
      return Container(
        width: 300,
        padding: const EdgeInsets.only(right: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '可用策略:',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 20),
            if (isLoading && strategies.isEmpty)
              const Center(
                child: CircularProgressIndicator(),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: strategies.length,
                  itemBuilder: (context, index) {
                    try {
                      final strategy = strategies[index];
                      return Card(
                        elevation: 2,
                        margin: const EdgeInsets.symmetric(vertical: 5),
                        child: ListTile(
                          title: Text(strategy.name),
                          subtitle: Text(strategy.description),
                          onTap: () {
                            onStrategySelected(strategy.id);
                          },
                          selected: selectedStrategy?.id == strategy.id,
                        ),
                      );
                    } catch (e) {
                      return Card(
                        color: ThemeUtils.getErrorColor(context).withOpacity(0.1),
                        child: Padding(
                          padding: const EdgeInsets.all(8.0),
                          child: Text(
                            '策略加载失败: $e',
                            style: TextStyle(color: ThemeUtils.getErrorColor(context)),
                          ),
                        ),
                      );
                    }
                  },
                ),
              ),
          ],
        ),
      );
    } catch (e) {
      return Container(
        width: 300,
        padding: const EdgeInsets.only(right: 20),
        child: Card(
          color: ThemeUtils.getErrorColor(context).withOpacity(0.1),
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              children: [
                Icon(Icons.error_outline, color: ThemeUtils.getErrorColor(context)),
                const SizedBox(height: 8),
                Text(
                  '策略列表加载失败: $e',
                  style: TextStyle(color: ThemeUtils.getErrorColor(context)),
                ),
              ],
            ),
          ),
        ),
      );
    }
  }
}
