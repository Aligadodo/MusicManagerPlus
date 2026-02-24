import 'package:flutter/material.dart';
import '../models/change_record.dart';

class ChangeRecordTable extends StatelessWidget {
  final List<ChangeRecord> records;
  final bool isLoading;
  final String errorMessage;

  const ChangeRecordTable({
    super.key,
    required this.records,
    required this.isLoading,
    required this.errorMessage,
  });

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (errorMessage.isNotEmpty) {
      return Center(
        child: Text(
          errorMessage,
          style: const TextStyle(color: Colors.red),
        ),
      );
    }

    if (records.isEmpty) {
      return const Center(
        child: Text('暂无变更记录'),
      );
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
}
