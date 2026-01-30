import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/pipeline_service.dart';
import '../api/source_directory_service.dart';
import '../models/change_record.dart';

class PreviewPage extends ConsumerStatefulWidget {
  const PreviewPage({super.key});

  @override
  ConsumerState<PreviewPage> createState() => _PreviewPageState();
}

class _PreviewPageState extends ConsumerState<PreviewPage> {
  final PipelineService _pipelineService = PipelineService(ApiClient());
  final SourceDirectoryService _sourceDirectoryService = SourceDirectoryService(ApiClient());
  List<ChangeRecord> _changeRecords = [];
  List<String> _sourceDirectories = [];
  List<Map<String, dynamic>> _pipeline = [];
  bool _isLoading = false;
  bool _isAnalyzing = false;
  String _errorMessage = '';

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
      // 加载源目录
      final directories = await _sourceDirectoryService.getSourceDirectories();
      _sourceDirectories = directories.map((d) => d['path'] as String).toList();

      // 加载流水线
      _pipeline = await _pipelineService.getPipeline();
    } catch (e) {
      setState(() {
        _errorMessage = '加载数据失败: $e';
      });
    } finally {
      setState(() {
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

      final changes = await _pipelineService.analyzePipeline(_sourceDirectories, _pipeline);
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

      final result = await _pipelineService.executePipeline(_sourceDirectories, _pipeline);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('执行任务已创建，任务ID: ${result['taskId']}')),
      );
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('预览分析'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            Navigator.pop(context);
          },
        ),
      ),
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            Card(
              elevation: 4,
              margin: const EdgeInsets.only(bottom: 20),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    const Text(
                      '分析配置',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('源目录数量:', style: TextStyle(fontWeight: FontWeight.bold)),
                              Text('${_sourceDirectories.length} 个目录'),
                            ],
                          ),
                        ),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('插件数量:', style: TextStyle(fontWeight: FontWeight.bold)),
                              Text('${_pipeline.length} 个插件'),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        ElevatedButton(
                          onPressed: _isAnalyzing ? null : _analyzePipeline,
                          child: _isAnalyzing
                              ? const SizedBox(
                                  height: 20,
                                  width: 20,
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                )
                              : const Text('分析变更'),
                        ),
                        const SizedBox(width: 16),
                        ElevatedButton(
                          onPressed: _executePipeline,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.green,
                          ),
                          child: const Text('执行变更'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            if (_errorMessage.isNotEmpty)
              Container(
                padding: const EdgeInsets.all(10),
                color: Colors.red[100],
                child: Text(
                  _errorMessage,
                  style: const TextStyle(color: Colors.red),
                ),
              ),
            const SizedBox(height: 20),
            const Text(
              '变更预览',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              )
            else if (_changeRecords.isEmpty)
              const Center(
                child: Text('暂无变更记录，请点击上方「分析变更」按钮'),
              )
            else
              Expanded(
                child: SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: DataTable(
                    columns: const [
                      DataColumn(label: Text('原始文件名')),
                      DataColumn(label: Text('新文件名')),
                      DataColumn(label: Text('状态')),
                    ],
                    rows: _changeRecords.map((record) {
                      return DataRow(cells: [
                        DataCell(Text(record.originalName)),
                        DataCell(Text(record.newName)),
                        DataCell(Text(record.status)),
                      ]);
                    }).toList(),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
