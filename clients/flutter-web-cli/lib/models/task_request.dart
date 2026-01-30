import 'package:filemanager_flutter/models/strategy_config.dart';

class TaskRequest {
  final String strategyId;
  final List<String> filePaths;
  final StrategyConfig strategyConfig;
  final String? taskName;
  final String? description;

  TaskRequest({
    required this.strategyId,
    required this.filePaths,
    required this.strategyConfig,
    this.taskName,
    this.description,
  });

  Map<String, dynamic> toJson() {
    return {
      'strategyId': strategyId,
      'filePaths': filePaths,
      'strategyConfig': strategyConfig.toJson(),
      'taskName': taskName,
      'description': description,
    };
  }
}
