class Precondition {
  final String id;
  final String field;
  final String operator;
  final dynamic value;
  final String description;

  Precondition({
    required this.id,
    required this.field,
    required this.operator,
    required this.value,
    required this.description,
  });

  factory Precondition.fromJson(Map<String, dynamic> json) {
    return Precondition(
      id: json['id'] ?? '',
      field: json['field'] ?? '',
      operator: json['operator'] ?? '',
      value: json['value'],
      description: json['description'] ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'field': field,
      'operator': operator,
      'value': value,
      'description': description,
    };
  }
}
