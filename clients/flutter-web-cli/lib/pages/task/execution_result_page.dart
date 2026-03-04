import 'package:flutter/material.dart';
import '../../api/task_data_service.dart';
import '../../models/task_record.dart';
import '../../widgets/common/column_config.dart';
import '../../widgets/common/generic_data_list.dart';

/// 执行结果页面
/// 使用通用数据列表组件展示执行阶段的操作结果
class ExecutionResultPage extends StatefulWidget {
  final String taskId;
  final String? taskName;

  const ExecutionResultPage({
    Key? key,
    required this.taskId,
    this.taskName,
  }) : super(key: key);

  @override
  State<ExecutionResultPage> createState() => _ExecutionResultPageState();
}

class _ExecutionResultPageState extends State<ExecutionResultPage> {
  final TaskDataService _dataService = TaskDataService();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.taskName ?? '任务'} - 执行结果'),
        actions: [
          // 返回预览按钮
          TextButton.icon(
            onPressed: _goBackToPreview,
            icon: const Icon(Icons.arrow_back, color: Colors.white),
            label: const Text('返回预览', style: TextStyle(color: Colors.white)),
          ),
          const SizedBox(width: 8),
          // 导出按钮
          IconButton(
            icon: const Icon(Icons.download),
            tooltip: '导出执行结果',
            onPressed: _exportExecutionResults,
          ),
          // 完成任务按钮
          ElevatedButton.icon(
            onPressed: _completeTask,
            icon: const Icon(Icons.check),
            label: const Text('完成任务'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue,
              foregroundColor: Colors.white,
            ),
          ),
          const SizedBox(width: 16),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: GenericDataList(
          title: '执行结果列表',
          columns: ExecutionColumnConfigs.defaultColumns,
          onLoadData: (params) => _dataService.queryExecutionRecords(widget.taskId, params),
          showSearch: true,
          showPagination: true,
          showColumnSettings: true,
          showRefresh: true,
          enableRowSelection: true,
          multiSelect: true,
          toolbarActions: [
            // 执行状态筛选
            _buildExecutionStatusFilter(),
            const SizedBox(width: 8),
            // 操作类型筛选
            _buildOperationTypeFilter(),
            const SizedBox(width: 8),
            // 统计信息
            _buildStatistics(),
          ],
          onRowSelect: (record, selected, isShiftClick) {
            print('选中记录: ${record.originalName}, 选中状态: $selected');
          },
          onRowDoubleTap: (record) {
            _showExecutionDetail(record);
          },
        ),
      ),
    );
  }

  /// 构建执行状态筛选器
  Widget _buildExecutionStatusFilter() {
    return DropdownButton<String>(
      hint: const Text('执行状态'),
      items: const [
        DropdownMenuItem(value: null, child: Text('全部状态')),
        DropdownMenuItem(value: 'SUCCESS', child: Text('成功')),
        DropdownMenuItem(value: 'FAILED', child: Text('失败')),
        DropdownMenuItem(value: 'SKIPPED', child: Text('跳过')),
        DropdownMenuItem(value: 'PENDING', child: Text('待执行')),
      ],
      onChanged: (value) {
        // 应用筛选
      },
    );
  }

  /// 构建操作类型筛选器
  Widget _buildOperationTypeFilter() {
    return DropdownButton<String>(
      hint: const Text('操作类型'),
      items: const [
        DropdownMenuItem(value: null, child: Text('全部类型')),
        DropdownMenuItem(value: 'RENAME', child: Text('重命名')),
        DropdownMenuItem(value: 'MOVE', child: Text('移动')),
        DropdownMenuItem(value: 'DELETE', child: Text('删除')),
        DropdownMenuItem(value: 'COPY', child: Text('复制')),
      ],
      onChanged: (value) {
        // 应用筛选
      },
    );
  }

  /// 构建统计信息
  Widget _buildStatistics() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildStatItem('成功', Colors.green, 0),
          const SizedBox(width: 12),
          _buildStatItem('失败', Colors.red, 0),
          const SizedBox(width: 12),
          _buildStatItem('跳过', Colors.orange, 0),
        ],
      ),
    );
  }

  /// 构建统计项
  Widget _buildStatItem(String label, Color color, int count) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 4),
        Text('$label: $count', style: const TextStyle(fontSize: 12)),
      ],
    );
  }

  /// 显示执行详情
  void _showExecutionDetail(TaskRecord record) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('执行详情'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildDetailSection('文件信息', [
                _buildDetailRow('原文件名', record.originalName),
                _buildDetailRow('目标文件名', record.newName.isNotEmpty ? record.newName : '-'),
                _buildDetailRow('原路径', record.originalPath),
                _buildDetailRow('目标路径', record.newPath.isNotEmpty ? record.newPath : '-'),
              ]),
              const Divider(),
              _buildDetailSection('执行信息', [
                _buildDetailRow('操作类型', _formatOperationType(record.operationType)),
                _buildDetailRow('执行状态', _formatExecutionStatus(record.status)),
                _buildDetailRow('执行时间', _formatTimestamp(record.executeTime)),
                _buildDetailRow('执行耗时', _formatDuration(record.duration)),
                if (record.retryCount != null && record.retryCount! > 0)
                  _buildDetailRow('重试次数', '${record.retryCount}'),
              ]),
              if (record.failReason != null && record.failReason!.isNotEmpty) ...[
                const Divider(),
                _buildDetailSection('错误信息', [
                  Text(
                    record.failReason!,
                    style: const TextStyle(color: Colors.red, fontSize: 12),
                  ),
                ]),
              ],
              if (record.processInfo != null && record.processInfo!.isNotEmpty) ...[
                const Divider(),
                _buildDetailSection('处理日志', [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade100,
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: record.processInfo!
                          .map((info) => Text(info, style: const TextStyle(fontSize: 12)))
                          .toList(),
                    ),
                  ),
                ]),
              ],
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('关闭'),
          ),
          if (record.status == 'FAILED')
            ElevatedButton(
              onPressed: () {
                Navigator.of(context).pop();
                _retryOperation(record);
              },
              child: const Text('重试'),
            ),
        ],
      ),
    );
  }

  /// 构建详情区块
  Widget _buildDetailSection(String title, List<Widget> children) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
        const SizedBox(height: 8),
        ...children,
        const SizedBox(height: 8),
      ],
    );
  }

  /// 构建详情行
  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text('$label:', style: const TextStyle(fontWeight: FontWeight.w500)),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }

  /// 格式化操作类型
  String _formatOperationType(String? type) {
    switch (type) {
      case 'RENAME':
        return '重命名';
      case 'MOVE':
        return '移动';
      case 'DELETE':
        return '删除';
      case 'COPY':
        return '复制';
      default:
        return type ?? '未知';
    }
  }

  /// 格式化执行状态
  String _formatExecutionStatus(String? status) {
    switch (status) {
      case 'SUCCESS':
        return '成功';
      case 'FAILED':
        return '失败';
      case 'SKIPPED':
        return '跳过';
      case 'PENDING':
        return '待执行';
      default:
        return status ?? '未知';
    }
  }

  /// 格式化时间戳
  String _formatTimestamp(int? timestamp) {
    if (timestamp == null) return '-';
    return DateTime.fromMillisecondsSinceEpoch(timestamp).toString().substring(0, 19);
  }

  /// 格式化执行耗时
  String _formatDuration(int? duration) {
    if (duration == null) return '-';
    if (duration < 1000) return '${duration}ms';
    return '${(duration / 1000).toStringAsFixed(2)}s';
  }

  /// 返回预览页面
  void _goBackToPreview() {
    Navigator.of(context).pop();
  }

  /// 导出执行结果
  void _exportExecutionResults() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('导出功能开发中...')),
    );
  }

  /// 重试操作
  void _retryOperation(TaskRecord record) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('重试操作: ${record.originalName}')),
    );
  }

  /// 完成任务
  void _completeTask() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('完成任务'),
        content: const Text('确定要完成任务吗？完成后将返回任务列表。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
              Navigator.of(context).pushNamedAndRemoveUntil(
                '/tasks',
                (route) => route.isFirst,
              );
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }
}
