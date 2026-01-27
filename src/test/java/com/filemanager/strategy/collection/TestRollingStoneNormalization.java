package com.filemanager.strategy.collection;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * 测试滚石文件名标准化
 */
public class TestRollingStoneNormalization {
    
    @Test
    public void testRollingStoneNormalization() {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        // 滚石测试用例的文件名
        List<String> rollingStoneFilenames = Arrays.asList(
            "群星.2001 - 文艺民歌时代【滚石】【WAV+CUE】",
            "群星.2002 - 文艺民歌时代2【滚石】【WAV+CUE】",
            "群星.2001 - 我华丽的摇滚梦【滚石】【WAV+CUE】",
            "群星.2002 - 我华丽的摇滚梦2【滚石】【WAV+CUE】",
            "群星.2001 - 欢庆迪斯尼100周年【滚石】【WAV+CUE】",
            "群星.2002 - 欢庆迪斯尼100周年2【滚石】【WAV+CUE】"
        );
        
        System.out.println("=== 滚石文件名标准化测试 ===");
        for (String filename : rollingStoneFilenames) {
            String normalized = normalizer.normalize(filename);
            System.out.println("原始: " + filename);
            System.out.println("标准化: " + normalized);
            System.out.println();
        }
        
        // 计算相似度
        System.out.println("=== 相似度计算 ===");
        String normalized1 = normalizer.normalize(rollingStoneFilenames.get(0));
        String normalized2 = normalizer.normalize(rollingStoneFilenames.get(1));
        double similarity1 = calculator.calculateSimilarity(normalized1, normalized2);
        System.out.println("文艺民歌时代 相似度: " + similarity1);
        
        String normalized3 = normalizer.normalize(rollingStoneFilenames.get(2));
        String normalized4 = normalizer.normalize(rollingStoneFilenames.get(3));
        double similarity2 = calculator.calculateSimilarity(normalized3, normalized4);
        System.out.println("我华丽的摇滚梦 相似度: " + similarity2);
        
        String normalized5 = normalizer.normalize(rollingStoneFilenames.get(4));
        String normalized6 = normalizer.normalize(rollingStoneFilenames.get(5));
        double similarity3 = calculator.calculateSimilarity(normalized5, normalized6);
        System.out.println("欢庆迪斯尼100周年 相似度: " + similarity3);
    }
}
