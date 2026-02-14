import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/database_task_service.dart';
import 'package:filemanager_flutter/api/providers.dart';

class DatabaseTaskListPage extends ConsumerStatefulWidget {
  const DatabaseTaskListPage({super.key});

  @override
  ConsumerState<DatabaseTaskListPage> createState() =>
      _DatabaseTaskListPageState();
}

class _DatabaseTaskListPageState extends ConsumerState<DatabaseTaskListPage> {
  final TextEditingController _searchController = TextEditingController();
  List<dynamic> _tasks = [];
  bool _isLoading = false;
  int _currentPage = 1;
  int _pageSize = 20;
  int _totalTasks = 0;
  String? _selectedStatus;
  Map<String, dynamic>? _statistics;

  @override
  void initState() {
    super.initState();
    _loadTasks();
    _loadStatistics();
  }

  Future<void> _loadTasks() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final databaseTaskService = ref.read(databaseTaskServiceProvider);
      final result = await databaseTaskService.getTasks(
        page: _currentPage,
        size: _pageSize,
        status: _selectedStatus,
        keyword: _searchController.text.isNotEmpty ? _searchController.text : null,
      );

      setState(() {
        _tasks = result['data'] ?? [];
        _totalTasks = result['total'] ?? 0;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载任务列表失败: $e')),
        );
      }
    }
  }

  Future<void> _loadStatistics() async {
    try {
      final databaseTaskService = ref.read(databaseTaskServiceProvider);
      final result = await databaseTaskService.getStatistics();
      setState(() {
        _statistics = result['data'];
      });
    } catch (e) {
      print('加载统计信息失败: $e');
    }
  }

  Future<void> _deleteTask(String taskId) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除这个任务吗？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        final databaseTaskService = ref.read(databaseTaskServiceProvider);
        await databaseTaskService.deleteTask(taskId);
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('任务已删除')),
          );
          _loadTasks();
          _loadStatistics();
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('删除任务失败: $e')),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('数据库任务管理'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              _loadTasks();
              _loadStatistics();
            },
          ),
        ],
      ),
      body: Column(
        children: [
          if (_statistics != null)
            _buildStatisticsCard(),
          _buildSearchBar(),
          _buildFilterBar(),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _buildTaskList(),
          ),
          _buildPagination(),
        ],
      ),
    );
  }

  Widget _buildStatisticsCard() {
    return Card(
      margin: const EdgeInsets.all(8),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            _buildStatItem('总任务数', _statistics?['totalTasks'] ?? 0),
            _buildStatItem('总变更数', _statistics?['totalChanges'] ?? 0),
            _buildStatItem('总日志数', _statistics?['totalLogs'] ?? 0),
          ],
        ),
      ),
    );
  }

  Widget _buildStatItem(String label, int value) {
    return Column(
      children: [
        Text(
          value.toString(),
          style: const TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
          ),
        ),
        Text(
          label,
          style: const TextStyle(fontSize: 12),
        ),
      ],
    );
  }

  Widget _buildSearchBar() {
    return Padding(
      padding: const EdgeInsets.all(8),
      child: TextField(
        controller: _searchController,
        decoration: InputDecoration(
          labelText: '搜索任务',
          prefixIcon: const Icon(Icons.search),
          suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: () {
                    _searchController.clear();
                    _loadTasks();
                  },
                )
              : null,
          border: const OutlineInputBorder(),
        ),
        onSubmitted: (_) => _loadTasks(),
      ),
    );
  }

  Widget _buildFilterBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      child: Row(
        children: [
          const Text('状态: '),
          DropdownButton<String>(
            value: _selectedStatus,
            hint: const Text('全部'),
            items: const [
              DropdownMenuItem(value: null, child: Text('全部')),
              DropdownMenuItem(value: 'CREATED', child: Text('已创建')),
              DropdownMenuItem(value: 'SCANNING', child: Text('正在扫描')),
              DropdownMenuItem(value: 'SCANNED', child: Text('扫描完成')),
              DropdownMenuItem(value: 'PREVIEWING', child: Text('正在预览')),
              DropdownMenuItem(value: 'PREVIEWED', child: Text('预览完成')),
              DropdownMenuItem(value: 'EXECUTING', child: Text('正在执行')),
              DropdownMenuItem(value: 'COMPLETED', child: Text('执行完成')),
              DropdownMenuItem(value: 'FAILED', child: Text('执行失败')),
              DropdownMenuItem(value: 'CANCELLED', child: Text('已取消')),
            ],
            onChanged: (value) {
              setState(() {
                _selectedStatus = value;
              });
              _loadTasks();
            },
          ),
        ],
      ),
    );
  }

  Widget _buildTaskList() {
    if (_tasks.isEmpty) {
      return const Center(child: Text('暂无任务'));
    }

    return ListView.builder(
      itemCount: _tasks.length,
      itemBuilder: (context, index) {
        final task = _tasks[index];
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          child: ListTile(
            title: Text(task['taskName'] ?? task['taskId']),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('ID: ${task['taskId']}'),
                Text('状态: ${task['status']}'),
                if (task['message'] != null) Text('消息: ${task['message']}'),
              ],
            ),
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                IconButton(
                  icon: const Icon(Icons.info),
                  onPressed: () {
                    _showTaskDetails(task);
                  },
                ),
                IconButton(
                  icon: const Icon(Icons.delete),
                  onPressed: () {
                    _deleteTask(task['taskId']);
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildPagination() {
    final totalPages = (_totalTasks / _pageSize).ceil();
    
    return Padding(
      padding: const EdgeInsets.all(8),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            icon: const Icon(Icons.chevron_left),
            onPressed: _currentPage > 1
                ? () {
                    setState(() {
                      _currentPage--;
                    });
                    _loadTasks();
                  }
                : null,
          ),
          Text('$_currentPage / $totalPages'),
          IconButton(
            icon: const Icon(Icons.chevron_right),
            onPressed: _currentPage < totalPages
                ? () {
                    setState(() {
                      _currentPage++;
                    });
                    _loadTasks();
                  }
                : null,
          ),
        ],
      ),
    );
  }

  void _showTaskDetails(Map<String, dynamic> task) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(task['taskName'] ?? task['taskId']),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildDetailRow('任务ID', task['taskId']),
              _buildDetailRow('任务名称', task['taskName']),
              _buildDetailRow('状态', task['status']),
              _buildDetailRow('当前阶段', task['currentStage']),
              _buildDetailRow('进度', '${(task['overallProgress'] ?? 0).toStringAsFixed(1)}%'),
              _buildDetailRow('消息', task['message']),
              _buildDetailRow('创建时间', task['createdAt']),
              _buildDetailRow('更新时间', task['updatedAt']),
            ],
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
  }

  Widget _buildDetailRow(String label, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              '$label:',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          Expanded(
            child: Text(value?.toString() ?? '-'),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }
}
