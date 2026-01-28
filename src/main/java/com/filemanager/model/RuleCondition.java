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

import com.filemanager.type.ConditionType;
import com.filemanager.util.file.CueParserUtil;
import com.filemanager.model.CueSheet;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
public class RuleCondition {
    public ConditionType type;
    public String value;

    public RuleCondition(ConditionType type, String value) {
        this.type = type;
        this.value = value;
    }

    public ConditionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    // --- 预设类型定义 ---
    private static final Set<String> AUDIO_EXTS = new HashSet<>(Arrays.asList(
            "mp3", "flac", "wav", "m4a", "ape", "dsf", "dff", "dts", "ogg", "wma", "aac", "alac", "opus", "tak", "tta", "wv"
    ));

    private static final Set<String> ARCHIVE_EXTS = new HashSet<>(Arrays.asList(
            "zip", "7z", "rar", "tar", "gz", "jar", "xz", "bz2", "iso"
    ));

    /**
     * 核心校验逻辑
     *
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
                case CONTAINS:
                    return name.contains(value);
                case NOT_CONTAINS:
                    return !name.contains(value);
                case STARTS_WITH:
                    return name.startsWith(value);
                case ENDS_WITH:
                    return name.endsWith(value);
                case REGEX_MATCH:
                    return name.matches(value);

                // 2. 属性匹配
                case FILE_SIZE_GT:
                    return f.length() > parseSize(value);
                case FILE_SIZE_LT:
                    return f.length() < parseSize(value);
                case PARENT_DIR_IS:
                    return f.getParentFile() != null && f.getParentFile().getName().equals(value);

                // 3. 路径匹配
                case PATH_CONTAINS:
                    return path.contains(value);
                case PATH_NOT_CONTAINS:
                    return !path.contains(value);

                // 4. 类型集合匹配
                case EXT_IN:
                    return checkExtensionList(ext, value, true);
                case EXT_NOT_IN:
                    return checkExtensionList(ext, value, false);

                // 5. 预设判断
                case IS_AUDIO:
                    return AUDIO_EXTS.contains(ext);
                case IS_NOT_AUDIO:
                    return !AUDIO_EXTS.contains(ext);

                case IS_ARCHIVE:
                    return ARCHIVE_EXTS.contains(ext);
                case IS_NOT_ARCHIVE:
                    return !ARCHIVE_EXTS.contains(ext);

                case IS_DIRECTORY:
                    return f.isDirectory();
                case IS_FILE:
                    return f.isFile();
                
                // 6. 父目录文件检查
                case PARENT_HAS_EXT:
                    return checkParentHasExtension(f, value, true);
                case PARENT_NOT_HAS_EXT:
                    return checkParentHasExtension(f, value, false);
                
                // 7. CUE音轨检查
                case IS_CUE_TRACK:
                    return isCueTrackFile(f);
                case IS_NOT_CUE_TRACK:
                    return !isCueTrackFile(f);

                default:
                    return true;
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
        return matchIfIn == found;
    }

    /**
     * 检查父目录下是否包含指定扩展名的文件
     * @param f 待检测的文件对象
     * @param configStr 配置的扩展名列表（逗号分隔）
     * @param matchIfFound 如果找到匹配的文件是否返回true
     * @return 是否满足条件
     */
    private boolean checkParentHasExtension(File f, String configStr, boolean matchIfFound) {
        if (f == null || configStr == null || configStr.isEmpty()) return false;
        
        // 获取父目录
        File parentDir = f.getParentFile();
        if (parentDir == null || !parentDir.isDirectory()) return false;
        
        // 解析目标扩展名列表
        Set<String> targetExts = Arrays.stream(configStr.split("[,，|]"))
                .map(s -> s.trim().toLowerCase().replace(".", "")) // 允许用户输入 ".mp3" 或 "mp3"
                .collect(Collectors.toSet());
        
        // 遍历父目录下的所有文件（非文件夹）
        File[] files = parentDir.listFiles(File::isFile);
        if (files == null) return false;
        
        for (File file : files) {
            // 跳过文件自身
            if (file.equals(f)) continue;
            
            // 检查文件扩展名
            String fileExt = getExtension(file.getName());
            if (targetExts.contains(fileExt)) {
                return matchIfFound;
            }
        }
        
        // 没有找到匹配的文件
        return !matchIfFound;
    }  

    /**
     * 检查音频文件是否是CUE文件中指定的音轨文件
     * @param f 待检测的文件对象
     * @return 如果是CUE音轨文件返回true，否则返回false
     */
    private boolean isCueTrackFile(File f) {
        if (f == null || !f.isFile()) return false;
        
        // 检查是否是音频文件
        String ext = getExtension(f.getName());
        if (!AUDIO_EXTS.contains(ext)) return false;
        
        // 获取当前文件名（不含扩展名）
        String currentFileName = f.getName();
        int dotIndex = currentFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            currentFileName = currentFileName.substring(0, dotIndex);
        }
        
        // 获取父目录
        File parentDir = f.getParentFile();
        if (parentDir == null || !parentDir.isDirectory()) return false;
        
        // 查找目录下的所有CUE文件
        File[] cueFiles = parentDir.listFiles(file -> {
            return file.isFile() && file.getName().toLowerCase().endsWith(".cue");
        });
        
        if (cueFiles == null || cueFiles.length == 0) return false;
        
        // 检查条件1：目录下只有一个音频文件且有cue文件
        File[] audioFiles = parentDir.listFiles(file -> {
            if (!file.isFile()) return false;
            String audioExt = getExtension(file.getName());
            return AUDIO_EXTS.contains(audioExt);
        });
        
        if (audioFiles != null && audioFiles.length == 1) {
            return true;
        }
        
        // 检查条件2：音频文件名与cue文件名相同
        for (File cueFile : cueFiles) {
            String cueFileName = cueFile.getName();
            int cueDotIndex = cueFileName.lastIndexOf('.');
            if (cueDotIndex > 0) {
                cueFileName = cueFileName.substring(0, cueDotIndex);
            }
            
            if (cueFileName.equals(currentFileName)) {
                return true;
            }
        }
        
        // 检查条件3：CUE文件中引用了该音频文件
        for (File cueFile : cueFiles) {
            try {
                // 解析CUE文件
                CueSheet cueSheet = CueParserUtil.parse(cueFile.toPath());
                if (cueSheet == null) continue;
                
                // 检查CUE文件中引用的所有音频文件
                for (String audioFileName : cueSheet.getAllFiles()) {
                    // 获取CUE中引用的文件名（不含扩展名）
                    String cueFileName = audioFileName;
                    int cueDotIndex = cueFileName.lastIndexOf('.');
                    if (cueDotIndex > 0) {
                        cueFileName = cueFileName.substring(0, cueDotIndex);
                    }
                    
                    // 只比较文件名（不含扩展名）是否相同
                    if (cueFileName.equals(currentFileName)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // 解析CUE文件失败，继续检查下一个
                continue;
            }
        }
        
        // 没有找到任何CUE文件引用该音频文件
        return false;
    }

    @Override
    public String toString() {
        if (!type.needsValue()) return type.toString();
        return type + " [" + (value == null ? "" : value) + "]";
    }
}