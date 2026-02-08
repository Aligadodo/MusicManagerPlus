import 'package:flutter/material.dart';
import 'package:filemanager_flutter/models/precondition.dart';
import 'package:filemanager_flutter/models/precondition_field_config.dart';
import 'package:filemanager_flutter/widgets/selectable_text_widget.dart';

class PreconditionItem extends StatefulWidget {
  final int groupIndex;
  final int conditionIndex;
  final Precondition condition;
  final Function(int, int, Precondition) onUpdate;
  final Function(int, int) onDelete;

  const PreconditionItem({
    super.key,
    required this.groupIndex,
    required this.conditionIndex,
    required this.condition,
    required this.onUpdate,
    required this.onDelete,
  });

  @override
  State<PreconditionItem> createState() => _PreconditionItemState();
}

class _PreconditionItemState extends State<PreconditionItem> {
  bool _isExpanded = false;
  late TextEditingController _valueController;

  @override
  void initState() {
    super.initState();
    _valueController = TextEditingController(text: widget.condition.value?.toString() ?? '');
    _isExpanded = widget.condition.value == null || widget.condition.value.toString().isEmpty;
  }

  @override
  void dispose() {
    _valueController.dispose();
    super.dispose();
  }

  void _toggleExpanded() {
    setState(() {
      _isExpanded = !_isExpanded;
    });
  }

  void _saveAndCollapse() {
    setState(() {
      _isExpanded = false;
    });
  }

  void _updateCondition(Precondition newCondition) {
    widget.onUpdate(widget.groupIndex, widget.conditionIndex, newCondition);
  }

  String? _validatePrecondition(Precondition condition) {
    PreconditionFieldConfig? fieldConfig = PreconditionFieldConfigs.getFieldConfig(condition.field);
    if (fieldConfig == null) return '无效的字段类型';

    // 处理文件类型的层级结构
    if (fieldConfig.code == 'fileType' && condition.subField != null) {
      PreconditionSubFieldConfig? subFieldConfig = fieldConfig.getSubFieldConfig(condition.subField!);
      if (subFieldConfig == null) return '无效的子字段类型';

      bool requiresValue = false;
      try {
        var operatorConfig = subFieldConfig.operators.firstWhere((op) => op.code == condition.operator);
        requiresValue = operatorConfig.inputType != PreconditionInputType.none;
      } catch (e) {
        return '无效的操作符';
      }

      if (requiresValue) {
        if (condition.value == null || condition.value.toString().isEmpty) {
          return '条件值不能为空';
        }
      }

      return null;
    }

    if (!fieldConfig.operatorRequiresValue(condition.operator)) {
      return null;
    }

    if (condition.value == null || condition.value.toString().isEmpty) {
      return '条件值不能为空';
    }
    
    String field = condition.field;
    String operator = condition.operator;
    String value = condition.value.toString();

    switch (field) {
      case 'size':
      case 'modified':
        if (operator == 'contains' || operator == 'startsWith' || operator == 'endsWith') {
          return '数值类型字段不支持包含、以...开头、以...结尾操作';
        }
        try {
          double.parse(value);
        } catch (e) {
          return '请输入有效的数值';
        }
        break;
      case 'extension':
        if (operator == 'equals' && !value.startsWith('.')) {
          return '扩展名应以点号开头，如 .mp3';
        }
        if (operator == 'in') {
          List<String> extensions = value.split(',').map((e) => e.trim()).toList();
          for (String ext in extensions) {
            if (!ext.startsWith('.')) {
              return '扩展名列表中的每一项都应以点号开头，如 .mp3,.wav';
            }
          }
        }
        break;
    }

    return null;
  }

