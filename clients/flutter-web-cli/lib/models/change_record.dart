class ChangeRecord {
  final String id;
  final String originalName;
  final String newName;
  final String status;
  final String? reason;
  final String? failReason;
  final Map<String, dynamic>? extraParams;
  final String? operationType;
  final String? filePath;
  final String? newPath;
  final bool changed;
  final bool isCreate;
  final bool isDeleteOrMove;
  final bool selected;
  final List<String>? processInfo;
  final int? analyzeTime;
  final int? executeTime;

  ChangeRecord({
    required this.id,
    required this.originalName,
    required this.newName,
    required this.status,
    this.reason,
    this.failReason,
    this.extraParams,
    this.operationType,
    this.filePath,
    this.newPath,
    this.changed = false,
    this.isCreate = false,
    this.isDeleteOrMove = false,
    this.selected = false,
    this.processInfo,
    this.analyzeTime,
    this.executeTime,
  });

  factory ChangeRecord.fromJson(Map<String, dynamic> json) {
    return ChangeRecord(
      id: json['id']?.toString() ?? '',
      originalName: json['originalName'] ?? '',
      newName: json['newName'] ?? '',
      status: json['status'] ?? 'PENDING',
      reason: json['reason'],
      failReason: json['failReason'],
      extraParams: json['extraParams'] != null 
          ? Map<String, dynamic>.from(json['extraParams']) 
          : null,
      operationType: json['operationType'],
      filePath: json['filePath'],
      newPath: json['newPath'],
      changed: json['changed'] ?? false,
      isCreate: json['isCreate'] ?? false,
      isDeleteOrMove: json['isDeleteOrMove'] ?? false,
      selected: json['selected'] ?? false,
      processInfo: json['processInfo'] != null
          ? List<String>.from(json['processInfo'])
          : null,
      analyzeTime: json['analyzeTime'] != null ? int.tryParse(json['analyzeTime'].toString()) : null,
      executeTime: json['executeTime'] != null ? int.tryParse(json['executeTime'].toString()) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'originalName': originalName,
      'newName': newName,
      'status': status,
      if (reason != null) 'reason': reason,
      if (failReason != null) 'failReason': failReason,
      if (extraParams != null) 'extraParams': extraParams,
      if (operationType != null) 'operationType': operationType,
      if (filePath != null) 'filePath': filePath,
      if (newPath != null) 'newPath': newPath,
      'changed': changed,
      'isCreate': isCreate,
      'isDeleteOrMove': isDeleteOrMove,
      'selected': selected,
      if (processInfo != null) 'processInfo': processInfo,
      if (analyzeTime != null) 'analyzeTime': analyzeTime,
      if (executeTime != null) 'executeTime': executeTime,
    };
  }
}
