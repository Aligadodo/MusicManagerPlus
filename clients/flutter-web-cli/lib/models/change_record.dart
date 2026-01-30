class ChangeRecord {
  final String id;
  final String originalName;
  final String newName;
  final String status;
  final String? reason;
  final Map<String, dynamic>? extraParams;
  final String? operationType;
  final String? filePath;
  final bool changed;

  ChangeRecord({
    required this.id,
    required this.originalName,
    required this.newName,
    required this.status,
    this.reason,
    this.extraParams,
    this.operationType,
    this.filePath,
    this.changed = false,
  });

  factory ChangeRecord.fromJson(Map<String, dynamic> json) {
    return ChangeRecord(
      id: json['id'] ?? '',
      originalName: json['originalName'] ?? '',
      newName: json['newName'] ?? '',
      status: json['status'] ?? 'PENDING',
      reason: json['reason'],
      extraParams: json['extraParams'] != null 
          ? Map<String, dynamic>.from(json['extraParams']) 
          : null,
      operationType: json['operationType'],
      filePath: json['filePath'],
      changed: json['changed'] ?? false,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'originalName': originalName,
      'newName': newName,
      'status': status,
      if (reason != null) 'reason': reason,
      if (extraParams != null) 'extraParams': extraParams,
      if (operationType != null) 'operationType': operationType,
      if (filePath != null) 'filePath': filePath,
      'changed': changed,
    };
  }
}
