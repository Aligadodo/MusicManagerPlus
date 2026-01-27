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
 * 测试文件归类算法的详细过程
 */
public class ClusteringProcessTest {
    
    @Test
    public void testClusteringProcess() throws Exception {
        System.out.println("=== 测试文件归类算法的详细过程 ===");
        
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
        
        Method isSamePolygramSeriesMethod = FileClusteringAlgorithm.class
                .getDeclaredMethod("isSamePolygramSeries", String.class, String.class);
        isSamePolygramSeriesMethod.setAccessible(true);
        
        Method extractCoreAlbumNameMethod = FileClusteringAlgorithm.class
                .getDeclaredMethod("extractCoreAlbumName", String.class);
        extractCoreAlbumNameMethod.setAccessible(true);
        
        System.out.println("文件名列表:");
        for (int i = 0; i < filenames.size(); i++) {
            String filename = filenames.get(i);
            String normalized = normalizer.normalize(filename);
            System.out.println("  " + (i + 1) + ": " + filename);
            System.out.println("     标准化后: " + normalized);
        }
        
        System.out.println("\n文件1与其他文件的相似度:");
        String file1 = filenames.get(0);
        String normalizedFile1 = normalizer.normalize(file1);
        for (int i = 1; i < filenames.size(); i++) {
            String fileI = filenames.get(i);
            String normalizedFileI = normalizer.normalize(fileI);
            
            double similarity = similarityCalculator.calculateSimilarity(normalizedFile1, normalizedFileI);
            boolean hasSameSeries = (Boolean) hasSameSeriesKeywordsMethod.invoke(
                    clusteringAlgorithm, file1, fileI);
            boolean isSamePolygram = (Boolean) isSamePolygramSeriesMethod.invoke(
                    clusteringAlgorithm, file1, fileI);
            String coreAlbum1 = (String) extractCoreAlbumNameMethod.invoke(clusteringAlgorithm, file1);
            String coreAlbumI = (String) extractCoreAlbumNameMethod.invoke(clusteringAlgorithm, fileI);
            
            System.out.println("  文件1 vs 文件" + (i + 1) + ":");
            System.out.println("    相似度: " + similarity);
            System.out.println("    相同系列: " + hasSameSeries);
            System.out.println("    相同宝丽金系列: " + isSamePolygram);
            System.out.println("    核心专辑1: " + coreAlbum1);
            System.out.println("    核心专辑" + (i + 1) + ": " + coreAlbumI);
        }
    }
}
