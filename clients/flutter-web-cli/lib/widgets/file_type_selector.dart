import 'package:flutter/material.dart';

class FileTypeNode {
  final String id;
  final String name;
  final List<String> extensions;
  final List<FileTypeNode> children;
  bool selected;
  bool indeterminate;

  FileTypeNode({
    required this.id,
    required this.name,
    this.extensions = const [],
    this.children = const [],
    this.selected = false,
    this.indeterminate = false,
  });

  bool get isLeaf => children.isEmpty;
}

class FileTypeSelector extends StatefulWidget {
  const FileTypeSelector({super.key});

  @override
  State<FileTypeSelector> createState() => _FileTypeSelectorState();
}

class _FileTypeSelectorState extends State<FileTypeSelector> {
  late List<FileTypeNode> _fileTypeTree;

  @override
  void initState() {
    super.initState();
    _fileTypeTree = _buildFileTypeTree();
  }

  List<FileTypeNode> _buildFileTypeTree() {
    return [
      FileTypeNode(
        id: 'folder',
        name: '文件夹',
        selected: true,
      ),
      FileTypeNode(
        id: 'file',
        name: '除文件夹',
        selected: true,
      ),
      FileTypeNode(
        id: 'audio',
        name: '音频',
        selected: true,
        children: [
          FileTypeNode(id: 'dsf', name: 'dsf'),
          FileTypeNode(id: 'dff', name: 'dff'),
          FileTypeNode(id: 'dts', name: 'dts'),
          FileTypeNode(id: 'ape', name: 'ape'),
          FileTypeNode(id: 'wav', name: 'wav'),
          FileTypeNode(id: 'flac', name: 'flac'),
          FileTypeNode(id: 'm4a', name: 'm4a'),
          FileTypeNode(id: 'dfd', name: 'dfd'),
          FileTypeNode(id: 'tak', name: 'tak'),
          FileTypeNode(id: 'tta', name: 'tta'),
          FileTypeNode(id: 'wv', name: 'wv'),
          FileTypeNode(id: 'mp3', name: 'mp3'),
          FileTypeNode(id: 'aac', name: 'aac'),
          FileTypeNode(id: 'ogg', name: 'ogg'),
          FileTypeNode(id: 'wma', name: 'wma'),
        ],
      ),
      FileTypeNode(
        id: 'audio_other',
        name: '音频其他',
        selected: true,
        children: [
          FileTypeNode(id: 'cue', name: 'cue'),
          FileTypeNode(id: 'lrc', name: 'lrc'),
        ],
      ),
      FileTypeNode(
        id: 'image',
        name: '图片',
        selected: true,
        children: [
          FileTypeNode(id: 'jpg', name: 'jpg'),
          FileTypeNode(id: 'jpeg', name: 'jpeg'),
          FileTypeNode(id: 'png', name: 'png'),
          FileTypeNode(id: 'gif', name: 'gif'),
          FileTypeNode(id: 'bmp', name: 'bmp'),
          FileTypeNode(id: 'webp', name: 'webp'),
          FileTypeNode(id: 'svg', name: 'svg'),
          FileTypeNode(id: 'ico', name: 'ico'),
          FileTypeNode(id: 'tif', name: 'tif'),
          FileTypeNode(id: 'tiff', name: 'tiff'),
        ],
      ),
      FileTypeNode(
        id: 'video',
        name: '视频',
        selected: true,
        children: [
          FileTypeNode(id: 'mp4', name: 'mp4'),
          FileTypeNode(id: 'mkv', name: 'mkv'),
          FileTypeNode(id: 'avi', name: 'avi'),
          FileTypeNode(id: 'mov', name: 'mov'),
          FileTypeNode(id: 'wmv', name: 'wmv'),
          FileTypeNode(id: 'flv', name: 'flv'),
          FileTypeNode(id: 'webm', name: 'webm'),
          FileTypeNode(id: 'ts', name: 'ts'),
        ],
      ),
      FileTypeNode(
        id: 'document',
        name: '文档',
        selected: true,
        children: [
          FileTypeNode(id: 'txt', name: 'txt'),
          FileTypeNode(id: 'pdf', name: 'pdf'),
          FileTypeNode(id: 'doc', name: 'doc'),
          FileTypeNode(id: 'docx', name: 'docx'),
          FileTypeNode(id: 'xls', name: 'xls'),
          FileTypeNode(id: 'xlsx', name: 'xlsx'),
          FileTypeNode(id: 'ppt', name: 'ppt'),
          FileTypeNode(id: 'pptx', name: 'pptx'),
          FileTypeNode(id: 'md', name: 'md'),
          FileTypeNode(id: 'csv', name: 'csv'),
        ],
      ),
      FileTypeNode(
        id: 'archive',
        name: '压缩包',
        selected: true,
        children: [
          FileTypeNode(id: 'zip', name: 'zip'),
          FileTypeNode(id: 'rar', name: 'rar'),
          FileTypeNode(id: '7z', name: '7z'),
          FileTypeNode(id: 'tar', name: 'tar'),
          FileTypeNode(id: 'gz', name: 'gz'),
          FileTypeNode(id: 'iso', name: 'iso'),
          FileTypeNode(id: 'jar', name: 'jar'),
        ],
      ),
      FileTypeNode(
        id: 'code',
        name: '代码',
        selected: true,
        children: [
          FileTypeNode(id: 'java', name: 'java'),
          FileTypeNode(id: 'c', name: 'c'),
          FileTypeNode(id: 'cpp', name: 'cpp'),
          FileTypeNode(id: 'py', name: 'py'),
          FileTypeNode(id: 'js', name: 'js'),
          FileTypeNode(id: 'html', name: 'html'),
          FileTypeNode(id: 'css', name: 'css'),
          FileTypeNode(id: 'json', name: 'json'),
          FileTypeNode(id: 'xml', name: 'xml'),
          FileTypeNode(id: 'sql', name: 'sql'),
          FileTypeNode(id: 'sh', name: 'sh'),
          FileTypeNode(id: 'bat', name: 'bat'),
        ],
      ),
      FileTypeNode(
        id: 'program',
        name: '程序',
        selected: true,
        children: [
          FileTypeNode(id: 'exe', name: 'exe'),
          FileTypeNode(id: 'msi', name: 'msi'),
          FileTypeNode(id: 'bat', name: 'bat'),
          FileTypeNode(id: 'cmd', name: 'cmd'),
          FileTypeNode(id: 'sh', name: 'sh'),
          FileTypeNode(id: 'app', name: 'app'),
        ],
      ),
    ];
  }

