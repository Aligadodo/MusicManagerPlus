package com.filemanager.strategy.collection;

import org.junit.Test;

/**
 * 测试15首精选滚石年度强打金曲文件名标准化
 */
public class Test15SongsNormalization {
    
    @Test
    public void test15SongsNormalization() {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        
        // 15首精选滚石年度强打金曲测试用例的文件名
        String filename1 = "滚石群星200雀巢咖啡时尚精选 15首精选滚石年度强打金曲[滚石][WAV+CUE]";
        String filename2 = "群星2000-雀巢咖啡时尚精选 15首精选滚石年度强打金曲[引进版][WAV+CUE]";
        
        System.out.println("=== 15首精选滚石年度强打金曲文件名标准化测试 ===");
        String normalized1 = normalizer.normalize(filename1);
        String normalized2 = normalizer.normalize(filename2);
        
        System.out.println("原始1: " + filename1);
        System.out.println("标准化1: " + normalized1);
        System.out.println();
        System.out.println("原始2: " + filename2);
        System.out.println("标准化2: " + normalized2);
        System.out.println();
        
        // 计算相似度
        double similarity = calculator.calculateSimilarity(normalized1, normalized2);
        System.out.println("相似度: " + similarity);
        System.out.println("是否可以聚类: " + (similarity >= 0.7 ? "是" : "否"));
    }
}
