import 'package:flutter/material.dart';
import '../../models/task_status.dart';

class TaskStatusHelpers {
  static String getStatusText(TaskStatusEnum status) {
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

  static Color getStatusColor(TaskStatusEnum status) {
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

  static Color getStatusBackgroundColor(TaskStatusEnum status) {
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

  static Color getProgressColor(TaskStatusEnum status) {
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

  static IconData getStatusIcon(String status) {
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

  static Color getChangeStatusColor(String status) {
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

  static IconData getOperationIcon(String? operationType) {
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

  static Color getOperationColor(String? operationType) {
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
