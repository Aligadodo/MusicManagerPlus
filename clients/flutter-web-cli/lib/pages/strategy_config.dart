import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/strategy_service.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/config_field.dart';

class StrategyConfigPage extends ConsumerStatefulWidget {
  const StrategyConfigPage({super.key});

  @override
  ConsumerState<StrategyConfigPage> createState() => _StrategyConfigPageState();
}

class _StrategyConfigPageState extends ConsumerState<StrategyConfigPage> {
  late ApiClient _apiClient;
  late StrategyService _strategyService;
  List<StrategyInfo> _strategies = [];
  bool _isLoading = false;
  String _errorMessage = '';
  StrategyInfo? _selectedStrategy;
  StrategyConfig? _strategyConfig;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient();
    _strategyService = StrategyService(_apiClient);
    _loadStrategies();
  }

  Future<void> _loadStrategies() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final strategies = await _strategyService.getAvailableStrategies();
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
      final strategy = await _strategyService.getStrategyInfo(strategyId);
      final config = await _strategyService.getStrategyConfig(strategyId);
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

  @override
  Widget build(BuildContext context) {
    try {
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
              _buildStrategyList(),
              _buildConfigArea(),
            ],
          ),
        ),
      );
    } catch (e) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('策略配置'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 48, color: Colors.red),
              const SizedBox(height: 16),
              Text(
                '页面加载失败: $e',
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _loadStrategies,
                child: const Text('重新加载'),
              ),
            ],
          ),
        ),
      );
    }
  }

  Widget _buildStrategyList() {
    try {
      return Container(
        width: 300,
        padding: const EdgeInsets.only(right: 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
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
                    try {
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
                    } catch (e) {
                      return Card(
                        color: Colors.red.shade50,
                        child: Padding(
                          padding: const EdgeInsets.all(8.0),
                          child: Text(
                            '策略加载失败: $e',
                            style: const TextStyle(color: Colors.red),
                          ),
                        ),
                      );
                    }
                  },
                ),
              ),
          ],
        ),
      );
    } catch (e) {
      return Container(
        width: 300,
        padding: const EdgeInsets.only(right: 20),
        child: Card(
          color: Colors.red.shade50,
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              children: [
                const Icon(Icons.error_outline, color: Colors.red),
                const SizedBox(height: 8),
                Text(
                  '策略列表加载失败: $e',
                  style: const TextStyle(color: Colors.red),
                ),
              ],
            ),
          ),
        ),
      );
    }
  }

  Widget _buildConfigArea() {
    try {
      return Expanded(
        child: Container(
          padding: const EdgeInsets.only(left: 20),
          decoration: const BoxDecoration(
            border: Border(
              left: BorderSide(color: Colors.grey, width: 1),
            ),
          ),
          child: _selectedStrategy == null
              ? const Center(
                  child: Text('请选择一个策略进行配置'),
                )
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
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
                        child: ListView.builder(
                          itemCount: _selectedStrategy!.configFields.length,
                          itemBuilder: (context, index) {
                            try {
                              final field = _selectedStrategy!.configFields[index];
                              return _buildConfigField(field);
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
                          },
                        ),
                      ),

                    const SizedBox(height: 20),
                  ],
                ),
        ),
      );
    } catch (e) {
      return Expanded(
        child: Container(
          padding: const EdgeInsets.only(left: 20),
          decoration: const BoxDecoration(
            border: Border(
              left: BorderSide(color: Colors.grey, width: 1),
            ),
          ),
          child: Center(
            child: Card(
              color: Colors.red.shade50,
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, color: Colors.red),
                    const SizedBox(height: 8),
                    Text(
                      '配置区域加载失败: $e',
                      style: const TextStyle(color: Colors.red),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      );
    }
  }

  Widget _buildConfigField(ConfigField field) {
    try {
      final value = _strategyConfig?.getValue(field.name);

      switch (field.type) {
        case 'text':
          return _buildTextField(field, value);
        case 'number':
          return _buildNumberField(field, value);
        case 'boolean':
          return _buildBooleanField(field, value);
        case 'select':
          return _buildSelectField(field, value);
        case 'directory':
          return _buildDirectoryField(field, value);
        case 'list':
          return _buildListField(field, value);
        default:
          return _buildTextField(field, value);
      }
    } catch (e) {
      return Card(
        color: Colors.red.shade50,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.red),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '字段 ${field.name} 加载失败',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red),
                    ),
                    Text(
                      '错误: $e',
                      style: const TextStyle(fontSize: 12, color: Colors.red),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
  }

  Widget _buildTextField(ConfigField field, dynamic value) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            field.label,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 5),
          TextField(
            decoration: InputDecoration(
              border: const OutlineInputBorder(),
              hintText: field.description,
            ),
            controller: TextEditingController(
              text: value?.toString() ?? field.defaultValue?.toString() ?? '',
            ),
            onChanged: (v) {
              _strategyConfig?.setValue(field.name, v);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildNumberField(ConfigField field, dynamic value) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            field.label,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 5),
          TextField(
            decoration: InputDecoration(
              border: const OutlineInputBorder(),
              hintText: field.description,
            ),
            controller: TextEditingController(
              text: value?.toString() ?? field.defaultValue?.toString() ?? '',
            ),
            keyboardType: TextInputType.number,
            onChanged: (v) {
              _strategyConfig?.setValue(field.name, int.tryParse(v));
            },
          ),
        ],
      ),
    );
  }

  Widget _buildBooleanField(ConfigField field, dynamic value) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  field.label,
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                Text(
                  field.description,
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
          Checkbox(
            value: value ?? field.defaultValue ?? false,
            onChanged: (v) {
              _strategyConfig?.setValue(field.name, v);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildSelectField(ConfigField field, dynamic value) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            field.label,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 5),
          DropdownButtonFormField<String>(
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
            ),
            value: value?.toString() ?? field.defaultValue?.toString(),
            items: field.options?.map((option) {
              return DropdownMenuItem<String>(
                value: option,
                child: Text(option),
              );
            }).toList() ?? [],
            onChanged: (v) {
              _strategyConfig?.setValue(field.name, v);
            },
          ),
        ],
      ),
    );
  }

  Widget _buildDirectoryField(ConfigField field, dynamic value) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            field.label,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 5),
          TextField(
            decoration: InputDecoration(
              border: const OutlineInputBorder(),
              hintText: field.description,
              suffixIcon: const Icon(Icons.folder),
            ),
            controller: TextEditingController(
              text: value?.toString() ?? field.defaultValue?.toString() ?? '',
            ),
            readOnly: true,
            onTap: () async {
              // TODO: 实现目录选择器
            },
          ),
        ],
      ),
    );
  }

  Widget _buildListField(ConfigField field, dynamic value) {
    try {
      final listValue = value is List ? List<String>.from(value) : <String>[];
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 5),
            Container(
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
                        final newValue = List<String>.from(listValue);
                        newValue.removeAt(index);
                        _strategyConfig?.setValue(field.name, newValue);
                      },
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 5),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: const InputDecoration(
                      border: OutlineInputBorder(),
                      hintText: '输入新项...',
                    ),
                    onSubmitted: (v) {
                      if (v.isNotEmpty) {
                        final newValue = List<String>.from(listValue);
                        newValue.add(v);
                        _strategyConfig?.setValue(field.name, newValue);
                      }
                    },
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    final newValue = List<String>.from(listValue);
                    newValue.add('新项');
                    _strategyConfig?.setValue(field.name, newValue);
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
          child: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.red),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '列表字段 ${field.name} 加载失败',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red),
                    ),
                    Text(
                      '错误: $e',
                      style: const TextStyle(fontSize: 12, color: Colors.red),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
  }
}
