import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;
import '../../models/task_record.dart';
import '../../api/task_data_service.dart';
import 'generic_data_list.dart';
import 'column_config.dart';

/// 阶段状态类型
enum StageStatusType {
  notStarted,   // 未开始
  running,      // 进行中
  completed,    // 已完成
  failed,       // 失败
  cancelled,    // 已取消
}

/// 获取阶段状态类型
StageStatusType _getStageStatusType(String? status) {
  switch (status?.toUpperCase()) {
    case 'PENDING':
    case 'CREATED':
      return StageStatusType.notStarted;
    case 'SCANNING':
    case 'PREVIEWING':
    case 'EXECUTING':
      return StageStatusType.running;
    case 'SCANNED':
    case 'PREVIEWED':
    case 'COMPLETED':
      return StageStatusType.completed;
    case 'FAILED':
      return StageStatusType.failed;
    case 'CANCELLED':
      return StageStatusType.cancelled;
    default:
      return StageStatusType.notStarted;
  }
}

/// 获取状态显示文本
String _getStatusDisplayText(String? status, StageStatusType type) {
  switch (type) {
    case StageStatusType.notStarted:
      return '等待中';
    case StageStatusType.running:
      return '进行中...';
    case StageStatusType.completed:
      return '已完成';
    case StageStatusType.failed:
      return '失败';
    case StageStatusType.cancelled:
      return '已取消';
  }
}

/// 获取状态颜色
Color _getStatusColor(StageStatusType type) {
  switch (type) {
    case StageStatusType.notStarted:
      return Colors.grey;
    case StageStatusType.running:
      return Colors.blue;
    case StageStatusType.completed:
      return Colors.green;
    case StageStatusType.failed:
      return Colors.red;
    case StageStatusType.cancelled:
      return Colors.orange;
  }
}

/// 格式化时间
String _formatTime(int? timestamp) {
  if (timestamp == null) return '--';
  final dateTime = DateTime.fromMillisecondsSinceEpoch(timestamp);
  return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} '
         '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}:${dateTime.second.toString().padLeft(2, '0')}';
}

/// 格式化耗时
String _formatDuration(int? startTime, int? endTime) {
  if (startTime == null) return '--';
  final end = endTime ?? DateTime.now().millisecondsSinceEpoch;
  final duration = Duration(milliseconds: end - startTime);
  
  if (duration.inHours > 0) {
    return '${duration.inHours}小时${duration.inMinutes % 60}分钟';
  } else if (duration.inMinutes > 0) {
    return '${duration.inMinutes}分钟${duration.inSeconds % 60}秒';
  } else {
    return '${duration.inSeconds}秒';
  }
}

class ScanResultCard extends StatefulWidget {
  final task_models.TaskStatus selectedTask;

  const ScanResultCard({super.key, required this.selectedTask});

  @override
  State<ScanResultCard> createState() => _ScanResultCardState();
}

class _ScanResultCardState extends State<ScanResultCard> {
  final TaskDataService _taskDataService = TaskDataService();
  bool _showDataList = true;

