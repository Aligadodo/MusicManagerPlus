import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'dart:async';
import 'dart:convert';
import '../api/api_client.dart';
import '../api/pipeline_service.dart';
import '../api/source_directory_service.dart';
import '../models/change_record.dart';
import '../models/source_directory.dart';
import '../models/strategy_info.dart';
import '../utils/tooltip_utils.dart';

class PreviewPage extends ConsumerStatefulWidget {
  const PreviewPage({super.key});

  @override
  ConsumerState<PreviewPage> createState() => _PreviewPageState();
}

class _PreviewPageState extends ConsumerState<PreviewPage> {
  final PipelineService _pipelineService = PipelineService(ApiClient());
  final SourceDirectoryService _sourceDirectoryService = SourceDirectoryService(ApiClient());
  final ApiClient _apiClient = ApiClient();
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

  // 分页相关
  int _currentPage = 1;
  int _pageSize = 20;
  int _totalRecords = 0;
  int _totalPages = 0;

  // 进度信息
  double _progress = 0.0;
  String _remainingTime = '00:00:00';
  String _progressStatus = '准备就绪';
  int _completedTasks = 0;
  int _totalTasks = 0;

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
      _progress = 0.0;
      _remainingTime = '计算中...';
      _progressStatus = '分析中';
      _completedTasks = 0;
      _totalTasks = 0;
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

      // 校验策略参数，确保所有参数都有配置或默认值
      bool allParamsValid = true;
      String validationMessage = '';
      
      for (int i = 0; i < _pipeline.length; i++) {
        final strategy = _pipeline[i];
        print('\n=== 校验策略参数: ${strategy.name} (${strategy.id}) ===');
        
        if (strategy.configFields != null) {
          for (final field in strategy.configFields!) {
            // 检查字段是否有值或默认值
            bool hasValue = false;
            if (field.defaultValue != null) {
              hasValue = true;
              print('字段 ${field.name}: 使用默认值 ${field.defaultValue}');
            } else {
              print('字段 ${field.name}: 无默认值，需要配置');
              if (field.required) {
                validationMessage = '策略 "${strategy.name}" 的参数 "${field.label}" 是必填项，请配置';
                allParamsValid = false;
                break;
              }
            }
          }
        }
        
        if (!allParamsValid) {
          break;
        }
      }

      if (!allParamsValid) {
        setState(() {
          _errorMessage = validationMessage;
        });
        return;
      }

      // 输出每个策略的参数信息
      print('\n=== 流水线策略参数信息 ===');
      for (int i = 0; i < _pipeline.length; i++) {
        final strategy = _pipeline[i];
        print('策略 ${i + 1}: ${strategy.name} (${strategy.id})');
        print('配置字段数量: ${strategy.configFields?.length ?? 0}');
        
        if (strategy.configFields != null) {
          for (final field in strategy.configFields!) {
            print('  - ${field.label} (${field.name}): ${field.defaultValue ?? '无默认值'}');
          }
        }
      }

      final sourcePaths = _sourceDirectories.map((d) => d.path).toList();
      final result = await _pipelineService.analyzePipeline(sourcePaths, _pipeline);

