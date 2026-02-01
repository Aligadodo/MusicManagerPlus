import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'dart:html' as html;
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

  void _copyToClipboard(String text) {
    Clipboard.setData(ClipboardData(text: text));
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: GestureDetector(
            onDoubleTap: () => _copyToClipboard('已复制到剪贴板'),
            child: const SelectableText('已复制到剪贴板'),
          ),
          duration: const Duration(seconds: 1),
        ),
      );
    }
  }

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
        try {
          List<dynamic> filesList = [];
          if (result.containsKey('files')) {
            filesList = result['files'] as List<dynamic>;
          }
          
          _logFiles = filesList
              .map((json) {
                try {
                  return LogFileInfo.fromJson(json as Map<String, dynamic>);
                } catch (e) {
                  print('解析日志文件信息失败: $e, json: $json');
                  return null;
                }
              })
              .whereType<LogFileInfo>()
              .toList();
          _isLoading = false;

          // 默认选择最新的日志文件
          if (_logFiles.isNotEmpty) {
            _selectedLogFile = _logFiles.first.fileName;
            _loadLogEntries();
          }
        } catch (e) {
          _errorMessage = '解析日志文件列表失败: $e';
          _isLoading = false;
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
        try {
          _logEntries = (result['entries'] as List<dynamic>? ?? [])
              .map((json) {
                try {
                  return LogEntry.fromJson(json as Map<String, dynamic>);
                } catch (e) {
                  print('解析日志条目失败: $e, json: $json');
                  return null;
                }
              })
              .whereType<LogEntry>()
              .toList();
          _totalRecords = (result['total'] as int?) ?? 0;
          _totalPages = (result['pages'] as int?) ?? 0;
          _isLoading = false;
        } catch (e) {
          _errorMessage = '解析日志内容失败: $e';
          _isLoading = false;
        }
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

  Future<void> _downloadLogFile(String fileName) async {
    try {
      final result = await _logService.downloadLogFile(fileName);
      
      if (result['success'] == true) {
        final downloadUrl = 'http://localhost:8080/api/logs/download/$fileName';
        final anchor = html.AnchorElement(href: downloadUrl)
          ..setAttribute('download', fileName)
          ..click();
        anchor.remove();
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: GestureDetector(
                onDoubleTap: () => _copyToClipboard('开始下载: $fileName'),
                child: SelectableText('开始下载: $fileName'),
              ),
            ),
          );
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: GestureDetector(
                onDoubleTap: () => _copyToClipboard('下载失败: ${result['message'] ?? '未知错误'}'),
                child: SelectableText('下载失败: ${result['message'] ?? '未知错误'}'),
              ),
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: GestureDetector(
              onDoubleTap: () => _copyToClipboard('下载失败: $e'),
              child: SelectableText('下载失败: $e'),
            ),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        padding: const EdgeInsets.all(12.0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              flex: 1,
              child: _buildLogFileList(),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 3,
              child: _buildLogContent(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLogFileList() {
    return Card(
      elevation: 2,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Row(
              children: [
                const Text(
                  '日志文件',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                IconButton(
                  onPressed: _refreshLogs,
                  icon: const Icon(Icons.refresh, size: 16),
                  tooltip: '刷新',
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: _isLoading && _logFiles.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : _errorMessage.isNotEmpty && _logFiles.isEmpty
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(12.0),
                          child: GestureDetector(
                            onDoubleTap: () => _copyToClipboard(_errorMessage),
                            child: SelectableText(
                              _errorMessage,
                              style: const TextStyle(color: Colors.red),
                            ),
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

    return GestureDetector(
      onTap: () => _selectLogFile(file.fileName),
      child: Container(
        decoration: BoxDecoration(
          color: isSelected ? Colors.blue.shade50 : Colors.transparent,
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          child: Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    GestureDetector(
                      onDoubleTap: () => _copyToClipboard(file.fileName),
                      child: SelectableText(
                        file.fileName,
                        style: TextStyle(
                          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                          fontSize: 13,
                        ),
                      ),
                    ),
                    GestureDetector(
                      onDoubleTap: () => _copyToClipboard('${file.fileSize} | ${file.createTime}'),
                      child: SelectableText(
                        '${file.fileSize} | ${file.createTime}',
                        style: const TextStyle(fontSize: 11),
                      ),
                    ),
                  ],
                ),
              ),
              SizedBox(
                width: 40,
                child: IconButton(
                  icon: const Icon(Icons.download, size: 16),
                  onPressed: () {
                    _downloadLogFile(file.fileName);
                  },
                  tooltip: '下载',
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLogContent() {
    return Card(
      elevation: 2,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                GestureDetector(
                  onDoubleTap: () => _copyToClipboard('日志内容: $_selectedLogFile'),
                  child: SelectableText(
                    '日志内容: $_selectedLogFile',
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  key: const ValueKey('log_search_row'),
                  children: [
                    Expanded(
                      child: TextField(
                        decoration: const InputDecoration(
                          labelText: '关键词筛选',
                          hintText: '输入关键词进行筛选...',
                          prefixIcon: Icon(Icons.search, size: 16),
                          border: OutlineInputBorder(),
                          contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
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
                    const SizedBox(width: 12),
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
                      icon: const Icon(Icons.clear, size: 14),
                      label: const Text('清除', style: TextStyle(fontSize: 12)),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  key: const ValueKey('log_page_size_row'),
                  children: [
                    const Text('每页显示:', style: TextStyle(fontSize: 12)),
                    const SizedBox(width: 8),
                    DropdownButton<int>(
                      value: _pageSize,
                      items: const [
                        DropdownMenuItem(value: 100, child: Text('100')),
                        DropdownMenuItem(value: 200, child: Text('200')),
                        DropdownMenuItem(value: 300, child: Text('300')),
                        DropdownMenuItem(value: 500, child: Text('500')),
                      ],
                      onChanged: (value) {
                        setState(() {
                          _pageSize = value ?? 100;
                          _currentPage = 1;
                        });
                        _loadLogEntries();
                      },
                      style: const TextStyle(fontSize: 12),
                    ),
                    const SizedBox(width: 16),
                    GestureDetector(
                      onDoubleTap: () => _copyToClipboard('总计: $_totalRecords 条记录'),
                      child: SelectableText(
                        '总计: $_totalRecords 条记录',
                        style: const TextStyle(fontSize: 11),
                      ),
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
                          padding: const EdgeInsets.all(12.0),
                          child: GestureDetector(
                            onDoubleTap: () => _copyToClipboard(_errorMessage),
                            child: SelectableText(
                              _errorMessage,
                              style: const TextStyle(color: Colors.red),
                            ),
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
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: Colors.grey.shade200),
        ),
      ),
      child: Column(
        key: ValueKey('log_entry_column_${entry.hashCode}'),
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          GestureDetector(
            onDoubleTap: () => _copyToClipboard('${entry.timestamp} ${entry.level} ${entry.message}'),
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 12),
              child: Row(
                key: ValueKey('log_entry_header_${entry.hashCode}'),
                children: [
                  SizedBox(
                    width: 140,
                    child: SelectableText(
                      entry.timestamp,
                      style: TextStyle(
                        fontSize: 11,
                        color: Colors.grey.shade600,
                      ),
                    ),
                  ),
                  SizedBox(
                    width: 70,
                    child: _buildLogLevelChip(entry.level),
                  ),
                  Expanded(
                    child: Text(
                      entry.message.replaceAll('\n', ' ').replaceAll('\r', ' '),
                      style: const TextStyle(fontSize: 12),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      softWrap: false,
                    ),
                  ),
                  if (entry.stackTrace != null && entry.stackTrace!.isNotEmpty)
                    IconButton(
                      icon: const Icon(Icons.expand_more, size: 14),
                      onPressed: () {
                        // 点击展开堆栈信息
                        showDialog(
                          context: context,
                          builder: (context) => AlertDialog(
                            title: const Text('堆栈信息'),
                            content: Container(
                              width: 800,
                              height: 400,
                              child: SingleChildScrollView(
                                child: GestureDetector(
                                  onDoubleTap: () => _copyToClipboard(entry.stackTrace!),
                                  child: SelectableText(
                                    entry.stackTrace!,
                                    style: TextStyle(
                                      fontFamily: 'monospace',
                                      fontSize: 12,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                            actions: [
                              TextButton(
                                onPressed: () => Navigator.pop(context),
                                child: const Text('关闭'),
                              ),
                            ],
                          ),
                        );
                      },
                    ),
                ],
              ),
            ),
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
      label: SelectableText(
        level,
        style: TextStyle(
          fontSize: 10,
          color: textColor,
        ),
      ),
      backgroundColor: backgroundColor,
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 0),
      visualDensity: VisualDensity.compact,
      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
    );
  }

  Widget _buildPagination() {
    if (_totalPages <= 1) {
      return const SizedBox.shrink();
    }

    return Container(
      padding: const EdgeInsets.all(12.0),
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(color: Colors.grey.shade200),
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
            icon: const Icon(Icons.chevron_left, size: 16),
            tooltip: '上一页',
          ),
          GestureDetector(
            onDoubleTap: () => _copyToClipboard('第 $_currentPage 页，共 $_totalPages 页'),
            child: SelectableText(
              '第 $_currentPage / $_totalPages 页',
              style: const TextStyle(fontSize: 12),
            ),
          ),
          IconButton(
            onPressed: _currentPage < _totalPages
                ? () {
                    setState(() {
                      _currentPage++;
                    });
                    _loadLogEntries();
                  }
                : null,
            icon: const Icon(Icons.chevron_right, size: 16),
            tooltip: '下一页',
          ),
        ],
      ),
    );
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
