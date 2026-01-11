/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.filemanager.type.ConditionType;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleCondition {
    public ConditionType type;
    public String value;

    // --- 预设类型定义 ---
    private static final Set<String> AUDIO_EXTS = new HashSet<>(Arrays.asList(
            "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "ogg", "wma", "aac", "alac", "opus", "tak", "tta", "wv"
    ));

    private static final Set<String> ARCHIVE_EXTS = new HashSet<>(Arrays.asList(
            "zip", "7z", "rar", "tar", "gz", "jar", "xz", "bz2", "iso"
    ));

    /**
     * 核心校验逻辑
     * @param f 待检测的文件对象
     * @return 是否满足条件
     */
    public boolean test(File f) {
        if (f == null) return false;

        String name = f.getName();
        String path = f.getAbsolutePath();
        String ext = getExtension(name);

        try {
            switch (type) {
                // 1. 文本匹配
                case CONTAINS: return name.contains(value);
                case NOT_CONTAINS: return !name.contains(value);
                case STARTS_WITH: return name.startsWith(value);
                case ENDS_WITH: return name.endsWith(value);
                case REGEX_MATCH: return name.matches(value);

                // 2. 属性匹配
                case FILE_SIZE_GT: return f.length() > parseSize(value);
                case FILE_SIZE_LT: return f.length() < parseSize(value);
                case PARENT_DIR_IS: return f.getParentFile() != null && f.getParentFile().getName().equals(value);

                // 3. 路径匹配
                case PATH_CONTAINS: return path.contains(value);
                case PATH_NOT_CONTAINS: return !path.contains(value);

                // 4. 类型集合匹配
                case EXT_IN: return checkExtensionList(ext, value, true);
                case EXT_NOT_IN: return checkExtensionList(ext, value, false);

                // 5. 预设判断
                case IS_AUDIO: return AUDIO_EXTS.contains(ext);
                case IS_NOT_AUDIO: return !AUDIO_EXTS.contains(ext);

                case IS_ARCHIVE: return ARCHIVE_EXTS.contains(ext);
                case IS_NOT_ARCHIVE: return !ARCHIVE_EXTS.contains(ext);

                case IS_DIRECTORY: return f.isDirectory();
                case IS_FILE: return f.isFile();

                default: return true;
            }
        } catch (Exception e) {
            return false; // 解析错误视为不匹配
        }
    }

    // --- Helpers ---

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    private long parseSize(String val) {
        try {
            return (long) (Double.parseDouble(val) * 1024 * 1024); // MB to Bytes
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean checkExtensionList(String currentExt, String configStr, boolean matchIfIn) {
        if (configStr == null || configStr.isEmpty()) return false;
        // 分割并去除空格，转小写
        Set<String> targetExts = Arrays.stream(configStr.split("[,，|]"))
                .map(s -> s.trim().toLowerCase().replace(".", "")) // 允许用户输入 ".mp3" 或 "mp3"
                .collect(Collectors.toSet());

        boolean found = targetExts.contains(currentExt);
        return matchIfIn ? found : !found;
    }

    @Override
    public String toString() {
        if (!type.needsValue()) return type.toString();
        return type + " [" + (value == null ? "" : value) + "]";
    }
}