import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/pages/home_page.dart';

class PipelineConfigPage extends ConsumerStatefulWidget {
  const PipelineConfigPage({super.key});

  @override
  ConsumerState<PipelineConfigPage> createState() => _PipelineConfigPageState();
}

class _PipelineConfigPageState extends ConsumerState<PipelineConfigPage> {
  List<Map<String, dynamic>> _pipeline = [];
  List<Map<String, dynamic>> _availableStrategies = [
    {'id': 'rename', 'name': '重命名策略'},
    {'id': 'move', 'name': '移动策略'},
    {'id': 'copy', 'name': '复制策略'},
    {'id': 'delete', 'name': '删除策略'},
    {'id': 'metadata', 'name': '元数据策略'},
  ];
  bool _isLoading = false;
  String _errorMessage = '';

  @override
  void initState() {
    super.initState();
    _loadPipeline();
  }

  Future<void> _loadPipeline() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final pipelineService = ref.read(pipelineServiceProvider);
      final pipeline = await pipelineService.getPipeline();
      setState(() {
        _pipeline = pipeline;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载流水线失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _updatePipeline() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final pipelineService = ref.read(pipelineServiceProvider);
      await pipelineService.updatePipeline(_pipeline);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('流水线更新成功')),
      );
    } catch (e) {
      setState(() {
        _errorMessage = '更新流水线失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  void _addStrategy(String strategyId) {
    setState(() {
      final strategyName = _availableStrategies
          .firstWhere((s) => s['id'] == strategyId, orElse: () => {'name': '未知策略'})
          ['name'];
      _pipeline.add({
        'strategyId': strategyId,
        'name': strategyName,
        'config': {},
      });
    });
  }

  void _removeStrategy(int index) {
    setState(() {
      _pipeline.removeAt(index);
    });
  }

  void _moveStrategyUp(int index) {
    if (index > 0) {
      setState(() {
        final temp = _pipeline[index];
        _pipeline[index] = _pipeline[index - 1];
        _pipeline[index - 1] = temp;
      });
    }
  }

  void _moveStrategyDown(int index) {
    if (index < _pipeline.length - 1) {
      setState(() {
        final temp = _pipeline[index];
        _pipeline[index] = _pipeline[index + 1];
        _pipeline[index + 1] = temp;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('策略流水线配置'),
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
                      '可用策略',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 10,
                      runSpacing: 10,
                      children: _availableStrategies.map((strategy) {
                        return ElevatedButton(
                          onPressed: () => _addStrategy(strategy['id'] as String),
                          child: Text(strategy['name'] as String),
                        );
                      }).toList(),
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
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  '策略流水线',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                ElevatedButton(
                  onPressed: _updatePipeline,
                  child: const Text('保存流水线'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              )
            else if (_pipeline.isEmpty)
              const Center(
                child: Text('暂无策略，从上方添加'),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: _pipeline.length,
                  itemBuilder: (context, index) {
                    final strategy = _pipeline[index];
                    return Card(
                      elevation: 2,
                      margin: const EdgeInsets.only(bottom: 10),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text(
                                  strategy['name'] as String,
                                  style: const TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete, color: Colors.red),
                                  onPressed: () => _removeStrategy(index),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text('策略ID: ${strategy['strategyId']}'),
                            const SizedBox(height: 16),
                            const Text('配置:', style: TextStyle(fontWeight: FontWeight.bold)),
                            const SizedBox(height: 8),
                            // 这里可以添加配置表单，根据策略类型动态生成
                            const TextField(
                              decoration: InputDecoration(
                                labelText: '配置项',
                                border: OutlineInputBorder(),
                              ),
                            ),
                            const SizedBox(height: 16),
                            Row(
                              children: [
                                IconButton(
                                  icon: const Icon(Icons.arrow_upward),
                                  onPressed: index > 0 ? () => _moveStrategyUp(index) : null,
                                  disabledColor: Colors.grey,
                                ),
                                IconButton(
                                  icon: const Icon(Icons.arrow_downward),
                                  onPressed: index < _pipeline.length - 1 ? () => _moveStrategyDown(index) : null,
                                  disabledColor: Colors.grey,
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }
}
