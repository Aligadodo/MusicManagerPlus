import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/source_directory.dart';
import 'package:filemanager_flutter/utils/theme_utils.dart';

class DirectoryListItem extends StatelessWidget {
  final SourceDirectory directory;
  final int index;
  final VoidCallback onMoveUp;
  final VoidCallback onMoveDown;
  final VoidCallback onDelete;

  const DirectoryListItem({
    super.key,
    required this.directory,
    required this.index,
    required this.onMoveUp,
    required this.onMoveDown,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(6),
        border: Border(
          bottom: BorderSide(color: ThemeUtils.getBorderColor(context)),
        ),
      ),
      child: Row(
        children: [
          Icon(Icons.folder, color: ThemeUtils.getPrimaryColor(context), size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  directory.path.split('/').last,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                ),
                Text(
                  directory.path,
                  style: TextStyle(
                    color: ThemeUtils.getTextSecondaryColor(context),
                    fontSize: 11,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
          _buildActionButtons(),
        ],
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
