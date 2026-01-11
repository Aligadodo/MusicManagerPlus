/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.type;

public enum ConditionType {
    // --- 1. 文件名文本匹配 ---
    CONTAINS("文件名包含"),
    NOT_CONTAINS("文件名不含"),
    STARTS_WITH("文件名开头是"),
    ENDS_WITH("文件名结尾是"),
    REGEX_MATCH("文件名正则匹配"),

    // --- 2. 文件属性匹配 ---
    FILE_SIZE_GT("文件大小 > (MB)"),
    FILE_SIZE_LT("文件大小 < (MB)"),
    PARENT_DIR_IS("父文件夹名称是"),

    // --- 3. 路径深度匹配 (新增) ---
    PATH_CONTAINS("完整路径包含"),
    PATH_NOT_CONTAINS("完整路径不含"),

    // --- 4. 灵活类型匹配 (新增) ---
    EXT_IN("扩展名属于 (逗号分隔)"),
    EXT_NOT_IN("扩展名不属于"),

    // --- 5. 便捷预设判断 (新增) ---
    IS_AUDIO("是音频文件"),
    IS_NOT_AUDIO("不是音频文件"),
    IS_ARCHIVE("是压缩文件"),
    IS_NOT_ARCHIVE("不是压缩文件"),
    IS_DIRECTORY("是文件夹"),
    IS_FILE("是文件");

    private final String description;
    ConditionType(String description) { this.description = description; }
    @Override public String toString() { return description; }

    // 辅助判断：是否需要用户输入值
    public boolean needsValue() {
        return this != IS_AUDIO && this != IS_NOT_AUDIO &&
                this != IS_ARCHIVE && this != IS_NOT_ARCHIVE &&
                this != IS_DIRECTORY && this != IS_FILE;
    }
}