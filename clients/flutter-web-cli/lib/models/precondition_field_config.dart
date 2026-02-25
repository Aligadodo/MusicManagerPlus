import 'precondition.dart';
import 'precondition_field_configs.dart';

enum PreconditionFieldType {
  text,
  number,
  boolean,
  fileType,
  directoryType,
  audioType,
  videoType,
  imageType,
  textType,
  documentType,
  archiveType,
  date,
}

enum PreconditionInputType {
  none,
  single,
  range,
  list,
  multiSelect,
}

class PreconditionOperatorConfig {
  final String code;
  final String name;
  final String description;
  final String valuePlaceholder;
  final PreconditionInputType inputType;
  final String? endValuePlaceholder;

  PreconditionOperatorConfig({
    required this.code,
    required this.name,
    required this.description,
    required this.valuePlaceholder,
    this.inputType = PreconditionInputType.single,
    this.endValuePlaceholder,
  });
}

class PreconditionSubFieldConfig {
  final String code;
  final String name;
  final String description;
  final PreconditionFieldType type;
  final List<PreconditionOperatorConfig> operators;

  PreconditionSubFieldConfig({
    required this.code,
    required this.name,
    required this.description,
    required this.type,
    required this.operators,
  });
}

class PreconditionFieldConfig {
  final String code;
  final String name;
  final String description;
  final List<PreconditionOperatorConfig> operators;
  final PreconditionFieldType type;
  final List<PreconditionSubFieldConfig>? subFields;

  PreconditionFieldConfig({
    required this.code,
    required this.name,
    required this.description,
    required this.operators,
    required this.type,
    this.subFields,
  });

  PreconditionOperatorConfig? getOperatorConfig(String operatorCode) {
    try {
      return operators.firstWhere((op) => op.code == operatorCode);
    } catch (e) {
      return null;
    }
  }

  PreconditionInputType getOperatorInputType(String operatorCode) {
    final operatorConfig = getOperatorConfig(operatorCode);
    return operatorConfig?.inputType ?? PreconditionInputType.single;
  }

  bool operatorRequiresValue(String operatorCode) {
    final operatorConfig = getOperatorConfig(operatorCode);
    return operatorConfig?.inputType != PreconditionInputType.none;
  }

  PreconditionSubFieldConfig? getSubFieldConfig(String subFieldCode) {
    if (subFields == null) return null;
    try {
      return subFields!.firstWhere((subField) => subField.code == subFieldCode);
    } catch (e) {
      return null;
    }
  }
}