  @override
  Widget build(BuildContext context) {
    // 从未执行过扫描
    if (widget.selectedTask.stages?.scan == null) {
      return _buildEmptyCard(
        title: '扫描结果',
        color: Colors.blue,
        message: '尚未开始扫描',
        icon: Icons.folder_open,
      );
    }

    final scanStage = widget.selectedTask.stages!.scan!;
    final statusType = _getStageStatusType(scanStage.status);
    final statusColor = _getStatusColor(statusType);
    final hasData = (scanStage.scannedFiles ?? 0) > 0;

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
            // 标题栏
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                  decoration: BoxDecoration(
                    color: Colors.blue.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4.0),
                  ),
                  child: const Text(
                    '扫描结果',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: Colors.blue,
                    ),
                  ),
                ),
                const Spacer(),
                // 状态标签
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: statusColor.withOpacity(0.3)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (statusType == StageStatusType.running)
                        SizedBox(
                          width: 12,
                          height: 12,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor: AlwaysStoppedAnimation<Color>(statusColor),
                          ),
                        ),
                      if (statusType == StageStatusType.running)
                        const SizedBox(width: 4),
                      Text(
                        _getStatusDisplayText(scanStage.status, statusType),
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            // 进行中显示进度
            if (statusType == StageStatusType.running)
              _buildProgressIndicator(
                scannedFiles: scanStage.scannedFiles ?? 0,
                totalFiles: scanStage.totalFiles,
              ),
            
            // 统计信息
            _buildInfoRow('扫描文件数', '${scanStage.scannedFiles ?? 0}${scanStage.totalFiles != null && scanStage.totalFiles! > 0 ? " / ${scanStage.totalFiles}" : ""}'),
            _buildInfoRow('开始时间', _formatTime(scanStage.scanStartTime)),
            if (scanStage.scanEndTime != null)
              _buildInfoRow('结束时间', _formatTime(scanStage.scanEndTime)),
            _buildInfoRow('耗时', _formatDuration(scanStage.scanStartTime, scanStage.scanEndTime)),
            
            // 结果显示
            if (statusType == StageStatusType.completed)
              _buildResultBanner(
                icon: Icons.check_circle,
                color: Colors.green,
                message: scanStage.scannedFiles == 0 
                    ? '扫描完成，未发现文件' 
                    : '扫描完成，共发现 ${scanStage.scannedFiles} 个文件',
              ),
            
            if (statusType == StageStatusType.failed)
              _buildResultBanner(
                icon: Icons.error,
                color: Colors.red,
                message: '扫描失败，请查看日志了解详情',
              ),

            // 数据列表展示
            if (statusType == StageStatusType.completed) ...[
              const SizedBox(height: 16),
              // 切换按钮
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: () {
                      setState(() {
                        _showDataList = !_showDataList;
                      });
                    },
                    icon: Icon(_showDataList ? Icons.visibility_off : Icons.visibility),
                    label: Text(_showDataList ? '隐藏文件列表' : '查看文件列表'),
                  ),
                ],
              ),
              // 数据列表
              if (_showDataList)
                Expanded(
                  child: GenericDataList(
                    columns: ScanColumnConfigs.defaultColumns,
                    onLoadData: (params) => _taskDataService.queryScanRecords(
                      widget.selectedTask.taskId!,
                      params,
                    ),
                    title: '扫描文件列表',
                    showSearch: true,
                    showPagination: true,
                    defaultPageSize: 20,
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

class PreviewResultCard extends StatefulWidget {
  final task_models.TaskStatus selectedTask;

  const PreviewResultCard({super.key, required this.selectedTask});

  @override
  State<PreviewResultCard> createState() => _PreviewResultCardState();
}

class _PreviewResultCardState extends State<PreviewResultCard> {
  final TaskDataService _taskDataService = TaskDataService();
  bool _showDataList = true;

  @override
  Widget build(BuildContext context) {
    // 从未执行过预览
    if (widget.selectedTask.stages?.preview == null) {
      return _buildEmptyCard(
        title: '预览结果',
        color: Colors.green,
        message: '尚未开始预览',
        icon: Icons.preview,
      );
    }

    final previewStage = widget.selectedTask.stages!.preview!;
    final statusType = _getStageStatusType(previewStage.status);
    final statusColor = _getStatusColor(statusType);
    final hasData = (previewStage.totalChanges ?? 0) > 0;

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
            // 标题栏
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                  decoration: BoxDecoration(
                    color: Colors.green.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4.0),
                  ),
                  child: const Text(
                    '预览结果',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: Colors.green,
                    ),
                  ),
                ),
                const Spacer(),
                // 状态标签
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: statusColor.withOpacity(0.3)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (statusType == StageStatusType.running)
                        SizedBox(
                          width: 12,
                          height: 12,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor: AlwaysStoppedAnimation<Color>(statusColor),
                          ),
                        ),
                      if (statusType == StageStatusType.running)
                        const SizedBox(width: 4),
                      Text(
                        _getStatusDisplayText(previewStage.status, statusType),
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            // 进行中显示进度
            if (statusType == StageStatusType.running)
              _buildProgressIndicator(
                scannedFiles: previewStage.analyzedFiles ?? 0,
                totalFiles: widget.selectedTask.stages?.scan?.scannedFiles,
              ),
            
            // 统计信息
            _buildInfoRow('分析文件数', '${previewStage.analyzedFiles ?? 0}'),
            _buildInfoRow('变更数量', '${previewStage.totalChanges ?? 0}'),
            _buildInfoRow('开始时间', _formatTime(previewStage.previewStartTime)),
            if (previewStage.previewEndTime != null)
              _buildInfoRow('结束时间', _formatTime(previewStage.previewEndTime)),
            _buildInfoRow('耗时', _formatDuration(previewStage.previewStartTime, previewStage.previewEndTime)),
            
            // 结果显示
            if (statusType == StageStatusType.completed)
              _buildResultBanner(
                icon: Icons.check_circle,
                color: Colors.green,
                message: previewStage.totalChanges == 0 
                    ? '预览完成，无文件需要变更' 
                    : '预览完成，共 ${previewStage.totalChanges} 个文件需要变更',
              ),
            
            if (statusType == StageStatusType.failed)
              _buildResultBanner(
                icon: Icons.error,
                color: Colors.red,
                message: '预览失败，请查看日志了解详情',
              ),

            // 数据列表展示
            if (statusType == StageStatusType.completed) ...[
              const SizedBox(height: 16),
              // 切换按钮
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: () {
                      setState(() {
                        _showDataList = !_showDataList;
                      });
                    },
                    icon: Icon(_showDataList ? Icons.visibility_off : Icons.visibility),
                    label: Text(_showDataList ? '隐藏变更列表' : '查看变更列表'),
                  ),
                ],
              ),
              // 数据列表
              if (_showDataList)
                Expanded(
                  child: GenericDataList(
                    columns: PreviewColumnConfigs.defaultColumns,
                    onLoadData: (params) => _taskDataService.queryPreviewRecords(
                      widget.selectedTask.taskId!,
                      params,
                    ),
                    title: '预览变更列表',
                    showSearch: true,
                    showPagination: true,
                    defaultPageSize: 20,
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

class ExecutionResultCard extends StatefulWidget {
  final task_models.TaskStatus selectedTask;

  const ExecutionResultCard({super.key, required this.selectedTask});

  @override
  State<ExecutionResultCard> createState() => _ExecutionResultCardState();
}

class _ExecutionResultCardState extends State<ExecutionResultCard> {
  final TaskDataService _taskDataService = TaskDataService();
  bool _showDataList = true;

  @override
  Widget build(BuildContext context) {
    // 从未执行过
    if (widget.selectedTask.stages?.execution == null) {
      return _buildEmptyCard(
        title: '执行结果',
        color: Colors.purple,
        message: '尚未开始执行',
        icon: Icons.play_arrow,
      );
    }

    final executionStage = widget.selectedTask.stages!.execution!;
    final statusType = _getStageStatusType(executionStage.status);
    final statusColor = _getStatusColor(statusType);
    final hasData = (executionStage.executedFiles ?? 0) > 0;

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
            // 标题栏
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
                  decoration: BoxDecoration(
                    color: Colors.purple.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4.0),
                  ),
                  child: const Text(
                    '执行结果',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: Colors.purple,
                    ),
                  ),
                ),
                const Spacer(),
                // 状态标签
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: statusColor.withOpacity(0.3)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (statusType == StageStatusType.running)
                        SizedBox(
                          width: 12,
                          height: 12,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor: AlwaysStoppedAnimation<Color>(statusColor),
                          ),
                        ),
                      if (statusType == StageStatusType.running)
                        const SizedBox(width: 4),
                      Text(
                        _getStatusDisplayText(executionStage.status, statusType),
                        style: TextStyle(
                          color: statusColor,
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            
            // 进行中显示进度
            if (statusType == StageStatusType.running)
              _buildProgressIndicator(
                scannedFiles: executionStage.executedFiles ?? 0,
                totalFiles: widget.selectedTask.stages?.preview?.totalChanges,
              ),
            
            // 统计信息
            _buildInfoRow('执行文件数', '${executionStage.executedFiles ?? 0}'),
            _buildInfoRow('成功数', '${executionStage.successCount ?? 0}', valueColor: Colors.green),
            _buildInfoRow('失败数', '${executionStage.failedCount ?? 0}', valueColor: Colors.red),
            _buildInfoRow('开始时间', _formatTime(executionStage.executionStartTime)),
            if (executionStage.executionEndTime != null)
              _buildInfoRow('结束时间', _formatTime(executionStage.executionEndTime)),
            _buildInfoRow('耗时', _formatDuration(executionStage.executionStartTime, executionStage.executionEndTime)),
            
            // 结果显示
            if (statusType == StageStatusType.completed)
              _buildResultBanner(
                icon: executionStage.failedCount == 0 ? Icons.check_circle : Icons.warning,
                color: executionStage.failedCount == 0 ? Colors.green : Colors.orange,
                message: executionStage.failedCount == 0 
                    ? '执行完成，所有操作成功' 
                    : '执行完成，${executionStage.successCount} 成功，${executionStage.failedCount} 失败',
              ),
            
            if (statusType == StageStatusType.failed)
              _buildResultBanner(
                icon: Icons.error,
                color: Colors.red,
                message: '执行失败，请查看日志了解详情',
              ),

            // 数据列表展示
            if (statusType == StageStatusType.completed || statusType == StageStatusType.failed) ...[
              const SizedBox(height: 16),
              // 切换按钮
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: () {
                      setState(() {
                        _showDataList = !_showDataList;
                      });
                    },
                    icon: Icon(_showDataList ? Icons.visibility_off : Icons.visibility),
                    label: Text(_showDataList ? '隐藏执行列表' : '查看执行列表'),
                  ),
                ],
              ),
              // 数据列表
              if (_showDataList)
                Expanded(
                  child: GenericDataList(
                    columns: ExecutionColumnConfigs.defaultColumns,
                    onLoadData: (params) => _taskDataService.queryExecutionRecords(
                      widget.selectedTask.taskId!,
                      params,
                    ),
                    title: '执行记录列表',
                    showSearch: true,
                    showPagination: true,
                    defaultPageSize: 20,
                  ),
                ),
            ],
          ],
        ),
      ),
    );
  }
}

