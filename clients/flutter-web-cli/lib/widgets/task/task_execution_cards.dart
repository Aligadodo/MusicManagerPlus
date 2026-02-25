import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class PreviewResultCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const PreviewResultCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
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
            _buildResultField('预览文件数', selectedTask.previewResult?.totalFiles),
            _buildResultField('变更文件数', selectedTask.previewResult?.changeCount),
            _buildResultField('预览耗时', '${selectedTask.previewResult?.costTime}秒'),
            if (selectedTask.previewResult?.changeDetails != null && selectedTask.previewResult!.changeDetails!.isNotEmpty) ...[
              const SizedBox(height: 8),
              const Text('变更详情:', style: TextStyle(fontWeight: FontWeight.w500)),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[50],
                  border: Border.all(color: Colors.grey[200]),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  selectedTask.previewResult!.changeDetails!.join('\n'),
                  style: const TextStyle(
                    fontSize: 13,
                    fontFamily: 'Monospace',
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildResultField(String label, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Text(
            label, style: const TextStyle(fontWeight: FontWeight.w500),
          ),
          const SizedBox(width: 8),
          Text(value?.toString() ?? 'N/A'),
        ],
      ),
    );
  }
}

class ExecutionResultCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ExecutionResultCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
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
            _buildResultField('执行文件数', selectedTask.executionResult?.totalFiles),
            _buildResultField('成功执行', selectedTask.executionResult?.successCount),
            _buildResultField('执行失败', selectedTask.executionResult?.failedCount),
            _buildResultField('执行耗时', '${selectedTask.executionResult?.costTime}秒'),
            if (selectedTask.executionResult?.errorFiles != null && selectedTask.executionResult!.errorFiles!.isNotEmpty) ...[
              const SizedBox(height: 8),
              const Text('错误文件:', style: TextStyle(fontWeight: FontWeight.w500)),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[50],
                  border: Border.all(color: Colors.grey[200]),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  selectedTask.executionResult!.errorFiles!.join('\n'),
                  style: const TextStyle(
                    fontSize: 13,
                    fontFamily: 'Monospace',
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildResultField(String label, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Text(
            label, style: const TextStyle(fontWeight: FontWeight.w500),
          ),
          const SizedBox(width: 8),
          Text(value?.toString() ?? 'N/A'),
        ],
      ),
    );
  }
}
