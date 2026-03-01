import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

/// 任务阶段指示器
/// 显示任务的三个阶段：扫描 -> 预览 -> 执行
class TaskStageIndicator extends StatelessWidget {
  final task_models.TaskStatus task;
  final bool showLabels;
  final bool isInteractive;
  final Function(String)? onStageTap;

  const TaskStageIndicator({
    super.key,
    required this.task,
    this.showLabels = true,
    this.isInteractive = false,
    this.onStageTap,
  });

  @override
  Widget build(BuildContext context) {
    final stages = [
      _StageInfo(
        key: 'SCAN',
        label: '扫描',
        icon: Icons.folder_open,
        status: _getStageStatus('SCAN'),
      ),
      _StageInfo(
        key: 'PREVIEW',
        label: '预览',
        icon: Icons.preview,
        status: _getStageStatus('PREVIEW'),
      ),
      _StageInfo(
        key: 'EXECUTION',
        label: '执行',
        icon: Icons.play_arrow,
        status: _getStageStatus('EXECUTION'),
      ),
    ];

    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (int i = 0; i < stages.length; i++) ...[
          _buildStageNode(stages[i]),
          if (i < stages.length - 1) _buildConnector(stages[i], stages[i + 1]),
        ],
      ],
    );
  }

  Widget _buildStageNode(_StageInfo stage) {
    final color = _getStageColor(stage.status);
    final icon = _getStageIcon(stage.status, stage.icon);

    Widget node = Container(
      width: 28,
      height: 28,
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(
          color: color,
          width: stage.status == _StageStatus.running ? 2 : 1,
        ),
      ),
      child: Center(
        child: Icon(
          icon,
          size: 14,
          color: color,
        ),
      ),
    );

    if (isInteractive && onStageTap != null) {
      node = InkWell(
        onTap: () => onStageTap!(stage.key),
        borderRadius: BorderRadius.circular(14),
        child: node,
      );
    }

    if (!showLabels) return node;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        node,
        const SizedBox(height: 2),
        Text(
          stage.label,
          style: TextStyle(
            fontSize: 9,
            color: color,
            fontWeight: stage.status == _StageStatus.running
                ? FontWeight.bold
                : FontWeight.normal,
          ),
        ),
      ],
    );
  }

  Widget _buildConnector(_StageInfo from, _StageInfo to) {
    Color color;
    if (from.status == _StageStatus.completed) {
      color = Colors.green;
    } else {
      color = Colors.grey.shade300;
    }

    return Container(
      width: 16,
      height: 2,
      margin: const EdgeInsets.symmetric(horizontal: 2),
      color: color,
    );
  }

  _StageStatus _getStageStatus(String stageKey) {
    final currentStage = task.currentStage ?? 'CREATED';
    final taskStatus = task.status ?? 'CREATED';

    // 获取阶段的具体状态
    String? stageStatus;
    switch (stageKey) {
      case 'SCAN':
        stageStatus = task.stages?.scan?.status?.toUpperCase();
        break;
      case 'PREVIEW':
        stageStatus = task.stages?.preview?.status?.toUpperCase();
        break;
      case 'EXECUTION':
        stageStatus = task.stages?.execution?.status?.toUpperCase();
        break;
    }

    // 根据阶段状态判断
    if (stageStatus == 'COMPLETED') {
      return _StageStatus.completed;
    } else if (stageStatus == 'RUNNING' || stageStatus == 'IN_PROGRESS') {
      return _StageStatus.running;
    } else if (stageStatus == 'FAILED') {
      return _StageStatus.failed;
    } else if (stageStatus == 'CANCELLED') {
      return _StageStatus.cancelled;
    }

    // 根据当前阶段和任务状态推断
    if (currentStage == stageKey) {
      if (taskStatus == 'SCANNING' ||
          taskStatus == 'PREVIEWING' ||
          taskStatus == 'EXECUTING') {
        return _StageStatus.running;
      } else if (taskStatus == 'FAILED') {
        return _StageStatus.failed;
      } else if (taskStatus == 'CANCELLED') {
        return _StageStatus.cancelled;
      }
    }

    // 判断阶段是否已完成
    final stageOrder = ['SCAN', 'PREVIEW', 'EXECUTION'];
    final currentIndex = stageOrder.indexOf(currentStage);
    final stageIndex = stageOrder.indexOf(stageKey);

    if (stageIndex < currentIndex) {
      return _StageStatus.completed;
    }

    return _StageStatus.pending;
  }

  Color _getStageColor(_StageStatus status) {
    switch (status) {
      case _StageStatus.completed:
        return Colors.green;
      case _StageStatus.running:
        return Colors.blue;
      case _StageStatus.failed:
        return Colors.red;
      case _StageStatus.cancelled:
        return Colors.grey;
      case _StageStatus.pending:
        return Colors.grey.shade400;
    }
  }

  IconData _getStageIcon(_StageStatus status, IconData defaultIcon) {
    switch (status) {
      case _StageStatus.completed:
        return Icons.check;
      case _StageStatus.running:
        return defaultIcon;
      case _StageStatus.failed:
        return Icons.close;
      case _StageStatus.cancelled:
        return Icons.block;
      case _StageStatus.pending:
        return Icons.circle_outlined;
    }
  }
}

enum _StageStatus {
  pending,    // 未开始
  running,    // 进行中
  completed,  // 已完成
  failed,     // 失败
  cancelled,  // 已取消
}

class _StageInfo {
  final String key;
  final String label;
  final IconData icon;
  final _StageStatus status;

  _StageInfo({
    required this.key,
    required this.label,
    required this.icon,
    required this.status,
  });
}
