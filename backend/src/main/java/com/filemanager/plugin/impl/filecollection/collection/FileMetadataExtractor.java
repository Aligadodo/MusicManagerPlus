package com.filemanager.plugin.impl.filecollection.collection;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileMetadataExtractor {

    /**
     * 检查两个文件名是否具有相同的标题但不同的数字序号
     */
    public static boolean hasSameTitleDifferentNumber(String fileName1, String fileName2) {
        if (fileName1 == null || fileName2 == null) {
            return false;
        }

        // 提取标题部分（去除数字序号）
        String title1 = removeNumberSequences(fileName1);
        String title2 = removeNumberSequences(fileName2);

        // 检查标题是否相同
        if (!title1.equals(title2)) {
            return false;
        }

        // 检查是否都包含数字序号
        boolean hasNumber1 = containsNumberSequence(fileName1);
        boolean hasNumber2 = containsNumberSequence(fileName2);

        return hasNumber1 && hasNumber2;
    }

    /**
     * 移除文件名中的数字序号
     */
    private static String removeNumberSequences(String fileName) {
        // 移除阿拉伯数字序号
        fileName = fileName.replaceAll("\\b\\d+\\b", "");
        // 移除中文数字序号
        fileName = fileName.replaceAll("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]", "");
        // 移除圆形序号
        fileName = fileName.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "");
        // 移除字母序号
        fileName = fileName.replaceAll("\\b[A-Za-z]\\b", "");
        // 移除括号和特殊符号
        fileName = fileName.replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】]", "");
        // 移除多余的空白
        fileName = fileName.replaceAll("\\s+", " ").trim();
        return fileName;
    }

    /**
     * 检查文件名是否包含数字序号
     */
    private static boolean containsNumberSequence(String fileName) {
        // 检查阿拉伯数字序号
        if (Pattern.matches(".*\\b\\d+\\b.*", fileName)) {
            return true;
        }
        // 检查中文数字序号
        if (Pattern.matches(".*[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟].*", fileName)) {
            return true;
        }
        // 检查圆形序号
        if (Pattern.matches(".*[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳].*", fileName)) {
            return true;
        }
        // 检查字母序号
        if (Pattern.matches(".*\\b[A-Za-z]\\b.*", fileName)) {
            return true;
        }
        return false;
    }

    /**
     * 从文件名中提取核心关键词
     */
    public static List<String> extractCoreKeywords(String fileName) {
        List<String> keywords = new ArrayList<>();

        if (fileName == null) {
            return keywords;
        }

        // 移除文件扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileName = fileName.substring(0, lastDotIndex);
        }

        // 移除序号
        fileName = removeNumberSequences(fileName);

        // 提取单词
        String[] parts = fileName.split("[\\s\\-_\\|]+|");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty() && part.length() > 1) {
                keywords.add(part);
            }
        }

        return keywords;
    }

    /**
     * 计算两个关键词列表的相似度
     */
    public static double calculateKeywordSimilarity(List<String> keywords1, List<String> keywords2) {
        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        // 计算交集大小
        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        // 计算并集大小
        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 从文件名中提取艺术家名称
     */
    public static String extractArtist(String fileName) {
        if (fileName == null) {
            return "";
        }

        // 常见的艺术家名称模式
        String[] patterns = {
                "(.+?)[\\s\\-_\\|]+-+", // 例如 "Artist Name - Song Title"
                "(.+?)[\\s\\-_\\|]+\\|", // 例如 "Artist Name | Song Title"
                "(.+?)[\\s\\-_\\|]+\\[", // 例如 "Artist Name [Album]"
                "(.+?)[\\s\\-_\\|]+\\(", // 例如 "Artist Name (Year)"
        };

        for (String pattern : patterns) {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(fileName);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return "";
    }

    /**
     * 从文件名中提取专辑名称
     */
    public static String extractAlbum(String fileName) {
        if (fileName == null) {
            return "";
        }

        // 常见的专辑名称模式
        Pattern albumPattern = Pattern.compile(".*\\[(.*?)\\].*");
        Matcher matcher = albumPattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "";
    }

    /**
     * 从文件名中提取年份
     */
    public static String extractYear(String fileName) {
        if (fileName == null) {
            return "";
        }

        // 常见的年份模式
        Pattern yearPattern = Pattern.compile(".*\\((\\d{4})\\).*");
        Matcher matcher = yearPattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * 从文件名中提取曲目编号
     */
    public static String extractTrackNumber(String fileName) {
        if (fileName == null) {
            return "";
        }

        // 常见的曲目编号模式
        Pattern trackPattern = Pattern.compile(".*\\b(\\d{1,3})\\b.*");
        Matcher matcher = trackPattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * 分析文件名，提取完整的元数据
     */
    public static Map<String, String> analyzeFileName(String fileName) {
        Map<String, String> metadata = new HashMap<>();

        metadata.put("artist", extractArtist(fileName));
        metadata.put("album", extractAlbum(fileName));
        metadata.put("year", extractYear(fileName));
        metadata.put("trackNumber", extractTrackNumber(fileName));
        metadata.put("coreKeywords", String.join(", ", extractCoreKeywords(fileName)));

        return metadata;
    }

    /**
     * 识别文件名中的系列信息
     */
    public static String identifySeries(String fileName) {
        if (fileName == null) {
            return "";
        }

        // 常见的系列名称模式
        String[] seriesPatterns = {
                "(.+?)[\\s\\-_\\|]+[0-9]+[\\s\\-_\\|]*[a-zA-Z]*", // 例如 "Series Name 01"
                "(.+?)[\\s\\-_\\|]+[a-zA-Z]+[\\s\\-_\\|]*[0-9]+", // 例如 "Series Name A01"
                "(.+?)[\\s\\-_\\|]+\\[[0-9]+\\]", // 例如 "Series Name [01]"
                "(.+?)[\\s\\-_\\|]+\\([0-9]+\\)", // 例如 "Series Name (01)"
                "(.+?)[\\s\\-_\\|]+第[0-9]+[集部卷]", // 例如 "Series Name 第01集"
        };

        for (String pattern : seriesPatterns) {
            Pattern regex = Pattern.compile(pattern);
            Matcher matcher = regex.matcher(fileName);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return "";
    }

    /**
     * 计算两个文件名的相似度，考虑元数据
     */
    public static double calculateMetadataSimilarity(String fileName1, String fileName2) {
        Map<String, String> metadata1 = analyzeFileName(fileName1);
        Map<String, String> metadata2 = analyzeFileName(fileName2);

        double similarity = 0.0;
        int matchCount = 0;

        // 比较艺术家
        if (!metadata1.get("artist").isEmpty() && metadata1.get("artist").equals(metadata2.get("artist"))) {
            similarity += 0.4;
            matchCount++;
        }

        // 比较专辑
        if (!metadata1.get("album").isEmpty() && metadata1.get("album").equals(metadata2.get("album"))) {
            similarity += 0.3;
            matchCount++;
        }

        // 比较年份
        if (!metadata1.get("year").isEmpty() && metadata1.get("year").equals(metadata2.get("year"))) {
            similarity += 0.1;
            matchCount++;
        }

        // 比较核心关键词
        List<String> keywords1 = Arrays.asList(metadata1.get("coreKeywords").split(", "));
        List<String> keywords2 = Arrays.asList(metadata2.get("coreKeywords").split(", "));
        double keywordSimilarity = calculateKeywordSimilarity(keywords1, keywords2);
        similarity += keywordSimilarity * 0.2;

        return similarity;
    }
}