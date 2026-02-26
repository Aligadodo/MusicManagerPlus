import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class ScanResultCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ScanResultCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    if (selectedTask == null || selectedTask.stages?.scan == null) {
      return Card(
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8.0),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无扫描结果信息', style: TextStyle(fontSize: 14, color: Colors.grey)),
        ),
      );
    }

    final scanStage = selectedTask.stages!.scan!;

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
              decoration: BoxDecoration(
                color: Colors.blue.withOpacity(0.1),
                borderRadius: BorderRadius.circular(4.0),
              ),
              child: const Text(
                '扫描结果',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.blue,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    scanStage.status ?? 'N/A',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (scanStage.scanStartTime != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('开始时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(scanStage.scanStartTime!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            if (scanStage.scanEndTime != null)
              const SizedBox(height: 12),
            if (scanStage.scanEndTime != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('结束时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(scanStage.scanEndTime!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('扫描文件数: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    '${scanStage.scannedFiles ?? 0}/${scanStage.totalFiles ?? 0}',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
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
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8.0),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无预览结果信息', style: TextStyle(fontSize: 14, color: Colors.grey)),
        ),
      );
    }

    final previewStage = selectedTask.stages!.preview!;

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
              decoration: BoxDecoration(
                color: Colors.green.withOpacity(0.1),
                borderRadius: BorderRadius.circular(4.0),
              ),
              child: const Text(
                '预览结果',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.green,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    previewStage.status ?? 'N/A',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (previewStage.previewStartTime != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('开始时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(previewStage.previewStartTime!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            if (previewStage.previewEndTime != null)
              const SizedBox(height: 12),
            if (previewStage.previewEndTime != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('结束时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(previewStage.previewEndTime!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('分析文件数: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    '${previewStage.analyzedFiles ?? 0}',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('变更数量: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    '${previewStage.totalChanges ?? 0}',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
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
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8.0),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无执行结果信息', style: TextStyle(fontSize: 14, color: Colors.grey)),
        ),
      );
    }

    final executionStage = selectedTask.stages!.execution!;

    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
              decoration: BoxDecoration(
                color: Colors.purple.withOpacity(0.1),
                borderRadius: BorderRadius.circular(4.0),
              ),
              child: const Text(
                '执行结果',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.purple,
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    executionStage.status ?? 'N/A',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (executionStage.executionStartTime != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('开始时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(executionStage.executionStartTime!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            if (executionStage.executionEndTime != null)
              const SizedBox(height: 12),
            if (executionStage.executionEndTime != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('结束时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(executionStage.executionEndTime!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('执行文件数: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    '${executionStage.executedFiles ?? 0}',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('成功数: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    '${executionStage.successCount ?? 0}',
                    style: const TextStyle(fontSize: 14, color: Colors.green),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(
                  width: 100,
                  child: Text('失败数: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    '${executionStage.failedCount ?? 0}',
                    style: const TextStyle(fontSize: 14, color: Colors.red),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
