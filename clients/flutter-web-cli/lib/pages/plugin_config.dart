import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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
        _config = config;
        _formValues.clear();
        _listValues.clear();
        
        for (final param in config.parameters) {
          final value = config.configValues[param.name] ?? param.defaultValue;
          _formValues[param.name] = value;
          
          if (param.type == 'list' && value is List) {
            _listValues[param.name] = List<String>.from(value);
          }
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
      final Map<String, dynamic> finalValues = Map.from(_formValues);
      
      _listValues.forEach((key, value) {
        finalValues[key] = value;
      });

      final updatedConfig = PluginConfig(
        configValues: finalValues,
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

  Widget _buildParameterField(PluginParameter param) {
    if (!_isParameterVisible(param)) {
      return const SizedBox.shrink();
    }

    final value = _formValues[param.name];

    switch (param.type) {
      case 'text':
        return _buildTextField(param, value);
      case 'number':
        return _buildNumberField(param, value);
      case 'boolean':
        return _buildBooleanField(param, value);
      case 'select':
        return _buildSelectField(param, value);
      case 'directory':
        return _buildDirectoryField(param, value);
      case 'file':
        return _buildFileField(param, value);
      case 'list':
        return _buildListField(param);
      default:
        return _buildTextField(param, value);
    }
  }

  Widget _buildTextField(PluginParameter param, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? ''),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: param.description != null
              ? IconButton(
                  icon: const Icon(Icons.info_outline),
                  onPressed: () => _showTooltip(param.label, param.description),
                )
              : null,
        ),
        onChanged: (v) => _handleParameterChange(param.name, v, param),
      ),
    );
  }

  Widget _buildNumberField(PluginParameter param, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? ''),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: param.description != null
              ? IconButton(
                  icon: const Icon(Icons.info_outline),
                  onPressed: () => _showTooltip(param.label, param.description),
                )
              : null,
        ),
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        onChanged: (v) => _handleParameterChange(param.name, int.tryParse(v), param),
      ),
    );
  }

  Widget _buildBooleanField(PluginParameter param, dynamic value) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 8.0),
      child: SwitchListTile(
        title: Text(param.label),
        subtitle: param.description != null ? Text(param.description) : null,
        value: value ?? false,
        onChanged: (v) => _handleParameterChange(param.name, v, param),
        secondary: param.description != null
            ? IconButton(
                icon: const Icon(Icons.info_outline),
                onPressed: () => _showTooltip(param.label, param.description),
              )
            : null,
      ),
    );
  }

  Widget _buildSelectField(PluginParameter param, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: DropdownButtonFormField<String>(
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          suffixIcon: param.description != null
              ? IconButton(
                  icon: const Icon(Icons.info_outline),
                  onPressed: () => _showTooltip(param.label, param.description),
                )
              : null,
        ),
        value: value?.toString(),
        items: param.options?.map((option) {
          return DropdownMenuItem<String>(
            value: option,
            child: Text(option),
          );
        }).toList(),
        onChanged: (v) => _handleParameterChange(param.name, v, param),
      ),
    );
  }

  Widget _buildDirectoryField(PluginParameter param, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Expanded(
            child: TextFormField(
              controller: _getController(param.name, value?.toString() ?? ''),
              decoration: InputDecoration(
                labelText: param.label,
                hintText: param.description,
                border: const OutlineInputBorder(),
                errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
                suffixIcon: param.description != null
                    ? IconButton(
                        icon: const Icon(Icons.info_outline),
                        onPressed: () => _showTooltip(param.label, param.description),
                      )
                    : null,
              ),
              onChanged: (v) => _handleParameterChange(param.name, v, param),
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: const Icon(Icons.folder_open),
            onPressed: () => _selectDirectory(param.name),
          ),
        ],
      ),
    );
  }

  Widget _buildFileField(PluginParameter param, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Expanded(
            child: TextFormField(
              controller: _getController(param.name, value?.toString() ?? ''),
              decoration: InputDecoration(
                labelText: param.label,
                hintText: param.description,
                border: const OutlineInputBorder(),
                errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
                suffixIcon: param.description != null
                    ? IconButton(
                        icon: const Icon(Icons.info_outline),
                        onPressed: () => _showTooltip(param.label, param.description),
                      )
                    : null,
              ),
              onChanged: (v) => _handleParameterChange(param.name, v, param),
            ),
          ),
          const SizedBox(width: 8),
          IconButton(
            icon: const Icon(Icons.file_open),
            onPressed: () => _selectFile(param.name),
          ),
        ],
      ),
    );
  }

  Widget _buildListField(PluginParameter param) {
    final items = _listValues[param.name] ?? [];
    final controller = TextEditingController();

    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 8.0),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    param.label,
                    style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                ),
                if (param.description != null)
                  IconButton(
                    icon: const Icon(Icons.info_outline),
                    onPressed: () => _showTooltip(param.label, param.description),
                  ),
              ],
            ),
            if (param.description != null)
              Text(
                param.description,
                style: const TextStyle(fontSize: 14, color: Colors.grey),
              ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: controller,
                    decoration: InputDecoration(
                      hintText: '输入${param.label}...',
                      border: const OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: () {
                    final value = controller.text.trim();
                    if (value.isNotEmpty && !items.contains(value)) {
                      setState(() {
                        _listValues[param.name] = [...items, value];
                      });
                      controller.clear();
                    }
                  },
                  child: const Text('添加'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (items.isEmpty)
              const Text(
                '暂无项目',
                style: TextStyle(color: Colors.grey),
              )
            else
              ...items.asMap().entries.map((entry) {
                final index = entry.key;
                final item = entry.value;
                return ListTile(
                  title: Text(item),
                  trailing: IconButton(
                    icon: const Icon(Icons.delete, color: Colors.red),
                    onPressed: () {
                      setState(() {
                        _listValues[param.name] = List.from(items)..removeAt(index);
                      });
                    },
                  ),
                );
              }).toList(),
          ],
        ),
      ),
    );
  }

  TextEditingController _getController(String paramName, String initialValue) {
    if (!_controllers.containsKey(paramName)) {
      _controllers[paramName] = TextEditingController(text: initialValue);
    }
    return _controllers[paramName]!;
  }

  Future<void> _selectDirectory(String paramName) async {
    // TODO: 实现目录选择功能
  }

  Future<void> _selectFile(String paramName) async {
    // TODO: 实现文件选择功能
  }

  void _showTooltip(String title, String content) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(content),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
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
            if (group.description != null)
              Text(
                group.description!,
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
    return ListTile(
      title: Text(precondition.field),
      subtitle: Text('${precondition.operator} ${precondition.value}'),
      trailing: precondition.description != null
          ? IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => _showTooltip(precondition.field, precondition.description!),
            )
          : null,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('${widget.pluginName} 配置'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadConfig,
          ),
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveConfig,
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
                      Text(_error!, style: const TextStyle(color: Colors.red)),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _loadConfig,
                        child: const Text('重试'),
                      ),
                    ],
                  ),
                )
              : _config == null
                  ? const Center(child: Text('未找到配置'))
                  : SingleChildScrollView(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            '参数配置',
                            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 16),
                          ..._config!.parameters.map((param) => _buildParameterField(param)),
                          if (_config!.preconditionGroups.isNotEmpty) ...[
                            const SizedBox(height: 24),
                            const Text(
                              '前置条件',
                              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                            ),
                            const SizedBox(height: 16),
                            ..._config!.preconditionGroups.map((group) => _buildPreconditionGroup(group)),
                          ],
                        ],
                      ),
                    ),
    );
  }
}