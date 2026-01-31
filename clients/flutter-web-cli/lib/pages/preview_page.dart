import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/pipeline_service.dart';
import '../api/source_directory_service.dart';
import '../models/change_record.dart';
import '../models/source_directory.dart';
import '../models/strategy_info.dart';

class PreviewPage extends ConsumerStatefulWidget {
  const PreviewPage({super.key});

  @override
  ConsumerState<PreviewPage> createState() => _PreviewPageState();
}

class _PreviewPageState extends ConsumerState<PreviewPage> {
  final PipelineService _pipelineService = PipelineService(ApiClient());
  final SourceDirectoryService _sourceDirectoryService = SourceDirectoryService(ApiClient());
  List<ChangeRecord> _changeRecords = [];
  List<SourceDirectory> _sourceDirectories = [];
  List<StrategyInfo> _pipeline = [];
  bool _isLoading = false;
  bool _isAnalyzing = false;
  String _errorMessage = '';

  String _searchFilter = '';
  String _statusFilter = '全部';
  String _operationTypeFilter = '全部';
  bool _hideUnchanged = true;
  int _previewLimit = 200;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final directories = await _sourceDirectoryService.getSourceDirectories();
      final pipeline = await _pipelineService.getPipeline();

      setState(() {
        _sourceDirectories = directories;
        _pipeline = pipeline;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载数据失败: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _analyzePipeline() async {
    setState(() {
      _isAnalyzing = true;
      _errorMessage = '';
    });

    try {
      if (_sourceDirectories.isEmpty) {
        setState(() {
          _errorMessage = '请先添加源目录';
        });
        return;
      }

      if (_pipeline.isEmpty) {
        setState(() {
          _errorMessage = '请先配置插件流水线';
        });
        return;
      }

      final sourcePaths = _sourceDirectories.map((d) => d.path).toList();
      final changes = await _pipelineService.analyzePipeline(sourcePaths, _pipeline);
      setState(() {
        _changeRecords = changes;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '分析流水线失败: $e';
      });
    } finally {
      setState(() {
        _isAnalyzing = false;
      });
    }
  }

  Future<void> _executePipeline() async {
    setState(() {
      _isAnalyzing = true;
      _errorMessage = '';
    });

    try {
      if (_sourceDirectories.isEmpty) {
        setState(() {
          _errorMessage = '请先添加源目录';
        });
        return;
      }

      if (_pipeline.isEmpty) {
        setState(() {
          _errorMessage = '请先配置插件流水线';
        });
        return;
      }

      final sourcePaths = _sourceDirectories.map((d) => d.path).toList();
      final result = await _pipelineService.executePipeline(sourcePaths, _pipeline);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('执行任务已创建，任务ID: ${result['taskId']}')),
        );
      }
    } catch (e) {
      setState(() {
        _errorMessage = '执行流水线失败: $e';
      });
    } finally {
      setState(() {
        _isAnalyzing = false;
      });
    }
  }

  List<ChangeRecord> get _filteredRecords {
    var records = _changeRecords;

    if (_searchFilter.isNotEmpty) {
      records = records.where((r) =>
          r.originalName.toLowerCase().contains(_searchFilter.toLowerCase()) ||
          (r.filePath != null && r.filePath!.toLowerCase().contains(_searchFilter.toLowerCase()))).toList();
    }

    if (_statusFilter != '全部') {
      records = records.where((r) => r.status == _statusFilter).toList();
    }

    if (_operationTypeFilter != '全部') {
      records = records.where((r) => r.operationType == _operationTypeFilter).toList();
    }

    if (_hideUnchanged) {
      records = records.where((r) => r.changed).toList();
    }

    return records.take(_previewLimit).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            _buildFilterBar(),
            const SizedBox(height: 20),
            _buildStatsBar(),
            const SizedBox(height: 20),
            Expanded(
              child: _buildPreviewTable(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterBar() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                decoration: const InputDecoration(
                  labelText: '搜索',
                  hintText: '请输入关键词进行搜索...',
                  prefixIcon: Icon(Icons.search),
                  border: OutlineInputBorder(),
                ),
                onChanged: (value) {
                  setState(() {
                    _searchFilter = value;
                  });
                },
              ),
            ),
            const SizedBox(width: 16),
            DropdownButton<String>(
              value: _statusFilter,
              items: const [
                DropdownMenuItem(value: '全部', child: Text('全部')),
                DropdownMenuItem(value: 'PENDING', child: Text('待执行')),
                DropdownMenuItem(value: 'SUCCESS', child: Text('成功')),
                DropdownMenuItem(value: 'FAILED', child: Text('失败')),
                DropdownMenuItem(value: 'SKIPPED', child: Text('跳过')),
              ],
              onChanged: (value) {
                setState(() {
                  _statusFilter = value ?? '全部';
                });
              },
            ),
            const SizedBox(width: 16),
            DropdownButton<String>(
              value: _operationTypeFilter,
              items: const [
                DropdownMenuItem(value: '全部', child: Text('全部')),
                DropdownMenuItem(value: 'RENAME', child: Text('重命名')),
                DropdownMenuItem(value: 'MOVE', child: Text('移动')),
                DropdownMenuItem(value: 'DELETE', child: Text('删除')),
                DropdownMenuItem(value: 'COPY', child: Text('复制')),
                DropdownMenuItem(value: 'METADATA_UPDATE', child: Text('元数据更新')),
              ],
              onChanged: (value) {
                setState(() {
                  _operationTypeFilter = value ?? '全部';
                });
              },
            ),
            const SizedBox(width: 16),
            CheckboxListTile(
              title: const Text('仅显示变更'),
              value: _hideUnchanged,
              onChanged: (value) {
                setState(() {
                  _hideUnchanged = value ?? true;
                });
              },
              controlAffinity: ListTileControlAffinity.leading,
              contentPadding: EdgeInsets.zero,
              visualDensity: VisualDensity.compact,
            ),
            const SizedBox(width: 16),
            DropdownButton<int>(
              value: _previewLimit,
              items: const [
                DropdownMenuItem(value: 50, child: Text('50')),
                DropdownMenuItem(value: 100, child: Text('100')),
                DropdownMenuItem(value: 200, child: Text('200')),
                DropdownMenuItem(value: 500, child: Text('500')),
                DropdownMenuItem(value: 1000, child: Text('1000')),
              ],
              onChanged: (value) {
                setState(() {
                  _previewLimit = value ?? 200;
                });
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatsBar() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          children: [
            Expanded(
              child: _buildStatItem('总记录', '${_changeRecords.length}', Icons.list),
            ),
            Expanded(
              child: _buildStatItem('变更记录', '${_filteredRecords.length}', Icons.edit),
            ),
            Expanded(
              child: _buildStatItem('成功', '${_changeRecords.where((r) => r.status == 'SUCCESS').length}', Icons.check_circle, Colors.green),
            ),
            Expanded(
              child: _buildStatItem('失败', '${_changeRecords.where((r) => r.status == 'FAILED').length}', Icons.error, Colors.red),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatItem(String label, String value, IconData icon, [Color? color]) {
    return Column(
      children: [
        Icon(icon, size: 32, color: color ?? Colors.blue),
        const SizedBox(height: 8),
        Text(
          label,
          style: const TextStyle(fontSize: 12, color: Colors.grey),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
            color: color ?? Colors.blue,
          ),
        ),
      ],
    );
  }

  Widget _buildPreviewTable() {
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
              onPressed: _loadData,
              child: const Text('重试'),
            ),
          ],
        ),
      );
    }

    if (_changeRecords.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.analytics_outlined, size: 64, color: Colors.grey.shade400),
            const SizedBox(height: 20),
            const Text(
              '暂无变更记录',
              style: TextStyle(fontSize: 18, color: Colors.grey),
            ),
            const SizedBox(height: 10),
            const Text(
              '请先在「任务编排」页面配置流水线，然后点击「分析变更」按钮',
              style: TextStyle(fontSize: 14, color: Colors.grey),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    return Card(
      elevation: 4,
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Row(
              children: [
                const Text(
                  '变更预览',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                if (_isAnalyzing)
                  const Row(
                    children: [
                      SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                      SizedBox(width: 10),
                      Text('正在分析...'),
                    ],
                  )
                else
                  Row(
                    children: [
                      ElevatedButton.icon(
                        onPressed: _analyzePipeline,
                        icon: const Icon(Icons.visibility),
                        label: const Text('分析变更'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.blue,
                          foregroundColor: Colors.white,
                        ),
                      ),
                      const SizedBox(width: 10),
                      ElevatedButton.icon(
                        onPressed: _executePipeline,
                        icon: const Icon(Icons.play_arrow),
                        label: const Text('执行变更'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.green,
                          foregroundColor: Colors.white,
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: DataTable(
                columns: const [
                  DataColumn(label: Text('操作类型')),
                  DataColumn(label: Text('原始文件名')),
                  DataColumn(label: Text('新文件名')),
                  DataColumn(label: Text('文件路径')),
                  DataColumn(label: Text('状态')),
                  DataColumn(label: Text('变更原因')),
                ],
                rows: _filteredRecords.map((record) {
                  return DataRow(
                    cells: [
                      DataCell(Text(record.operationType ?? 'N/A')),
                      DataCell(Text(record.originalName)),
                      DataCell(Text(record.newName ?? '-')),
                      DataCell(
                        Tooltip(
                          message: record.filePath,
                          child: Text(
                            record.filePath,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ),
                      DataCell(
                        Row(
                          children: [
                            Icon(
                              _getStatusIcon(record.status),
                              size: 16,
                              color: _getStatusColor(record.status),
                            ),
                            const SizedBox(width: 4),
                            Text(record.status),
                          ],
                        ),
                      ),
                      DataCell(Text(record.reason ?? '')),
                    ],
                  );
                }).toList(),
              ),
            ),
          ),
        ],
      ),
    );
  }

  IconData _getStatusIcon(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return Icons.check_circle;
      case 'FAILED':
        return Icons.error;
      case 'PENDING':
        return Icons.pending;
      default:
        return Icons.help_outline;
    }
  }

  Color _getStatusColor(String status) {
    switch (status.toUpperCase()) {
      case 'SUCCESS':
        return Colors.green;
      case 'FAILED':
        return Colors.red;
      case 'PENDING':
        return Colors.orange;
      default:
        return Colors.grey;
    }
  }
}
