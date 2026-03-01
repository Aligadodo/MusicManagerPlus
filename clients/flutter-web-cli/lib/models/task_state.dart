import 'package:flutter_riverpod/flutter_riverpod.dart';

enum TaskStatus {
  idle,
  analyzing,
  running,
  stopping,
  completed,
  error,
}

class TaskState {
  final TaskStatus status;
  final String? taskId;
  final String? message;
  final int progress;
  final String? errorMessage;
  final bool? autoExecute;

  TaskState({
    required this.status,
    this.taskId,
    this.message,
    this.progress = 0,
    this.errorMessage,
    this.autoExecute,
  });

  TaskState copyWith({
    TaskStatus? status,
    String? taskId,
    String? message,
    int? progress,
    String? errorMessage,
    bool? autoExecute,
  }) {
    return TaskState(
      status: status ?? this.status,
      taskId: taskId ?? this.taskId,
      message: message ?? this.message,
      progress: progress ?? this.progress,
      errorMessage: errorMessage ?? this.errorMessage,
      autoExecute: autoExecute ?? this.autoExecute,
    );
  }
}

class TaskNotifier extends StateNotifier<TaskState> {
  TaskNotifier() : super(TaskState(status: TaskStatus.idle, message: '就绪'));

  void startAnalyzing() {
    state = state.copyWith(
      status: TaskStatus.analyzing,
      message: '正在分析...',
      progress: 0,
    );
  }

  void startRunning(String taskId) {
    state = state.copyWith(
      status: TaskStatus.running,
      taskId: taskId,
      message: '正在执行...',
      progress: 0,
    );
  }

  void updateProgress(int progress, String message) {
    state = state.copyWith(
      progress: progress,
      message: message,
    );
  }

  void complete() {
    state = state.copyWith(
      status: TaskStatus.completed,
      message: '执行完成',
      progress: 100,
    );
  }

  void stop() {
    state = state.copyWith(
      status: TaskStatus.stopping,
      message: '正在停止...',
    );
  }

  void error(String errorMessage) {
    state = state.copyWith(
      status: TaskStatus.error,
      message: '执行失败',
      errorMessage: errorMessage,
    );
  }

  void reset() {
    state = TaskState(status: TaskStatus.idle, message: '就绪');
  }

  void updateAutoExecute(bool autoExecute) {
    state = state.copyWith(autoExecute: autoExecute);
  }
}

final taskStateProvider = StateNotifierProvider<TaskNotifier, TaskState>((ref) {
  return TaskNotifier();
});
