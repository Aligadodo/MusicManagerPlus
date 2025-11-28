package com.filemanager.plusv2.type;

public enum ConditionType {
    // 文本匹配
    CONTAINS("文件名包含"),
    NOT_CONTAINS("文件名不含"),
    STARTS_WITH("文件名开头"),
    ENDS_WITH("文件名结尾"),
    REGEX_MATCH("正则匹配"),

    // 属性匹配 (New Features)
    FILE_SIZE_GT("文件大小 > (MB)"),
    FILE_SIZE_LT("文件大小 < (MB)"),
    PARENT_DIR_IS("父文件夹名为"),
    FILE_EXT_IS("文件扩展名是");

    private final String description;
    ConditionType(String description) { this.description = description; }
    @Override public String toString() { return description; }
}
