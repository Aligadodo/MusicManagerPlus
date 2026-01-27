package com.filemanager.strategy.collection;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 测试文件名标准化
 */
public class TestFilenameNormalization {
    
    @Test
    public void testNormalization() {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        // 龙音测试用例的文件名
        List<String> longyinFilenames = Arrays.asList(
            "[海文版 CD-0174]望秦川-王中山古筝专辑之四",
            "[龙音海文版 CD-0073]溟山-王中山古筝专辑(一)",
            "[龙音海文版 CD-0074]黄河魂-王中山古筝专辑(二)",
            "[龙音海文版 CD-0173]夜深沉-王中山古筝专辑之三"
        );
        
        System.out.println("=== 龙音文件名标准化测试 ===");
        for (String filename : longyinFilenames) {
            String normalized = normalizer.normalize(filename);
            System.out.println("原始: " + filename);
            System.out.println("标准化: " + normalized);
            System.out.println();
        }
        
        // 计算相似度
        System.out.println("=== 相似度计算 ===");
        String normalized1 = normalizer.normalize(longyinFilenames.get(0));
        String normalized2 = normalizer.normalize(longyinFilenames.get(1));
        double similarity = calculator.calculateSimilarity(normalized1, normalized2);
        System.out.println("相似度: " + similarity);
        
        // 滚石测试用例的文件名
        List<String> rollingStoneFilenames = Arrays.asList(
            "群星.2001 - 文艺民歌时代【滚石】【WAV+CUE】",
            "群星.2002 - 文艺民歌时代2【滚石】【WAV+CUE】"
        );
        
        System.out.println("\n=== 滚石文件名标准化测试 ===");
        for (String filename : rollingStoneFilenames) {
            String normalized = normalizer.normalize(filename);
            System.out.println("原始: " + filename);
            System.out.println("标准化: " + normalized);
            System.out.println();
        }
        
        // 计算相似度
        String rsNormalized1 = normalizer.normalize(rollingStoneFilenames.get(0));
        String rsNormalized2 = normalizer.normalize(rollingStoneFilenames.get(1));
        double rsSimilarity = calculator.calculateSimilarity(rsNormalized1, rsNormalized2);
        System.out.println("相似度: " + rsSimilarity);
    }
}
