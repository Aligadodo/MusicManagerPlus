package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.FileClusteringAlgorithm;
import com.filemanager.strategy.collection.FilenameNormalizer;
import com.filemanager.strategy.collection.TextSimilarityCalculator;
import com.filemanager.strategy.collection.PreciseNamingStrategy;
import com.filemanager.strategy.collection.StringSimilarityCalculator;
import com.filemanager.strategy.collection.TextSimilarityCalculatorAdapter;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试hasSameSeriesKeywords方法
 */
public class HasSameSeriesKeywordsTest {
    
    @Test
    public void testHasSameSeriesKeywords() throws Exception {
        System.out.println("=== 测试hasSameSeriesKeywords方法 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤]钢琴弄潮篇-爱情故事");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥]华夏风情篇-睡莲");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦]异国风情篇-美丽的梭罗河");
        
        FilenameNormalizer normalizer = FilenameNormalizer.builder().build();
        TextSimilarityCalculator similarityCalculator = TextSimilarityCalculator.builder()
                .similarityThreshold(0.7)
                .build();
        StringSimilarityCalculator stringSimilarityCalculator = 
            new TextSimilarityCalculatorAdapter(similarityCalculator);
        PreciseNamingStrategy namingStrategy = new PreciseNamingStrategy(stringSimilarityCalculator);
        
        FileClusteringAlgorithm clusteringAlgorithm = FileClusteringAlgorithm.builder()
                .normalizer(normalizer)
                .similarityCalculator(similarityCalculator)
                .similarityThreshold(0.7)
                .minClusterSize(2)
                .namingStrategy(namingStrategy)
                .build();
        
        Method hasSameSeriesKeywordsMethod = FileClusteringAlgorithm.class
                .getDeclaredMethod("hasSameSeriesKeywords", String.class, String.class);
        hasSameSeriesKeywordsMethod.setAccessible(true);
        
        Method extractSeriesKeywordMethod = FileClusteringAlgorithm.class
                .getDeclaredMethod("extractSeriesKeyword", String.class);
        extractSeriesKeywordMethod.setAccessible(true);
        
        System.out.println("系列关键词提取:");
        for (String filename : filenames) {
            String seriesKeyword = (String) extractSeriesKeywordMethod.invoke(clusteringAlgorithm, filename);
            System.out.println("  " + filename);
            System.out.println("  系列关键词: " + seriesKeyword);
        }
        
        System.out.println("\n系列关键词匹配:");
        for (int i = 0; i < filenames.size(); i++) {
            for (int j = i + 1; j < filenames.size(); j++) {
                boolean hasSameSeries = (Boolean) hasSameSeriesKeywordsMethod.invoke(
                        clusteringAlgorithm, filenames.get(i), filenames.get(j));
                System.out.println("  文件" + (i + 1) + " vs 文件" + (j + 1) + ": " + hasSameSeries);
            }
        }
    }
}
