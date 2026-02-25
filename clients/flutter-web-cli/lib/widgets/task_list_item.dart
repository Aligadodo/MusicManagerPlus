import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/task_status.dart';

class TaskListItem extends StatelessWidget {
  final TaskStatus task;
  final Function() onViewDetails;
  final Function()? onExecute;
  final Function()? onCancel;

  const TaskListItem({
    super.key,
    required this.task,
    required this.onViewDetails,
    this.onExecute,
    this.onCancel,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 6),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade200),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    task.taskId,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                Chip(
                  label: Text(
                    _getStatusText(task.status),
                    style: TextStyle(
                      color: _getStatusColor(task.status),
                    ),
                  ),
                  backgroundColor: _getStatusBackgroundColor(task.status),
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  visualDensity: VisualDensity.compact,
                ),
              ],
            ),
            const SizedBox(height: 8),
            LinearProgressIndicator(
              value: task.progress,
              backgroundColor: Colors.grey[200],
              valueColor: AlwaysStoppedAnimation<Color>(
                _getProgressColor(task.status),
              ),
              minHeight: 6,
            ),
            const SizedBox(height: 4),
            Text(
              '进度: ${(task.progress * 100).toStringAsFixed(1)}%',
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              task.message,
              style: const TextStyle(
                fontSize: 14,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              '变更记录: ${task.changes.length} 条',
              style: const TextStyle(
                fontSize: 12,
                color: Colors.grey,
              ),
            ),
            if (task.totalFiles > 0)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(height: 8),
                  Text(
                    '统计信息',
                    style: const TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: Colors.grey,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    '总文件: ${task.totalFiles}',
                    style: const TextStyle(fontSize: 12),
                  ),
                  Text(
                    '已处理: ${task.processedFiles}',
                    style: const TextStyle(fontSize: 12),
                  ),
                  Row(
                    children: [
                      Text(
                        '成功: ${task.successCount}',
                        style: const TextStyle(fontSize: 12, color: Colors.green),
                      ),
                      const SizedBox(width: 16),
                      Text(
                        '失败: ${task.failedCount}',
                        style: const TextStyle(fontSize: 12, color: Colors.red),
                      ),
                      const SizedBox(width: 16),
                      Text(
                        '跳过: ${task.skippedCount}',
                        style: const TextStyle(fontSize: 12, color: Colors.orange),
                      ),
                    ],
                  ),
                  if (task.operationStats != null && task.operationStats!.isNotEmpty)
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SizedBox(height: 4),
                        const Text(
                          '操作统计',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Colors.grey,
                          ),
                        ),
                        const SizedBox(height: 4),
                        ...task.operationStats!.entries.map((entry) {
                          return Padding(
                            padding: const EdgeInsets.only(left: 8),
                            child: Text(
                              '${entry.key}: ${entry.value}',
                              style: const TextStyle(fontSize: 12),
                            ),
                          );
                        }).toList(),
                      ],
                    ),
                ],
              ),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                if (task.changes.isNotEmpty)
                  TextButton.icon(
                    onPressed: onViewDetails,
                    icon: const Icon(Icons.list, size: 14),
                    label: const Text('查看详情', style: TextStyle(fontSize: 12)),
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    ),
                  ),
                const SizedBox(width: 8),
                if (onExecute != null)
                  ElevatedButton(
                    onPressed: onExecute,
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    ),
                    child: const Text('执行', style: TextStyle(fontSize: 12)),
                  ),
                if (onCancel != null)
                  ElevatedButton(
                    onPressed: onCancel,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.orange,
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    ),
                    child: const Text('取消', style: TextStyle(fontSize: 12)),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _getStatusText(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return '等待中';
      case TaskStatusEnum.RUNNING:
        return '运行中';
      case TaskStatusEnum.SUCCESS:
        return '成功';
      case TaskStatusEnum.FAILED:
        return '失败';
      case TaskStatusEnum.CANCELLED:
        return '已取消';
    }
  }

  Color _getStatusColor(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return Colors.blue;
      case TaskStatusEnum.RUNNING:
        return Colors.green;
      case TaskStatusEnum.SUCCESS:
        return Colors.green;
      case TaskStatusEnum.FAILED:
        return Colors.red;
      case TaskStatusEnum.CANCELLED:
        return Colors.orange;
    }
  }

  Color _getStatusBackgroundColor(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return Colors.blue[100]!;
      case TaskStatusEnum.RUNNING:
        return Colors.green[100]!;
      case TaskStatusEnum.SUCCESS:
        return Colors.green[100]!;
      case TaskStatusEnum.FAILED:
        return Colors.red[100]!;
      case TaskStatusEnum.CANCELLED:
        return Colors.orange[100]!;
    }
  }

  Color _getProgressColor(TaskStatusEnum status) {
    switch (status) {
      case TaskStatusEnum.PENDING:
        return Colors.blue;
      case TaskStatusEnum.RUNNING:
        return Colors.green;
      case TaskStatusEnum.SUCCESS:
        return Colors.green;
      case TaskStatusEnum.FAILED:
        return Colors.red;
      case TaskStatusEnum.CANCELLED:
        return Colors.orange;
    }
  }
}
