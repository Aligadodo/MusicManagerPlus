import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/pages/home_page.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';

class StrategyConfigPage extends ConsumerStatefulWidget {
  const StrategyConfigPage({super.key});

  @override
  ConsumerState<StrategyConfigPage> createState() => _StrategyConfigPageState();
}

class _StrategyConfigPageState extends ConsumerState<StrategyConfigPage> {
  List<StrategyInfo> _strategies = [];
  bool _isLoading = false;
  String _errorMessage = '';
  StrategyInfo? _selectedStrategy;
  StrategyConfig? _strategyConfig;

  @override
  void initState() {
    super.initState();
    _loadStrategies();
  }

  Future<void> _loadStrategies() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final strategyService = ref.read(strategyServiceProvider);
      final strategies = await strategyService.getAvailableStrategies();
      setState(() {
        _strategies = strategies;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load strategies: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _loadStrategyConfig(String strategyId) async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final strategyService = ref.read(strategyServiceProvider);
      final strategy = await strategyService.getStrategyInfo(strategyId);
      final config = await strategyService.getStrategyConfig(strategyId);
      setState(() {
        _selectedStrategy = strategy;
        _strategyConfig = config;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to load strategy config: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _saveStrategyConfig() async {
    if (_selectedStrategy == null || _strategyConfig == null) return;

    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final strategyService = ref.read(strategyServiceProvider);
      await strategyService.updateStrategyConfig(
        _selectedStrategy!.id,
        _strategyConfig!,
      );
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('配置保存成功')),
      );
    } catch (e) {
      setState(() {
        _errorMessage = 'Failed to save strategy config: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('策略配置'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            Navigator.pop(context);
          },
        ),
      ),
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Row(
          children: [
            // 策略列表
            Container(
              width: 300,
              padding: const EdgeInsets.only(right: 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children:
                  [
                    const Text(
                      '可用策略:',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 20),
                    if (_isLoading && _strategies.isEmpty)
                      const Center(
                        child: CircularProgressIndicator(),
                      )
                    else
                      Expanded(
                        child: ListView.builder(
                          itemCount: _strategies.length,
                          itemBuilder: (context, index) {
                            final strategy = _strategies[index];
                            return Card(
                              elevation: 2,
                              margin: const EdgeInsets.symmetric(vertical: 5),
                              child: ListTile(
                                title: Text(strategy.name),
                                subtitle: Text(strategy.description),
                                onTap: () {
                                  _loadStrategyConfig(strategy.id);
                                },
                                selected: _selectedStrategy?.id == strategy.id,
                              ),
                            );
                          },
                        ),
                      ),
                  ],
              ),
            ),

            // 配置区域
            Expanded(
              child: Container(
                padding: const EdgeInsets.only(left: 20),
                decoration: const BoxDecoration(
                  border: Border(
                    left: BorderSide(color: Colors.grey, width: 1),
                  ),
                ),
                child:
                  _selectedStrategy == null
                      ? const Center(
                          child: Text('请选择一个策略进行配置'),
                        )
                      : Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children:
                            [
                              Text(
                                _selectedStrategy!.name,
                                style: const TextStyle(
                                  fontSize: 20,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              Text(
                                _selectedStrategy!.description,
                                style: const TextStyle(
                                  color: Colors.grey,
                                ),
                              ),
                              const SizedBox(height: 30),

                              if (_errorMessage.isNotEmpty)
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  color: Colors.red[100],
                                  child: Text(
                                    _errorMessage,
                                    style: const TextStyle(color: Colors.red),
                                  ),
                                ),

                              if (_isLoading && _strategyConfig != null)
                                const Center(
                                  child: CircularProgressIndicator(),
                                )
                              else if (_strategyConfig != null)
                                Expanded(
                                  child:
                                    ListView.builder(
                                      itemCount: _selectedStrategy!.configFields.length,
                                      itemBuilder: (context, index) {
                                        final field = _selectedStrategy!.configFields[index];
                                        return _buildConfigField(field);
                                      },
                                    ),
                                ),

                              const SizedBox(height: 20),
                              Align(
                                alignment: Alignment.bottomRight,
                                child:
                                  ElevatedButton(
                                    onPressed: _saveStrategyConfig,
                                    child: const Text('保存配置'),
                                  ),
                              ),
                            ],
                        ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConfigField(dynamic field) {
    // 这里简化实现，实际应该根据字段类型构建不同的UI组件
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child:
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children:
            [
              Text(
                field.label,
                style: const TextStyle(
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 5),
              TextField(
                decoration: InputDecoration(
                  border: const OutlineInputBorder(),
                  hintText: field.description,
                ),
                controller: TextEditingController(
                  text: _strategyConfig?.getValue(field.name)?.toString() ?? '',
                ),
                onChanged: (value) {
                  _strategyConfig?.setValue(field.name, value);
                },
              ),
            ],
        ),
    );
  }
}
