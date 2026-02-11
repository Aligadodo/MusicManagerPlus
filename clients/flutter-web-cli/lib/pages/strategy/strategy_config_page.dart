import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../api/api_client.dart';
import '../../api/strategy_service.dart';
import '../../api/enum_service.dart';
import '../../models/strategy_info.dart';
import '../../models/strategy_config.dart';
import '../../models/enum_option.dart';
import 'strategy_list_panel.dart';
import 'strategy_config_panel.dart';

class StrategyConfigPage extends ConsumerStatefulWidget {
  const StrategyConfigPage({super.key});

  @override
  ConsumerState<StrategyConfigPage> createState() => _StrategyConfigPageState();
}

class _StrategyConfigPageState extends ConsumerState<StrategyConfigPage> {
  late ApiClient _apiClient;
  late StrategyService _strategyService;
  late EnumService _enumService;
  List<StrategyInfo> _strategies = [];
  bool _isLoading = false;
  String _errorMessage = '';
  StrategyInfo? _selectedStrategy;
  StrategyConfig? _strategyConfig;
  Map<String, List<EnumOption>> _enumOptionsCache = {};

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient();
    _strategyService = StrategyService(_apiClient);
    _enumService = EnumService(_apiClient);
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
      print('Strategy config fields count: ${strategy.configFields.length}');
      for (var field in strategy.configFields) {
        print('Field: ${field.name}, Type: ${field.type}');
      }
      
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

  Future<void> _saveStrategyConfig() async {
    if (_selectedStrategy == null || _strategyConfig == null) {
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = '';
    });

    try {
      print('Saving strategy config for: ${_selectedStrategy!.id}');
      print('Config values: ${_strategyConfig!.configValues}');
      
      await _strategyService.updateStrategyConfig(
        _selectedStrategy!.id,
        _strategyConfig!,
      );
      
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('策略配置保存成功')),
      );
      
      print('Strategy config saved successfully');
    } catch (e, stackTrace) {
      print('Error saving strategy config: $e');
      print('Stack trace: $stackTrace');
      setState(() {
        _errorMessage = '保存策略配置失败: $e';
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('保存失败: $e')),
      );
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
          actions: [
            if (_selectedStrategy != null && _strategyConfig != null)
              IconButton(
                icon: const Icon(Icons.save),
                onPressed: _saveStrategyConfig,
                tooltip: '保存配置',
              ),
          ],
        ),
        body: Container(
          padding: const EdgeInsets.all(20.0),
          child: Row(
            children: [
              StrategyListPanel(
                strategies: _strategies,
                isLoading: _isLoading && _strategies.isEmpty,
                selectedStrategy: _selectedStrategy,
                onStrategySelected: _loadStrategyConfig,
              ),
              StrategyConfigPanel(
                selectedStrategy: _selectedStrategy,
                strategyConfig: _strategyConfig,
                isLoading: _isLoading,
                errorMessage: _errorMessage,
                onConfigChanged: (config) {
                  setState(() {
                    _strategyConfig = config;
                  });
                },
              ),
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
}
