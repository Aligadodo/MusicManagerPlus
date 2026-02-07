class Precondition {
  final String id;
  final String field;
  final String? subField;
  final String operator;
  final dynamic value;
  final String description;

  Precondition({
    required this.id,
    required this.field,
    this.subField,
    required this.operator,
    required this.value,
    required this.description,
  });

  factory Precondition.fromJson(Map<String, dynamic> json) {
    return Precondition(
      id: json['id'] ?? '',
      field: json['field'] ?? '',
      subField: json['subField'],
      operator: json['operator'] ?? '',
      value: json['value'],
      description: json['description'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'field': field,
      'subField': subField,
      'operator': operator,
      'value': value,
      'description': description,
    };
  }

  Precondition copyWith({
    String? id,
    String? field,
    String? subField,
    String? operator,
    dynamic value,
    String? description,
  }) {
    return Precondition(
      id: id ?? this.id,
      field: field ?? this.field,
      subField: subField ?? this.subField,
      operator: operator ?? this.operator,
      value: value ?? this.value,
      description: description ?? this.description,
    );
  }
}
