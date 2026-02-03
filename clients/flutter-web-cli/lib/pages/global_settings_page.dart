import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/config_service.dart';
import '../api/api_client.dart';

// 文件类型树形结构
class FileTypeNode {
  String id;
  String name;
  List<String> extensions;
  List<FileTypeNode> children;
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

class GlobalSettingsPage extends ConsumerStatefulWidget {
  const GlobalSettingsPage({super.key});

  @override
  ConsumerState<GlobalSettingsPage> createState() => _GlobalSettingsPageState();
}

class _GlobalSettingsPageState extends ConsumerState<GlobalSettingsPage> {
  final ConfigService _configService = ConfigService(ApiClient());

  // 线程池配置
  int _previewThreads = 10;
  int _executionThreads = 4;
  String _threadPoolMode = 'GLOBAL'; // GLOBAL 或 ROOT_PATH

  // 扫描配置
  String _recursionMode = 'ALL'; // ALL, CURRENT, SPECIFIC, RANGE
  int _recursionDepth = 3;
  int _minRecursionDepth = 1;
  int _maxRecursionDepth = 3;

  // 过滤规则
  final List<String> _scanFilterList = [
    '*Convert*',
    '*Split*',
    '*System*',
    '*trash*',
    '*Temp*',
    '*tmp*',
    '*cache*',
    '*backup*',
  ];
  String _newFilterRule = '';

  // 手动输入的文件类型后缀
  final List<String> _customFileTypes = [];
  String _newFileType = '';

  // 初始化文件类型树
  FileTypeNode _initFileTypeTree() {
    return FileTypeNode(
      id: 'root',
      name: '所有文件类型',
      selected: true,
      children: [
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
      ],
    );
  }

  late FileTypeNode _fileTypeTree;

  @override
  void initState() {
    super.initState();
    _fileTypeTree = _initFileTypeTree();
  }

