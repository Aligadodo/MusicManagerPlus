import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class ConfigSnapshotCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ConfigSnapshotCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '配置快照',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            _buildConfigField('源目录', selectedTask.configSnapshot?.sourceDirectories),
            _buildConfigField('插件流水线', selectedTask.configSnapshot?.pipeline),
            _buildConfigField('全局设置', selectedTask.configSnapshot?.globalSettings),
          ],
        ),
      ),
    );
  }

  Widget _buildConfigField(String label, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 4),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey[50],
              border: Border.all(color: Colors.grey[200]),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              _formatConfigValue(value),
              style: const TextStyle(
                fontSize: 13,
                fontFamily: 'Monospace',
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _formatConfigValue(dynamic value) {
    if (value == null) return 'N/A';
    
    if (value is List) {
      return value.map((item) => item.toString()).join(', ');
    } else if (value is Map) {
      return value.entries
          .map((entry) => '${entry.key}: ${entry.value}')
          .join('\n');
    }
    
    return value.toString();
  }
}

class ScanResultCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ScanResultCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '扫描结果',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 12),
            _buildResultField('扫描文件数', selectedTask.scanResult?.totalFiles),
            _buildResultField('成功扫描', selectedTask.scanResult?.scannedFiles),
            _buildResultField('扫描失败', selectedTask.scanResult?.failedFiles),
            _buildResultField('扫描耗时', '${selectedTask.scanResult?.costTime}秒'),
            if (selectedTask.scanResult?.errorFiles != null && selectedTask.scanResult!.errorFiles!.isNotEmpty) ...[
              const SizedBox(height: 8),
              const Text('错误文件:', style: TextStyle(fontWeight: FontWeight.w500)),
              const SizedBox(height: 4),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[50],
                  border: Border.all(color: Colors.grey[200]),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: Text(
                  selectedTask.scanResult!.errorFiles!.join('\n'),
                  style: const TextStyle(
                    fontSize: 13,
                    fontFamily: 'Monospace',
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildResultField(String label, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          Text(
            label, style: const TextStyle(fontWeight: FontWeight.w500),
          ),
          const SizedBox(width: 8),
          Text(value?.toString() ?? 'N/A'),
        ],
      ),
    );
  }
}
