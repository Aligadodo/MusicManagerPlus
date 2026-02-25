import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/task_status.dart';

class ChangeDetailsDialog extends StatelessWidget {
  final TaskStatus task;

  const ChangeDetailsDialog({super.key, required this.task});

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('任务详情: ${task.taskId}'),
      content: SizedBox(
        width: double.maxFinite,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('状态: ${_getStatusText(task.status)}'),
            Text('进度: ${(task.progress * 100).toStringAsFixed(1)}%'),
            Text('消息: ${task.message}'),
            const SizedBox(height: 16),
            const Text(
              '变更记录',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            if (task.changes.isEmpty)
              const Text('暂无变更记录')
            else
              Expanded(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: task.changes.length,
                  itemBuilder: (context, index) {
                    final change = task.changes[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(vertical: 4),
                      child: ListTile(
                        leading: Icon(
                          _getOperationIcon(change.operationType),
                          color: _getOperationColor(change.operationType),
                        ),
                        title: Text(change.originalName),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('新名称: ${change.newName}'),
                            if (change.reason != null && change.reason!.isNotEmpty)
                              Text('原因: ${change.reason}'),
                            if (change.extraParams != null && change.extraParams!.isNotEmpty)
                              Text('额外参数: ${change.extraParams}'),
                          ],
                        ),
                        trailing: SizedBox(
                          width: 100,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              Icon(
                                _getStatusIcon(change.status),
                                size: 16,
                                color: _getChangeStatusColor(change.status),
                              ),
                              const SizedBox(width: 4),
                              Text(change.status),
                            ],
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('关闭'),
        ),
      ],
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

  IconData _getStatusIcon(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return Icons.check_circle;
      case 'FAILED':
        return Icons.error;
      case 'PENDING':
        return Icons.pending;
      default:
        return Icons.help_outline;
    }
  }

  Color _getChangeStatusColor(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'PENDING':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }

  IconData _getOperationIcon(String? operationType) {
    switch (operationType?.toUpperCase()) {
      case 'RENAME':
        return Icons.edit;
      case 'MOVE':
        return Icons.drive_file_move;
      case 'DELETE':
        return Icons.delete;
      case 'COPY':
        return Icons.copy;
      case 'METADATA_UPDATE':
        return Icons.info;
      case 'CONVERT':
        return Icons.transform;
      case 'MERGE':
        return Icons.merge_type;
      case 'UNZIP':
        return Icons.unarchive;
      case 'FIX_TYPE':
        return Icons.build;
      case 'DEDUP':
        return Icons.content_copy;
      case 'SPLIT':
        return Icons.call_split;
      case 'ALBUM_RENAME':
        return Icons.folder;
      default:
        return Icons.description;
    }
  }

  Color _getOperationColor(String? operationType) {
    switch (operationType?.toUpperCase()) {
      case 'RENAME':
        return Colors.blue;
      case 'MOVE':
        return Colors.orange;
      case 'DELETE':
        return Colors.red;
      case 'COPY':
        return Colors.green;
      case 'METADATA_UPDATE':
        return Colors.purple;
      case 'CONVERT':
        return Colors.teal;
      case 'MERGE':
        return Colors.indigo;
      case 'UNZIP':
        return Colors.amber;
      case 'FIX_TYPE':
        return Colors.cyan;
      case 'DEDUP':
        return Colors.lime;
      case 'SPLIT':
        return Colors.pink;
      case 'ALBUM_RENAME':
        return Colors.deepOrange;
      default:
        return Colors.grey;
    }
  }
}
