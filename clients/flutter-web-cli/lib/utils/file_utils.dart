class FileUtils {
  /// 获取文件名（不包含路径）
  static String getFileName(String path) {
    if (path.isEmpty) return '';
    final parts = path.split('/');
    return parts.isNotEmpty ? parts.last : path;
  }

  /// 获取文件扩展名
  static String getFileExtension(String fileName) {
    if (fileName.isEmpty) return '';
    final lastDotIndex = fileName.lastIndexOf('.');
    if (lastDotIndex == -1) return '';
    return fileName.substring(lastDotIndex + 1).toLowerCase();
  }

  /// 获取文件路径（不包含文件名）
  static String getDirectoryPath(String path) {
    if (path.isEmpty || path == '/') return path;
    final lastSlashIndex = path.lastIndexOf('/');
    if (lastSlashIndex == -1) return '';
    return path.substring(0, lastSlashIndex + 1);
  }

  /// 格式化文件大小
  static String formatFileSize(int sizeInBytes) {
    if (sizeInBytes < 1024) {
      return '$sizeInBytes B';
    } else if (sizeInBytes < 1024 * 1024) {
      return '${(sizeInBytes / 1024).toStringAsFixed(2)} KB';
    } else if (sizeInBytes < 1024 * 1024 * 1024) {
      return '${(sizeInBytes / (1024 * 1024)).toStringAsFixed(2)} MB';
    } else {
      return '${(sizeInBytes / (1024 * 1024 * 1024)).toStringAsFixed(2)} GB';
    }
  }

  /// 格式化时间戳
  static String formatTimestamp(int timestamp) {
    final dateTime = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return dateTime.toString();
  }

  /// 检查是否是音频文件
  static bool isAudioFile(String fileName) {
    final audioExtensions = {
      'mp3', 'wav', 'flac', 'ogg', 'aac', 'wma', 'm4a', 'opus',
    };
    final extension = getFileExtension(fileName);
    return audioExtensions.contains(extension);
  }

  /// 检查是否是视频文件
  static bool isVideoFile(String fileName) {
    final videoExtensions = {
      'mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv', 'webm', 'm4v',
    };
    final extension = getFileExtension(fileName);
    return videoExtensions.contains(extension);
  }

  /// 检查是否是图片文件
  static bool isImageFile(String fileName) {
    final imageExtensions = {
      'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'heic',
    };
    final extension = getFileExtension(fileName);
    return imageExtensions.contains(extension);
  }
}
