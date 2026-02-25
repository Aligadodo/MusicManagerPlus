import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/utils/theme_utils.dart';

class PipelineListItem extends StatelessWidget {
  final StrategyInfo strategy;
  final int index;
  final bool isSelected;
  final VoidCallback onTap;
  final VoidCallback onMoveUp;
  final VoidCallback onMoveDown;
  final VoidCallback onDelete;

  const PipelineListItem({
    super.key,
    required this.strategy,
    required this.index,
    required this.isSelected,
    required this.onTap,
    required this.onMoveUp,
    required this.onMoveDown,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 6),
      decoration: BoxDecoration(
        color: isSelected ? ThemeUtils.getPrimaryColor(context).withOpacity(0.1) : Colors.white,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(
          color: isSelected ? ThemeUtils.getPrimaryColor(context) : ThemeUtils.getBorderColor(context),
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(6),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          child: Row(
            children: [
              CircleAvatar(
                radius: 14,
                backgroundColor: ThemeUtils.getPrimaryColor(context),
                child: Text(
                  '${index + 1}',
                  style: const TextStyle(color: Colors.white, fontSize: 12),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      strategy.name,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                    ),
                    Text(
                      strategy.description,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 11,
                        color: ThemeUtils.getTextSecondaryColor(context),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              _buildActionButtons(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        IconButton(
          icon: const Icon(Icons.arrow_upward, size: 18),
          onPressed: onMoveUp,
          tooltip: '上移',
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints(),
        ),
        IconButton(
          icon: const Icon(Icons.arrow_downward, size: 18),
          onPressed: onMoveDown,
          tooltip: '下移',
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints(),
        ),
        IconButton(
          icon: const Icon(Icons.delete, size: 18, color: Colors.red),
          onPressed: onDelete,
          tooltip: '删除',
          padding: EdgeInsets.zero,
          constraints: const BoxConstraints(),
        ),
      ],
    );
  }
}
