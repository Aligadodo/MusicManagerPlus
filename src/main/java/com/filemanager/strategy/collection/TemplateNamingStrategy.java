package com.filemanager.strategy.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 选取模板命名策略（最极简的兜底策略）
 * 按名称排序后取中间的一个，只做最简单的干扰文本去除，保证只去除不超过20%的文本
 */
public class TemplateNamingStrategy extends BaseCollectionNamingStrategy {
    
    private static final double MAX_REMOVAL_RATE = 0.20; // 最大去除率20%
    
    public TemplateNamingStrategy(StringSimilarityCalculator similarityCalculator) {
        super(similarityCalculator);
    }
    
    @Override
    public String getStrategyName() {
        return "选取模板";
    }
    
    @Override
    public String getStrategyDescription() {
        return "按名称排序后取中间的一个，只做最简单的干扰文本去除，保证信息丢失率不超过20%";
    }
    
    @Override
    protected double getAlgorithmWeight(String algorithm, FilenamePattern pattern) {
        // TEMPLATE策略只使用模板提取算法
        if (algorithm.equals("template")) {
            return 2.0; // 给予最高权重
        }
        return 0.5; // 其他算法权重降低
    }
    
    @Override
    protected String optimizeFinalName(String name, List<String> filenames, FilenamePattern pattern) {
        // TEMPLATE策略直接使用模板选择算法的结果，忽略传入的name参数
        // 因为模板选择算法已经在generateByTemplateSelection中实现了完整的逻辑
        String templateName = generateByTemplateSelection(filenames);
        
        if (templateName != null && !templateName.isEmpty()) {
            return templateName;
        }
        
        // 兜底：使用第一个文件名的清理版本
        return cleanFilenameLight(filenames.get(0));
    }
    
    /**
     * 模板选择算法：按名称排序后取中间的一个
     */
    protected String generateByTemplateSelection(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            return "";
        }
        
        if (filenames.size() == 1) {
            return cleanFilenameLight(filenames.get(0));
        }
        
        // 复制文件名列表并排序
        List<String> sortedFilenames = new ArrayList<>(filenames);
        Collections.sort(sortedFilenames);
        
        // 取中间的文件名
        int middleIndex = sortedFilenames.size() / 2;
        String middleFilename = sortedFilenames.get(middleIndex);
        
        // 清理文件名，只去除最简单的干扰信息
        String cleaned = cleanMinimal(middleFilename);
        
        // 检查信息丢失率
        double removalRate = calculateRemovalRate(middleFilename, cleaned);
        
        // 如果去除率超过20%，则使用更保守的清理方式
        if (removalRate > MAX_REMOVAL_RATE) {
            cleaned = cleanVeryMinimal(middleFilename);
        }
        
        return cleaned;
    }
    
    /**
     * 最小化清理：只去除最明显的干扰信息
     */
    protected String cleanMinimal(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        String result = filename;
        
        // 1. 去除方括号中的CD编号（如CD-0221、CD-0176等）
        // 但保留"龙音海文版"等版本信息
        result = result.replaceAll("\\[龙音[海文香港环球]+版[^\\]]*\\d+[^\\]]*\\]", "[龙音海文版]");
        
        // 2. 去除文件扩展名
        int lastDotIndex = result.lastIndexOf('.');
        if (lastDotIndex > 0) {
            result = result.substring(0, lastDotIndex);
        }
        
        // 3. 去除CD序号（如CD1、CD01、CD 1、cd1、cd2等格式）
        result = result.replaceAll("\\s*[Cc][Dd]\\s*\\d+\\b", "");
        result = result.replaceAll("\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\b", "");
        
        // 4. 去除CD数量信息（如3CD、2CD等）
        result = result.replaceAll("\\s*\\d+\\s*[Cc][Dd]\\b", "");
        
        // 5. 去除方括号中的CD序号（如[CD1]、[CD2]等）
        result = result.replaceAll("\\[\\s*[Cc][Dd]\\s*\\d+\\s*\\]", "");
        result = result.replaceAll("\\[\\s*[Dd][Ii][Ss][Cc]\\s*\\d+\\s*\\]", "");
        
        // 6. 去除序号（如之一、之二、之三等）
        result = result.replaceAll("[之][一二三四五六七八九十]+", "");
        
        // 7. 去除多余空格和特殊字符
        result = result.trim();
        result = result.replaceAll("\\s+", " ");
        
        return result;
    }
    
    /**
     * 极小化清理：只去除文件扩展名
     */
    protected String cleanVeryMinimal(String filename) {
        if (filename == null || filename.isEmpty()) {
            return filename;
        }
        
        String result = filename;
        
        // 1. 去除方括号中的CD编号（如CD-0221、CD-0176等）
        // 但保留"龙音海文版"等版本信息
        result = result.replaceAll("\\[龙音[海文香港环球]+版[^\\]]*\\d+[^\\]]*\\]", "[龙音海文版]");
        
        // 2. 只去除文件扩展名
        int lastDotIndex = result.lastIndexOf('.');
        if (lastDotIndex > 0) {
            result = result.substring(0, lastDotIndex);
        }
        
        // 3. 去除序号（如之一、之二、之三等）
        result = result.replaceAll("[之][一二三四五六七八九十]+", "");
        
        // 4. 去除多余空格
        result = result.trim();
        result = result.replaceAll("\\s+", " ");
        
        return result;
    }
    
    /**
     * 计算文本去除率
     */
    protected double calculateRemovalRate(String original, String cleaned) {
        if (original == null || original.isEmpty()) {
            return 0.0;
        }
        
        if (cleaned == null || cleaned.isEmpty()) {
            return 1.0;
        }
        
        int originalLength = original.length();
        int cleanedLength = cleaned.length();
        
        return (double)(originalLength - cleanedLength) / originalLength;
    }
    
    @Override
    protected String normalizeForPrefixComparison(String filename) {
        // TEMPLATE策略使用极简的标准化方式
        return cleanMinimal(filename);
    }
}
