package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试TEMPLATE命名策略
 */
public class TemplateNamingStrategyTest {
    
    @Test
    public void testTemplateNamingStrategy() {
        System.out.println("=== 测试TEMPLATE命名策略 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一");
        filenames.add("[龙音海文版 CD-0176]小河淌水-陈爱娟古筝独奏之二");
        
        System.out.println("文件名列表:");
        for (String filename : filenames) {
            System.out.println("  - " + filename);
        }
        
        TextSimilarityCalculator textSimilarityCalculator = new TextSimilarityCalculator(0.7);
        StringSimilarityCalculator stringSimilarityCalculator = 
            new TextSimilarityCalculatorAdapter(textSimilarityCalculator);
        
        System.out.println("\n--- TEMPLATE策略 ---");
        TemplateNamingStrategy templateStrategy = new TemplateNamingStrategy(stringSimilarityCalculator);
        String templateName = templateStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + templateName);
        
        System.out.println("\n--- 对比其他策略 ---");
        
        System.out.println("\n简洁风格:");
        ConciseNamingStrategy conciseStrategy = new ConciseNamingStrategy(stringSimilarityCalculator);
        String conciseName = conciseStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + conciseName);
        
        System.out.println("\n精确风格:");
        PreciseNamingStrategy preciseStrategy = new PreciseNamingStrategy(stringSimilarityCalculator);
        String preciseName = preciseStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + preciseName);
    }
    
    @Test
    public void testTemplateNamingStrategyMultiple() {
        System.out.println("=== 测试TEMPLATE命名策略（多文件） ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一");
        filenames.add("[龙音海文版 CD-0176]小河淌水-陈爱娟古筝独奏之二");
        filenames.add("[龙音海文版 CD-0222]高山流水-陈爱娟古筝独奏之三");
        
        System.out.println("文件名列表:");
        for (String filename : filenames) {
            System.out.println("  - " + filename);
        }
        
        TextSimilarityCalculator textSimilarityCalculator = new TextSimilarityCalculator(0.7);
        StringSimilarityCalculator stringSimilarityCalculator = 
            new TextSimilarityCalculatorAdapter(textSimilarityCalculator);
        
        System.out.println("\n--- TEMPLATE策略 ---");
        TemplateNamingStrategy templateStrategy = new TemplateNamingStrategy(stringSimilarityCalculator);
        String templateName = templateStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + templateName);
    }
}
