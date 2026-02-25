import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../api/plugin_service.dart';
import '../models/plugin_config.dart';
import '../models/plugin_parameter.dart';
import './plugin_parameter_fields.dart';
import './precondition_builder.dart';

class PluginConfigPage extends StatefulWidget {
  final String pluginId;
  final String pluginName;

  const PluginConfigPage({super.key, required this.pluginId, required this.pluginName});

  @override
  State<PluginConfigPage> createState() => _PluginConfigPageState();
}

class _PluginConfigPageState extends State<PluginConfigPage> {
  final PluginService _pluginService = PluginService(ApiClient());
  PluginConfig? _config;
  bool _isLoading = false;
  String? _error;
  final Map<String, dynamic> _formValues = {};
  final Map<String, TextEditingController> _controllers = {};
  final Map<String, List<String>> _listValues = {};

  @override
  void initState() {
    super.initState();
    _loadConfig();
  }

  @override
  void dispose() {
    _controllers.forEach((key, controller) => controller.dispose());
    super.dispose();
  }

  Future<void> _loadConfig() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final config = await _pluginService.getPluginConfig(widget.pluginId);
      setState(() {
        try {
          _config = config;
          _formValues.clear();
          _listValues.clear();

          final parameters = config.parameters ?? [];
          for (final param in parameters) {
            try {
              final value = config.configValues[param.name] ?? param.defaultValue;
              _formValues[param.name] = value;

              if (param.type == 'list' && value is List) {
                _listValues[param.name] = List<String>.from(value);
              }
            } catch (e) {
              print('初始化参数 ${param.name} 失败: $e');
            }
          }
          _isLoading = false;
        } catch (e) {
          _error = '解析配置失败: $e';
          _isLoading = false;
        }
      });
    } catch (e) {
      setState(() {
        _error = '加载配置失败: $e';
        _isLoading = false;
      });
    }
  }

  bool _isParameterVisible(PluginParameter param) {
    if (param.visibilityConditions == null || param.visibilityConditions!.isEmpty) {
      return true;
    }

    for (final condition in param.visibilityConditions!) {
      final dependentValue = _formValues[condition['dependentParam']];
      if (dependentValue == condition['expectedValue']) {
        return true;
      }
    }

    return false;
  }

  void _handleParameterChange(String paramName, dynamic value, PluginParameter param) {
    setState(() {
      _formValues[paramName] = value;

      if (param.exclusiveParams != null) {
        for (final exclusiveParam in param.exclusiveParams!) {
          if (value == true && exclusiveParam['condition'] == 'when_true') {
            _formValues[exclusiveParam['name']] = false;
          } else if (value == false && exclusiveParam['condition'] == 'when_false') {
            _formValues[exclusiveParam['name']] = true;
          }
        }
      }

      if (param.autoDetectParams != null && value is String) {
        _handleAutoDetect(param.autoDetectParams!, value);
      }
    });
  }

  void _handleAutoDetect(Map<String, dynamic> autoDetectParams, String selectedValue) {
    if (autoDetectParams['triggerValues'] != null &&
        (autoDetectParams['triggerValues'] as List).contains(selectedValue)) {
      final paths = autoDetectParams['paths'] as List<String>;
      for (final path in paths) {
        _formValues[autoDetectParams['targetParam']] = path;
        break;
      }
    }
  }

  TextEditingController _getController(String paramName, String initialValue) {
    if (!_controllers.containsKey(paramName)) {
      _controllers[paramName] = TextEditingController(text: initialValue);
    }
    return _controllers[paramName]!;
  }

  void _showTooltip(String title, String description) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(description),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    try {
      return Scaffold(
        appBar: AppBar(
          title: Text('${widget.pluginName} 配置'),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _loadConfig,
            ),
          ],
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.error_outline, size: 48, color: Colors.red),
                        const SizedBox(height: 16),
                        Text(
                          _error!,
                          style: const TextStyle(color: Colors.red),
                        ),
                        const SizedBox(height: 16),
                        ElevatedButton(
                          onPressed: _loadConfig,
                          child: const Text('重试'),
                        ),
                      ],
                    ),
                  )
                : _config == null
                    ? const Center(child: Text('无配置'))
                    : _buildConfigContent(),
      );
    } catch (e) {
      return Scaffold(
        appBar: AppBar(
          title: Text('${widget.pluginName} 配置'),
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
                onPressed: _loadConfig,
                child: const Text('重新加载'),
              ),
            ],
          ),
        ),
      );
    }
  }

  Widget _buildConfigContent() {
    try {
      return SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '参数配置',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            ..._config!.parameters.map((param) => 
              PluginParameterFields.buildParameterField(
                param, 
                _formValues[param.name], 
                _listValues,
                (name, value) => _handleParameterChange(name, value, param),
                _getController,
                _showTooltip,
                _isParameterVisible(param)
              )
            ),
            const SizedBox(height: 32),
            const Text(
              '前置条件',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
            if (_config!.preconditionGroups.isEmpty)
              const Text('暂无前置条件')
            else
              ..._config!.preconditionGroups.asMap().entries.map((entry) =>
                  PreconditionBuilder.buildPreconditionGroup(
                    entry.value, 
                    entry.key + 1, 
                    () => setState(() {})
                  )
              ),
          ],
        ),
      );
    } catch (e) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 16),
            Text(
              '配置内容加载失败: $e',
              style: const TextStyle(color: Colors.red),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadConfig,
              child: const Text('重新加载'),
            ),
          ],
        ),
      );
    }
  }
}
