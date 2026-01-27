package com.filemanager.strategy.collection;

import lombok.Data;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 多算法融合的合集名称生成器
 * 
 * 设计思路：
 * 1. 采用多种算法分别生成候选名称
 * 2. 对每个候选名称计算与集合内所有文件的匹配度
 * 3. 选择匹配度最高的名称作为最终结果
 * 
 * 算法列表：
 * 1. 最长公共子串（LCS）算法
 * 2. TF-IDF关键词提取算法
 * 3. N-gram模式识别算法
 * 4. 模板匹配算法
 * 5. 语义相似度聚合算法
 * 
 * @author FileEditTools Team
 */
public class MultiAlgorithmCollectionNameGenerator {
    
    private final StringSimilarityCalculator similarityCalculator;
    
    public MultiAlgorithmCollectionNameGenerator(StringSimilarityCalculator similarityCalculator) {
        this.similarityCalculator = similarityCalculator;
    }
    
    /**
     * 生成合集名称（多算法融合）
     */
    public String generateCollectionName(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return "未命名";
        }
        
        if (filenames.size() == 1) {
            return cleanSingleFilename(filenames.get(0));
        }
        
        // 1. 使用多种算法生成候选名称
        List<CollectionNameCandidate> candidates = generateCandidates(filenames);
        
        if (candidates.isEmpty()) {
            return filenames.get(0);
        }
        
        // 2. 对每个候选名称计算综合得分
        for (CollectionNameCandidate candidate : candidates) {
            double score = calculateCandidateScore(candidate, filenames);
            candidate.setScore(score);
        }
        
        // 3. 选择得分最高的候选名称
        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        CollectionNameCandidate bestCandidate = candidates.get(0);
        String bestName = bestCandidate.getName();
        
