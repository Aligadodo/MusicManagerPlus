class RenameCondition {
  final String type;
  final String operator;
  final dynamic value;

  RenameCondition({
    required this.type,
    required this.operator,
    required this.value,
  });

  factory RenameCondition.fromJson(Map<String, dynamic> json) {
    return RenameCondition(
      type: json['type'] as String? ?? '',
      operator: json['operator'] as String? ?? '',
      value: json['value'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'operator': operator,
      'value': value,
    };
  }

  RenameCondition copyWith({
    String? type,
    String? operator,
    dynamic value,
  }) {
    return RenameCondition(
      type: type ?? this.type,
      operator: operator ?? this.operator,
      value: value ?? this.value,
    );
  }
}
