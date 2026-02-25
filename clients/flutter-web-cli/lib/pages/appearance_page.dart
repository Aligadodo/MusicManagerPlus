import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/api_client.dart';
import '../api/config_service.dart';
import '../providers/config_provider.dart';
import '../utils/ui_utils.dart';
import '../widgets/selectable_text_widget.dart';
import '../widgets/appearance_settings_fields.dart';
import '../widgets/preset_manager.dart';
import '../widgets/preset_operations.dart';

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
            SnackBar(
              content: SelectableTextWidget(
                text: '加载主题失败: $fallbackError',
                style: const TextStyle(color: Colors.white),
                maxLines: 5,
              ),
              backgroundColor: Colors.red,
              duration: const Duration(seconds: 5),
            ),
          );
        }
      }
    }
  }

  Future<void> _applyPreset(Map<String, dynamic> preset) async {
    final operations = PresetOperations(
      context: context,
      ref: ref,
      configService: _configService,
      appearanceConfig: _appearanceConfig,
      onLoadingChanged: (loading) {
        setState(() {
          _isLoading = loading;
        });
      },
      onRefreshPresets: _loadThemePresets,
    );
    await operations.applyPreset(preset);
  }

  Future<void> _saveAsPreset() async {
    final operations = PresetOperations(
      context: context,
      ref: ref,
      configService: _configService,
      appearanceConfig: _appearanceConfig,
      onLoadingChanged: (loading) {
        setState(() {
          _isLoading = loading;
        });
      },
      onRefreshPresets: _loadThemePresets,
    );
    await operations.saveAsPreset(_appearanceConfig);
  }

  Future<void> _editTheme(Map<String, dynamic> preset) async {
    final operations = PresetOperations(
      context: context,
      ref: ref,
      configService: _configService,
      appearanceConfig: _appearanceConfig,
      onLoadingChanged: (loading) {
        setState(() {
          _isLoading = loading;
        });
      },
      onRefreshPresets: _loadThemePresets,
    );
    await operations.editTheme(preset);
  }

  Future<void> _deleteTheme(String? themeId) async {
    final operations = PresetOperations(
      context: context,
      ref: ref,
      configService: _configService,
      appearanceConfig: _appearanceConfig,
      onLoadingChanged: (loading) {
        setState(() {
          _isLoading = loading;
        });
      },
      onRefreshPresets: _loadThemePresets,
    );
    await operations.deleteTheme(themeId);
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
    return PresetManager.buildPresetTabContent(
      _themePresets,
      theme,
      _appearanceConfig,
      _applyPreset,
      _editTheme,
      _deleteTheme,
      _saveAsPreset,
    );
  }

  Widget _buildColorTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AppearanceSettingsFields.buildColorField('背景颜色', 'bgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('强调色', 'accentColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('主文本颜色', 'textPrimaryColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('次要文本颜色', 'textSecondaryColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('第三文本颜色', 'textTertiaryColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('禁用文本颜色', 'textDisabledColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('面板背景颜色', 'panelBgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表背景颜色', 'listBgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表偶数行颜色', 'listRowEvenBgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表奇数行颜色', 'listRowOddBgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表选中行颜色', 'listRowSelectedBgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表选中行文本颜色', 'listRowSelectedTextColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表悬停行颜色', 'listRowHoverBgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表边框颜色', 'listBorderColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表表头背景颜色', 'listHeaderBgColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('列表表头文本颜色', 'listHeaderTextColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildColorField('边框颜色', 'borderColor', theme, _appearanceConfig, context, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
        ],
      ),
    );
  }

  Widget _buildBackgroundTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AppearanceSettingsFields.buildSliderField('玻璃效果透明度', 'glassOpacity', 0.0, 1.0, 0.1, theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildSwitchField('深色背景', 'darkBackground', theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
        ],
      ),
    );
  }

  Widget _buildFontTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AppearanceSettingsFields.buildDropdownField('字体', 'fontFamily', [
            'Roboto',
            'Arial',
            'Helvetica',
            'Times New Roman',
            'Courier New',
            'Verdana',
          ], theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildNumberField('字体大小', 'fontSize', 8, 24, theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
        ],
      ),
    );
  }

  Widget _buildStyleTabContent(ThemeData theme) {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AppearanceSettingsFields.buildNumberField('圆角半径', 'cornerRadius', 0, 20, theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildNumberField('边框宽度', 'borderWidth', 0, 5, theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildNumberField('大按钮尺寸', 'buttonLargeSize', 32, 64, theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
          AppearanceSettingsFields.buildNumberField('小按钮尺寸', 'buttonSmallSize', 24, 48, theme, _appearanceConfig, (key, value) {
            setState(() {
              _appearanceConfig[key] = value;
            });
            _autoSaveConfig();
          }),
        ],
      ),
    );
  }
}
