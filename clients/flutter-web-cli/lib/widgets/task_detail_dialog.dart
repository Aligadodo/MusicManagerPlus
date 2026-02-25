import 'package:flutter/material.dart';
import '../models/task_status.dart';
import './task_status_helpers.dart';

class TaskDetailDialog {
  static void show(BuildContext context, TaskStatus task) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('任务详情: ${task.taskId}'),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('状态: ${TaskStatusHelpers.getStatusText(task.status)}'),
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
                            TaskStatusHelpers.getOperationIcon(change.operationType),
                            color: TaskStatusHelpers.getOperationColor(change.operationType),
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
                                  TaskStatusHelpers.getStatusIcon(change.status),
                                  size: 16,
                                  color: TaskStatusHelpers.getChangeStatusColor(change.status),
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
      ),
    );
  }
}
