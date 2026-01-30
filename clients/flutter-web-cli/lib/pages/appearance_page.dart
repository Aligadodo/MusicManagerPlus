import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';

class AppearancePage extends ConsumerStatefulWidget {
  const AppearancePage({super.key});

  @override
  ConsumerState<AppearancePage> createState() => _AppearancePageState();
}

class _AppearancePageState extends ConsumerState<AppearancePage> {
  Map<String, dynamic> _appearanceConfig = {
    'theme': 'light',
    'primaryColor': '#2196F3',
    'accentColor': '#FF4081',
    'backgroundColor': '#FFFFFF',
    'textColor': '#000000',
    'fontFamily': 'Roboto',
    'fontSize': 14,
  };
  bool _isLoading = false;
  String _errorMessage = '';
  String _successMessage = '';

  @override
  void initState() {
    super.initState();
    _loadAppearanceConfig();
  }

  Future<void> _loadAppearanceConfig() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
      _successMessage = '';
    });

    try {
      final apiClient = ref.read(apiClientProvider);
      final response = await apiClient.get('/api/config');
      
      setState(() {
        // 从配置中加载外观设置
        if (response.containsKey('theme')) {
          _appearanceConfig['theme'] = response['theme'];
        }
        if (response.containsKey('primaryColor')) {
          _appearanceConfig['primaryColor'] = response['primaryColor'];
        }
        if (response.containsKey('accentColor')) {
          _appearanceConfig['accentColor'] = response['accentColor'];
        }
        if (response.containsKey('backgroundColor')) {
          _appearanceConfig['backgroundColor'] = response['backgroundColor'];
        }
        if (response.containsKey('textColor')) {
          _appearanceConfig['textColor'] = response['textColor'];
        }
        if (response.containsKey('fontFamily')) {
          _appearanceConfig['fontFamily'] = response['fontFamily'];
        }
        if (response.containsKey('fontSize')) {
          _appearanceConfig['fontSize'] = response['fontSize'];
        }
      });
    } catch (e) {
      // 加载失败时使用默认配置
      print('加载外观配置失败: $e');
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _saveAppearanceConfig() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
      _successMessage = '';
    });

    try {
      final apiClient = ref.read(apiClientProvider);
      await apiClient.post('/api/config', _appearanceConfig);

      setState(() {
        _successMessage = '外观设置保存成功';
      });
    } catch (e) {
      setState(() {
        _errorMessage = '保存外观设置失败: $e';
      });
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _resetToDefault() async {
    setState(() {
      _appearanceConfig = {
        'theme': 'light',
        'primaryColor': '#2196F3',
        'accentColor': '#FF4081',
        'backgroundColor': '#FFFFFF',
        'textColor': '#000000',
        'fontFamily': 'Roboto',
        'fontSize': 14,
      };
    });
  }

  Color _parseColor(String colorString) {
    try {
      return Color(int.parse(colorString.replaceAll('#', '0xFF')));
    } catch (e) {
      return Colors.blue; // 默认颜色
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('界面设置'),
      ),
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          children: [
            // 操作按钮
            Card(
              elevation: 4,
              margin: const EdgeInsets.only(bottom: 20),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  children: [
                    const Text(
                      '外观操作',
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
                            onPressed: _saveAppearanceConfig,
                            child: const Text('保存设置'),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _resetToDefault,
                            child: const Text('恢复默认'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),

            // 消息显示
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

            // 外观设置
            const Text(
              '外观设置',
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
            else
              Expanded(
                child: ListView(
                  children: [
                    // 主题设置
                    Card(
                      elevation: 4,
                      margin: const EdgeInsets.only(bottom: 20),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            const Text(
                              '主题',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 16),
                            Row(
                              children: [
                                Expanded(
                                  child: RadioListTile<String>(
                                    title: const Text('亮色主题'),
                                    value: 'light',
                                    groupValue: _appearanceConfig['theme'],
                                    onChanged: (value) {
                                      setState(() {
                                        _appearanceConfig['theme'] = value!;
                                      });
                                    },
                                  ),
                                ),
                                Expanded(
                                  child: RadioListTile<String>(
                                    title: const Text('暗色主题'),
                                    value: 'dark',
                                    groupValue: _appearanceConfig['theme'],
                                    onChanged: (value) {
                                      setState(() {
                                        _appearanceConfig['theme'] = value!;
                                      });
                                    },
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),

                    // 颜色设置
                    Card(
                      elevation: 4,
                      margin: const EdgeInsets.only(bottom: 20),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            const Text(
                              '颜色设置',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 16),
                            Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    children: [
                                      const Text('主色调'),
                                      const SizedBox(height: 8),
                                      Container(
                                        width: 100,
                                        height: 50,
                                        decoration: BoxDecoration(
                                          color: _parseColor(_appearanceConfig['primaryColor']),
                                          borderRadius: BorderRadius.circular(8),
                                          border: Border.all(color: Colors.grey),
                                        ),
                                      ),
                                      const SizedBox(height: 8),
                                      TextField(
                                        onChanged: (value) {
                                          setState(() {
                                            _appearanceConfig['primaryColor'] = value;
                                          });
                                        },
                                        controller: TextEditingController(text: _appearanceConfig['primaryColor']),
                                        decoration: const InputDecoration(
                                          labelText: '颜色代码',
                                          border: OutlineInputBorder(),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 16),
                                Expanded(
                                  child: Column(
                                    children: [
                                      const Text('强调色'),
                                      const SizedBox(height: 8),
                                      Container(
                                        width: 100,
                                        height: 50,
                                        decoration: BoxDecoration(
                                          color: _parseColor(_appearanceConfig['accentColor']),
                                          borderRadius: BorderRadius.circular(8),
                                          border: Border.all(color: Colors.grey),
                                        ),
                                      ),
                                      const SizedBox(height: 8),
                                      TextField(
                                        onChanged: (value) {
                                          setState(() {
                                            _appearanceConfig['accentColor'] = value;
                                          });
                                        },
                                        controller: TextEditingController(text: _appearanceConfig['accentColor']),
                                        decoration: const InputDecoration(
                                          labelText: '颜色代码',
                                          border: OutlineInputBorder(),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),

                    // 字体设置
                    Card(
                      elevation: 4,
                      margin: const EdgeInsets.only(bottom: 20),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            const Text(
                              '字体设置',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 16),
                            Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    children: [
                                      const Text('字体'),
                                      const SizedBox(height: 8),
                                      DropdownButtonFormField<String>(
                                        value: _appearanceConfig['fontFamily'],
                                        onChanged: (value) {
                                          setState(() {
                                            _appearanceConfig['fontFamily'] = value!;
                                          });
                                        },
                                        items: const [
                                          DropdownMenuItem(value: 'Roboto', child: Text('Roboto')),
                                          DropdownMenuItem(value: 'Arial', child: Text('Arial')),
                                          DropdownMenuItem(value: 'Helvetica', child: Text('Helvetica')),
                                          DropdownMenuItem(value: 'Times New Roman', child: Text('Times New Roman')),
                                        ],
                                        decoration: const InputDecoration(
                                          border: OutlineInputBorder(),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 16),
                                Expanded(
                                  child: Column(
                                    children: [
                                      const Text('字体大小'),
                                      const SizedBox(height: 8),
                                      TextField(
                                        onChanged: (value) {
                                          setState(() {
                                            _appearanceConfig['fontSize'] = int.tryParse(value) ?? 14;
                                          });
                                        },
                                        controller: TextEditingController(text: _appearanceConfig['fontSize'].toString()),
                                        decoration: const InputDecoration(
                                          border: OutlineInputBorder(),
                                        ),
                                        keyboardType: TextInputType.number,
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ),

                    // 预览
                    Card(
                      elevation: 4,
                      margin: const EdgeInsets.only(bottom: 20),
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Column(
                          children: [
                            const Text(
                              '预览',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 16),
                            Container(
                              width: double.infinity,
                              height: 100,
                              decoration: BoxDecoration(
                                color: _parseColor(_appearanceConfig['backgroundColor']),
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(color: Colors.grey),
                              ),
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Text(
                                    '预览文本',
                                    style: TextStyle(
                                      color: _parseColor(_appearanceConfig['textColor']),
                                      fontFamily: _appearanceConfig['fontFamily'],
                                      fontSize: _appearanceConfig['fontSize'].toDouble(),
                                    ),
                                  ),
                                  const SizedBox(height: 8),
                                  ElevatedButton(
                                    onPressed: () {},
                                    style: ElevatedButton.styleFrom(
                                      backgroundColor: _parseColor(_appearanceConfig['primaryColor']),
                                      foregroundColor: Colors.white,
                                    ),
                                    child: const Text('预览按钮'),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
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
