import 'precondition.dart';

class PreconditionGroup {
  final String id;
  final String name;
  final String description;
  final String logicOperator;
  final String logicType;
  final List<Precondition> preconditions;

  PreconditionGroup({
    required this.id,
    required this.name,
    required this.description,
    this.logicOperator = 'AND',
    required this.logicType,
    required this.preconditions,
  });

  factory PreconditionGroup.fromJson(Map<String, dynamic> json) {
    return PreconditionGroup(
      id: json['id'] ?? '',
      name: json['name'] ?? '',
      description: json['description'] ?? '',
      logicOperator: json['logicOperator'] ?? 'AND',
      logicType: json['logicType'] ?? '',
      preconditions: json['preconditions'] != null 
          ? List<Precondition>.from(json['preconditions'].map((x) => Precondition.fromJson(x)))
          : [],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'logicOperator': logicOperator,
      'logicType': logicType,
      'preconditions': preconditions.map((x) => x.toJson()).toList(),
    };
  }

  PreconditionGroup copyWith({
    String? id,
    String? name,
    String? description,
    String? logicOperator,
    String? logicType,
    List<Precondition>? preconditions,
  }) {
    return PreconditionGroup(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      logicOperator: logicOperator ?? this.logicOperator,
      logicType: logicType ?? this.logicType,
      preconditions: preconditions ?? this.preconditions,
    );
  }
}
