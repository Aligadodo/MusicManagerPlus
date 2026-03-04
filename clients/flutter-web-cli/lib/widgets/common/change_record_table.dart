import 'package:flutter/material.dart';
import '../../models/change_record.dart';

class ChangeRecordTable extends StatelessWidget {
  final List<ChangeRecord> records;
  final bool isLoading;
  final String errorMessage;
  final String? taskStatus;

  const ChangeRecordTable({
    super.key,
    required this.records,
    required this.isLoading,
    required this.errorMessage,
    this.taskStatus,
  });

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return _buildLoadingState();
    }

    if (errorMessage.isNotEmpty) {
      return _buildErrorState(errorMessage);
    }

    if (records.isEmpty) {
      return _buildEmptyState();
    }

    return Expanded(
      child: SingleChildScrollView(
        child: DataTable(
          columns: const [
            DataColumn(label: Text('原文件名')),
            DataColumn(label: Text('新文件名')),
            DataColumn(label: Text('文件路径')),
            DataColumn(label: Text('操作类型')),
            DataColumn(label: Text('状态')),
          ],
          rows: records.map((record) {
            return DataRow(
              cells: [
                DataCell(Text(record.originalName ?? '')),
                DataCell(Text(record.newName ?? '')),
                DataCell(Text(record.filePath ?? '')),
                DataCell(Text(record.operationType ?? '')),
                DataCell(
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: record.changed == true ? Colors.green : Colors.grey,
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Text(
                      record.changed == true ? '已修改' : '未修改',
                      style: const TextStyle(color: Colors.white),
                    ),
                  ),
                ),
              ],
            );
          }).toList(),
        ),
      ),
    );
  }

  /// 加载中状态
  Widget _buildLoadingState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: 16),
          Text(
            '正在加载数据...',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey[600],
            ),
          ),
        ],
      ),
    );
  }

  /// 错误状态
  Widget _buildErrorState(String message) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, size: 48, color: Colors.red[300]),
          const SizedBox(height: 16),
          Text(
            '加载失败',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Colors.red[700],
            ),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 32),
            child: Text(
              message,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
              textAlign: TextAlign.center,
            ),
          ),
        ],
      ),
    );
  }

  /// 空状态
  Widget _buildEmptyState() {
    // 根据任务状态显示不同的提示
    String title;
    String message;
    IconData icon;
    Color color;

    switch (taskStatus?.toUpperCase()) {
      case 'SCANNING':
        title = '正在扫描';
        message = '正在扫描文件，请稍候...';
        icon = Icons.search;
        color = Colors.blue;
        break;
      case 'PREVIEWING':
        title = '正在预览';
        message = '正在分析文件变更，请稍候...';
        icon = Icons.preview;
        color = Colors.green;
        break;
      case 'EXECUTING':
        title = '正在执行';
        message = '正在执行文件操作，请稍候...';
        icon = Icons.play_arrow;
        color = Colors.purple;
        break;
      case 'SCANNED':
        title = '扫描完成';
        message = '扫描已完成，但未发现任何文件\n请检查源目录配置是否正确';
        icon = Icons.folder_open;
        color = Colors.orange;
        break;
      case 'PREVIEWED':
        title = '预览完成';
        message = '预览已完成，没有文件需要变更';
        icon = Icons.check_circle;
        color = Colors.green;
        break;
      case 'COMPLETED':
        title = '执行完成';
        message = '所有操作已成功执行';
        icon = Icons.done_all;
        color = Colors.green;
        break;
      case 'FAILED':
        title = '执行失败';
        message = '任务执行失败，请查看日志了解详情';
        icon = Icons.error;
        color = Colors.red;
        break;
      default:
        title = '暂无数据';
        message = '请先开始扫描以查看变更记录';
        icon = Icons.insert_drive_file;
        color = Colors.grey;
    }

    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, size: 64, color: color.withOpacity(0.5)),
          const SizedBox(height: 16),
          Text(
            title,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: color,
            ),
          ),
          const SizedBox(height: 8),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 32),
            child: Text(
              message,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
              textAlign: TextAlign.center,
            ),
          ),
        ],
      ),
    );
  }
}
