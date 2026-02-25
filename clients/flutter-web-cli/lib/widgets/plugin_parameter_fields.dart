import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/plugin_parameter.dart';

class PluginParameterFields {
  static Widget buildParameterField(
    PluginParameter param,
    dynamic value,
    Map<String, dynamic> formValues,
    Map<String, TextEditingController> controllers,
    Map<String, List<String>> listValues,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
    try {
      if (!_isParameterVisible(param, formValues)) {
        return const SizedBox.shrink();
      }

      switch (param.type) {
        case 'text':
          return _buildTextField(param, value, controllers, onChanged, showTooltip);
        case 'number':
          return _buildNumberField(param, value, controllers, onChanged, showTooltip);
        case 'boolean':
          return _buildBooleanField(param, value, onChanged, showTooltip);
        case 'select':
          return _buildSelectField(param, value, onChanged, showTooltip);
        case 'directory':
          return _buildDirectoryField(param, value, controllers, onChanged, showTooltip);
        case 'file':
          return _buildFileField(param, value, controllers, onChanged, showTooltip);
        case 'list':
          return _buildListField(param, listValues, onChanged, showTooltip);
        default:
          return _buildTextField(param, value, controllers, onChanged, showTooltip);
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

  static bool _isParameterVisible(PluginParameter param, Map<String, dynamic> formValues) {
    if (param.visibilityConditions == null || param.visibilityConditions!.isEmpty) {
      return true;
    }

    for (final condition in param.visibilityConditions!) {
      final dependentValue = formValues[condition['dependentParam']];
      if (dependentValue == condition['expectedValue']) {
        return true;
      }
    }

    return false;
  }

  static TextEditingController _getController(
    String paramName, 
    String initialValue, 
    Map<String, TextEditingController> controllers
  ) {
    if (!controllers.containsKey(paramName)) {
      controllers[paramName] = TextEditingController(text: initialValue);
    }
    return controllers[paramName]!;
  }

  static Widget _buildTextField(
    PluginParameter param,
    dynamic value,
    Map<String, TextEditingController> controllers,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? '', controllers),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => showTooltip(param.label, param.description),
            ),
          ),
        ),
        onChanged: (v) => onChanged(param.name, v, param),
      ),
    );
  }

  static Widget _buildNumberField(
    PluginParameter param,
    dynamic value,
    Map<String, TextEditingController> controllers,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? '', controllers),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => showTooltip(param.label, param.description),
            ),
          ),
        ),
        keyboardType: TextInputType.number,
        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
        onChanged: (v) => onChanged(param.name, int.tryParse(v), param),
      ),
    );
  }

  static Widget _buildBooleanField(
    PluginParameter param,
    dynamic value,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
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
              onChanged: (v) => onChanged(param.name, v, param),
            ),
          ],
        ),
      ),
    );
  }

  static Widget _buildSelectField(
    PluginParameter param,
    dynamic value,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
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
              onPressed: () => showTooltip(param.label, param.description),
            ),
          ),
        ),
        initialValue: value?.toString(),
        items: param.options?.map((option) {
          return DropdownMenuItem<String>(
            value: option,
            child: Text(option),
          );
        }).toList() ?? [],
        onChanged: (v) => onChanged(param.name, v, param),
      ),
    );
  }

  static Widget _buildDirectoryField(
    PluginParameter param,
    dynamic value,
    Map<String, TextEditingController> controllers,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? '', controllers),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => showTooltip(param.label, param.description),
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

  static Widget _buildFileField(
    PluginParameter param,
    dynamic value,
    Map<String, TextEditingController> controllers,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: TextFormField(
        controller: _getController(param.name, value?.toString() ?? '', controllers),
        decoration: InputDecoration(
          labelText: param.label,
          hintText: param.description,
          border: const OutlineInputBorder(),
          errorText: param.required && (value == null || value.toString().isEmpty) ? '必填项' : null,
          suffixIcon: SizedBox(
            width: 40,
            child: IconButton(
              icon: const Icon(Icons.info_outline),
              onPressed: () => showTooltip(param.label, param.description),
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

  static Widget _buildListField(
    PluginParameter param,
    Map<String, List<String>> listValues,
    Function(String, dynamic, PluginParameter) onChanged,
    Function(String, String) showTooltip
  ) {
    try {
      final listValue = listValues[param.name] ?? [];
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
                  onPressed: () => showTooltip(param.label, param.description),
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
                        // 这里需要通过回调更新父组件的状态
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
                        // 这里需要通过回调更新父组件的状态
                      }
                    },
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  onPressed: () {
                    // 这里需要通过回调更新父组件的状态
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
}
