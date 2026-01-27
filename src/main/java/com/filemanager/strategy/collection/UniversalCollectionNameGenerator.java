package com.filemanager.strategy.collection;

import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用合集名称生成器
 * 
 * 设计思路：
 * 1. 自动识别文件名模式，不依赖硬编码规则
 * 2. 使用多种算法生成候选名称
 * 3. 通过投票机制选择最佳结果
 * 4. 自适应权重，根据文件名特征调整算法权重
 * 
 * @author FileEditTools Team
 */
public class UniversalCollectionNameGenerator {
    
    private final StringSimilarityCalculator similarityCalculator;
    
    public UniversalCollectionNameGenerator(StringSimilarityCalculator similarityCalculator) {
        this.similarityCalculator = similarityCalculator;
    }
    
    /**
     * 生成合集名称（通用方法）
     */
    public String generateCollectionName(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return "未命名";
        }
        
        if (filenames.size() == 1) {
            return cleanSingleFilename(filenames.get(0));
        }
        
        // 1. 分析文件名模式
        FilenamePattern pattern = analyzeFilenamePattern(filenames);
        
        // 2. 使用多种算法生成候选名称
        List<CollectionNameCandidate> candidates = generateCandidates(filenames, pattern);
        
        if (candidates.isEmpty()) {
            return filenames.get(0);
        }
        
        // 3. 计算每个候选名称的得分
        for (CollectionNameCandidate candidate : candidates) {
            double score = calculateCandidateScore(candidate, filenames, pattern);
            candidate.setScore(score);
        }
        
        // 4. 选择得分最高的候选名称
        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        String bestName = candidates.get(0).getName();
        
