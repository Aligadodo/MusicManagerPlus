package com.filemanager.strategy.collection;

import org.junit.Test;

/**
 * 测试改进后的相似度计算
 */
public class TestImprovedSimilarity {
    
    @Test
    public void testImprovedSimilarity() {
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        // 测试15首精选滚石年度强打金曲
        String s1 = "滚石群星200雀巢咖啡时尚精选 15首精选滚石年度强打金曲 滚石";
        String s2 = "雀巢咖啡时尚精选 15首精选滚石年度强打金曲";
        
        System.out.println("=== 改进后的相似度计算测试 ===");
        System.out.println("字符串1: " + s1);
        System.out.println("字符串2: " + s2);
        System.out.println("相似度: " + calculator.calculateSimilarity(s1, s2));
        System.out.println("是否可以聚类: " + (calculator.calculateSimilarity(s1, s2) >= 0.7 ? "是" : "否"));
        
        // 测试美丽新世界
        String s3 = "美丽新世界 滚石 WAV+CUE";
        String s4 = "美丽新世界Ⅲ大风吹 滚石 WAV+CUE";
        
        System.out.println("\n=== 美丽新世界相似度计算测试 ===");
        System.out.println("字符串1: " + s3);
        System.out.println("字符串2: " + s4);
        System.out.println("相似度: " + calculator.calculateSimilarity(s3, s4));
        System.out.println("是否可以聚类: " + (calculator.calculateSimilarity(s3, s4) >= 0.7 ? "是" : "否"));
    }
}
