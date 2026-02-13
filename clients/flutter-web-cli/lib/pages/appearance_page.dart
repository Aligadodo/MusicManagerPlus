import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_colorpicker/flutter_colorpicker.dart';
import '../api/api_client.dart';
import '../api/config_service.dart';
import '../providers/config_provider.dart';
import '../utils/ui_utils.dart';

class AppearancePage extends ConsumerStatefulWidget {
  const AppearancePage({super.key});

  @override
  ConsumerState<AppearancePage> createState() => _AppearancePageState();
}

class _AppearancePageState extends ConsumerState<AppearancePage> {
  final ConfigService _configService = ConfigService(ApiClient());

  Map<String, dynamic> _appearanceConfig = {};
  List<Map<String, dynamic>> _themePresets = [];
  final int _selectedPresetIndex = -1;
  int _selectedSection = 0;
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
      // 使用新的主题接口
      final themes = await _configService.getThemes();
      setState(() {
        _themePresets = themes;
      });
      print('成功加载 ${themes.length} 个主题');
    } catch (e) {
      print('加载主题失败: $e');
      //  fallback 到旧接口
      try {
        final presets = await _configService.getThemePresets();
        setState(() {
          _themePresets = presets;
        });
        print('成功加载 ${presets.length} 个主题预设 (fallback)');
      } catch (fallbackError) {
        print('加载主题预设失败: $fallbackError');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('加载主题失败: $fallbackError')),
          );
        }
      }
    }
  }

  Future<void> _applyPreset(Map<String, dynamic> preset) async {
    try {
      final config = preset['config'];
      if (config == null) {
        throw Exception('主题配置为空');
      }

      if (config is! Map<String, dynamic>) {
        throw Exception('主题配置格式错误');
      }

      setState(() {
        _appearanceConfig = Map<String, dynamic>.from(config);
        _isLoading = true;
      });

      final configNotifier = ref.read(configProvider.notifier);
      configNotifier.updateAppearanceConfig(_appearanceConfig);
      await configNotifier.saveConfig();
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('主题预设应用成功')),
        );
      }
    } catch (e) {
      print('应用主题预设失败: $e');
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
      final newTheme = {
        'name': nameController.text,
        'description': descriptionController.text,
        'config': Map.from(_appearanceConfig),
      };

      try {
        await _configService.createTheme(newTheme);
        await _loadThemePresets();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('主题预设保存成功')),
          );
        }
      } catch (e) {
        print('保存主题预设失败: $e');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('保存主题预设失败: $e')),
          );
        }
      }
    }
  }

  Future<void> _editTheme(Map<String, dynamic> preset) async {
    final nameController = TextEditingController(text: preset['name'] ?? '');
    final descriptionController = TextEditingController(text: preset['description'] ?? '');
    final themeId = preset['id'];

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('编辑主题'),
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

    if (result == true && nameController.text.isNotEmpty && themeId != null) {
      final updatedTheme = {
        'name': nameController.text,
        'description': descriptionController.text,
        'config': Map.from(preset['config'] ?? {}),
      };

      try {
        await _configService.updateTheme(themeId, updatedTheme);
        await _loadThemePresets();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('主题编辑成功')),
          );
        }
      } catch (e) {
        print('编辑主题失败: $e');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('编辑主题失败: $e')),
          );
        }
      }
    }
  }

  Future<void> _deleteTheme(String? themeId) async {
    if (themeId == null) return;

    final theme = Theme.of(context);

    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('确定要删除这个主题吗？此操作不可撤销。'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(
              backgroundColor: theme.colorScheme.error,
              foregroundColor: theme.colorScheme.onError,
            ),
            child: const Text('删除'),
          ),
        ],
      ),
    );

    if (result == true) {
      try {
        await _configService.deleteTheme(themeId);
        await _loadThemePresets();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('主题删除成功')),
          );
        }
      } catch (e) {
        print('删除主题失败: $e');
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('删除主题失败: $e')),
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



  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final primaryColor = theme.primaryColor;
    final backgroundColor = theme.scaffoldBackgroundColor;
    final textColor = theme.textTheme.bodyLarge?.color ?? Colors.black;
    final borderColor = theme.dividerColor;
    final cardColor = theme.cardColor;

    return Scaffold(
      appBar: AppBar(
        title: const Text(''),
      ),
      body: Row(
        children: [
          Container(
            width: 200,
            color: theme.colorScheme.surfaceContainer,
            child: ListView(
              children: [
                _buildNavItem('主题预设', 0, theme),
                _buildNavItem('颜色设置', 1, theme),
                _buildNavItem('背景设置', 2, theme),
                _buildNavItem('字体设置', 3, theme),
                _buildNavItem('样式管理', 4, theme),
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
                  if (_selectedSection == 0)
                    _buildPresetTabContent(theme),
                  if (_selectedSection == 1)
                    _buildColorTabContent(theme),
                  if (_selectedSection == 2)
                    _buildBackgroundTabContent(theme),
                  if (_selectedSection == 3)
                    _buildFontTabContent(theme),
                  if (_selectedSection == 4)
                    _buildStyleTabContent(theme),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNavItem(String title, int index, ThemeData theme) {
    final primaryColor = theme.primaryColor;
    final textColor = theme.textTheme.bodyLarge?.color ?? Colors.black;
    final backgroundColor = _selectedSection == index ? primaryColor.withOpacity(0.1) : Colors.transparent;
    
    return InkWell(
      onTap: () {
        setState(() {
          _selectedSection = index;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: backgroundColor,
          border: Border(
            left: BorderSide(
              color: _selectedSection == index ? primaryColor : Colors.transparent,
              width: 4,
            ),
          ),
        ),
        child: Text(
          title,
          style: TextStyle(
            color: _selectedSection == index ? primaryColor : textColor,
            fontWeight: _selectedSection == index ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }



  Widget _buildPresetTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_themePresets.isEmpty)
            Center(
              child: Text('暂无主题预设', style: theme.textTheme.bodyMedium),
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
                return _buildPresetCard(preset, index, theme);
              },
            ),
          const SizedBox(height: 20),
          Center(
            child: ElevatedButton.icon(
              onPressed: _saveAsPreset,
              icon: const Icon(Icons.add),
              label: const Text('保存当前主题为预设'),
              style: ElevatedButton.styleFrom(
                backgroundColor: theme.colorScheme.primary,
                foregroundColor: theme.colorScheme.onPrimary,
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

  Widget _buildPresetCard(Map<String, dynamic> preset, int index, ThemeData theme) {
    final config = preset['config'] as Map<String, dynamic>;
    final isApplied = _isPresetApplied(preset);
    final accentColor = UiUtils.parseColor(config['accentColor'] ?? '#2196F3');
    final isDefault = preset['type'] == 'default';
    final themeId = preset['id'];
    
    return Card(
      elevation: isApplied ? 6 : 2,
      borderOnForeground: true,
      shape: RoundedRectangleBorder(
        side: BorderSide(
          color: isApplied ? accentColor : theme.dividerColor,
          width: isApplied ? 2 : 1,
        ),
        borderRadius: BorderRadius.circular(10),
      ),
      child: InkWell(
        onTap: () => _applyPreset(preset),
        hoverColor: UiUtils.parseColor(config['listRowHoverBgColor'] ?? '#F0F8FF'),
        splashColor: accentColor.withOpacity(0.3),
        child: Container(
          padding: const EdgeInsets.all(15),
          decoration: BoxDecoration(
            color: UiUtils.parseColor(config['bgColor'] ?? '#FFFFFF'),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Stack(
            children: [
              Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    preset['name'] ?? '未命名主题',
                    style: TextStyle(
                      color: UiUtils.parseColor(config['textPrimaryColor'] ?? '#000000'),
                      fontWeight: FontWeight.bold,
                      fontSize: 16,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    preset['description'] ?? '',
                    style: TextStyle(
                      color: UiUtils.parseColor(config['textSecondaryColor'] ?? '#666666'),
                      fontSize: 12,
                    ),
                    textAlign: TextAlign.center,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (isDefault)
                    Container(
                      margin: const EdgeInsets.only(top: 8),
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(
                        color: accentColor.withOpacity(0.1),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        '系统预设',
                        style: TextStyle(
                          color: accentColor,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  if (!isDefault)
                    Container(
                      margin: const EdgeInsets.only(top: 8),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          IconButton(
                            icon: Icon(Icons.edit, size: 16, color: accentColor),
                            onPressed: () => _editTheme(preset),
                            tooltip: '编辑主题',
                          ),
                          IconButton(
                            icon: Icon(Icons.delete, size: 16, color: theme.colorScheme.error),
                            onPressed: () => _deleteTheme(themeId),
                            tooltip: '删除主题',
                          ),
                        ],
                      ),
                    ),
                ],
              ),
              if (isApplied)
                Positioned(
                  top: 5,
                  right: 5,
                  child: Icon(
                    Icons.check_circle,
                    color: accentColor,
                    size: 20,
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildColorTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildColorField('背景颜色', 'bgColor', theme),
          _buildColorField('强调色', 'accentColor', theme),
          _buildColorField('主文本颜色', 'textPrimaryColor', theme),
          _buildColorField('次要文本颜色', 'textSecondaryColor', theme),
          _buildColorField('第三文本颜色', 'textTertiaryColor', theme),
          _buildColorField('禁用文本颜色', 'textDisabledColor', theme),
          _buildColorField('面板背景颜色', 'panelBgColor', theme),
          _buildColorField('列表背景颜色', 'listBgColor', theme),
          _buildColorField('列表偶数行颜色', 'listRowEvenBgColor', theme),
          _buildColorField('列表奇数行颜色', 'listRowOddBgColor', theme),
          _buildColorField('列表选中行颜色', 'listRowSelectedBgColor', theme),
          _buildColorField('列表选中行文本颜色', 'listRowSelectedTextColor', theme),
          _buildColorField('列表悬停行颜色', 'listRowHoverBgColor', theme),
          _buildColorField('列表边框颜色', 'listBorderColor', theme),
          _buildColorField('列表表头背景颜色', 'listHeaderBgColor', theme),
          _buildColorField('列表表头文本颜色', 'listHeaderTextColor', theme),
          _buildColorField('边框颜色', 'borderColor', theme),
        ],
      ),
    );
  }

  Widget _buildColorField(String label, String key, ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 15),
      child: Row(
        children: [
          Expanded(
            flex: 2,
            child: Text(label, style: theme.textTheme.bodyMedium),
          ),
          Expanded(
            flex: 1,
            child: InkWell(
              onTap: () {
                _showColorPicker(key);
              },
              child: Container(
                height: 40,
                decoration: BoxDecoration(
                  color: UiUtils.parseColor(_appearanceConfig[key]),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: theme.dividerColor),
                ),
                child: const Center(
                  child: Icon(Icons.color_lens, size: 20),
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            flex: 2,
            child: TextField(
              controller: TextEditingController(text: _appearanceConfig[key]),
              decoration: InputDecoration(
                border: OutlineInputBorder(),
                contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                labelStyle: theme.textTheme.bodyMedium,
                hintStyle: theme.textTheme.bodySmall?.copyWith(color: theme.hintColor),
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

  void _showColorPicker(String key) {
    Color currentColor = UiUtils.parseColor(_appearanceConfig[key]);
    
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('选择颜色'),
          content: SingleChildScrollView(
            child: ColorPicker(
              pickerColor: currentColor,
              onColorChanged: (color) {
                currentColor = color;
              },
              showLabel: true,
              pickerAreaHeightPercent: 0.8,
            ),
          ),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: const Text('取消'),
            ),
            TextButton(
              onPressed: () {
                String hexColor = '#${currentColor.value.toRadixString(16).substring(2).toUpperCase()}';
                setState(() {
                  _appearanceConfig[key] = hexColor;
                });
                _autoSaveConfig();
                Navigator.of(context).pop();
              },
              child: const Text('确定'),
            ),
          ],
        );
      },
    );
  }

  Widget _buildBackgroundTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildSliderField('玻璃效果透明度', 'glassOpacity', 0.0, 1.0, 0.1, theme),
          _buildSwitchField('深色背景', 'darkBackground', theme),
        ],
      ),
    );
  }

  Widget _buildFontTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildDropdownField('字体', 'fontFamily', [
            'Roboto',
            'Arial',
            'Helvetica',
            'Times New Roman',
            'Courier New',
            'Verdana',
          ], theme),
          _buildNumberField('字体大小', 'fontSize', 8, 24, theme),
        ],
      ),
    );
  }

  Widget _buildStyleTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildNumberField('圆角半径', 'cornerRadius', 0, 20, theme),
          _buildNumberField('边框宽度', 'borderWidth', 0, 5, theme),
          _buildNumberField('大按钮尺寸', 'buttonLargeSize', 32, 64, theme),
          _buildNumberField('小按钮尺寸', 'buttonSmallSize', 24, 48, theme),
        ],
      ),
    );
  }

  Widget _buildSliderField(String label, String key, double min, double max, double divisions, ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '$label: ${(_appearanceConfig[key] as double).toStringAsFixed(1)}',
            style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
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
            activeColor: theme.primaryColor,
            inactiveColor: theme.dividerColor,
          ),
        ],
      ),
    );
  }

  Widget _buildSwitchField(String label, String key, ThemeData theme) {
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
          title: Text(label, style: theme.textTheme.bodyMedium),
          value: getValue(),
          onChanged: (value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          },
          activeColor: theme.primaryColor,
        ),
      );
    } catch (e) {
      print('构建开关字段失败: $e');
      return Padding(
        padding: const EdgeInsets.only(bottom: 20),
        child: ListTile(
          title: Text(label, style: theme.textTheme.bodyMedium),
          subtitle: Text('加载失败: $e', style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
        ),
      );
    }
  }

  Widget _buildDropdownField(String label, String key, List<String> options, ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          DropdownButtonFormField<String>(
            initialValue: _appearanceConfig[key] as String,
            items: options.map((option) {
              return DropdownMenuItem<String>(
                value: option,
                child: Text(option, style: theme.textTheme.bodyMedium),
              );
            }).toList(),
            onChanged: (value) {
              setState(() {
                _appearanceConfig[key] = value;
              });
              _autoSaveConfig();
            },
            decoration: InputDecoration(
              border: OutlineInputBorder(),
              contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              labelStyle: theme.textTheme.bodyMedium,
              hintStyle: theme.textTheme.bodySmall?.copyWith(color: theme.hintColor),
            ),
            dropdownColor: theme.colorScheme.surface,
            style: theme.textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }

  Widget _buildNumberField(String label, String key, int min, int max, ThemeData theme) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: TextEditingController(text: _appearanceConfig[key].toString()),
            keyboardType: TextInputType.number,
            decoration: InputDecoration(
              border: OutlineInputBorder(),
              contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              labelStyle: theme.textTheme.bodyMedium,
              hintStyle: theme.textTheme.bodySmall?.copyWith(color: theme.hintColor),
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
