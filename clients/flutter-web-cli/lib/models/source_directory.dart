class SourceDirectory {
  final String path;
  final int threadCount;

  SourceDirectory({
    required this.path,
    required this.threadCount,
  });

  factory SourceDirectory.fromJson(Map<String, dynamic> json) {
    return SourceDirectory(
      path: json['path'] as String,
      threadCount: json['threadCount'] as int? ?? 4,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'path': path,
      'threadCount': threadCount,
    };
  }
}
