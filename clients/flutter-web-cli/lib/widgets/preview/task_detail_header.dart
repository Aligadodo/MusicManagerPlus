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
            if (onRestartScan != null)
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: (status == 'SCANNED' || status == 'PREVIEWED' || status == 'COMPLETED' || status == 'FAILED' || status == 'CANCELLED')
                      ? () => onRestartScan!(selectedTask!.taskId!)
                      : null,
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
            if (onRestartPreview != null)
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: (status == 'PREVIEWED' || status == 'COMPLETED' || status == 'FAILED' || status == 'CANCELLED')
                      ? () => onRestartPreview!(selectedTask!.taskId!)
                      : null,
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
            if (onRestartExecution != null)
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: (status == 'COMPLETED' || status == 'FAILED' || status == 'CANCELLED')
                      ? () => onRestartExecution!(selectedTask!.taskId!)
                      : null,
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
            if (onRerunTask != null)
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: (status == 'FAILED' || status == 'CANCELLED')
                      ? () => onRerunTask!(selectedTask!.taskId!)
                      : null,
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
            if (onCancelTask != null)
              Padding(
                padding: const EdgeInsets.only(right: 8.0),
                child: ElevatedButton.icon(
                  onPressed: (status == 'SCANNING' || status == 'PREVIEWING' || status == 'EXECUTING')
                      ? () => onCancelTask!(selectedTask!.taskId!)
                      : null,
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

class TaskInfoCard extends StatefulWidget {
  final task_models.TaskStatus selectedTask;
  final Function(String)? onTaskNameChanged;

  const TaskInfoCard({super.key, required this.selectedTask, this.onTaskNameChanged});

  @override
  State<TaskInfoCard> createState() => _TaskInfoCardState();
}

class _TaskInfoCardState extends State<TaskInfoCard> {
  bool _isEditing = false;
  late TextEditingController _nameController;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.selectedTask.taskName ?? '未命名任务');
  }

  @override
  void didUpdateWidget(TaskInfoCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.selectedTask.taskName != widget.selectedTask.taskName) {
      _nameController.text = widget.selectedTask.taskName ?? '未命名任务';
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  void _saveTaskName() {
    final newName = _nameController.text.trim();
    if (newName.isNotEmpty && newName != widget.selectedTask.taskName) {
      widget.onTaskNameChanged?.call(newName);
    }
    setState(() {
      _isEditing = false;
    });
  }

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
                    widget.selectedTask.taskId ?? 'N/A',
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
                  child: _isEditing
                      ? Row(
                          children: [
                            Expanded(
                              child: TextField(
                                controller: _nameController,
                                style: const TextStyle(fontSize: 14),
                                decoration: const InputDecoration(
                                  isDense: true,
                                  contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                                  border: OutlineInputBorder(),
                                ),
                                onSubmitted: (_) => _saveTaskName(),
                              ),
                            ),
                            const SizedBox(width: 8),
                            IconButton(
                              icon: const Icon(Icons.check, color: Colors.green, size: 20),
                              onPressed: _saveTaskName,
                              tooltip: '保存',
                            ),
                            IconButton(
                              icon: const Icon(Icons.close, color: Colors.red, size: 20),
                              onPressed: () {
                                _nameController.text = widget.selectedTask.taskName ?? '未命名任务';
                                setState(() {
                                  _isEditing = false;
                                });
                              },
                              tooltip: '取消',
                            ),
                          ],
                        )
                      : Row(
                          children: [
                            Expanded(
                              child: Text(
                                widget.selectedTask.taskName ?? '未命名任务',
                                style: const TextStyle(fontSize: 14, color: Colors.black87),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            IconButton(
                              icon: const Icon(Icons.edit, size: 16, color: Colors.blue),
                              onPressed: () {
                                _nameController.text = widget.selectedTask.taskName ?? '未命名任务';
                                setState(() {
                                  _isEditing = true;
                                });
                              },
                              tooltip: '编辑任务名称',
                            ),
                          ],
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
                    _getFriendlyStatus(widget.selectedTask.status ?? 'UNKNOWN'),
                    style: TextStyle(
                      fontSize: 12,
                      color: _getStatusColor(widget.selectedTask.status ?? 'UNKNOWN'),
                    ),
                  ),
                  backgroundColor: _getStatusColor(widget.selectedTask.status ?? 'UNKNOWN').withOpacity(0.1),
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
                    widget.selectedTask.currentStage ?? 'N/A',
                    style: const TextStyle(fontSize: 14, color: Colors.black87),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (widget.selectedTask.createdAt != null)
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const SizedBox(
                    width: 100,
                    child: Text('创建时间: ', style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14)),
                  ),
                  Expanded(
                    child: Text(
                      DateTime.fromMillisecondsSinceEpoch(widget.selectedTask.createdAt!).toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            if (widget.selectedTask.message != null && widget.selectedTask.message!.isNotEmpty)
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
                        widget.selectedTask.message!,
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
