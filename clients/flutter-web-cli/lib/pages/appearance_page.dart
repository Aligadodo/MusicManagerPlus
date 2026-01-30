import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/config_service.dart';

class AppearancePage extends ConsumerStatefulWidget {
  const AppearancePage({super.key});

  @override
  ConsumerState<AppearancePage> createState() => _AppearancePageState();
}

class _AppearancePageState extends ConsumerState<AppearancePage> {
  final ConfigService _configService = ConfigService(ApiClient());

  Map<String, dynamic> _appearanceConfig = {
    'theme': 'light',
    'bgColor': '#FFFFFF',
    'accentColor': '#2196F3',
    'textPrimaryColor': '#000000',
    'textSecondaryColor': '#666666',
    'textTertiaryColor': '#999999',
    'textDisabledColor': '#CCCCCC',
    'glassOpacity': 0.9,
    'darkBackground': false,
    'panelBgColor': '#FFFFFF',
    'fontFamily': 'Roboto',
    'fontSize': 14,
    'cornerRadius': 8.0,
    'borderWidth': 1.0,
    'borderColor': '#E0E0E0',
    'listBgColor': '#FFFFFF',
    'listRowEvenBgColor': '#FFFFFF',
    'listRowOddBgColor': '#F5F5F5',
    'listRowSelectedBgColor': '#2196F3',
    'listRowSelectedTextColor': '#FFFFFF',
    'listRowHoverBgColor': '#E3F2FD',
    'listBorderColor': '#E0E0E0',
    'listHeaderBgColor': '#F5F5F5',
    'listHeaderTextColor': '#000000',
    'buttonLargeSize': 48.0,
    'buttonSmallSize': 32.0,
  };

  List<Map<String, dynamic>> _themePresets = [];
  int _selectedPresetIndex = 0;
  bool _isLoading = false;
  String _errorMessage = '';
  String _successMessage = '';

  @override
  void initState() {
    super.initState();
    _loadAppearanceConfig();
    _loadThemePresets();
  }

  Future<void> _loadAppearanceConfig() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      final config = await _configService.getConfig();
      setState(() {
        if (config.containsKey('theme')) {
          _appearanceConfig['theme'] = config['theme'];
        }
        if (config.containsKey('bgColor')) {
          _appearanceConfig['bgColor'] = config['bgColor'];
        }
        if (config.containsKey('accentColor')) {
          _appearanceConfig['accentColor'] = config['accentColor'];
        }
        if (config.containsKey('textPrimaryColor')) {
          _appearanceConfig['textPrimaryColor'] = config['textPrimaryColor'];
        }
        if (config.containsKey('glassOpacity')) {
          _appearanceConfig['glassOpacity'] = config['glassOpacity'];
        }
        if (config.containsKey('fontFamily')) {
          _appearanceConfig['fontFamily'] = config['fontFamily'];
        }
        if (config.containsKey('fontSize')) {
          _appearanceConfig['fontSize'] = config['fontSize'];
        }
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = '加载外观配置失败: $e';
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
    } catch (e) {
      print('加载主题预设失败: $e');
    }
  }

  Future<void> _saveAppearanceConfig() async {
    setState(() {
      _isLoading = true;
      _errorMessage = '';
      _successMessage = '';
    });

    try {
      await _configService.saveConfig(_appearanceConfig);
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

  Future<void> _applyPreset(Map<String, dynamic> preset) async {
    setState(() {
      _appearanceConfig = Map.from(preset);
    });
    await _saveAppearanceConfig();
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
    setState(() {
      _appearanceConfig = {
        'theme': 'light',
        'bgColor': '#FFFFFF',
        'accentColor': '#2196F3',
        'textPrimaryColor': '#000000',
        'textSecondaryColor': '#666666',
        'textTertiaryColor': '#999999',
        'textDisabledColor': '#CCCCCC',
        'glassOpacity': 0.9,
        'darkBackground': false,
        'panelBgColor': '#FFFFFF',
        'fontFamily': 'Roboto',
        'fontSize': 14,
        'cornerRadius': 8.0,
        'borderWidth': 1.0,
        'borderColor': '#E0E0E0',
        'listBgColor': '#FFFFFF',
        'listRowEvenBgColor': '#FFFFFF',
        'listRowOddBgColor': '#F5F5F5',
        'listRowSelectedBgColor': '#2196F3',
        'listRowSelectedTextColor': '#FFFFFF',
        'listRowHoverBgColor': '#E3F2FD',
        'listBorderColor': '#E0E0E0',
        'listHeaderBgColor': '#F5F5F5',
        'listHeaderTextColor': '#000000',
        'buttonLargeSize': 48.0,
        'buttonSmallSize': 32.0,
      };
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
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.9),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade300),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildActionButtons(),
          const SizedBox(height: 20),
          Expanded(
            child: DefaultTabController(
              length: 5,
              child: Column(
                children: [
                  _buildTabBar(),
                  const SizedBox(height: 10),
                  Expanded(
                    child: TabBarView(
                      children: [
                        _buildPresetTabContent(),
                        _buildColorTabContent(),
                        _buildBackgroundTabContent(),
                        _buildFontTabContent(),
                        _buildStyleTabContent(),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.end,
      children: [
        ElevatedButton.icon(
          onPressed: _isLoading ? null : _saveAppearanceConfig,
          icon: const Icon(Icons.save),
          label: const Text('保存设置'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.blue,
            foregroundColor: Colors.white,
          ),
        ),
        const SizedBox(width: 10),
        OutlinedButton.icon(
          onPressed: _resetToDefault,
          icon: const Icon(Icons.refresh),
          label: const Text('恢复默认'),
        ),
      ],
    );
  }

  Widget _buildTabBar() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.grey.shade100,
        borderRadius: BorderRadius.circular(8),
      ),
      child: const TabBar(
        tabs: [
          Tab(text: '主题预设'),
          Tab(text: '颜色设置'),
          Tab(text: '背景设置'),
          Tab(text: '字体设置'),
          Tab(text: '样式管理'),
        ],
        labelColor: Colors.blue,
        unselectedLabelColor: Colors.grey,
        indicatorColor: Colors.blue,
      ),
    );
  }

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

  Widget _buildPresetCard(Map<String, dynamic> preset, int index) {
    final config = preset['config'] as Map<String, dynamic>;
    return Card(
      elevation: 2,
      child: InkWell(
        onTap: () => _applyPreset(config),
        child: Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            color: _parseColor(config['bgColor']),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
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
            },
          ),
        ],
      ),
    );
  }

  Widget _buildSwitchField(String label, String key) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: SwitchListTile(
        title: Text(label),
        value: _appearanceConfig[key] as bool,
        onChanged: (value) {
          setState(() {
            _appearanceConfig[key] = value;
          });
        },
      ),
    );
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
            value: _appearanceConfig[key] as String,
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
              }
            },
          ),
        ],
      ),
    );
  }
}
