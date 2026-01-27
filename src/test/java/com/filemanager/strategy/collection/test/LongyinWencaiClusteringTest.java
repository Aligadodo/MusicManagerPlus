package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.FileClusteringAlgorithm;
import com.filemanager.strategy.collection.FilenameNormalizer;
import com.filemanager.strategy.collection.TextSimilarityCalculator;
import com.filemanager.strategy.collection.PreciseNamingStrategy;
import com.filemanager.strategy.collection.StringSimilarityCalculator;
import com.filemanager.strategy.collection.TextSimilarityCalculatorAdapter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 测试龙音文采华音版-轻舟随波系列的文件归类
 */
public class LongyinWencaiClusteringTest {
    
    @Test
    public void testClustering() {
        System.out.println("=== 测试龙音文采华音版-轻舟随波系列的文件归类 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤]钢琴弄潮篇-爱情故事");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥]华夏风情篇-睡莲");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦]异国风情篇-美丽的梭罗河");
        
        System.out.println("文件名列表:");
        for (String filename : filenames) {
            System.out.println("  - " + filename);
        }
        
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
        
        Map<String, List<String>> clusters = clusteringAlgorithm.clusterFilenames(filenames);
        
        System.out.println("\n归类结果:");
        System.out.println("合集数量: " + clusters.size());
        for (Map.Entry<String, List<String>> entry : clusters.entrySet()) {
            System.out.println("\n合集名称: " + entry.getKey());
            System.out.println("文件数量: " + entry.getValue().size());
            for (String filename : entry.getValue()) {
                System.out.println("  - " + filename);
            }
        }
    }
}
