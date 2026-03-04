import 'package:flutter/material.dart';
import '../../api/task_data_service.dart';
import '../../models/task_record.dart';
import '../../widgets/common/column_config.dart';
import '../../widgets/common/generic_data_list.dart';

/// 预览结果页面
/// 使用通用数据列表组件展示预览阶段的操作列表
class PreviewResultPage extends StatefulWidget {
  final String taskId;
  final String? taskName;

  const PreviewResultPage({
    Key? key,
    required this.taskId,
    this.taskName,
  }) : super(key: key);

  @override
  State<PreviewResultPage> createState() => _PreviewResultPageState();
}

class _PreviewResultPageState extends State<PreviewResultPage> {
  final TaskDataService _dataService = TaskDataService();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.taskName ?? '任务'} - 预览结果'),
        actions: [
          // 返回扫描按钮
          TextButton.icon(
            onPressed: _goBackToScan,
            icon: const Icon(Icons.arrow_back, color: Colors.white),
            label: const Text('返回扫描', style: TextStyle(color: Colors.white)),
          ),
          const SizedBox(width: 8),
          // 导出按钮
          IconButton(
            icon: const Icon(Icons.download),
            tooltip: '导出预览结果',
            onPressed: _exportPreviewResults,
          ),
          // 开始执行按钮
          ElevatedButton.icon(
            onPressed: _startExecution,
            icon: const Icon(Icons.play_arrow),
            label: const Text('开始执行'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
            ),
          ),
          const SizedBox(width: 16),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: GenericDataList(
          title: '预览操作列表',
          columns: PreviewColumnConfigs.defaultColumns,
          onLoadData: (params) => _dataService.queryPreviewRecords(widget.taskId, params),
          showSearch: true,
          showPagination: true,
          showColumnSettings: true,
          showRefresh: true,
          enableRowSelection: true,
          multiSelect: true,
          toolbarActions: [
            // 操作类型筛选
            _buildOperationTypeFilter(),
            const SizedBox(width: 8),
            // 状态筛选
            _buildStatusFilter(),
            const SizedBox(width: 8),
            // 是否变更筛选
            _buildChangedFilter(),
          ],
          onRowSelect: (record, selected, isShiftClick) {
            print('选中操作: ${record.originalName} -> ${record.newName}, 选中状态: $selected');
          },
          onRowDoubleTap: (record) {
            _showOperationDetail(record);
          },
        ),
      ),
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

  /// 构建状态筛选器
  Widget _buildStatusFilter() {
    return DropdownButton<String>(
      hint: const Text('状态'),
      items: const [
        DropdownMenuItem(value: null, child: Text('全部状态')),
        DropdownMenuItem(value: 'CHANGED', child: Text('已变更')),
        DropdownMenuItem(value: 'UNCHANGED', child: Text('未变更')),
        DropdownMenuItem(value: 'PENDING', child: Text('待处理')),
        DropdownMenuItem(value: 'ERROR', child: Text('错误')),
      ],
      onChanged: (value) {
        // 应用筛选
      },
    );
  }

  /// 构建是否变更筛选器
  Widget _buildChangedFilter() {
    return DropdownButton<bool>(
      hint: const Text('是否变更'),
      items: const [
        DropdownMenuItem(value: null, child: Text('全部')),
        DropdownMenuItem(value: true, child: Text('已变更')),
        DropdownMenuItem(value: false, child: Text('未变更')),
      ],
      onChanged: (value) {
        // 应用筛选
      },
    );
  }

  /// 显示操作详情
  void _showOperationDetail(TaskRecord record) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('操作详情'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildDetailSection('原文件信息', [
                _buildDetailRow('原文件名', record.originalName),
                _buildDetailRow('原路径', record.originalPath),
              ]),
              const Divider(),
              _buildDetailSection('新文件信息', [
                _buildDetailRow('新文件名', record.newName.isNotEmpty ? record.newName : '-'),
                _buildDetailRow('新路径', record.newPath.isNotEmpty ? record.newPath : '-'),
              ]),
              const Divider(),
              _buildDetailSection('操作信息', [
                _buildDetailRow('操作类型', _formatOperationType(record.operationType)),
                _buildDetailRow('状态', _formatStatus(record.status)),
                _buildDetailRow('是否变更', record.changed == true ? '是' : '否'),
                if (record.reason != null && record.reason!.isNotEmpty)
                  _buildDetailRow('变更原因', record.reason!),
              ]),
              if (record.extraParams != null && record.extraParams!.isNotEmpty) ...[
                const Divider(),
                _buildDetailSection('额外参数', [
                  ...record.extraParams!.entries.map((e) => _buildDetailRow(e.key, e.value)),
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

  /// 格式化状态
  String _formatStatus(String? status) {
    switch (status) {
      case 'CHANGED':
        return '已变更';
      case 'UNCHANGED':
        return '未变更';
      case 'PENDING':
        return '待处理';
      case 'ERROR':
        return '错误';
      default:
        return status ?? '未知';
    }
  }

  /// 返回扫描页面
  void _goBackToScan() {
    Navigator.of(context).pop();
  }

  /// 导出预览结果
  void _exportPreviewResults() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('导出功能开发中...')),
    );
  }

  /// 开始执行
  void _startExecution() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认开始执行'),
        content: const Text('确定要开始执行所有预览的操作吗？此操作将实际修改文件。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
              Navigator.of(context).pushNamed(
                '/task/execution',
                arguments: {
                  'taskId': widget.taskId,
                  'taskName': widget.taskName,
                },
              );
            },
            style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
            child: const Text('确认执行'),
          ),
        ],
      ),
    );
  }
}
