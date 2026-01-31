import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/file_service.dart';
import 'package:filemanager_flutter/api/strategy_service.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/api/thread_pool_service.dart';
import 'package:filemanager_flutter/api/log_service.dart';
import 'package:filemanager_flutter/api/config_service.dart';
import 'package:filemanager_flutter/pages/compose_page.dart';
import 'package:filemanager_flutter/pages/preview_page.dart';
import 'package:filemanager_flutter/pages/log_page.dart';
import 'package:filemanager_flutter/pages/appearance_page.dart';
import 'package:filemanager_flutter/pages/global_settings_page.dart';


final apiClientProvider = Provider<ApiClient>((ref) => ApiClient());

void main() {
  runZonedGuarded(
    () => runApp(
      const ProviderScope(
        child: FileManagerApp(),
      ),
    ),
    (error, stack) {
      print('Uncaught error: $error');
      print('Stack trace: $stack');
    },
  );
}

class FileManagerApp extends StatelessWidget {
  const FileManagerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MUSIC MANAGER PLUS - By chrse1997@163.com',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: Colors.blue,
          brightness: Brightness.light,
        ),
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
  bool _showTooltips = true;
  bool _autoRun = false;
  bool _taskRunning = false;
  String _statusMessage = '就绪';

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 5, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _runPipelineAnalysis() {
    setState(() {
      _statusMessage = '正在分析...';
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('开始预览分析')),
    );
  }

  void _runPipelineExecution() {
    setState(() {
      _taskRunning = true;
      _statusMessage = '正在执行...';
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('开始执行流水线')),
    );
  }

  void _forceStop() {
    setState(() {
      _taskRunning = false;
      _statusMessage = '就绪';
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('任务已停止')),
    );
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
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        border: Border(
          bottom: BorderSide(
            color: Colors.grey.shade300,
            width: 1,
          ),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
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
    return Text(
      'MUSIC MANAGER PLUS',
      style: TextStyle(
        fontSize: 20,
        fontWeight: FontWeight.bold,
        color: Colors.grey.shade800,
        shadows: [
          Shadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 3,
            offset: const Offset(0, 1),
          ),
        ],
      ),
    );
  }

  Widget _buildMenu() {
    return PopupMenuButton<String>(
      icon: const Icon(Icons.menu, color: Colors.grey),
      onSelected: (value) {
        switch (value) {
          case 'load':
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('加载配置...')),
            );
            break;
          case 'save':
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('保存配置...')),
            );
            break;
          case 'reset':
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('重置配置...')),
            );
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
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Checkbox(
              value: _showTooltips,
              onChanged: (value) {
                setState(() {
                  _showTooltips = value ?? true;
                });
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
    return Row(
      children: [
        ElevatedButton.icon(
          onPressed: _runPipelineAnalysis,
          icon: const Icon(Icons.visibility),
          label: const Text('预览'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
          ),
        ),
        const SizedBox(width: 10),
        ElevatedButton.icon(
          onPressed: _taskRunning ? null : _runPipelineExecution,
          icon: const Icon(Icons.play_arrow),
          label: const Text('执行'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.green,
            foregroundColor: Colors.white,
          ),
        ),
        const SizedBox(width: 10),
        ElevatedButton.icon(
          onPressed: _taskRunning ? _forceStop : null,
          icon: const Icon(Icons.stop),
          label: const Text('停止'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.red,
            foregroundColor: Colors.white,
          ),
        ),
      ],
    );
  }

  Widget _buildTabBar() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        border: Border(
          bottom: BorderSide(
            color: Colors.grey.shade300,
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
        labelColor: Colors.blue,
        unselectedLabelColor: Colors.grey,
        indicatorColor: Colors.blue,
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
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 5),
      decoration: BoxDecoration(
        color: Colors.grey.shade200.withOpacity(0.8),
        border: Border(
          top: BorderSide(
            color: Colors.grey.shade300,
            width: 1,
          ),
        ),
      ),
      child: Row(
        children: [
          Icon(
            Icons.circle,
            size: 10,
            color: _taskRunning ? Colors.orange : Colors.green,
          ),
          const SizedBox(width: 10),
          Text(
            _statusMessage,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }
}
