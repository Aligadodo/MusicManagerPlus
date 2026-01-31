import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/log_service.dart';

class LogPage extends ConsumerStatefulWidget {
  const LogPage({super.key});

  @override
  ConsumerState<LogPage> createState() => _LogPageState();
}

class _LogPageState extends ConsumerState<LogPage> {
  final LogService _logService = LogService(ApiClient());
  List<LogFileInfo> _logFiles = [];
  List<LogEntry> _logEntries = [];
  bool _isLoading = false;
  String _errorMessage = '';

  String _selectedLogFile = '';
  String _keywordFilter = '';
  int _currentPage = 1;
  int _pageSize = 100;
  int _totalRecords = 0;
  int _totalPages = 0;

  @override
  void initState() {
    super.initState();
    _loadLogFiles();
  }

  Future<void> _loadLogFiles() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final result = await _logService.getLogFiles();

      setState(() {
        _logFiles = (result['files'] as List<dynamic>)
            .map((json) => LogFileInfo.fromJson(json as Map<String, dynamic>))
            .toList();
        _isLoading = false;

        // 默认选择最新的日志文件
        if (_logFiles.isNotEmpty) {
          _selectedLogFile = _logFiles.first.fileName;
          _loadLogEntries();
        }
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载日志文件失败: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _loadLogEntries() async {
    if (_selectedLogFile.isEmpty) {
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final result = await _logService.getLogEntries(
        fileName: _selectedLogFile,
        keyword: _keywordFilter.isNotEmpty ? _keywordFilter : null,
        page: _currentPage,
        size: _pageSize,
      );

      setState(() {
        _logEntries = (result['entries'] as List<dynamic>)
            .map((json) => LogEntry.fromJson(json as Map<String, dynamic>))
            .toList();
        _totalRecords = result['total'] ?? 0;
        _totalPages = result['pages'] ?? 0;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载日志内容失败: $e';
        _isLoading = false;
      });
    }
  }

  void _selectLogFile(String fileName) {
    setState(() {
      _selectedLogFile = fileName;
      _currentPage = 1;
      _keywordFilter = '';
    });
    _loadLogEntries();
  }

  void _refreshLogs() {
    _loadLogFiles();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            _buildHeader(),
            const SizedBox(height: 20),
            Expanded(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    flex: 1,
                    child: _buildLogFileList(),
                  ),
                  const SizedBox(width: 20),
                  Expanded(
                    flex: 3,
                    child: _buildLogContent(),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            const Text(
              '运行日志',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const Spacer(),
            IconButton(
              onPressed: _refreshLogs,
              icon: const Icon(Icons.refresh),
              tooltip: '刷新',
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLogFileList() {
    return Card(
      elevation: 4,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.all(16.0),
            child: Text(
              '日志文件列表',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: _isLoading && _logFiles.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _errorMessage.isNotEmpty && _logFiles.isEmpty
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: SelectableText(
                            _errorMessage,
                            style: const TextStyle(color: Colors.red),
                          ),
                        ),
                      )
                    : ListView.builder(
                        itemCount: _logFiles.length,
                        itemBuilder: (context, index) {
                          final file = _logFiles[index];
                          return _buildLogFileItem(file);
                        },
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildLogFileItem(LogFileInfo file) {
    final isSelected = file.fileName == _selectedLogFile;

    return InkWell(
      onTap: () => _selectLogFile(file.fileName),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    file.fileName,
                    style: TextStyle(
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                    ),
                  ),
                  Text(
                    '${file.fileSize} | ${file.createTime}',
                    style: const TextStyle(fontSize: 12),
                  ),
                ],
              ),
            ),
            SizedBox(
              width: 50,
              child: IconButton(
                icon: const Icon(Icons.download, size: 20),
                onPressed: () {
                  _downloadLogFile(file.fileName);
                },
                tooltip: '下载',
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLogContent() {
    return Card(
      elevation: 4,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '日志内容: $_selectedLogFile',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  key: const ValueKey('log_search_row'),
                  children: [
                    Expanded(
                      child: TextField(
                        decoration: const InputDecoration(
                          labelText: '关键词筛选',
                          hintText: '输入关键词进行筛选...',
                          prefixIcon: Icon(Icons.search),
                          border: OutlineInputBorder(),
                        ),
                        onChanged: (value) {
                          setState(() {
                            _keywordFilter = value;
                            _currentPage = 1;
                          });
                        },
                        onSubmitted: (value) {
                          _loadLogEntries();
                        },
                      ),
                    ),
                    const SizedBox(width: 16),
                    ElevatedButton.icon(
                      onPressed: _keywordFilter.isNotEmpty
                          ? () {
                              setState(() {
                                _keywordFilter = '';
                                _currentPage = 1;
                              });
                              _loadLogEntries();
                            }
                          : null,
                      icon: const Icon(Icons.clear),
                      label: const Text('清除筛选'),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  key: const ValueKey('log_page_size_row'),
                  children: [
                    const Text('每页显示:'),
                    const SizedBox(width: 10),
                    DropdownButton<int>(
                      value: _pageSize,
                      items: const [
                        DropdownMenuItem(value: 50, child: Text('50')),
                        DropdownMenuItem(value: 100, child: Text('100')),
                        DropdownMenuItem(value: 200, child: Text('200')),
                        DropdownMenuItem(value: 500, child: Text('500')),
                      ],
                      onChanged: (value) {
                        setState(() {
                          _pageSize = value ?? 100;
                          _currentPage = 1;
                        });
                        _loadLogEntries();
                      },
                    ),
                    const SizedBox(width: 20),
                    Text(
                      '总计: $_totalRecords 条记录',
                      style: const TextStyle(fontSize: 12),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _errorMessage.isNotEmpty
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: SelectableText(
                            _errorMessage,
                            style: const TextStyle(color: Colors.red),
                          ),
                        ),
                      )
                    : _logEntries.isEmpty
                        ? const Center(
                            child: Text('暂无日志记录'),
                          )
                        : Column(
                            children: [
                              Expanded(
                                child: ListView.builder(
                                  itemCount: _logEntries.length,
                                  itemBuilder: (context, index) {
                                    final entry = _logEntries[index];
                                    return _buildLogEntry(entry);
                                  },
                                ),
                              ),
                              _buildPagination(),
                            ],
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildLogEntry(LogEntry entry) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: Colors.grey.shade300),
        ),
      ),
      child: Column(
        key: ValueKey('log_entry_column_${entry.hashCode}'),
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            key: ValueKey('log_entry_header_${entry.hashCode}'),
            children: [
              Text(
                entry.timestamp,
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey.shade600,
                ),
              ),
              const SizedBox(width: 10),
              _buildLogLevelChip(entry.level),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            entry.message,
            style: const TextStyle(fontSize: 13),
          ),
          if (entry.stackTrace != null && entry.stackTrace!.isNotEmpty)
            ExpansionTile(
              title: const Text(
                '查看堆栈信息',
                style: TextStyle(fontSize: 12),
              ),
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.grey.shade100,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: SelectableText(
                    entry.stackTrace!,
                    style: TextStyle(
                      fontSize: 11,
                      fontFamily: 'monospace',
                      color: Colors.grey.shade800,
                    ),
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }

  Widget _buildLogLevelChip(String level) {
    Color backgroundColor;
    Color textColor;

    switch (level.toUpperCase()) {
      case 'ERROR':
        backgroundColor = Colors.red.shade100;
        textColor = Colors.red.shade700;
        break;
      case 'WARN':
        backgroundColor = Colors.orange.shade100;
        textColor = Colors.orange.shade700;
        break;
      case 'INFO':
        backgroundColor = Colors.blue.shade100;
        textColor = Colors.blue.shade700;
        break;
      case 'DEBUG':
        backgroundColor = Colors.grey.shade100;
        textColor = Colors.grey.shade700;
        break;
      default:
        backgroundColor = Colors.grey.shade100;
        textColor = Colors.grey.shade700;
    }

    return Chip(
      label: Text(
        level,
        style: TextStyle(
          fontSize: 11,
          color: textColor,
        ),
      ),
      backgroundColor: backgroundColor,
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 0),
      visualDensity: VisualDensity.compact,
    );
  }

  Widget _buildPagination() {
    if (_totalPages <= 1) {
      return const SizedBox.shrink();
    }

    return Container(
      padding: const EdgeInsets.all(16.0),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(color: Colors.grey.shade300),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            onPressed: _currentPage > 1
                ? () {
                    setState(() {
                      _currentPage--;
                    });
                    _loadLogEntries();
                  }
                : null,
            icon: const Icon(Icons.chevron_left),
            tooltip: '上一页',
          ),
          Text('第 $_currentPage 页，共 $_totalPages 页'),
          IconButton(
            onPressed: _currentPage < _totalPages
                ? () {
                    setState(() {
                      _currentPage++;
                    });
                    _loadLogEntries();
                  }
                : null,
            icon: const Icon(Icons.chevron_right),
            tooltip: '下一页',
          ),
        ],
      ),
    );
  }

  Future<void> _downloadLogFile(String fileName) async {
    try {
      final result = await _logService.downloadLogFile(fileName);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result['message'] ?? '下载已开始')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('下载失败: $e')),
        );
      }
    }
  }
}

class LogFileInfo {
  final String fileName;
  final String fileSize;
  final String createTime;

  LogFileInfo({
    required this.fileName,
    required this.fileSize,
    required this.createTime,
  });

  factory LogFileInfo.fromJson(Map<String, dynamic> json) {
    return LogFileInfo(
      fileName: json['fileName'] ?? '',
      fileSize: json['fileSize'] ?? '',
      createTime: json['createTime'] ?? '',
    );
  }
}

class LogEntry {
  final String timestamp;
  final String level;
  final String message;
  final String? stackTrace;

  LogEntry({
    required this.timestamp,
    required this.level,
    required this.message,
    this.stackTrace,
  });

  factory LogEntry.fromJson(Map<String, dynamic> json) {
    return LogEntry(
      timestamp: json['timestamp'] ?? '',
      level: json['level'] ?? 'INFO',
      message: json['message'] ?? '',
      stackTrace: json['stackTrace'],
    );
  }
}
