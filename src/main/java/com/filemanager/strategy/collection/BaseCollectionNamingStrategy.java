package com.filemanager.strategy.collection;

import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 合集命名策略基础类
 * 提取可复用的规则和方法
 */
public abstract class BaseCollectionNamingStrategy implements ICollectionNamingStrategy {
    
    protected final StringSimilarityCalculator similarityCalculator;
    
    public BaseCollectionNamingStrategy(StringSimilarityCalculator similarityCalculator) {
        this.similarityCalculator = similarityCalculator;
    }
    
    /**
     * 生成合集名称（通用方法）
     */
    @Override
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
            
            // 检查信息丢失率：如果删除了太多信息，降低得分
            double infoLossPenalty = calculateInfoLossPenalty(candidate, filenames);
            score = score * (1.0 - infoLossPenalty);
            
            candidate.setScore(score);
        }
        
        // 4. 选择得分最高的候选名称
        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        String bestName = candidates.get(0).getName();
        
        // 调试信息：打印所有候选名称
        System.out.println("候选名称列表:");
        for (CollectionNameCandidate candidate : candidates) {
            double infoLoss = calculateInfoLossPenalty(candidate, filenames);
            System.out.println("  - " + candidate.getName() + " (得分: " + candidate.getScore() + ", 算法: " + candidate.getAlgorithm() + ", 信息丢失率: " + (infoLoss * 100) + "%)");
        }
        System.out.println("最佳候选名称: " + bestName);
        
        // 5. 清理和优化最终名称（根据策略类型）
        return optimizeFinalName(bestName, filenames, pattern);
    }
    
    /**
     * 清理和优化最终名称（由子类实现具体逻辑）
     */
    protected abstract String optimizeFinalName(String name, List<String> filenames, FilenamePattern pattern);
    
    /**
     * 分析文件名模式
     */
    protected FilenamePattern analyzeFilenamePattern(List<String> filenames) {
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
    protected List<CollectionNameCandidate> generateCandidates(List<String> filenames, FilenamePattern pattern) {
        List<CollectionNameCandidate> candidates = new ArrayList<>();
        
        // 算法1: 改进的模板提取（根据识别的模式）
        String templateName = generateByImprovedTemplateExtraction(filenames, pattern);
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
        
        // 算法5: 模板选择（TEMPLATE策略专用）
        String templateSelectionName = generateByTemplateSelection(filenames);
        if (!templateSelectionName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(templateSelectionName, "模板选择算法", getAlgorithmWeight("template", pattern)));
        }
        
        // 算法6: 模式匹配
        String patternName = generateByPatternMatching(filenames, pattern);
        if (!patternName.isEmpty()) {
            candidates.add(new CollectionNameCandidate(patternName, "模式匹配算法", getAlgorithmWeight("pattern", pattern)));
        }
        
        return candidates;
    }
    
    /**
     * 根据模式获取算法权重（由子类重写）
     */
    protected double getAlgorithmWeight(String algorithm, FilenamePattern pattern) {
        return 1.0;
    }
    
    /**
     * 模板提取算法
     */
    protected String generateByTemplateExtraction(List<String> filenames, FilenamePattern pattern) {
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
        
        // 如果没有找到共同模板（每个文件名都不同），尝试找到共同部分
        if (templateMatches.size() >= filenames.size()) {
            List<String> extractedContents = new ArrayList<>(templateMatches.keySet());
            String commonPart = findCommonPart(extractedContents);
            if (!commonPart.isEmpty() && commonPart.length() >= 3) {
                return commonPart;
            }
        }
        
        // 如果匹配次数足够，返回最佳匹配
        if (maxCount >= filenames.size() * 0.5 && !bestMatch.isEmpty()) {
            return bestMatch;
        }
        
        // 如果最佳匹配只包含单个文件名的特征（如"之一"、"之二"等），使用共同前缀方法
        if (bestMatch.matches(".*[之一之二之三之四之五].*")) {
            return findCommonPrefixIgnoringDifferences(filenames);
        }
        
        // 如果没有找到合适的模板，尝试使用共同前缀方法
        if (bestMatch.length() < 5 || bestMatch.matches("^(CD|Disc|cd|VOL|Vol|\\d+|RC|RA|RB)\\s*$")) {
            return findCommonPrefixIgnoringDifferences(filenames);
        }
        
        return "";
    }
    
    /**
     * 改进的模板提取算法，用于处理龙音系列等特殊情况
     */
    protected String generateByImprovedTemplateExtraction(List<String> filenames, FilenamePattern pattern) {
        // 先尝试使用原始的模板提取算法
        String templateName = generateByTemplateExtraction(filenames, pattern);
        if (!templateName.isEmpty()) {
            return templateName;
        }
        
        // 如果原始算法失败，尝试使用改进的方法
        // 对于龙音系列，提取方括号后的内容，然后找到共同的部分
        if (pattern.getMainPattern().equals("bracket")) {
            List<String> extractedContents = new ArrayList<>();
            for (String filename : filenames) {
                String extracted = extractByPattern(filename, "bracket");
                if (!extracted.isEmpty()) {
                    extractedContents.add(extracted);
                }
            }
            
            if (!extractedContents.isEmpty()) {
                // 找到所有提取内容的共同部分
                String commonPart = findCommonPart(extractedContents);
                if (!commonPart.isEmpty() && commonPart.length() >= 3) {
                    return commonPart;
                }
            }
        }
        
        return "";
    }
    
    /**
     * 找到多个字符串的共同部分
     */
    protected String findCommonPart(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        
        if (strings.size() == 1) {
            return strings.get(0);
        }
        
        // 找到第一个字符串的所有子串，然后检查是否在其他字符串中出现
        String firstString = strings.get(0);
        String bestCommonPart = "";
        int maxLength = 0;
        
        for (int i = 0; i < firstString.length(); i++) {
            for (int j = i + 1; j <= firstString.length(); j++) {
                String substring = firstString.substring(i, j);
                if (substring.length() > maxLength && isCommonSubstring(substring, strings)) {
                    bestCommonPart = substring;
                    maxLength = substring.length();
                }
            }
        }
        
        // 清理共同部分，去除前面的分隔符
        if (!bestCommonPart.isEmpty()) {
            bestCommonPart = bestCommonPart.replaceAll("^[\\-\\s]+", "");
            bestCommonPart = bestCommonPart.trim();
        }
        
        return bestCommonPart;
    }
    
    /**
     * 检查子串是否在所有字符串中出现
     */
    protected boolean isCommonSubstring(String substring, List<String> strings) {
        for (String str : strings) {
            if (!str.contains(substring)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 根据模式提取名称
     */
    protected String extractByPattern(String filename, String pattern) {
        switch (pattern) {
            case "bracket":
                // 提取方括号后的内容（龙音模式）
                Matcher bracketMatcher = Pattern.compile("\\[[^\\]]+\\](.+)").matcher(filename);
                if (bracketMatcher.find()) {
                    String content = bracketMatcher.group(1).trim();
                    // 清理CD序号和文件格式信息
                    content = content.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
                    content = content.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
                    content = content.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*[Cc][Uu][Ee]\\b", "");
                    content = content.replaceAll("\\s*[Ww][Aa][Vv]\\b", "");
                    content = content.replaceAll("\\s*[Ff][Ll][Aa][Cc]\\b", "");
                    content = content.trim();
                    // 去除序号（如"之一"、"之二"等）
                    content = content.replaceAll("[之][一二三四五六七八九十]+\\s*$", "");
                    content = content.trim();
                    // 不再根据横线分割，保留完整内容
                    // 因为横线可能是曲名和系列名称的分隔符
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
    protected String extractGeneric(String filename) {
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
     * 最长公共子串算法（改进版，避免提取到CD序号等无意义信息）
     */
    protected String generateByLongestCommonSubstring(List<String> filenames) {
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
        
        String cleaned = cleanName(lcs);
        cleaned = removeExtraInfo(cleaned);
        
        // 如果LCS太短或无意义，尝试找共同前缀
        if (cleaned.length() < 5 || cleaned.matches("^(CD|Disc|cd|VOL|Vol|\\d+)\\s*$")) {
            return findCommonPrefixIgnoringDifferences(filenames);
        }
        
        return cleaned;
    }
    
    /**
     * 查找两个字符串的最长公共子串
     */
    protected String findLongestCommonSubstring(String s1, String s2) {
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
     * 找到忽略差异信息的共同前缀
     */
    protected String findCommonPrefixIgnoringDifferences(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return "";
        }
        
        // 标准化所有文件名，去除差异信息
        List<String> normalizedFilenames = new ArrayList<>();
        for (String filename : filenames) {
            String normalized = normalizeForPrefixComparison(filename);
            normalizedFilenames.add(normalized);
        }
        
        // 找到所有标准化文件名的共同前缀
        String commonPrefix = normalizedFilenames.get(0);
        for (int i = 1; i < normalizedFilenames.size(); i++) {
            commonPrefix = findCommonPrefix(commonPrefix, normalizedFilenames.get(i));
            if (commonPrefix.isEmpty()) {
                break;
            }
        }
        
        // 如果共同前缀太短，尝试使用第一个文件名的清理版本
        if (commonPrefix.length() < 5) {
            return cleanFilenameLight(filenames.get(0));
        }
        
        return commonPrefix.trim();
    }
    
    /**
     * 标准化文件名用于前缀比较（由子类重写以实现不同策略）
     */
    protected String normalizeForPrefixComparison(String filename) {
        String normalized = filename;
        
        // 去除方括号中的CD编号（如CD-0221、CD-0176等）
        // 但保留"龙音海文版"等版本信息
        normalized = normalized.replaceAll("\\[龙音[海文香港环球]+版\\s*CD\\s*-\\d+\\]", "[龙音海文版]");
        normalized = normalized.replaceAll("\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        
        // 去除CD序号（包括CD1、CD01、CD 1、cd1、cd2等格式）
        normalized = normalized.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        normalized = normalized.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        
        // 去除CD数量信息（如3CD、2CD等）
        normalized = normalized.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        
        // 去除文件格式信息（包括WAV、FLAC、MP3、DTS、CUE等）
        normalized = normalized.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*[Cc][Uu][Ee]\\b", "");
        normalized = normalized.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*分轨\\b", "");
        normalized = normalized.replaceAll("\\s*[Ww][Aa][Vv]\\b", "");
        normalized = normalized.replaceAll("\\s*[Ff][Ll][Aa][Cc]\\b", "");
        normalized = normalized.replaceAll("\\s*[Mm][Pp]3\\b", "");
        normalized = normalized.replaceAll("\\s*[Dd][Tt][Ss]\\b", "");
        normalized = normalized.replaceAll("\\s*[Cc][Uu][Ee]\\b", "");
        normalized = normalized.replaceAll("\\s*分轨\\b", "");
        normalized = normalized.replaceAll("\\s*[Aa][Pp][Ee]\\b", "");
        
        // 去除方括号中的CD序号（如[CD1]、[CD2]等）
        normalized = normalized.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        normalized = normalized.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        
        // 去除多余空格和特殊字符
        normalized = normalized.trim();
        normalized = normalized.replaceAll("\\s+", " ");
        
        return normalized;
    }
    
    /**
     * 找到两个字符串的共同前缀
     */
    protected String findCommonPrefix(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        int i = 0;
        
        while (i < minLength && s1.charAt(i) == s2.charAt(i)) {
            i++;
        }
        
        return s1.substring(0, i);
    }
    
    /**
     * 关键词提取算法
     */
    protected String generateByKeywordExtraction(List<String> filenames) {
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
    protected String generateBySemanticAggregation(List<String> filenames) {
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
     * 模板选择算法（由子类重写以实现不同策略）
     */
    protected String generateByTemplateSelection(List<String> filenames) {
        // 默认实现：返回空字符串，由子类重写
        return "";
    }
    
    /**
     * 模式匹配算法
     */
    protected String generateByPatternMatching(List<String> filenames, FilenamePattern pattern) {
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
     * 计算信息丢失率（如果删除了太多信息，降低得分）
     */
    protected double calculateInfoLossPenalty(CollectionNameCandidate candidate, List<String> filenames) {
        String candidateName = candidate.getName();
        if (candidateName == null || candidateName.isEmpty()) {
            return 1.0; // 完全丢失
        }
        
        // 计算候选名称与所有原始文件名的平均信息保留率
        double totalRetention = 0.0;
        for (String filename : filenames) {
            double retention = calculateInfoRetention(candidateName, filename);
            totalRetention += retention;
        }
        double avgRetention = totalRetention / filenames.size();
        
        // 如果信息保留率低于85%（丢失率超过15%），则应用惩罚
        if (avgRetention < 0.85) {
            // 惩罚系数：丢失率越高，惩罚越大
            double lossRate = 1.0 - avgRetention;
            return lossRate * 2.0; // 放大惩罚效果
        }
        
        return 0.0; // 没有惩罚
    }
    
    /**
     * 计算候选名称相对于原始文件名的信息保留率
     */
    protected double calculateInfoRetention(String candidateName, String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return 1.0;
        }
        
        if (candidateName == null || candidateName.isEmpty()) {
            return 0.0;
        }
        
        // 清理原始文件名，去除CD编号、文件格式等差异信息
        String cleanedOriginal = cleanFilenameLight(originalFilename);
        
        // 使用相似度计算器来计算信息保留率
        // 这样可以更准确地反映候选名称与原始文件名的相似程度
        // 考虑字符顺序和连续性，避免简单的字符匹配导致的高分
        double retention = similarityCalculator.calculateSimilarity(candidateName, cleanedOriginal);
        
        // 确保保留率在0-1之间
        return Math.max(0.0, Math.min(1.0, retention));
    }
    
    /**
     * 计算候选名称的得分
     */
    protected double calculateCandidateScore(CollectionNameCandidate candidate, List<String> filenames, FilenamePattern pattern) {
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
    protected double calculateRepresentativeness(String name, List<String> filenames) {
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
    protected double calculateConciseness(String name) {
        // 去除括号内容后的长度
        String cleaned = name.replaceAll("[\\[\\]\\(\\)\\【\\】\\《\\》]", "");
        double ratio = (double) cleaned.length() / name.length();
        
        // 比例越高，说明越简洁
        return Math.min(ratio, 1.0);
    }
    
    /**
     * 清理名称（由子类重写以实现不同策略）
     */
    protected String cleanName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除方括号中的CD编号（如CD-0221、CD-0176等）
        // 但保留"龙音海文版"等版本信息
        // 使用更宽松的匹配模式，确保能匹配到所有变体
        name = name.replaceAll("\\[龙音[海文香港环球]+版[^\\]]*\\d+[^\\]]*\\]", "[龙音海文版]");
        
        // 去除年份前缀
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
        // 去除多余空格
        name = name.trim();
        name = name.replaceAll("\\s+", " ");
        
        return name;
    }
    
    /**
     * 去除额外信息（由子类重写以实现不同策略）
     */
    protected String removeExtraInfo(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除方括号中的CD编号（如CD-0221、CD-0176等）
        // 但保留"龙音海文版"等版本信息
        name = name.replaceAll("\\[龙音[海文香港环球]+版\\s+CD\\s*-\\d+\\]", "[龙音海文版]");
        name = name.replaceAll("\\[龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        name = name.replaceAll("\\[龙音[海文香港环球]+版\\s*CD\\s*-\\d+\\]", "[龙音海文版]");
        name = name.replaceAll("\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        
        // 去除龙音系列中的CD编号（不包含方括号的情况）
        name = name.replaceAll("龙音[海文香港环球]+版\\s+CD\\s*-\\d+", "龙音海文版");
        name = name.replaceAll("龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        name = name.replaceAll("龙音[海文香港环球]+版\\s*CD\\s*-\\d+", "龙音海文版");
        name = name.replaceAll("龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}", "龙音海文版");
        
        // 只去除明显不必要的内容，保留专辑名称、艺术家信息等
        
        // 1. 去除CD序号（包括CD1、CD01、CD 1等格式）
        name = name.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        
        // 2. 去除CD数量信息（如3CD、2CD等）
        name = name.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        
        // 3. 去除文件格式信息（包括WAV、FLAC、MP3、DTS、CUE等）
        name = name.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*[Cc][Uu][Ee]\\b", "");
        name = name.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*分轨\\b", "");
        name = name.replaceAll("\\s*[Ww][Aa][Vv]\\b", "");
        name = name.replaceAll("\\s*[Ff][Ll][Aa][Cc]\\b", "");
        name = name.replaceAll("\\s*[Mm][Pp]3\\b", "");
        name = name.replaceAll("\\s*[Dd][Tt][Ss]\\b", "");
        name = name.replaceAll("\\s*[Cc][Uu][Ee]\\b", "");
        name = name.replaceAll("\\s*分轨\\b", "");
        name = name.replaceAll("\\s*[Aa][Pp][Ee]\\b", "");
        
        // 4. 去除方括号中的CD序号（如[CD1]、[CD2]等）
        name = name.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        name = name.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        
        // 5. 去除特殊字符和多余空格
        name = name.replaceAll("\\s+", " ");
        name = name.trim();
        
        // 6. 如果清理后的名称太短或只包含无意义字符，返回原始名称
        if (name.length() < 3 || name.matches("^(WAV|CUE|DTS|MP3|FLAC|分轨|VOL|CD|Disc)\\s*$")) {
            return "";
        }
        
        return name;
    }
    
    /**
     * 获取最佳原始文件名（作为兜底机制）
     */
    protected String getBestOriginalFilename(List<String> filenames) {
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
    protected int calculateFilenameScore(String filename) {
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
     * 轻量级清理文件名（由子类重写以实现不同策略）
     */
    protected String cleanFilenameLight(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        String result = filename;
        
        // 去除方括号中的CD编号（如CD-0221、CD-0176等）
        // 但保留"龙音海文版"等版本信息
        result = result.replaceAll("\\[龙音[海文香港环球]+版\\s+CD\\s*-\\d+\\]", "[龙音海文版]");
        result = result.replaceAll("\\[龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        result = result.replaceAll("\\[龙音[海文香港环球]+版\\s*CD\\s*-\\d+\\]", "[龙音海文版]");
        result = result.replaceAll("\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        
        // 只去除文件格式信息（保留其他有用信息）
        result = result.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*[Cc][Uu][Ee]\\b", "");
        result = result.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*分轨\\b", "");
        result = result.replaceAll("\\s*[Ww][Aa][Vv]\\b", "");
        result = result.replaceAll("\\s*[Ff][Ll][Aa][Cc]\\b", "");
        result = result.replaceAll("\\s*[Mm][Pp]3\\b", "");
        result = result.replaceAll("\\s*[Dd][Tt][Ss]\\b", "");
        result = result.replaceAll("\\s*[Cc][Uu][Ee]\\b", "");
        result = result.replaceAll("\\s*分轨\\b", "");
        result = result.replaceAll("\\s*[Aa][Pp][Ee]\\b", "");
        
        // 去除CD序号（但保留其他数字信息）
        result = result.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        result = result.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        
        // 去除CD数量信息（如3CD、2CD等）
        result = result.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        
        // 去除方括号中的CD序号（如[CD1]、[CD2]等）
        result = result.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        result = result.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        
        // 去除年份前缀（但保留其他年份信息）
        result = result.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        result = result.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
        // 去除多余空格
        result = result.trim();
        result = result.replaceAll("\\s+", " ");
        
        return result;
    }
    
    /**
     * 清理单个文件名
     */
    protected String cleanSingleFilename(String filename) {
        String cleaned = cleanName(filename);
        cleaned = removeExtraInfo(cleaned);
        return cleaned;
    }
    
    /**
     * 文件名模式类
     */
    @Data
    protected static class FilenamePattern {
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
    protected static class CollectionNameCandidate {
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