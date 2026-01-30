class ChangeRecord {
  final String id;
  final String originalName;
  final String newName;
  final String status;

  ChangeRecord({
    required this.id,
    required this.originalName,
    required this.newName,
    required this.status,
  });

  factory ChangeRecord.fromJson(Map<String, dynamic> json) {
    return ChangeRecord(
      id: json['id'] ?? '',
      originalName: json['originalName'] ?? '',
      newName: json['newName'] ?? '',
      status: json['status'] ?? 'PENDING',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'originalName': originalName,
      'newName': newName,
      'status': status,
    };
  }
}