        // 5. 清理和优化最终名称
        return optimizeFinalName(bestName, filenames, pattern);
    }
    
    /**
     * 分析文件名模式
     */
    private FilenamePattern analyzeFilenamePattern(List<String> filenames) {
        FilenamePattern pattern = new FilenamePattern();
        
        // 统计各种特征的出现频率
        int bracketCount = 0;
        int bookTitleCount = 0;
        int dashSeparatorCount = 0;
        int yearPrefixCount = 0;
        
        for (String filename : filenames) {
            // 检测方括号
            if (filename.contains("[") || filename.contains("]")) {
                bracketCount++;
            }
            
            // 检测书名号
            if (filename.contains("《") || filename.contains("》")) {
                bookTitleCount++;
            }
            
            // 检测横线分隔符
            if (filename.contains("-")) {
                dashSeparatorCount++;
            }
            
            // 检测年份前缀
            if (filename.matches(".*\\d{4}\\s*-.*")) {
                yearPrefixCount++;
            }
        }
        
        // 设置模式特征
        pattern.setBracketRatio((double) bracketCount / filenames.size());
        pattern.setBookTitleRatio((double) bookTitleCount / filenames.size());
        pattern.setDashSeparatorRatio((double) dashSeparatorCount / filenames.size());
        pattern.setYearPrefixRatio((double) yearPrefixCount / filenames.size());
        
        // 识别主要模式
        if (pattern.getBracketRatio() > 0.8) {
            pattern.setMainPattern("bracket");
        } else if (pattern.getBookTitleRatio() > 0.5) {
            pattern.setMainPattern("book_title");
        } else if (pattern.getYearPrefixRatio() > 0.5) {
            pattern.setMainPattern("year_prefix");
        } else if (pattern.getDashSeparatorRatio() > 0.5) {
            pattern.setMainPattern("dash_separator");
        } else {
            pattern.setMainPattern("generic");
        }
        
        return pattern;
    }
    
    /**
     * 生成候选名称
     */
    private List<CollectionNameCandidate> generateCandidates(List<String> filenames, FilenamePattern pattern) {
        List<CollectionNameCandidate> candidates = new ArrayList<>();
        
        // 算法1: 模板提取（根据识别的模式）
        String templateName = generateByTemplateExtraction(filenames, pattern);
        if (!templateName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(templateName, "模板提取算法", getAlgorithmWeight("template", pattern)));
        }
        
        // 算法2: 最长公共子串（LCS）
        String lcsName = generateByLongestCommonSubstring(filenames);
        if (!lcsName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(lcsName, "LCS算法", getAlgorithmWeight("lcs", pattern)));
        }
        
        // 算法3: 关键词提取
        String keywordName = generateByKeywordExtraction(filenames);
        if (!keywordName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(keywordName, "关键词提取算法", getAlgorithmWeight("keyword", pattern)));
        }
        
        // 算法4: 语义相似度聚合
        String semanticName = generateBySemanticAggregation(filenames);
        if (!semanticName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(semanticName, "语义聚合算法", getAlgorithmWeight("semantic", pattern)));
        }
        
        // 算法5: 模式匹配
        String patternName = generateByPatternMatching(filenames, pattern);
        if (!patternName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(patternName, "模式匹配算法", getAlgorithmWeight("pattern", pattern)));
        }
        
        return candidates;
    }
    
    /**
     * 根据模式获取算法权重
     */
    private double getAlgorithmWeight(String algorithm, FilenamePattern pattern) {
        String mainPattern = pattern.getMainPattern();
        
        // 根据主要模式调整算法权重
        switch (mainPattern) {
            case "bracket":
                if (algorithm.equals("template")) return 1.5;
                if (algorithm.equals("pattern")) return 1.3;
                break;
            case "book_title":
                if (algorithm.equals("template")) return 1.5;
                if (algorithm.equals("keyword")) return 1.2;
                break;
            case "year_prefix":
                if (algorithm.equals("pattern")) return 1.4;
                if (algorithm.equals("keyword")) return 1.2;
                break;
            case "dash_separator":
                if (algorithm.equals("pattern")) return 1.3;
                if (algorithm.equals("lcs")) return 1.1;
                break;
            default:
                // 通用模式，所有算法权重相近
                break;
        }
        
        return 1.0;
    }
    
    /**
     * 模板提取算法
     */
    private String generateByTemplateExtraction(List<String> filenames, FilenamePattern pattern) {
        Map<String, Integer> templateMatches = new HashMap<>();
        
        for (String filename : filenames) {
            String extracted = extractByPattern(filename, pattern.getMainPattern());
            if (!extracted.isEmpty()) {
                // 清理提取的结果
                extracted = cleanName(extracted);
                extracted = removeExtraInfo(extracted);
                if (extracted.length() >= 3) {
                    templateMatches.put(extracted, templateMatches.getOrDefault(extracted, 0) + 1);
                }
            }
        }
        
        // 找到匹配次数最多的模板
        String bestMatch = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : templateMatches.entrySet()) {
            if (entry.getValue() > maxCount && entry.getKey().length() >= 3) {
                maxCount = entry.getValue();
                bestMatch = entry.getKey();
            }
        }
        
        if (maxCount >= filenames.size() * 0.5 && !bestMatch.isEmpty()) {
            return bestMatch;
        }
        
        return "";
    }
    
    /**
     * 根据模式提取名称
     */
    private String extractByPattern(String filename, String pattern) {
        switch (pattern) {
            case "bracket":
                // 提取方括号后的内容（龙音模式）
                Matcher bracketMatcher = Pattern.compile("\\[[^\\]]+\\](.+)").matcher(filename);
                if (bracketMatcher.find()) {
                    String content = bracketMatcher.group(1).trim();
                    // 如果有横线，取横线前的内容
                    if (content.contains("-")) {
                        String[] parts = content.split("-");
                        if (parts.length > 0) {
                            return parts[0].trim();
                        }
                    }
                    return content;
                }
                break;
            case "book_title":
                // 提取书名号内的内容
                Matcher bookMatcher = Pattern.compile("《([^》]+)》").matcher(filename);
                if (bookMatcher.find()) {
                    return bookMatcher.group(1).trim();
                }
                break;
            case "year_prefix":
                // 提取年份后的内容
                Matcher yearMatcher = Pattern.compile("\\d{4}\\s*-\\s*(.+)").matcher(filename);
                if (yearMatcher.find()) {
                    return yearMatcher.group(1).trim();
                }
                break;
            case "dash_separator":
                // 提取第一个横线后的内容（滚石模式）
                String[] parts = filename.split("-");
                if (parts.length > 1) {
                    return parts[1].trim();
                }
                break;
            default:
                // 通用模式，尝试多种提取方式
                return extractGeneric(filename);
        }
        
        return "";
    }
    
    /**
     * 通用提取方法
     */
    private String extractGeneric(String filename) {
        // 1. 尝试书名号
        Matcher bookMatcher = Pattern.compile("《([^》]+)》").matcher(filename);
        if (bookMatcher.find()) {
            return bookMatcher.group(1).trim();
        }
        
        // 2. 尝试圆括号
        Matcher parenMatcher = Pattern.compile("\\(([^)]+)\\)").matcher(filename);
        if (parenMatcher.find()) {
            return parenMatcher.group(1).trim();
        }
        
        // 3. 尝试方括号
        Matcher bracketMatcher = Pattern.compile("\\[([^\\]]+)\\]").matcher(filename);
        if (bracketMatcher.find()) {
            return bracketMatcher.group(1).trim();
        }
        
        // 4. 尝试横线分隔
        String[] parts = filename.split("-");
        if (parts.length > 1) {
            return parts[parts.length - 1].trim();
        }
        
        return filename;
    }
    
    /**
     * 最长公共子串算法
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
        
        return cleanName(lcs);
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
     * 关键词提取算法
     */
    private String generateByKeywordExtraction(List<String> filenames) {
        Map<String, Integer> keywordCount = new HashMap<>();
        
        for (String filename : filenames) {
            String[] words = filename.split("[\\s\\-\\._\\[\\]\\(\\)\\<\\>\\《\\》\\【\\】]+");
            
            for (String word : words) {
                word = word.trim();
                // 过滤掉数字、CD序号、文件格式等无意义关键词
                if (word.length() >= 2 && !word.matches("^\\d+$") && 
                    !word.matches("^(CD|Disc|VOL|Vol|WAV|FLAC|MP3|DTS|CUE|分轨)\\d*$")) {
                    keywordCount.put(word, keywordCount.getOrDefault(word, 0) + 1);
                }
            }
        }
        
        // 找到出现次数最多的关键词
        List<Map.Entry<String, Integer>> sortedKeywords = new ArrayList<>(keywordCount.entrySet());
        sortedKeywords.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // 选择前2-3个关键词
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedKeywords) {
            if (count >= 3) break;
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(entry.getKey());
            count++;
        }
        
        String keywordName = result.toString();
        keywordName = cleanName(keywordName);
        keywordName = removeExtraInfo(keywordName);
        
        return keywordName;
    }
    
    /**
     * 语义相似度聚合算法
     */
    private String generateBySemanticAggregation(List<String> filenames) {
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
        
        double[] centrality = new double[filenames.size()];
        
        for (int i = 0; i < filenames.size(); i++) {
            double sum = 0.0;
            for (int j = 0; j < filenames.size(); j++) {
                sum += similarityMatrix[i][j];
            }
            centrality[i] = sum / filenames.size();
        }
        
        int bestIndex = 0;
        double maxCentrality = 0.0;
        
        for (int i = 0; i < filenames.size(); i++) {
            if (centrality[i] > maxCentrality) {
                maxCentrality = centrality[i];
                bestIndex = i;
            }
        }
        
        String bestFilename = filenames.get(bestIndex);
        String cleaned = cleanName(bestFilename);
        cleaned = removeExtraInfo(cleaned);
        return cleaned;
    }
    
    /**
     * 模式匹配算法
     */
    private String generateByPatternMatching(List<String> filenames, FilenamePattern pattern) {
        List<Pattern> patterns = Arrays.asList(
            // 书名号内的内容（优先级最高）
            Pattern.compile("《([^》]+)》"),
            // 方括号内的内容
            Pattern.compile("\\[([^\\]]+)\\]"),
            // 圆括号内的内容
            Pattern.compile("\\(([^)]+)\\)"),
            // 横线分隔后的内容（去除CD序号等）
            Pattern.compile(".*?\\s*-\\s*([^\\[\\(]+?)\\s*[\\[\\(]"),
            // 年份后的内容
            Pattern.compile(".*?\\d{4}\\s*-\\s*([^\\[\\(]+?)\\s*[\\[\\(]")
        );
        
        Map<String, Integer> patternMatches = new HashMap<>();
        
        for (String filename : filenames) {
            for (Pattern p : patterns) {
                Matcher matcher = p.matcher(filename);
                if (matcher.find()) {
                    String match = matcher.group(1).trim();
                    // 清理匹配结果
                    match = cleanName(match);
                    match = removeExtraInfo(match);
                    if (match.length() >= 2) {
                        patternMatches.put(match, patternMatches.getOrDefault(match, 0) + 1);
                    }
                }
            }
        }
        
        String bestMatch = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : patternMatches.entrySet()) {
            if (entry.getValue() > maxCount && entry.getKey().length() >= 3) {
                maxCount = entry.getValue();
                bestMatch = entry.getKey();
            }
        }
        
        if (maxCount >= filenames.size() * 0.5 && !bestMatch.isEmpty()) {
            return bestMatch;
        }
        
        return "";
    }
    
    /**
     * 计算候选名称的得分
     */
    private double calculateCandidateScore(CollectionNameCandidate candidate, List<String> filenames, FilenamePattern pattern) {
        String candidateName = candidate.getName();
        
        // 1. 计算与所有文件名的平均相似度
        double totalSimilarity = 0.0;
        for (String filename : filenames) {
            double similarity = similarityCalculator.calculateSimilarity(filename, candidateName);
            totalSimilarity += similarity;
        }
        double avgSimilarity = totalSimilarity / filenames.size();
        
        // 2. 计算名称的代表性
        double representativeness = calculateRepresentativeness(candidateName, filenames);
        
        // 3. 计算名称的简洁性
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
        int length = name.length();
        double lengthScore = 0.0;
        if (length >= 5 && length <= 30) {
            lengthScore = 1.0;
        } else if (length >= 3 && length <= 50) {
            lengthScore = 0.8;
        } else {
            lengthScore = 0.5;
        }
        
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
        
        return (lengthScore + keywordScore) / 2;
    }
    
    /**
     * 计算名称的简洁性
     */
    private double calculateConciseness(String name) {
        // 去除括号内容后的长度
        String cleaned = name.replaceAll("[\\[\\]\\(\\)\\【\\】\\《\\》]", "");
        double ratio = (double) cleaned.length() / name.length();
        
        // 比例越高，说明越简洁
        return Math.min(ratio, 1.0);
    }
    
    /**
     * 优化最终名称
     */
    private String optimizeFinalName(String name, List<String> filenames, FilenamePattern pattern) {
        String optimized = cleanName(name);
        optimized = removeExtraInfo(optimized);
        
        // 兜底机制：如果优化后的名称太短或为空，使用原始文件名
        if (optimized == null || optimized.trim().isEmpty() || optimized.trim().length() < 3) {
            return getBestOriginalFilename(filenames);
        }
        
        return optimized.trim();
    }
    
    /**
     * 获取最佳原始文件名（作为兜底机制）
     */
    private String getBestOriginalFilename(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return "未命名";
        }
        
        // 选择最短且包含最多信息的文件名
        String bestFilename = filenames.get(0);
        int bestScore = calculateFilenameScore(bestFilename);
        
        for (String filename : filenames) {
            int score = calculateFilenameScore(filename);
            if (score > bestScore) {
                bestScore = score;
                bestFilename = filename;
            }
        }
        
        // 清理文件名，但保留更多原始信息
        return cleanFilenameLight(bestFilename);
    }
    
    /**
     * 计算文件名得分（用于选择最佳文件名）
     */
    private int calculateFilenameScore(String filename) {
        int score = 0;
        
        // 长度适中得分（不要太长也不要太短）
        int length = filename.trim().length();
        if (length >= 10 && length <= 50) {
            score += 10;
        } else if (length >= 5 && length <= 80) {
            score += 5;
        }
        
        // 包含中文得分
        if (filename.matches(".*[\\u4e00-\\u9fa5].*")) {
            score += 5;
        }
        
        // 不包含太多特殊字符得分
        int specialCharCount = filename.replaceAll("[^\\s\\p{Punct}]", "").length();
        if (specialCharCount < filename.length() * 0.3) {
            score += 5;
        }
        
        return score;
    }
    
    /**
     * 轻量级清理文件名（保留更多原始信息）
     */
    private String cleanFilenameLight(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        String result = filename;
        
        // 只去除文件格式信息
        result = result.replaceAll("\\s*WAV\\s*\\+\\s*CUE\\b", "");
        result = result.replaceAll("\\s*WAV\\s*\\+\\s*分轨\\b", "");
        result = result.replaceAll("\\s*WAV\\b", "");
        result = result.replaceAll("\\s*FLAC\\b", "");
        result = result.replaceAll("\\s*MP3\\b", "");
        result = result.replaceAll("\\s*DTS\\b", "");
        result = result.replaceAll("\\s*CUE\\b", "");
        result = result.replaceAll("\\s*分轨\\b", "");
        result = result.replaceAll("\\s*APE\\b", "");
        
        // 去除CD序号
        result = result.replaceAll("\\s*CD\\s*\\d+\\b", "");
        result = result.replaceAll("\\s*Disc\\s*\\d+\\b", "");
        
        // 去除年份前缀
        result = result.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        result = result.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
        // 去除多余空格
        result = result.trim();
        result = result.replaceAll("\\s+", " ");
        
        return result;
    }
    
    /**
     * 清理名称
     */
    private String cleanName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除年份前缀
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
        // 去除多余空格
        name = name.trim();
        name = name.replaceAll("\\s+", " ");
        
        return name;
    }
    
    /**
     * 去除额外信息
     */
    private String removeExtraInfo(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 只去除明显不必要的内容
        
        // 1. 去除CD序号（包括CD1、CD01、CD 1等格式）
        name = name.replaceAll("\\s*CD\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*Disc\\s*\\d+\\b", "");
        
        // 2. 去除文件格式信息（包括WAV、FLAC、MP3、DTS、CUE等）
        name = name.replaceAll("\\s*WAV\\s*\\+\\s*CUE\\b", "");
        name = name.replaceAll("\\s*WAV\\s*\\+\\s*分轨\\b", "");
        name = name.replaceAll("\\s*WAV\\b", "");
        name = name.replaceAll("\\s*FLAC\\b", "");
        name = name.replaceAll("\\s*MP3\\b", "");
        name = name.replaceAll("\\s*DTS\\b", "");
        name = name.replaceAll("\\s*CUE\\b", "");
        name = name.replaceAll("\\s*分轨\\b", "");
        
        // 3. 去除特殊字符和多余空格
        name = name.replaceAll("\\s+", " ");
        name = name.trim();
        
        // 4. 如果清理后的名称太短或只包含无意义字符，返回原始名称
        if (name.length() < 3 || name.matches("^(WAV|CUE|DTS|MP3|FLAC|分轨|VOL|CD|Disc)\\s*$")) {
            return "";
        }
        
        return name;
    }
    
    /**
     * 清理单个文件名
     */
    private String cleanSingleFilename(String filename) {
        String cleaned = cleanName(filename);
        cleaned = removeExtraInfo(cleaned);
        return cleaned;
    }
    
    /**
     * 文件名模式类
     */
    @Data
    private static class FilenamePattern {
        private String mainPattern;
        private double bracketRatio;
        private double bookTitleRatio;
        private double dashSeparatorRatio;
        private double yearPrefixRatio;
    }
    
    /**
     * 候选名称类
     */
    @Data
    private static class CollectionNameCandidate {
        private String name;
        private String algorithm;
        private double algorithmWeight;
        private double score;
        
        public CollectionNameCandidate(String name, String algorithm, double algorithmWeight) {
            this.name = name;
            this.algorithm = algorithm;
            this.algorithmWeight = algorithmWeight;
            this.score = 0.0;
        }
    }
}
