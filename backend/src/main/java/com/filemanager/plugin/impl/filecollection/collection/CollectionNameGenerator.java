package com.filemanager.plugin.impl.filecollection.collection;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CollectionNameGenerator {

    /**
     * 生成合集名称，支持各种命名策略
     */
    public String generateCollectionName(List<String> fileNames, NamingStrategy strategy) {
        if (fileNames == null || fileNames.isEmpty()) {
            return "";
        }

        switch (strategy) {
            case PRECISE:
                return generatePreciseCollectionName(fileNames);
            case COMMON_PREFIX:
                return generateCommonPrefixCollectionName(fileNames);
            case MOST_FREQUENT:
                return generateMostFrequentCollectionName(fileNames);
            case COMBINED:
                return generateCombinedCollectionName(fileNames);
            default:
                return generatePreciseCollectionName(fileNames);
        }
    }

    /**
     * 生成精确的合集名称
     */
    public String generatePreciseCollectionName(List<String> fileNames) {
        if (fileNames.size() == 1) {
            return extractBaseName(fileNames.get(0));
        }

        // 提取所有文件名的基础名称（去除序号和特殊符号）
        List<String> baseNames = fileNames.stream()
                .map(this::extractBaseName)
                .collect(Collectors.toList());

        // 找出最长的公共前缀
        String commonPrefix = findLongestCommonPrefix(baseNames);

        if (commonPrefix.length() > 2) {
            return cleanCollectionName(commonPrefix);
        }

        // 如果公共前缀太短，尝试更复杂的分析
        return generateFromPatterns(fileNames);
    }

    /**
     * 基于公共前缀生成合集名称
     */
    public String generateCommonPrefixCollectionName(List<String> fileNames) {
        List<String> baseNames = fileNames.stream()
                .map(this::extractBaseName)
                .collect(Collectors.toList());

        String commonPrefix = findLongestCommonPrefix(baseNames);
        return cleanCollectionName(commonPrefix);
    }

    /**
     * 基于最频繁出现的词生成合集名称
     */
    public String generateMostFrequentCollectionName(List<String> fileNames) {
        Map<String, Integer> wordFrequency = new HashMap<>();

        for (String fileName : fileNames) {
            List<String> words = extractWords(fileName);
            for (String word : words) {
                if (word.length() > 1) { // 忽略单字
                    wordFrequency.put(word, wordFrequency.getOrDefault(word, 0) + 1);
                }
            }
        }

        // 找出最频繁的词
        String mostFrequentWord = "";
        int maxFrequency = 0;

        for (Map.Entry<String, Integer> entry : wordFrequency.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                maxFrequency = entry.getValue();
                mostFrequentWord = entry.getKey();
            }
        }

        return mostFrequentWord;
    }

    /**
     * 组合多种方法生成合集名称
     */
    public String generateCombinedCollectionName(List<String> fileNames) {
        String preciseName = generatePreciseCollectionName(fileNames);
        String frequentName = generateMostFrequentCollectionName(fileNames);

        if (preciseName.length() > frequentName.length()) {
            return preciseName;
        } else {
            return frequentName;
        }
    }

    /**
     * 从文件名模式中生成合集名称
     */
    private String generateFromPatterns(List<String> fileNames) {
        // 尝试识别系列名称模式
        for (String fileName : fileNames) {
            // 检查是否有明显的系列名称模式
            String patternMatch = matchSeriesPattern(fileName);
            if (!patternMatch.isEmpty()) {
                return patternMatch;
            }
        }

        // 如果没有识别出模式，返回第一个文件名的基础名称
        return extractBaseName(fileNames.get(0));
    }

    /**
     * 匹配系列名称模式
     */
    private String matchSeriesPattern(String fileName) {
        // 常见的系列名称模式
        String[] patterns = {
                "(.+?)[\\s_-]*[0-9]+[\\s_-]*[a-zA-Z]*", // 例如 "Series Name 01"
                "(.+?)[\\s_-]*[a-zA-Z]+[\\s_-]*[0-9]+", // 例如 "Series Name A01"
                "(.+?)[\\s_-]*\\[[0-9]+\\]", // 例如 "Series Name [01]"
                "(.+?)[\\s_-]*\\([0-9]+\\)", // 例如 "Series Name (01)"
                "(.+?)[\\s_-]*第[0-9]+[集部卷]", // 例如 "Series Name 第01集"
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
     * 提取文件名的基础名称
     */
    private String extractBaseName(String fileName) {
        // 去除文件扩展名
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileName = fileName.substring(0, lastDotIndex);
        }

        // 去除序号和特殊符号
        fileName = fileName.replaceAll("\\b\\d+\\b", ""); // 去除数字序号
        fileName = fileName.replaceAll("[①②③④⑤⑥⑦⑧⑨⑩]", ""); // 去除圆形序号
        fileName = fileName.replaceAll("[\\[\\]\\(\\)\\{\\}<>]", ""); // 去除括号
        fileName = fileName.replaceAll("\\s+-", ""); // 去除分隔符
        fileName = fileName.replaceAll("\\s+_", "");
        fileName = fileName.replaceAll("\\s+\\|", "");

        return fileName.trim();
    }

    /**
     * 找出字符串列表的最长公共前缀
     */
    private String findLongestCommonPrefix(List<String> strings) {
        if (strings.isEmpty()) {
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
     * 清理合集名称
     */
    private String cleanCollectionName(String name) {
        // 去除末尾的特殊字符和空白
        name = name.replaceAll("[\\s\\-_\\|]+$", "");
        name = name.replaceAll("[\\[\\]\\(\\)\\{\\}<>", "");
        
        // 去除多余的空白
        name = name.replaceAll("\\s+", " ").trim();

        return name;
    }

    /**
     * 从文件名中提取单词
     */
    private List<String> extractWords(String fileName) {
        // 简单的单词提取，按空白和常见分隔符分割
        List<String> words = new ArrayList<>();
        String[] parts = fileName.split("[\\s\\-_\\|]+");
        
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                words.add(part);
            }
        }
        
        return words;
    }

    /**
     * 生成合集名称的静态方法
     */
    public static String generateCollectionName(List<String> fileNames) {
        CollectionNameGenerator generator = new CollectionNameGenerator();
        return generator.generateCollectionName(fileNames, NamingStrategy.PRECISE);
    }

    /**
     * 生成合集名称的静态方法，支持指定策略
     */
    public static String generateCollectionName(List<String> fileNames, String strategyName) {
        CollectionNameGenerator generator = new CollectionNameGenerator();
        NamingStrategy strategy = NamingStrategy.valueOf(strategyName.toUpperCase());
        return generator.generateCollectionName(fileNames, strategy);
    }

    /**
     * 命名策略枚举
     */
    public enum NamingStrategy {
        PRECISE,
        COMMON_PREFIX,
        MOST_FREQUENT,
        COMBINED
    }
}