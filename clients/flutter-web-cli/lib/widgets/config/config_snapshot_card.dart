import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class ConfigSnapshotCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ConfigSnapshotCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    if (selectedTask == null || selectedTask.configSnapshot == null) {
      return Card(
        elevation: 4,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8.0),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无配置快照信息'),
        ),
      );
    }

    final configSnapshot = selectedTask.configSnapshot!;
    final globalSettings = configSnapshot.globalSettings ?? {};

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
                color: Colors.green.withOpacity(0.1),
                borderRadius: BorderRadius.circular(4.0),
              ),
              child: const Text(
                '配置快照',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.green,
                ),
              ),
            ),
            const SizedBox(height: 16),
            _buildSourceDirectoriesList(configSnapshot.sourceDirectories),
            const SizedBox(height: 16),
            _buildStrategyConfig(configSnapshot.strategyId, configSnapshot.strategyConfig),
            const SizedBox(height: 16),
            if (globalSettings.isNotEmpty)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(vertical: 2, horizontal: 6),
                    decoration: BoxDecoration(
                      color: Colors.grey.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(4.0),
                    ),
                    child: const Text(
                      '全局设置',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Colors.grey,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  ...globalSettings.entries.map((entry) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          SizedBox(
                            width: 150,
                            child: Text(
                              '${entry.key}: ',
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                          Expanded(
                            child: Text(
                              entry.value.toString(),
                              style: const TextStyle(fontSize: 14, color: Colors.black87),
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildSourceDirectoriesList(List<task_models.SourceDirectoryConfig>? sourceDirs) {
    if (sourceDirs == null || sourceDirs.isEmpty) {
      return const Text('无源目录配置', style: TextStyle(fontSize: 14, color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 2, horizontal: 6),
          decoration: BoxDecoration(
            color: Colors.orange.withOpacity(0.1),
            borderRadius: BorderRadius.circular(4.0),
          ),
          child: const Text(
            '源目录',
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
              color: Colors.orange,
            ),
          ),
        ),
        const SizedBox(height: 12),
        ...sourceDirs.map((dir) {
          return Padding(
            padding: const EdgeInsets.only(left: 16, bottom: 8),
            child: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.orange.withOpacity(0.05),
                borderRadius: BorderRadius.circular(4.0),
                border: Border.all(color: Colors.orange.withOpacity(0.2)),
              ),
              child: Text(
                dir.path ?? 'N/A',
                style: const TextStyle(fontSize: 14, color: Colors.black87),
              ),
            ),
          );
        }).toList(),
      ],
    );
  }

  Widget _buildStrategyConfig(String? strategyId, Map<String, dynamic>? strategyConfig) {
    if (strategyId == null) {
      return const Text('无策略配置', style: TextStyle(fontSize: 14, color: Colors.grey));
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(vertical: 2, horizontal: 6),
          decoration: BoxDecoration(
            color: Colors.purple.withOpacity(0.1),
            borderRadius: BorderRadius.circular(4.0),
          ),
          child: const Text(
            '策略配置',
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
              color: Colors.purple,
            ),
          ),
        ),
        const SizedBox(height: 12),
        Padding(
          padding: const EdgeInsets.only(left: 16, bottom: 8),
          child: Text(
            '策略ID: $strategyId',
            style: const TextStyle(fontSize: 14, color: Colors.black87),
          ),
        ),
        if (strategyConfig != null && strategyConfig.isNotEmpty)
          ...strategyConfig.entries.map((entry) {
            return Padding(
              padding: const EdgeInsets.only(left: 16, bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 150,
                    child: Text(
                      '${entry.key}: ',
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  Expanded(
                    child: Text(
                      entry.value.toString(),
                      style: const TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ],
              ),
            );
          }).toList(),
      ],
    );
  }
}
