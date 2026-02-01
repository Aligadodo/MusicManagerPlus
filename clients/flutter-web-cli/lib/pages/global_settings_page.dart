import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

// 文件类型树形结构
class FileTypeNode {
  final String name;
  final String? extension;
  final List<FileTypeNode> children;
  bool isSelected;

  FileTypeNode({
    required this.name,
    this.extension,
    required this.children,
    this.isSelected = false,
  });
}

class GlobalSettingsPage extends ConsumerStatefulWidget {
  const GlobalSettingsPage({super.key});

  @override
  ConsumerState<GlobalSettingsPage> createState() => _GlobalSettingsPageState();
}

class _GlobalSettingsPageState extends ConsumerState<GlobalSettingsPage> {
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
  List<String> _scanFilterList = [
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

  // 初始化文件类型树
  FileTypeNode _initFileTypeTree() {
    return FileTypeNode(
      name: '所有文件类型',
      children: [
        FileTypeNode(
          name: '音频文件',
          children: [
            FileTypeNode(name: 'MP3', extension: 'mp3', children: []),
            FileTypeNode(name: 'WAV', extension: 'wav', children: []),
            FileTypeNode(name: 'FLAC', extension: 'flac', children: []),
            FileTypeNode(name: 'AAC', extension: 'aac', children: []),
            FileTypeNode(name: 'WMA', extension: 'wma', children: []),
            FileTypeNode(name: 'OGG', extension: 'ogg', children: []),
          ],
        ),
        FileTypeNode(
          name: '视频文件',
          children: [
            FileTypeNode(name: 'MP4', extension: 'mp4', children: []),
            FileTypeNode(name: 'MKV', extension: 'mkv', children: []),
            FileTypeNode(name: 'AVI', extension: 'avi', children: []),
            FileTypeNode(name: 'MOV', extension: 'mov', children: []),
            FileTypeNode(name: 'WMV', extension: 'wmv', children: []),
            FileTypeNode(name: 'FLV', extension: 'flv', children: []),
          ],
        ),
        FileTypeNode(
          name: '图片文件',
          children: [
            FileTypeNode(name: 'JPG/JPEG', extension: 'jpg', children: []),
            FileTypeNode(name: 'PNG', extension: 'png', children: []),
            FileTypeNode(name: 'GIF', extension: 'gif', children: []),
            FileTypeNode(name: 'WebP', extension: 'webp', children: []),
            FileTypeNode(name: 'BMP', extension: 'bmp', children: []),
            FileTypeNode(name: 'TIFF', extension: 'tiff', children: []),
          ],
        ),
        FileTypeNode(
          name: '文档文件',
          children: [
            FileTypeNode(name: 'PDF', extension: 'pdf', children: []),
            FileTypeNode(name: 'DOC/DOCX', extension: 'doc', children: []),
            FileTypeNode(name: 'XLS/XLSX', extension: 'xls', children: []),
            FileTypeNode(name: 'PPT/PPTX', extension: 'ppt', children: []),
            FileTypeNode(name: 'TXT', extension: 'txt', children: []),
            FileTypeNode(name: 'MD', extension: 'md', children: []),
          ],
        ),
        FileTypeNode(
          name: '压缩文件',
          children: [
            FileTypeNode(name: 'ZIP', extension: 'zip', children: []),
            FileTypeNode(name: 'RAR', extension: 'rar', children: []),
            FileTypeNode(name: '7Z', extension: '7z', children: []),
            FileTypeNode(name: 'GZ', extension: 'gz', children: []),
            FileTypeNode(name: 'TAR', extension: 'tar', children: []),
          ],
        ),
        FileTypeNode(
          name: '其他文件',
          children: [
            FileTypeNode(name: 'EXE', extension: 'exe', children: []),
            FileTypeNode(name: 'DLL', extension: 'dll', children: []),
            FileTypeNode(name: 'ISO', extension: 'iso', children: []),
            FileTypeNode(name: 'IMG', extension: 'img', children: []),
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

  // 预览配置
  bool _autoRefresh = true;
  int _previewLimit = 200;

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
                _buildNavItem('扫描配置', 1),
                _buildNavItem('过滤规则', 2),
                _buildNavItem('文件类型筛选', 3),
                _buildNavItem('预览配置', 4),
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
                  if (_selectedSection == 1) ...[
                    _buildScanSettingsSection(),
                    const SizedBox(height: 30),
                  ],
                  if (_selectedSection == 2) ...[
                    _buildFilterRulesSection(),
                    const SizedBox(height: 30),
                  ],
                  if (_selectedSection == 3) ...[
                    _buildFileTypeTreeSection(),
                    const SizedBox(height: 30),
                  ],
                  if (_selectedSection == 4) ...[
                    _buildPreviewSettingsSection(),
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
            const Text(
              '文件类型筛选',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Container(
              height: 400,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey),
                borderRadius: BorderRadius.circular(8),
              ),
              child: SingleChildScrollView(
                child: _buildFileTypeTreeNode(_fileTypeTree, 0),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                ElevatedButton(
                  onPressed: () {
                    // 全选
                    _selectAll(_fileTypeTree, true);
                    setState(() {});
                  },
                  child: const Text('全选'),
                ),
                ElevatedButton(
                  onPressed: () {
                    // 取消全选
                    _selectAll(_fileTypeTree, false);
                    setState(() {});
                  },
                  child: const Text('取消全选'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // 递归构建树形节点
  Widget _buildFileTypeTreeNode(FileTypeNode node, int level) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          margin: EdgeInsets.only(left: level * 20.0),
          child: Row(
            children: [
              Checkbox(
                value: node.isSelected,
                onChanged: (value) {
                  setState(() {
                    node.isSelected = value ?? false;
                    // 递归更新子节点
                    _updateChildrenSelection(node, value ?? false);
                  });
                },
              ),
              Text(node.name),
              if (node.extension != null)
                Padding(
                  padding: const EdgeInsets.only(left: 10),
                  child: Text(
                    '(.${node.extension})',
                    style: TextStyle(color: Colors.grey),
                  ),
                ),
            ],
          ),
        ),
        if (node.children.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(left: 32.0, top: 8.0),
            child: Wrap(
              spacing: 16.0,
              runSpacing: 12.0,
              children: node.children.map((child) {
                return _buildFileTypeNodeInline(child);
              }).toList(),
            ),
          ),
      ],
    );
  }

  // 内联构建文件类型节点（用于横向展示）
  Widget _buildFileTypeNodeInline(FileTypeNode node) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 4.0),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(6.0),
        color: node.isSelected ? Colors.blue.shade50 : Colors.white,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Checkbox(
            value: node.isSelected,
            onChanged: (value) {
              setState(() {
                node.isSelected = value ?? false;
              });
            },
          ),
          const SizedBox(width: 4.0),
          Text(
            node.name,
            style: TextStyle(
              fontSize: 13.0,
              color: node.isSelected ? Colors.blue.shade700 : Colors.black87,
            ),
          ),
          if (node.extension != null) ...[
            const SizedBox(width: 4.0),
            Text(
              '(.${node.extension})',
              style: TextStyle(
                fontSize: 11.0,
                color: Colors.grey.shade600,
              ),
            ),
          ],
        ],
      ),
    );
  }

  // 更新子节点选择状态
  void _updateChildrenSelection(FileTypeNode node, bool isSelected) {
    for (var child in node.children) {
      child.isSelected = isSelected;
      _updateChildrenSelection(child, isSelected);
    }
  }

  // 全选/取消全选
  void _selectAll(FileTypeNode node, bool isSelected) {
    node.isSelected = isSelected;
    for (var child in node.children) {
      _selectAll(child, isSelected);
    }
  }

  Widget _buildPreviewSettingsSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '预览配置',
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
                    },
                    divisions: 19,
                    label: '$_previewLimit',
                  ),
                ),
                Text('$_previewLimit'),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
