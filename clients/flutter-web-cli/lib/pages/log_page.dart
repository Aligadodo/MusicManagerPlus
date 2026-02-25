import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import 'dart:html' as html;
import 'dart:async';
import '../api/api_client.dart';
import '../api/log_service.dart';
import '../utils/theme_utils.dart';
import '../widgets/log_file_list_widget.dart';
import '../widgets/log_content_widget.dart';

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

  Timer? _autoRefreshTimer;
  bool _autoRefreshEnabled = true;
  static const Duration _refreshInterval = Duration(seconds: 5);

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
    _startAutoRefresh();
  }

  @override
  void dispose() {
    _stopAutoRefresh();
    super.dispose();
  }

  void _startAutoRefresh() {
    _stopAutoRefresh();
    if (_autoRefreshEnabled) {
      _autoRefreshTimer = Timer.periodic(_refreshInterval, (_) {
        _loadLogEntries();
      });
    }
  }

  void _stopAutoRefresh() {
    _autoRefreshTimer?.cancel();
    _autoRefreshTimer = null;
  }

  void _toggleAutoRefresh() {
    setState(() {
      _autoRefreshEnabled = !_autoRefreshEnabled;
      if (_autoRefreshEnabled) {
        _startAutoRefresh();
      } else {
        _stopAutoRefresh();
      }
    });
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

  Future<void> _deleteLogFile(String fileName) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: Text('确定要删除日志文件 "$fileName" 吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(
              foregroundColor: Colors.red,
            ),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed != true) {
      return;
    }

    try {
      await _logService.deleteLogFile(fileName);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: GestureDetector(
              onDoubleTap: () => _copyToClipboard('删除成功: $fileName'),
              child: SelectableText('删除成功: $fileName'),
            ),
          ),
        );
        
        // 刷新日志文件列表
        _loadLogFiles();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: GestureDetector(
              onDoubleTap: () => _copyToClipboard('删除失败: $e'),
              child: SelectableText('删除失败: $e'),
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
    return LogFileListWidget(
      logFiles: _logFiles,
      selectedLogFile: _selectedLogFile,
      isLoading: _isLoading,
      errorMessage: _errorMessage,
      onFileSelected: _selectLogFile,
      onRefresh: _refreshLogs,
      onToggleAutoRefresh: _toggleAutoRefresh,
      autoRefreshEnabled: _autoRefreshEnabled,
      onDownload: _downloadLogFile,
      onDelete: _deleteLogFile,
      onCopyToClipboard: _copyToClipboard,
    );
  }

  Widget _buildLogContent() {
    return LogContentWidget(
      logEntries: _logEntries,
      keywordFilter: _keywordFilter,
      selectedLogFile: _selectedLogFile,
      currentPage: _currentPage,
      totalPages: _totalPages,
      totalRecords: _totalRecords,
      isLoading: _isLoading,
      errorMessage: _errorMessage,
      onKeywordFilterChanged: (value) {
        setState(() {
          _keywordFilter = value;
        });
      },
      onPageChanged: (page) {
        setState(() {
          _currentPage = page;
        });
        _loadLogEntries();
      },
      onCopyToClipboard: _copyToClipboard,
    );
  }

}
