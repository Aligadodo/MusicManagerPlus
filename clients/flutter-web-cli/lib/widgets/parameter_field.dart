import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/config_field.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';

class ParameterField extends StatefulWidget {
  final ConfigField field;
  final StrategyConfig? strategyConfig;
  final Function(String, dynamic) onValueChanged;

  const ParameterField({
    super.key,
    required this.field,
    required this.strategyConfig,
    required this.onValueChanged,
  });

  @override
  State<ParameterField> createState() => _ParameterFieldState();
}

class _ParameterFieldState extends State<ParameterField> {
  void _updateConfigValue(String fieldName, dynamic value) {
    widget.onValueChanged(fieldName, value);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: Colors.grey.shade200),
      ),
      padding: const EdgeInsets.all(10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildLabel(),
          const SizedBox(height: 8),
          _buildInput(),
        ],
      ),
    );
  }

  Widget _buildLabel() {
    return Row(
      children: [
        Text(
          widget.field.label,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
            color: Colors.grey.shade800,
          ),
        ),
        if (widget.field.description.isNotEmpty) ...[
          const SizedBox(width: 6),
          Tooltip(
            message: widget.field.description,
            child: Icon(
              Icons.help_outline,
              color: Colors.grey.shade500,
              size: 14,
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildInput() {
    final fieldType = widget.field.type ?? 'string';
    final fieldValue = widget.strategyConfig?.getValue(widget.field.name) ?? widget.field.defaultValue;

    switch (fieldType) {
      case 'string':
        return _buildStringInput(fieldValue);
      case 'number':
        return _buildNumberInput(fieldValue);
      case 'boolean':
        return _buildBooleanInput(fieldValue);
      case 'directory':
        return _buildDirectoryInput(fieldValue);
      case 'select':
        return _buildSelectInput(fieldValue);
      case 'list':
        return _buildListInput(fieldValue);
      case 'enum':
        return _buildEnumInput(fieldValue);
      default:
        return _buildStringInput(fieldValue);
    }
  }

  Widget _buildStringInput(dynamic fieldValue) {
    return TextField(
      decoration: InputDecoration(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4),
          borderSide: const BorderSide(color: Colors.blue, width: 1.5),
        ),
        hintText: widget.field.defaultValue?.toString(),
        hintStyle: TextStyle(color: Colors.grey.shade400),
        contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        isDense: true,
      ),
      controller: TextEditingController(text: fieldValue?.toString() ?? ''),
      style: const TextStyle(fontSize: 12),
      onChanged: (value) {
        _updateConfigValue(widget.field.name, value);
      },
    );
  }

  Widget _buildNumberInput(dynamic fieldValue) {
    return TextField(
      keyboardType: TextInputType.number,
      decoration: InputDecoration(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4),
          borderSide: const BorderSide(color: Colors.blue, width: 1.5),
        ),
        hintText: widget.field.defaultValue?.toString(),
        hintStyle: TextStyle(color: Colors.grey.shade400),
        contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        isDense: true,
      ),
      controller: TextEditingController(text: fieldValue?.toString() ?? ''),
      style: const TextStyle(fontSize: 12),
      onChanged: (value) {
        _updateConfigValue(widget.field.name, int.tryParse(value) ?? 0);
      },
    );
  }

  Widget _buildBooleanInput(dynamic fieldValue) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          Checkbox(
            value: fieldValue == true,
            onChanged: (value) {
              _updateConfigValue(widget.field.name, value ?? false);
            },
            activeColor: Colors.blue,
            checkColor: Colors.white,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
          const SizedBox(width: 6),
          Text(
            '启用',
            style: TextStyle(color: Colors.grey.shade700, fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildDirectoryInput(dynamic fieldValue) {
    return Row(
      children: [
        Expanded(
          child: TextField(
            decoration: InputDecoration(
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(4),
                borderSide: BorderSide(color: Colors.grey.shade300),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(4),
                borderSide: BorderSide(color: Colors.grey.shade300),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(4),
                borderSide: const BorderSide(color: Colors.blue, width: 1.5),
              ),
              hintText: widget.field.defaultValue?.toString(),
              hintStyle: TextStyle(color: Colors.grey.shade400),
              contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              isDense: true,
            ),
            controller: TextEditingController(text: fieldValue?.toString() ?? ''),
            style: const TextStyle(fontSize: 12),
            onChanged: (value) {
              _updateConfigValue(widget.field.name, value);
            },
          ),
        ),
        const SizedBox(width: 8),
        Container(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: Colors.grey.shade300),
          ),
          child: IconButton(
            icon: const Icon(Icons.folder_open, color: Colors.blue, size: 18),
            onPressed: () {},
            padding: const EdgeInsets.all(6),
            constraints: const BoxConstraints(),
          ),
        ),
      ],
    );
  }

  Widget _buildSelectInput(dynamic fieldValue) {
    if (widget.field.options != null && widget.field.options!.isNotEmpty) {
      return Container(
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey.shade300),
          borderRadius: BorderRadius.circular(4),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 8),
        child: DropdownButton<String>(
          value: fieldValue?.toString(),
          items: widget.field.options!.map((option) {
            return DropdownMenuItem<String>(
              value: option,
              child: Text(option, style: TextStyle(color: Colors.grey.shade700, fontSize: 12)),
            );
          }).toList(),
          onChanged: (value) {
            _updateConfigValue(widget.field.name, value);
          },
          isExpanded: true,
          underline: const SizedBox(),
          icon: Icon(Icons.keyboard_arrow_down, color: Colors.grey.shade600, size: 18),
          hint: Text('请选择...', style: TextStyle(color: Colors.grey.shade400, fontSize: 12)),
          isDense: true,
        ),
      );
    }
    return _buildStringInput(fieldValue);
  }

  Widget _buildEnumInput(dynamic fieldValue) {
    if (widget.field.enumOptions != null && widget.field.enumOptions!.isNotEmpty) {
      return Container(
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey.shade300),
          borderRadius: BorderRadius.circular(4),
        ),
        padding: const EdgeInsets.symmetric(horizontal: 8),
        child: DropdownButton<String>(
          value: fieldValue?.toString(),
          items: widget.field.enumOptions!.map((option) {
            return DropdownMenuItem<String>(
              value: option.code,
              child: Text(option.displayName, style: TextStyle(color: Colors.grey.shade700, fontSize: 12)),
            );
          }).toList(),
          onChanged: (value) {
            _updateConfigValue(widget.field.name, value);
          },
          isExpanded: true,
          underline: const SizedBox(),
          icon: Icon(Icons.keyboard_arrow_down, color: Colors.grey.shade600, size: 18),
          hint: Text('请选择...', style: TextStyle(color: Colors.grey.shade400, fontSize: 12)),
          isDense: true,
        ),
      );
    }
    return _buildStringInput(fieldValue);
  }

  Widget _buildListInput(dynamic fieldValue) {
    List<String> items = [];
    try {
      if (fieldValue is List) {
        items = (fieldValue as List).map((item) => item?.toString() ?? '').toList();
      }
    } catch (e) {
      items = [];
    }

    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey.shade300),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '列表项 (${items.length})',
                style: const TextStyle(fontSize: 11, color: Colors.grey),
              ),
              ElevatedButton.icon(
                onPressed: () => _showAddListItemDialog(items),
                icon: const Icon(Icons.add, size: 14),
                label: const Text('添加项目', style: TextStyle(fontSize: 11)),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  minimumSize: const Size(0, 0),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ),
            ],
          ),
          if (items.isNotEmpty) ...[
            const SizedBox(height: 8),
            ...items.asMap().entries.map((entry) {
              final index = entry.key;
              final item = entry.value;
              return Container(
                margin: const EdgeInsets.only(bottom: 4),
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.grey.shade50,
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: Colors.grey.shade200),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        item,
                        style: const TextStyle(fontSize: 12),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.edit, size: 14, color: Colors.blue),
                      onPressed: () => _showEditListItemDialog(items, index),
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                    ),
                    const SizedBox(width: 4),
                    IconButton(
                      icon: const Icon(Icons.delete, size: 14, color: Colors.red),
                      onPressed: () => _removeListItem(items, index),
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                    ),
                  ],
                ),
              );
            }).toList(),
          ],
        ],
      ),
    );
  }

  void _showAddListItemDialog(List<String> items) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('添加列表项', style: TextStyle(fontSize: 14)),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: '请输入项目内容',
            border: OutlineInputBorder(),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                final newItems = List<String>.from(items);
                newItems.add(controller.text);
                _updateConfigValue(widget.field.name, newItems);
                Navigator.pop(context);
              }
            },
            child: const Text('添加'),
          ),
        ],
      ),
    );
  }

  void _showEditListItemDialog(List<String> items, int index) {
    final controller = TextEditingController(text: items[index]);
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('编辑项目', style: TextStyle(fontSize: 14)),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: '请输入内容',
            border: OutlineInputBorder(),
          ),
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () {
              if (controller.text.isNotEmpty) {
                final newItems = List<String>.from(items);
                newItems[index] = controller.text;
                _updateConfigValue(widget.field.name, newItems);
                Navigator.pop(context);
              }
            },
            child: const Text('保存'),
          ),
        ],
      ),
    );
  }

  void _removeListItem(List<String> items, int index) {
    final newItems = List<String>.from(items);
    newItems.removeAt(index);
    _updateConfigValue(widget.field.name, newItems);
  }
}
