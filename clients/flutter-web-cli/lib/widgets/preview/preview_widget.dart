import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../models/change_record.dart';
import '../../models/local_task_state.dart';
import '../common/change_record_table.dart';

class PreviewWidget extends ConsumerWidget {
  final String searchFilter;
  final String statusFilter;
  final String operationTypeFilter;
  final bool hideUnchanged;
  final bool isLoading;
  final String errorMessage;
  final int currentPage;
  final int totalPages;
  final List<ChangeRecord> records;
  final LocalTaskState taskState;
  final String message;
  final bool isStatusBarExpanded;
  final String currentStep;
  final String remainingTime;
  final int changeCount;
  final int scannedFiles;
  final int totalFiles;
  final String logMessage;

  final Function(String) onSearchFilterChanged;
  final Function(String) onStatusFilterChanged;
  final Function(String) onOperationTypeFilterChanged;
  final Function(bool) onHideUnchangedChanged;
  final Function() onPreview;
  final Function() onExecute;
  final Function() onStop;
  final Function(int) onPageChanged;
  final Function() onToggleStatusBar;

  const PreviewWidget({
    super.key,
    required this.searchFilter,
    required this.statusFilter,
    required this.operationTypeFilter,
    required this.hideUnchanged,
    required this.isLoading,
    required this.errorMessage,
    required this.currentPage,
    required this.totalPages,
    required this.records,
    required this.taskState,
    required this.message,
    required this.isStatusBarExpanded,
    required this.currentStep,
    required this.remainingTime,
    required this.changeCount,
    required this.scannedFiles,
    required this.totalFiles,
    required this.logMessage,
    required this.onSearchFilterChanged,
    required this.onStatusFilterChanged,
    required this.onOperationTypeFilterChanged,
    required this.onHideUnchangedChanged,
    required this.onPreview,
    required this.onExecute,
    required this.onStop,
    required this.onPageChanged,
    required this.onToggleStatusBar,
  });

  List<ChangeRecord> _getCurrentPageRecords() {
    final filteredRecords = _getFilteredRecords();
    final startIndex = (currentPage - 1) * 20;
    final endIndex = (startIndex + 20).clamp(0, filteredRecords.length);
    return filteredRecords.sublist(startIndex, endIndex);
  }

