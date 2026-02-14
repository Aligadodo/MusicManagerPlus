import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dart:async';
import 'dart:convert';
import '../api/api_client.dart';
import '../api/pipeline_service.dart';
import '../api/source_directory_service.dart';
import '../api/task_service.dart';
import '../models/change_record.dart';
import '../models/source_directory.dart';
import '../models/strategy_info.dart';
import '../models/task_status.dart' as task_models;
import '../utils/tooltip_utils.dart';
import '../utils/theme_utils.dart';

// 导入main.dart中的taskStateProvider
import '../main.dart' as main_app;

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

enum ViewMode {
  taskList,
  taskDetail,
  preview,
}

class PreviewPage extends ConsumerStatefulWidget {
  const PreviewPage({super.key});

  @override
  ConsumerState<PreviewPage> createState() => _PreviewPageState();
}

class _PreviewPageState extends ConsumerState<PreviewPage> {
  final PipelineService _pipelineService = PipelineService(ApiClient());
  final SourceDirectoryService _sourceDirectoryService = SourceDirectoryService(ApiClient());
  final TaskService _taskService = TaskService(ApiClient());
  final ApiClient _apiClient = ApiClient();
  
  ViewMode _viewMode = ViewMode.taskList;
  task_models.TaskStatus? _selectedTask;
  
  List<ChangeRecord> _changeRecords = [];
  List<SourceDirectory> _sourceDirectories = [];
  List<StrategyInfo> _pipeline = [];
  List<task_models.TaskStatus> _tasks = [];
  
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
  Timer? _refreshTimer;

  bool _isStatusBarExpanded = false;
  bool _isLogMessageExpanded = false;

  @override
  void initState() {
    super.initState();
    _loadTasks();
    _startAutoRefresh();
    _checkGlobalTaskState();
  }

  void _checkGlobalTaskState() {
    final taskState = ref.read(main_app.taskStateProvider);
    
    if (taskState.status == main_app.TaskStatus.analyzing) {
      _createNewTask();
    } else if (taskState.status == main_app.TaskStatus.running) {
      _executeLatestTask();
    }
  }

  Future<void> _createNewTask() async {
    await _loadData();
    await _analyzePipeline();
  }

  Future<void> _executeLatestTask() async {
    if (_tasks.isEmpty) {
      _showErrorSnackBar('没有可执行的任务');
      return;
    }
    
    final latestTask = _tasks.first;
    setState(() {
      _selectedTask = latestTask;
      _viewMode = ViewMode.taskDetail;
    });
    
    if (latestTask.status == 'PREVIEWED') {
      await _executeTask();
    } else {
      _showErrorSnackBar('最新任务状态为 ${latestTask.status}，无法执行');
    }
  }

  @override
  void dispose() {
    _statusTimer?.cancel();
    _progressTimer?.cancel();
    _remainingTimeTimer?.cancel();
    _refreshTimer?.cancel();
    super.dispose();
  }

