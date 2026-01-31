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
  bool _autoRefresh = true;
  int _previewLimit = 200;
  int _previewThreads = 10;
  int _executionThreads = 4;
  String _threadPoolMode = 'GLOBAL';

  // 分页相关
  int _currentPage = 1;
  int _pageSize = 20;
  int _totalRecords = 0;
  int _totalPages = 0;

  // 刷新时间间隔配置
  int _listRefreshInterval = 2; // 列表刷新间隔（秒）
  int _progressRefreshInterval = 1; // 进度刷新间隔（秒）

  // 进度信息
  double _progress = 0.0;
  String _remainingTime = '00:00:00';
  String _progressStatus = '准备就绪';
  int _completedTasks = 0;
  int _totalTasks = 0;

  // 运行参数配置
  bool _showAdvancedParams = false;
  int _globalPreviewLimit = 100;
  int _globalExecutionLimit = 100;
  bool _unlimitedPreview = false;
  bool _unlimitedExecution = false;
  int _previewTimeout = 300;
  int _executionTimeout = 600;
  bool _unlimitedPreviewTimeout = false;
  bool _unlimitedExecutionTimeout = false;

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

    // 定期获取进度
    final progressTimer = Timer.periodic(Duration(seconds: _progressRefreshInterval), (timer) {
      _fetchProgress();
    });

    // 定期获取变更记录
    final changesTimer = Timer.periodic(Duration(seconds: _listRefreshInterval), (timer) {
      if (_autoRefresh) {
        _fetchChanges();
      }
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
    } finally {
      // 等待任务完成后再取消定时器
      // 这里不立即取消，因为任务在后台执行，我们需要继续获取进度
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

    // 定期获取进度
    final progressTimer = Timer.periodic(Duration(seconds: _progressRefreshInterval), (timer) {
      _fetchProgress();
    });

    // 定期获取变更记录
    final changesTimer = Timer.periodic(Duration(seconds: _listRefreshInterval), (timer) {
      if (_autoRefresh) {
        _fetchChanges();
      }
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
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            // Header部分 - 包含操作按钮
            _buildHeader(),
            const SizedBox(height: 20),
            _buildFilterBar(),
            const SizedBox(height: 20),
            _buildProgressSection(),
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

  // Header部分 - 包含预览、运行、中止按钮
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
            ] else ...[
              ElevatedButton.icon(
                key: const ValueKey('analyze_button'),
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
                key: const ValueKey('execute_button'),
                onPressed: _executePipeline,
                icon: const Icon(Icons.play_arrow),
                label: const Text('执行变更'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.green,
                  foregroundColor: Colors.white,
                ),
              ),
              const SizedBox(width: 10),
              ElevatedButton.icon(
                key: const ValueKey('stop_button'),
                onPressed: _stopPipeline,
                icon: const Icon(Icons.stop),
                label: const Text('中止'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  foregroundColor: Colors.white,
                ),
              ),
            ],
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
        child: Column(
          key: const ValueKey('filter_bar_column'),
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              key: const ValueKey('filter_bar_search_row'),
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
                        _currentPage = 1;
                      });
                      _fetchChanges();
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
                      _currentPage = 1;
                    });
                    _fetchChanges();
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
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                ),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              key: const ValueKey('filter_bar_checkbox_row'),
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  mainAxisSize: MainAxisSize.min,
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
                    ),
                    const Text('仅显示变更'),
                  ],
                ),
                const SizedBox(width: 20),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Checkbox(
                      value: _autoRefresh,
                      onChanged: (value) {
                        setState(() {
                          _autoRefresh = value ?? true;
                        });
                      },
                    ),
                    const Text('自动刷新'),
                  ],
                ),
                const SizedBox(width: 20),
                const Text('分页大小:'),
                const SizedBox(width: 10),
                DropdownButton<int>(
                  value: _pageSize,
                  items: const [
                    DropdownMenuItem(value: 10, child: Text('10')),
                    DropdownMenuItem(value: 20, child: Text('20')),
                    DropdownMenuItem(value: 50, child: Text('50')),
                    DropdownMenuItem(value: 100, child: Text('100')),
                    DropdownMenuItem(value: 200, child: Text('200')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _pageSize = value ?? 20;
                      _currentPage = 1;
                    });
                    _fetchChanges();
                  },
                ),
                const SizedBox(width: 20),
                const Text('列表刷新间隔(秒):'),
                const SizedBox(width: 10),
                DropdownButton<int>(
                  value: _listRefreshInterval,
                  items: const [
                    DropdownMenuItem(value: 1, child: Text('1')),
                    DropdownMenuItem(value: 2, child: Text('2')),
                    DropdownMenuItem(value: 5, child: Text('5')),
                    DropdownMenuItem(value: 10, child: Text('10')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _listRefreshInterval = value ?? 2;
                    });
                  },
                ),
                const SizedBox(width: 20),
                const Text('进度刷新间隔(秒):'),
                const SizedBox(width: 10),
                DropdownButton<int>(
                  value: _progressRefreshInterval,
                  items: const [
                    DropdownMenuItem(value: 1, child: Text('1')),
                    DropdownMenuItem(value: 2, child: Text('2')),
                    DropdownMenuItem(value: 5, child: Text('5')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _progressRefreshInterval = value ?? 1;
                    });
                  },
                ),
                const SizedBox(width: 20),
                TextButton.icon(
                  onPressed: () {
                    setState(() {
                      _showAdvancedParams = !_showAdvancedParams;
                    });
                  },
                  icon: Icon(_showAdvancedParams ? Icons.expand_less : Icons.expand_more),
                  label: Text(_showAdvancedParams ? '收起高级参数' : '展开高级参数'),
                ),
              ],
            ),
            if (_showAdvancedParams)
              Padding(
                padding: const EdgeInsets.only(top: 16),
                child: _buildAdvancedParams(),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildAdvancedParams() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '线程池配置',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 10),
          Row(
            key: const ValueKey('thread_pool_mode_row'),
            children: [
              const Text('线程池模式:'),
              const SizedBox(width: 10),
              DropdownButton<String>(
                value: _threadPoolMode,
                items: const [
                  DropdownMenuItem(value: 'GLOBAL', child: Text('全局统一配置')),
                  DropdownMenuItem(value: 'ROOT_PATH', child: Text('根路径独立配置')),
                ],
                onChanged: (value) {
                  setState(() {
                    _threadPoolMode = value ?? 'GLOBAL';
                  });
                },
              ),
              const SizedBox(width: 20),
              const Text('预览线程数:'),
              const SizedBox(width: 10),
              SizedBox(
                width: 80,
                child: TextField(
                  keyboardType: TextInputType.number,
                  controller: TextEditingController(text: '$_previewThreads'),
                  onChanged: (value) {
                    setState(() {
                      _previewThreads = int.tryParse(value) ?? 10;
                    });
                  },
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              const SizedBox(width: 20),
              const Text('执行线程数:'),
              const SizedBox(width: 10),
              SizedBox(
                width: 80,
                child: TextField(
                  keyboardType: TextInputType.number,
                  controller: TextEditingController(text: '$_executionThreads'),
                  onChanged: (value) {
                    setState(() {
                      _executionThreads = int.tryParse(value) ?? 4;
                    });
                  },
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          const Text(
            '数量上限配置',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 10),
          Row(
            key: const ValueKey('preview_limit_row'),
            children: [
              const Text('预览数量上限:'),
              const SizedBox(width: 10),
              SizedBox(
                width: 80,
                child: TextField(
                  keyboardType: TextInputType.number,
                  controller: TextEditingController(text: '$_globalPreviewLimit'),
                  onChanged: (value) {
                    setState(() {
                      _globalPreviewLimit = int.tryParse(value) ?? 100;
                    });
                  },
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Checkbox(
                value: _unlimitedPreview,
                onChanged: (value) {
                  setState(() {
                    _unlimitedPreview = value ?? false;
                  });
                },
              ),
              const Text('无限制'),
              const SizedBox(width: 20),
              const Text('执行数量上限:'),
              const SizedBox(width: 10),
              SizedBox(
                width: 80,
                child: TextField(
                  keyboardType: TextInputType.number,
                  controller: TextEditingController(text: '$_globalExecutionLimit'),
                  onChanged: (value) {
                    setState(() {
                      _globalExecutionLimit = int.tryParse(value) ?? 100;
                    });
                  },
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Checkbox(
                value: _unlimitedExecution,
                onChanged: (value) {
                  setState(() {
                    _unlimitedExecution = value ?? false;
                  });
                },
              ),
              const Text('无限制'),
            ],
          ),
          const SizedBox(height: 20),
          const Text(
            '超时配置',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 14,
            ),
          ),
          const SizedBox(height: 10),
          Row(
            key: const ValueKey('preview_timeout_row'),
            children: [
              const Text('预览超时(秒):'),
              const SizedBox(width: 10),
              SizedBox(
                width: 80,
                child: TextField(
                  keyboardType: TextInputType.number,
                  controller: TextEditingController(text: '$_previewTimeout'),
                  onChanged: (value) {
                    setState(() {
                      _previewTimeout = int.tryParse(value) ?? 300;
                    });
                  },
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Checkbox(
                value: _unlimitedPreviewTimeout,
                onChanged: (value) {
                  setState(() {
                    _unlimitedPreviewTimeout = value ?? false;
                  });
                },
              ),
              const Text('无限制'),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            key: const ValueKey('execution_timeout_row'),
            children: [
              const Text('执行超时(秒):'),
              const SizedBox(width: 10),
              SizedBox(
                width: 80,
                child: TextField(
                  keyboardType: TextInputType.number,
                  controller: TextEditingController(text: '$_executionTimeout'),
                  onChanged: (value) {
                    setState(() {
                      _executionTimeout = int.tryParse(value) ?? 600;
                    });
                  },
                  decoration: const InputDecoration(
                    border: OutlineInputBorder(),
                    contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Checkbox(
                value: _unlimitedExecutionTimeout,
                onChanged: (value) {
                  setState(() {
                    _unlimitedExecutionTimeout = value ?? false;
                  });
                },
              ),
              const Text('无限制'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildStatsBar() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          key: const ValueKey('stats_bar_row'),
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

  // 进度和过滤条件显示区域
  Widget _buildProgressSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 过滤条件显示
            _buildFilterConditions(),
            const SizedBox(height: 16),
            // 进度条和剩余时间
            _buildProgressBar(),
          ],
        ),
      ),
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
        const Text(
          '执行进度',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 14,
            color: Colors.grey,
          ),
        ),
        const SizedBox(height: 8),
        LinearProgressIndicator(
          value: _isAnalyzing ? null : _progress,
          backgroundColor: Colors.grey.shade200,
          valueColor: const AlwaysStoppedAnimation<Color>(Colors.blue),
          minHeight: 10,
          borderRadius: BorderRadius.circular(5),
        ),
        const SizedBox(height: 8),
        Row(
          key: const ValueKey('progress_info_row'),
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              _isAnalyzing ? '分析中...' : _progressStatus,
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade600,
              ),
            ),
            Text(
              '剩余时间: ${_isAnalyzing ? '计算中...' : _remainingTime}',
              style: TextStyle(
                fontSize: 12,
                color: Colors.grey.shade600,
              ),
            ),
          ],
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
