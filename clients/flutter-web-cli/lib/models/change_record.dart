class ChangeRecord {
  final String id;
  final String originalName;
  final String? newName;
  final String filePath;
  final bool changed;
  final OperationType operationType;
  final ExecStatus status;
  final String? failReason;

  ChangeRecord({
    required this.id,
    required this.originalName,
    this.newName,
    required this.filePath,
    required this.changed,
    required this.operationType,
    required this.status,
    this.failReason,
  });

  factory ChangeRecord.fromJson(Map<String, dynamic> json) {
    return ChangeRecord(
      id: json['id'] as String,
      originalName: json['originalName'] as String,
      newName: json['newName'] as String?,
      filePath: json['filePath'] as String,
      changed: json['changed'] as bool,
      operationType: OperationType.values.firstWhere((e) => e.name == json['operationType']),
      status: ExecStatus.values.firstWhere((e) => e.name == json['status']),
      failReason: json['failReason'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'originalName': originalName,
      'newName': newName,
      'filePath': filePath,
      'changed': changed,
      'operationType': operationType.name,
      'status': status.name,
      'failReason': failReason,
    };
  }
}

enum OperationType {
  RENAME,
  MOVE,
  DELETE,
  COPY,
  METADATA_UPDATE;
}

enum ExecStatus {
  PENDING,
  SUCCESS,
  FAILED;
}
