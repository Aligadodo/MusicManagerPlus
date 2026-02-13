import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dart:async';
import 'dart:convert';
import '../api/api_client.dart';
import '../api/pipeline_service.dart';
import '../api/source_directory_service.dart';
import '../models/change_record.dart';
import '../models/source_directory.dart';
import '../models/strategy_info.dart';
import '../utils/tooltip_utils.dart';
import '../utils/theme_utils.dart';

// 导入main.dart中的taskStateProvider
import '../main.dart';

// 保留本地TaskState枚举，用于内部状态管理
enum LocalTaskState {
  ready,
  previewing,
  previewCompleted,
  previewFailed,
  executing,
  executionCompleted,
  executionFailed,
  cancelled,
}

class PreviewPage extends ConsumerStatefulWidget {
  const PreviewPage({super.key});

  @override
  ConsumerState<PreviewPage> createState() => _PreviewPageState();
}

class _PreviewPageState extends ConsumerState<PreviewPage> {
  final PipelineService _pipelineService = PipelineService(ApiClient());
  final SourceDirectoryService _sourceDirectoryService = SourceDirectoryService(ApiClient());
  final ApiClient _apiClient = ApiClient();
  List<ChangeRecord> _changeRecords = [];
  List<SourceDirectory> _sourceDirectories = [];
  List<StrategyInfo> _pipeline = [];
  bool _isLoading = false;
  String _errorMessage = '';

  String _searchFilter = '';
  String _statusFilter = '全部';
  String _operationTypeFilter = '全部';
  bool _hideUnchanged = true;

  int _currentPage = 1;
  int _pageSize = 20;
  int _totalRecords = 0;
  int _totalPages = 0;

  LocalTaskState _taskState = LocalTaskState.ready;
  String _taskId = '';
  int _progress = 0;
  String _remainingTime = '';
  String _currentStep = '';
  String _message = '';
  bool _hasChanges = false;
  int _changeCount = 0;
  int _scannedFiles = 0;
  int _totalFiles = 0;
  String _logMessage = '';

  Timer? _statusTimer;
  Timer? _progressTimer;
  Timer? _remainingTimeTimer;

  bool _isStatusBarExpanded = false;
  bool _isLogMessageExpanded = false;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _statusTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final directories = await _sourceDirectoryService.getSourceDirectories();
      final pipeline = await _pipelineService.getPipeline();

