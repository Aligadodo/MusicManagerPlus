package com.filemanager.plugin.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MetadataExtractor {

    public static Map<String, String> extractMetadata(File file) {
        Map<String, String> metadata = new HashMap<>();
        
        if (!file.isFile()) {
            return metadata;
        }
        
        // 这里简化处理，实际需要使用音频元数据库（如JAudioTagger）来提取元数据
        // 包括：艺术家、专辑、年份、流派、音轨编号等
        
        String fileName = file.getName();
        
        // 尝试从文件名提取元数据
        extractFromFilename(fileName, metadata);
        
        return metadata;
    }

    private static void extractFromFilename(String fileName, Map<String, String> metadata) {
        // 移除文件扩展名
        String nameWithoutExt = fileName;
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExt = fileName.substring(0, lastDotIndex);
        }
        
        // 尝试解析常见格式
        // 格式1: 艺术家 - 专辑
        if (nameWithoutExt.contains(" - ")) {
            String[] parts = nameWithoutExt.split(" - ");
            if (parts.length >= 2) {
                metadata.put("artist", parts[0].trim());
                metadata.put("album", parts[1].trim());
            }
        }
        
        // 格式2: 艺术家 - 专辑 (年份)
        String yearMatch = extractYear(nameWithoutExt);
        if (yearMatch != null) {
            metadata.put("year", yearMatch);
        }
        
        // 提取音轨编号
        String trackMatch = extractTrackNumber(nameWithoutExt);
        if (trackMatch != null) {
            metadata.put("track", trackMatch);
        }
    }

    private static String extractYear(String text) {
        // 匹配4位数字年份
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }

    private static String extractTrackNumber(String text) {
        // 匹配音轨编号（如：01, 1, 001等）
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(text.trim());
        
        if (matcher.find()) {
            return matcher.group();
        }
        
        return null;
    }

    public static boolean validateMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        
        // 验证关键字段
        String artist = metadata.get("artist");
        String album = metadata.get("album");
        String title = metadata.get("title");
        
        // 检查是否为空或未知
        if (artist == null || artist.isEmpty() || "Unknown Artist".equalsIgnoreCase(artist)) {
            return false;
        }
        
        if (album == null || album.isEmpty() || "Unknown Album".equalsIgnoreCase(album)) {
            return false;
        }
        
        if (title == null || title.isEmpty() || "Unknown Title".equalsIgnoreCase(title)) {
            return false;
        }
        
        return true;
    }

    public static String getArtist(Map<String, String> metadata) {
        return metadata.getOrDefault("artist", "Unknown Artist");
    }

    public static String getAlbum(Map<String, String> metadata) {
        return metadata.getOrDefault("album", "Unknown Album");
    }

    public static String getYear(Map<String, String> metadata) {
        return metadata.getOrDefault("year", "");
    }

    public static String getGenre(Map<String, String> metadata) {
        return metadata.getOrDefault("genre", "");
    }

    public static String getTrack(Map<String, String> metadata) {
        return metadata.getOrDefault("track", "");
    }
}
