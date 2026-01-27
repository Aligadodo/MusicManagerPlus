package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.TemplateNamingStrategy;
import com.filemanager.strategy.collection.TextSimilarityCalculator;
import com.filemanager.strategy.collection.TextSimilarityCalculatorAdapter;
import com.filemanager.strategy.collection.StringSimilarityCalculator;
import org.junit.Test;

/**
 * 测试calculateRemovalRate方法
 */
public class CalculateRemovalRateTest {
    
    @Test
    public void testCalculateRemovalRate() {
        System.out.println("=== 测试calculateRemovalRate方法 ===");
        
        String original = "[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一";
        String cleaned = "[龙音海文版]茉莉芬芳-陈爱娟古筝独奏";
        
        System.out.println("原始字符串: " + original);
        System.out.println("清理后字符串: " + cleaned);
        System.out.println("原始长度: " + original.length());
        System.out.println("清理后长度: " + cleaned.length());
        
        double removalRate = (double)(original.length() - cleaned.length()) / original.length();
        System.out.println("去除率: " + (removalRate * 100) + "%");
        
        System.out.println("\n最大允许去除率: 20%");
        System.out.println("是否超过最大去除率: " + (removalRate > 0.20));
    }
}
