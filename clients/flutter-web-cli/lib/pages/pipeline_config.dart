import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/plugin_service.dart';
import '../api/strategy_service.dart';
import '../api/pipeline_service.dart';
import '../models/plugin_info.dart';
import '../models/strategy_info.dart';
import '../models/config_field.dart';
import '../utils/tooltip_utils.dart';

class PipelineConfigPage extends ConsumerStatefulWidget {
  const PipelineConfigPage({super.key});

  @override
  ConsumerState<PipelineConfigPage> createState() => _PipelineConfigPageState();
}

class _PipelineConfigPageState extends ConsumerState<PipelineConfigPage> {
  final PluginService _pluginService = PluginService(ApiClient());
  final StrategyService _strategyService = StrategyService(ApiClient());
  final PipelineService _pipelineService = PipelineService(ApiClient());
  List<StrategyInfo> _pipeline = [];
  List<PluginInfo> _availablePlugins = [];
  List<StrategyInfo> _availableStrategies = [];
  bool _isLoading = false;
  String _errorMessage = '';

  @override
  void initState() {
    super.initState();
    _loadPipeline();
    _loadAvailablePlugins();
    _loadAvailableStrategies();
  }

  Future<void> _loadAvailableStrategies() async {
    try {
      final strategies = await _strategyService.getAvailableStrategies();
      setState(() {
        _availableStrategies = strategies;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载策略失败: $e';
      });
    }
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
      _pipeline.add(StrategyInfo(
        id: plugin.id,
        name: plugin.name,
        description: plugin.description,
        configFields: [],
        enabled: true,
      ));
    });
  }

  void _addStrategy(StrategyInfo strategy) {
    setState(() {
      // 创建策略的副本，确保包含完整的配置字段
      _pipeline.add(StrategyInfo(
        id: strategy.id,
        name: strategy.name,
        description: strategy.description,
        configFields: strategy.configFields ?? [],
        enabled: true,
      ));
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

  Widget _buildConfigField(ConfigField field) {
    try {
      switch (field.type) {
        case 'text':
          return _buildTextField(field);
        case 'number':
          return _buildNumberField(field);
        case 'boolean':
          return _buildBooleanField(field);
        case 'select':
          return _buildSelectField(field);
        case 'directory':
          return _buildDirectoryField(field);
        case 'list':
          return _buildListField(field);
        default:
          return _buildTextField(field);
      }
    } catch (e) {
      return Card(
        color: Colors.red.shade50,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Text(
            '字段加载失败: $e',
            style: const TextStyle(color: Colors.red),
          ),
        ),
      );
    }
  }

  Widget _buildTextField(ConfigField field) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: TextField(
              decoration: InputDecoration(
                border: const OutlineInputBorder(),
                hintText: field.description,
              ),
              controller: TextEditingController(
                text: field.defaultValue?.toString() ?? '',
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNumberField(ConfigField field) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: TextField(
              decoration: InputDecoration(
                border: const OutlineInputBorder(),
                hintText: field.description,
              ),
              controller: TextEditingController(
                text: field.defaultValue?.toString() ?? '',
              ),
              keyboardType: TextInputType.number,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBooleanField(ConfigField field) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Tooltip(
                  message: field.description ?? '',
                  child: Text(
                    field.label,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
                Text(
                  field.description ?? '',
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
          Tooltip(
            message: field.description ?? '',
            child: Checkbox(
              value: field.defaultValue ?? false,
              onChanged: (v) {
                // TODO: 保存配置值
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSelectField(ConfigField field) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: DropdownButtonFormField<String>(
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
              ),
              initialValue: field.defaultValue?.toString(),
              items: field.options?.map((option) {
                return DropdownMenuItem<String>(
                  value: option,
                  child: Text(option),
                );
              }).toList() ?? [],
              onChanged: (v) {
                // TODO: 保存配置值
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDirectoryField(ConfigField field) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: TextField(
              decoration: InputDecoration(
                border: const OutlineInputBorder(),
                hintText: field.description,
                suffixIcon: const Icon(Icons.folder),
              ),
              controller: TextEditingController(
                text: field.defaultValue?.toString() ?? '',
              ),
              readOnly: true,
              onTap: () async {
                // TODO: 实现目录选择器
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildListField(ConfigField field) {
    try {
      List<String> listValue = <String>[];
      if (field.defaultValue is List) {
        try {
          listValue = List<String>.from(field.defaultValue);
        } catch (e) {
          listValue = (field.defaultValue as List).map((item) => item?.toString() ?? '').toList();
        }
      } else if (field.defaultValue != null) {
        listValue = [field.defaultValue.toString()];
      }
      
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Tooltip(
              message: field.description ?? '',
              child: Text(
                field.label,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
            const SizedBox(height: 5),
            Tooltip(
              message: field.description ?? '',
              child: Container(
                height: 100,
                decoration: BoxDecoration(
                  border: Border.all(color: Colors.grey),
                  borderRadius: BorderRadius.circular(4),
                ),
                child: ListView.builder(
                  itemCount: listValue.length,
                  itemBuilder: (context, index) {
                    return ListTile(
                      title: Text(listValue[index]),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete, color: Colors.red),
                        onPressed: () {
                          // TODO: 移除列表项
                        },
                      ),
                    );
                  },
                ),
              ),
            ),
            const SizedBox(height: 5),
            Row(
              children: [
                Expanded(
                  child: Tooltip(
                    message: field.description ?? '',
                    child: TextField(
                      decoration: const InputDecoration(
                        border: OutlineInputBorder(),
                        hintText: '输入新项...',
                      ),
                      onSubmitted: (v) {
                        if (v.isNotEmpty) {
                          // TODO: 添加新列表项
                        }
                      },
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    // TODO: 添加新列表项
                  },
                  child: const Text('添加'),
                ),
              ],
            ),
          ],
        ),
      );
    } catch (e) {
      return Card(
        color: Colors.red.shade50,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Text(
            '列表字段加载失败: $e',
            style: const TextStyle(color: Colors.red),
          ),
        ),
      );
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
                    if (_availableStrategies.isEmpty)
                      const Text('加载策略中...')
                    else
                      Wrap(
                        spacing: 10,
                        runSpacing: 10,
                        children: _availableStrategies.map((strategy) {
                          return ElevatedButton(
                            onPressed: () => _addStrategy(strategy),
                            child: Text(strategy.name),
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
                                  plugin.name,
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
                            Text('ID: ${plugin.id}'),
                            const SizedBox(height: 16),
                            const Text('配置:', style: TextStyle(fontWeight: FontWeight.bold)),
                            const SizedBox(height: 8),
                            if (plugin.configFields.isNotEmpty)
                              ...plugin.configFields.map((field) {
                                return _buildConfigField(field);
                              })
                            else
                              const Text('此插件/策略暂无配置项'),
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
