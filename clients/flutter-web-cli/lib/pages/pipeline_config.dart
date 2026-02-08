import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/plugin_service.dart';
import '../api/strategy_service.dart';
import '../api/pipeline_service.dart';
import '../models/plugin_info.dart';
import '../models/strategy_info.dart';
import '../models/config_field.dart';
import '../widgets/strategy_config_card.dart';
import '../widgets/selectable_text_widget.dart';

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
        preconditionGroups: [],
        enabled: true,
      ));
    });
    // 添加后立即保存
    _updatePipeline();
  }

  void _addStrategy(StrategyInfo strategy) {
    setState(() {
      // 创建策略的深拷贝，确保包含完整的配置字段
      final copiedConfigFields = strategy.configFields?.map((field) => ConfigField(
        name: field.name,
        label: field.label,
        type: field.type,
        defaultValue: field.defaultValue,
        description: field.description,
        required: field.required,
        dependsOn: field.dependsOn,
        dependsValue: field.dependsValue,
        options: field.options,
        enumOptions: field.enumOptions,
        subFields: field.subFields,
        isModule: field.isModule,
        moduleType: field.moduleType,
      )).toList() ?? [];
      
      // 创建空的前置条件面板，而不是复制现有配置
      final emptyPreconditionGroups = <PreconditionGroup>[];
      
      _pipeline.add(StrategyInfo(
        id: strategy.id,
        name: strategy.name,
        description: strategy.description,
        configFields: copiedConfigFields,
        preconditionGroups: emptyPreconditionGroups,
        enabled: true,
      ));
    });
    // 添加后立即保存
    _updatePipeline();
  }

  void _removePlugin(int index) {
    setState(() {
      _pipeline.removeAt(index);
    });
    // 删除后立即保存
    _updatePipeline();
  }

  void _movePluginUp(int index) {
    if (index > 0) {
      setState(() {
        final temp = _pipeline[index];
        _pipeline[index] = _pipeline[index - 1];
        _pipeline[index - 1] = temp;
      });
      // 移动后立即保存
      _updatePipeline();
    }
  }

  void _movePluginDown(int index) {
    if (index < _pipeline.length - 1) {
      setState(() {
        final temp = _pipeline[index];
        _pipeline[index] = _pipeline[index + 1];
        _pipeline[index + 1] = temp;
      });
      // 移动后立即保存
      _updatePipeline();
    }
  }

  void _handleStrategyChanged(int index, StrategyInfo updatedStrategy) {
    setState(() {
      _pipeline[index] = updatedStrategy;
    });
    // 策略配置变化后立即保存
    _updatePipeline();
  }

  Future<void> _resetPipeline() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认重置'),
        content: const Text('确定要重置流水线配置吗？这将清空所有策略和配置。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('确定'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      try {
        print('开始重置流水线配置...');
        final result = await _pipelineService.resetPipeline();
        print('重置流水线配置成功: $result');
        
        // 重置后重新加载流水线配置，确保界面正确更新
        await _loadPipeline();
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('流水线重置成功')),
          );
        }
      } catch (e) {
        print('重置流水线配置失败: $e');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('重置失败: $e')),
          );
        }
      }
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
                child: SelectableTextWidget(
                  text: _errorMessage,
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
                Row(
                  children: [
                    ElevatedButton(
                      onPressed: _resetPipeline,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.red.shade600,
                        foregroundColor: Colors.white,
                      ),
                      child: const Text('重置配置'),
                    ),
                    const SizedBox(width: 10),
                    ElevatedButton(
                      onPressed: _updatePipeline,
                      child: const Text('保存流水线'),
                    ),
                  ],
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
                    final strategy = _pipeline[index];
                    return StrategyConfigCard(
                      key: ValueKey('pipeline_strategy_${strategy.id}_${strategy.hashCode}'),
                      strategy: strategy,
                      index: index,
                      onDelete: () => _removePlugin(index),
                      onMoveUp: () => _movePluginUp(index),
                      onMoveDown: () => _movePluginDown(index),
                      onStrategyChanged: (updatedStrategy) => _handleStrategyChanged(index, updatedStrategy),
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