import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:filemanager_flutter/api/api_client.dart';
import 'package:filemanager_flutter/api/strategy_service.dart';
import 'package:filemanager_flutter/models/strategy_info.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/utils/tooltip_utils.dart';

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
      _strategyConfig = null;
    });

    try {
      print('Loading strategy info for: $strategyId');
      final strategy = await _strategyService.getStrategyInfo(strategyId);
      print('Strategy info loaded: ${strategy.name}');
      
      print('Loading strategy config for: $strategyId');
      final config = await _strategyService.getStrategyConfig(strategyId);
      print('Strategy config loaded: ${config.configValues}');
      
      setState(() {
        _selectedStrategy = strategy;
              _strategyConfig = config;
            });
    } catch (e, stackTrace) {
      print('Error loading strategy config: $e');
      print('Stack trace: $stackTrace');
      setState(() {
        _errorMessage = '加载策略配置失败: $e';
        _strategyConfig = null;
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

                    if (_isLoading && _strategyConfig == null)
                      const Center(
                        child: CircularProgressIndicator(),
                      )
                    else if (_strategyConfig != null && _selectedStrategy != null)
                      Expanded(
                        child: ListView.builder(
                          itemCount: _selectedStrategy!.configFields.length,
                          itemBuilder: (context, index) {
                            try {
                              final ConfigField field = _selectedStrategy!.configFields[index];
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
      // 安全获取字段值，即使_strategyConfig为null
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
      // 友好处理错误，显示警告而不是崩溃
      print('字段 ${field.name} 加载失败: $e');
      return Card(
        color: Colors.yellow.shade50,
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Row(
            children: [
              const Icon(Icons.warning, color: Colors.orange),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '字段 ${field.label} 加载异常',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.orange),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      '该字段将使用默认值或保持为空',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
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
    try {
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
                  text: value?.toString() ?? field.defaultValue?.toString() ?? '',
                ),
                onChanged: (v) {
                  if (_strategyConfig != null) {
                    _strategyConfig?.setValue(field.name, v);
                  }
                },
              ),
            ),
          ],
        ),
      );
    } catch (e) {
      print('构建文本字段 ${field.name} 失败: $e');
      return _buildErrorField(field, e);
    }
  }

  Widget _buildNumberField(ConfigField field, dynamic value) {
    try {
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
                  text: value?.toString() ?? field.defaultValue?.toString() ?? '',
                ),
                keyboardType: TextInputType.number,
                onChanged: (v) {
                  if (_strategyConfig != null) {
                    _strategyConfig?.setValue(field.name, int.tryParse(v));
                  }
                },
              ),
            ),
          ],
        ),
      );
    } catch (e) {
      print('构建数字字段 ${field.name} 失败: $e');
      return _buildErrorField(field, e);
    }
  }

  Widget _buildBooleanField(ConfigField field, dynamic value) {
    try {
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
                value: value ?? field.defaultValue ?? false,
                onChanged: (v) {
                  if (_strategyConfig != null) {
                    _strategyConfig?.setValue(field.name, v);
                  }
                },
              ),
            ),
          ],
        ),
      );
    } catch (e) {
      print('构建布尔字段 ${field.name} 失败: $e');
      return _buildErrorField(field, e);
    }
  }

  Widget _buildSelectField(ConfigField field, dynamic value) {
    try {
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
                initialValue: value?.toString() ?? field.defaultValue?.toString(),
                items: field.options?.map((option) {
                  return DropdownMenuItem<String>(
                    value: option,
                    child: Text(option),
                  );
                }).toList() ?? [],
                onChanged: (v) {
                  if (_strategyConfig != null) {
                    _strategyConfig?.setValue(field.name, v);
                  }
                },
              ),
            ),
          ],
        ),
      );
    } catch (e) {
      print('构建选择字段 ${field.name} 失败: $e');
      return _buildErrorField(field, e);
    }
  }

  Widget _buildDirectoryField(ConfigField field, dynamic value) {
    try {
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
                  text: value?.toString() ?? field.defaultValue?.toString() ?? '',
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
    } catch (e) {
      print('构建目录字段 ${field.name} 失败: $e');
      return _buildErrorField(field, e);
    }
  }

  Widget _buildListField(ConfigField field, dynamic value) {
    try {
      List<String> listValue = <String>[];
      if (value != null) {
        if (value is List) {
          try {
            listValue = List<String>.from(value);
          } catch (e) {
            listValue = value.map((item) => item?.toString() ?? '').toList();
          }
        } else {
          listValue = [value.toString()];
        }
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
                          if (_strategyConfig != null) {
                            final newValue = List<String>.from(listValue);
                            newValue.removeAt(index);
                            _strategyConfig?.setValue(field.name, newValue);
                          }
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
                        if (v.isNotEmpty && _strategyConfig != null) {
                          final newValue = List<String>.from(listValue);
                          newValue.add(v);
                          _strategyConfig?.setValue(field.name, newValue);
                        }
                      },
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    if (_strategyConfig != null) {
                      final newValue = List<String>.from(listValue);
                      newValue.add('新项');
                      _strategyConfig?.setValue(field.name, newValue);
                    }
                  },
                  child: const Text('添加'),
                ),
              ],
            ),
          ],
        ),
      );
    } catch (e) {
      print('构建列表字段 ${field.name} 失败: $e');
      return _buildErrorField(field, e);
    }
  }

  // 辅助方法：构建错误提示卡片
  Widget _buildErrorField(ConfigField field, dynamic error) {
    return Card(
      color: Colors.yellow.shade50,
      child: Padding(
        padding: const EdgeInsets.all(12.0),
        child: Row(
          children: [
            const Icon(Icons.warning, color: Colors.orange),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '字段 ${field.label} 加载异常',
                    style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.orange),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    '该字段将使用默认值或保持为空',
                    style: TextStyle(fontSize: 12, color: Colors.grey),
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
