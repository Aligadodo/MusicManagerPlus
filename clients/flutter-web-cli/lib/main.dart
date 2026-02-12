import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/pages/compose_page.dart';
import 'package:filemanager_flutter/pages/preview_page.dart';
import 'package:filemanager_flutter/pages/log_page.dart';
import 'package:filemanager_flutter/pages/appearance_page.dart';
import 'package:filemanager_flutter/pages/global_settings_page.dart';
import 'package:filemanager_flutter/providers/config_provider.dart';
import 'package:filemanager_flutter/utils/ui_utils.dart';


final apiClientProvider = Provider<ApiClient>((ref) => ApiClient());

enum TaskStatus {
  idle,
  analyzing,
  running,
  stopping,
  completed,
  error,
}

class TaskState {
  final TaskStatus status;
  final String? taskId;
  final String? message;
  final int progress;
  final String? errorMessage;

  TaskState({
    required this.status,
    this.taskId,
    this.message,
    this.progress = 0,
    this.errorMessage,
  });

  TaskState copyWith({
    TaskStatus? status,
    String? taskId,
    String? message,
    int? progress,
    String? errorMessage,
  }) {
    return TaskState(
      status: status ?? this.status,
      taskId: taskId ?? this.taskId,
      message: message ?? this.message,
      progress: progress ?? this.progress,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

class TaskNotifier extends StateNotifier<TaskState> {
  TaskNotifier() : super(TaskState(status: TaskStatus.idle, message: '就绪'));

  void startAnalyzing() {
    state = state.copyWith(
      status: TaskStatus.analyzing,
      message: '正在分析...',
      progress: 0,
    );
  }

  void startRunning(String taskId) {
    state = state.copyWith(
      status: TaskStatus.running,
      taskId: taskId,
      message: '正在执行...',
      progress: 0,
    );
  }

  void updateProgress(int progress, String message) {
    state = state.copyWith(
      progress: progress,
      message: message,
    );
  }

  void complete() {
    state = state.copyWith(
      status: TaskStatus.completed,
      message: '执行完成',
      progress: 100,
    );
  }

  void stop() {
    state = state.copyWith(
      status: TaskStatus.stopping,
      message: '正在停止...',
    );
  }

  void stopComplete() {
    state = state.copyWith(
      status: TaskStatus.idle,
      taskId: null,
      message: '已停止',
      progress: 0,
    );
  }

  void error(String errorMessage) {
    state = state.copyWith(
      status: TaskStatus.error,
      message: '执行出错',
      errorMessage: errorMessage,
    );
  }

  void reset() {
    state = TaskState(status: TaskStatus.idle, message: '就绪');
  }
}

final taskStateProvider = StateNotifierProvider<TaskNotifier, TaskState>((ref) {
  return TaskNotifier();
});

void main() {
  runApp(
    const ProviderScope(
      child: FileManagerApp(),
    ),
  );
}

class FileManagerApp extends ConsumerWidget {
  const FileManagerApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final config = ref.watch(configProvider);
    final appearanceConfig = config.appearanceConfig;

    final isDark = appearanceConfig['darkBackground'] as bool? ?? false;
    final primaryColor = UiUtils.parseColor(appearanceConfig['accentColor'] as String? ?? '#2196F3');
    final backgroundColor = UiUtils.parseColor(appearanceConfig['bgColor'] as String? ?? '#FFFFFF');

    return MaterialApp(
      title: 'MUSIC MANAGER PLUS - By chrse1997@163.com',
      theme: ThemeData(
        useMaterial3: true,
        brightness: isDark ? Brightness.dark : Brightness.light,
        colorScheme: ColorScheme.fromSeed(
          seedColor: primaryColor,
          brightness: isDark ? Brightness.dark : Brightness.light,
        ),
        scaffoldBackgroundColor: backgroundColor,
        cardColor: UiUtils.parseColor(appearanceConfig['panelBgColor'] as String? ?? '#FFFFFF'),
        textTheme: TextTheme(
          bodyLarge: TextStyle(
            color: UiUtils.parseColor(appearanceConfig['textPrimaryColor'] as String? ?? '#000000'),
            fontSize: (appearanceConfig['fontSize'] as int? ?? 14).toDouble(),
          ),
          bodyMedium: TextStyle(
            color: UiUtils.parseColor(appearanceConfig['textPrimaryColor'] as String? ?? '#000000'),
            fontSize: (appearanceConfig['fontSize'] as int? ?? 14).toDouble(),
          ),
          bodySmall: TextStyle(
            color: UiUtils.parseColor(appearanceConfig['textSecondaryColor'] as String? ?? '#666666'),
            fontSize: ((appearanceConfig['fontSize'] as int? ?? 14) - 2).toDouble(),
          ),
        ),
        fontFamily: appearanceConfig['fontFamily'] as String? ?? 'Roboto',
      ),
      home: const MainLayout(),
      debugShowCheckedModeBanner: false,
    );
  }


}

class MainLayout extends ConsumerStatefulWidget {
  const MainLayout({super.key});

  @override
  ConsumerState<MainLayout> createState() => _MainLayoutState();
}

class _MainLayoutState extends ConsumerState<MainLayout> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  bool _autoRun = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 5, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadConfig();
    });
  }

  Future<void> _loadConfig() async {
    try {
      final configNotifier = ref.read(configProvider.notifier);
      await configNotifier.loadConfig();
    } catch (e) {
      print('加载配置失败: $e');
    }
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _runPipelineAnalysis() async {
    final taskNotifier = ref.read(taskStateProvider.notifier);
    
    try {
      taskNotifier.startAnalyzing();
      
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('开始预览分析')),
      );
      
      // 切换到预览执行页面
      _tabController.animateTo(1);
      
      // 这里不需要手动执行分析，因为预览执行页面会自动处理
      // 我们只需要确保状态已经正确设置
    } catch (e) {
      taskNotifier.error(e.toString());
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('预览分析失败: $e')),
      );
    }
  }

  Future<void> _runPipelineExecution() async {
    final taskNotifier = ref.read(taskStateProvider.notifier);
    final currentState = ref.read(taskStateProvider);
    
    if (currentState.status == TaskStatus.running || currentState.status == TaskStatus.analyzing) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('任务正在执行中，请先停止当前任务')),
      );
      return;
    }
    
    try {
      // 切换到预览执行页面
      _tabController.animateTo(1);
      
      // 这里不需要手动执行任务，因为预览执行页面会自动处理
      // 我们只需要确保状态已经正确设置
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请在预览执行页面确认变更后执行')),
      );
    } catch (e) {
      taskNotifier.error(e.toString());
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('执行失败: $e')),
      );
    }
  }

  Future<String?> _createTask() async {
    try {
      final apiClient = ref.read(apiClientProvider);
      final response = await apiClient.post(
        '/tasks',
        body: {
          'strategyId': 'default',
          'filePaths': [],
          'taskName': '文件管理任务',
          'description': '通过前端创建的任务',
        },
      );
      
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map;
        return data['taskId'] as String?;
      }
      return null;
    } catch (e) {
      print('创建任务失败: $e');
      return null;
    }
  }

  Future<void> _executeTask(String taskId) async {
    try {
      final apiClient = ref.read(apiClientProvider);
      final response = await apiClient.post(
        '/tasks/$taskId/execute',
        body: {},
      );
      
      if (response.statusCode != 200) {
        throw Exception('执行任务失败');
      }
    } catch (e) {
      print('执行任务失败: $e');
      rethrow;
    }
  }

  Future<void> _forceStop() async {
    final taskNotifier = ref.read(taskStateProvider.notifier);
    final currentState = ref.read(taskStateProvider);
    
    if (currentState.status == TaskStatus.idle || currentState.status == TaskStatus.completed) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('当前没有正在执行的任务')),
      );
      return;
    }
    
    try {
      taskNotifier.stop();
      
      // 切换到预览执行页面
      _tabController.animateTo(1);
      
      // 停止流水线任务
      final apiClient = ref.read(apiClientProvider);
      await apiClient.post('/pipeline/stop', body: {});
      
      await Future.delayed(const Duration(seconds: 1));
      
      taskNotifier.stopComplete();
      
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('任务已停止')),
      );
    } catch (e) {
      taskNotifier.error(e.toString());
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('停止任务失败: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          _buildHeader(),
          _buildTabBar(),
          _buildContent(),
          _buildStatusBar(),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    final config = ref.watch(configProvider);
    final appearanceConfig = config.appearanceConfig;
    final backgroundColor = UiUtils.parseColor(appearanceConfig['panelBgColor'] as String? ?? '#FFFFFF');
    final borderColor = UiUtils.parseColor(appearanceConfig['borderColor'] as String? ?? '#E0E0E0');
    final textColor = UiUtils.parseColor(appearanceConfig['textPrimaryColor'] as String? ?? '#000000');
    
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
      decoration: BoxDecoration(
        color: backgroundColor.withOpacity(0.9),
        border: Border(
          bottom: BorderSide(
            color: borderColor,
            width: 1,
          ),
        ),
        boxShadow: [
          BoxShadow(
            color: textColor.withOpacity(0.05),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          _buildLogo(),
          const Spacer(),
          _buildMenu(),
          const SizedBox(width: 20),
          _buildCheckboxes(),
          const SizedBox(width: 20),
          _buildActionButtons(),
        ],
      ),
    );
  }

  Widget _buildLogo() {
    final config = ref.watch(configProvider);
    final appearanceConfig = config.appearanceConfig;
    final textColor = UiUtils.parseColor(appearanceConfig['textPrimaryColor'] as String? ?? '#000000');
    
    return Text(
      'MUSIC MANAGER PLUS',
      style: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.bold,
        color: textColor,
        shadows: [
          Shadow(
            color: textColor.withOpacity(0.1),
            blurRadius: 3,
            offset: const Offset(0, 1),
          ),
        ],
      ),
    );
  }

  Widget _buildMenu() {
    final config = ref.watch(configProvider);
    final appearanceConfig = config.appearanceConfig;
    final secondaryTextColor = UiUtils.parseColor(appearanceConfig['textSecondaryColor'] as String? ?? '#666666');
    final textColor = UiUtils.parseColor(appearanceConfig['textPrimaryColor'] as String? ?? '#000000');
    final accentColor = UiUtils.parseColor(appearanceConfig['accentColor'] as String? ?? '#2196F3');
    
    return PopupMenuButton<String>(
      icon: Icon(Icons.menu, color: secondaryTextColor),
      onSelected: (value) async {
        final configNotifier = ref.read(configProvider.notifier);
        switch (value) {
          case 'load':
            try {
              await configNotifier.loadConfig();
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('配置加载成功')),
                );
              }
            } catch (e) {
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('配置加载失败: $e')),
                );
              }
            }
            break;
          case 'save':
            try {
              await configNotifier.saveConfig();
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('配置保存成功')),
                );
              }
            } catch (e) {
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('配置保存失败: $e')),
                );
              }
            }
            break;
          case 'reset':
            final confirmed = await showDialog<bool>(
              context: context,
              builder: (context) => AlertDialog(
                title: const Text('确认重置'),
                content: const Text('确定要重置所有配置到默认值吗？此操作不可撤销。'),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.pop(context, false),
                    child: const Text('取消'),
                  ),
                  ElevatedButton(
                    onPressed: () => Navigator.pop(context, true),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                    ),
                    child: const Text('确认重置'),
                  ),
                ],
              ),
            );

            if (confirmed == true) {
              try {
                final apiClient = ApiClient();
                final pipelineService = PipelineService(apiClient);
                final sourceDirectoryService = SourceDirectoryService(apiClient);
                
                await pipelineService.resetPipeline();
                await sourceDirectoryService.clearSourceDirectories();
                await configNotifier.resetConfig();
                
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('所有配置已重置为默认值')),
                  );
                }
              } catch (e) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(content: Text('配置重置失败: $e')),
                  );
                }
              }
            }
            break;
        }
      },
      itemBuilder: (context) => [
        const PopupMenuItem(
          value: 'load',
          child: Text('加载配置...'),
        ),
        const PopupMenuItem(
          value: 'save',
          child: Text('保存配置...'),
        ),
        const PopupMenuItem(
          value: 'reset',
          child: Text('重置配置'),
        ),
      ],
    );
  }

  Widget _buildCheckboxes() {
    final config = ref.watch(configProvider);
    final showTooltips = config.globalSettings['showTooltips'] as bool? ?? true;
    final configNotifier = ref.read(configProvider.notifier);

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Checkbox(
              value: showTooltips,
              onChanged: (value) {
                final newShowTooltips = value ?? true;
                final updatedGlobalSettings = Map<String, dynamic>.from(config.globalSettings);
                updatedGlobalSettings['showTooltips'] = newShowTooltips;
                configNotifier.updateGlobalSettings(updatedGlobalSettings);
              },
            ),
            const Text('开启使用说明'),
          ],
        ),
        const SizedBox(width: 10),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Checkbox(
              value: _autoRun,
              onChanged: (value) {
                setState(() {
                  _autoRun = value ?? false;
                });
              },
            ),
            const Text('预览成功立即运行'),
          ],
        ),
      ],
    );
  }

  Widget _buildActionButtons() {
    return Consumer(
      builder: (context, ref, child) {
        final taskState = ref.watch(taskStateProvider);
        final isRunning = taskState.status == TaskStatus.running || taskState.status == TaskStatus.analyzing;
        
        return Row(
          children: [
            ElevatedButton.icon(
              onPressed: isRunning ? null : _runPipelineAnalysis,
              icon: const Icon(Icons.visibility),
              label: const Text('预览'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
            ),
            const SizedBox(width: 10),
            ElevatedButton.icon(
              onPressed: isRunning ? null : _runPipelineExecution,
              icon: const Icon(Icons.play_arrow),
              label: const Text('执行'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
            ),
            const SizedBox(width: 10),
            ElevatedButton.icon(
              onPressed: isRunning ? _forceStop : null,
              icon: const Icon(Icons.stop),
              label: const Text('停止'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
              ),
            ),
          ],
        );
      },
    );
  }

  Widget _buildTabBar() {
    final config = ref.watch(configProvider);
    final appearanceConfig = config.appearanceConfig;
    final backgroundColor = UiUtils.parseColor(appearanceConfig['panelBgColor'] as String? ?? '#FFFFFF');
    final borderColor = UiUtils.parseColor(appearanceConfig['borderColor'] as String? ?? '#E0E0E0');
    final accentColor = UiUtils.parseColor(appearanceConfig['accentColor'] as String? ?? '#2196F3');
    final secondaryTextColor = UiUtils.parseColor(appearanceConfig['textSecondaryColor'] as String? ?? '#666666');
    
    return Container(
      decoration: BoxDecoration(
        color: backgroundColor.withOpacity(0.9),
        border: Border(
          bottom: BorderSide(
            color: borderColor,
            width: 1,
          ),
        ),
      ),
      child: TabBar(
        controller: _tabController,
        tabs: const [
          Tab(text: '任务编排'),
          Tab(text: '预览执行'),
          Tab(text: '运行日志'),
          Tab(text: '全局设置'),
          Tab(text: '界面设置'),
        ],
        labelColor: accentColor,
        unselectedLabelColor: secondaryTextColor,
        indicatorColor: accentColor,
      ),
    );
  }

  Widget _buildContent() {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(10),
        child: TabBarView(
          controller: _tabController,
          children: const [
            ComposePage(),
            PreviewPage(),
            LogPage(),
            GlobalSettingsPage(),
            AppearancePage(),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusBar() {
    return Consumer(
      builder: (context, ref, child) {
        final taskState = ref.watch(taskStateProvider);
        final config = ref.watch(configProvider);
        final appearanceConfig = config.appearanceConfig;
        final backgroundColor = UiUtils.parseColor(appearanceConfig['panelBgColor'] as String? ?? '#FFFFFF');
        final borderColor = UiUtils.parseColor(appearanceConfig['borderColor'] as String? ?? '#E0E0E0');
        final textColor = UiUtils.parseColor(appearanceConfig['textPrimaryColor'] as String? ?? '#000000');
        final isRunning = taskState.status == TaskStatus.running || taskState.status == TaskStatus.analyzing;
        
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 5),
          decoration: BoxDecoration(
            color: backgroundColor.withOpacity(0.8),
            border: Border(
              top: BorderSide(
                color: borderColor,
                width: 1,
              ),
            ),
          ),
          child: Row(
            children: [
              Icon(
                Icons.circle,
                size: 10,
                color: isRunning ? Colors.orange : Colors.green,
              ),
              const SizedBox(width: 10),
              Text(
                taskState.message ?? '就绪',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: textColor,
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
