class RenameAction {
  final String type;
  final dynamic value;

  RenameAction({
    required this.type,
    required this.value,
  });

  factory RenameAction.fromJson(Map<String, dynamic> json) {
    return RenameAction(
      type: json['type'] as String? ?? '',
      value: json['value'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'type': type,
      'value': value,
    };
  }

  RenameAction copyWith({
    String? type,
    dynamic value,
  }) {
    return RenameAction(
      type: type ?? this.type,
      value: value ?? this.value,
    );
  }
}
