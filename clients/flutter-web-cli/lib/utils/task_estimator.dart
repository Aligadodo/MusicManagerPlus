import 'dart:collection';
import 'dart:core';

/// 多线程任务执行进度预估工具类
class TaskEstimator {
  // 总任务数
  final int totalTasks;
  // 已完成任务数
  int completedTasks = 0;
  // 已开始任务数
  int startedTasks = 0;
  // 已失败任务数
  int failedTasks = 0;

  // 滑动窗口：用于存储最近完成任务的时间戳，计算近期吞吐量
  final int windowSize;
  final Queue<int> completionWindow;
  // 任务开始时间 (ms)
  int? startTime;
  // 任务结束时间 (ms)
  int? endTime;
  bool isStarted = false;
  bool isFinished = false;

  /// 构造函数
  /// [totalTasks] 总任务数
  /// [windowSize] 样本窗口大小（建议设为线程数的 5-10 倍）
  TaskEstimator(this.totalTasks, {this.windowSize = 50}) : 
    completionWindow = Queue<int>() {
  }

  /// 辅助格式化工具
  static String formatDuration(int milliseconds) {
    int hours = milliseconds ~/ (60 * 60 * 1000);
    int minutes = (milliseconds ~/ (60 * 1000)) % 60;
    int seconds = (milliseconds ~/ 1000) % 60;
    return '\$hours:\$minutes:\$seconds'.replaceAllMapped(
      RegExp(r'\b(\d)\b'), 
      (match) => '0${match.group(1)}'
    );
  }

  /// 标记任务正式开始
  void start() {
    if (!isStarted) {
      startTime = DateTime.now().millisecondsSinceEpoch;
      isStarted = true;
    }
  }

  /// 每当一个子任务开始时调用
  void taskStarted() {
    if (!isStarted || isFinished) return;
    startedTasks++;
  }

  /// 每当一个子任务完成时调用
  void taskCompleted() {
    if (!isStarted || isFinished) return;

    int now = DateTime.now().millisecondsSinceEpoch;
    completedTasks++;

    // 记录完成时间戳到滑动窗口
    completionWindow.addLast(now);
    if (completionWindow.length > windowSize) {
      completionWindow.removeFirst();
    }
  }

  /// 每当一个子任务失败时调用
  void taskFailed() {
    if (!isStarted || isFinished) return;
    failedTasks++;
  }

  /// 获取正在运行的任务数
  int get runningTaskCount => startedTasks - completedTasks;

  /// 标记全部任务结束
  void finish() {
    if (!isFinished) {
      isFinished = true;
      endTime = DateTime.now().millisecondsSinceEpoch;
    }
  }

  /// 获取已运行总时长（毫秒）
  int get elapsedMillis {
    if (!isStarted) return 0;
    int end = isFinished ? endTime! : DateTime.now().millisecondsSinceEpoch;
    return end - startTime!;
  }

  /// 获取预估剩余时长（毫秒）
  int get estimatedRemainingMillis {
    if (!isStarted || isFinished || completedTasks == 0) return -1;

    int now = DateTime.now().millisecondsSinceEpoch;
    int done = completedTasks;
    int remaining = totalTasks - done;

    if (remaining <= 0) return 0;

    // 算法核心：计算近期吞吐量 (Tasks per ms)
    double tasksPerMs;

    if (completionWindow.length < 2) {
      // 如果窗口样本不足，退化为全局平均速度
      tasksPerMs = done / (now - startTime!);
    } else {
      // 计算窗口内第一个和最后一个样本的时间差
      int firstInWindow = completionWindow.first;
      int lastInWindow = completionWindow.last;
      int duration = lastInWindow - firstInWindow;

      if (duration > 0) {
        // 近期速度 = 窗口内任务数 / 窗口时间跨度
        tasksPerMs = completionWindow.length / duration;
      } else {
        // 极短时间内大量完成，降级处理
        tasksPerMs = done / (now - startTime!);
      }
    }

    return tasksPerMs > 0 ? (remaining / tasksPerMs).round() : -1;
  }

  /// 获取格式化的预估剩余时间 (HH:mm:ss)
  String get formattedRemainingTime {
    int ms = estimatedRemainingMillis;
    if (ms < 0) return '计算中...';
    if (ms == 0) return '00:00:00';
    return formatDuration(ms);
  }

  /// 获取格式化的已用时间 (HH:mm:ss)
  String get formattedElapsedTime {
    int ms = elapsedMillis;
    return formatDuration(ms);
  }

  /// 获取进度百分比
  String get progressPercentage {
    if (totalTasks == 0) return '0.00';
    return ((completedTasks / totalTasks) * 100).toStringAsFixed(2);
  }

  /// 获取进度值 (0.0 到 1.0)
  double get progress {
    if (totalTasks == 0) return 0.0;
    return (completedTasks / totalTasks).clamp(0.0, 1.0);
  }

  /// 获取显示信息
  String get displayInfo {
    return '总共：$totalTasks 已处理:$completedTasks 耗时:${formattedElapsedTime} 进度:${progressPercentage}% 预计剩余时间：$formattedRemainingTime';
  }
}
