import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class TaskDetailHeader extends StatelessWidget {
  final Function() onBack;

  const TaskDetailHeader({super.key, required this.onBack});

  @override
  Widget build(BuildContext context) {
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
}

class TaskInfoCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const TaskInfoCard({super.key, required this.selectedTask});

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
                    selectedTask.taskId ?? 'N/A',
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
                    selectedTask.taskName ?? '未命名任务',
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
                  _getFriendlyStatus(selectedTask.status ?? 'UNKNOWN'),
                  style: TextStyle(
                    fontSize: 13,
                    color: _getStatusColor(selectedTask.status ?? 'UNKNOWN'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              children: [
                const Text('当前阶段: ', style: TextStyle(fontWeight: FontWeight.w500)),
                Text(
                  selectedTask.currentStage ?? 'N/A',
                  style: const TextStyle(fontSize: 13),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (selectedTask.createdAt != null)
              Row(
                children: [
                  const Text('创建时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(selectedTask.createdAt!).toString(),
                      style: const TextStyle(fontSize: 13),
                    ),
                  ),
                ],
              ),
            if (selectedTask.message != null && selectedTask.message!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('消息: ', style: TextStyle(fontWeight: FontWeight.w500)),
                    Expanded(
                      child: Text(
                        selectedTask.message!,
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