  void _startAutoRefresh() {
    _refreshTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      if (_viewMode == ViewMode.taskList) {
        _refreshTasks();
      } else if (_viewMode == ViewMode.taskDetail && _selectedTask != null) {
        _refreshTaskDetail();
      }
    });
  }

  String? _mapStatusToApi(String status) {
    switch (status) {
      case '等待中':
        return 'PENDING';
      case '进行中':
        return 'SCANNING,SCANNED,PREVIEWING,EXECUTING';
      case '已完成':
        return 'SCANNED,PREVIEWED,EXECUTED';
      case '失败':
        return 'FAILED';
      case '已取消':
        return 'CANCELLED';
      default:
        return null;
    }
  }

  Future<void> _loadTasks() async {
    if (_isLoading) return;

    setState(() {
      _isLoading = true;
      _currentPage = 1;
    });

    try {
      final result = await _taskService.getTaskList(
        page: _currentPage,
        size: 20,
        status: _mapStatusToApi(_statusFilter),
      );

      final data = result['data'] as Map<String, dynamic>?;
      final tasks = (data?['list'] as List<dynamic>?)?.map((json) => task_models.TaskStatus.fromJson(json as Map<String, dynamic>)).toList() ?? [];

      setState(() {
        _tasks = tasks;
        final total = data?['total'] as int?;
        _totalPages = total != null ? (total / 20).ceil() : 1;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      _showErrorSnackBar('加载任务列表失败: $e');
    }
  }

  Future<void> _refreshTasks() async {
    try {
      final result = await _taskService.getTaskList(
        page: 1,
        size: _tasks.length,
        status: _mapStatusToApi(_statusFilter),
      );

      final data = result['data'] as Map<String, dynamic>?;
      final tasks = (data?['list'] as List<dynamic>?)?.map((json) => task_models.TaskStatus.fromJson(json as Map<String, dynamic>)).toList() ?? [];

      if (mounted) {
        setState(() {
          _tasks = tasks;
        });
      }
    } catch (e) {
      if (mounted) {
        _showErrorSnackBar('刷新任务列表失败: $e');
      }
    }
  }

  Future<void> _refreshTaskDetail() async {
    if (_selectedTask == null) return;
    
    try {
      final taskInfo = await _taskService.getTaskInfo(_selectedTask!.taskId!);
      if (mounted) {
        setState(() {
          _selectedTask = taskInfo;
        });
      }
    } catch (e) {
      if (mounted) {
        _showErrorSnackBar('刷新任务信息失败: $e');
      }
    }
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

  void _showErrorSnackBar(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: Colors.red,
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
    final taskNotifier = ref.read(main_app.taskStateProvider.notifier);
    taskNotifier.startAnalyzing();

    setState(() {
      _taskState = LocalTaskState.previewing;
      _errorMessage = '';
      _progress = 0;
      _remainingTime = '计算中...';
      _currentStep = '初始化预览任务';
      _message = '开始分析流水线...';
      _logMessage = '';
    });

    try {
      final sourceDirectories = _sourceDirectories.map((d) => d.path).toList();
      final result = await _pipelineService.analyzePipeline(sourceDirectories, _pipeline);

      if (result['success'] == true) {
        _showSuccess(result['message'] ?? '分析任务已开始');
        await _fetchChanges();
        await _refreshTasks(); // 刷新任务列表，确保预览任务显示
        
        setState(() {
          _taskState = LocalTaskState.previewCompleted;
          _message = '预览分析完成';
        });
      } else {
        _showError(result['message'] ?? '分析失败');
        await _refreshTasks(); // 即使分析失败，也刷新任务列表
        setState(() {
          _taskState = LocalTaskState.previewFailed;
          _errorMessage = result['message'] ?? '分析失败';
        });
      }
    } catch (e) {
      print('分析流水线失败: $e');
      _showError('分析流水线失败: $e');
      await _refreshTasks(); // 即使发生异常，也刷新任务列表
      setState(() {
        _taskState = LocalTaskState.previewFailed;
        _errorMessage = '分析流水线失败: $e';
      });
    }
  }

  Future<void> _executeTask() async {
    if (!_validateConfiguration()) {
      return;
    }

    // 更新全局任务状态
    final taskNotifier = ref.read(main_app.taskStateProvider.notifier);
    taskNotifier.startRunning(_taskId);

    setState(() {
      _taskState = LocalTaskState.executing;
      _errorMessage = '';
      _progress = 0;
      _remainingTime = '计算中...';
      _currentStep = '初始化执行任务';
      _message = '开始执行流水线...';
      _logMessage = '';
    });

    try {
      final sourceDirectories = _sourceDirectories.map((d) => d.path).toList();
      final result = await _pipelineService.executePipeline(sourceDirectories, _pipeline);

      if (result['success'] == true) {
        _showSuccess(result['message'] ?? '执行任务已开始');
        await _fetchChanges();
        
        setState(() {
          _taskState = LocalTaskState.executionCompleted;
          _message = '执行完成';
        });
      } else {
        _showError(result['message'] ?? '执行失败');
        setState(() {
          _taskState = LocalTaskState.executionFailed;
          _errorMessage = result['message'] ?? '执行失败';
        });
      }
    } catch (e) {
      print('执行流水线失败: $e');
      _showError('执行流水线失败: $e');
      setState(() {
        _taskState = LocalTaskState.executionFailed;
        _errorMessage = '执行流水线失败: $e';
      });
    }
  }

  Future<void> _stopTask() async {
    setState(() {
      _message = '正在停止任务...';
    });

    try {
      final result = await _apiClient.post('/api/tasks/stop');
      
      if (result.statusCode == 200) {
        final data = jsonDecode(result.body);
        if (data['success'] == true) {
          _showSuccess(data['message'] ?? '任务已停止');
          
          // 更新全局任务状态
          final taskNotifier = ref.read(main_app.taskStateProvider.notifier);
          taskNotifier.stopComplete();
          
          setState(() {
            _taskState = LocalTaskState.ready;
            _message = '任务已停止';
          });
        } else {
          _showError(data['message'] ?? '停止任务失败');
        }
      } else {
        _showError('停止任务失败: ${result.statusCode}');
      }
    } catch (e) {
      print('停止任务失败: $e');
      _showError('停止任务失败: $e');
    }
  }

  Future<void> _fetchChanges() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final response = await _apiClient.get('/api/pipeline/changes');
      
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        
        if (data['success'] == true) {
          final changes = (data['changes'] as List<dynamic>)
              .map((json) => ChangeRecord.fromJson(json as Map<String, dynamic>))
              .toList();
          
          setState(() {
            _changeRecords = changes;
            _totalRecords = changes.length;
            _totalPages = (_totalRecords / _pageSize).ceil();
            _hasChanges = changes.isNotEmpty;
            _changeCount = changes.length;
            _isLoading = false;
          });
        } else {
          _showError(data['message'] ?? '获取变更记录失败');
          setState(() {
            _isLoading = false;
          });
        }
      } else {
        _showError('获取变更记录失败: ${response.statusCode}');
        setState(() {
          _isLoading = false;
        });
      }
    } catch (e) {
      print('获取变更记录失败: $e');
      _showError('获取变更记录失败: $e');
      setState(() {
        _isLoading = false;
      });
    }
  }

  List<ChangeRecord> _getCurrentPageRecords() {
    var records = _changeRecords;

    if (_searchFilter.isNotEmpty) {
      records = records.where((record) {
        return (record.originalName?.toLowerCase().contains(_searchFilter.toLowerCase()) ?? false) ||
               (record.newName?.toLowerCase().contains(_searchFilter.toLowerCase()) ?? false) ||
               (record.filePath?.toLowerCase().contains(_searchFilter.toLowerCase()) ?? false);
      }).toList();
    }

    if (_statusFilter != '全部') {
      records = records.where((record) {
        if (_statusFilter == '已修改') return record.changed == true;
        if (_statusFilter == '未修改') return record.changed == false;
        return true;
      }).toList();
    }

    if (_operationTypeFilter != '全部') {
      records = records.where((record) => record.operationType == _operationTypeFilter).toList();
    }

    if (_hideUnchanged) {
      records = records.where((record) => record.changed == true).toList();
    }

    _totalRecords = records.length;
    _totalPages = (_totalRecords / _pageSize).ceil();

    final startIndex = (_currentPage - 1) * _pageSize;
    final endIndex = startIndex + _pageSize;
    
    return records.sublist(
      startIndex,
      endIndex > records.length ? records.length : endIndex,
    );
  }

  void _goToPage(int page) {
    setState(() {
      _currentPage = page;
    });
  }

  Widget _buildCurrentView() {
    switch (_viewMode) {
      case ViewMode.taskList:
        return _buildTaskListView();
      case ViewMode.taskDetail:
        return _buildTaskDetailView();
      case ViewMode.preview:
        return _buildPreviewView();
      default:
        return _buildTaskListView();
    }
  }

  Widget _buildTaskListView() {
    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          _buildTaskListHeader(),
          const SizedBox(height: 12),
          Expanded(
            child: _buildTaskList(),
          ),
        ],
      ),
    );
  }

  Widget _buildTaskListHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '任务列表',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            SizedBox(
              width: 150,
              child: DropdownButtonFormField<String>(
                value: _statusFilter,
                onChanged: (value) {
                  setState(() {
                    _statusFilter = value!;
                    _loadTasks();
                  });
                },
                items: [
                  '全部',
                  '等待中',
                  '进行中',
                  '已完成',
                  '失败',
                  '已取消',
                ].map((status) => DropdownMenuItem<String>(
                  value: status,
                  child: Text(status),
                )).toList(),
                decoration: const InputDecoration(
                  labelText: '状态筛选',
                  border: OutlineInputBorder(),
                  contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                ),
              ),
            ),
            const SizedBox(width: 12),
            ElevatedButton(
              onPressed: _loadTasks,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
              child: const Text('刷新'),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildTaskList() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage.isNotEmpty) {
      return Center(
        child: Text(
          _errorMessage,
          style: const TextStyle(color: Colors.red),
        ),
      );
    }

    if (_tasks.isEmpty) {
      return const Center(
        child: Text('暂无任务记录'),
      );
    }

    return ListView.builder(
      itemCount: _tasks.length,
      itemBuilder: (context, index) {
        final task = _tasks[index];
        return _buildTaskCard(task);
      },
    );
  }

  Widget _buildTaskCard(task_models.TaskStatus task) {
    final createdAt = task.createdAt != null
        ? DateTime.fromMillisecondsSinceEpoch(task.createdAt!)
        : null;

    return Card(
      margin: const EdgeInsets.symmetric(vertical: 6),
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    task.taskName ?? '未命名任务',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                Chip(
                  label: Text(
                    _getFriendlyStatus(task.status ?? 'UNKNOWN'),
                    style: TextStyle(
                      color: _getStatusColor(task.status ?? 'UNKNOWN'),
                    ),
                  ),
                  backgroundColor: _getStatusColor(task.status ?? 'UNKNOWN').withOpacity(0.1),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (createdAt != null)
              Text(
                '创建时间: ${createdAt.toString()}',
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.grey,
                ),
              ),
            if (task.currentStage != null)
              Text(
                '当前阶段: ${task.currentStage}',
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.grey,
                ),
              ),
            if (task.message != null)
              Text(
                '消息: ${task.message}',
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.grey,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () {
                    setState(() {
                      _selectedTask = task;
                      _viewMode = ViewMode.taskDetail;
                    });
                  },
                  child: const Text('查看详情'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _getFriendlyStatus(String status) {
    switch (status) {
      case 'PENDING':
        return '等待中';
      case 'SCANNING':
        return '扫描中';
      case 'SCANNED':
        return '已扫描';
      case 'PREVIEWING':
        return '预览中';
      case 'PREVIEWED':
        return '已预览';
      case 'EXECUTING':
        return '执行中';
      case 'EXECUTED':
        return '已执行';
      case 'FAILED':
        return '失败';
      case 'CANCELLED':
        return '已取消';
      default:
        return '未知状态';
    }
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'PENDING':
        return Colors.yellow;
      case 'SCANNING':
      case 'PREVIEWING':
      case 'EXECUTING':
        return Colors.blue;
      case 'SCANNED':
      case 'PREVIEWED':
      case 'EXECUTED':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'CANCELLED':
        return Colors.grey;
      default:
        return Colors.grey;
    }
  }

  Widget _buildTaskDetailView() {
    if (_selectedTask == null) {
      return const Center(child: Text('请选择一个任务'));
    }

    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          _buildTaskDetailHeader(),
          const SizedBox(height: 12),
          Expanded(
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildTaskInfoCard(),
                  const SizedBox(height: 16),
                  _buildConfigSnapshotCard(),
                  const SizedBox(height: 16),
                  _buildScanResultCard(),
                  const SizedBox(height: 16),
                  _buildPreviewResultCard(),
                  const SizedBox(height: 16),
                  _buildExecutionResultCard(),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTaskDetailHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        const Text(
          '任务详情',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        ElevatedButton(
          onPressed: () {
            setState(() {
              _viewMode = ViewMode.taskList;
            });
          },
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
          ),
          child: const Text('返回任务列表'),
        ),
      ],
    );
  }

  Widget _buildTaskInfoCard() {
    if (_selectedTask == null) return Container();

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '任务基本信息',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Text('任务ID: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Expanded(
                  child: Text(
                    _selectedTask!.taskId ?? 'N/A',
                    style: const TextStyle(fontSize: 13),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('任务名称: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Expanded(
                  child: Text(
                    _selectedTask!.taskName ?? 'N/A',
                    style: const TextStyle(fontSize: 13),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  _getFriendlyStatus(_selectedTask!.status ?? 'UNKNOWN'),
                  style: TextStyle(
                    fontSize: 13,
                    color: _getStatusColor(_selectedTask!.status ?? 'UNKNOWN'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('当前阶段: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  _selectedTask!.currentStage ?? 'N/A',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('总体进度: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${(_selectedTask!.overallProgress ?? 0).toStringAsFixed(1)}%',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (_selectedTask!.createdAt != null)
              Row(
                children: [
                  const Text('创建时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    DateTime.fromMillisecondsSinceEpoch(_selectedTask!.createdAt!).toString(),
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            if (_selectedTask!.message != null)
              const SizedBox(height: 8),
            if (_selectedTask!.message != null)
              Row(
                children: [
                  const Text('消息: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Expanded(
                    child: Text(
                      _selectedTask!.message!,
                      style: const TextStyle(fontSize: 13),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildConfigSnapshotCard() {
    if (_selectedTask == null || _selectedTask!.configSnapshot == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无配置快照信息'),
        ),
      );
    }

    final configSnapshot = _selectedTask!.configSnapshot!;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '配置快照',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            _buildSourceDirectoriesList(configSnapshot.sourceDirectories),
            const SizedBox(height: 16),
            _buildStrategyConfig(configSnapshot.strategyId, configSnapshot.strategyConfig),
          ],
        ),
      ),
    );
  }

  Widget _buildSourceDirectoriesList(List<dynamic>? sourceDirs) {
    if (sourceDirs == null || sourceDirs.isEmpty) {
      return const Text('无源目录配置', style: TextStyle(color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('源目录:', style: TextStyle(fontWeight: FontWeight.w500)),
        const SizedBox(height: 8),
        for (var dir in sourceDirs)
          Padding(
            padding: const EdgeInsets.only(left: 16),
            child: Text(
              dir is Map<String, dynamic> ? dir['path']?.toString() ?? '未知路径' : dir.toString(),
              style: const TextStyle(fontSize: 13),
            ),
          ),
      ],
    );
  }

  Widget _buildStrategyConfig(String? strategyId, Map<String, dynamic>? strategyConfig) {
    if (strategyId == null) {
      return const Text('无策略配置', style: TextStyle(color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('策略ID: $strategyId',
            style: const TextStyle(fontSize: 13)),
        if (strategyConfig != null)
          Text('配置参数: ${strategyConfig.length} 项',
              style: const TextStyle(fontSize: 13)),
      ],
    );
  }

  Widget _buildScanResultCard() {
    if (_selectedTask == null || _selectedTask!.stages?.scan == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无扫描结果信息'),
        ),
      );
    }

    final scanStage = _selectedTask!.stages!.scan!;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '扫描结果',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  scanStage.status ?? 'N/A',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (scanStage.scanStartTime != null)
              Row(
                children: [
                  const Text('开始时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    DateTime.fromMillisecondsSinceEpoch(scanStage.scanStartTime!).toString(),
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            if (scanStage.scanEndTime != null)
              const SizedBox(height: 8),
            if (scanStage.scanEndTime != null)
              Row(
                children: [
                  const Text('结束时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    DateTime.fromMillisecondsSinceEpoch(scanStage.scanEndTime!).toString(),
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('扫描文件数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${scanStage.scannedFiles ?? 0}/${scanStage.totalFiles ?? 0}',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPreviewResultCard() {
    if (_selectedTask == null || _selectedTask!.stages?.preview == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无预览结果信息'),
        ),
      );
    }

    final previewStage = _selectedTask!.stages!.preview!;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '预览结果',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  previewStage.status ?? 'N/A',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (previewStage.previewStartTime != null)
              Row(
                children: [
                  const Text('开始时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    DateTime.fromMillisecondsSinceEpoch(previewStage.previewStartTime!).toString(),
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            if (previewStage.previewEndTime != null)
              const SizedBox(height: 8),
            if (previewStage.previewEndTime != null)
              Row(
                children: [
                  const Text('结束时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    DateTime.fromMillisecondsSinceEpoch(previewStage.previewEndTime!).toString(),
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('分析文件数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${previewStage.analyzedFiles ?? 0}',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('变更总数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${previewStage.totalChanges ?? 0}',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildExecutionResultCard() {
    if (_selectedTask == null || _selectedTask!.stages?.execution == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无执行结果信息'),
        ),
      );
    }

    final executionStage = _selectedTask!.stages!.execution!;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '执行结果',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                const Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  executionStage.status ?? 'N/A',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (executionStage.executionStartTime != null)
              Row(
                children: [
                  const Text('开始时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    DateTime.fromMillisecondsSinceEpoch(executionStage.executionStartTime!).toString(),
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            if (executionStage.executionEndTime != null)
              const SizedBox(height: 8),
            if (executionStage.executionEndTime != null)
              Row(
                children: [
                  const Text('结束时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Text(
                    DateTime.fromMillisecondsSinceEpoch(executionStage.executionEndTime!).toString(),
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('执行文件数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${executionStage.executedFiles ?? 0}',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('成功数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${executionStage.successCount ?? 0}',
                  style: const TextStyle(fontSize: 13, color: Colors.green),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('失败数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${executionStage.failedCount ?? 0}',
                  style: const TextStyle(fontSize: 13, color: Colors.red),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('执行次数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${executionStage.executionCount ?? 0}',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPreviewView() {
    return Container(
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
    );
  }

  Widget _buildFilterBar() {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: TextField(
                decoration: const InputDecoration(
                  labelText: '搜索',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.search),
                ),
                onChanged: (value) {
                  setState(() {
                    _searchFilter = value;
                    _currentPage = 1;
                  });
                },
              ),
            ),
            const SizedBox(width: 12),
            ElevatedButton(
              onPressed: () {
                setState(() {
                  _viewMode = ViewMode.taskList;
                });
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
              child: const Text('查看任务列表'),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                value: _statusFilter,
                onChanged: (value) {
                  setState(() {
                    _statusFilter = value!;
                    _currentPage = 1;
                  });
                },
                items: ['全部', '已修改', '未修改'].map((status) => DropdownMenuItem<String>(
                  value: status,
                  child: Text(status),
                )).toList(),
                decoration: const InputDecoration(
                  labelText: '状态筛选',
                  border: OutlineInputBorder(),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: DropdownButtonFormField<String>(
                value: _operationTypeFilter,
                onChanged: (value) {
                  setState(() {
                    _operationTypeFilter = value!;
                    _currentPage = 1;
                  });
                },
                items: ['全部', '重命名', '移动', '删除', '其他'].map((type) => DropdownMenuItem<String>(
                  value: type,
                  child: Text(type),
                )).toList(),
                decoration: const InputDecoration(
                  labelText: '操作类型',
                  border: OutlineInputBorder(),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Row(
                children: [
                  Checkbox(
                    value: _hideUnchanged,
                    onChanged: (value) {
                      setState(() {
                        _hideUnchanged = value!;
                        _currentPage = 1;
                      });
                    },
                  ),
                  const Text('隐藏未修改项'),
                ],
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildPreviewTable() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage.isNotEmpty) {
      return Center(
        child: Text(
          _errorMessage,
          style: const TextStyle(color: Colors.red),
        ),
      );
    }

    final currentRecords = _getCurrentPageRecords();

    if (currentRecords.isEmpty) {
      return const Center(
        child: Text('暂无变更记录'),
      );
    }

    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            child: DataTable(
              columns: const [
                DataColumn(label: Text('原文件名')),
                DataColumn(label: Text('新文件名')),
                DataColumn(label: Text('文件路径')),
                DataColumn(label: Text('操作类型')),
                DataColumn(label: Text('状态')),
              ],
              rows: currentRecords.map((record) {
                return DataRow(
                  cells: [
                    DataCell(Text(record.originalName ?? '')),
                    DataCell(Text(record.newName ?? '')),
                    DataCell(Text(record.filePath ?? '')),
                    DataCell(Text(record.operationType ?? '')),
                    DataCell(
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: record.changed == true ? Colors.green : Colors.grey,
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          record.changed == true ? '已修改' : '未修改',
                          style: const TextStyle(color: Colors.white),
                        ),
                      ),
                    ),
                  ],
                );
              }).toList(),
            ),
          ),
        ),
        _buildPagination(),
      ],
    );
  }

  Widget _buildPagination() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        TextButton(
          onPressed: _currentPage > 1
              ? () => _goToPage(_currentPage - 1)
              : null,
          child: const Text('上一页'),
        ),
        Text('第 $_currentPage 页，共 $_totalPages 页'),
        TextButton(
          onPressed: _currentPage < _totalPages
              ? () => _goToPage(_currentPage + 1)
              : null,
          child: const Text('下一页'),
        ),
      ],
    );
  }

  Widget _buildStatusBar() {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500)),
                          Text(
                            _getTaskStateText(),
                            style: TextStyle(
                              color: _getTaskStateColor(),
                            ),
                          ),
                        ],
                      ),
                      if (_message.isNotEmpty)
                        Row(
                          children: [
                            const Text('消息: ', style: TextStyle(fontWeight: FontWeight.w500)),
                            Expanded(
                              child: Text(
                                _message,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    ElevatedButton(
                      onPressed: _taskState == LocalTaskState.ready || _taskState == LocalTaskState.previewCompleted
                          ? _analyzePipeline
                          : null,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.blue,
                        foregroundColor: Colors.white,
                      ),
                      child: const Text('预览'),
                    ),
                    const SizedBox(height: 8),
                    ElevatedButton(
                      onPressed: _taskState == LocalTaskState.previewCompleted
                          ? _executeTask
                          : null,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.green,
                        foregroundColor: Colors.white,
                      ),
                      child: const Text('执行'),
                    ),
                    const SizedBox(height: 8),
                    ElevatedButton(
                      onPressed: _taskState == LocalTaskState.previewing || _taskState == LocalTaskState.executing
                          ? _stopTask
                          : null,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.red,
                        foregroundColor: Colors.white,
                      ),
                      child: const Text('停止'),
                    ),
                  ],
                ),
              ],
            ),
            if (_isStatusBarExpanded)
              const SizedBox(height: 12),
            if (_isStatusBarExpanded)
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (_currentStep.isNotEmpty)
                          Row(
                            children: [
                              const Text('当前步骤: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Expanded(
                                child: Text(
                                  _currentStep,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        if (_remainingTime.isNotEmpty)
                          Row(
                            children: [
                              const Text('剩余时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Text(_remainingTime),
                            ],
                          ),
                        if (_changeCount > 0)
                          Row(
                            children: [
                              const Text('变更数量: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Text('$_changeCount'),
                            ],
                          ),
                        if (_scannedFiles > 0)
                          Row(
                            children: [
                              const Text('扫描进度: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Text('$_scannedFiles/$_totalFiles'),
                            ],
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            if (_logMessage.isNotEmpty)
              const SizedBox(height: 12),
            if (_logMessage.isNotEmpty)
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Text('日志: ', style: TextStyle(fontWeight: FontWeight.w500)),
                            Expanded(
                              child: Text(
                                _logMessage,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 12),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            Row(
              children: [
                TextButton(
                  onPressed: () {
                    setState(() {
                      _isStatusBarExpanded = !_isStatusBarExpanded;
                    });
                  },
                  child: Text(
                    _isStatusBarExpanded ? '收起详情' : '展开详情',
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _getTaskStateText() {
    switch (_taskState) {
      case LocalTaskState.ready:
        return '就绪';
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
        return '已取消';
      default:
        return '未知';
    }
  }

  Color _getTaskStateColor() {
    switch (_taskState) {
      case LocalTaskState.ready:
        return Colors.grey;
      case LocalTaskState.previewing:
      case LocalTaskState.executing:
        return Colors.blue;
      case LocalTaskState.previewCompleted:
      case LocalTaskState.executionCompleted:
        return Colors.green;
      case LocalTaskState.previewFailed:
      case LocalTaskState.executionFailed:
        return Colors.red;
      case LocalTaskState.cancelled:
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _buildCurrentView(),
    );
  }
}