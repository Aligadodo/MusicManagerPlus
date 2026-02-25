import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class ScanResultCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ScanResultCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    if (selectedTask == null || selectedTask.stages?.scan == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无扫描结果信息'),
        ),
      );
    }

    final scanStage = selectedTask.stages!.scan!;

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
}

class PreviewResultCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const PreviewResultCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    if (selectedTask == null || selectedTask.stages?.preview == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无预览结果信息'),
        ),
      );
    }

    final previewStage = selectedTask.stages!.preview!;

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
}

class ExecutionResultCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ExecutionResultCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    if (selectedTask == null || selectedTask.stages?.execution == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无执行结果信息'),
        ),
      );
    }

    final executionStage = selectedTask.stages!.execution!;

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
}
