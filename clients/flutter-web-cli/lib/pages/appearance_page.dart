import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/config_service.dart';
import '../providers/config_provider.dart';

class AppearancePage extends ConsumerStatefulWidget {
  const AppearancePage({super.key});

  @override
  ConsumerState<AppearancePage> createState() => _AppearancePageState();
}

class _AppearancePageState extends ConsumerState<AppearancePage> {
  final ConfigService _configService = ConfigService(ApiClient());

  Map<String, dynamic> _appearanceConfig = {};
  List<Map<String, dynamic>> _themePresets = [];
  int _selectedPresetIndex = -1;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadAppearanceConfig();
      _loadThemePresets();
    });
  }

  Future<void> _loadAppearanceConfig() async {
    setState(() {
      _isLoading = true;
    });

    try {
      final config = ref.read(configProvider);
      setState(() {
        _appearanceConfig = Map.from(config.appearanceConfig);
        _isLoading = false;
      });
    } catch (e) {
      print('加载外观配置失败: $e');
      setState(() {
        _appearanceConfig = {};
        _isLoading = false;
      });
    }
  }

  Future<void> _loadThemePresets() async {
    try {
      final presets = await _configService.getThemePresets();
      setState(() {
        _themePresets = presets;
      });
      print('成功加载 ${presets.length} 个主题预设');
    } catch (e) {
      print('加载主题预设失败: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载主题预设失败: $e')),
        );
      }
    }
  }

  Future<void> _applyPreset(Map<String, dynamic> preset) async {
    setState(() {
      final config = preset['config'] as Map<String, dynamic>;
      _appearanceConfig = Map.from(config);
      _isLoading = true;
    });

    try {
      final configNotifier = ref.read(configProvider.notifier);
      configNotifier.updateAppearanceConfig(_appearanceConfig);
      await configNotifier.saveConfig();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('主题预设应用成功')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('应用主题预设失败: $e')),
        );
      }
    } finally {
      setState(() {
        _isLoading = false;
      });
    }
  }

  Future<void> _saveAsPreset() async {
    final nameController = TextEditingController();
    final descriptionController = TextEditingController();

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('保存主题预设'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameController,
              decoration: const InputDecoration(
                labelText: '主题名称',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: descriptionController,
              decoration: const InputDecoration(
                labelText: '主题描述',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('保存'),
          ),
        ],
      ),
    );

    if (result == true && nameController.text.isNotEmpty) {
      final newPreset = {
        'name': nameController.text,
        'description': descriptionController.text,
        'config': Map.from(_appearanceConfig),
      };

      try {
        await _configService.saveThemePreset(newPreset);
        await _loadThemePresets();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('主题预设保存成功')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('保存主题预设失败: $e')),
          );
        }
      }
    }
  }

  void _resetToDefault() {
    final config = ref.read(configProvider);
    setState(() {
      _appearanceConfig = Map.from(config.appearanceConfig);
    });
  }



  void _autoSaveConfig() {
    final configNotifier = ref.read(configProvider.notifier);
    configNotifier.updateAppearanceConfig(_appearanceConfig);
    configNotifier.saveConfig().catchError((e) {
      print('自动保存配置失败: $e');
    });
  }

  Color _parseColor(String colorString) {
    try {
      return Color(int.parse(colorString.replaceAll('#', '0xFF')));
    } catch (e) {
      return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('界面设置'),
      ),
      body: Row(
        children: [
          Container(
            width: 200,
            color: Colors.grey.shade100,
            child: ListView(
              children: [
                _buildNavItem('主题预设', 0),
                _buildNavItem('颜色设置', 1),
                _buildNavItem('背景设置', 2),
                _buildNavItem('字体设置', 3),
                _buildNavItem('样式管理', 4),
              ],
            ),
          ),
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(20.0),
              child: ListView(
                children: [
                  if (_isLoading)
                    const Center(
                      child: CircularProgressIndicator(),
                    ),
                  const SizedBox(height: 20),
                  if (_selectedSection == 0)
                    _buildPresetTabContent(),
                  if (_selectedSection == 1)
                    _buildColorTabContent(),
                  if (_selectedSection == 2)
                    _buildBackgroundTabContent(),
                  if (_selectedSection == 3)
                    _buildFontTabContent(),
                  if (_selectedSection == 4)
                    _buildStyleTabContent(),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem(String title, int index) {
    return InkWell(
      onTap: () {
        setState(() {
          _selectedSection = index;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: _selectedSection == index ? Colors.blue.shade100 : Colors.transparent,
          border: Border(
            left: BorderSide(
              color: _selectedSection == index ? Colors.blue : Colors.transparent,
              width: 4,
            ),
          ),
        ),
        child: Text(
          title,
          style: TextStyle(
            color: _selectedSection == index ? Colors.blue.shade700 : Colors.black87,
            fontWeight: _selectedSection == index ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  int _selectedSection = 0;

  Widget _buildPresetTabContent() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '主题预设',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 20),
          if (_themePresets.isEmpty)
            const Center(
              child: Text('暂无主题预设'),
            )
          else
            GridView.builder(
              shrinkWrap: true,
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 2,
              ),
              itemCount: _themePresets.length,
              itemBuilder: (context, index) {
                final preset = _themePresets[index];
                return _buildPresetCard(preset, index);
              },
            ),
          const SizedBox(height: 20),
          Center(
            child: ElevatedButton.icon(
              onPressed: _saveAsPreset,
              icon: const Icon(Icons.add),
              label: const Text('保存当前主题为预设'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
            ),
          ),
        ],
      ),
    );
  }

  bool _isPresetApplied(Map<String, dynamic> preset) {
    final config = preset['config'] as Map<String, dynamic>;
    return config['bgColor'] == _appearanceConfig['bgColor'] &&
           config['accentColor'] == _appearanceConfig['accentColor'] &&
           config['theme'] == _appearanceConfig['theme'];
  }

  Widget _buildPresetCard(Map<String, dynamic> preset, int index) {
    final config = preset['config'] as Map<String, dynamic>;
    final isApplied = _isPresetApplied(preset);
    return Card(
      elevation: isApplied ? 4 : 2,
      borderOnForeground: true,
      shape: RoundedRectangleBorder(
        side: BorderSide(
          color: isApplied ? Colors.blue : Colors.grey.shade300,
          width: isApplied ? 2 : 1,
        ),
        borderRadius: BorderRadius.circular(8),
      ),
      child: InkWell(
        onTap: () => _applyPreset(config),
        child: Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: _parseColor(config['bgColor']),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Stack(
            children: [
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    preset['name'] ?? '未命名主题',
                    style: TextStyle(
                      color: _parseColor(config['textPrimaryColor']),
                      fontWeight: FontWeight.bold,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 5),
                  Text(
                    preset['description'] ?? '',
                    style: TextStyle(
                      color: _parseColor(config['textSecondaryColor']),
                      fontSize: 12,
                    ),
                    textAlign: TextAlign.center,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
              if (isApplied)
                Positioned(
                  top: 5,
                  right: 5,
                  child: Icon(
                    Icons.check_circle,
                    color: Colors.green,
                    size: 20,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildColorTabContent() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '颜色设置',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 20),
          _buildColorField('背景颜色', 'bgColor'),
          _buildColorField('强调色', 'accentColor'),
          _buildColorField('主文本颜色', 'textPrimaryColor'),
          _buildColorField('次要文本颜色', 'textSecondaryColor'),
          _buildColorField('第三文本颜色', 'textTertiaryColor'),
          _buildColorField('禁用文本颜色', 'textDisabledColor'),
          _buildColorField('面板背景颜色', 'panelBgColor'),
          _buildColorField('列表背景颜色', 'listBgColor'),
          _buildColorField('列表偶数行颜色', 'listRowEvenBgColor'),
          _buildColorField('列表奇数行颜色', 'listRowOddBgColor'),
          _buildColorField('列表选中行颜色', 'listRowSelectedBgColor'),
          _buildColorField('列表选中行文本颜色', 'listRowSelectedTextColor'),
          _buildColorField('列表悬停行颜色', 'listRowHoverBgColor'),
          _buildColorField('列表边框颜色', 'listBorderColor'),
          _buildColorField('列表表头背景颜色', 'listHeaderBgColor'),
          _buildColorField('列表表头文本颜色', 'listHeaderTextColor'),
          _buildColorField('边框颜色', 'borderColor'),
        ],
      ),
    );
  }

  Widget _buildColorField(String label, String key) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 15),
      child: Row(
        children: [
          Expanded(
            flex: 2,
            child: Text(label),
          ),
          Expanded(
            flex: 1,
            child: Container(
              height: 40,
              decoration: BoxDecoration(
                color: _parseColor(_appearanceConfig[key]),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            flex: 2,
            child: TextField(
              controller: TextEditingController(text: _appearanceConfig[key]),
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              ),
              onChanged: (value) {
                setState(() {
                  _appearanceConfig[key] = value;
                });
                _autoSaveConfig();
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBackgroundTabContent() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '背景设置',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 20),
          _buildSliderField('玻璃效果透明度', 'glassOpacity', 0.0, 1.0, 0.1),
          _buildSwitchField('深色背景', 'darkBackground'),
        ],
      ),
    );
  }

  Widget _buildFontTabContent() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '字体设置',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 20),
          _buildDropdownField('字体', 'fontFamily', [
            'Roboto',
            'Arial',
            'Helvetica',
            'Times New Roman',
            'Courier New',
            'Verdana',
          ]),
          _buildNumberField('字体大小', 'fontSize', 8, 24),
        ],
      ),
    );
  }

  Widget _buildStyleTabContent() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '样式设置',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 20),
          _buildNumberField('圆角半径', 'cornerRadius', 0, 20),
          _buildNumberField('边框宽度', 'borderWidth', 0, 5),
          _buildNumberField('大按钮尺寸', 'buttonLargeSize', 32, 64),
          _buildNumberField('小按钮尺寸', 'buttonSmallSize', 24, 48),
        ],
      ),
    );
  }

  Widget _buildSliderField(String label, String key, double min, double max, double divisions) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$label: ${(_appearanceConfig[key] as double).toStringAsFixed(1)}',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          Slider(
            value: _appearanceConfig[key] as double,
            min: min,
            max: max,
            divisions: (max - min / divisions).toInt(),
            onChanged: (value) {
              setState(() {
                _appearanceConfig[key] = value;
              });
              _autoSaveConfig();
            },
          ),
        ],
      ),
    );
  }

  Widget _buildSwitchField(String label, String key) {
    try {
      // 安全获取布尔值，处理各种类型情况
      bool getValue() {
        final value = _appearanceConfig[key];
        if (value == null) {
          return false;
        }
        if (value is bool) {
          return value;
        }
        if (value is String) {
          return value.toLowerCase() == 'true';
        }
        if (value is int) {
          return value == 1;
        }
        return false;
      }

      return Padding(
        padding: const EdgeInsets.only(bottom: 20),
        child: SwitchListTile(
          title: Text(label),
          value: getValue(),
          onChanged: (value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          },
        ),
      );
    } catch (e) {
      print('构建开关字段失败: $e');
      return Padding(
        padding: const EdgeInsets.only(bottom: 20),
        child: ListTile(
          title: Text(label),
          subtitle: Text('加载失败: $e'),
        ),
      );
    }
  }

  Widget _buildDropdownField(String label, String key, List<String> options) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          DropdownButtonFormField<String>(
            initialValue: _appearanceConfig[key] as String,
            items: options.map((option) {
              return DropdownMenuItem<String>(
                value: option,
                child: Text(option),
              );
            }).toList(),
            onChanged: (value) {
              setState(() {
                _appearanceConfig[key] = value;
              });
              _autoSaveConfig();
            },
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNumberField(String label, String key, int min, int max) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: TextEditingController(text: _appearanceConfig[key].toString()),
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              contentPadding: EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            ),
            onChanged: (value) {
              final numValue = int.tryParse(value);
              if (numValue != null && numValue >= min && numValue <= max) {
                setState(() {
                  _appearanceConfig[key] = numValue;
                });
                _autoSaveConfig();
              }
            },
          ),
        ],
      ),
    );
  }
}
