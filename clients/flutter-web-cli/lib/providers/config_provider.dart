import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../api/config_service.dart';
import '../main.dart';

class AppConfig {
  final Map<String, dynamic> appearanceConfig;
  final Map<String, dynamic> globalSettings;
  final Map<String, dynamic> pluginConfigs;

  AppConfig({
    Map<String, dynamic>? appearanceConfig,
    Map<String, dynamic>? globalSettings,
    Map<String, dynamic>? pluginConfigs,
  })  : appearanceConfig = appearanceConfig ?? _defaultAppearanceConfig(),
        globalSettings = globalSettings ?? _defaultGlobalSettings(),
        pluginConfigs = pluginConfigs ?? {};

  static Map<String, dynamic> _defaultAppearanceConfig() {
    return {
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
  }

  static Map<String, dynamic> _defaultGlobalSettings() {
    return {
      'previewThreads': 10,
      'executionThreads': 4,
      'threadPoolMode': 'GLOBAL',
      'recursionMode': 'ALL',
      'recursionDepth': 3,
      'minRecursionDepth': 1,
      'maxRecursionDepth': 3,
      'scanFilterList': [
        '*Convert*',
        '*Split*',
        '*System*',
        '*trash*',
        '*Temp*',
        '*tmp*',
        '*cache*',
        '*backup*',
      ],
      'autoRefresh': true,
      'previewLimit': 200,
      'fileTypeTree': {},
      'showTooltips': true,
    };
  }

  AppConfig copyWith({
    Map<String, dynamic>? appearanceConfig,
    Map<String, dynamic>? globalSettings,
    Map<String, dynamic>? pluginConfigs,
  }) {
    return AppConfig(
      appearanceConfig: appearanceConfig ?? this.appearanceConfig,
      globalSettings: globalSettings ?? this.globalSettings,
      pluginConfigs: pluginConfigs ?? this.pluginConfigs,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'appearance': appearanceConfig,
      'globalSettings': globalSettings,
      'pluginConfigs': pluginConfigs,
    };
  }

  factory AppConfig.fromJson(Map<String, dynamic> json) {
    return AppConfig(
      appearanceConfig: json['appearance'] as Map<String, dynamic>? ?? _defaultAppearanceConfig(),
      globalSettings: json['globalSettings'] as Map<String, dynamic>? ?? _defaultGlobalSettings(),
      pluginConfigs: json['pluginConfigs'] as Map<String, dynamic>? ?? {},
    );
  }
}

class ConfigNotifier extends StateNotifier<AppConfig> {
  final ConfigService _configService;

  ConfigNotifier(this._configService) : super(AppConfig());

  Future<void> loadConfig() async {
    try {
      final config = await _configService.getConfig();
      if (config is Map<String, dynamic>) {
        state = AppConfig.fromJson(config);
      }
    } catch (e) {
      print('加载配置失败: $e');
    }
  }

  Future<void> saveConfig() async {
    try {
      await _configService.saveConfig(state.toJson());
    } catch (e) {
      print('保存配置失败: $e');
      rethrow;
    }
  }

  Future<void> resetConfig() async {
    state = AppConfig();
    await saveConfig();
  }

  void updateAppearanceConfig(Map<String, dynamic> newConfig) {
    state = state.copyWith(appearanceConfig: newConfig);
  }

  void updateGlobalSettings(Map<String, dynamic> newSettings) {
    state = state.copyWith(globalSettings: newSettings);
  }

  void updatePluginConfig(String pluginId, Map<String, dynamic> config) {
    final updatedPluginConfigs = Map<String, dynamic>.from(state.pluginConfigs);
    updatedPluginConfigs[pluginId] = config;
    state = state.copyWith(pluginConfigs: updatedPluginConfigs);
  }
}

final configServiceProvider = Provider<ConfigService>((ref) {
  return ConfigService(ref.watch(apiClientProvider));
});

final configProvider = StateNotifierProvider<ConfigNotifier, AppConfig>((ref) {
  return ConfigNotifier(ref.watch(configServiceProvider));
});