      if (result['success'] == true) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result['message'] ?? '分析任务已开始执行')),
          );
        }

        // 立即获取一次变更记录
        await _fetchChanges();
      } else {
        setState(() {
          _errorMessage = result['message'] ?? '分析任务提交失败';
          _progress = 0.0;
          _remainingTime = '00:00:00';
          _progressStatus = '分析失败';
        });
      }
    } catch (e) {
      setState(() {
        _errorMessage = '分析流水线失败: $e';
        _progress = 0.0;
        _remainingTime = '00:00:00';
        _progressStatus = '分析失败';
      });
    }
  }

  Future<void> _executePipeline() async {
    setState(() {
      _isAnalyzing = true;
      _errorMessage = '';
      _progress = 0.0;
      _remainingTime = '计算中...';
      _progressStatus = '执行中';
      _completedTasks = 0;
      _totalTasks = _changeRecords.length;
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

      // 校验策略参数，确保所有参数都有配置或默认值
      bool allParamsValid = true;
      String validationMessage = '';
      
      for (int i = 0; i < _pipeline.length; i++) {
        final strategy = _pipeline[i];
        print('\n=== 校验策略参数: ${strategy.name} (${strategy.id}) ===');
        
        if (strategy.configFields != null) {
          for (final field in strategy.configFields!) {
            // 检查字段是否有值或默认值
            bool hasValue = false;
            if (field.defaultValue != null) {
              hasValue = true;
              print('字段 ${field.name}: 使用默认值 ${field.defaultValue}');
            } else {
              print('字段 ${field.name}: 无默认值，需要配置');
              if (field.required) {
                validationMessage = '策略 "${strategy.name}" 的参数 "${field.label}" 是必填项，请配置';
                allParamsValid = false;
                break;
              }
            }
          }
        }
        
        if (!allParamsValid) {
          break;
        }
      }

      if (!allParamsValid) {
        setState(() {
          _errorMessage = validationMessage;
        });
        return;
      }

      // 输出每个策略的参数信息
      print('\n=== 流水线策略参数信息 ===');
      for (int i = 0; i < _pipeline.length; i++) {
        final strategy = _pipeline[i];
        print('策略 ${i + 1}: ${strategy.name} (${strategy.id})');
        print('配置字段数量: ${strategy.configFields?.length ?? 0}');
        
        if (strategy.configFields != null) {
          for (final field in strategy.configFields!) {
            print('  - ${field.label} (${field.name}): ${field.defaultValue ?? '无默认值'}');
          }
        }
      }

      final sourcePaths = _sourceDirectories.map((d) => d.path).toList();
      final result = await _pipelineService.executePipeline(sourcePaths, _pipeline);
      
      if (result['success'] == true) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result['message'] ?? '执行任务已开始执行')),
          );
        }

        // 立即获取一次变更记录
        await _fetchChanges();
      } else {
        setState(() {
          _errorMessage = result['message'] ?? '执行任务提交失败';
          _progress = 0.0;
          _remainingTime = '00:00:00';
          _progressStatus = '执行失败';
        });
      }
    } catch (e) {
      setState(() {
        _errorMessage = '执行流水线失败: $e';
        _progress = 0.0;
        _remainingTime = '00:00:00';
        _progressStatus = '执行失败';
      });
    } finally {
      // 等待任务完成后再取消定时器
      // 这里不立即取消，因为任务在后台执行，我们需要继续获取进度
    }
  }

  Future<void> _stopPipeline() async {
    setState(() {
      _isAnalyzing = false;
      _progress = 0.0;
      _remainingTime = '00:00:00';
      _progressStatus = '已中止';
    });

    try {
      // 调用后端API停止任务
      final result = await _pipelineService.stopPipeline();
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(result['message'] ?? '任务已成功中止')),
        );
      }
    } catch (e) {
      print('停止任务失败: $e');
    }
  }

  List<ChangeRecord> get _filteredRecords {
    // 现在过滤和分页在后端完成，直接返回当前记录
    return _changeRecords;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        padding: const EdgeInsets.all(12.0),
        child: Column(
          children: [
            _buildFilterBar(),
            const SizedBox(height: 12),
            _buildProgressSection(),
            const SizedBox(height: 12),
            _buildStatsBar(),
            const SizedBox(height: 12),
            Expanded(
              child: _buildPreviewTable(),
            ),
          ],
        ),
      ),
    );
  }

  // Header部分 - 显示任务预览标题
  Widget _buildHeader() {
    return Card(
      key: const ValueKey('preview_header_card'),
      elevation: 4,
      child: Padding(
        key: const ValueKey('preview_header_padding'),
        padding: const EdgeInsets.all(16.0),
        child: Row(
          key: const ValueKey('preview_header_row'),
          children: [
            const Text(
              '任务预览',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const Spacer(),
            if (_isAnalyzing) ...[
              SizedBox(
                key: const ValueKey('analyzing_indicator'),
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2),
              ),
              const SizedBox(width: 10),
              Text('处理中...'),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildFilterBar() {
    return Container(
      padding: const EdgeInsets.all(12.0),
      child: Row(
        key: const ValueKey('filter_bar_row'),
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Tooltip(
            message: ParameterDescriptions.previewPage['search']!,
            child: SizedBox(
              width: 200,
              child: TextField(
                decoration: const InputDecoration(
                  labelText: '搜索',
                  hintText: '搜索文件...',
                  prefixIcon: Icon(Icons.search),
                  border: OutlineInputBorder(),
                  contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                ),
                onChanged: (value) {
                  setState(() {
                    _searchFilter = value;
                    _currentPage = 1;
                  });
                  _fetchChanges();
                },
              ),
            ),
          ),
          const SizedBox(width: 12),
          Tooltip(
            message: ParameterDescriptions.previewPage['statusFilter']!,
            child: Row(
              children: [
                const Text('状态:', style: TextStyle(fontSize: 12)),
                const SizedBox(width: 4),
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
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  style: const TextStyle(fontSize: 12),
                  iconSize: 20,
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Tooltip(
            message: ParameterDescriptions.previewPage['operationTypeFilter']!,
            child: Row(
              children: [
                const Text('操作类型:', style: TextStyle(fontSize: 12)),
                const SizedBox(width: 4),
                DropdownButton<String>(
                  value: _operationTypeFilter,
                  items: const [
                    DropdownMenuItem(value: '全部', child: Text('全部')),
                    DropdownMenuItem(value: 'RENAME', child: Text('重命名')),
                    DropdownMenuItem(value: 'MOVE', child: Text('移动')),
                    DropdownMenuItem(value: 'DELETE', child: Text('删除')),
                    DropdownMenuItem(value: 'COPY', child: Text('复制')),
                    DropdownMenuItem(value: 'METADATA_UPDATE', child: Text('元数据')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _operationTypeFilter = value ?? '全部';
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  style: const TextStyle(fontSize: 12),
                  iconSize: 20,
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Tooltip(
            message: ParameterDescriptions.previewPage['hideUnchanged']!,
            child: Row(
              children: [
                Checkbox(
                  value: _hideUnchanged,
                  onChanged: (value) {
                    setState(() {
                      _hideUnchanged = value ?? true;
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  visualDensity: VisualDensity.compact,
                ),
                const Text('仅显示变更', style: TextStyle(fontSize: 12)),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Row(
            children: [
              const Text('分页:', style: TextStyle(fontSize: 12)),
              const SizedBox(width: 4),
              Tooltip(
                message: ParameterDescriptions.previewPage['pageSize']!,
                child: DropdownButton<int>(
                  value: _pageSize,
                  items: const [
                    DropdownMenuItem(value: 10, child: Text('10')),
                    DropdownMenuItem(value: 20, child: Text('20')),
                    DropdownMenuItem(value: 50, child: Text('50')),
                    DropdownMenuItem(value: 100, child: Text('100')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _pageSize = value ?? 20;
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                  style: const TextStyle(fontSize: 12),
                  iconSize: 20,
                ),
              ),
            ],
          ),
          const Spacer(),
          Tooltip(
            message: ParameterDescriptions.previewPage['refresh']!,
            child: IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: () {
                _fetchChanges();
              },
              tooltip: '刷新',
              iconSize: 20,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatsBar() {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade200),
        borderRadius: BorderRadius.circular(8),
        color: Colors.grey.shade50,
      ),
      child: Row(
        key: const ValueKey('stats_bar_row'),
        children: [
          _buildCompactStatItem('总记录', '${_changeRecords.length}', Icons.list),
          const SizedBox(width: 24),
          _buildCompactStatItem('变更记录', '${_filteredRecords.length}', Icons.edit),
          const SizedBox(width: 24),
          _buildCompactStatItem('成功', '${_changeRecords.where((r) => r.status == 'SUCCESS').length}', Icons.check_circle, Colors.green),
          const SizedBox(width: 24),
          _buildCompactStatItem('失败', '${_changeRecords.where((r) => r.status == 'FAILED').length}', Icons.error, Colors.red),
        ],
      ),
    );
  }

  Widget _buildCompactStatItem(String label, String value, IconData icon, [Color? color]) {
    return Row(
      children: [
        Icon(icon, size: 16, color: color ?? Colors.blue),
        const SizedBox(width: 4),
        Text(
          '$label: $value',
          style: TextStyle(
            fontSize: 12,
            color: color ?? Colors.black,
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
                  ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Column(
                children: [
                  DataTable(
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
                              message: record.filePath ?? '',
                              child: Text(
                                record.filePath ?? '',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ),
                          DataCell(
                            Row(
                              key: ValueKey('status_cell_${record.id}'),
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
                  // 分页控件
                  _buildPagination(),
                ],
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

  // 进度和过滤条件配置区域
  Widget _buildProgressSection() {
    return Container(
      padding: const EdgeInsets.all(16.0),
      child: _buildProgressBar(),
    );
  }

  // 过滤条件显示
  Widget _buildFilterConditions() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '当前过滤条件',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
            color: Colors.grey,
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 12.0,
          runSpacing: 8.0,
          children: [
            if (_searchFilter.isNotEmpty)
              Chip(
                label: Text('搜索: $_searchFilter'),
                backgroundColor: Colors.blue.shade100,
                labelStyle: TextStyle(color: Colors.blue.shade700),
              ),
            Chip(
              label: Text('状态: $_statusFilter'),
              backgroundColor: Colors.green.shade100,
              labelStyle: TextStyle(color: Colors.green.shade700),
            ),
            Chip(
              label: Text('操作类型: $_operationTypeFilter'),
              backgroundColor: Colors.orange.shade100,
              labelStyle: TextStyle(color: Colors.orange.shade700),
            ),
            Chip(
              label: Text(_hideUnchanged ? '仅显示变更' : '显示所有'),
              backgroundColor: Colors.purple.shade100,
              labelStyle: TextStyle(color: Colors.purple.shade700),
            ),
            Chip(
              label: Text('分页: ${_currentPage}/${_totalPages > 0 ? _totalPages : 1}, ${_pageSize}条/页'),
              backgroundColor: Colors.teal.shade100,
              labelStyle: TextStyle(color: Colors.teal.shade700),
            ),
          ],
        ),
      ],
    );
  }

  // 进度条和剩余时间显示
  Widget _buildProgressBar() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        LinearProgressIndicator(
          value: _isAnalyzing ? null : _progress,
          backgroundColor: Colors.grey.shade200,
          valueColor: const AlwaysStoppedAnimation<Color>(Colors.blue),
          minHeight: 8,
          borderRadius: BorderRadius.circular(4),
        ),
        if (_isAnalyzing)
          const SizedBox(height: 4),
        if (_isAnalyzing)
          Row(
            key: const ValueKey('progress_info_row'),
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                _isAnalyzing ? '分析中...' : '',
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey.shade600,
                ),
              ),
              if (_totalTasks > 0)
                Text(
                  '已完成: $_completedTasks / $_totalTasks',
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.grey.shade600,
                  ),
                ),
            ],
          ),
      ],
    );
  }

  // 从后端获取进度信息
  Future<void> _fetchProgress() async {
    if (_isAnalyzing) {
      try {
        // 调用后端API获取任务状态
        final status = await _pipelineService.getPipelineStatus();
        setState(() {
          _progress = status['progress'] ?? 0.0;
          _remainingTime = status['remainingTime'] ?? '00:00:00';
          _progressStatus = status['status'] ?? '分析中';
          _completedTasks = status['completedTasks'] ?? 0;
          _totalTasks = status['totalTasks'] ?? 0;
        });

        // 如果任务完成，更新状态
        if (_progressStatus == '分析完成' || _progressStatus == '执行完成' || _progressStatus == '已中止' || _progressStatus == '分析失败' || _progressStatus == '执行失败') {
          setState(() {
            _isAnalyzing = false;
          });
        }
      } catch (e) {
        print('获取进度信息失败: $e');
      }
    }
  }

  // 从后端获取变更记录
  Future<void> _fetchChanges() async {
    try {
      final result = await _pipelineService.getChanges(
        searchFilter: _searchFilter,
        statusFilter: _statusFilter != '全部' ? _statusFilter : null,
        operationTypeFilter: _operationTypeFilter != '全部' ? _operationTypeFilter : null,
        hideUnchanged: _hideUnchanged,
        page: _currentPage,
        size: _pageSize,
        sortBy: 'id',
        sortDirection: 'ASC',
      );

      setState(() {
        _changeRecords = (result['records'] as List<dynamic>)
            .map((json) => ChangeRecord.fromJson(json as Map<String, dynamic>))
            .toList();
        _totalRecords = result['total'] ?? 0;
        _totalPages = result['pages'] ?? 0;
      });
    } catch (e) {
      print('获取变更记录失败: $e');
    }
  }

  // 分页控件
  Widget _buildPagination() {
    if (_totalPages <= 1) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            onPressed: _currentPage > 1
                ? () {
                    setState(() {
                      _currentPage--;
                    });
                    _fetchChanges();
                  }
                : null,
            icon: const Icon(Icons.chevron_left),
            tooltip: '上一页',
          ),
          Text('第 $_currentPage 页，共 $_totalPages 页，总计 $_totalRecords 条记录'),
          IconButton(
            onPressed: _currentPage < _totalPages
                ? () {
                    setState(() {
                      _currentPage++;
                    });
                    _fetchChanges();
                  }
                : null,
            icon: const Icon(Icons.chevron_right),
            tooltip: '下一页',
          ),
        ],
      ),
    );
  }
} 
