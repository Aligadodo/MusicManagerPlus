import 'dart:convert';
import 'dart:html' as html;
import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/source_directory.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/widgets/directory_list_item.dart';
import 'package:filemanager_flutter/widgets/selectable_text_widget.dart';
import 'package:filemanager_flutter/utils/theme_utils.dart';

class ComposeDirectoryPanel extends StatefulWidget {
  final List<SourceDirectory> sourceDirectories;
  final Function(List<SourceDirectory>) onDirectoriesChanged;
  final SourceDirectoryService sourceDirectoryService;

  const ComposeDirectoryPanel({
    super.key,
    required this.sourceDirectories,
    required this.onDirectoriesChanged,
    required this.sourceDirectoryService,
  });

  @override
  State<ComposeDirectoryPanel> createState() => _ComposeDirectoryPanelState();
}

class _ComposeDirectoryPanelState extends State<ComposeDirectoryPanel> {
  bool _isDisposed = false;

  @override
  void dispose() {
    _isDisposed = true;
    super.dispose();
  }

  Future<void> _addDirectory() async {
    try {
      showDialog(
        context: context,
        builder: (BuildContext context) {
          final TextEditingController pathController = TextEditingController();
          return AlertDialog(
            title: const Text('添加目录'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  '选择添加方式：',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                ElevatedButton.icon(
                  onPressed: () {
                    Navigator.of(context).pop();
                    _selectDirectoryByFilePicker();
                  },
                  icon: const Icon(Icons.folder_open, size: 20),
                  label: const Text('通过文件选择器选择'),
                  style: ElevatedButton.styleFrom(
                    minimumSize: const Size(double.infinity, 48),
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  '（仅获取目录路径，不会上传文件）',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 12),
                const Text(
                  '或手动输入路径：',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: pathController,
                  decoration: const InputDecoration(
                    labelText: '目录路径',
                    hintText: '例如: /Users/username/Music',
                    border: OutlineInputBorder(),
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop();
                },
                child: const Text('取消'),
              ),
              TextButton(
                onPressed: () {
                  final path = pathController.text.trim();
                  Navigator.of(context).pop();
                  if (path.isNotEmpty) {
                    _doAddDirectory(path);
                  }
                },
                child: const Text('确定'),
              ),
            ],
          );
        },
      );
    } catch (e) {
      print('选择目录失败: $e');
      if (!_isDisposed) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: SelectableTextWidget(text: '选择目录失败: $e')),
        );
      }
    }
  }

  Future<void> _selectDirectoryByFilePicker() async {
    try {
      final input = html.InputElement(type: 'file')
        ..attributes['webkitdirectory'] = 'true'
        ..attributes['directory'] = 'true'
        ..multiple = false;

      input.onChange.listen((event) {
        if (input.files?.isNotEmpty == true) {
          final file = input.files![0];
          String path = '';
          
          if (file.relativePath != null && file.relativePath!.isNotEmpty) {
            final firstSlashIndex = file.relativePath!.indexOf('/');
            if (firstSlashIndex != -1) {
              path = file.relativePath!.substring(0, firstSlashIndex);
            } else {
              path = file.name;
            }
          } else {
            path = file.name;
          }
          
          if (path.isNotEmpty) {
            _doAddDirectory(path);
          }
        }
      });

      input.click();
    } catch (e) {
      print('文件选择器选择目录失败: $e');
      if (!_isDisposed) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: SelectableTextWidget(text: '文件选择器选择目录失败: $e')),
        );
      }
    }
  }

  Future<void> _doAddDirectory(String path) async {
    try {
      final directory = SourceDirectory(path: path, threadCount: 4);
      await widget.sourceDirectoryService.addSourceDirectory(directory);
      final sources = await widget.sourceDirectoryService.getSourceDirectories();
      if (!_isDisposed) {
        widget.onDirectoriesChanged(sources);
      }
    } catch (e) {
      print('添加目录失败: $e');
      if (!_isDisposed) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: SelectableTextWidget(text: '添加目录失败: $e')),
        );
      }
    }
  }

  void _removeDirectory(SourceDirectory directory) {
    final newDirectories = List<SourceDirectory>.from(widget.sourceDirectories);
    newDirectories.remove(directory);
    widget.onDirectoriesChanged(newDirectories);
  }

  Future<void> _clearDirectories() async {
    try {
      await widget.sourceDirectoryService.clearSourceDirectories();
      if (!_isDisposed) {
        widget.onDirectoriesChanged([]);
      }
    } catch (e) {
      print('清空目录失败: $e');
      if (!_isDisposed) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: SelectableTextWidget(text: '清空目录失败: $e')),
        );
      }
    }
  }

  void _moveDirectory(int index, int direction) {
    final newIndex = index + direction;
    if (newIndex >= 0 && newIndex < widget.sourceDirectories.length) {
      final newDirectories = List<SourceDirectory>.from(widget.sourceDirectories);
      final directory = newDirectories.removeAt(index);
      newDirectories.insert(newIndex, directory);
      widget.onDirectoriesChanged(newDirectories);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: ThemeUtils.getCardDecoration(context),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildHeader(),
          const SizedBox(height: 10),
          _buildTools(),
          const SizedBox(height: 10),
          Expanded(
            child: DragTarget<String>(
              onAccept: (path) {
                _doAddDirectory(path);
              },
              onWillAccept: (data) => true,
              onLeave: (data) {},
              builder: (context, candidateData, rejectedData) {
                return Stack(
                  children: [
                    _buildSourceList(),
                    if (candidateData.isNotEmpty)
                      Container(
                        color: ThemeUtils.getPrimaryColor(context).withOpacity(0.2),
                        child: Center(
                          child: Text(
                            '释放以添加目录',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: ThemeUtils.getPrimaryColor(context),
                            ),
                          ),
                        ),
                      ),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Icon(Icons.folder_open, color: ThemeUtils.getPrimaryColor(context), size: 18),
        const SizedBox(width: 8),
        const Text(
          '源目录配置',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 15,
            color: Colors.black87,
          ),
        ),
        const SizedBox(width: 8),
        Tooltip(
          message: '添加要处理的源目录',
          child: Icon(Icons.help_outline, color: ThemeUtils.getTextSecondaryColor(context), size: 16),
        ),
      ],
    );
  }

  Widget _buildTools() {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        ElevatedButton.icon(
          onPressed: _addDirectory,
          icon: const Icon(Icons.add, size: 16),
          label: const Text('添加目录', style: TextStyle(fontSize: 12)),
          style: ElevatedButton.styleFrom(
            backgroundColor: ThemeUtils.getPrimaryColor(context),
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ),
        const SizedBox(width:8),
        ElevatedButton.icon(
          onPressed: _clearDirectories,
          icon: const Icon(Icons.clear, size: 16),
          label: const Text('清空', style: TextStyle(fontSize: 12)),
          style: ElevatedButton.styleFrom(
            backgroundColor: ThemeUtils.getErrorColor(context),
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSourceList() {
    if (widget.sourceDirectories.isEmpty) {
      return Container(
        alignment: Alignment.center,
        child: Text(
          '暂无源目录',
          style: TextStyle(
            color: ThemeUtils.getTextSecondaryColor(context),
            fontSize: 13,
          ),
        ),
      );
    }

    return ListView.builder(
      itemCount: widget.sourceDirectories.length,
      itemBuilder: (context, index) {
        final directory = widget.sourceDirectories[index];
        return DirectoryListItem(
          directory: directory,
          index: index,
          onMoveUp: index > 0 ? () => _moveDirectory(index, -1) : () {},
          onMoveDown: index < widget.sourceDirectories.length - 1 ? () => _moveDirectory(index, 1) : () {},
          onDelete: () => _removeDirectory(directory),
        );
      },
    );
  }
}
