import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';

class ConfigPage extends ConsumerStatefulWidget {
  const ConfigPage({super.key});

  @override
  ConsumerState<ConfigPage> createState() => _ConfigPageState();
}

class _ConfigPageState extends ConsumerState<ConfigPage> {
  Map<String, dynamic> _config = {};
  Map<String, TextEditingController> _controllers = {};
  bool _isLoading = false;
  String _errorMessage = '';
  String _successMessage = '';

  @override
  void initState() {
    super.initState();
    _loadConfig();
  }

  Future<void> _loadConfig() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
      _successMessage = '';
    });

    try {
      final apiClient = ApiClient();
      final response = await apiClient.get('/api/config');
      
      setState(() {
        _config = Map<String, dynamic>.from(jsonDecode(response.body));
        _controllers = {};
        for (var entry in _config.entries) {
          _controllers[entry.key] = TextEditingController(text: entry.value.toString());
        }
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载配置失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _saveConfig() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
      _successMessage = '';
    });

    try {
      final updatedConfig = <String, dynamic>{};
      for (var entry in _controllers.entries) {
        updatedConfig[entry.key] = _parseValue(entry.value.text);
      }

      final apiClient = ApiClient();
      await apiClient.post('/api/config', body: updatedConfig);

      setState(() {
        _successMessage = '配置保存成功';
      });
    } catch (e) {
      setState(() {
        _errorMessage = '保存配置失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _resetConfig() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
      _successMessage = '';
    });

    try {
      final apiClient = ApiClient();
      await apiClient.delete('/api/config');
      await _loadConfig();
      setState(() {
        _successMessage = '配置已重置为默认值';
      });
    } catch (e) {
      setState(() {
        _errorMessage = '重置配置失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _addConfigItem() async {
    String key = '';
    String value = '';

    await showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('添加配置项'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                onChanged: (text) => key = text,
                decoration: const InputDecoration(labelText: '配置键'),
              ),
              TextField(
                onChanged: (text) => value = text,
                decoration: const InputDecoration(labelText: '配置值'),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () {
                if (key.isNotEmpty) {
                  setState(() {
                    _config[key] = _parseValue(value);
                    _controllers[key] = TextEditingController(text: value);
                  });
                }
                Navigator.pop(context);
              },
              child: const Text('添加'),
            ),
          ],
        );
      },
    );
  }

  void _removeConfigItem(String key) {
    setState(() {
      _config.remove(key);
      _controllers.remove(key);
    });
  }

  dynamic _parseValue(String text) {
    final intValue = int.tryParse(text);
    if (intValue != null) return intValue;

    final doubleValue = double.tryParse(text);
    if (doubleValue != null) return doubleValue;

    if (text.toLowerCase() == 'true') return true;
    if (text.toLowerCase() == 'false') return false;

    return text;
  }

  @override
  void dispose() {
    for (var controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('配置管理'),
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
                      '配置操作',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _saveConfig,
                            child: const Text('保存配置'),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _resetConfig,
                            child: const Text('重置配置'),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _addConfigItem,
                            child: const Text('添加配置项'),
                          ),
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
            if (_successMessage.isNotEmpty)
              Container(
                padding: const EdgeInsets.all(10),
                color: Colors.green[100],
                child: Text(
                  _successMessage,
                  style: const TextStyle(color: Colors.green),
                ),
              ),

            const SizedBox(height: 20),

            const Text(
              '配置项列表',
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
            else if (_config.isEmpty)
              const Center(
                child: Text('暂无配置项'),
              )
            else
              Expanded(
                child: ListView.builder(
                  itemCount: _config.length,
                  itemBuilder: (context, index) {
                    final key = _config.keys.elementAt(index);
                    final value = _config[key];
                    final controller = _controllers[key];

                    return Card(
                      elevation: 2,
                      margin: const EdgeInsets.only(bottom: 10),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    key,
                                    style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 16,
                                    ),
                                  ),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete, color: Colors.red),
                                  onPressed: () => _removeConfigItem(key),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            TextField(
                              controller: controller,
                              decoration: const InputDecoration(
                                labelText: '值',
                                border: OutlineInputBorder(),
                                hintText: '输入配置值',
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '类型: ${value.runtimeType}',
                              style: const TextStyle(
                                fontSize: 12,
                                color: Colors.grey,
                              ),
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
