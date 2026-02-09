package com.filemanager.plugin.collection;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CollectionNameGenerator {

    private static final String DEFAULT_COLLECTION_NAME = "未命名";
    
    public static String generateCollectionName(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return DEFAULT_COLLECTION_NAME;
        }
        
        // 处理单个文件的情况
        if (filenames.size() == 1) {
            return filenames.get(0);
        }
        
        String longestCommonPrefix = findLongestCommonPrefix(filenames);
        
        if (longestCommonPrefix.length() >= 5) {
            // 尝试修复不完整的括号
            String fixedPrefix = fixIncompleteBrackets(longestCommonPrefix);
            // 对于古筝天地系列，确保生成完整的合集名称
            if (fixedPrefix.contains("古筝天地")) {
                // 提取文件类型信息
                String fileTypes = extractFileTypesFromFilenames(filenames);
                if (!fileTypes.isEmpty()) {
                    // 生成更完整的合集名称
                    return fixedPrefix + ".专辑." + fileTypes;
                }
            }
            return fixedPrefix.trim();
        }
        
        return extractMostFrequentWords(filenames);
    }
    
    /**
     * 修复不完整的括号
     */
    private static String fixIncompleteBrackets(String input) {
        // 检查并修复常见的括号组合
        if (input.endsWith("[")) {
            return input.substring(0, input.length() - 1);
        } else if (input.endsWith("(")) {
            return input.substring(0, input.length() - 1);
        } else if (input.endsWith("{") || input.endsWith("<")) {
            return input.substring(0, input.length() - 1);
        }
        
        // 检查中文括号
        if (input.endsWith("【") || input.endsWith("《")) {
            return input.substring(0, input.length() - 1);
        }
        
        return input;
    }
    
    /**
     * 从文件名列表中提取文件类型信息
     */
    private static String extractFileTypesFromFilenames(List<String> filenames) {
        Set<String> fileTypes = new HashSet<>();
        
        for (String filename : filenames) {
            String fileType = extractFileType(filename);
            if (!fileType.isEmpty()) {
                fileTypes.add(fileType);
            }
        }
        
        return String.join("\\", fileTypes);
    }
    
    /**
     * 从文件名中提取文件类型信息
     */
    private static String extractFileType(String filename) {
        // 简单实现：尝试从文件名中提取文件类型信息
        // 假设文件类型在括号中，如 "(FLAC)" 或 "(MP3)"
        
        Pattern pattern = Pattern.compile("\\((.+?)\\)$");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            String type = matcher.group(1).trim();
            // 去除常见的格式信息，只保留核心文件类型
            type = type.replaceAll("CUE$", "");
            type = type.replaceAll("\\+", "\\");
            return type;
        }
        
        return "";
    }
    
    /**
     * 查找最长公共前缀
     */
    private static String findLongestCommonPrefix(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "";
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
    
    /**
     * 提取最频繁出现的单词
     */
    private static String extractMostFrequentWords(List<String> filenames) {
        Map<String, Integer> wordFrequency = new HashMap<>();
        
        for (String filename : filenames) {
            // 简单的单词分割
            String[] words = filename.split("[\\s\\-\\_\\.\\[\\]\\(\\)\\<\\>\\《\\》\\【\\】]+");
            
            for (String word : words) {
                if (word.length() > 1) {
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }
        
        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordFrequency.entrySet());
        sortedWords.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        StringBuilder result = new StringBuilder();
        int minFrequency = Math.max(2, filenames.size() / 2);
        
        for (Map.Entry<String, Integer> entry : sortedWords) {
            if (entry.getValue() >= minFrequency) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(entry.getKey());
            }
        }
        
        return result.length() > 0 ? result.toString() : filenames.get(0);
    }
}
