import 'package:flutter/material.dart';
import '../../utils/theme_utils.dart';

class LogFileInfo {
  final String fileName;
  final int fileSize;
  final DateTime lastModified;
  final String? description;

  LogFileInfo({
    required this.fileName,
    required this.fileSize,
    required this.lastModified,
    this.description,
  });

  factory LogFileInfo.fromJson(Map<String, dynamic> json) {
    return LogFileInfo(
      fileName: json['fileName'] as String? ?? '',
      fileSize: json['fileSize'] as int? ?? 0,
      lastModified: DateTime.tryParse(json['lastModified'] as String? ?? '') ?? DateTime.now(),
      description: json['description'] as String?,
    );
  }
}

class LogFileListWidget extends StatelessWidget {
  final List<LogFileInfo> logFiles;
  final String selectedLogFile;
  final bool isLoading;
  final String errorMessage;
  final Function(String) onFileSelected;
  final Function() onRefresh;
  final Function() onToggleAutoRefresh;
  final bool autoRefreshEnabled;
  final Function(String) onDownload;
  final Function(String) onDelete;
  final Function(String) onCopyToClipboard;

  const LogFileListWidget({
    super.key,
    required this.logFiles,
    required this.selectedLogFile,
    required this.isLoading,
    required this.errorMessage,
    required this.onFileSelected,
    required this.onRefresh,
    required this.onToggleAutoRefresh,
    required this.autoRefreshEnabled,
    required this.onDownload,
    required this.onDelete,
    required this.onCopyToClipboard,
  });

  String _formatFileSize(int bytes) {
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(2)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(2)} MB';
  }

  String _formatDateTime(DateTime dateTime) {
    return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} '
           '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}:${dateTime.second.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 8.0),
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
                onPressed: onRefresh,
                icon: const Icon(Icons.refresh, size: 16),
                tooltip: '刷新',
              ),
              IconButton(
                onPressed: onToggleAutoRefresh,
                icon: Icon(
                  autoRefreshEnabled ? Icons.autorenew : Icons.play_disabled,
                  size: 16,
                  color: autoRefreshEnabled ? Colors.green : Colors.grey,
                ),
                tooltip: autoRefreshEnabled ? '自动刷新已开启' : '自动刷新已关闭',
              ),
            ],
          ),
        ),
        Expanded(
          child: isLoading && logFiles.isEmpty
              ? const Center(child: CircularProgressIndicator())
              : errorMessage.isNotEmpty && logFiles.isEmpty
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
                  : ListView.builder(
                      itemCount: logFiles.length,
                      itemBuilder: (context, index) {
                        final file = logFiles[index];
                        return _buildLogFileItem(context, file);
                      },
                    ),
        ),
      ],
    );
  }

  Widget _buildLogFileItem(BuildContext context, LogFileInfo file) {
    final isSelected = file.fileName == selectedLogFile;

    return GestureDetector(
      onDoubleTap: () {
        onFileSelected(file.fileName);
        onCopyToClipboard(file.fileName);
      },
      child: Container(
        decoration: BoxDecoration(
          color: isSelected ? ThemeUtils.getPrimaryColor(context).withOpacity(0.1) : Colors.transparent,
        ),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          child: Row(
            children: [
              Radio<String>(
                value: file.fileName,
                groupValue: selectedLogFile,
                onChanged: (value) {
                  if (value != null) {
                    onFileSelected(value);
                  }
                },
                activeColor: ThemeUtils.getPrimaryColor(context),
                visualDensity: VisualDensity.standard,
                materialTapTargetSize: MaterialTapTargetSize.padded,
              ),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      file.fileName,
                      style: TextStyle(
                        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                        fontSize: 13,
                      ),
                    ),
                    if (file.description != null && file.description!.isNotEmpty)
                      Text(
                        file.description!,
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.grey[600],
                        ),
                      ),
                    Text(
                      '${_formatFileSize(file.fileSize)} - ${_formatDateTime(file.lastModified)}',
                      style: TextStyle(
                        fontSize: 11,
                        color: Colors.grey[600],
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                onPressed: () => onDownload(file.fileName),
                icon: const Icon(Icons.download, size: 16),
                tooltip: '下载',
                visualDensity: VisualDensity.compact,
                padding: const EdgeInsets.symmetric(horizontal: 4),
              ),
              IconButton(
                onPressed: () => onDelete(file.fileName),
                icon: const Icon(Icons.close, size: 16),
                tooltip: '删除',
                visualDensity: VisualDensity.compact,
                padding: const EdgeInsets.symmetric(horizontal: 4),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