  void _updateNodeSelection(FileTypeNode node, bool? value) {
    setState(() {
      _updateChildren(node, value ?? !node.selected);
    });
  }

  void _updateChildren(FileTypeNode node, bool selected) {
    node.selected = selected;
    node.indeterminate = false;

    for (var child in node.children) {
      _updateChildren(child, selected);
    }

    _updateParentSelection(node);
  }

  void _updateParentSelection(FileTypeNode node) {
    final parent = _findParent(node);
    if (parent != null) {
      final allSelected = parent.children.every((child) => child.selected);
      final anySelected = parent.children.any((child) => child.selected || child.indeterminate);

      parent.selected = allSelected;
      parent.indeterminate = anySelected && !allSelected;

      _updateParentSelection(parent);
    }
  }

  FileTypeNode? _findParent(FileTypeNode node) {
    for (var category in _fileTypeTree) {
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

  Set<String> getSelectedExtensions() {
    Set<String> extensions = {};

    for (var category in _fileTypeTree) {
      if (category.id == 'folder' && category.selected) {
        extensions.add('[DIR]');
      } else if (category.id == 'file' && category.selected) {
        extensions.add('[FILE]');
      } else if (category.selected || category.indeterminate) {
        for (var child in category.children) {
          if (child.selected) {
            extensions.add(child.id);
          }
        }
      }
    }

    return extensions;
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(8),
        color: Colors.white,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: Colors.grey.shade100,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(8),
                topRight: Radius.circular(8),
              ),
            ),
            child: Row(
              children: [
                Checkbox(
                  value: _fileTypeTree.every((node) => node.selected),
                  tristate: true,
                  onChanged: (value) {
                    setState(() {
                      for (var node in _fileTypeTree) {
                        _updateChildren(node, value ?? true);
                      }
                    });
                  },
                ),
                const SizedBox(width: 8),
                const Text(
                  '所有文件类型',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
              ],
            ),
          ),
          Expanded(
            child: ListView.builder(
              shrinkWrap: true,
              itemCount: _fileTypeTree.length,
              itemBuilder: (context, index) {
                return _buildCategoryNode(_fileTypeTree[index]);
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCategoryNode(FileTypeNode category) {
    return ExpansionTile(
      key: ValueKey(category.id),
      initiallyExpanded: false,
      title: Row(
        children: [
          Checkbox(
            value: category.selected,
            tristate: category.indeterminate,
            onChanged: (value) {
              _updateNodeSelection(category, value);
            },
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              category.name,
              style: const TextStyle(fontWeight: FontWeight.w500),
            ),
          ),
        ],
      ),
      children: category.children.map((child) {
        if (child.isLeaf) {
          return CheckboxListTile(
            key: ValueKey(child.id),
            title: Text(child.name),
            value: child.selected,
            onChanged: (value) {
              _updateNodeSelection(child, value);
            },
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
                  onChanged: (value) {
                    _updateNodeSelection(child, value);
                  },
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    child.name,
                    style: const TextStyle(fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
            children: child.children.map((leaf) {
              return CheckboxListTile(
                key: ValueKey(leaf.id),
                title: Text(leaf.name),
                value: leaf.selected,
                onChanged: (value) {
                  _updateNodeSelection(leaf, value);
                },
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
