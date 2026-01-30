class FileInfo {
  final String path;
  final String name;
  final bool directory;
  final int size;
  final int lastModified;
  final String? extension;
  final String? mimeType;

  FileInfo({
    required this.path,
    required this.name,
    required this.directory,
    required this.size,
    required this.lastModified,
    this.extension,
    this.mimeType,
  });

  factory FileInfo.fromJson(Map<String, dynamic> json) {
    return FileInfo(
      path: json['path'] as String,
      name: json['name'] as String,
      directory: json['directory'] as bool,
      size: json['size'] as int,
      lastModified: json['lastModified'] as int,
      extension: json['extension'] as String?,
      mimeType: json['mimeType'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'path': path,
      'name': name,
      'directory': directory,
      'size': size,
      'lastModified': lastModified,
      'extension': extension,
      'mimeType': mimeType,
    };
  }
}
