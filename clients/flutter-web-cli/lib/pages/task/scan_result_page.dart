import 'package:flutter/material.dart';
import '../../api/task_data_service.dart';
import '../../models/task_record.dart';
import '../../widgets/common/column_config.dart';
import '../../widgets/common/generic_data_list.dart';

/// 扫描结果页面
/// 使用通用数据列表组件展示扫描阶段的文件列表
class ScanResultPage extends StatefulWidget {
  final String taskId;
  final String? taskName;

  const ScanResultPage({
    Key? key,
    required this.taskId,
    this.taskName,
  }) : super(key: key);

  @override
  State<ScanResultPage> createState() => _ScanResultPageState();
}

class _ScanResultPageState extends State<ScanResultPage> {
  final TaskDataService _dataService = TaskDataService();
  final GlobalKey<GenericDataListState> _listKey = GlobalKey<GenericDataListState>();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.taskName ?? '任务'} - 扫描结果'),
        actions: [
          // 导出按钮
          IconButton(
            icon: const Icon(Icons.download),
            tooltip: '导出扫描结果',
            onPressed: _exportScanResults,
          ),
          // 继续到预览按钮
          ElevatedButton.icon(
            onPressed: _goToPreview,
            icon: const Icon(Icons.arrow_forward),
            label: const Text('继续到预览'),
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
          key: _listKey,
          title: '扫描文件列表',
          columns: ScanColumnConfigs.defaultColumns,
          onLoadData: (params) => _dataService.queryScanRecords(widget.taskId, params),
          showSearch: true,
          showPagination: true,
          showColumnSettings: true,
          showRefresh: true,
          enableRowSelection: true,
          multiSelect: true,
          toolbarActions: [
            // 文件类型筛选
            _buildFileTypeFilter(),
            const SizedBox(width: 8),
            // 大小筛选
            _buildSizeFilter(),
          ],
          onRowSelect: (record, selected, isShiftClick) {
            print('选中文件: ${record.originalName}, 选中状态: $selected');
          },
          onRowDoubleTap: (record) {
            _showFileDetail(record);
          },
        ),
      ),
    );
  }

  /// 构建文件类型筛选器
  Widget _buildFileTypeFilter() {
    return DropdownButton<String>(
      hint: const Text('文件类型'),
      items: const [
        DropdownMenuItem(value: null, child: Text('全部类型')),
        DropdownMenuItem(value: 'mp3', child: Text('MP3')),
        DropdownMenuItem(value: 'flac', child: Text('FLAC')),
        DropdownMenuItem(value: 'wav', child: Text('WAV')),
        DropdownMenuItem(value: 'm4a', child: Text('M4A')),
        DropdownMenuItem(value: 'ogg', child: Text('OGG')),
      ],
      onChanged: (value) {
        // 应用筛选
        _listKey.currentState?.refresh();
      },
    );
  }

  /// 构建大小筛选器
  Widget _buildSizeFilter() {
    return DropdownButton<String>(
      hint: const Text('文件大小'),
      items: const [
        DropdownMenuItem(value: null, child: Text('全部大小')),
        DropdownMenuItem(value: '0-1MB', child: Text('小于 1MB')),
        DropdownMenuItem(value: '1-10MB', child: Text('1MB - 10MB')),
        DropdownMenuItem(value: '10-50MB', child: Text('10MB - 50MB')),
        DropdownMenuItem(value: '50MB+', child: Text('大于 50MB')),
      ],
      onChanged: (value) {
        // 应用筛选
        _listKey.currentState?.refresh();
      },
    );
  }

  /// 显示文件详情
  void _showFileDetail(TaskRecord record) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(record.originalName),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildDetailRow('文件路径', record.originalPath),
              _buildDetailRow('文件大小', _formatFileSize(record.fileSize)),
              _buildDetailRow('文件类型', record.fileType ?? '-'),
              _buildDetailRow('修改时间', _formatTimestamp(record.lastModified)),
              if (record.metadata != null) ...[
                const Divider(),
                const Text('元数据:', style: TextStyle(fontWeight: FontWeight.bold)),
                ...record.metadata!.entries.map((e) => _buildDetailRow(e.key, e.value.toString())),
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

  /// 构建详情行
  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text('$label:', style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }

  /// 格式化文件大小
  String _formatFileSize(int? bytes) {
    if (bytes == null) return '-';
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(2)} KB';
    if (bytes < 1024 * 1024 * 1024) return '${(bytes / (1024 * 1024)).toStringAsFixed(2)} MB';
    return '${(bytes / (1024 * 1024 * 1024)).toStringAsFixed(2)} GB';
  }

  /// 格式化时间戳
  String _formatTimestamp(int? timestamp) {
    if (timestamp == null) return '-';
    return DateTime.fromMillisecondsSinceEpoch(timestamp).toString().substring(0, 19);
  }

  /// 导出扫描结果
  void _exportScanResults() {
    // TODO: 实现导出功能
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('导出功能开发中...')),
    );
  }

  /// 跳转到预览页面
  void _goToPreview() {
    Navigator.of(context).pushNamed(
      '/task/preview',
      arguments: {
        'taskId': widget.taskId,
        'taskName': widget.taskName,
      },
    );
  }
}

/// GenericDataList 的状态访问扩展
class GenericDataListState extends State<GenericDataList> {
  void refresh() {
    // 通过 context 找到 GenericDataList 的状态并调用刷新
    // 实际实现中需要通过 GlobalKey 或其他方式访问
  }
  
  @override
  Widget build(BuildContext context) {
    throw UnimplementedError();
  }
}
