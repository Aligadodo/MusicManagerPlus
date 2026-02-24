import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/task_status.dart' as task_models;

class TaskDetailWidget extends ConsumerWidget {
  final task_models.TaskStatus? selectedTask;
  final Function() onBack;

  const TaskDetailWidget({
    super.key,
    required this.selectedTask,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (selectedTask == null) {
      return const Center(child: Text('请选择一个任务'));
    }

    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          _buildTaskDetailHeader(context),
          const SizedBox(height: 12),
          Expanded(
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildTaskInfoCard(context),
                  const SizedBox(height: 16),
                  _buildConfigSnapshotCard(context),
                  const SizedBox(height: 16),
                  _buildScanResultCard(context),
                  const SizedBox(height: 16),
                  _buildPreviewResultCard(context),
                  const SizedBox(height: 16),
                  _buildExecutionResultCard(context),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTaskDetailHeader(BuildContext context) {
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
          onPressed: onBack,
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
          ),
          child: const Text('返回任务列表'),
        ),
      ],
    );
  }

  Widget _buildTaskInfoCard(BuildContext context) {
    if (selectedTask == null) return Container();

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
                    selectedTask!.taskId ?? 'N/A',
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
                    selectedTask!.taskName ?? '未命名任务',
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
                  _getFriendlyStatus(selectedTask!.status ?? 'UNKNOWN'),
                  style: TextStyle(
                    fontSize: 13,
                    color: _getStatusColor(selectedTask!.status ?? 'UNKNOWN'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('当前阶段: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  selectedTask!.currentStage ?? 'N/A',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (selectedTask!.createdAt != null)
              Row(
                children: [
                  const Text('创建时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(selectedTask!.createdAt!).toString(),
                      style: const TextStyle(fontSize: 13),
                    ),
                  ),
                ],
              ),
            if (selectedTask!.message != null && selectedTask!.message!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('消息: ', style: TextStyle(fontWeight: FontWeight.w500)),
                    Expanded(
                      child: Text(
                        selectedTask!.message!,
                        style: const TextStyle(fontSize: 13),
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildConfigSnapshotCard(BuildContext context) {
    if (selectedTask == null || selectedTask!.configSnapshot == null) {
      return Container();
    }

    final configSnapshot = selectedTask!.configSnapshot!;
    final globalSettings = configSnapshot.globalSettings ?? {};

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
            const SizedBox(height: 12),
            _buildStrategyConfig(configSnapshot.strategyId, configSnapshot.strategyConfig),
            const SizedBox(height: 12),
            if (globalSettings.isNotEmpty)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '全局设置',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  ...globalSettings.entries.map((entry) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 4),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          SizedBox(
                            width: 150,
                            child: Text(
                              '${entry.key}: ',
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                          Expanded(
                            child: Text(
                              entry.value.toString(),
                              style: const TextStyle(fontSize: 13),
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSourceDirectoriesList(List<task_models.SourceDirectoryConfig>? sourceDirs) {
    if (sourceDirs == null || sourceDirs.isEmpty) {
      return const Text('无源目录配置');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '源目录',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        ...sourceDirs.map((dir) {
          return Padding(
            padding: const EdgeInsets.only(left: 16, bottom: 4),
            child: Text(
              dir.path ?? 'N/A',
              style: const TextStyle(fontSize: 13),
            ),
          );
        }).toList(),
      ],
    );
  }

  Widget _buildStrategyConfig(String? strategyId, Map<String, dynamic>? strategyConfig) {
    if (strategyId == null) {
      return const Text('无策略配置');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '策略配置',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        Padding(
          padding: const EdgeInsets.only(left: 16),
          child: Text(
            '策略ID: $strategyId',
            style: const TextStyle(fontSize: 13),
          ),
        ),
        if (strategyConfig != null && strategyConfig.isNotEmpty)
          ...strategyConfig.entries.map((entry) {
            return Padding(
              padding: const EdgeInsets.only(left: 16, bottom: 4),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 150,
                    child: Text(
                      '${entry.key}: ',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  Expanded(
                    child: Text(
                      entry.value.toString(),
                      style: const TextStyle(fontSize: 13),
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
      ],
    );
  }

  Widget _buildScanResultCard(BuildContext context) {
    if (selectedTask == null || selectedTask!.stages?.scan == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无扫描结果信息'),
        ),
      );
    }

    final scanStage = selectedTask!.stages!.scan!;

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

  Widget _buildPreviewResultCard(BuildContext context) {
    if (selectedTask == null || selectedTask!.stages?.preview == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无预览结果信息'),
        ),
      );
    }

    final previewStage = selectedTask!.stages!.preview!;

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
                const Text('变更数量: ', style: TextStyle(fontWeight: FontWeight.w500)),
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

  Widget _buildExecutionResultCard(BuildContext context) {
    if (selectedTask == null || selectedTask!.stages?.execution == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无执行结果信息'),
        ),
      );
    }

    final executionStage = selectedTask!.stages!.execution!;

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
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('失败数: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  '${executionStage.failedCount ?? 0}',
                  style: const TextStyle(fontSize: 13),
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
}
