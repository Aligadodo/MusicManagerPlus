import 'package:flutter/material.dart';

class FileTypeNode {
  String id;
  String name;
  List<String> extensions;
  List<FileTypeNode> children;
  bool selected;
  bool indeterminate;
  bool isLeaf;

  FileTypeNode({
    required this.id,
    required this.name,
    this.extensions = const [],
    this.children = const [],
    this.selected = false,
    this.indeterminate = false,
    this.isLeaf = false,
  });

  static void updateNodeSelection(FileTypeNode node, bool? value, FileTypeNode root) {
    bool newValue = value ?? !node.selected;
    updateChildren(node, newValue);
    updateParentSelection(node, root);
  }

  static void updateChildren(FileTypeNode node, bool selected) {
    node.selected = selected;
    node.indeterminate = false;

    for (var child in node.children) {
      updateChildren(child, selected);
    }
  }

  static void updateParentSelection(FileTypeNode node, FileTypeNode root) {
    final parent = findParent(node, root);
    if (parent != null) {
      final allSelected = parent.children.every((child) => child.selected);
      final anySelected = parent.children.any((child) => child.selected || child.indeterminate);

      parent.selected = allSelected;
      parent.indeterminate = anySelected && !allSelected;

      updateParentSelection(parent, root);
    }
  }

  static FileTypeNode? findParent(FileTypeNode node, FileTypeNode root) {
    for (var category in root.children) {
      if (category.children.contains(node)) {
        return category;
      }
      for (var child in category.children) {
        if (child.children.contains(node)) {
          return child;
        }
      }
    }
    return null;
  }

  static void selectAll(FileTypeNode node, bool isSelected) {
    node.selected = isSelected;
    node.indeterminate = false;
    for (var child in node.children) {
      selectAll(child, isSelected);
    }
  }
}

class FileTypeTree extends StatefulWidget {
  final FileTypeNode fileTypeTree;
  final List<String> customFileTypes;
  final String newFileType;
  final Function(FileTypeNode, bool?) onNodeSelectionChanged;
  final Function(FileTypeNode, bool) onSelectAll;
  final Function(String) onNewFileTypeChanged;
  final Function() onAddCustomFileType;
  final Function(String) onRemoveCustomFileType;
  final ThemeData theme;

  const FileTypeTree({
    super.key,
    required this.fileTypeTree,
    required this.customFileTypes,
    required this.newFileType,
    required this.onNodeSelectionChanged,
    required this.onSelectAll,
    required this.onNewFileTypeChanged,
    required this.onAddCustomFileType,
    required this.onRemoveCustomFileType,
    required this.theme,
  });

  @override
  State<FileTypeTree> createState() => _FileTypeTreeState();
}

class _FileTypeTreeState extends State<FileTypeTree> {
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
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const SizedBox(),
                Row(
                  children: [
                    TextButton(
                      onPressed: () => widget.onSelectAll(widget.fileTypeTree, true),
                      child: const Text('全选'),
                    ),
                    TextButton(
                      onPressed: () => widget.onSelectAll(widget.fileTypeTree, false),
                      child: const Text('取消全选'),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 16),
            Container(
              height: 400,
              decoration: BoxDecoration(
                border: Border.all(color: widget.theme.dividerColor),
                borderRadius: BorderRadius.circular(8),
                color: widget.theme.colorScheme.surfaceContainer,
              ),
              child: ListView(
                children: widget.fileTypeTree.children.map((category) {
                  return _buildCategoryNode(category);
                }).toList(),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              '手动添加文件类型后缀',
              style: widget.theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: InputDecoration(
                      labelText: '输入文件类型后缀',
                      hintText: '例如：mp3,flac,wav',
                      border: const OutlineInputBorder(),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      labelStyle: widget.theme.textTheme.bodyMedium,
                      hintStyle: widget.theme.textTheme.bodySmall?.copyWith(color: widget.theme.hintColor),
                    ),
                    onChanged: widget.onNewFileTypeChanged,
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: widget.onAddCustomFileType,
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (widget.customFileTypes.isNotEmpty)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '自定义文件类型:',
                    style: widget.theme.textTheme.bodySmall?.copyWith(color: widget.theme.hintColor),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: widget.customFileTypes.map((type) {
                      return Chip(
                        label: Text(type, style: widget.theme.textTheme.bodySmall),
                        onDeleted: () => widget.onRemoveCustomFileType(type),
                        deleteIcon: const Icon(Icons.close, size: 16),
                        deleteIconColor: widget.theme.colorScheme.error,
                        backgroundColor: widget.theme.colorScheme.surfaceContainer,
                      );
                    }).toList(),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildCategoryNode(FileTypeNode category) {
    if (category.isLeaf) {
      return CheckboxListTile(
        key: ValueKey(category.id),
        title: Text(category.name, style: widget.theme.textTheme.bodyMedium),
        value: category.selected,
        onChanged: (value) => widget.onNodeSelectionChanged(category, value),
        controlAffinity: ListTileControlAffinity.leading,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16),
      );
    }

    return ExpansionTile(
      key: ValueKey(category.id),
      title: Row(
        children: [
          Checkbox(
            value: category.selected,
            tristate: category.indeterminate,
            onChanged: (value) => widget.onNodeSelectionChanged(category, value),
          ),
          Expanded(
            child: Text(
              category.name,
              style: widget.theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w500),
            ),
          ),
        ],
      ),
      children: category.children.map((child) {
        if (child.isLeaf) {
          return CheckboxListTile(
            key: ValueKey(child.id),
            title: Text(child.name, style: widget.theme.textTheme.bodyMedium),
            value: child.selected,
            onChanged: (value) => widget.onNodeSelectionChanged(child, value),
            controlAffinity: ListTileControlAffinity.leading,
            contentPadding: const EdgeInsets.only(left: 48),
          );
        } else {
          return ExpansionTile(
            key: ValueKey(child.id),
            title: Row(
              children: [
                Checkbox(
                  value: child.selected,
                  tristate: child.indeterminate,
                  onChanged: (value) => widget.onNodeSelectionChanged(child, value),
                ),
                Expanded(
                  child: Text(
                    child.name,
                    style: widget.theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
            children: child.children.map((leaf) {
              return CheckboxListTile(
                key: ValueKey(leaf.id),
                title: Text(leaf.name, style: widget.theme.textTheme.bodyMedium),
                value: leaf.selected,
                onChanged: (value) => widget.onNodeSelectionChanged(leaf, value),
                controlAffinity: ListTileControlAffinity.leading,
                contentPadding: const EdgeInsets.only(left: 72),
              );
            }).toList(),
          );
        }
      }).toList(),
    );
  }
}