      setState(() {
        _sourceDirectories = directories;
        _pipeline = pipeline;
        _isLoading = false;
      });
    } catch (e) {
      print('加载数据失败: $e');
      setState(() {
        _errorMessage = '加载数据失败: $e';
        _isLoading = false;
      });
    }
  }

  bool _validateConfiguration() {
    if (_sourceDirectories.isEmpty) {
      _showError('请先添加源目录');
      return false;
    }

    if (_pipeline.isEmpty) {
      _showError('请先配置插件流水线');
      return false;
    }

    return true;
  }

  bool _validatePipelineParameters() {
    for (int i = 0; i < _pipeline.length; i++) {
      final strategy = _pipeline[i];
      
      for (final field in strategy.configFields!) {
        if (field.required && (field.defaultValue == null || field.defaultValue!.isEmpty)) {
          _showError('策略 "${strategy.name}" 的参数 "${field.label}" 是必填项，请配置');
          return false;
        }
      }
        }

    return true;
  }

  void _showError(String message) {
    print('错误: $message');
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  void _showSuccess(String message) {
    print('成功: $message');
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: Colors.green,
        ),
      );
    }
  }

  Future<void> _analyzePipeline() async {
    if (!_validateConfiguration()) {
      return;
    }

    if (!_validatePipelineParameters()) {
      return;
    }

    // 更新全局任务状态
    final taskNotifier = ref.read(taskStateProvider.notifier);
    taskNotifier.startAnalyzing();

    setState(() {
      _taskState = LocalTaskState.previewing;
      _errorMessage = '';
      _progress = 0;
      _remainingTime = '计算中...';
      _currentStep = '初始化预览任务';
      _message = '开始分析流水线...';
      _hasChanges = false;
      _changeCount = 0;
      _scannedFiles = 0;
      _totalFiles = 0;
      _logMessage = '';
    });

    try {
      final sourcePaths = _sourceDirectories.map((d) => d.path).toList();
      final result = await _pipelineService.analyzePipeline(sourcePaths, _pipeline);

      if (result['success'] == true) {
        _taskId = result['taskId'];
        _showSuccess(result['message'] ?? '分析任务已开始执行');
        
        await _fetchChanges();
        _startStatusTimer();
      } else {
        setState(() {
          _taskState = LocalTaskState.previewFailed;
          _errorMessage = result['message'] ?? '分析任务提交失败';
        });
        _showError(_errorMessage);
        taskNotifier.error(_errorMessage);
      }
    } catch (e) {
      print('分析流水线失败: $e');
      setState(() {
        _taskState = LocalTaskState.previewFailed;
        _errorMessage = '分析流水线失败: $e';
      });
      _showError(_errorMessage);
      taskNotifier.error(_errorMessage);
    }
  }

  Future<void> _executePipeline() async {
    if (_taskState != LocalTaskState.previewCompleted) {
      _showError('请先完成预览分析');
      return;
    }

    if (!_hasChanges) {
      _showError('没有需要执行的变更');
      return;
    }

    if (!_validateConfiguration()) {
      return;
    }

    if (!_validatePipelineParameters()) {
      return;
    }

    setState(() {
      _taskState = LocalTaskState.executing;
      _errorMessage = '';
      _progress = 0;
      _remainingTime = '计算中...';
      _currentStep = '初始化执行任务';
      _message = '开始执行流水线...';
      _scannedFiles = 0;
      _totalFiles = 0;
      _logMessage = '';
    });

    try {
      final sourcePaths = _sourceDirectories.map((d) => d.path).toList();
      final result = await _pipelineService.executePipeline(sourcePaths, _pipeline);

      if (result['success'] == true) {
        _taskId = result['taskId'];
        _showSuccess(result['message'] ?? '执行任务已开始执行');
        
        // 更新全局任务状态
        final taskNotifier = ref.read(taskStateProvider.notifier);
        taskNotifier.startRunning(_taskId);
        
        await _fetchChanges();
        _startStatusTimer();
      } else {
        setState(() {
          _taskState = LocalTaskState.executionFailed;
          _errorMessage = result['message'] ?? '执行任务提交失败';
        });
        _showError(_errorMessage);
        
        // 更新全局任务状态
        final taskNotifier = ref.read(taskStateProvider.notifier);
        taskNotifier.error(_errorMessage);
      }
    } catch (e) {
      print('执行流水线失败: $e');
      setState(() {
        _taskState = LocalTaskState.executionFailed;
        _errorMessage = '执行流水线失败: $e';
      });
      _showError(_errorMessage);
      
      // 更新全局任务状态
      final taskNotifier = ref.read(taskStateProvider.notifier);
      taskNotifier.error(_errorMessage);
    }
  }

  Future<void> _stopPipeline() async {
    try {
      await _pipelineService.stopPipeline();
      setState(() {
        _taskState = LocalTaskState.cancelled;
        _message = '任务已中止';
      });
      _stopStatusTimer();
      _showSuccess('任务已成功中止');
      
      // 更新全局任务状态
      final taskNotifier = ref.read(taskStateProvider.notifier);
      taskNotifier.stop();
      taskNotifier.stopComplete();
    } catch (e) {
      print('停止任务失败: $e');
      _showError('停止任务失败: $e');
      
      // 更新全局任务状态
      final taskNotifier = ref.read(taskStateProvider.notifier);
      taskNotifier.error('停止任务失败: $e');
    }
  }

  void _startStatusTimer() {
    _stopStatusTimer();
    _statusTimer = Timer.periodic(const Duration(seconds: 2), (timer) async {
      await _fetchStatus();
      await _fetchChanges();
    });
  }

  void _stopStatusTimer() {
    _statusTimer?.cancel();
    _statusTimer = null;
  }

  Future<void> _fetchStatus() async {
    try {
      final status = await _pipelineService.getPipelineStatus();
      setState(() {
        _progress = (status['progress'] as num?)?.toInt() ?? 0;
        _remainingTime = status['remainingTime']?.toString() ?? '00:00:00';
        _currentStep = status['currentStep']?.toString() ?? '';
        _message = status['message']?.toString() ?? '';
        _hasChanges = status['hasChanges'] as bool? ?? false;
        _changeCount = (status['changeCount'] as num?)?.toInt() ?? 0;
        _scannedFiles = (status['scannedFiles'] as num?)?.toInt() ?? 0;
        _totalFiles = (status['totalFiles'] as num?)?.toInt() ?? 0;
        _logMessage = status['logMessage']?.toString() ?? '';

        final statusStr = status['status']?.toString();
        if (statusStr == '预览完成') {
          _taskState = LocalTaskState.previewCompleted;
        } else if (statusStr == '预览失败') {
          _taskState = LocalTaskState.previewFailed;
        } else if (statusStr == '执行完成') {
          _taskState = LocalTaskState.executionCompleted;
        } else if (statusStr == '执行失败') {
          _taskState = LocalTaskState.executionFailed;
        } else if (statusStr == '已中止') {
          _taskState = LocalTaskState.cancelled;
        }
      });

      // 更新全局任务状态
      final taskNotifier = ref.read(taskStateProvider.notifier);
      if (_taskState.isRunning) {
        taskNotifier.updateProgress(_progress, _message);
      } else if (_taskState.isCompleted) {
        taskNotifier.complete();
      } else if (_taskState.isFailed) {
        taskNotifier.error(_errorMessage);
      }

      if (_taskState.isCompleted || _taskState.isFailed || _taskState == LocalTaskState.cancelled) {
        _stopStatusTimer();
      }
        } catch (e) {
      print('获取状态信息失败: $e');
    }
  }

  Future<void> _fetchChanges() async {
    try {
      final response = await _apiClient.get('/pipeline/changes', queryParams: {
        'searchFilter': _searchFilter,
        'statusFilter': _statusFilter,
        'operationTypeFilter': _operationTypeFilter,
        'hideUnchanged': _hideUnchanged.toString(),
        'page': _currentPage.toString(),
        'size': _pageSize.toString(),
      });

      if (response.statusCode == 200) {
        final jsonResponse = json.decode(response.body);
        setState(() {
          _changeRecords = (jsonResponse['data'] as List)
              .map((e) => ChangeRecord.fromJson(e))
              .toList();
          _totalRecords = (jsonResponse['total'] as num?)?.toInt() ?? 0;
          _totalPages = (_totalRecords / _pageSize).ceil();
        });
      }
    } catch (e) {
      print('获取变更记录失败: $e');
    }
  }

  List<ChangeRecord> get _filteredRecords {
    return _changeRecords;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          children: [
            _buildFilterBar(),
            const SizedBox(height: 12),
            Expanded(
              child: Row(
                children: [
                  Expanded(
                    child: _buildPreviewTable(),
                  ),
                ],
              ),
            ),
            _buildStatusBar(),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterBar() {
    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Tooltip(
            message: ParameterDescriptions.previewPage['search']!,
            child: SizedBox(
              width: 200,
              child: TextField(
                decoration: const InputDecoration(
                  labelText: '搜索',
                  hintText: '搜索文件...',
                  prefixIcon: Icon(Icons.search),
                  border: OutlineInputBorder(),
                  contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                ),
                onChanged: (value) {
                  setState(() {
                    _searchFilter = value;
                    _currentPage = 1;
                  });
                  _fetchChanges();
                },
              ),
            ),
          ),
          const SizedBox(width: 12),
          Tooltip(
            message: ParameterDescriptions.previewPage['statusFilter']!,
            child: Row(
              children: [
                const Text('状态:', style: TextStyle(fontSize: 12)),
                const SizedBox(width: 4),
                DropdownButton<String>(
                  value: _statusFilter,
                  items: const [
                    DropdownMenuItem(value: '全部', child: Text('全部')),
                    DropdownMenuItem(value: 'PENDING', child: Text('待执行')),
                    DropdownMenuItem(value: 'SUCCESS', child: Text('成功')),
                    DropdownMenuItem(value: 'FAILED', child: Text('失败')),
                    DropdownMenuItem(value: 'SKIPPED', child: Text('跳过')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _statusFilter = value ?? '全部';
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  style: const TextStyle(fontSize: 12),
                  iconSize: 20,
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Tooltip(
            message: ParameterDescriptions.previewPage['operationTypeFilter']!,
            child: Row(
              children: [
                const Text('操作类型:', style: TextStyle(fontSize: 12)),
                const SizedBox(width: 4),
                DropdownButton<String>(
                  value: _operationTypeFilter,
                  items: const [
                    DropdownMenuItem(value: '全部', child: Text('全部')),
                    DropdownMenuItem(value: 'RENAME', child: Text('重命名')),
                    DropdownMenuItem(value: 'MOVE', child: Text('移动')),
                    DropdownMenuItem(value: 'DELETE', child: Text('删除')),
                    DropdownMenuItem(value: 'COPY', child: Text('复制')),
                    DropdownMenuItem(value: 'METADATA_UPDATE', child: Text('元数据')),
                    DropdownMenuItem(value: 'CONVERT', child: Text('转换')),
                    DropdownMenuItem(value: 'UNZIP', child: Text('解压')),
                    DropdownMenuItem(value: 'FIX_TYPE', child: Text('修复类型')),
                    DropdownMenuItem(value: 'DEDUP', child: Text('去重')),
                    DropdownMenuItem(value: 'SPLIT', child: Text('分割')),
                    DropdownMenuItem(value: 'ALBUM_RENAME', child: Text('专辑重命名')),
                    DropdownMenuItem(value: 'MERGE', child: Text('合并')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _operationTypeFilter = value ?? '全部';
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  style: const TextStyle(fontSize: 12),
                  iconSize: 20,
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Tooltip(
            message: ParameterDescriptions.previewPage['hideUnchanged']!,
            child: Row(
              children: [
                Checkbox(
                  value: _hideUnchanged,
                  onChanged: (value) {
                    setState(() {
                      _hideUnchanged = value ?? true;
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  visualDensity: VisualDensity.compact,
                ),
                const Text('仅显示变更', style: TextStyle(fontSize: 12)),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Row(
            children: [
              const Text('分页:', style: TextStyle(fontSize: 12)),
              const SizedBox(width: 4),
              Tooltip(
                message: ParameterDescriptions.previewPage['pageSize']!,
                child: DropdownButton<int>(
                  value: _pageSize,
                  items: const [
                    DropdownMenuItem(value: 10, child: Text('10')),
                    DropdownMenuItem(value: 20, child: Text('20')),
                    DropdownMenuItem(value: 50, child: Text('50')),
                    DropdownMenuItem(value: 100, child: Text('100')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _pageSize = value ?? 20;
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  style: const TextStyle(fontSize: 12),
                  iconSize: 20,
                ),
              ),
            ],
          ),
          const Spacer(),
          Tooltip(
            message: ParameterDescriptions.previewPage['refresh']!,
            child: IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _taskState.isRunning ? null : () {
                _fetchChanges();
              },
              tooltip: '刷新变更记录',
              iconSize: 20,
              color: _taskState.isRunning ? Colors.grey : null,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusBar() {
    return Container(
      padding: const EdgeInsets.all(12.0),
      decoration: ThemeUtils.getCardDecoration(context),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                _getTaskStateIcon(),
                size: 20,
                color: _getTaskStateColor(),
              ),
              const SizedBox(width: 8),
              Text(
                _getTaskStateText(),
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: _getTaskStateColor(),
                ),
              ),
              const Spacer(),
              if (_taskState.isRunning)
                const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
              if (_taskState.isCompleted)
                Icon(
                  Icons.check_circle,
                  size: 20,
                  color: ThemeUtils.getSuccessColor(context),
                ),
              IconButton(
                icon: Icon(
                  _isStatusBarExpanded ? Icons.expand_less : Icons.expand_more,
                  size: 18,
                ),
                onPressed: () {
                  setState(() {
                    _isStatusBarExpanded = !_isStatusBarExpanded;
                  });
                },
                tooltip: _isStatusBarExpanded ? '折叠详情' : '展开详情',
              ),
            ],
          ),
          if (_isStatusBarExpanded)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 8),
                if (_message.isNotEmpty)
                  Text(
                    _message,
                    style: const TextStyle(fontSize: 12),
                  ),
                if (_message.isNotEmpty)
                  const SizedBox(height: 8),
                if (_taskState.isRunning || _taskState.isCompleted)
                  Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('进度: $_progress%', style: const TextStyle(fontSize: 12)),
                            LinearProgressIndicator(
                              value: _progress / 100,
                              backgroundColor: Colors.grey.shade200,
                              valueColor: AlwaysStoppedAnimation<Color>(_getTaskStateColor()),
                              minHeight: 6,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 16),
                      if (_totalFiles > 0)
                        Text('文件: $_scannedFiles/$_totalFiles', style: const TextStyle(fontSize: 12)),
                      if (_totalFiles > 0)
                        const SizedBox(width: 16),
                      if (_changeCount > 0)
                        Text('变更: $_changeCount', style: const TextStyle(fontSize: 12)),
                    ],
                  ),
                if (_currentStep.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 8.0),
                    child: Text(
                      '当前步骤: $_currentStep',
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                  ),
                if (_logMessage.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 8.0),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Text(
                              '日志信息:',
                              style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
                            ),
                            IconButton(
                              icon: Icon(
                                _isLogMessageExpanded ? Icons.expand_less : Icons.expand_more,
                                size: 16,
                              ),
                              onPressed: () {
                                setState(() {
                                  _isLogMessageExpanded = !_isLogMessageExpanded;
                                });
                              },
                              tooltip: _isLogMessageExpanded ? '折叠日志' : '展开日志',
                            ),
                          ],
                        ),
                        if (_isLogMessageExpanded)
                          Container(
                            margin: const EdgeInsets.only(top: 4.0),
                            padding: const EdgeInsets.all(8.0),
                            decoration: BoxDecoration(
                              border: Border.all(color: Colors.grey.shade300),
                              borderRadius: BorderRadius.circular(4),
                              color: Colors.white,
                            ),
                            child: Text(
                              _logMessage,
                              style: const TextStyle(fontSize: 11, fontFamily: 'monospace'),
                              maxLines: 10,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                      ],
                    ),
                  ),
              ],
            ),
        ],
      ),
    );
  }

  IconData _getTaskStateIcon() {
    switch (_taskState) {
      case LocalTaskState.ready:
        return Icons.check_circle_outline;
      case LocalTaskState.previewing:
        return Icons.analytics;
      case LocalTaskState.previewCompleted:
        return Icons.check_circle;
      case LocalTaskState.previewFailed:
        return Icons.error;
      case LocalTaskState.executing:
        return Icons.play_circle;
      case LocalTaskState.executionCompleted:
        return Icons.check_circle;
      case LocalTaskState.executionFailed:
        return Icons.error;
      case LocalTaskState.cancelled:
        return Icons.cancel;
    }
  }

  Color _getTaskStateColor() {
    switch (_taskState) {
      case LocalTaskState.ready:
        return Colors.grey;
      case LocalTaskState.previewing:
        return Colors.blue;
      case LocalTaskState.previewCompleted:
        return Colors.green;
      case LocalTaskState.previewFailed:
        return Colors.red;
      case LocalTaskState.executing:
        return Colors.orange;
      case LocalTaskState.executionCompleted:
        return Colors.green;
      case LocalTaskState.executionFailed:
        return Colors.red;
      case LocalTaskState.cancelled:
        return Colors.orange;
    }
  }

  String _getTaskStateText() {
    switch (_taskState) {
      case LocalTaskState.ready:
        return '准备就绪';
      case LocalTaskState.previewing:
        return '预览中';
      case LocalTaskState.previewCompleted:
        return '预览完成';
      case LocalTaskState.previewFailed:
        return '预览失败';
      case LocalTaskState.executing:
        return '执行中';
      case LocalTaskState.executionCompleted:
        return '执行完成';
      case LocalTaskState.executionFailed:
        return '执行失败';
      case LocalTaskState.cancelled:
        return '已中止';
    }
  }

  Widget _buildPreviewTable() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage.isNotEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_errorMessage, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _loadData,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    if (_changeRecords.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.analytics_outlined, size: 64, color: Colors.grey.shade400),
            const SizedBox(height: 20),
            const Text(
              '暂无变更记录',
              style: TextStyle(fontSize: 18, color: Colors.grey),
            ),
            const SizedBox(height: 10),
            const Text(
              '请先在「任务编排」页面配置流水线，然后点击「分析变更」按钮',
              style: TextStyle(fontSize: 14, color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    return Card(
      elevation: 4,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                const Text(
                  '变更预览',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                if (_taskState.isRunning)
                  const Row(
                    children: [
                      SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                      SizedBox(width: 10),
                      Text('正在处理...'),
                    ],
                  ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Column(
                children: [
                  DataTable(
                    columns: const [
                      DataColumn(label: Text('文件名')),
                      DataColumn(label: Text('新文件名')),
                      DataColumn(label: Text('文件路径')),
                      DataColumn(label: Text('操作类型')),
                      DataColumn(label: Text('状态')),
                    ],
                    rows: _filteredRecords.map((record) {
                      return DataRow(
                        cells: [
                          DataCell(Text(record.originalName ?? '')),
                          DataCell(Text(record.newName ?? '')),
                          DataCell(Text(record.filePath ?? '')),
                          DataCell(Text(record.operationType ?? '')),
                          DataCell(
                            Row(
                              children: [
                                Icon(
                                  _getStatusIcon(record.status ?? ''),
                                  size: 16,
                                  color: _getStatusColor(record.status ?? ''),
                                ),
                                const SizedBox(width: 4),
                                Text(record.status ?? ''),
                              ],
                            ),
                          ),
                        ],
                      );
                    }).toList(),
                  ),
                ],
              ),
            ),
          ),
          _buildPagination(),
        ],
      ),
    );
  }

  Widget _buildPagination() {
    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Row(
        children: [
          Text('共 $_totalRecords 条记录', style: const TextStyle(fontSize: 12)),
          const Spacer(),
          IconButton(
            icon: const Icon(Icons.chevron_left),
            onPressed: _currentPage > 1
                ? () {
                    setState(() {
                      _currentPage--;
                    });
                    _fetchChanges();
                  }
                : null,
          ),
          Text('$_currentPage/$_totalPages', style: const TextStyle(fontSize: 12)),
          IconButton(
            icon: const Icon(Icons.chevron_right),
            onPressed: _currentPage < _totalPages
                ? () {
                    setState(() {
                      _currentPage++;
                    });
                    _fetchChanges();
                  }
                : null,
          ),
        ],
      ),
    );
  }

  IconData _getStatusIcon(String status) {
    switch (status) {
      case 'SUCCESS':
        return Icons.check_circle;
      case 'FAILED':
        return Icons.error;
      case 'PENDING':
        return Icons.pending;
      case 'SKIPPED':
        return Icons.skip_next;
      default:
        return Icons.help;
    }
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'SUCCESS':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'PENDING':
        return Colors.orange;
      case 'SKIPPED':
        return Colors.grey;
      default:
        return Colors.grey;
    }
  }
}

extension on LocalTaskState {
  bool get isRunning => this == LocalTaskState.previewing || this == LocalTaskState.executing;
  bool get isCompleted => this == LocalTaskState.previewCompleted || this == LocalTaskState.executionCompleted;
  bool get isFailed => this == LocalTaskState.previewFailed || this == LocalTaskState.executionFailed;
}