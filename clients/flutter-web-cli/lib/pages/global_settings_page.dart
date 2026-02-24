import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/config_service.dart';
import '../api/api_client.dart';
import '../widgets/thread_pool_settings.dart';
import '../widgets/scan_settings.dart';
import '../widgets/filter_rules.dart';
import '../widgets/file_type_tree.dart';
import '../widgets/run_settings.dart';

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
    final theme = Theme.of(context);
    final primaryColor = theme.primaryColor;
    final backgroundColor = theme.scaffoldBackgroundColor;
    final textColor = theme.textTheme.bodyLarge?.color ?? Colors.black;
    final borderColor = theme.dividerColor;
    final cardColor = theme.cardColor;

    return Scaffold(
      appBar: AppBar(
        title: const Text(''),
      ),
      body: Row(
        children: [
          Container(
            width: 200,
            color: theme.colorScheme.surfaceContainer,
            child: ListView(
              children: [
                _buildNavItem('线程池配置', 0, primaryColor, theme),
                _buildNavItem('运行配置', 1, primaryColor, theme),
                _buildNavItem('扫描配置', 2, primaryColor, theme),
                _buildNavItem('过滤规则', 3, primaryColor, theme),
                _buildNavItem('文件类型筛选', 4, primaryColor, theme),
              ],
            ),
          ),
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(20.0),
              child: ListView(
                children: [
                  if (_selectedSection == 0)
                    _buildThreadPoolSection(theme),
                  if (_selectedSection ==1)
                    _buildRunSettingsSection(theme),
                  if (_selectedSection == 2)
                    _buildScanSettingsSection(theme),
                  if (_selectedSection == 3)
                    _buildFilterRulesSection(theme),
                  if (_selectedSection == 4)
                    _buildFileTypeTreeSection(theme),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem(String title, int index, Color primaryColor, ThemeData theme) {
    return InkWell(
      onTap: () {
        setState(() {
          _selectedSection = index;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: _selectedSection == index ? primaryColor.withOpacity(0.1) : Colors.transparent,
          border: Border(
            left: BorderSide(
              color: _selectedSection == index ? primaryColor : Colors.transparent,
              width: 4,
            ),
          ),
        ),
        child: Text(
          title,
          style: TextStyle(
            color: _selectedSection == index ? primaryColor : theme.textTheme.bodyLarge?.color,
            fontWeight: _selectedSection == index ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  int _selectedSection = 0;

  Widget _buildThreadPoolSection(ThemeData theme) {
    return ThreadPoolSettings(
      previewThreads: _previewThreads,
      executionThreads: _executionThreads,
      threadPoolMode: _threadPoolMode,
      onPreviewThreadsChanged: (value) {
        setState(() {
          _previewThreads = value;
        });
        _autoSaveConfig();
      },
      onExecutionThreadsChanged: (value) {
        setState(() {
          _executionThreads = value;
        });
        _autoSaveConfig();
      },
      onThreadPoolModeChanged: (value) {
        setState(() {
          _threadPoolMode = value;
        });
        _autoSaveConfig();
      },
      theme: theme,
    );
  }

  Widget _buildScanSettingsSection(ThemeData theme) {
    return ScanSettings(
      recursionMode: _recursionMode,
      recursionDepth: _recursionDepth,
      minRecursionDepth: _minRecursionDepth,
      maxRecursionDepth: _maxRecursionDepth,
      onRecursionModeChanged: (value) {
        setState(() {
          _recursionMode = value;
        });
        _autoSaveConfig();
      },
      onRecursionDepthChanged: (value) {
        setState(() {
          _recursionDepth = value;
        });
        _autoSaveConfig();
      },
      onMinRecursionDepthChanged: (value) {
        setState(() {
          _minRecursionDepth = value;
        });
        _autoSaveConfig();
      },
      onMaxRecursionDepthChanged: (value) {
        setState(() {
          _maxRecursionDepth = value;
        });
        _autoSaveConfig();
      },
      theme: theme,
    );
  }

  Widget _buildFilterRulesSection(ThemeData theme) {
    return FilterRules(
      filterList: _scanFilterList,
      newFilterRule: _newFilterRule,
      onNewFilterRuleChanged: (value) {
        _newFilterRule = value;
      },
      onAddFilterRule: () {
        if (_newFilterRule.isNotEmpty && !_scanFilterList.contains(_newFilterRule)) {
          setState(() {
            _scanFilterList.add(_newFilterRule);
            _newFilterRule = '';
          });
          _autoSaveConfig();
        }
      },
      onRemoveFilterRule: (index) {
        setState(() {
          _scanFilterList.removeAt(index);
        });
        _autoSaveConfig();
      },
      theme: theme,
    );
  }

  Widget _buildFileTypeTreeSection(ThemeData theme) {
    return FileTypeTree(
      fileTypeTree: _fileTypeTree,
      customFileTypes: _customFileTypes,
      newFileType: _newFileType,
      onNodeSelectionChanged: (node, value) {
        setState(() {
          FileTypeNode.updateNodeSelection(node, value, _fileTypeTree);
        });
        _autoSaveConfig();
      },
      onSelectAll: (node, isSelected) {
        setState(() {
          FileTypeNode.selectAll(node, isSelected);
        });
        _autoSaveConfig();
      },
      onNewFileTypeChanged: (value) {
        _newFileType = value;
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
          _autoSaveConfig();
        }
      },
      onRemoveCustomFileType: (type) {
        setState(() {
          _customFileTypes.remove(type);
        });
        _autoSaveConfig();
      },
      theme: theme,
    );
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

  Widget _buildRunSettingsSection(ThemeData theme) {
    return RunSettings(
      autoRefresh: _autoRefresh,
      previewLimit: _previewLimit,
      executionLimit: _executionLimit,
      onAutoRefreshChanged: (value) {
        setState(() {
          _autoRefresh = value;
        });
        _autoSaveConfig();
      },
      onPreviewLimitChanged: (value) {
        setState(() {
          _previewLimit = value;
        });
        _autoSaveConfig();
      },
      onExecutionLimitChanged: (value) {
        setState(() {
          _executionLimit = value;
        });
        _autoSaveConfig();
      },
      theme: theme,
    );
  }
}