  List<ChangeRecord> _getFilteredRecords() {
    var filtered = records;

    if (searchFilter.isNotEmpty) {
      filtered = filtered.where((record) {
        final searchText = searchFilter.toLowerCase();
        return (record.originalName?.toLowerCase().contains(searchText) ?? false) ||
            (record.newName?.toLowerCase().contains(searchText) ?? false) ||
            (record.filePath?.toLowerCase().contains(searchText) ?? false);
      }).toList();
    }

    if (statusFilter != '全部') {
      if (statusFilter == '已修改') {
        filtered = filtered.where((record) => record.changed == true).toList();
      } else if (statusFilter == '未修改') {
        filtered = filtered.where((record) => record.changed == false).toList();
      }
    }

    if (operationTypeFilter != '全部') {
      filtered = filtered.where((record) {
        if (operationTypeFilter == '其他') {
          return record.operationType != '重命名' &&
              record.operationType != '移动' &&
              record.operationType != '删除';
        }
        return record.operationType == operationTypeFilter;
      }).toList();
    }

    if (hideUnchanged) {
      filtered = filtered.where((record) => record.changed == true).toList();
    }

    return filtered;
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        children: [
          _buildFilterBar(),
          const SizedBox(height: 12),
          Expanded(
            child: Row(
              children: [
                Expanded(
                  child: _buildPreviewTable(),
                ),
              ],
            ),
          ),
          _buildStatusBar(),
        ],
      ),
    );
  }

  Widget _buildFilterBar() {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: TextField(
                decoration: const InputDecoration(
                  labelText: '搜索',
                  border: OutlineInputBorder(),
                  prefixIcon: Icon(Icons.search),
                ),
                onChanged: onSearchFilterChanged,
              ),
            ),
            const SizedBox(width: 12),
            ElevatedButton(
              onPressed: () {
                onStatusFilterChanged('全部');
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
              child: const Text('查看任务列表'),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: DropdownButtonFormField<String>(
                value: statusFilter,
                onChanged: (value) {
                  if (value != null) {
                    onStatusFilterChanged(value);
                  }
                },
                items: ['全部', '已修改', '未修改'].map((status) => DropdownMenuItem<String>(
                  value: status,
                  child: Text(status),
                )).toList(),
                decoration: const InputDecoration(
                  labelText: '状态筛选',
                  border: OutlineInputBorder(),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: DropdownButtonFormField<String>(
                value: operationTypeFilter,
                onChanged: (value) {
                  if (value != null) {
                    onOperationTypeFilterChanged(value);
                  }
                },
                items: ['全部', '重命名', '移动', '删除', '其他'].map((type) => DropdownMenuItem<String>(
                  value: type,
                  child: Text(type),
                )).toList(),
                decoration: const InputDecoration(
                  labelText: '操作类型',
                  border: OutlineInputBorder(),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Row(
                children: [
                  Checkbox(
                    value: hideUnchanged,
                    onChanged: (value) {
                      if (value != null) {
                        onHideUnchangedChanged(value);
                      }
                    },
                  ),
                  const Text('隐藏未修改项'),
                ],
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildPreviewTable() {
    final currentRecords = _getCurrentPageRecords();
    return Column(
      children: [
        Expanded(
          child: ChangeRecordTable(
            records: currentRecords,
            isLoading: isLoading,
            errorMessage: errorMessage,
            taskStatus: _getTaskStatusFromLocalState(taskState),
          ),
        ),
        _buildPagination(),
      ],
    );
  }

  String _getTaskStatusFromLocalState(LocalTaskState state) {
    switch (state) {
      case LocalTaskState.ready:
        return 'PENDING';
      case LocalTaskState.previewing:
        return 'PREVIEWING';
      case LocalTaskState.previewCompleted:
        return 'PREVIEWED';
      case LocalTaskState.previewFailed:
        return 'FAILED';
      case LocalTaskState.executing:
        return 'EXECUTING';
      case LocalTaskState.executionCompleted:
        return 'COMPLETED';
      case LocalTaskState.executionFailed:
        return 'FAILED';
      case LocalTaskState.cancelled:
        return 'CANCELLED';
    }
  }

  Widget _buildPagination() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        TextButton(
          onPressed: currentPage > 1
              ? () => onPageChanged(currentPage - 1)
              : null,
          child: const Text('上一页'),
        ),
        Text('第 $currentPage 页，共 $totalPages 页'),
        TextButton(
          onPressed: currentPage < totalPages
              ? () => onPageChanged(currentPage + 1)
              : null,
          child: const Text('下一页'),
        ),
      ],
    );
  }

  Widget _buildStatusBar() {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          const Text('状态: ', style: TextStyle(fontWeight: FontWeight.w500)),
                          Text(
                            _getTaskStateText(),
                            style: TextStyle(
                              color: _getTaskStateColor(),
                            ),
                          ),
                        ],
                      ),
                      if (message.isNotEmpty)
                        Row(
                          children: [
                            const Text('消息: ', style: TextStyle(fontWeight: FontWeight.w500)),
                            Expanded(
                              child: Text(
                                message,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                    ],
                  ),
                ),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    ElevatedButton(
                      onPressed: onPreview,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.blue,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                      ),
                      child: const Text('创建任务'),
                    ),
                  ],
                ),
              ],
            ),
            if (isStatusBarExpanded)
              const SizedBox(height: 12),
            if (isStatusBarExpanded)
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (currentStep.isNotEmpty)
                          Row(
                            children: [
                              const Text('当前步骤: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Expanded(
                                child: Text(
                                  currentStep,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        if (remainingTime.isNotEmpty)
                          Row(
                            children: [
                              const Text('剩余时间: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Text(remainingTime),
                            ],
                          ),
                        if (changeCount > 0)
                          Row(
                            children: [
                              const Text('变更数量: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Text('$changeCount'),
                            ],
                          ),
                        if (scannedFiles > 0)
                          Row(
                            children: [
                              const Text('扫描进度: ', style: TextStyle(fontWeight: FontWeight.w500)),
                              Text('$scannedFiles/$totalFiles'),
                            ],
                          ),
                      ],
                    ),
                  ),
                ],
              ),
            if (logMessage.isNotEmpty)
              const SizedBox(height: 12),
            if (logMessage.isNotEmpty)
              Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Text('日志: ', style: TextStyle(fontWeight: FontWeight.w500)),
                            Expanded(
                              child: Text(
                                logMessage,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 12),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            Row(
              children: [
                TextButton(
                  onPressed: onToggleStatusBar,
                  child: Text(
                    isStatusBarExpanded ? '收起详情' : '展开详情',
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _getTaskStateText() {
    switch (taskState) {
      case LocalTaskState.ready:
        return '就绪';
      case LocalTaskState.previewing:
        return '预览中';
      case LocalTaskState.previewCompleted:
        return '预览完成';
      case LocalTaskState.previewFailed:
        return '预览失败';
      case LocalTaskState.executing:
        return '执行中';
      case LocalTaskState.executionCompleted:
        return '执行完成';
      case LocalTaskState.executionFailed:
        return '执行失败';
      case LocalTaskState.cancelled:
        return '已取消';
    }
  }

  Color _getTaskStateColor() {
    switch (taskState) {
      case LocalTaskState.ready:
        return Colors.grey;
      case LocalTaskState.previewing:
      case LocalTaskState.executing:
        return Colors.blue;
      case LocalTaskState.previewCompleted:
      case LocalTaskState.executionCompleted:
        return Colors.green;
      case LocalTaskState.previewFailed:
      case LocalTaskState.executionFailed:
        return Colors.red;
      case LocalTaskState.cancelled:
        return Colors.orange;
    }
  }
}
