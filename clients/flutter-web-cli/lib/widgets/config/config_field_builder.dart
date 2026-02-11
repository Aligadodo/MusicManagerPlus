import 'package:flutter/material.dart';
import '../../models/config_field.dart';

abstract class ConfigFieldBuilder {
  Widget build(
    ConfigField field,
    dynamic value,
    Function(dynamic) onChanged,
  );
}

class ConfigFieldBuilderFactory {
  static ConfigFieldBuilder createBuilder(String fieldType) {
    switch (fieldType) {
      case 'text':
        return TextConfigFieldBuilder();
      case 'number':
        return NumberConfigFieldBuilder();
      case 'boolean':
        return BooleanConfigFieldBuilder();
      case 'select':
        return SelectConfigFieldBuilder();
      case 'directory':
        return DirectoryConfigFieldBuilder();
      case 'list':
        return ListConfigFieldBuilder();
      default:
        return TextConfigFieldBuilder();
    }
  }
}

class TextConfigFieldBuilder extends ConfigFieldBuilder {
  @override
  Widget build(
    ConfigField field,
    dynamic value,
    Function(dynamic) onChanged,
  ) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: TextField(
              decoration: InputDecoration(
                border: const OutlineInputBorder(),
                hintText: field.description,
              ),
              controller: TextEditingController(
                text: value?.toString() ?? field.defaultValue?.toString() ?? '',
              ),
              onChanged: onChanged,
            ),
          ),
        ],
      ),
    );
  }
}

class NumberConfigFieldBuilder extends ConfigFieldBuilder {
  @override
  Widget build(
    ConfigField field,
    dynamic value,
    Function(dynamic) onChanged,
  ) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: TextField(
              decoration: InputDecoration(
                border: const OutlineInputBorder(),
                hintText: field.description,
              ),
              controller: TextEditingController(
                text: value?.toString() ?? field.defaultValue?.toString() ?? '',
              ),
              keyboardType: TextInputType.number,
              onChanged: (v) {
                final numValue = num.tryParse(v);
                if (numValue != null) {
                  onChanged(numValue);
                }
              },
            ),
          ),
        ],
      ),
    );
  }
}

class BooleanConfigFieldBuilder extends ConfigFieldBuilder {
  @override
  Widget build(
    ConfigField field,
    dynamic value,
    Function(dynamic) onChanged,
  ) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Tooltip(
                  message: field.description ?? '',
                  child: Text(
                    field.label,
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
                Text(
                  field.description ?? '',
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
          Tooltip(
            message: field.description ?? '',
            child: Checkbox(
              value: value ?? field.defaultValue ?? false,
              onChanged: onChanged,
            ),
          ),
        ],
      ),
    );
  }
}

class SelectConfigFieldBuilder extends ConfigFieldBuilder {
  @override
  Widget build(
    ConfigField field,
    dynamic value,
    Function(dynamic) onChanged,
  ) {
    final dropdownItems = _buildDropdownItems(field);
    final itemValues = dropdownItems.map((item) => item.value).toList();
    final currentValue = value?.toString() ?? field.defaultValue?.toString();
    final initialValue = itemValues.contains(currentValue) ? currentValue : (itemValues.isNotEmpty ? itemValues.first : null);

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: DropdownButtonFormField<String>(
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
              ),
              initialValue: initialValue,
              items: dropdownItems,
              onChanged: onChanged,
            ),
          ),
        ],
      ),
    );
  }

  List<DropdownMenuItem<String>> _buildDropdownItems(ConfigField field) {
    if (field.enumOptions != null && field.enumOptions!.isNotEmpty) {
      return field.enumOptions!.map((enumOption) {
        return DropdownMenuItem<String>(
          value: enumOption.value?.toString() ?? enumOption.code,
          child: Text(enumOption.displayName),
        );
      }).toList();
    }
    return [];
  }
}

class DirectoryConfigFieldBuilder extends ConfigFieldBuilder {
  @override
  Widget build(
    ConfigField field,
    dynamic value,
    Function(dynamic) onChanged,
  ) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: TextField(
              decoration: InputDecoration(
                border: const OutlineInputBorder(),
                hintText: field.description,
                suffixIcon: const Icon(Icons.folder),
              ),
              controller: TextEditingController(
                text: value?.toString() ?? field.defaultValue?.toString() ?? '',
              ),
              readOnly: true,
              onTap: () async {
                // TODO: 实现目录选择器
              },
            ),
          ),
        ],
      ),
    );
  }
}

class ListConfigFieldBuilder extends ConfigFieldBuilder {
  @override
  Widget build(
    ConfigField field,
    dynamic value,
    Function(dynamic) onChanged,
  ) {
    List<String> listValue = <String>[];
    if (value != null) {
      if (value is List) {
        try {
          listValue = List<String>.from(value.map((item) => item?.toString() ?? ''));
        } catch (e) {
          listValue = [];
        }
      } else {
        listValue = [value.toString()];
      }
    }

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Tooltip(
            message: field.description ?? '',
            child: Text(
              field.label,
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 5),
          Tooltip(
            message: field.description ?? '',
            child: Container(
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
                        final newValue = List<String>.from(listValue);
                        newValue.removeAt(index);
                        onChanged(newValue);
                      },
                    ),
                  );
                },
              ),
            ),
          ),
          const SizedBox(height: 5),
          Row(
            children: [
              Expanded(
                child: Tooltip(
                  message: field.description ?? '',
                  child: TextField(
                    decoration: const InputDecoration(
                      border: OutlineInputBorder(),
                      hintText: '输入新项...',
                    ),
                    onSubmitted: (v) {
                      if (v.isNotEmpty) {
                        final newValue = List<String>.from(listValue);
                        newValue.add(v);
                        onChanged(newValue);
                      }
                    },
                  ),
                ),
              ),
              const SizedBox(width: 10),
              IconButton(
                icon: const Icon(Icons.add),
                onPressed: () {
                  // TODO: 实现添加逻辑
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}
