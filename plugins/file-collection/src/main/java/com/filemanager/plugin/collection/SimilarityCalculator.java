package com.filemanager.plugin.collection;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SimilarityCalculator {
    
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    public enum SimilarityType {
        LEVENSHTEIN,
        JARO_WINKLER,
        COSINE,
        JACCARD
    }
    
    public static double calculateSimilarity(String str1, String str2, SimilarityType type) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        switch (type) {
            case LEVENSHTEIN:
                return calculateLevenshteinSimilarity(str1, str2);
            case JARO_WINKLER:
                return calculateJaroWinklerSimilarity(str1, str2);
            case COSINE:
                return calculateCosineSimilarity(str1, str2);
            case JACCARD:
                return calculateJaccardSimilarity(str1, str2);
            default:
                return calculateLevenshteinSimilarity(str1, str2);
        }
    }
    
    public static double calculateSimilarity(String str1, String str2) {
        return calculateSimilarity(str1, str2, SimilarityType.LEVENSHTEIN);
    }
    
    /**
     * 处理特殊符号和序号，使用更通用的策略
     */
    public static String processSpecialSymbolsAndNumbers(String input) {
        String result = input;
        
        // 1. 处理各种类型的序号
        // 阿拉伯数字（如 1, 2, 3, 01, 02, 03 等）
        result = result.replaceAll("\\b\\d+\\b", "__NUMBER__");
        
        // 2. 处理中文序号
        // 中文数字（如 一, 二, 三, 十, 百 等）
        result = result.replaceAll("[一二三四五六七八九十百千万壹贰叁肆伍陆柒捌玖拾佰仟]", "__CHINESE_NUM__");
        
        // 3. 处理特殊符号序号
        // 圆形序号（如 ①, ②, ③ 等）
        result = result.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳]", "__CIRCLE_NUM__");
        
        // 4. 处理其他常见的序号格式
        // 字母序号（如 A, B, C, a, b, c 等）
        result = result.replaceAll("\\b[A-Za-z]\\b", "__LETTER__");
        
        // 5. 处理括号和特殊符号
        // 去除无意义的括号和特殊符号
        result = result.replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】]", "");
        
        return result;
    }
    
    /**
     * 计算增强版相似度，考虑特殊符号和序号处理
     */
    public static double calculateEnhancedSimilarity(String str1, String str2) {
        // 使用更通用的策略处理各种类型的序号和特殊符号
        String processed1 = processSpecialSymbolsAndNumbers(str1);
        String processed2 = processSpecialSymbolsAndNumbers(str2);
        
        // 计算基本相似度
        int distance = calculateLevenshteinDistance(processed1, processed2);
        int maxLen = Math.max(processed1.length(), processed2.length());
        double baseSimilarity = (maxLen == 0) ? 1.0 : 1.0 - ((double) distance / maxLen);
        
        // 检查是否有相同的标题和不同的数字序号
        if (FileMetadataExtractor.hasSameTitleDifferentNumber(str1, str2)) {
            // 如果有相同的标题和不同的序号，相似度提高
            baseSimilarity = Math.min(1.0, baseSimilarity + 0.3);
        }
        
        // 检查是否包含相同的核心关键词
        List<String> keywords1 = FileMetadataExtractor.extractCoreKeywords(str1);
        List<String> keywords2 = FileMetadataExtractor.extractCoreKeywords(str2);
        double keywordSimilarity = FileMetadataExtractor.calculateKeywordSimilarity(keywords1, keywords2);
        
        // 综合考虑基本相似度和关键词相似度
        double finalSimilarity = (baseSimilarity * 0.7) + (keywordSimilarity * 0.3);
        
        // 特殊处理：如果两个文件名包含相同的艺术家名称，相似度提高
        String artist1 = FileMetadataExtractor.extractArtist(str1);
        String artist2 = FileMetadataExtractor.extractArtist(str2);
        if (!artist1.isEmpty() && !artist2.isEmpty() && artist1.equals(artist2)) {
            finalSimilarity = Math.min(1.0, finalSimilarity + 0.2);
        }
        
        return finalSimilarity;
    }
    
    public static double calculateFileNameSimilarity(String fileName1, String fileName2) {
        if (fileName1 == null || fileName2 == null) {
            return 0.0;
        }
        
        String name1 = removeExtension(fileName1);
        String name2 = removeExtension(fileName2);
        
        name1 = normalizeString(name1);
        name2 = normalizeString(name2);
        
        return calculateSimilarity(name1, name2, SimilarityType.JARO_WINKLER);
    }
    
    public static double calculateFilePathSimilarity(String filePath1, String filePath2) {
        if (filePath1 == null || filePath2 == null) {
            return 0.0;
        }
        
        File file1 = new File(filePath1);
        File file2 = new File(filePath2);
        
        String name1 = file1.getName();
        String name2 = file2.getName();
        
        String parent1 = file1.getParent() != null ? file1.getParent() : "";
        String parent2 = file2.getParent() != null ? file2.getParent() : "";
        
        double nameSimilarity = calculateFileNameSimilarity(name1, name2);
        double pathSimilarity = calculateSimilarity(parent1, parent2, SimilarityType.LEVENSHTEIN);
        
        return nameSimilarity * 0.7 + pathSimilarity * 0.3;
    }
    
    private static double calculateLevenshteinSimilarity(String str1, String str2) {
        int distance = calculateLevenshteinDistance(str1, str2);
        int maxLength = Math.max(str1.length(), str2.length());
        
        if (maxLength == 0) {
            return 1.0;
        }
        
        return 1.0 - (double) distance / maxLength;
    }
    
    private static int calculateLevenshteinDistance(String str1, String str2) {
        int len1 = str1.length();
        int len2 = str2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    private static double calculateJaroWinklerSimilarity(String str1, String str2) {
        if (str1.equals(str2)) {
            return 1.0;
        }
        
        int len1 = str1.length();
        int len2 = str2.length();
        
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }
        
        int matchDistance = Math.max(len1, len2) / 2 - 1;
        if (matchDistance < 0) {
            matchDistance = 0;
        }
        
        boolean[] str1Matches = new boolean[len1];
        boolean[] str2Matches = new boolean[len2];
        
        int matches = 0;
        int transpositions = 0;
        
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            
            for (int j = start; j < end; j++) {
                if (str2Matches[j] || str1.charAt(i) != str2.charAt(j)) {
                    continue;
                }
                
                str1Matches[i] = true;
                str2Matches[j] = true;
                matches++;
                break;
            }
        }
        
        if (matches == 0) {
            return 0.0;
        }
        
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!str1Matches[i]) {
                continue;
            }
            
            while (!str2Matches[k]) {
                k++;
            }
            
            if (str1.charAt(i) != str2.charAt(k)) {
                transpositions++;
            }
            
            k++;
        }
        
        double jaro = ((double) matches / len1 + (double) matches / len2 + (double) (matches - transpositions / 2.0) / matches) / 3.0;
        
        int prefix = 0;
        for (int i = 0; i < Math.min(Math.min(len1, len2), 4); i++) {
            if (str1.charAt(i) == str2.charAt(i)) {
                prefix++;
            } else {
                break;
            }
        }
        
        double jaroWinkler = jaro + prefix * 0.1 * (1.0 - jaro);
        
        return jaroWinkler;
    }
    
    private static double calculateCosineSimilarity(String str1, String str2) {
        List<String> words1 = Arrays.asList(str1.toLowerCase().split("\\s+"));
        List<String> words2 = Arrays.asList(str2.toLowerCase().split("\\s+"));
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (String word : words1) {
            double count1 = words1.stream().filter(w -> w.equals(word)).count();
            double count2 = words2.stream().filter(w -> w.equals(word)).count();
            
            dotProduct += count1 * count2;
            norm1 += count1 * count1;
        }
        
        for (String word : words2) {
            double count2 = words2.stream().filter(w -> w.equals(word)).count();
            norm2 += count2 * count2;
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private static double calculateJaccardSimilarity(String str1, String str2) {
        List<String> words1 = Arrays.asList(str1.toLowerCase().split("\\s+"));
        List<String> words2 = Arrays.asList(str2.toLowerCase().split("\\s+"));
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        long intersection = words1.stream().filter(words2::contains).count();
        long union = words1.stream().filter(w -> !words2.contains(w)).count() + 
                    words2.stream().filter(w -> !words1.contains(w)).count() + intersection;
        
        if (union == 0) {
            return 0.0;
        }
        
        return (double) intersection / union;
    }
    
    private static String normalizeString(String str) {
        if (str == null) {
            return "";
        }
        
        str = SPECIAL_CHARS_PATTERN.matcher(str).replaceAll(" ");
        str = WHITESPACE_PATTERN.matcher(str).replaceAll(" ").trim();
        str = str.toLowerCase();
        
        return str;
    }
    
    private static String removeExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        
        return fileName;
    }
    
    public static String findLongestCommonPrefix(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        
        if (strings.size() == 1) {
            return strings.get(0);
        }
        
        String prefix = strings.get(0);
        
        for (int i = 1; i < strings.size(); i++) {
            while (strings.get(i).indexOf(prefix) != 0) {
                prefix = prefix.substring(0, prefix.length() - 1);
                
                if (prefix.isEmpty()) {
                    return "";
                }
            }
        }
        
        return prefix;
    }
    
    public static String findLongestCommonSubstring(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return "";
        }
        
        if (str1.equals(str2)) {
            return str1;
        }
        
        int len1 = str1.length();
        int len2 = str2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        int maxLength = 0;
        int endIndex = 0;
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    
                    if (dp[i][j] > maxLength) {
                        maxLength = dp[i][j];
                        endIndex = i;
                    }
                } else {
                    dp[i][j] = 0;
                }
            }
        }
        
        if (maxLength == 0) {
            return "";
        }
        
        return str1.substring(endIndex - maxLength, endIndex);
    }
}
