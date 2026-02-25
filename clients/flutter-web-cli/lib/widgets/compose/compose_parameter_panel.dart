import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/strategy_config.dart';
import 'package:filemanager_flutter/models/config_field.dart';

class ComposeParameterPanel extends StatefulWidget {
  final StrategyConfig? strategyConfig;
  final Function(StrategyConfig?) onConfigChanged;

  const ComposeParameterPanel({
    super.key,
    required this.strategyConfig,
    required this.onConfigChanged,
  });

  @override
  State<ComposeParameterPanel> createState() => _ComposeParameterPanelState();
}

class _ComposeParameterPanelState extends State<ComposeParameterPanel> {
  void _updateConfigValue(String key, dynamic value) {
    if (widget.strategyConfig == null) return;
    
    final newConfig = StrategyConfig(
      Map<String, dynamic>.from(widget.strategyConfig!.configValues),
    );
    newConfig.setValue(key, value);
    widget.onConfigChanged(newConfig);
  }

  @override
  Widget build(BuildContext context) {
    if (widget.strategyConfig == null) {
      return const Center(
        child: Text(
          '无',
          style: TextStyle(
            color: Color(0xFFBDBDBD),
            fontSize: 16,
          ),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: widget.strategyConfig!.configValues.entries.map((entry) {
        final fieldName = entry.key;
        final fieldValue = entry.value;
        return _buildParameterField(fieldName, fieldValue);
      }).toList(),
    );
  }

  Widget _buildParameterField(String fieldName, dynamic fieldValue) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.grey.shade200),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.shade100,
            blurRadius: 2,
            offset: const Offset(0, 1),
          ),
        ],
      ),
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                fieldName,
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 14,
                  color: Colors.grey.shade800,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          _buildParameterInput(fieldName, fieldValue),
        ],
      ),
    );
  }

  Widget _buildParameterInput(String fieldName, dynamic fieldValue) {
    return TextField(
      decoration: InputDecoration(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(4),
          borderSide: BorderSide(color: Colors.grey.shade300),
        ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      ),
      style: const TextStyle(fontSize: 14),
      onChanged: (value) => _updateConfigValue(fieldName, value),
      controller: TextEditingController(
        text: fieldValue?.toString() ?? '',
      ),
    );
  }
}
