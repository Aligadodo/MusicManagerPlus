import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/task_status.dart' as task_models;
import 'task_detail_header.dart';
import '../config/config_snapshot_card.dart';
import '../common/stage_result_cards.dart';

class TaskDetailWidget extends ConsumerWidget {
  final task_models.TaskStatus? selectedTask;
  final Function() onBack;

  const TaskDetailWidget({
    super.key,
    required this.selectedTask,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (selectedTask == null) {
      return const Center(child: Text('请选择一个任务'));
    }

    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          TaskDetailHeader(onBack: onBack),
          const SizedBox(height: 12),
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.only(bottom: 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  TaskInfoCard(selectedTask: selectedTask!),
                  const SizedBox(height: 16),
                  ConfigSnapshotCard(selectedTask: selectedTask!),
                  const SizedBox(height: 16),
                  ScanResultCard(selectedTask: selectedTask!),
                  const SizedBox(height: 16),
                  PreviewResultCard(selectedTask: selectedTask!),
                  const SizedBox(height: 16),
                  ExecutionResultCard(selectedTask: selectedTask!),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}