        // 4. 清理和优化最终名称
        return optimizeFinalName(bestName, filenames);
    }
    
    /**
     * 生成候选名称列表
     */
    private List<CollectionNameCandidate> generateCandidates(List<String> filenames) {
        List<CollectionNameCandidate> candidates = new ArrayList<>();
        
        // 算法1: 最长公共子串（LCS）
        String lcsName = generateByLongestCommonSubstring(filenames);
        if (!lcsName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(lcsName, "LCS算法", 1.0));
        }
        
        // 算法2: TF-IDF关键词提取
        String tfidfName = generateByTFIDF(filenames);
        if (!tfidfName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(tfidfName, "TF-IDF算法", 1.0));
        }
        
        // 算法3: N-gram模式识别
        String ngramName = generateByNGram(filenames);
        if (!ngramName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(ngramName, "N-gram算法", 1.0));
        }
        
        // 算法4: 模板匹配
        String templateName = generateByTemplateMatching(filenames);
        if (!templateName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(templateName, "模板匹配算法", 1.2));
        }
        
        // 算法5: 语义相似度聚合
        String semanticName = generateBySemanticAggregation(filenames);
        if (!semanticName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(semanticName, "语义聚合算法", 1.1));
        }
        
        // 算法6: 改进的最长公共前缀
        String lcpName = generateByImprovedLCP(filenames);
        if (!lcpName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(lcpName, "改进LCP算法", 0.8));
        }
        
        return candidates;
    }
    
    /**
     * 计算候选名称的得分
     */
    private double calculateCandidateScore(CollectionNameCandidate candidate, List<String> filenames) {
        String candidateName = candidate.getName();
        
        // 1. 计算与所有文件名的平均相似度
        double totalSimilarity = 0.0;
        for (String filename : filenames) {
            double similarity = similarityCalculator.calculateSimilarity(filename, candidateName);
            totalSimilarity += similarity;
        }
        double avgSimilarity = totalSimilarity / filenames.size();
        
        // 2. 计算名称的代表性（长度适中、包含关键词）
        double representativeness = calculateRepresentativeness(candidateName, filenames);
        
        // 3. 计算名称的简洁性（去除冗余信息）
        double conciseness = calculateConciseness(candidateName);
        
        // 4. 综合得分（加权平均）
        double score = avgSimilarity * 0.5 + representativeness * 0.3 + conciseness * 0.2;
        
        // 5. 应用算法权重
        score *= candidate.getAlgorithmWeight();
        
        return score;
    }
    
    /**
     * 计算名称的代表性
     */
    private double calculateRepresentativeness(String name, List<String> filenames) {
        // 1. 名称长度适中（5-30个字符）
        int length = name.length();
        double lengthScore = 0.0;
        if (length >= 5 && length <= 30) {
            lengthScore = 1.0;
        } else if (length >= 3 && length <= 50) {
            lengthScore = 0.8;
        } else {
            lengthScore = 0.5;
        }
        
        // 2. 包含关键词（非数字、非特殊字符的词）
        double keywordScore = 0.0;
        String[] words = name.split("[\\s\\-\\._\\[\\]\\(\\)\\<\\>\\《\\》\\【\\】]+");
        int meaningfulWords = 0;
        for (String word : words) {
            if (word.length() >= 2 && !word.matches("^\\d+$")) {
                meaningfulWords++;
            }
        }
        if (meaningfulWords >= 2) {
            keywordScore = 1.0;
        } else if (meaningfulWords >= 1) {
            keywordScore = 0.7;
        }
        
        // 3. 名称在文件名中的出现频率
        double frequencyScore = 0.0;
        int matchCount = 0;
        for (String filename : filenames) {
            if (filename.contains(name)) {
                matchCount++;
            }
        }
        if (matchCount >= filenames.size()) {
            frequencyScore = 1.0;
        } else if (matchCount >= filenames.size() * 0.8) {
            frequencyScore = 0.8;
        } else if (matchCount >= filenames.size() * 0.5) {
            frequencyScore = 0.6;
        }
        
        return (lengthScore + keywordScore + frequencyScore) / 3.0;
    }
    
    /**
     * 计算名称的简洁性
     */
    private double calculateConciseness(String name) {
        // 1. 去除冗余信息（CD、VOL、Disc等）
        String cleaned = name.replaceAll("(?i)(CD|VOL|DISC)\\s*\\d+", "");
        cleaned = cleaned.replaceAll("(?i)(2CD|3CD|4CD)", "");
        
        // 2. 去除括号内容
        cleaned = cleaned.replaceAll("\\[.*?\\]", "");
        cleaned = cleaned.replaceAll("\\(.*?\\)", "");
        cleaned = cleaned.replaceAll("【.*?】", "");
        cleaned = cleaned.replaceAll("《.*?》", "");
        
        // 3. 去除特殊字符
        cleaned = cleaned.replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】\\.\\,\\!\\?\\;\\:'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", "");
        
        // 4. 计算简洁性得分
        int originalLength = name.length();
        int cleanedLength = cleaned.length();
        double ratio = (double) cleanedLength / originalLength;
        
        if (ratio >= 0.8) {
            return 1.0;
        } else if (ratio >= 0.6) {
            return 0.8;
        } else if (ratio >= 0.4) {
            return 0.6;
        } else {
            return 0.4;
        }
    }
    
    /**
     * 优化最终名称
     */
    private String optimizeFinalName(String name, List<String> filenames) {
        // 1. 清理名称
        String optimized = cleanCollectionName(name);
        
        // 2. 修复不完整的括号
        optimized = fixIncompleteBrackets(optimized);
        
        // 3. 去除CD序号、文件格式等额外信息
        optimized = removeExtraInfo(optimized);
        
        return optimized.trim();
    }
    
    /**
     * 去除CD序号、文件格式等额外信息
     */
    private String removeExtraInfo(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 1. 先去除括号内的内容（包括中文和英文括号）
        name = name.replaceAll("\\[.*?\\]", "");
        name = name.replaceAll("\\(.*?\\)", "");
        name = name.replaceAll("【.*?】", "");
        name = name.replaceAll("《.*?》", "");
        
        // 2. 去除CD序号（包括CD1、CD01、CD 1等格式）
        name = name.replaceAll("\\s*CD\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*Disc\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*VOL\\.\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*Vol\\.\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*VOL\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*Vol\\s*\\d+\\b", "");
        
        // 3. 去除专辑标记
        name = name.replaceAll("\\s*\\.专辑\\.", "");
        name = name.replaceAll("\\s*\\.专辑", "");
        
        // 4. 去除特殊字符和多余空格
        name = name.replaceAll("\\s+", " ");
        name = name.trim();
        
        return name;
    }
    
    /**
     * 算法1: 最长公共子串（LCS）
     */
    private String generateByLongestCommonSubstring(List<String> filenames) {
        if (filenames.size() < 2) {
            return "";
        }
        
        String lcs = findLongestCommonSubstring(filenames.get(0), filenames.get(1));
        
        for (int i = 2; i < filenames.size(); i++) {
            lcs = findLongestCommonSubstring(lcs, filenames.get(i));
            if (lcs.length() < 3) {
                break;
            }
        }
        
        return cleanCollectionName(lcs);
    }
    
    /**
     * 查找两个字符串的最长公共子串
     */
    private String findLongestCommonSubstring(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];
        int maxLength = 0;
        int endIndex = 0;
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
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
        
        return s1.substring(endIndex - maxLength, endIndex);
    }
    
    /**
     * 算法2: TF-IDF关键词提取
     */
    private String generateByTFIDF(List<String> filenames) {
        // 1. 统计词频
        Map<String, Integer> termFrequency = new HashMap<>();
        Map<String, Integer> documentFrequency = new HashMap<>();
        
        for (String filename : filenames) {
            Set<String> wordsInDoc = new HashSet<>();
            String[] words = filename.split("[\\s\\-\\._\\[\\]\\(\\)\\<\\>\\《\\》\\【\\】]+");
            
            for (String word : words) {
                word = word.trim();
                if (word.length() >= 2 && !word.matches("^\\d+$")) {
                    termFrequency.put(word, termFrequency.getOrDefault(word, 0) + 1);
                    wordsInDoc.add(word);
                }
            }
            
            for (String word : wordsInDoc) {
                documentFrequency.put(word, documentFrequency.getOrDefault(word, 0) + 1);
            }
        }
        
        // 2. 计算TF-IDF得分
        Map<String, Double> tfidfScores = new HashMap<>();
        int totalDocs = filenames.size();
        
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();
            int df = documentFrequency.get(term);
            
            // TF-IDF = TF * log(N/DF)
            double tfidf = tf * Math.log((double) totalDocs / df);
            tfidfScores.put(term, tfidf);
        }
        
        // 3. 选择得分最高的词
        List<Map.Entry<String, Double>> sortedTerms = new ArrayList<>(tfidfScores.entrySet());
        sortedTerms.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 4. 构建名称（选择前3个最高分的词）
        StringBuilder nameBuilder = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Double> entry : sortedTerms) {
            if (count >= 3) break;
            if (nameBuilder.length() > 0) {
                nameBuilder.append(" ");
            }
            nameBuilder.append(entry.getKey());
            count++;
        }
        
        return nameBuilder.toString();
    }
    
    /**
     * 算法3: N-gram模式识别
     */
    private String generateByNGram(List<String> filenames) {
        // 1. 提取所有N-gram（N=2,3,4）
        Map<String, Integer> ngramFrequency = new HashMap<>();
        
        for (String filename : filenames) {
            // 标准化文件名
            String normalized = normalizeFilename(filename);
            
            // 提取2-gram
            for (int i = 0; i <= normalized.length() - 2; i++) {
                String ngram = normalized.substring(i, i + 2);
                if (isValidNgram(ngram)) {
                    ngramFrequency.put(ngram, ngramFrequency.getOrDefault(ngram, 0) + 1);
                }
            }
            
            // 提取3-gram
            for (int i = 0; i <= normalized.length() - 3; i++) {
                String ngram = normalized.substring(i, i + 3);
                if (isValidNgram(ngram)) {
                    ngramFrequency.put(ngram, ngramFrequency.getOrDefault(ngram, 0) + 1);
                }
            }
            
            // 提取4-gram
            for (int i = 0; i <= normalized.length() - 4; i++) {
                String ngram = normalized.substring(i, i + 4);
                if (isValidNgram(ngram)) {
                    ngramFrequency.put(ngram, ngramFrequency.getOrDefault(ngram, 0) + 1);
                }
            }
        }
        
        // 2. 找到频率最高的N-gram
        List<Map.Entry<String, Integer>> sortedNgrams = new ArrayList<>(ngramFrequency.entrySet());
        sortedNgrams.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // 3. 合并高频N-gram构建名称
        String bestNgram = "";
        for (Map.Entry<String, Integer> entry : sortedNgrams) {
            String ngram = entry.getKey();
            if (ngram.length() >= 3 && entry.getValue() >= filenames.size() * 0.5) {
                bestNgram = ngram;
                break;
            }
        }
        
        return cleanCollectionName(bestNgram);
    }
    
    /**
     * 检查N-gram是否有效
     */
    private boolean isValidNgram(String ngram) {
        // 不包含特殊字符
        if (ngram.matches(".*[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】\\.\\,\\!\\?\\;\\:'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]+.*")) {
            return false;
        }
        
        // 不全是数字
        if (ngram.matches("^\\d+$")) {
            return false;
        }
        
        // 长度至少为2
        if (ngram.length() < 2) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 算法4: 模板匹配
     */
    private String generateByTemplateMatching(List<String> filenames) {
        // 1. 定义常见模板
        List<Pattern> templates = Arrays.asList(
            // 专辑名称模板（优先级最高）
            Pattern.compile(".*?《([^》]+)》.*?"),
            Pattern.compile(".*?\\[([^\\]]+)\\].*?"),
            Pattern.compile(".*?\\(([^)]+)\\).*?"),
            // 艺术家-专辑模板
            Pattern.compile(".*?\\s*-\\s*([^\\[\\(]+?)\\s*[\\[\\(]"),
            // 系列名称模板（排除CD序号）
            Pattern.compile(".*?([^\\d]+?)\\s*(?=CD|VOL|Disc)\\s*\\d+"),
            // 去除年份前缀后的模板
            Pattern.compile(".*?\\d{4}\\s*-\\s*([^\\[\\(]+?)\\s*[\\[\\(]")
        );
        
        // 2. 匹配模板
        Map<String, Integer> templateMatches = new HashMap<>();
        
        for (String filename : filenames) {
            for (Pattern template : templates) {
                java.util.regex.Matcher matcher = template.matcher(filename);
                if (matcher.find()) {
                    String match = matcher.group(1).trim();
                    // 清理匹配结果，去除CD序号等额外信息
                    match = removeExtraInfo(match);
                    if (match.length() >= 2) {
                        templateMatches.put(match, templateMatches.getOrDefault(match, 0) + 1);
                    }
                }
            }
        }
        
        // 3. 找到匹配次数最多的模板
        String bestMatch = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : templateMatches.entrySet()) {
            if (entry.getValue() > maxCount && entry.getKey().length() >= 3) {
                maxCount = entry.getValue();
                bestMatch = entry.getKey();
            }
        }
        
        // 4. 如果匹配次数足够多，返回最佳匹配
        if (maxCount >= filenames.size() * 0.5) {
            return cleanCollectionName(bestMatch);
        }
        
        return "";
    }
    
    /**
     * 算法5: 语义相似度聚合
     */
    private String generateBySemanticAggregation(List<String> filenames) {
        // 1. 计算所有文件名之间的相似度矩阵
        double[][] similarityMatrix = new double[filenames.size()][filenames.size()];
        
        for (int i = 0; i < filenames.size(); i++) {
            for (int j = 0; j < filenames.size(); j++) {
                if (i == j) {
                    similarityMatrix[i][j] = 1.0;
                } else {
                    similarityMatrix[i][j] = similarityCalculator.calculateSimilarity(filenames.get(i), filenames.get(j));
                }
            }
        }
        
        // 2. 计算每个文件名的中心性（与其他文件名的平均相似度）
        double[] centrality = new double[filenames.size()];
        
        for (int i = 0; i < filenames.size(); i++) {
            double sum = 0.0;
            for (int j = 0; j < filenames.size(); j++) {
                sum += similarityMatrix[i][j];
            }
            centrality[i] = sum / filenames.size();
        }
        
        // 3. 找到中心性最高的文件名
        int bestIndex = 0;
        double maxCentrality = 0.0;
        
        for (int i = 0; i < filenames.size(); i++) {
            if (centrality[i] > maxCentrality) {
                maxCentrality = centrality[i];
                bestIndex = i;
            }
        }
        
        // 4. 清理并返回该文件名
        String bestFilename = filenames.get(bestIndex);
        String cleaned = cleanCollectionName(bestFilename);
        cleaned = removeExtraInfo(cleaned);
        return cleaned;
    }
    
    /**
     * 算法6: 改进的最长公共前缀（LCP）
     */
    private String generateByImprovedLCP(List<String> filenames) {
        // 1. 标准化文件名
        List<String> normalized = filenames.stream()
            .map(this::normalizeFilename)
            .collect(Collectors.toList());
        
        // 2. 查找最长公共前缀
        String lcp = findLongestCommonPrefix(normalized);
        
        // 3. 清理公共前缀
        String cleaned = cleanCollectionName(lcp);
        
        return cleaned;
    }
    
    /**
     * 查找最长公共前缀
     */
    private String findLongestCommonPrefix(List<String> strings) {
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
        
        return prefix.trim();
    }
    
    /**
     * 标准化文件名
     */
    private String normalizeFilename(String filename) {
        String normalized = filename;
        
        // 1. 统一空格和分隔符
        normalized = normalized.replaceAll("[\\s\\-_]+", " ");
        
        // 2. 转换为小写（用于比较）
        normalized = normalized.toLowerCase();
        
        // 3. 去除特殊字符
        normalized = normalized.replaceAll("[\\[\\]\\(\\)\\{\\}\\<>\\《\\》\\【\\】\\.\\,\\!\\?\\;\\:'\"\\`\\~\\|\\=\\+\\\\\\/\\#\\$\\%\\^\\&\\*\\_]", "");
        
        return normalized.trim();
    }
    
    /**
     * 清理合集名称
     */
    private String cleanCollectionName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除常见的不必要前缀
        String[] prefixesToRemove = {
            "缇ゆ槦\\.",
            "缇ゆ槦",
            "滚石合集\\.",
            "滚石合集",
            "滚石\\.",
            "滚石",
            "龙音唱片\\.-\\.",
            "龙音唱片\\.-",
            "龙音唱片\\.",
            "龙音唱片",
            "龙音\\.",
            "龙音",
            "合集\\.",
            "合集",
            "Collection\\.",
            "Collection",
            "缇\\.",
            "缇",
            "唱片\\.",
            "唱片",
            "唱片公司\\.",
            "唱片公司",
            "音乐\\.",
            "音乐",
            "专辑\\.",
            "专辑",
            "群星\\.",
            "群星"
        };
        
        for (String prefix : prefixesToRemove) {
            if (name.matches(prefix + ".*")) {
                name = name.replaceFirst(prefix, "");
                break;
            }
        }
        
        // 去除年份前缀（包括带点的格式）
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
        // 去除括号内的内容
        name = name.replaceAll("\\[.*?\\]", "");
        name = name.replaceAll("\\(.*?\\)", "");
        name = name.replaceAll("【.*?】", "");
        name = name.replaceAll("《.*?》", "");
        
        // 去除常见的不必要后缀
        String[] suffixesToRemove = {
            "\\s*CD$",
            "\\s*CD\\s*$",
            "\\s*VOL\\.$",
            "\\s*VOL\\.\\s*$",
            "\\s*Disc$",
            "\\s*Disc\\s*$"
        };
        
        for (String suffix : suffixesToRemove) {
            name = name.replaceAll(suffix, "");
        }
        
        // 去除多余的空格和特殊字符
        name = name.trim();
        name = name.replaceAll("\\s+", " ");
        name = name.replaceAll("[-_]{2,}", "-");
        
        return name;
    }
    
    /**
     * 修复不完整的括号
     */
    private String fixIncompleteBrackets(String name) {
        // 修复不完整的方括号
        int openBracketCount = name.length() - name.replace("[", "").length();
        int closeBracketCount = name.length() - name.replace("]", "").length();
        
        if (openBracketCount > closeBracketCount) {
            name += "]";
        } else if (closeBracketCount > openBracketCount) {
            name = "[" + name;
        }
        
        // 修复不完整的圆括号
        int openParenCount = name.length() - name.replace("(", "").length();
        int closeParenCount = name.length() - name.replace(")", "").length();
        
        if (openParenCount > closeParenCount) {
            name += ")";
        } else if (closeParenCount > openParenCount) {
            name = "(" + name;
        }
        
        return name;
    }
    
    /**
     * 提取文件类型
     */
    private String extractFileTypes(List<String> filenames) {
        Set<String> fileTypes = new TreeSet<>();
        
        for (String filename : filenames) {
            if (filename.contains("FLAC")) {
                fileTypes.add("FLAC");
            } else if (filename.contains("WAV")) {
                fileTypes.add("WAV");
            } else if (filename.contains("MP3")) {
                fileTypes.add("MP3");
            } else if (filename.contains("APE")) {
                fileTypes.add("APE");
            }
        }
        
        return String.join("+", fileTypes);
    }
    
    /**
     * 清理单个文件名
     */
    private String cleanSingleFilename(String filename) {
        // 去除文件扩展名
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            filename = filename.substring(0, lastDot);
        }
        
        // 去除CD/VOL序号
        filename = filename.replaceAll("(?i)(CD|VOL|Disc)\\s*\\d+", "");
        
        // 去除括号内容
        filename = filename.replaceAll("\\[.*?\\]", "");
        filename = filename.replaceAll("\\(.*?\\)", "");
        
        return filename.trim();
    }
    
    /**
     * 候选名称类
     */
    @Data
    private static class CollectionNameCandidate {
        private String name;
        private String algorithm;
        private double score;
        private double algorithmWeight;
        
        public CollectionNameCandidate(String name, String algorithm, double algorithmWeight) {
            this.name = name;
            this.algorithm = algorithm;
            this.algorithmWeight = algorithmWeight;
            this.score = 0.0;
        }
    }
}
