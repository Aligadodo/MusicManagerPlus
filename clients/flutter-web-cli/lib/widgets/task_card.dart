import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/task_status.dart';

class TaskCard extends StatelessWidget {
  final TaskStatus task;
  final VoidCallback? onTap;
  final VoidCallback? onCancel;
  final VoidCallback? onDelete;

  const TaskCard({
    Key? key,
    required this.task,
    this.onTap,
    this.onCancel,
    this.onDelete,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _buildStatusIcon(),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          task.taskName ?? '未命名任务',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          task.taskId ?? '',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                  ),
                  _buildActionButtons(context),
                ],
              ),
              const SizedBox(height: 12),
              _buildProgressBar(),
              const SizedBox(height: 8),
              _buildTaskInfo(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatusIcon() {
    IconData iconData;
    Color iconColor;

    switch (task.status) {
      case 'CREATED':
        iconData = Icons.folder_open;
        iconColor = Colors.blue;
        break;
      case 'SCANNING':
        iconData = Icons.scanner;
        iconColor = Colors.orange;
        break;
      case 'SCANNED':
        iconData = Icons.check_circle_outline;
        iconColor = Colors.green;
        break;
      case 'PREVIEWING':
        iconData = Icons.preview;
        iconColor = Colors.orange;
        break;
      case 'PREVIEWED':
        iconData = Icons.check_circle_outline;
        iconColor = Colors.green;
        break;
      case 'EXECUTING':
        iconData = Icons.play_circle_outline;
        iconColor = Colors.orange;
        break;
      case 'COMPLETED':
        iconData = Icons.check_circle;
        iconColor = Colors.green;
        break;
      case 'FAILED':
        iconData = Icons.error;
        iconColor = Colors.red;
        break;
      case 'CANCELLED':
        iconData = Icons.cancel;
        iconColor = Colors.grey;
        break;
      default:
        iconData = Icons.help_outline;
        iconColor = Colors.grey;
    }

    return Icon(iconData, color: iconColor, size: 32);
  }

  Widget _buildActionButtons(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (_canCancel())
          IconButton(
            icon: const Icon(Icons.cancel_outlined),
            onPressed: onCancel,
            tooltip: '取消任务',
          ),
        if (_canDelete())
          IconButton(
            icon: const Icon(Icons.delete_outline),
            onPressed: onDelete,
            tooltip: '删除任务',
          ),
      ],
    );
  }

  bool _canCancel() {
    return ['SCANNING', 'PREVIEWING', 'EXECUTING'].contains(task.status);
  }

  bool _canDelete() {
    return ['CREATED', 'SCANNED', 'PREVIEWED', 'COMPLETED', 'FAILED', 'CANCELLED'].contains(task.status);
  }

  Widget _buildProgressBar() {
    final progress = task.overallProgress ?? 0.0;
    final currentStage = task.currentStage ?? '';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              currentStage,
              style: const TextStyle(fontSize: 12),
            ),
            Text(
              '${progress.toStringAsFixed(1)}%',
              style: const TextStyle(fontSize: 12),
            ),
          ],
        ),
        const SizedBox(height: 4),
        LinearProgressIndicator(
          value: progress / 100,
          backgroundColor: Colors.grey[200],
          valueColor: AlwaysStoppedAnimation<Color>(
            _getProgressColor(progress),
          ),
        ),
      ],
    );
  }

  Color _getProgressColor(double progress) {
    if (progress >= 100) return Colors.green;
    if (progress >= 50) return Colors.blue;
    return Colors.orange;
  }

  Widget _buildTaskInfo() {
    final createdAt = task.createdAt != null
        ? DateTime.fromMillisecondsSinceEpoch(task.createdAt!)
        : null;
    final message = task.message ?? '';

    return Row(
      children: [
        Icon(Icons.access_time, size: 14, color: Colors.grey[600]),
        const SizedBox(width: 4),
        Text(
          createdAt != null
              ? _formatDateTime(createdAt)
              : '未知时间',
          style: TextStyle(fontSize: 12, color: Colors.grey[600]),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Text(
            message,
            style: TextStyle(fontSize: 12, color: Colors.grey[600]),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  String _formatDateTime(DateTime dateTime) {
    final now = DateTime.now();
    final difference = now.difference(dateTime);

    if (difference.inMinutes < 1) {
      return '刚刚';
    } else if (difference.inHours < 1) {
      return '${difference.inMinutes}分钟前';
    } else if (difference.inDays < 1) {
      return '${difference.inHours}小时前';
    } else if (difference.inDays < 7) {
      return '${difference.inDays}天前';
    } else {
      return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')}';
    }
  }
}
