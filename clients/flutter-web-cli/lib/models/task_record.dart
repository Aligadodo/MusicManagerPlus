/// 任务记录数据模型
/// 统一的数据模型，用于扫描/预览/执行三个阶段的数据展示
class TaskRecord {
  // 基础字段（所有阶段通用）
  final String id;                              // 记录ID
  final String originalName;                    // 原文件名
  final String newName;                         // 新文件名
  final String originalPath;                    // 原路径
  final String newPath;                         // 新路径
  
  // 文件信息字段（扫描阶段主要使用）
  final int? fileSize;                          // 文件大小（字节）
  final String? fileType;                       // 文件类型（扩展名）
  final int? lastModified;                      // 最后修改时间戳
  final Map<String, dynamic>? metadata;         // 元数据
  
  // 操作相关字段（预览和执行阶段使用）
  final String? operationType;                  // 操作类型
  final String? status;                         // 状态
  final String? reason;                         // 变更原因/说明
  final String? failReason;                     // 失败原因
  final Map<String, String>? extraParams;       // 额外参数
  
  // 状态标记字段
  final bool? changed;                          // 是否发生变更
  final bool? isCreate;                         // 是否是新建文件
  final bool? isDeleteOrMove;                   // 是否是删除或移动操作
  final bool? selected;                         // 是否被选中
  
  // 时间相关字段
  final int? analyzeTime;                       // 分析时间戳
  final int? executeTime;                       // 执行时间戳
  final int? duration;                          // 执行耗时（毫秒）
  
  // 执行相关字段
  final int? retryCount;                        // 重试次数
  final List<String>? processInfo;              // 处理信息列表

  TaskRecord({
    required this.id,
    required this.originalName,
    this.newName = '',
    required this.originalPath,
    this.newPath = '',
    this.fileSize,
    this.fileType,
    this.lastModified,
    this.metadata,
    this.operationType,
    this.status,
    this.reason,
    this.failReason,
    this.extraParams,
    this.changed,
    this.isCreate,
    this.isDeleteOrMove,
    this.selected,
    this.analyzeTime,
    this.executeTime,
    this.duration,
    this.retryCount,
    this.processInfo,
  });

  factory TaskRecord.fromJson(Map<String, dynamic> json) {
    return TaskRecord(
      id: json['id']?.toString() ?? '',
      originalName: json['originalName'] ?? json['fileName'] ?? '',
      newName: json['newName'] ?? '',
      originalPath: json['originalPath'] ?? json['filePath'] ?? '',
      newPath: json['newPath'] ?? '',
      fileSize: json['fileSize'] != null ? int.tryParse(json['fileSize'].toString()) : null,
      fileType: json['fileType'],
      lastModified: json['lastModified'] != null ? int.tryParse(json['lastModified'].toString()) : null,
      metadata: json['metadata'] != null ? Map<String, dynamic>.from(json['metadata']) : null,
      operationType: json['operationType'],
      status: json['status'],
      reason: json['reason'],
      failReason: json['failReason'],
      extraParams: json['extraParams'] != null 
          ? Map<String, String>.from(json['extraParams']) 
          : null,
      changed: json['changed'],
      isCreate: json['isCreate'],
      isDeleteOrMove: json['isDeleteOrMove'],
      selected: json['selected'],
      analyzeTime: json['analyzeTime'] != null ? int.tryParse(json['analyzeTime'].toString()) : null,
      executeTime: json['executeTime'] != null ? int.tryParse(json['executeTime'].toString()) : null,
      duration: json['duration'] != null ? int.tryParse(json['duration'].toString()) : null,
      retryCount: json['retryCount'] != null ? int.tryParse(json['retryCount'].toString()) : null,
      processInfo: json['processInfo'] != null 
          ? List<String>.from(json['processInfo']) 
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'originalName': originalName,
      'newName': newName,
      'originalPath': originalPath,
      'newPath': newPath,
      if (fileSize != null) 'fileSize': fileSize,
      if (fileType != null) 'fileType': fileType,
      if (lastModified != null) 'lastModified': lastModified,
      if (metadata != null) 'metadata': metadata,
      if (operationType != null) 'operationType': operationType,
      if (status != null) 'status': status,
      if (reason != null) 'reason': reason,
      if (failReason != null) 'failReason': failReason,
      if (extraParams != null) 'extraParams': extraParams,
      if (changed != null) 'changed': changed,
      if (isCreate != null) 'isCreate': isCreate,
      if (isDeleteOrMove != null) 'isDeleteOrMove': isDeleteOrMove,
      if (selected != null) 'selected': selected,
      if (analyzeTime != null) 'analyzeTime': analyzeTime,
      if (executeTime != null) 'executeTime': executeTime,
      if (duration != null) 'duration': duration,
      if (retryCount != null) 'retryCount': retryCount,
      if (processInfo != null) 'processInfo': processInfo,
    };
  }

