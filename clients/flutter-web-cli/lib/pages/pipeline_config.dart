import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/plugin_service.dart';
import '../api/pipeline_service.dart';
import '../models/plugin_info.dart';

class PipelineConfigPage extends ConsumerStatefulWidget {
  const PipelineConfigPage({super.key});

  @override
  ConsumerState<PipelineConfigPage> createState() => _PipelineConfigPageState();
}

class _PipelineConfigPageState extends ConsumerState<PipelineConfigPage> {
  final PluginService _pluginService = PluginService(ApiClient());
  final PipelineService _pipelineService = PipelineService(ApiClient());
  List<Map<String, dynamic>> _pipeline = [];
  List<PluginInfo> _availablePlugins = [];
  bool _isLoading = false;
  String _errorMessage = '';

  @override
  void initState() {
    super.initState();
    _loadPipeline();
    _loadAvailablePlugins();
  }

  Future<void> _loadPipeline() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final pipeline = await _pipelineService.getPipeline();
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

  Future<void> _loadAvailablePlugins() async {
    try {
      final plugins = await _pluginService.getPlugins();
      setState(() {
        _availablePlugins = plugins;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载插件失败: $e';
      });
    }
  }

  Future<void> _updatePipeline() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      await _pipelineService.updatePipeline(_pipeline);
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

  void _addPlugin(PluginInfo plugin) {
    setState(() {
      _pipeline.add({
        'pluginId': plugin.id,
        'name': plugin.name,
        'config': {},
      });
    });
  }

  void _removePlugin(int index) {
    setState(() {
      _pipeline.removeAt(index);
    });
  }

  void _movePluginUp(int index) {
    if (index > 0) {
      setState(() {
        final temp = _pipeline[index];
        _pipeline[index] = _pipeline[index - 1];
        _pipeline[index - 1] = temp;
      });
    }
  }

  void _movePluginDown(int index) {
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
        title: const Text('插件流水线配置'),
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
                      '可用插件',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    if (_availablePlugins.isEmpty)
                      const Text('加载插件中...')
                    else
                      Wrap(
                        spacing: 10,
                        runSpacing: 10,
                        children: _availablePlugins.map((plugin) {
                          return ElevatedButton(
                            onPressed: () => _addPlugin(plugin),
                            child: Text(plugin.name),
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
                  '插件流水线',
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
                child: Text('暂无插件，从上方添加'),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: _pipeline.length,
                  itemBuilder: (context, index) {
                    final plugin = _pipeline[index];
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
                                  plugin['name'] as String,
                                  style: const TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete, color: Colors.red),
                                  onPressed: () => _removePlugin(index),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Text('插件ID: ${plugin['pluginId']}'),
                            const SizedBox(height: 16),
                            const Text('配置:', style: TextStyle(fontWeight: FontWeight.bold)),
                            const SizedBox(height: 8),
                            // 这里可以添加配置表单，根据插件类型动态生成
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
                                  onPressed: index > 0 ? () => _movePluginUp(index) : null,
                                  disabledColor: Colors.grey,
                                ),
                                IconButton(
                                  icon: const Icon(Icons.arrow_downward),
                                  onPressed: index < _pipeline.length - 1 ? () => _movePluginDown(index) : null,
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