// ==================== 辅助组件 ====================

/// 空状态卡片
Widget _buildEmptyCard({
  required String title,
  required Color color,
  required String message,
  required IconData icon,
}) {
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
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(4.0),
            ),
            child: Text(
              title,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ),
          const SizedBox(height: 24),
          Center(
            child: Column(
              children: [
                Icon(icon, size: 48, color: Colors.grey[400]),
                const SizedBox(height: 12),
                Text(
                  message,
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey[600],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
        ],
      ),
    ),
  );
}

/// 信息行
Widget _buildInfoRow(String label, String value, {Color? valueColor}) {
  return Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 100,
          child: Text(
            label,
            style: const TextStyle(
              fontWeight: FontWeight.w500,
              fontSize: 14,
              color: Colors.black54,
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: TextStyle(
              fontSize: 14,
              color: valueColor ?? Colors.black87,
              fontWeight: valueColor != null ? FontWeight.w600 : FontWeight.normal,
            ),
          ),
        ),
      ],
    ),
  );
}

/// 进度指示器
Widget _buildProgressIndicator({required int scannedFiles, int? totalFiles}) {
  final progress = totalFiles != null && totalFiles > 0 
      ? scannedFiles / totalFiles 
      : null;
  
  return Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      ClipRRect(
        borderRadius: BorderRadius.circular(4),
        child: LinearProgressIndicator(
          value: progress,
          backgroundColor: Colors.grey[200],
          valueColor: const AlwaysStoppedAnimation<Color>(Colors.blue),
          minHeight: 8,
        ),
      ),
      const SizedBox(height: 4),
      Text(
        '已处理: $scannedFiles${totalFiles != null ? " / $totalFiles" : ""}',
        style: TextStyle(
          fontSize: 12,
          color: Colors.grey[600],
        ),
      ),
      const SizedBox(height: 12),
    ],
  );
}

/// 结果横幅
Widget _buildResultBanner({
  required IconData icon,
  required Color color,
  required String message,
}) {
  return Container(
    margin: const EdgeInsets.only(top: 12),
    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
    decoration: BoxDecoration(
      color: color.withOpacity(0.1),
      borderRadius: BorderRadius.circular(8),
      border: Border.all(color: color.withOpacity(0.3)),
    ),
    child: Row(
      children: [
        Icon(icon, color: color, size: 20),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            message,
            style: TextStyle(
              color: color,
              fontWeight: FontWeight.w500,
              fontSize: 14,
            ),
          ),
        ),
      ],
    ),
  );
}
