import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/file_service.dart';
import 'package:filemanager_flutter/api/strategy_service.dart';
import 'package:filemanager_flutter/api/task_service.dart';
import 'package:filemanager_flutter/api/source_directory_service.dart';
import 'package:filemanager_flutter/api/pipeline_service.dart';
import 'package:filemanager_flutter/api/thread_pool_service.dart';

// 服务提供者
final apiClientProvider = Provider((ref) => ApiClient());
final fileServiceProvider = Provider((ref) => FileService(ref.watch(apiClientProvider)));
final strategyServiceProvider = Provider((ref) => StrategyService(ref.watch(apiClientProvider)));
final taskServiceProvider = Provider((ref) => TaskService(ref.watch(apiClientProvider)));
final sourceDirectoryServiceProvider = Provider((ref) => SourceDirectoryService(ref.watch(apiClientProvider)));
final pipelineServiceProvider = Provider((ref) => PipelineService(ref.watch(apiClientProvider)));
final threadPoolServiceProvider = Provider((ref) => ThreadPoolService(ref.watch(apiClientProvider)));

class HomePage extends ConsumerWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('FileManager Plus'),
        centerTitle: true,
      ),
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '欢迎使用 FileManager Plus',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              '选择一个功能开始使用:',
              style: TextStyle(
                fontSize: 18,
              ),
            ),
            const SizedBox(height: 30),
            GridView.count(
              shrinkWrap: true,
              crossAxisCount: 2,
              crossAxisSpacing: 20,
              mainAxisSpacing: 20,
              children: [
                _buildFeatureCard(
                  context,
                  title: '文件浏览器',
                  description: '浏览和管理文件系统',
                  icon: Icons.folder_open,
                  route: '/file-browser',
                ),
                _buildFeatureCard(
                  context,
                  title: '策略配置',
                  description: '配置和管理文件处理策略',
                  icon: Icons.settings,
                  route: '/strategy-config',
                ),
                _buildFeatureCard(
                  context,
                  title: '任务监控',
                  description: '查看和管理执行任务',
                  icon: Icons.task,
                  route: '/task-monitor',
                ),
                _buildFeatureCard(
                  context,
                  title: '源目录管理',
                  description: '管理文件处理的源目录',
                  icon: Icons.folder,
                  route: '/source-directories',
                ),
                _buildFeatureCard(
                  context,
                  title: '策略流水线',
                  description: '组合和配置策略流水线',
                  icon: Icons.linear_scale,
                  route: '/pipeline-config',
                ),
                _buildFeatureCard(
                  context,
                  title: '预览分析',
                  description: '分析和预览文件变更',
                  icon: Icons.analytics,
                  route: '/preview',
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFeatureCard(BuildContext context, {
    required String title,
    required String description,
    required IconData icon,
    required String route,
  }) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      child: InkWell(
        onTap: () {
          Navigator.pushNamed(context, route);
        },
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 48,
                color: Colors.blue,
              ),
              const SizedBox(height: 16),
              Text(
                title,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                description,
                style: const TextStyle(
                  fontSize: 14,
                  color: Colors.grey,
                ),
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
