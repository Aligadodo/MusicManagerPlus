import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';

class LogPage extends ConsumerStatefulWidget {
  const LogPage({super.key});

  @override
  ConsumerState<LogPage> createState() => _LogPageState();
}

class _LogPageState extends ConsumerState<LogPage> {
  List<Map<String, dynamic>> _logs = [];
  bool _isLoading = false;
  String _errorMessage = '';
  String _selectedLevel = 'ALL';
  String _selectedSource = 'ALL';
  int _currentPage = 1;
  final int _pageSize = 50;

  @override
  void initState() {
    super.initState();
    _loadLogs();
  }

  Future<void> _loadLogs() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final apiClient = ref.read(apiClientProvider);
      final response = await apiClient.get('/api/logs', {
        'level': _selectedLevel == 'ALL' ? null : _selectedLevel,
        'source': _selectedSource == 'ALL' ? null : _selectedSource,
        'page': _currentPage.toString(),
        'size': _pageSize.toString(),
      });

      setState(() {
        _logs = List<Map<String, dynamic>>.from(response);
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载日志失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _clearLogs() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final apiClient = ref.read(apiClientProvider);
      await apiClient.delete('/api/logs');
      await _loadLogs();
    } catch (e) {
      setState(() {
        _errorMessage = '清空日志失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  String _formatTimestamp(int timestamp) {
    final date = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')} ${date.hour.toString().padLeft(2, '0')}:${date.minute.toString().padLeft(2, '0')}:${date.second.toString().padLeft(2, '0')}';
  }

  Color _getLevelColor(String level) {
    switch (level.toUpperCase()) {
      case 'ERROR':
        return Colors.red;
      case 'WARN':
        return Colors.orange;
      case 'INFO':
        return Colors.blue;
      case 'DEBUG':
        return Colors.green;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('日志管理'),
      ),
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            // 筛选和操作栏
            Card(
              elevation: 4,
              margin: const EdgeInsets.only(bottom: 20),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    const Text(
                      '日志筛选',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: DropdownButtonFormField<String>(
                            value: _selectedLevel,
                            onChanged: (value) {
                              setState(() {
                                _selectedLevel = value!;
                                _currentPage = 1;
                              });
                              _loadLogs();
                            },
                            items: const [
                              DropdownMenuItem(value: 'ALL', child: Text('所有级别')),
                              DropdownMenuItem(value: 'ERROR', child: Text('错误')),
                              DropdownMenuItem(value: 'WARN', child: Text('警告')),
                              DropdownMenuItem(value: 'INFO', child: Text('信息')),
                              DropdownMenuItem(value: 'DEBUG', child: Text('调试')),
                            ],
                            decoration: const InputDecoration(
                              labelText: '日志级别',
                              border: OutlineInputBorder(),
                            ),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: DropdownButtonFormField<String>(
                            value: _selectedSource,
                            onChanged: (value) {
                              setState(() {
                                _selectedSource = value!;
                                _currentPage = 1;
                              });
                              _loadLogs();
                            },
                            items: const [
                              DropdownMenuItem(value: 'ALL', child: Text('所有来源')),
                              DropdownMenuItem(value: 'api', child: Text('API')),
                              DropdownMenuItem(value: 'service', child: Text('服务')),
                              DropdownMenuItem(value: 'plugin', child: Text('插件')),
                              DropdownMenuItem(value: 'system', child: Text('系统')),
                            ],
                            decoration: const InputDecoration(
                              labelText: '日志来源',
                              border: OutlineInputBorder(),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: _clearLogs,
                      child: const Text('清空日志'),
                    ),
                  ],
                ),
              ),
            ),

            // 错误信息
            if (_errorMessage.isNotEmpty)
              Container(
                padding: const EdgeInsets.all(10),
                color: Colors.red[100],
                child: Text(
                  _errorMessage,
                  style: const TextStyle(color: Colors.red),
                ),
              ),

            const SizedBox(height: 20),

            // 日志列表
            const Text(
              '日志列表',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),

            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              )
            else if (_logs.isEmpty)
              const Center(
                child: Text('暂无日志'),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: _logs.length,
                  itemBuilder: (context, index) {
                    final log = _logs[index];
                    return Card(
                      elevation: 2,
                      margin: const EdgeInsets.only(bottom: 10),
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  _formatTimestamp(log['timestamp']),
                                  style: const TextStyle(
                                    fontSize: 12,
                                    color: Colors.grey,
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 2,
                                  ),
                                  decoration: BoxDecoration(
                                    color: _getLevelColor(log['level']).withOpacity(0.2),
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Text(
                                    log['level'],
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: _getLevelColor(log['level']),
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text(
                              log['message'],
                              style: const TextStyle(
                                fontSize: 14,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '来源: ${log['source']}',
                              style: const TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
              ),

            // 分页控制
            if (!_isLoading && _logs.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 20),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    ElevatedButton(
                      onPressed: _currentPage > 1
                          ? () {
                              setState(() {
                                _currentPage--;
                              });
                              _loadLogs();
                            }
                          : null,
                      child: const Text('上一页'),
                    ),
                    const SizedBox(width: 20),
                    Text('第 $_currentPage 页'),
                    const SizedBox(width: 20),
                    ElevatedButton(
                      onPressed: _logs.length == _pageSize
                          ? () {
                              setState(() {
                                _currentPage++;
                              });
                              _loadLogs();
                            }
                          : null,
                      child: const Text('下一页'),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
