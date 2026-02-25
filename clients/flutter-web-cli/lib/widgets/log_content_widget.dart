import 'package:flutter/material.dart';
import '../utils/theme_utils.dart';

class LogEntry {
  final String timestamp;
  final String level;
  final String message;
  final String? logger;
  final String? thread;

  LogEntry({
    required this.timestamp,
    required this.level,
    required this.message,
    this.logger,
    this.thread,
  });

  factory LogEntry.fromJson(Map<String, dynamic> json) {
    return LogEntry(
      timestamp: json['timestamp'] as String? ?? '',
      level: json['level'] as String? ?? 'INFO',
      message: json['message'] as String? ?? '',
      logger: json['logger'] as String?,
      thread: json['thread'] as String?,
    );
  }
}

class LogContentWidget extends StatelessWidget {
  final List<LogEntry> logEntries;
  final String keywordFilter;
  final String selectedLogFile;
  final int currentPage;
  final int totalPages;
  final int totalRecords;
  final bool isLoading;
  final String errorMessage;
  final Function(String) onKeywordFilterChanged;
  final Function(int) onPageChanged;
  final Function(String) onCopyToClipboard;

  const LogContentWidget({
    super.key,
    required this.logEntries,
    required this.keywordFilter,
    required this.selectedLogFile,
    required this.currentPage,
    required this.totalPages,
    required this.totalRecords,
    required this.isLoading,
    required this.errorMessage,
    required this.onKeywordFilterChanged,
    required this.onPageChanged,
    required this.onCopyToClipboard,
  });

  List<LogEntry> get _filteredEntries {
    if (keywordFilter.isEmpty) return logEntries;
    return logEntries.where((entry) {
      return entry.message.toLowerCase().contains(keywordFilter.toLowerCase()) ||
             entry.level.toLowerCase().contains(keywordFilter.toLowerCase()) ||
             (entry.logger?.toLowerCase().contains(keywordFilter.toLowerCase()) ?? false);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final filteredEntries = _filteredEntries;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 8.0),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  selectedLogFile.isEmpty ? '请选择日志文件' : selectedLogFile,
                  style: const TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              if (selectedLogFile.isNotEmpty)
                Text(
                  '共 $totalRecords 条记录',
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey[600],
                  ),
                ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 4.0),
          child: TextField(
            decoration: InputDecoration(
              labelText: '关键词过滤',
              prefixIcon: const Icon(Icons.search, size: 16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(4),
              ),
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 8,
                vertical: 8,
              ),
              isDense: true,
            ),
            onChanged: onKeywordFilterChanged,
          ),
        ),
        Expanded(
          child: isLoading
              ? const Center(child: CircularProgressIndicator())
              : errorMessage.isNotEmpty
                  ? Center(
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        child: GestureDetector(
                          onDoubleTap: () => onCopyToClipboard(errorMessage),
                          child: SelectableText(
                            errorMessage,
                            style: const TextStyle(color: Colors.red),
                          ),
                        ),
                      ),
                    )
                  : selectedLogFile.isEmpty
                      ? const Center(child: Text('请选择日志文件'))
                      : filteredEntries.isEmpty
                          ? const Center(child: Text('没有找到匹配的日志'))
                          : ListView.builder(
                              itemCount: filteredEntries.length,
                              itemBuilder: (context, index) {
                                final entry = filteredEntries[index];
                                return _buildLogEntry(context, entry);
                              },
                            ),
        ),
        if (totalPages > 1) _buildPagination(context),
      ],
    );
  }

  Widget _buildLogEntry(BuildContext context, LogEntry entry) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8.0, vertical: 2.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildLogLevelChip(entry.level),
          const SizedBox(width: 8),
          Expanded(
            child: GestureDetector(
              onDoubleTap: () => onCopyToClipboard(entry.message),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    entry.timestamp,
                    style: TextStyle(
                      fontSize: 11,
                      color: Colors.grey[600],
                      fontFamily: 'monospace',
                    ),
                  ),
                  SelectableText(
                    entry.message,
                    style: TextStyle(
                      fontSize: 12,
                      color: _getLevelColor(entry.level),
                    ),
                  ),
                  if (entry.logger != null || entry.thread != null)
                    Text(
                      '${entry.logger ?? ''}${entry.thread != null ? ' [${entry.thread}]' : ''}',
                      style: TextStyle(
                        fontSize: 10,
                        color: Colors.grey[500],
                        fontStyle: FontStyle.italic,
                      ),
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
    Color chipColor;
    Color textColor;
    switch (level.toUpperCase()) {
      case 'ERROR':
        chipColor = Colors.red[100]!;
        textColor = Colors.red[900]!;
        break;
      case 'WARN':
        chipColor = Colors.orange[100]!;
        textColor = Colors.orange[900]!;
        break;
      case 'DEBUG':
        chipColor = Colors.blue[100]!;
        textColor = Colors.blue[900]!;
        break;
      case 'TRACE':
        chipColor = Colors.purple[100]!;
        textColor = Colors.purple[900]!;
        break;
      default:
        chipColor = Colors.green[100]!;
        textColor = Colors.green[900]!;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: chipColor,
        borderRadius: BorderRadius.circular(3),
      ),
      child: Text(
        level.toUpperCase(),
        style: TextStyle(
          fontSize: 10,
          fontWeight: FontWeight.bold,
          color: textColor,
        ),
      ),
    );
  }

  Color _getLevelColor(String level) {
    switch (level.toUpperCase()) {
      case 'ERROR':
        return Colors.red[700]!;
      case 'WARN':
        return Colors.orange[700]!;
      case 'DEBUG':
        return Colors.blue[700]!;
      case 'TRACE':
        return Colors.purple[700]!;
      default:
        return Colors.black87;
    }
  }

  Widget _buildPagination(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8.0),
      decoration: BoxDecoration(
        color: ThemeUtils.getPrimaryColor(context).withOpacity(0.05),
        border: Border(
          top: BorderSide(color: Colors.grey[300]!),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            onPressed: currentPage > 1 ? () => onPageChanged(currentPage - 1) : null,
            icon: const Icon(Icons.chevron_left, size: 20),
            tooltip: '上一页',
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12.0),
            child: Text(
              '第 $currentPage / $totalPages 页',
              style: const TextStyle(fontSize: 12),
            ),
          ),
          IconButton(
            onPressed: currentPage < totalPages ? () => onPageChanged(currentPage + 1) : null,
            icon: const Icon(Icons.chevron_right, size: 20),
            tooltip: '下一页',
          ),
        ],
      ),
    );
  }
}
