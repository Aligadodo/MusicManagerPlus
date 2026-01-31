import 'package:flutter/material.dart';

class GlobalSettingsPage extends StatefulWidget {
  const GlobalSettingsPage({super.key});

  @override
  State<GlobalSettingsPage> createState() => _GlobalSettingsPageState();
}

class _GlobalSettingsPageState extends State<GlobalSettingsPage> {
  // 线程池配置
  int _previewThreads = 10;
  int _executionThreads = 4;
  String _threadPoolMode = 'GLOBAL'; // GLOBAL 或 ROOT_PATH

  // 扫描配置
  String _recursionMode = 'ALL'; // ALL, CURRENT, SPECIFIC, RANGE
  int _recursionDepth = 3;
  int _minRecursionDepth = 1;
  int _maxRecursionDepth = 3;

  // 过滤规则
  List<String> _scanFilterList = [
    '*Convert*',
    '*Split*',
    '*System*',
    '*trash*',
    '*Temp*',
    '*tmp*',
    '*cache*',
    '*backup*',
  ];
  String _newFilterRule = '';

  // 预览配置
  bool _autoRefresh = true;
  int _previewLimit = 200;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('全局设置'),
      ),
      body: Container(
        padding: const EdgeInsets.all(20.0),
        child: ListView(
          children: [
            _buildThreadPoolSection(),
            const SizedBox(height: 30),
            _buildScanSettingsSection(),
            const SizedBox(height: 30),
            _buildFilterRulesSection(),
            const SizedBox(height: 30),
            _buildPreviewSettingsSection(),
            const SizedBox(height: 30),
            _buildActionButtons(),
          ],
        ),
      ),
    );
  }

  Widget _buildThreadPoolSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '线程池配置',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('预览线程数:'),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 1,
                    max: 16,
                    value: _previewThreads.toDouble(),
                    onChanged: (value) {
                      setState(() {
                        _previewThreads = value.toInt();
                      });
                    },
                    divisions: 15,
                    label: '$_previewThreads',
                  ),
                ),
                Text('$_previewThreads'),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('执行线程数:'),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 1,
                    max: 12,
                    value: _executionThreads.toDouble(),
                    onChanged: (value) {
                      setState(() {
                        _executionThreads = value.toInt();
                      });
                    },
                    divisions: 11,
                    label: '$_executionThreads',
                  ),
                ),
                Text('$_executionThreads'),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('线程池模式:'),
                const SizedBox(width: 20),
                DropdownButton<String>(
                  value: _threadPoolMode,
                  items: const [
                    DropdownMenuItem(value: 'GLOBAL', child: Text('全局统一配置')),
                    DropdownMenuItem(value: 'ROOT_PATH', child: Text('根路径独立配置')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _threadPoolMode = value ?? 'GLOBAL';
                    });
                  },
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildScanSettingsSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '扫描配置',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('扫描模式:'),
                const SizedBox(width: 20),
                DropdownButton<String>(
                  value: _recursionMode,
                  items: const [
                    DropdownMenuItem(value: 'ALL', child: Text('全部文件')),
                    DropdownMenuItem(value: 'CURRENT', child: Text('当前目录')),
                    DropdownMenuItem(value: 'SPECIFIC', child: Text('指定目录层级')),
                    DropdownMenuItem(value: 'RANGE', child: Text('目录层级范围')),
                  ],
                  onChanged: (value) {
                    setState(() {
                      _recursionMode = value ?? 'ALL';
                    });
                  },
                ),
              ],
            ),
            if (_recursionMode == 'SPECIFIC')
              Padding(
                padding: const EdgeInsets.only(left: 120, top: 16),
                child: Row(
                  children: [
                    const Text('扫描层级:'),
                    const SizedBox(width: 20),
                    Expanded(
                      child: Slider(
                        min: 1,
                        max: 10,
                        value: _recursionDepth.toDouble(),
                        onChanged: (value) {
                          setState(() {
                            _recursionDepth = value.toInt();
                          });
                        },
                        divisions: 9,
                        label: '$_recursionDepth',
                      ),
                    ),
                    Text('$_recursionDepth'),
                  ],
                ),
              ),
            if (_recursionMode == 'RANGE')
              Column(
                children: [
                  Padding(
                    padding: const EdgeInsets.only(left: 120, top: 16),
                    child: Row(
                      children: [
                        const Text('最小层级:'),
                        const SizedBox(width: 20),
                        Expanded(
                          child: Slider(
                            min: 1,
                            max: 10,
                            value: _minRecursionDepth.toDouble(),
                            onChanged: (value) {
                              setState(() {
                                _minRecursionDepth = value.toInt();
                              });
                            },
                            divisions: 9,
                            label: '$_minRecursionDepth',
                          ),
                        ),
                        Text('$_minRecursionDepth'),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.only(left: 120, top: 16),
                    child: Row(
                      children: [
                        const Text('最大层级:'),
                        const SizedBox(width: 20),
                        Expanded(
                          child: Slider(
                            min: _minRecursionDepth.toDouble(),
                            max: 10,
                            value: _maxRecursionDepth.toDouble(),
                            onChanged: (value) {
                              setState(() {
                                _maxRecursionDepth = value.toInt();
                              });
                            },
                            divisions: 10 - _minRecursionDepth,
                            label: '$_maxRecursionDepth',
                          ),
                        ),
                        Text('$_maxRecursionDepth'),
                      ],
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterRulesSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '过滤规则',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: TextEditingController(text: _newFilterRule),
                    onChanged: (value) {
                      _newFilterRule = value;
                    },
                    decoration: const InputDecoration(
                      labelText: '添加过滤规则',
                      hintText: '例如：*Convert*',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    if (_newFilterRule.isNotEmpty && !_scanFilterList.contains(_newFilterRule)) {
                      setState(() {
                        _scanFilterList.add(_newFilterRule);
                        _newFilterRule = '';
                      });
                    }
                  },
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 16),
            if (_scanFilterList.isNotEmpty)
              Column(
                children: [
                  const Text('当前过滤规则:'),
                  const SizedBox(height: 8),
                  Container(
                    decoration: BoxDecoration(
                      border: Border.all(color: Colors.grey),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      children: _scanFilterList.asMap().entries.map((entry) {
                        int index = entry.key;
                        String rule = entry.value;
                        return Row(
                          children: [
                            Expanded(
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Text(rule),
                              ),
                            ),
                            IconButton(
                              icon: const Icon(Icons.delete, color: Colors.red),
                              onPressed: () {
                                setState(() {
                                  _scanFilterList.removeAt(index);
                                });
                              },
                            ),
                          ],
                        );
                      }).toList(),
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildPreviewSettingsSection() {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '预览配置',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Checkbox(
                  value: _autoRefresh,
                  onChanged: (value) {
                    setState(() {
                      _autoRefresh = value ?? true;
                    });
                  },
                ),
                const Text('自动刷新'),
              ],
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                const Text('预览数量限制:'),
                const SizedBox(width: 20),
                Expanded(
                  child: Slider(
                    min: 50,
                    max: 1000,
                    value: _previewLimit.toDouble(),
                    onChanged: (value) {
                      setState(() {
                        _previewLimit = value.toInt();
                      });
                    },
                    divisions: 19,
                    label: '$_previewLimit',
                  ),
                ),
                Text('$_previewLimit'),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      children: [
        Expanded(
          child: ElevatedButton(
            onPressed: () {
              // 保存配置
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('配置保存成功')),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.green,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
            child: const Text('保存配置'),
          ),
        ),
        const SizedBox(width: 20),
        Expanded(
          child: ElevatedButton(
            onPressed: () {
              // 重置配置
              setState(() {
                _previewThreads = 10;
                _executionThreads = 4;
                _threadPoolMode = 'GLOBAL';
                _recursionMode = 'ALL';
                _recursionDepth = 3;
                _minRecursionDepth = 1;
                _maxRecursionDepth = 3;
                _scanFilterList = [
                  '*Convert*',
                  '*Split*',
                  '*System*',
                  '*trash*',
                  '*Temp*',
                  '*tmp*',
                  '*cache*',
                  '*backup*',
                ];
                _autoRefresh = true;
                _previewLimit = 200;
              });
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('配置已重置')),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.grey,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 16),
            ),
            child: const Text('重置配置'),
          ),
        ),
      ],
    );
  }
}