  // 便捷属性：文件名（兼容扫描阶段）
  String get fileName => originalName;
  
  // 便捷属性：文件路径（兼容扫描阶段）
  String get filePath => originalPath;
  
  // 便捷属性：目标文件名（兼容执行阶段）
  String get targetName => newName;
  
  // 便捷属性：目标路径（兼容执行阶段）
  String get targetPath => newPath;

  @override
  String toString() {
    return 'TaskRecord{id: $id, originalName: $originalName, newName: $newName, operationType: $operationType, status: $status}';
  }
}

/// 分页响应数据模型
class PaginatedResponse<T> {
  final List<T> list;        // 数据列表
  final int total;           // 总记录数
  final int page;            // 当前页码
  final int pageSize;        // 每页数量
  final int totalPages;      // 总页数
  final bool hasNext;        // 是否有下一页
  final bool hasPrevious;    // 是否有上一页

  PaginatedResponse({
    required this.list,
    required this.total,
    required this.page,
    required this.pageSize,
    required this.totalPages,
    required this.hasNext,
    required this.hasPrevious,
  });

  factory PaginatedResponse.fromJson(
    Map<String, dynamic> json,
    T Function(Map<String, dynamic>) fromJsonT,
  ) {
    return PaginatedResponse(
      list: (json['list'] as List<dynamic>?)
              ?.map((item) => fromJsonT(item as Map<String, dynamic>))
              .toList() ??
          [],
      total: json['total'] ?? 0,
      page: json['page'] ?? 1,
      pageSize: json['pageSize'] ?? 20,
      totalPages: json['totalPages'] ?? 0,
      hasNext: json['hasNext'] ?? false,
      hasPrevious: json['hasPrevious'] ?? false,
    );
  }

  Map<String, dynamic> toJson(Object? Function(T) toJsonT) {
    return {
      'list': list.map((item) => toJsonT(item)).toList(),
      'total': total,
      'page': page,
      'pageSize': pageSize,
      'totalPages': totalPages,
      'hasNext': hasNext,
      'hasPrevious': hasPrevious,
    };
  }

  /// 创建空响应
  static PaginatedResponse<T> empty<T>(int page, int pageSize) {
    return PaginatedResponse<T>(
      list: [],
      total: 0,
      page: page,
      pageSize: pageSize,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
    );
  }
}

/// 分页查询参数模型
class PaginationParams {
  int page;                    // 当前页码
  int pageSize;                // 每页数量
  String? search;              // 搜索关键词
  String? sortField;           // 排序字段
  String sortOrder;            // 排序方向
  
  // 扫描阶段筛选参数
  String? fileType;            // 文件类型筛选
  int? minSize;                // 最小文件大小
  int? maxSize;                // 最大文件大小
  int? startTime;              // 开始时间戳
  int? endTime;                // 结束时间戳
  
  // 预览/执行阶段筛选参数
  String? operationType;       // 操作类型筛选
  String? status;              // 状态筛选
  bool? changed;               // 是否变更筛选

  PaginationParams({
    this.page = 1,
    this.pageSize = 20,
    this.search,
    this.sortField,
    this.sortOrder = 'asc',
    this.fileType,
    this.minSize,
    this.maxSize,
    this.startTime,
    this.endTime,
    this.operationType,
    this.status,
    this.changed,
  });

  Map<String, dynamic> toQueryParams() {
    final params = <String, dynamic>{
      'page': page,
      'pageSize': pageSize,
      if (search != null && search!.isNotEmpty) 'search': search,
      if (sortField != null && sortField!.isNotEmpty) 'sortField': sortField,
      'sortOrder': sortOrder,
      if (fileType != null) 'fileType': fileType,
      if (minSize != null) 'minSize': minSize,
      if (maxSize != null) 'maxSize': maxSize,
      if (startTime != null) 'startTime': startTime,
      if (endTime != null) 'endTime': endTime,
      if (operationType != null) 'operationType': operationType,
      if (status != null) 'status': status,
      if (changed != null) 'changed': changed,
    };
    return params;
  }

  PaginationParams copyWith({
    int? page,
    int? pageSize,
    String? search,
    String? sortField,
    String? sortOrder,
    String? fileType,
    int? minSize,
    int? maxSize,
    int? startTime,
    int? endTime,
    String? operationType,
    String? status,
    bool? changed,
  }) {
    return PaginationParams(
      page: page ?? this.page,
      pageSize: pageSize ?? this.pageSize,
      search: search ?? this.search,
      sortField: sortField ?? this.sortField,
      sortOrder: sortOrder ?? this.sortOrder,
      fileType: fileType ?? this.fileType,
      minSize: minSize ?? this.minSize,
      maxSize: maxSize ?? this.maxSize,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      operationType: operationType ?? this.operationType,
      status: status ?? this.status,
      changed: changed ?? this.changed,
    );
  }
}
