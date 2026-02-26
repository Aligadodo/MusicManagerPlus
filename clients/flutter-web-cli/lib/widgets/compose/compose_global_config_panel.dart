import 'package:flutter/material.dart';
import '../../widgets/settings/thread_pool_settings.dart';
import '../../widgets/settings/run_settings.dart';
import '../../widgets/settings/scan_settings.dart';
import '../../widgets/common/filter_rules.dart';
import '../../widgets/config/file_type_tree.dart';
import '../../utils/theme_utils.dart';

class ComposeGlobalConfigPanel extends StatefulWidget {
  final Map<String, dynamic> globalSettings;
  final Function(Map<String, dynamic>) onGlobalSettingsChanged;

  const ComposeGlobalConfigPanel({
    super.key,
    required this.globalSettings,
    required this.onGlobalSettingsChanged,
  });

  @override
  State<ComposeGlobalConfigPanel> createState() => _ComposeGlobalConfigPanelState();
}

class _ComposeGlobalConfigPanelState extends State<ComposeGlobalConfigPanel> {
  // 线程池配置
  late int _previewThreads;
  late int _executionThreads;
  late String _threadPoolMode;

  // 扫描配置
  late String _recursionMode;
  late int _recursionDepth;
  late int _minRecursionDepth;
  late int _maxRecursionDepth;

  // 过滤规则
  late List<String> _scanFilterList;
  String _newFilterRule = '';

  // 手动输入的文件类型后缀
  late List<String> _customFileTypes;
  String _newFileType = '';

  // 运行配置
  late bool _autoRefresh;
  late int _previewLimit;
  late int _executionLimit;

  // 文件类型树
  late FileTypeNode _fileTypeTree;

  @override
  void initState() {
    super.initState();
    _initializeSettings();
  }