  @override
  Widget build(BuildContext context) {
    PreconditionFieldConfig? fieldConfig = PreconditionFieldConfigs.getFieldConfig(widget.condition.field);
    if (fieldConfig == null) return const SizedBox.shrink();

    // 处理文件类型的层级结构
    PreconditionSubFieldConfig? subFieldConfig;
    if (fieldConfig.code == 'fileType' && widget.condition.subField != null) {
      subFieldConfig = fieldConfig.getSubFieldConfig(widget.condition.subField!);
    }

    PreconditionOperatorConfig? operatorConfig;
    try {
      if (subFieldConfig != null) {
        operatorConfig = subFieldConfig.operators.firstWhere((op) => op.code == widget.condition.operator);
      } else {
        operatorConfig = fieldConfig.operators.firstWhere((op) => op.code == widget.condition.operator);
      }
    } catch (e) {
      return const SizedBox.shrink();
    }

    String description = PreconditionFieldConfigs.getConditionDescription(widget.condition);
    String? validationError = _validatePrecondition(widget.condition);

    return GestureDetector(
      onDoubleTap: _toggleExpanded,
      child: Container(
        margin: const EdgeInsets.only(bottom: 6),
        decoration: BoxDecoration(
          color: _isExpanded ? Colors.blue.shade50 : Colors.white,
          borderRadius: BorderRadius.circular(6),
          border: Border.all(
            color: validationError != null 
              ? Colors.red.shade300 
              : (_isExpanded ? Colors.blue.shade200 : Colors.grey.shade200),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              decoration: BoxDecoration(
                color: _isExpanded ? Colors.blue.shade100 : Colors.grey.shade50,
                borderRadius: const BorderRadius.only(
                  topLeft: Radius.circular(6),
                  topRight: Radius.circular(6),
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    _isExpanded ? Icons.expand_less : Icons.expand_more,
                    color: Colors.grey.shade600,
                    size: 16,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: SelectableTextWidget(
                      text: description,
                      style: TextStyle(
                        fontWeight: FontWeight.w500,
                        fontSize: 12,
                        color: Colors.grey.shade800,
                      ),
                    ),
                  ),
                  if (!_isExpanded)
                    IconButton(
                      icon: Icon(Icons.edit, color: Colors.blue.shade400, size: 16),
                      onPressed: () {
                        setState(() {
                          _isExpanded = true;
                        });
                      },
                      tooltip: '编辑条件',
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                    ),
                  const SizedBox(width: 4),
                  IconButton(
                    icon: Icon(Icons.close, color: Colors.red.shade400, size: 16),
                    onPressed: () => widget.onDelete(widget.groupIndex, widget.conditionIndex),
                    tooltip: '删除条件',
                    padding: EdgeInsets.zero,
                    constraints: const BoxConstraints(),
                  ),
                ],
              ),
            ),
            if (_isExpanded) ...[
              Container(
                padding: const EdgeInsets.all(10),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: _buildFieldDropdown(fieldConfig),
                        ),
                        const SizedBox(width: 8),
                        if (fieldConfig.code == 'fileType') ...[
                          Expanded(
                            child: _buildSubFieldDropdown(fieldConfig),
                          ),
                          const SizedBox(width: 8),
                        ],
                        Expanded(
                          child: _buildOperatorDropdown(fieldConfig, subFieldConfig),
                        ),
                      ],
                    ),
                    if ((subFieldConfig != null ? 
                        subFieldConfig.operators.firstWhere((op) => op.code == widget.condition.operator).inputType != PreconditionInputType.none : 
                        fieldConfig.operatorRequiresValue(widget.condition.operator)))
                    ...[
                      const SizedBox(height: 8),
                      _buildValueInput(operatorConfig),
                    ],
                    if (validationError != null) ...[
                      const SizedBox(height: 6),
                      Row(
                        children: [
                          Icon(Icons.warning_amber_rounded, color: Colors.amber.shade700, size: 14),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              validationError,
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.amber.shade700,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ],
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        OutlinedButton(
                          onPressed: () {
                            setState(() {
                              _isExpanded = false;
                            });
                          },
                          style: OutlinedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            minimumSize: const Size(0, 0),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(4),
                            ),
                          ),
                          child: const Text('取消', style: TextStyle(fontSize: 12)),
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton.icon(
                          onPressed: validationError == null ? _saveAndCollapse : null,
                          icon: const Icon(Icons.check, size: 16),
                          label: const Text('保存', style: TextStyle(fontSize: 12)),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.green.shade600,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                            minimumSize: const Size(0, 0),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(4),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildFieldDropdown(PreconditionFieldConfig fieldConfig) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: Colors.grey.shade300),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: DropdownButton<String>(
        value: widget.condition.field,
        isExpanded: true,
        items: PreconditionFieldConfigs.fields.map((field) {
          return DropdownMenuItem(
            value: field.code,
            child: Text(field.name, style: const TextStyle(fontSize: 12)),
          );
        }).toList(),
        onChanged: (value) {
          PreconditionFieldConfig? newFieldConfig = PreconditionFieldConfigs.getFieldConfig(value ?? 'file');
          String defaultOperator = newFieldConfig?.operators.first.code ?? 'equals';
          String? defaultSubField;
          
          // 如果是文件类型，设置默认子字段
          if (value == 'fileType' && newFieldConfig?.subFields != null && newFieldConfig!.subFields!.isNotEmpty) {
            defaultSubField = newFieldConfig.subFields!.first.code;
            // 获取子字段的默认操作符
            PreconditionSubFieldConfig subFieldConfig = newFieldConfig.subFields!.first;
            defaultOperator = subFieldConfig.operators.first.code;
          }
          
          _updateCondition(Precondition(
            id: widget.condition.id,
            field: value ?? 'file',
            subField: defaultSubField,
            operator: defaultOperator,
            value: '',
            description: widget.condition.description,
          ));
          setState(() {
            _isExpanded = true;
          });
        },
        style: const TextStyle(fontSize: 12),
        dropdownColor: Colors.white,
        underline: const SizedBox.shrink(),
        icon: Icon(Icons.arrow_drop_down, color: Colors.grey.shade600, size: 20),
      ),
    );
  }

  Widget _buildSubFieldDropdown(PreconditionFieldConfig fieldConfig) {
    final subFields = fieldConfig.subFields ?? [];
    final subFieldCodes = subFields.map((sf) => sf.code).toList();
    final currentSubField = widget.condition.subField != null && subFieldCodes.contains(widget.condition.subField)
        ? widget.condition.subField
        : (subFields.isNotEmpty ? subFields.first.code : '');
    
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: Colors.grey.shade300),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: DropdownButton<String>(
        value: currentSubField,
        isExpanded: true,
        items: subFields.map((subField) {
          return DropdownMenuItem(
            value: subField.code,
            child: Text(subField.name, style: const TextStyle(fontSize: 12)),
          );
        }).toList(),
        onChanged: (value) {
          PreconditionSubFieldConfig? subFieldConfig = fieldConfig.getSubFieldConfig(value ?? '');
          String defaultOperator = subFieldConfig?.operators.first.code ?? 'is';
          
          _updateCondition(Precondition(
            id: widget.condition.id,
            field: widget.condition.field,
            subField: value,
            operator: defaultOperator,
            value: '',
            description: widget.condition.description,
          ));
        },
        style: const TextStyle(fontSize: 12),
        dropdownColor: Colors.white,
        underline: const SizedBox.shrink(),
        icon: Icon(Icons.arrow_drop_down, color: Colors.grey.shade600, size: 20),
      ),
    );
  }

  Widget _buildOperatorDropdown(PreconditionFieldConfig fieldConfig, PreconditionSubFieldConfig? subFieldConfig) {
    final operators = subFieldConfig != null ? subFieldConfig.operators : fieldConfig.operators;
    final operatorCodes = operators.map((op) => op.code).toList();
    final currentOperator = operatorCodes.contains(widget.condition.operator) 
        ? widget.condition.operator 
        : operators.first.code;
    
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: Colors.grey.shade300),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      child: DropdownButton<String>(
        value: currentOperator,
        isExpanded: true,
        items: operators.map((op) {
          return DropdownMenuItem(
            value: op.code,
            child: Text(op.name, style: const TextStyle(fontSize: 12)),
          );
        }).toList(),
        onChanged: (value) {
          _updateCondition(Precondition(
            id: widget.condition.id,
            field: widget.condition.field,
            subField: widget.condition.subField,
            operator: value ?? 'equals',
            value: widget.condition.value,
            description: widget.condition.description,
          ));
        },
        style: const TextStyle(fontSize: 12),
        dropdownColor: Colors.white,
        underline: const SizedBox.shrink(),
        icon: Icon(Icons.arrow_drop_down, color: Colors.grey.shade600, size: 20),
      ),
    );
  }

  Widget _buildValueInput(PreconditionOperatorConfig operatorConfig) {
    return TextField(
      controller: _valueController,
      onChanged: (value) {
        _updateCondition(Precondition(
          id: widget.condition.id,
          field: widget.condition.field,
          subField: widget.condition.subField,
          operator: widget.condition.operator,
          value: value,
          description: widget.condition.description,
        ));
      },
      decoration: InputDecoration(
        hintText: operatorConfig.valuePlaceholder,
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
        contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        isDense: true,
      ),
      style: const TextStyle(fontSize: 12),
    );
  }
}
