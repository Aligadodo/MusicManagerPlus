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
  final ScrollController _scrollController = ScrollController();
  final TextEditingController _logController = TextEditingController();

  List<Map<String, dynamic>> _logs = [];
  bool _isLoading = false;
  String _errorMessage = '';

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
      final logs = await _logService.getLogs();
      setState(() {
        _logs = logs;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载日志失败: $e';
        _isLoading = false;
      });
    }
  }

  void _clearLogs() {
    setState(() {
      _logs.clear();
      _logController.clear();
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('日志已清空')),
    );
  }

  void _scrollToTop() {
    _scrollController.animateTo(
      0,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
    );
  }

  void _scrollToBottom() {
    if (_scrollController.hasClients) {
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildLogTools(),
          const SizedBox(height: 10),
          Expanded(
            child: _buildLogArea(),
          ),
        ],
      ),
    );
  }

  Widget _buildLogTools() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        Tooltip(
          message: '清空日志\n清空当前显示的所有日志\n此操作不可撤销',
          child: OutlinedButton.icon(
            onPressed: _clearLogs,
            icon: const Icon(Icons.clear, size: 16),
            label: const Text('清空日志'),
            style: OutlinedButton.styleFrom(
              foregroundColor: Colors.grey.shade700,
            ),
          ),
        ),
        const SizedBox(width: 10),
        Tooltip(
          message: '查看顶部\n滚动到日志的顶部\n查看最早的日志记录',
          child: OutlinedButton.icon(
            onPressed: _scrollToTop,
            icon: const Icon(Icons.arrow_upward, size: 16),
            label: const Text('查看顶部'),
            style: OutlinedButton.styleFrom(
              foregroundColor: Colors.grey.shade700,
            ),
          ),
        ),
        const SizedBox(width: 10),
        Tooltip(
          message: '查看底部\n滚动到日志的底部\n查看最新的日志记录',
          child: OutlinedButton.icon(
            onPressed: _scrollToBottom,
            icon: const Icon(Icons.arrow_downward, size: 16),
            label: const Text('查看底部'),
            style: OutlinedButton.styleFrom(
              foregroundColor: Colors.grey.shade700,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildLogArea() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_errorMessage.isNotEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_errorMessage, style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _loadLogs,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.grey.shade50,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: TextField(
        controller: _logController,
        maxLines: null,
        readOnly: true,
        style: const TextStyle(
          fontFamily: 'monospace',
          fontSize: 12,
        ),
        decoration: const InputDecoration(
          border: InputBorder.none,
          contentPadding: EdgeInsets.all(10),
        ),
        scrollController: _scrollController,
      ),
    );
  }
}
