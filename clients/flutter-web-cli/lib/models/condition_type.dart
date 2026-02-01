enum ConditionType {
  // --- 1. 文件名文本匹配 ---
  contains("文件名包含"),
  notContains("文件名不含"),
  startsWith("文件名开头是"),
  endsWith("文件名结尾是"),
  regexMatch("文件名正则匹配"),

  // --- 2. 文件属性匹配 ---
  fileSizeGt("文件大小 > (MB)"),
  fileSizeLt("文件大小 < (MB)"),
  parentDirIs("父文件夹名称是"),

  // --- 3. 路径深度匹配 ---
  pathContains("完整路径包含"),
  pathNotContains("完整路径不含"),

  // --- 4. 灵活类型匹配 ---
  extIn("扩展名属于 (逗号分隔)"),
  extNotIn("扩展名不属于"),

  // --- 5. 便捷预设判断 ---
  isAudio("是音频文件"),
  isNotAudio("不是音频文件"),
  isArchive("是压缩文件"),
  isNotArchive("不是压缩文件"),
  isDirectory("是文件夹"),
  isFile("是文件"),

  // --- 6. 父目录文件检查 ---
  parentHasExt("父目录包含扩展名 (逗号分隔)"),
  parentNotHasExt("父目录不包含扩展名"),

  // --- 7. 目录文件模式检查 ---
  dirHasPattern("目录包含匹配文件模式的文件"),
  dirNotHasPattern("目录不包含匹配文件模式的文件"),

  // --- 8. CUE音轨检查 ---
  isCueTrack("是CUE音轨文件"),
  isNotCueTrack("不是CUE音轨文件");

  final String description;

  const ConditionType(this.description);

  static ConditionType fromString(String value) {
    return ConditionType.values.firstWhere(
      (type) => type.description == value,
      orElse: () => ConditionType.contains,
    );
  }

  bool needsValue() {
    return this != isAudio && this != isNotAudio &&
           this != isArchive && this != isNotArchive &&
           this != isDirectory && this != isFile &&
           this != isCueTrack && this != isNotCueTrack;
  }
}
