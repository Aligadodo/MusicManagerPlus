package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试精确风格的详细执行过程
 */
public class PreciseNamingStrategyDebugTest {
    
    @Test
    public void testPreciseNamingStrategyDebug() {
        System.out.println("=== 测试精确风格的详细执行过程 ===");
        
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
        
        System.out.println("\n--- 精确风格 ---");
        PreciseNamingStrategy preciseStrategy = new PreciseNamingStrategy(stringSimilarityCalculator);
        String preciseName = preciseStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + preciseName);
    }
}
