import 'package:flutter/material.dart';
import 'package:flutter_colorpicker/flutter_colorpicker.dart';

class AppearanceSettingsFields {
  static Widget buildNavItem(String title, int index, ThemeData theme, int selectedSection, Function(int) onTap) {
    return InkWell(
      onTap: () => onTap(index),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: selectedSection == index ? theme.colorScheme.primary.withOpacity(0.1) : Colors.transparent,
          border: Border(
            bottom: BorderSide(
              color: selectedSection == index ? theme.colorScheme.primary : theme.dividerColor,
              width: selectedSection == index ? 2 : 1,
            ),
          ),
        ),
        child: Text(
          title,
          style: TextStyle(
            color: selectedSection == index ? theme.colorScheme.primary : theme.textTheme.bodyMedium?.color,
            fontWeight: selectedSection == index ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  static Widget buildColorField(String label, String key, ThemeData theme, Map<String, dynamic> config, BuildContext context, Function(String, Color) onChanged) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: theme.textTheme.bodyMedium),
          ),
          Container(
            width: 100,
            height: 40,
            decoration: BoxDecoration(
              color: config[key] as Color? ?? Colors.blue,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: theme.dividerColor),
            ),
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: () => _showColorPicker(context, key, config[key] as Color?, onChanged),
                child: const Center(
                  child: Icon(Icons.colorize, color: Colors.white),
                ),
              ),
            ),
          ),
          const SizedBox(width: 20),
          Text(
            (config[key] as Color?)?.toString() ?? '',
            style: theme.textTheme.bodySmall,
          ),
          const SizedBox(width: 20),
          IconButton(
            icon: const Icon(Icons.restore),
            onPressed: () => onChanged(key, Colors.blue),
            tooltip: '恢复默认',
          ),
        ],
      ),
    );
  }

  static Widget buildSliderField(String label, String key, double min, double max, double divisions, ThemeData theme, Map<String, dynamic> config, Function(String, double) onChanged) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: theme.textTheme.bodyMedium),
          ),
          const SizedBox(width: 20),
          Expanded(
            child: Slider(
              value: (config[key] as num?)?.toDouble() ?? min,
              min: min,
              max: max,
              divisions: divisions.toInt(),
              label: '${config[key]}',
              onChanged: (value) => onChanged(key, value),
              activeColor: theme.colorScheme.primary,
            ),
          ),
          const SizedBox(width: 20),
          Text(
            '${config[key]}',
            style: theme.textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }

  static Widget buildSwitchField(String label, String key, ThemeData theme, Map<String, dynamic> config, Function(String, bool) onChanged) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: theme.textTheme.bodyMedium),
          ),
          const SizedBox(width: 20),
          Switch(
            value: config[key] as bool? ?? false,
            onChanged: (value) => onChanged(key, value),
            activeColor: theme.colorScheme.primary,
          ),
        ],
      ),
    );
  }

  static Widget buildDropdownField(String label, String key, List<String> options, ThemeData theme, Map<String, dynamic> config, Function(String, String) onChanged) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: theme.textTheme.bodyMedium),
          ),
          const SizedBox(width: 20),
          Expanded(
            child: DropdownButton<String>(
              value: config[key] as String?,
              items: options.map((option) {
                return DropdownMenuItem<String>(
                  value: option,
                  child: Text(option),
                );
              }).toList(),
              onChanged: (value) {
                if (value != null) {
                  onChanged(key, value);
                }
              },
              isExpanded: true,
            ),
          ),
        ],
      ),
    );
  }

  static Widget buildNumberField(String label, String key, int min, int max, ThemeData theme, Map<String, dynamic> config, Function(String, int) onChanged) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Text(label, style: theme.textTheme.bodyMedium),
          ),
          const SizedBox(width: 20),
          Container(
            width: 100,
            child: TextFormField(
              initialValue: config[key]?.toString() ?? min.toString(),
              keyboardType: TextInputType.number,
              decoration: InputDecoration(
                border: OutlineInputBorder(),
                contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              ),
              onChanged: (value) {
                final number = int.tryParse(value ?? '');
                if (number != null && number >= min && number <= max) {
                  onChanged(key, number);
                }
              },
            ),
          ),
        ],
      ),
    );
  }

  static void _showColorPicker(BuildContext context, String key, Color? currentColor, Function(String, Color) onChanged) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('选择颜色'),
        content: SingleChildScrollView(
          child: ColorPicker(
            pickerColor: currentColor ?? Colors.blue,
            onColorChanged: (color) {
              onChanged(key, color);
              Navigator.of(context).pop();
            },
            pickerAreaBorderRadius: const BorderRadius.only(
              topLeft: Radius.circular(2),
              topRight: Radius.circular(2),
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('取消'),
          ),
        ],
      ),
    );
  }
}
