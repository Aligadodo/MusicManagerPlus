/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-25 
 */
package com.filemanager.strategy.ncm;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件名提取器
 * 负责从文件名或路径中提取歌曲信息
 */
public class FileNameExtractor {
    
    /**
     * 从文件名中提取歌曲ID
     * @param fileName 文件名
     * @return 歌曲ID
     */
    public String extractSongIdFromFileName(String fileName) {
        try {
            // 从文件名中提取歌曲ID（文件名格式：{songId}-{bitrate}-{hash}.uc）
            Pattern pattern = Pattern.compile("^(\\d+)-");
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            System.err.println("从文件名提取歌曲ID失败: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 从文件中提取歌曲名称
     * @param file 文件
     * @return 歌曲名称
     */
    public String extractSongName(File file) {
        String fileName = file.getName();
        // 移除文件扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        // 尝试从文件名中提取歌曲名（假设格式为"艺术家 - 歌曲名"）
        int dashIndex = fileName.indexOf(" - ");
        if (dashIndex > 0) {
            return fileName.substring(dashIndex + 3).trim();
        }
        return fileName.trim();
    }
    
    /**
     * 从文件中提取艺术家名称
     * @param file 文件
     * @return 艺术家名称
     */
    public String extractArtistName(File file) {
        String fileName = file.getName();
        // 移除文件扩展名
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileName = fileName.substring(0, dotIndex);
        }
        // 尝试从文件名中提取艺术家名（假设格式为"艺术家 - 歌曲名"）
        int dashIndex = fileName.indexOf(" - ");
        if (dashIndex > 0) {
            return fileName.substring(0, dashIndex).trim();
        }
        return "Unknown Artist";
    }
    
    /**
     * 从文件路径中提取歌曲名称
     * @param file 文件
     * @return 歌曲名称
     */
    public String extractSongNameFromPath(File file) {
        try {
            // 从文件路径中提取歌曲名称
            // 例如：如果路径中包含歌曲名称信息
            String fileName = file.getName();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
            
            // 尝试从目录结构中提取歌曲信息
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                String parentName = parentDir.getName();
                // 尝试从父目录名称中提取歌曲信息
                // 例如：如果父目录名称包含歌曲名称
                // 这里使用简单的逻辑，实际需要根据真实的目录结构进行解析
            }
            
            // 最后返回基于文件名的名称
            return baseName;
        } catch (Exception e) {
            System.err.println("从路径提取歌曲名称失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从文件路径中提取艺术家名称
     * @param file 文件
     * @return 艺术家名称
     */
    public String extractArtistNameFromPath(File file) {
        try {
            // 从文件路径中提取艺术家名称
            // 例如：如果路径中包含艺术家名称信息
            
            // 尝试从目录结构中提取艺术家信息
            File parentDir = file.getParentFile();
            if (parentDir != null) {
                String parentName = parentDir.getName();
                // 尝试从父目录名称中提取艺术家信息
                // 例如：如果父目录名称包含艺术家名称
                // 这里使用简单的逻辑，实际需要根据真实的目录结构进行解析
            }
            
            // 最后返回默认值
            return "Unknown Artist";
        } catch (Exception e) {
            System.err.println("从路径提取艺术家名称失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 生成目标文件名
     * @param ucFile .uc文件
     * @param audioFormat 音频格式
     * @return 目标文件名
     */
    public String generateTargetFileName(File ucFile, String audioFormat) {
        try {
            String baseName = ucFile.getName().substring(0, ucFile.getName().lastIndexOf('.'));
            return baseName + "." + audioFormat;
        } catch (Exception e) {
            System.err.println("生成目标文件名失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查文件是否为缓存文件
     * @param file 文件
     * @return 是否为缓存文件
     */
    public boolean isCacheFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".uc");
    }
}
