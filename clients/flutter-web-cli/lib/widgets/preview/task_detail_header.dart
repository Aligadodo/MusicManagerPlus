import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class TaskDetailHeader extends StatelessWidget {
  final Function() onBack;
  final task_models.TaskStatus? selectedTask;
  final Function(String)? onRestartScan;
  final Function(String)? onRestartPreview;
  final Function(String)? onRestartExecution;
  final Function(String)? onRerunTask;
  final Function(String)? onCancelTask;

  const TaskDetailHeader({
    super.key,
    required this.onBack,
    this.selectedTask,
    this.onRestartScan,
    this.onRestartPreview,
    this.onRestartExecution,
    this.onRerunTask,
    this.onCancelTask,
  });

  @override
  Widget build(BuildContext context) {
    final status = selectedTask?.status ?? 'UNKNOWN';
    final currentStage = selectedTask?.currentStage ?? 'UNKNOWN';

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
        Row(
          children: [
            // 重新扫描按钮
            if (onRestartScan != null && (status == 'SCANNED' || status == 'PREVIEWED' || status == 'COMPLETED' || status == 'FAILED' || status == 'CANCELLED'))
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: () => onRestartScan!(selectedTask!.taskId!),
                  icon: const Icon(Icons.refresh, size: 16),
                  label: const Text('重新扫描'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.orange,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ),

            // 重新预览按钮
            if (onRestartPreview != null && (status == 'PREVIEWED' || status == 'COMPLETED' || status == 'FAILED' || status == 'CANCELLED'))
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: () => onRestartPreview!(selectedTask!.taskId!),
                  icon: const Icon(Icons.preview, size: 16),
                  label: const Text('重新预览'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.purple,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ),

            // 重新执行按钮
            if (onRestartExecution != null && (status == 'COMPLETED' || status == 'FAILED' || status == 'CANCELLED'))
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: () => onRestartExecution!(selectedTask!.taskId!),
                  icon: const Icon(Icons.play_arrow, size: 16),
                  label: const Text('重新执行'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ),

            // 重新运行按钮
            if (onRerunTask != null && (status == 'FAILED' || status == 'CANCELLED'))
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: () => onRerunTask!(selectedTask!.taskId!),
                  icon: const Icon(Icons.replay, size: 16),
                  label: const Text('重新运行'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.blue,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
                ),
              ),

            // 终止任务按钮
            if (onCancelTask != null && (status == 'SCANNING' || status == 'PREVIEWING' || status == 'EXECUTING'))
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: () => onCancelTask!(selectedTask!.taskId!),
                  icon: const Icon(Icons.stop, size: 16),
                  label: const Text('终止'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  ),
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
                '任务基本信息',
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
                  child: Text('任务ID: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    selectedTask.taskId ?? 'N/A',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                    overflow: TextOverflow.ellipsis,
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
                  child: Text('任务名称: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    selectedTask.taskName ?? '未命名任务',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                    overflow: TextOverflow.ellipsis,
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
                  child: Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Chip(
                  label: Text(
                    _getFriendlyStatus(selectedTask.status ?? 'UNKNOWN'),
                    style: TextStyle(
                      fontSize: 12,
                      color: _getStatusColor(selectedTask.status ?? 'UNKNOWN'),
                    ),
                  ),
                  backgroundColor: _getStatusColor(selectedTask.status ?? 'UNKNOWN').withOpacity(0.1),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12.0),
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
                  child: Text('当前阶段: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                ),
                Expanded(
                  child: Text(
                    selectedTask.currentStage ?? 'N/A',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (selectedTask.createdAt != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('创建时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(selectedTask.createdAt!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            if (selectedTask.message != null && selectedTask.message!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(
                      width: 100,
                      child: Text('消息: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                    ),
                    Expanded(
                      child: Text(
                        selectedTask.message!,
                        style: const TextStyle(fontSize: 14, color: Colors.black87),
                        textAlign: TextAlign.left,
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
