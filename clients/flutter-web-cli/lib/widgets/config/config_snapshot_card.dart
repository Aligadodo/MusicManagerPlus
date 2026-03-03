import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class ConfigSnapshotCard extends StatefulWidget {
  final task_models.TaskStatus selectedTask;

  const ConfigSnapshotCard({super.key, required this.selectedTask});

  @override
  State<ConfigSnapshotCard> createState() => _ConfigSnapshotCardState();
}

class _ConfigSnapshotCardState extends State<ConfigSnapshotCard>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.selectedTask.configSnapshot == null) {
      return Card(
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8.0),
        ),
        child: const Padding(
          padding: EdgeInsets.all(16),
          child: Text('无配置快照信息'),
        ),
      );
    }

    final configSnapshot = widget.selectedTask.configSnapshot!;

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Tab 标题栏
          Container(
            decoration: BoxDecoration(
              color: Colors.grey.shade100,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(8.0)),
            ),
            child: TabBar(
              controller: _tabController,
              labelColor: Colors.blue,
              unselectedLabelColor: Colors.grey.shade600,
              indicatorColor: Colors.blue,
              indicatorWeight: 3,
              tabs: const [
                Tab(icon: Icon(Icons.folder_open, size: 18), text: '源目录'),
                Tab(icon: Icon(Icons.account_tree, size: 18), text: '流水线'),
                Tab(icon: Icon(Icons.settings, size: 18), text: '全局设置'),
              ],
            ),
          ),
          // Tab 内容
          SizedBox(
            height: 400, // 固定高度，避免无限展开
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildSourceDirectoriesTab(configSnapshot.sourceDirectories),
                _buildPipelineTab(configSnapshot.pipelineConfig),
                _buildGlobalSettingsTab(configSnapshot.globalSettings),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // 源目录 Tab
  Widget _buildSourceDirectoriesTab(List<task_models.SourceDirectoryConfig>? sourceDirs) {
    if (sourceDirs == null || sourceDirs.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.folder_open, size: 48, color: Colors.grey),
            SizedBox(height: 16),
            Text('无源目录配置', style: TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.all(16),
      itemCount: sourceDirs.length,
      itemBuilder: (context, index) {
        final dir = sourceDirs[index];
        return Card(
          margin: const EdgeInsets.only(bottom: 12),
          elevation: 1,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.folder, color: Colors.orange, size: 20),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        dir.path ?? '未命名目录',
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 16,
                  runSpacing: 8,
                  children: [
                    _buildInfoChip(Icons.layers, '深度: ${dir.depth ?? 1}'),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  // 流水线 Tab
  Widget _buildPipelineTab(Map<String, dynamic>? pipelineConfig) {
    if (pipelineConfig == null || pipelineConfig.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.account_tree, size: 48, color: Colors.grey),
            SizedBox(height: 16),
            Text('无流水线配置', style: TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }

    final items = (pipelineConfig['items'] as List<dynamic>?) ?? [];

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 流水线基本信息
          Card(
            elevation: 1,
            margin: const EdgeInsets.only(bottom: 16),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    pipelineConfig['name'] ?? '未命名流水线',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  if (pipelineConfig['description'] != null &&
                      pipelineConfig['description'].toString().isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: Text(
                        pipelineConfig['description'].toString(),
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey.shade600,
                        ),
                      ),
                    ),
                  const SizedBox(height: 8),
                  Text(
                    'ID: ${pipelineConfig['pipelineId'] ?? 'N/A'}',
                    style: TextStyle(
                      fontSize: 11,
                      color: Colors.grey.shade500,
                    ),
                  ),
                ],
              ),
            ),
          ),
          // 节点列表
          if (items.isNotEmpty) ...[
            const Padding(
              padding: EdgeInsets.only(left: 8, bottom: 12),
              child: Text(
                '处理节点',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: Colors.blue,
                ),
              ),
            ),
            ...items.asMap().entries.map((entry) {
              final index = entry.key;
              final item = entry.value as Map<String, dynamic>;
              return _buildPipelineNodeCard(index + 1, item);
            }).toList(),
          ],
        ],
      ),
    );
  }

  Widget _buildPipelineNodeCard(int order, Map<String, dynamic> item) {
    final isEnabled = item['enabled'] ?? true;
    final strategyName = item['strategyName'] ?? item['strategyId'] ?? '未知策略';

    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      elevation: 1,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            // 序号
            Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                color: isEnabled ? Colors.blue : Colors.grey,
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Text(
                  '$order',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            // 节点信息
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    strategyName,
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                      color: isEnabled ? Colors.black87 : Colors.grey,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    'ID: ${item['strategyId'] ?? 'N/A'}',
                    style: TextStyle(
                      fontSize: 11,
                      color: Colors.grey.shade500,
                    ),
                  ),
                ],
              ),
            ),
            // 状态标签
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                color: isEnabled
                    ? Colors.green.withOpacity(0.1)
                    : Colors.grey.withOpacity(0.1),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Text(
                isEnabled ? '启用' : '禁用',
                style: TextStyle(
                  fontSize: 10,
                  color: isEnabled ? Colors.green : Colors.grey,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  // 全局设置 Tab
  Widget _buildGlobalSettingsTab(Map<String, dynamic>? globalSettings) {
    if (globalSettings == null || globalSettings.isEmpty) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.settings, size: 48, color: Colors.grey),
            SizedBox(height: 16),
            Text('无全局设置', style: TextStyle(color: Colors.grey)),
          ],
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // 线程池设置
        _buildSettingsGroup(
          '线程池设置',
          Icons.memory,
          Colors.blue,
          [
            _buildSettingItem('maxThreads', globalSettings['maxThreads']),
            _buildSettingItem('previewThreads', globalSettings['previewThreads']),
            _buildSettingItem('executionThreads', globalSettings['executionThreads']),
            _buildSettingItem('threadPoolMode', globalSettings['threadPoolMode']),
          ],
        ),
        const SizedBox(height: 16),
        // 执行控制
        _buildSettingsGroup(
          '执行控制',
          Icons.timer,
          Colors.orange,
          [
            _buildSettingItem('timeout', globalSettings['timeout']),
            _buildSettingItem('retryCount', globalSettings['retryCount']),
            _buildSettingItem('retryInterval', globalSettings['retryInterval']),
          ],
        ),
        const SizedBox(height: 16),
        // 文件处理
        _buildSettingsGroup(
          '文件处理',
          Icons.file_copy,
          Colors.purple,
          [
            _buildSettingItem('overwrite', globalSettings['overwrite']),
            _buildSettingItem('backup', globalSettings['backup']),
            _buildSettingItem('backupPath', globalSettings['backupPath']),
          ],
        ),
        const SizedBox(height: 16),
        // 扫描设置
        _buildSettingsGroup(
          '扫描设置',
          Icons.search,
          Colors.teal,
          [
            _buildSettingItem('minRecursionDepth', globalSettings['minRecursionDepth']),
            _buildSettingItem('maxRecursionDepth', globalSettings['maxRecursionDepth']),
            _buildSettingItem('previewLimit', globalSettings['previewLimit']),
            _buildSettingItem('executionLimit', globalSettings['executionLimit']),
          ],
        ),
        const SizedBox(height: 16),
        // 自动化设置
        _buildSettingsGroup(
          '自动化设置',
          Icons.auto_mode,
          Colors.green,
          [
            _buildSettingItem('autoRefresh', globalSettings['autoRefresh']),
            _buildSettingItem('autoExecute', globalSettings['autoExecute']),
          ],
        ),
        const SizedBox(height: 16),
        // 调试模式
        _buildSettingsGroup(
          '调试模式',
          Icons.bug_report,
          Colors.red,
          [
            _buildSettingItem('dryRun', globalSettings['dryRun']),
          ],
        ),
      ],
    );
  }

  Widget _buildSettingsGroup(
    String title,
    IconData icon,
    Color color,
    List<Widget> children,
  ) {
    return Card(
      elevation: 1,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 18, color: color),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                    color: color,
                  ),
                ),
              ],
            ),
            const Divider(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _buildSettingItem(String key, dynamic value) {
    final labels = {
      'maxThreads': '最大线程数',
      'timeout': '超时时间(ms)',
      'dryRun': '模拟运行',
      'overwrite': '覆盖已有文件',
      'backup': '启用备份',
      'backupPath': '备份路径',
      'retryCount': '重试次数',
      'retryInterval': '重试间隔(ms)',
      'previewThreads': '预览线程数',
      'executionThreads': '执行线程数',
      'threadPoolMode': '线程池模式',
      'minRecursionDepth': '最小递归深度',
      'maxRecursionDepth': '最大递归深度',
      'previewLimit': '预览文件上限',
      'executionLimit': '执行文件上限',
      'autoRefresh': '自动刷新',
      'autoExecute': '自动执行',
    };

    String displayValue;
    if (value == null) {
      displayValue = '未设置';
    } else if (value is bool) {
      displayValue = value ? '是' : '否';
    } else {
      displayValue = value.toString();
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(
            width: 120,
            child: Text(
              labels[key] ?? key,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade700,
              ),
            ),
          ),
          Expanded(
            child: Text(
              displayValue,
              style: const TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: Colors.grey.shade600),
          const SizedBox(width: 4),
          Text(
            text,
            style: TextStyle(
              fontSize: 11,
              color: Colors.grey.shade700,
            ),
          ),
        ],
      ),
    );
  }
}