  void _initializeSettings() {
    _previewThreads = widget.globalSettings.getOrDefault('previewThreads', 10);
    _executionThreads = widget.globalSettings.getOrDefault('executionThreads', 4);
    _threadPoolMode = widget.globalSettings.getOrDefault('threadPoolMode', 'GLOBAL');

    _recursionMode = widget.globalSettings.getOrDefault('recursionMode', 'ALL');
    _recursionDepth = widget.globalSettings.getOrDefault('recursionDepth', 3);
    _minRecursionDepth = widget.globalSettings.getOrDefault('minRecursionDepth', 1);
    _maxRecursionDepth = widget.globalSettings.getOrDefault('maxRecursionDepth', 3);

    _scanFilterList = List<String>.from(widget.globalSettings.getOrDefault('scanFilterList', [
      '*Convert*',
      '*Split*',
      '*System*',
      '*trash*',
      '*Temp*',
      '*tmp*',
      '*cache*',
      '*backup*',
    ]));

    _customFileTypes = List<String>.from(widget.globalSettings.getOrDefault('customFileTypes', []));

    _autoRefresh = widget.globalSettings.getOrDefault('autoRefresh', true);
    _previewLimit = widget.globalSettings.getOrDefault('previewLimit', 200);
    _executionLimit = widget.globalSettings.getOrDefault('executionLimit', 1000);

    _fileTypeTree = _initFileTypeTree();
  }

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

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: ThemeUtils.getCardDecoration(context),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.settings, color: ThemeUtils.getPrimaryColor(context), size: 18),
                const SizedBox(width: 8),
                const Text(
                  '全局参数配置',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(width: 8),
                Tooltip(
                  message: '配置任务的全局参数',
                  child: Icon(Icons.help_outline, color: ThemeUtils.getTextSecondaryColor(context), size: 16),
                ),
              ],
            ),
            const SizedBox(height: 16),
            _buildThreadPoolSection(theme),
            const SizedBox(height: 20),
            _buildRunSettingsSection(theme),
            const SizedBox(height: 20),
            _buildScanSettingsSection(theme),
            const SizedBox(height: 20),
            _buildFilterRulesSection(theme),
            const SizedBox(height: 20),
            _buildFileTypeTreeSection(theme),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildThreadPoolSection(ThemeData theme) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.help_outline, color: theme.primaryColor, size: 18),
                const SizedBox(width: 8),
                const Text(
                  '线程池配置',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ThreadPoolSettings(
              previewThreads: _previewThreads,
              executionThreads: _executionThreads,
              threadPoolMode: _threadPoolMode,
              onPreviewThreadsChanged: (value) {
                setState(() {
                  _previewThreads = value;
                });
                _updateGlobalSettings();
              },
              onExecutionThreadsChanged: (value) {
                setState(() {
                  _executionThreads = value;
                });
                _updateGlobalSettings();
              },
              onThreadPoolModeChanged: (value) {
                setState(() {
                  _threadPoolMode = value;
                });
                _updateGlobalSettings();
              },
              theme: theme,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildRunSettingsSection(ThemeData theme) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.run_circle, color: theme.primaryColor, size: 18),
                const SizedBox(width: 8),
                const Text(
                  '运行配置',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            RunSettings(
              autoRefresh: _autoRefresh,
              previewLimit: _previewLimit,
              executionLimit: _executionLimit,
              onAutoRefreshChanged: (value) {
                setState(() {
                  _autoRefresh = value;
                });
                _updateGlobalSettings();
              },
              onPreviewLimitChanged: (value) {
                setState(() {
                  _previewLimit = value;
                });
                _updateGlobalSettings();
              },
              onExecutionLimitChanged: (value) {
                setState(() {
                  _executionLimit = value;
                });
                _updateGlobalSettings();
              },
              theme: theme,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildScanSettingsSection(ThemeData theme) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.search, color: theme.primaryColor, size: 18),
                const SizedBox(width: 8),
                const Text(
                  '扫描配置',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ScanSettings(
              recursionMode: _recursionMode,
              recursionDepth: _recursionDepth,
              minRecursionDepth: _minRecursionDepth,
              maxRecursionDepth: _maxRecursionDepth,
              onRecursionModeChanged: (value) {
                setState(() {
                  _recursionMode = value;
                });
                _updateGlobalSettings();
              },
              onRecursionDepthChanged: (value) {
                setState(() {
                  _recursionDepth = value;
                });
                _updateGlobalSettings();
              },
              onMinRecursionDepthChanged: (value) {
                setState(() {
                  _minRecursionDepth = value;
                });
                _updateGlobalSettings();
              },
              onMaxRecursionDepthChanged: (value) {
                setState(() {
                  _maxRecursionDepth = value;
                });
                _updateGlobalSettings();
              },
              theme: theme,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterRulesSection(ThemeData theme) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.filter_list, color: theme.primaryColor, size: 18),
                const SizedBox(width: 8),
                const Text(
                  '过滤规则',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            FilterRules(
              filterList: _scanFilterList,
              newFilterRule: _newFilterRule,
              onNewFilterRuleChanged: (value) {
                setState(() {
                  _newFilterRule = value;
                });
              },
              onAddFilterRule: () {
                if (_newFilterRule.isNotEmpty && !_scanFilterList.contains(_newFilterRule)) {
                  setState(() {
                    _scanFilterList.add(_newFilterRule);
                    _newFilterRule = '';
                  });
                  _updateGlobalSettings();
                }
              },
              onRemoveFilterRule: (index) {
                setState(() {
                  _scanFilterList.removeAt(index);
                });
                _updateGlobalSettings();
              },
              theme: theme,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFileTypeTreeSection(ThemeData theme) {
    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.file_copy, color: theme.primaryColor, size: 18),
                const SizedBox(width: 8),
                const Text(
                  '文件类型筛选',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            FileTypeTree(
              fileTypeTree: _fileTypeTree,
              customFileTypes: _customFileTypes,
              newFileType: _newFileType,
              onNodeSelectionChanged: (node, value) {
                setState(() {
                  FileTypeNode.updateNodeSelection(node, value, _fileTypeTree);
                });
                _updateGlobalSettings();
              },
              onSelectAll: (node, isSelected) {
                setState(() {
                  FileTypeNode.selectAll(node, isSelected);
                });
                _updateGlobalSettings();
              },
              onNewFileTypeChanged: (value) {
                setState(() {
                  _newFileType = value;
                });
              },
              onAddCustomFileType: () {
                if (_newFileType.isNotEmpty) {
                  final types = _newFileType.split(',')
                      .map((t) => t.trim())
                      .where((t) => t.isNotEmpty)
                      .toList();
                  setState(() {
                    _customFileTypes.addAll(types);
                    _newFileType = '';
                  });
                  _updateGlobalSettings();
                }
              },
              onRemoveCustomFileType: (type) {
                setState(() {
                  _customFileTypes.remove(type);
                });
                _updateGlobalSettings();
              },
              theme: theme,
            ),
          ],
        ),
      ),
    );
  }

  // 更新全局设置
  void _updateGlobalSettings() {
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

    widget.onGlobalSettingsChanged(config);
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
}

// 扩展 Map 类，添加 getOrDefault 方法
extension MapExtension on Map<String, dynamic> {
  T getOrDefault<T>(String key, T defaultValue) {
    if (containsKey(key)) {
      var value = this[key];
      if (value is T) {
        return value;
      }
    }
    return defaultValue;
  }
}
