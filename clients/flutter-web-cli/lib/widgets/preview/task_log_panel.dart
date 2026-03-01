import 'package:flutter/material.dart';
import 'dart:async';
import 'dart:convert';
import '../../api/api_client.dart';

class TaskLogPanel extends StatefulWidget {
  final String taskId;

  const TaskLogPanel({
    super.key,
    required this.taskId,
  });

  @override
  State<TaskLogPanel> createState() => _TaskLogPanelState();
}

class _TaskLogPanelState extends State<TaskLogPanel> {
  List<Map<String, dynamic>> _logs = [];
  bool _isLoading = true;
  bool _autoScroll = true;
  String? _error;
  Timer? _refreshTimer;
  int _lastTimestamp = 0;
  final ScrollController _scrollController = ScrollController();
  String _selectedLogLevel = 'ALL';

  @override
  void initState() {
    super.initState();
    _loadLogs();
    _startAutoRefresh();
  }

  @override
  void dispose() {
    _refreshTimer?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  void _startAutoRefresh() {
    _refreshTimer = Timer.periodic(const Duration(seconds: 2), (timer) {
      _loadNewLogs();
    });
  }

  Future<void> _loadLogs() async {
    try {
      setState(() {
        _isLoading = true;
        _error = null;
      });

      final apiClient = ApiClient();
      final response = await apiClient.get('/api/tasks/${widget.taskId}/execution-logs?page=1&pageSize=100');
      final responseData = jsonDecode(response.body) as Map<String, dynamic>;

      if (responseData['success'] == true) {
        final data = responseData['data'] as Map<String, dynamic>;
        final logList = data['list'] as List;

        setState(() {
          _logs = logList.cast<Map<String, dynamic>>();
          _isLoading = false;
          if (_logs.isNotEmpty) {
            _lastTimestamp = _logs.first['timestamp'] as int;
          }
        });

        if (_autoScroll) {
          _scrollToBottom();
        }
      } else {
        setState(() {
          _error = responseData['error']?['message'] ?? '加载日志失败';
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _error = '加载日志失败: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _loadNewLogs() async {
    if (_lastTimestamp == 0) {
      await _loadLogs();
      return;
    }

    try {
      final apiClient = ApiClient();
      final response = await apiClient.get('/api/tasks/${widget.taskId}/execution-logs/new?since=$_lastTimestamp');
      final responseData = jsonDecode(response.body) as Map<String, dynamic>;

      if (responseData['success'] == true) {
        final data = responseData['data'] as Map<String, dynamic>;
        final logList = data['list'] as List;

        if (logList.isNotEmpty) {
          setState(() {
            _logs.insertAll(0, logList.cast<Map<String, dynamic>>());
            _lastTimestamp = _logs.first['timestamp'] as int;
          });

          if (_autoScroll) {
            _scrollToBottom();
          }
        }
      }
    } catch (e) {
      debugPrint('加载新日志失败: $e');
    }
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  Color _getLogColor(String level) {
    switch (level.toUpperCase()) {
      case 'ERROR':
        return Colors.red;
      case 'WARN':
        return Colors.orange;
      case 'DEBUG':
        return Colors.grey;
      case 'INFO':
      default:
        return Colors.blue;
    }
  }

  IconData _getLogIcon(String level) {
    switch (level.toUpperCase()) {
      case 'ERROR':
        return Icons.error;
      case 'WARN':
        return Icons.warning;
      case 'DEBUG':
        return Icons.bug_report;
      case 'INFO':
      default:
        return Icons.info;
    }
  }

  String _formatTimestamp(int timestamp) {
    final date = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return '${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}:${date.second.toString().padLeft(2, '0')}';
  }

  List<Map<String, dynamic>> _getFilteredLogs() {
    if (_selectedLogLevel == 'ALL') {
      return _logs;
    }
    return _logs.where((log) => log['logLevel'] == _selectedLogLevel).toList();
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('错误: $_error', style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadLogs,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    final filteredLogs = _getFilteredLogs();

    return Column(
      children: [
        _buildToolbar(),
        const Divider(height: 1),
        Expanded(
          child: filteredLogs.isEmpty
              ? const Center(child: Text('暂无日志'))
              : ListView.builder(
                  controller: _scrollController,
                  padding: const EdgeInsets.all(8),
                  itemCount: filteredLogs.length,
                  itemBuilder: (context, index) {
                    final log = filteredLogs[index];
                    return _buildLogItem(log);
                  },
                ),
        ),
      ],
    );
  }

  Widget _buildToolbar() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: Row(
        children: [
          const Text('日志级别: '),
          DropdownButton<String>(
            value: _selectedLogLevel,
            items: const [
              DropdownMenuItem(value: 'ALL', child: Text('全部')),
              DropdownMenuItem(value: 'INFO', child: Text('INFO')),
              DropdownMenuItem(value: 'WARN', child: Text('WARN')),
              DropdownMenuItem(value: 'ERROR', child: Text('ERROR')),
              DropdownMenuItem(value: 'DEBUG', child: Text('DEBUG')),
            ],
            onChanged: (value) {
              setState(() {
                _selectedLogLevel = value ?? 'ALL';
              });
            },
          ),
          const Spacer(),
          Row(
            children: [
              const Text('自动滚动'),
              Switch(
                value: _autoScroll,
                onChanged: (value) {
                  setState(() {
                    _autoScroll = value;
                  });
                },
              ),
            ],
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadLogs,
            tooltip: '刷新',
          ),
        ],
      ),
    );
  }

  Widget _buildLogItem(Map<String, dynamic> log) {
    final level = log['logLevel'] as String? ?? 'INFO';
    final message = log['message'] as String? ?? '';
    final timestamp = log['timestamp'] as int? ?? 0;
    final logType = log['logType'] as String? ?? '';
    final details = log['details'] as String?;

    return Container(
      margin: const EdgeInsets.only(bottom: 4),
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: Colors.grey.shade200),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            _getLogIcon(level),
            size: 16,
            color: _getLogColor(level),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      _formatTimestamp(timestamp),
                      style: TextStyle(
                        fontSize: 11,
                        color: Colors.grey.shade600,
                        fontFamily: 'monospace',
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                      decoration: BoxDecoration(
                        color: _getLogColor(level).withOpacity(0.1),
                        borderRadius: BorderRadius.circular(2),
                      ),
                      child: Text(
                        level,
                        style: TextStyle(
                          fontSize: 10,
                          color: _getLogColor(level),
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    if (logType.isNotEmpty) ...[
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                        decoration: BoxDecoration(
                          color: Colors.blue.withOpacity(0.1),
                          borderRadius: BorderRadius.circular(2),
                        ),
                        child: Text(
                          logType,
                          style: const TextStyle(
                            fontSize: 10,
                            color: Colors.blue,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 4),
                SelectableText(
                  message,
                  style: const TextStyle(fontSize: 12, fontFamily: 'monospace'),
                ),
                if (details != null && details.isNotEmpty) ...[
                  const SizedBox(height: 4),
                  Container(
                    padding: const EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade100,
                      borderRadius: BorderRadius.circular(2),
                    ),
                    child: SelectableText(
                      details,
                      style: TextStyle(
                        fontSize: 11,
                        color: Colors.grey.shade700,
                        fontFamily: 'monospace',
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
