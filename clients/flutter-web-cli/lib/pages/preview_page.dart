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
import '../models/local_task_state.dart';
import '../widgets/selectable_text_widget.dart';
import '../widgets/task_list_widget.dart';
import '../widgets/task_detail_widget.dart';
import '../widgets/preview_widget.dart';

// 导入main.dart中的taskStateProvider
import '../main.dart' as main_app;

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
    await _loadTasks();
    
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
        _refreshTaskDetailSafe();
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
      _errorMessage = '';
    });

    try {
      final result = await _taskService.getTaskList(
        page: 1,
        size: 20,
        status: _mapStatusToApi(_statusFilter),
      );

      final data = result['data'] as Map<String, dynamic>?;
      final tasks = (data?['list'] as List<dynamic>?)?.map((json) => task_models.TaskStatus.fromJson(json as Map<String, dynamic>)).toList() ?? [];

      setState(() {
        _tasks = tasks;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = '加载任务列表失败: $e';
      });
    }
  }

  Future<void> _refreshTasks() async {
    try {
      final result = await _taskService.getTaskList(
        page: 1,
        size: 20,
        status: _mapStatusToApi(_statusFilter),
      );

      final data = result['data'] as Map<String, dynamic>?;
      final tasks = (data?['list'] as List<dynamic>?)?.map((json) => task_models.TaskStatus.fromJson(json as Map<String, dynamic>)).toList() ?? [];

      if (mounted) {
        setState(() {
          _tasks = tasks;
        });
        
        // 检查是否有任务状态变为 PREVIEWED，如果有，获取变更记录
        for (var task in tasks) {
          if (task.status == 'PREVIEWED' && _taskId.isNotEmpty && task.taskId == _taskId) {
            await _fetchChanges();
            setState(() {
              _taskState = LocalTaskState.previewCompleted;
              _message = '预览分析完成';
            });
            break;
          }
        }
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

  Future<void> _refreshTaskDetailSafe() async {
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
        print('刷新任务详情失败: $e');
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
          content: SelectableTextWidget(
            text: message,
            style: const TextStyle(color: Colors.white),
            maxLines: 5,
          ),
          backgroundColor: Colors.red,
          duration: const Duration(seconds: 5),
        ),
      );
    }
  }

  String _getFriendlyStatus(String status) {
    switch (status) {
      case 'CREATED':
        return '已创建';
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
      case 'COMPLETED':
        return '已完成';
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
      case 'CREATED':
      case 'PENDING':
        return Colors.yellow;
      case 'SCANNING':
      case 'PREVIEWING':
      case 'EXECUTING':
        return Colors.blue;
      case 'SCANNED':
      case 'PREVIEWED':
      case 'COMPLETED':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'CANCELLED':
        return Colors.grey;
      default:
        return Colors.grey;
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
        _taskId = result['taskId'] ?? '';
        _showSuccess(result['message'] ?? '分析任务已开始');
        await _refreshTasks(); // 刷新任务列表，确保预览任务显示
        
        // 不在这里立即获取变更记录，而是通过自动刷新来获取
        // 当任务状态变为 PREVIEWED 时，再获取变更记录
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

  Future<void> _rerunTask(String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认重新运行'),
        content: const Text('确定要重新运行此任务吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确定'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await _taskService.rerunTask(taskId);
      _showSuccess('任务已重新运行');
      _refreshTasks();
    } catch (e) {
      _showError('重新运行任务失败: $e');
    }
  }

  Future<void> _cancelTask(String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认终止'),
        content: const Text('确定要终止此任务吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确定'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await _taskService.cancelTask(taskId);
      _showSuccess('任务已终止');
      _refreshTasks();
    } catch (e) {
      _showError('终止任务失败: $e');
    }
  }

  Future<void> _deleteTask(String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除此任务吗？此操作不可恢复！'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(
              foregroundColor: Colors.red,
            ),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    try {
      await _taskService.deleteTask(taskId);
      _showSuccess('任务已删除');
      _refreshTasks();
    } catch (e) {
      _showError('删除任务失败: $e');
    }
  }

  Future<void> _fetchChanges() async {
    setState(() {
      _isLoading = true;
    });

    try {
      String url = '/api/pipeline/changes';
      if (_taskId.isNotEmpty) {
        url += '?taskId=$_taskId';
      }
      final response = await _apiClient.get(url);
      
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
    return TaskListWidget(
      tasks: _tasks,
      selectedTask: _selectedTask,
      onTaskSelected: (task) {
        setState(() {
          _selectedTask = task;
        });
      },
      onViewModeChanged: (mode) {
        setState(() {
          if (mode == 'taskDetail') {
            _viewMode = ViewMode.taskDetail;
          }
        });
      },
      isLoading: _isLoading,
      errorMessage: _errorMessage,
    );
  }

  Widget _buildTaskDetailView() {
    return TaskDetailWidget(
      selectedTask: _selectedTask,
      onBack: () {
        setState(() {
          _viewMode = ViewMode.taskList;
        });
      },
    );
  }

  Widget _buildPreviewView() {
    return PreviewWidget(
      searchFilter: _searchFilter,
      statusFilter: _statusFilter,
      operationTypeFilter: _operationTypeFilter,
      hideUnchanged: _hideUnchanged,
      isLoading: _isLoading,
      errorMessage: _errorMessage,
      currentPage: _currentPage,
      totalPages: _totalPages,
      records: _changeRecords,
      taskState: _taskState,
      message: _message,
      isStatusBarExpanded: _isStatusBarExpanded,
      currentStep: _currentStep,
      remainingTime: _remainingTime,
      changeCount: _changeCount,
      scannedFiles: _scannedFiles,
      totalFiles: _totalFiles,
      logMessage: _logMessage,
      onSearchFilterChanged: (value) {
        setState(() {
          _searchFilter = value;
          _currentPage = 1;
        });
      },
      onStatusFilterChanged: (value) {
        setState(() {
          _statusFilter = value;
          _currentPage = 1;
        });
      },
      onOperationTypeFilterChanged: (value) {
        setState(() {
          _operationTypeFilter = value;
          _currentPage = 1;
        });
      },
      onHideUnchangedChanged: (value) {
        setState(() {
          _hideUnchanged = value;
          _currentPage = 1;
        });
      },
      onPreview: _analyzePipeline,
      onExecute: _executeTask,
      onStop: _stopTask,
      onPageChanged: (page) {
        setState(() {
          _currentPage = page;
        });
      },
      onToggleStatusBar: () {
        setState(() {
          _isStatusBarExpanded = !_isStatusBarExpanded;
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _buildCurrentView(),
    );
  }
}