package com.filemanager.strategy.collection;

import java.util.List;

/**
 * 精确风格命名策略
 * 尽可能保留更多有用信息，只去除CD序号、文件格式等差异信息
 */
public class PreciseNamingStrategy extends BaseCollectionNamingStrategy {
    
    public PreciseNamingStrategy(StringSimilarityCalculator similarityCalculator) {
        super(similarityCalculator);
    }
    
    @Override
    public String getStrategyName() {
        return "精确风格";
    }
    
    @Override
    public String getStrategyDescription() {
        return "保留年份、专辑类型等重要信息，只去除CD序号、文件格式等差异信息";
    }
    
    @Override
    protected double getAlgorithmWeight(String algorithm, FilenamePattern pattern) {
        String mainPattern = pattern.getMainPattern();
        
        // 精确风格更倾向于模板提取和模式匹配算法
        switch (mainPattern) {
            case "bracket":
                if (algorithm.equals("template")) return 1.5;
                if (algorithm.equals("pattern")) return 1.4;
                if (algorithm.equals("semantic")) return 1.2;
                break;
            case "book_title":
                if (algorithm.equals("template")) return 1.5;
                if (algorithm.equals("pattern")) return 1.3;
                break;
            case "year_prefix":
                if (algorithm.equals("pattern")) return 1.4;
                if (algorithm.equals("template")) return 1.3;
                break;
            case "dash_separator":
                if (algorithm.equals("pattern")) return 1.3;
                if (algorithm.equals("template")) return 1.2;
                break;
            default:
                // 通用模式，模板提取权重最高
                if (algorithm.equals("template")) return 1.4;
                if (algorithm.equals("pattern")) return 1.3;
                break;
        }
        
        return 1.0;
    }
    
    @Override
    protected String optimizeFinalName(String name, List<String> filenames, FilenamePattern pattern) {
        String optimized = cleanName(name);
        optimized = removeExtraInfo(optimized);
        
        // 精确风格：保留更多有用信息，只去除CD序号、文件格式等差异信息
        // 不去除年份、专辑类型等信息
        
        // 兜底机制1：如果优化后的名称太短或为空，使用共同前缀方法
        if (optimized == null || optimized.trim().isEmpty() || optimized.trim().length() < 5) {
            String commonPrefix = findCommonPrefixIgnoringDifferences(filenames);
            if (commonPrefix != null && commonPrefix.length() >= 5) {
                return commonPrefix.trim();
            }
        }
        
        // 兜底机制2：如果名称仍然太短或无意义，使用第一个文件名的清理版本
        if (optimized == null || optimized.trim().isEmpty() || 
            optimized.trim().length() < 3 || 
            optimized.matches("^(CD|Disc|cd|VOL|Vol|\\d+)\\s*$")) {
            return getBestOriginalFilename(filenames);
        }
        
        return optimized.trim();
    }
    
    @Override
    protected String normalizeForPrefixComparison(String filename) {
        String normalized = filename;
        
        // 精确风格：只去除CD序号、文件格式等差异信息，保留年份等信息
        
        // 去除方括号中的CD编号（如CD-0221、CD-0176等）
        // 但保留"龙音海文版"等版本信息
        normalized = normalized.replaceAll("\\[龙音[海文香港环球]+版\\s+CD\\s*-\\d+\\]", "[龙音海文版]");
        normalized = normalized.replaceAll("\\[龙音[海文香港环球]+版\\s+[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        normalized = normalized.replaceAll("\\[龙音[海文香港环球]+版\\s*CD\\s*-\\d+\\]", "[龙音海文版]");
        normalized = normalized.replaceAll("\\[龙音[海文香港环球]+版\\s*[A-Z]{2,3}\\s*-\\s*\\d{4,6}\\]", "[龙音海文版]");
        
        // 不去除方括号中的版本号信息，保留完整的版本信息用于比较
        // 这样可以更好地识别共同的前缀
        
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
    
    @Override
    protected String cleanName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 精确风格：只去除年份前缀，保留其他年份信息
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        name = name.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
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
        
        // 精确风格：只去除CD序号、文件格式等差异信息，保留专辑名称、艺术家信息等
        
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
    
    @Override
    protected String cleanFilenameLight(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        String result = filename;
        
        // 精确风格：只去除文件格式信息，保留其他有用信息
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
        
        // 去除方括号中的剩余信息（如[+]等）
        result = result.replaceAll("\\[\\+\\]", "");
        
        // 去除年份前缀（但保留其他年份信息）
        result = result.replaceAll("^[.\\s]*\\d{4}\\s*-\\s*", "");
        result = result.replaceAll("^[.\\s]*\\d{4}\\s*\\.\\s*", "");
        
        // 去除多余空格
        result = result.trim();
        result = result.replaceAll("\\s+", " ");
        
        return result;
    }
}