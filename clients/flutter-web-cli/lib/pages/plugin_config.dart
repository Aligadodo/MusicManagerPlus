import 'package:flutter/material.dart';
import '../api/api_client.dart';
import '../api/plugin_service.dart';
import '../models/plugin_config.dart';
import '../models/plugin_parameter.dart';
import '../models/precondition_group.dart';
import '../models/precondition.dart';

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

  @override
  void initState() {
    super.initState();
    _loadConfig();
  }

  Future<void> _loadConfig() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final config = await _pluginService.getPluginConfig(widget.pluginId);
      setState(() {
        _config = config;
        // 初始化表单值
        _formValues.clear();
        for (final param in config.parameters) {
          _formValues[param.name] = config.configValues[param.name] ?? param.defaultValue;
        }
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = '加载配置失败: $e';
        _isLoading = false;
      });
    }
  }

  Future<void> _saveConfig() async {
    if (_config == null) return;

    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final updatedConfig = PluginConfig(
        configValues: _formValues,
        parameters: _config!.parameters,
        preconditionGroups: _config!.preconditionGroups,
      );
      await _pluginService.savePluginConfig(widget.pluginId, updatedConfig);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('配置保存成功')),
      );
      setState(() {
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = '保存配置失败: $e';
        _isLoading = false;
      });
    }
  }

  Widget _buildParameterField(PluginParameter param) {
    final value = _formValues[param.name];

    switch (param.type) {
      case 'text':
        return TextFormField(
          decoration: InputDecoration(
            labelText: param.label,
            hintText: param.description,
            errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          ),
          onChanged: (v) => _formValues[param.name] = v,
          initialValue: value?.toString() ?? '',
        );
      case 'number':
        return TextFormField(
          decoration: InputDecoration(
            labelText: param.label,
            hintText: param.description,
            errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          ),
          keyboardType: TextInputType.number,
          onChanged: (v) => _formValues[param.name] = int.tryParse(v),
          initialValue: value?.toString() ?? '',
        );
      case 'boolean':
        return SwitchListTile(
          title: Text(param.label),
          subtitle: Text(param.description),
          value: value ?? false,
          onChanged: (v) => setState(() => _formValues[param.name] = v),
        );
      case 'select':
        return DropdownButtonFormField<String>(
          decoration: InputDecoration(
            labelText: param.label,
            hintText: param.description,
          ),
          value: value?.toString(),
          items: param.options?.map((option) {
            return DropdownMenuItem<String>(
              value: option,
              child: Text(option),
            );
          }).toList(),
          onChanged: (v) => _formValues[param.name] = v,
        );
      case 'directory':
        return TextFormField(
          decoration: InputDecoration(
            labelText: param.label,
            hintText: param.description,
            errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          ),
          onChanged: (v) => _formValues[param.name] = v,
          initialValue: value?.toString() ?? '',
        );
      case 'file':
        return TextFormField(
          decoration: InputDecoration(
            labelText: param.label,
            hintText: param.description,
            errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          ),
          onChanged: (v) => _formValues[param.name] = v,
          initialValue: value?.toString() ?? '',
        );
      default:
        return TextFormField(
          decoration: InputDecoration(
            labelText: param.label,
            hintText: param.description,
          ),
          onChanged: (v) => _formValues[param.name] = v,
          initialValue: value?.toString() ?? '',
        );
    }
  }

  Widget _buildPreconditionGroup(PreconditionGroup group) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 8),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              group.name,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              group.description,
              style: const TextStyle(
                fontSize: 14,
                color: Colors.grey,
              ),
            ),
            Text(
              '逻辑类型: ${group.logicType}',
              style: const TextStyle(
                fontSize: 12,
                color: Colors.blue,
              ),
            ),
            const SizedBox(height: 8),
            ...group.preconditions.map((precondition) => _buildPrecondition(precondition)),
          ],
        ),
      ),
    );
  }

  Widget _buildPrecondition(Precondition precondition) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  precondition.description,
                  style: const TextStyle(fontSize: 14),
                ),
                Text(
                  '${precondition.field} ${precondition.operator} ${precondition.value}',
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.pluginName} 配置'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (_isLoading)
                const Center(
                  child: CircularProgressIndicator(),
                )
              else if (_error != null)
                Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(_error!, style: const TextStyle(color: Colors.red)),
                      ElevatedButton(
                        onPressed: _loadConfig,
                        child: const Text('重试'),
                      ),
                    ],
                  ),
                )
              else if (_config != null)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '插件参数',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    ..._config!.parameters.map((param) => Padding(
                      padding: const EdgeInsets.only(bottom: 16),
                      child: _buildParameterField(param),
                    )),
                    const SizedBox(height: 24),
                    const Text(
                      '前置条件',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    ..._config!.preconditionGroups.map((group) => _buildPreconditionGroup(group)),
                    const SizedBox(height: 32),
                    Center(
                      child: ElevatedButton(
                        onPressed: _saveConfig,
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 12),
                        ),
                        child: const Text('保存配置'),
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }
}
