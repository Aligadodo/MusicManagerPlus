package com.filemanager.strategy.collection;

import org.junit.Test;

/**
 * 测试被遗漏的合集
 */
public class TestMissedCollections {
    
    @Test
    public void testMissedCollections() {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        // 文艺民歌时代
        String s1 = "群星.2001 - 文艺民歌时代【滚石】【WAV+CUE】";
        String s2 = "群星.2002 - 文艺民歌时代2【滚石】【WAV+CUE】";
        
        System.out.println("=== 文艺民歌时代 ===");
        String normalized1 = normalizer.normalize(s1);
        String normalized2 = normalizer.normalize(s2);
        System.out.println("原始1: " + s1);
        System.out.println("标准化1: " + normalized1);
        System.out.println("原始2: " + s2);
        System.out.println("标准化2: " + normalized2);
        System.out.println("相似度: " + calculator.calculateSimilarity(normalized1, normalized2));
        System.out.println("是否可以聚类: " + (calculator.calculateSimilarity(normalized1, normalized2) >= 0.7 ? "是" : "否"));
        
        // 美丽新世界
        String s3 = "滚石群星.1989 - 美丽新世界【滚石】【WAV+CUE】";
        String s4 = "滚石群星.1991 - 美丽新世界Ⅲ大风吹【滚石】【WAV+CUE】";
        
        System.out.println("\n=== 美丽新世界 ===");
        String normalized3 = normalizer.normalize(s3);
        String normalized4 = normalizer.normalize(s4);
        System.out.println("原始1: " + s3);
        System.out.println("标准化1: " + normalized3);
        System.out.println("原始2: " + s4);
        System.out.println("标准化2: " + normalized4);
        System.out.println("相似度: " + calculator.calculateSimilarity(normalized3, normalized4));
        System.out.println("是否可以聚类: " + (calculator.calculateSimilarity(normalized3, normalized4) >= 0.7 ? "是" : "否"));
        
        // 情感万花筒
        String s5 = "群星.2003 - 情感万花筒 CD1【滚石】【WAV+CUE】";
        String s6 = "群星.2003 - 情感万花筒 CD2【滚石】【WAV+CUE】";
        
        System.out.println("\n=== 情感万花筒 ===");
        String normalized5 = normalizer.normalize(s5);
        String normalized6 = normalizer.normalize(s6);
        System.out.println("原始1: " + s5);
        System.out.println("标准化1: " + normalized5);
        System.out.println("原始2: " + s6);
        System.out.println("标准化2: " + normalized6);
        System.out.println("相似度: " + calculator.calculateSimilarity(normalized5, normalized6));
        System.out.println("是否可以聚类: " + (calculator.calculateSimilarity(normalized5, normalized6) >= 0.7 ? "是" : "否"));
    }
}
