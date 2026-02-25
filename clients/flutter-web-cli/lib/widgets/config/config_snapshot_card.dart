import 'package:flutter/material.dart';
import '../../models/task_status.dart' as task_models;

class ConfigSnapshotCard extends StatelessWidget {
  final task_models.TaskStatus selectedTask;

  const ConfigSnapshotCard({super.key, required this.selectedTask});

  @override
  Widget build(BuildContext context) {
    if (selectedTask == null || selectedTask.configSnapshot == null) {
      return Card(
        elevation: 2,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: const Text('无配置快照信息'),
        ),
      );
    }

    final configSnapshot = selectedTask.configSnapshot!;
    final globalSettings = configSnapshot.globalSettings ?? {};

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
            _buildSourceDirectoriesList(configSnapshot.sourceDirectories),
            const SizedBox(height: 12),
            _buildStrategyConfig(configSnapshot.strategyId, configSnapshot.strategyConfig),
            const SizedBox(height: 12),
            if (globalSettings.isNotEmpty)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    '全局设置',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  ...globalSettings.entries.map((entry) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 4),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          SizedBox(
                            width: 150,
                            child: Text(
                              '${entry.key}: ',
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                          Expanded(
                            child: Text(
                              entry.value.toString(),
                              style: const TextStyle(fontSize: 13),
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
      return const Text('无源目录配置');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '源目录',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        ...sourceDirs.map((dir) {
          return Padding(
            padding: const EdgeInsets.only(left: 16, bottom: 4),
            child: Text(
              dir.path ?? 'N/A',
              style: const TextStyle(fontSize: 13),
            ),
          );
        }).toList(),
      ],
    );
  }

  Widget _buildStrategyConfig(String? strategyId, Map<String, dynamic>? strategyConfig) {
    if (strategyId == null) {
      return const Text('无策略配置');
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '策略配置',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        Padding(
          padding: const EdgeInsets.only(left: 16),
          child: Text(
            '策略ID: $strategyId',
            style: const TextStyle(fontSize: 13),
          ),
        ),
        if (strategyConfig != null && strategyConfig.isNotEmpty)
          ...strategyConfig.entries.map((entry) {
            return Padding(
              padding: const EdgeInsets.only(left: 16, bottom: 4),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  SizedBox(
                    width: 150,
                    child: Text(
                      '${entry.key}: ',
                      style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                  Expanded(
                    child: Text(
                      entry.value.toString(),
                      style: const TextStyle(fontSize: 13),
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