  // 运行配置
  bool _autoRefresh = true;
  int _previewLimit = 200;
  int _executionLimit = 1000;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('全局设置'),
      ),
      body: Row(
        children: [
          Container(
            width: 200,
            color: Colors.grey.shade100,
            child: ListView(
              children: [
                _buildNavItem('线程池配置', 0),
                _buildNavItem('运行配置', 1),
                _buildNavItem('扫描配置', 2),
                _buildNavItem('过滤规则', 3),
                _buildNavItem('文件类型筛选', 4),
              ],
            ),
          ),
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(20.0),
              child: ListView(
                children: [
                  if (_selectedSection == 0) ...[
                    _buildThreadPoolSection(),
                    const SizedBox(height: 30),
                  ],
                  if (_selectedSection ==1) ...[
                    _buildRunSettingsSection(),
                    const SizedBox(height: 30),
                  ],
                  if (_selectedSection == 2) ...[
                    _buildScanSettingsSection(),
                    const SizedBox(height: 30),
                  ],
                  if (_selectedSection == 3) ...[
                    _buildFilterRulesSection(),
                    const SizedBox(height: 30),
                  ],
                  if (_selectedSection == 4) ...[
                    _buildFileTypeTreeSection(),
                    const SizedBox(height: 30),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem(String title, int index) {
    return InkWell(
      onTap: () {
        setState(() {
          _selectedSection = index;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: _selectedSection == index ? Colors.blue.shade100 : Colors.transparent,
          border: Border(
            left: BorderSide(
              color: _selectedSection == index ? Colors.blue : Colors.transparent,
              width: 4,
            ),
          ),
        ),
        child: Text(
          title,
          style: TextStyle(
            color: _selectedSection == index ? Colors.blue.shade700 : Colors.black87,
            fontWeight: _selectedSection == index ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  int _selectedSection = 0;

  Widget _buildThreadPoolSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '线程池配置',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('preview_threads_row'),
              children: [
                const Text('预览线程数:'),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 1,
                    max: 16,
                    value: _previewThreads.toDouble(),
                    onChanged: (value) {
                      setState(() {
                        _previewThreads = value.toInt();
                      });
                      _autoSaveConfig();
                    },
                    divisions: 15,
                    label: '$_previewThreads',
                  ),
                ),
                Text('$_previewThreads'),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('execution_threads_row'),
              children: [
                const Text('执行线程数:'),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 1,
                    max: 12,
                    value: _executionThreads.toDouble(),
                    onChanged: (value) {
                      setState(() {
                        _executionThreads = value.toInt();
                      });
                      _autoSaveConfig();
                    },
                    divisions: 11,
                    label: '$_executionThreads',
                  ),
                ),
                Text('$_executionThreads'),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('thread_pool_mode_row'),
              children: [
                const Text('线程池模式:'),
                const SizedBox(width: 20),
                DropdownButton<String>(
                  value: _threadPoolMode,
                  items: const [
                    DropdownMenuItem(value: 'GLOBAL', child: Text('全局统一配置')),
                    DropdownMenuItem(value: 'ROOT_PATH', child: Text('根路径独立配置')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _threadPoolMode = value ?? 'GLOBAL';
                    });
                    _autoSaveConfig();
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildScanSettingsSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '扫描配置',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('scan_mode_row'),
              children: [
                const Text('扫描模式:'),
                const SizedBox(width: 20),
                DropdownButton<String>(
                  value: _recursionMode,
                  items: const [
                    DropdownMenuItem(value: 'ALL', child: Text('全部文件')),
                    DropdownMenuItem(value: 'CURRENT', child: Text('当前目录')),
                    DropdownMenuItem(value: 'SPECIFIC', child: Text('指定目录层级')),
                    DropdownMenuItem(value: 'RANGE', child: Text('目录层级范围')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _recursionMode = value ?? 'ALL';
                    });
                    _autoSaveConfig();
                  },
                ),
              ],
            ),
            if (_recursionMode == 'SPECIFIC')
              Padding(
                padding: const EdgeInsets.only(left: 120, top: 16),
                child: Row(
                  children: [
                    const Text('扫描层级:'),
                    const SizedBox(width: 20),
                    Expanded(
                      child: Slider(
                        min: 1,
                        max: 10,
                        value: _recursionDepth.toDouble(),
                        onChanged: (value) {
                          setState(() {
                            _recursionDepth = value.toInt();
                          });
                          _autoSaveConfig();
                        },
                        divisions: 9,
                        label: '$_recursionDepth',
                      ),
                    ),
                    Text('$_recursionDepth'),
                  ],
                ),
              ),
            if (_recursionMode == 'RANGE')
              Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(left: 120, top: 16),
                    child: Row(
                      key: const ValueKey('recursion_depth_row'),
                      children: [
                        const Text('最小层级:'),
                        const SizedBox(width: 20),
                        Expanded(
                          child: Slider(
                            min: 1,
                            max: 10,
                            value: _minRecursionDepth.toDouble(),
                            onChanged: (value) {
                              setState(() {
                                _minRecursionDepth = value.toInt();
                              });
                              _autoSaveConfig();
                            },
                            divisions: 9,
                            label: '$_minRecursionDepth',
                          ),
                        ),
                        Text('$_minRecursionDepth'),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(left: 120, top: 16),
                    child: Row(
                      key: const ValueKey('max_recursion_depth_row'),
                      children: [
                        const Text('最大层级:'),
                        const SizedBox(width: 20),
                        Expanded(
                          child: Slider(
                            min: _minRecursionDepth.toDouble(),
                            max: 10,
                            value: _maxRecursionDepth.toDouble(),
                            onChanged: (value) {
                              setState(() {
                                _maxRecursionDepth = value.toInt();
                              });
                              _autoSaveConfig();
                            },
                            divisions: 10 - _minRecursionDepth,
                            label: '$_maxRecursionDepth',
                          ),
                        ),
                        Text('$_maxRecursionDepth'),
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

  Widget _buildFilterRulesSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '过滤规则',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('add_filter_rule_row'),
              children: [
                Expanded(
                  child: TextField(
                    controller: TextEditingController(text: _newFilterRule),
                    onChanged: (value) {
                      _newFilterRule = value;
                    },
                    decoration: const InputDecoration(
                      labelText: '添加过滤规则',
                      hintText: '例如：*Convert*',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    if (_newFilterRule.isNotEmpty && !_scanFilterList.contains(_newFilterRule)) {
                      setState(() {
                        _scanFilterList.add(_newFilterRule);
                        _newFilterRule = '';
                      });
                      _autoSaveConfig();
                    }
                  },
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_scanFilterList.isNotEmpty)
              Column(
                children: [
                  const Text('当前过滤规则:'),
                  const SizedBox(height: 8),
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: ListView.builder(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: _scanFilterList.length,
                      itemBuilder: (context, index) {
                        String rule = _scanFilterList[index];
                        return Row(
                          key: ValueKey('scan_filter_rule_row_$index'),
                          children: [
                            Expanded(
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Text(rule),
                              ),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete, color: Colors.red),
                              onPressed: () {
                                setState(() {
                                  _scanFilterList.removeAt(index);
                                });
                                _autoSaveConfig();
                              },
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

  // 构建文件类型树形组件
  Widget _buildFileTypeTreeSection() {
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
                const Text(
                  '文件类型筛选',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Row(
                  children: [
                    TextButton(
                      onPressed: () {
                        setState(() {
                          _selectAll(_fileTypeTree, true);
                        });
                      },
                      child: const Text('全选'),
                    ),
                    TextButton(
                      onPressed: () {
                        setState(() {
                          _selectAll(_fileTypeTree, false);
                        });
                      },
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
                border: Border.all(color: Colors.grey),
                borderRadius: BorderRadius.circular(8),
              ),
              child: ListView(
                children: _fileTypeTree.children.map((category) {
                  return _buildCategoryNode(category);
                }).toList(),
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              '手动添加文件类型后缀',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: const InputDecoration(
                      labelText: '输入文件类型后缀',
                      hintText: '例如：mp3,flac,wav',
                      border: OutlineInputBorder(),
                      contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    ),
                    onChanged: (value) {
                      _newFileType = value;
                    },
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    if (_newFileType.isNotEmpty) {
                      final types = _newFileType.split(',')
                          .map((t) => t.trim())
                          .where((t) => t.isNotEmpty)
                          .toList();
                      setState(() {
                        _customFileTypes.addAll(types);
                        _newFileType = '';
                      });
                      _autoSaveConfig();
                    }
                  },
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_customFileTypes.isNotEmpty)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '自定义文件类型:',
                    style: TextStyle(fontSize: 12, color: Colors.grey),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: _customFileTypes.map((type) {
                      return Chip(
                        label: Text(type),
                        onDeleted: () {
                          setState(() {
                            _customFileTypes.remove(type);
                          });
                          _autoSaveConfig();
                        },
                        deleteIcon: const Icon(Icons.close, size: 16),
                        deleteIconColor: Colors.red,
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

  // 构建类别节点
  Widget _buildCategoryNode(FileTypeNode category) {
    if (category.isLeaf) {
      return CheckboxListTile(
        key: ValueKey(category.id),
        title: Text(category.name),
        value: category.selected,
        onChanged: (value) {
          setState(() {
            _updateNodeSelection(category, value);
          });
          _autoSaveConfig();
        },
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
            onChanged: (value) {
              setState(() {
                _updateNodeSelection(category, value);
              });
              _autoSaveConfig();
            },
          ),
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
              setState(() {
                _updateNodeSelection(child, value);
              });
              _autoSaveConfig();
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
                    setState(() {
                      _updateNodeSelection(child, value);
                    });
                    _autoSaveConfig();
                  },
                ),
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
                  setState(() {
                    _updateNodeSelection(leaf, value);
                  });
                  _autoSaveConfig();
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

  // 更新节点选择状态
  void _updateNodeSelection(FileTypeNode node, bool? value) {
    bool newValue = value ?? !node.selected;
    _updateChildren(node, newValue);
    _updateParentSelection(node);
  }

  // 更新子节点选择状态
  void _updateChildren(FileTypeNode node, bool selected) {
    node.selected = selected;
    node.indeterminate = false;

    for (var child in node.children) {
      _updateChildren(child, selected);
    }
  }

  // 向上更新父节点的选择状态
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

  // 查找父节点
  FileTypeNode? _findParent(FileTypeNode node) {
    for (var category in _fileTypeTree.children) {
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

  // 全选/取消全选
  void _selectAll(FileTypeNode node, bool isSelected) {
    node.selected = isSelected;
    node.indeterminate = false;
    for (var child in node.children) {
      _selectAll(child, isSelected);
    }
  }

  // 自动保存配置
  void _autoSaveConfig() {
    try {
      final config = {
        'previewThreads': _previewThreads,
        'executionThreads': _executionThreads,
        'threadPoolMode': _threadPoolMode,
        'autoRefresh': _autoRefresh,
        'previewLimit': _previewLimit,
        'executionLimit': _executionLimit,
        'recursionMode': _recursionMode,
        'recursionDepth': _recursionDepth,
        'minRecursionDepth': _minRecursionDepth,
        'maxRecursionDepth': _maxRecursionDepth,
        'scanFilterList': _scanFilterList,
        'customFileTypes': _customFileTypes,
        'selectedFileTypes': _getSelectedFileTypes(),
      };

      _configService.saveConfig(config).catchError((e) {
        print('自动保存配置失败: $e');
      });
    } catch (e) {
      print('构建配置对象失败: $e');
    }
  }

  // 获取选中的文件类型
  Map<String, dynamic> _getSelectedFileTypes() {
    Map<String, dynamic> selectedTypes = {};
    
    for (var category in _fileTypeTree.children) {
      if (category.id == 'folder' || category.id == 'file') {
        selectedTypes[category.id] = category.selected;
      } else if (category.selected || category.indeterminate) {
        List<String> selectedExtensions = [];
        for (var child in category.children) {
          if (child.selected) {
            selectedExtensions.add(child.id);
          }
        }
        selectedTypes[category.id] = selectedExtensions;
      }
    }
    
    return selectedTypes;
  }

  Widget _buildRunSettingsSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '运行配置',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('auto_refresh_row'),
              children: [
                Checkbox(
                  value: _autoRefresh,
                  onChanged: (value) {
                    setState(() {
                      _autoRefresh = value ?? true;
                    });
                    _autoSaveConfig();
                  },
                ),
                const Text('自动刷新'),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('preview_limit_row'),
              children: [
                const Text('预览数量限制:'),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 50,
                    max: 1000,
                    value: _previewLimit.toDouble(),
                    onChanged: (value) {
                      setState(() {
                        _previewLimit = value.toInt();
                      });
                      _autoSaveConfig();
                    },
                    divisions: 19,
                    label: '$_previewLimit',
                  ),
                ),
                Text('$_previewLimit'),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('execution_limit_row'),
              children: [
                const Text('执行数量限制:'),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 100,
                    max: 5000,
                    value: _executionLimit.toDouble(),
                    onChanged: (value) {
                      setState(() {
                        _executionLimit = value.toInt();
                      });
                      _autoSaveConfig();
                    },
                    divisions: 49,
                    label: '$_executionLimit',
                  ),
                ),
                Text('$_executionLimit'),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
