import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/task_status.dart' as task_models;
import 'task_detail_header.dart';
import '../config/config_snapshot_card.dart';
import '../common/stage_result_cards.dart';

class TaskDetailWidget extends ConsumerStatefulWidget {
  final task_models.TaskStatus? selectedTask;
  final Function() onBack;

  const TaskDetailWidget({
    super.key,
    required this.selectedTask,
    required this.onBack,
  });

  @override
  ConsumerState<TaskDetailWidget> createState() => _TaskDetailWidgetState();
}

class _TaskDetailWidgetState extends ConsumerState<TaskDetailWidget> with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 5, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.selectedTask == null) {
      return const Center(child: Text('请选择一个任务'));
    }

    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          TaskDetailHeader(onBack: widget.onBack),
          const SizedBox(height: 12),
          
          // 添加TabBar导航条
          Container(
            decoration: BoxDecoration(
              border: Border(bottom: BorderSide(color: Colors.grey.shade200)),
            ),
            child: TabBar(
              controller: _tabController,
              isScrollable: true,
              tabs: const [
                Tab(text: '任务信息'),
                Tab(text: '配置快照'),
                Tab(text: '扫描结果'),
                Tab(text: '预览结果'),
                Tab(text: '执行结果'),
              ],
              labelColor: Colors.blue,
              unselectedLabelColor: Colors.grey,
              indicatorColor: Colors.blue,
              indicatorWeight: 2,
            ),
          ),
          
          const SizedBox(height: 12),
          
          // 添加TabBarView滑动展示
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                // 任务信息
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: TaskInfoCard(selectedTask: widget.selectedTask!),
                ),
                
                // 配置快照
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: ConfigSnapshotCard(selectedTask: widget.selectedTask!),
                ),
                
                // 扫描结果
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: ScanResultCard(selectedTask: widget.selectedTask!),
                ),
                
                // 预览结果
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: PreviewResultCard(selectedTask: widget.selectedTask!),
                ),
                
                // 执行结果
                SingleChildScrollView(
                  padding: const EdgeInsets.only(bottom: 24),
                  child: ExecutionResultCard(selectedTask: widget.selectedTask!),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}