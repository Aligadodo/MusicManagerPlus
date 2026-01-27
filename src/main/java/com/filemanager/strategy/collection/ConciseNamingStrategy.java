package com.filemanager.strategy.collection;

import java.util.List;

/**
 * 简洁风格命名策略
 * 保留核心关键词，去除年份、文件格式等冗余信息
 */
public class ConciseNamingStrategy extends BaseCollectionNamingStrategy {
    
    public ConciseNamingStrategy(StringSimilarityCalculator similarityCalculator) {
        super(similarityCalculator);
    }
    
    @Override
    public String getStrategyName() {
        return "简洁风格";
    }
    
    @Override
    public String getStrategyDescription() {
        return "保留核心关键词，去除年份、文件格式等冗余信息";
    }
    
    @Override
    protected double getAlgorithmWeight(String algorithm, FilenamePattern pattern) {
        String mainPattern = pattern.getMainPattern();
        
        // 简洁风格更倾向于关键词提取和LCS算法
        switch (mainPattern) {
            case "bracket":
                if (algorithm.equals("keyword")) return 1.5;
                if (algorithm.equals("lcs")) return 1.3;
                if (algorithm.equals("template")) return 1.2;
                break;
            case "book_title":
                if (algorithm.equals("keyword")) return 1.5;
                if (algorithm.equals("template")) return 1.3;
                break;
            case "year_prefix":
                if (algorithm.equals("keyword")) return 1.4;
                if (algorithm.equals("lcs")) return 1.2;
                break;
            case "dash_separator":
                if (algorithm.equals("keyword")) return 1.4;
                if (algorithm.equals("lcs")) return 1.3;
                break;
            default:
                // 通用模式，关键词提取权重最高
                if (algorithm.equals("keyword")) return 1.5;
                if (algorithm.equals("lcs")) return 1.2;
                break;
        }
        
        return 1.0;
    }
    
    @Override
    protected String optimizeFinalName(String name, List<String> filenames, FilenamePattern pattern) {
        String optimized = cleanName(name);
        optimized = removeExtraInfo(optimized);
        
        // 简洁风格：去除年份、专辑类型等冗余信息
        optimized = removeYearInfo(optimized);
        optimized = removeAlbumTypeInfo(optimized);
        
        // 兜底机制1：如果优化后的名称太短或为空，使用共同前缀方法
        if (optimized == null || optimized.trim().isEmpty() || optimized.trim().length() < 3) {
            String commonPrefix = findCommonPrefixIgnoringDifferences(filenames);
            if (commonPrefix != null && commonPrefix.length() >= 3) {
                return commonPrefix.trim();
            }
        }
        
        // 兜底机制2：如果名称仍然太短或无意义，使用第一个文件名的清理版本
        if (optimized == null || optimized.trim().isEmpty() || 
            optimized.trim().length() < 2 || 
            optimized.matches("^(CD|Disc|cd|VOL|Vol|\\d+)\\s*$")) {
            return getBestOriginalFilename(filenames);
        }
        
        return optimized.trim();
    }
    
    @Override
    protected String normalizeForPrefixComparison(String filename) {
        String normalized = super.normalizeForPrefixComparison(filename);
        
        // 简洁风格：去除年份信息
        normalized = removeYearInfo(normalized);
        
        // 去除专辑类型信息（如"精选"、"合集"等）
        normalized = removeAlbumTypeInfo(normalized);
        
        return normalized;
    }
    
    @Override
    protected String cleanName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除年份前缀
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
        // 去除年份信息（如1998、2005等）
        name = name.replaceAll("\\s*\\d{4}\\s*", "");
        
        // 去除多余空格
        name = name.trim();
        name = name.replaceAll("\\s+", " ");
        
        return name;
    }
    
    @Override
    protected String removeExtraInfo(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除CD序号和数量信息
        name = name.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        name = name.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        
        // 去除文件格式信息
        name = name.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*[Cc][Uu][Ee]\\b", "");
        name = name.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*分轨\\b", "");
        name = name.replaceAll("\\s*[Ww][Aa][Vv]\\b", "");
        name = name.replaceAll("\\s*[Ff][Ll][Aa][Cc]\\b", "");
        name = name.replaceAll("\\s*[Mm][Pp]3\\b", "");
        name = name.replaceAll("\\s*[Dd][Tt][Ss]\\b", "");
        name = name.replaceAll("\\s*[Cc][Uu][Ee]\\b", "");
        name = name.replaceAll("\\s*分轨\\b", "");
        name = name.replaceAll("\\s*[Aa][Pp][Ee]\\b", "");
        
        // 去除方括号中的CD序号
        name = name.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        name = name.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        
        // 去除多余空格
        name = name.replaceAll("\\s+", " ");
        name = name.trim();
        
        // 如果清理后的名称太短，返回空
        if (name.length() < 2) {
            return "";
        }
        
        return name;
    }
    
    @Override
    protected String cleanFilenameLight(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        String result = filename;
        
        // 去除文件格式信息
        result = result.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*[Cc][Uu][Ee]\\b", "");
        result = result.replaceAll("\\s*[Ww][Aa][Vv]\\s*\\+\\s*分轨\\b", "");
        result = result.replaceAll("\\s*[Ww][Aa][Vv]\\b", "");
        result = result.replaceAll("\\s*[Ff][Ll][Aa][Cc]\\b", "");
        result = result.replaceAll("\\s*[Mm][Pp]3\\b", "");
        result = result.replaceAll("\\s*[Dd][Tt][Ss]\\b", "");
        result = result.replaceAll("\\s*[Cc][Uu][Ee]\\b", "");
        result = result.replaceAll("\\s*分轨\\b", "");
        result = result.replaceAll("\\s*[Aa][Pp][Ee]\\b", "");
        
        // 去除CD序号（包括.cd1、.cd2、.cd3等格式）
        result = result.replaceAll("\\.[Cc][Dd]\\d+\\s*$", "");
        result = result.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        result = result.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        
        // 去除CD数量信息（如3CD、2CD等）
        result = result.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        
        // 去除方括号中的CD序号（如[CD1]、[CD2]等）
        result = result.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        result = result.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        
        // 去除方括号中的版本号信息（如[龙音香港版 RC-011007-3C]等）
        result = result.replaceAll("\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}-\\d{4,6}-?\\d*[A-Z]?\\]", "");
        result = result.replaceAll("\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "");
        
        // 去除方括号中的剩余信息（如[+]等）
        result = result.replaceAll("\\[\\+\\]", "");
        
        // 简洁风格：去除年份信息
        result = removeYearInfo(result);
        
        // 去除专辑类型信息
        result = removeAlbumTypeInfo(result);
        
        // 去除多余空格
        result = result.trim();
        result = result.replaceAll("\\s+", " ");
        
        return result;
    }
    
    /**
     * 去除年份信息
     */
    private String removeYearInfo(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除年份信息（如1998、2005等）
        name = name.replaceAll("\\s*\\d{4}\\s*", "");
        
        return name;
    }
    
    /**
     * 去除专辑类型信息
     */
    private String removeAlbumTypeInfo(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去除专辑类型信息（如"精选"、"合集"、"系列"等）
        name = name.replaceAll("\\s*精选\\b", "");
        name = name.replaceAll("\\s*合集\\b", "");
        name = name.replaceAll("\\s*系列\\b", "");
        name = name.replaceAll("\\s*专辑\\b", "");
        name = name.replaceAll("\\s*全集\\b", "");
        name = name.replaceAll("\\s*精选集\\b", "");
        name = name.replaceAll("\\s*合集\\b", "");
        
        return name;
    }
}