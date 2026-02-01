import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../api/api_client.dart';
import '../api/plugin_service.dart';
import '../models/plugin_config.dart';
import '../models/plugin_parameter.dart';
import '../models/rule_condition_group.dart';
import '../models/rule_condition.dart';
import '../models/condition_type.dart';

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

  Widget _buildParameterField(PluginParameter param) {
    try {
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
    } catch (e) {
      return Card(
        color: Colors.red.shade50,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.red),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '参数 ${param.name} 加载失败',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red),
                    ),
                    Text(
                      '错误: $e',
                      style: const TextStyle(fontSize: 12, color: Colors.red),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
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
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => _showTooltip(param.label, param.description),
            ),
          ),
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
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => _showTooltip(param.label, param.description),
            ),
          ),
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
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    param.label,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  Text(
                    param.description,
                    style: const TextStyle(fontSize: 12),
                  ),
                ],
              ),
            ),
            Checkbox(
              value: value ?? false,
              onChanged: (v) => _handleParameterChange(param.name, v, param),
            ),
          ],
        ),
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
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => _showTooltip(param.label, param.description),
            ),
          ),
        ),
        value: value?.toString(),
        items: param.options?.map((option) {
          return DropdownMenuItem<String>(
            value: option,
            child: Text(option),
          );
        }).toList() ?? [],
        onChanged: (v) => _handleParameterChange(param.name, v, param),
      ),
    );
  }

  Widget _buildDirectoryField(PluginParameter param, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? ''),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => _showTooltip(param.label, param.description),
            ),
          ),
        ),
        readOnly: true,
        onTap: () async {
          // TODO: 实现目录选择器
        },
      ),
    );
  }

  Widget _buildFileField(PluginParameter param, dynamic value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? ''),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => _showTooltip(param.label, param.description),
            ),
          ),
        ),
        readOnly: true,
        onTap: () async {
          // TODO: 实现文件选择器
        },
      ),
    );
  }

  Widget _buildListField(PluginParameter param) {
    try {
      final listValue = _listValues[param.name] ?? [];
      return Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  param.label,
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                const SizedBox(width: 10),
                IconButton(
                  icon: const Icon(Icons.info_outline),
                  onPressed: () => _showTooltip(param.label, param.description),
                ),
              ],
            ),
            const SizedBox(height: 5),
            Container(
              height: 100,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey),
                borderRadius: BorderRadius.circular(4),
              ),
              child: ListView.builder(
                itemCount: listValue.length,
                itemBuilder: (context, index) {
                  return ListTile(
                    title: Text(listValue[index]),
                    trailing: IconButton(
                      icon: const Icon(Icons.delete, color: Colors.red),
                      onPressed: () {
                        setState(() {
                          _listValues[param.name] = List.from(listValue)..removeAt(index);
                        });
                      },
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 5),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: const InputDecoration(
                      border: OutlineInputBorder(),
                      hintText: '输入新项...',
                    ),
                    onSubmitted: (v) {
                      if (v.isNotEmpty) {
                        setState(() {
                          _listValues[param.name] = List.from(listValue)..add(v);
                        });
                      }
                    },
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    setState(() {
                      _listValues[param.name] = List.from(listValue)..add('新项');
                    });
                  },
                  child: const Text('添加'),
                ),
              ],
            ),
          ],
        ),
      );
    } catch (e) {
      return Card(
        color: Colors.red.shade50,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.red),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '列表参数 ${param.name} 加载失败',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red),
                    ),
                    Text(
                      '错误: $e',
                      style: const TextStyle(fontSize: 12, color: Colors.red),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
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

  Widget _buildPreconditionGroup(RuleConditionGroup group, int index) {
    try {
      return Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          border: Border.all(color: const Color(0xFFBDBDBD)),
          borderRadius: BorderRadius.circular(4),
          color: Colors.white.withOpacity(0.4),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text(
                  '条件组 $index (一组条件内为且)',
                  style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF616161)),
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close, color: Colors.red),
                  onPressed: () {
                    setState(() {
                      _config!.preconditionGroups.remove(group);
                    });
                  },
                ),
              ],
            ),
            const SizedBox(height: 8),
            ...group.conditions.map((condition) => _buildConditionItem(group, condition)),
            const SizedBox(height: 8),
            _buildAddConditionForm(group),
          ],
        ),
      );
    } catch (e) {
      return Card(
        color: Colors.red.shade50,
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.red),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '条件组 $index 加载失败',
                      style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.red),
                    ),
                    Text(
                      '错误: $e',
                      style: const TextStyle(fontSize: 12, color: Colors.red),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      );
    }
  }

  Widget _buildConditionItem(RuleConditionGroup group, RuleCondition condition) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          const Text('• ', style: TextStyle(fontSize: 16)),
          Expanded(
            child: Text(
              condition.toString(),
              style: const TextStyle(color: Color(0xFF424242)),
            ),
          ),
          const SizedBox(width: 5),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) {
              switch (value) {
                case 'move_up':
                  setState(() {
                    final index = group.conditions.indexOf(condition);
                    if (index > 0) {
                      group.conditions.removeAt(index);
                      group.conditions.insert(index - 1, condition);
                    }
                  });
                  break;
                case 'move_down':
                  setState(() {
                    final index = group.conditions.indexOf(condition);
                    if (index < group.conditions.length - 1) {
                      group.conditions.removeAt(index);
                      group.conditions.insert(index + 1, condition);
                    }
                  });
                  break;
                case 'delete':
                  setState(() {
                    group.conditions.remove(condition);
                  });
                  break;
              }
            },
            itemBuilder: (context) => [
              const PopupMenuItem(
                value: 'move_up',
                child: Text('上移'),
              ),
              const PopupMenuItem(
                value: 'move_down',
                child: Text('下移'),
              ),
              const PopupMenuItem(
                value: 'delete',
                child: Text('删除'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAddConditionForm(RuleConditionGroup group) {
    ConditionType selectedType = ConditionType.contains;
    String valueText = '';

    return StatefulBuilder(
      builder: (context, setState) {
        return Row(
          children: [
            DropdownButton<ConditionType>(
              value: selectedType,
              items: ConditionType.values.map((type) {
                return DropdownMenuItem<ConditionType>(
                  value: type,
                  child: Text(type.description),
                );
              }).toList(),
              onChanged: (type) {
                setState(() {
                  selectedType = type ?? ConditionType.contains;
                });
              },
            ),
            const SizedBox(width: 5),
            Expanded(
              child: TextField(
                enabled: selectedType.needsValue(),
                decoration: const InputDecoration(
                  border: OutlineInputBorder(),
                  hintText: '值',
                  contentPadding: EdgeInsets.symmetric(horizontal: 8, vertical: 8),
                ),
                onChanged: (v) {
                  valueText = v;
                },
              ),
            ),
            const SizedBox(width: 5),
            ElevatedButton(
              onPressed: () {
                if (!selectedType.needsValue() || valueText.isNotEmpty) {
                  setState(() {
                    group.conditions.add(RuleCondition(
                      type: selectedType,
                      value: selectedType.needsValue() ? valueText : null,
                    ));
                  });
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF2980B9),
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              ),
              child: const Text('添加条件'),
            ),
          ],
        );
      },
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
            ..._config!.parameters.map((param) => _buildParameterField(param)),
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
                  _buildPreconditionGroup(entry.value, entry.key + 1)),
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
