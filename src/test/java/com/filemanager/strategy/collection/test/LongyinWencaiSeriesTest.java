package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试龙音文采华音版-轻舟随波系列
 */
public class LongyinWencaiSeriesTest {
    
    @Test
    public void testLongyinWencaiSeries() {
        System.out.println("=== 测试龙音文采华音版-轻舟随波系列 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列④]排箫爱情篇-罗密欧与朱丽叶");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑤]钢琴弄潮篇-爱情故事");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑥]华夏风情篇-睡莲");
        filenames.add("龙音唱片.-.[龙音文采华音版-轻舟随波系列⑦]异国风情篇-美丽的梭罗河");
        
        System.out.println("文件名列表:");
        for (String filename : filenames) {
            System.out.println("  - " + filename);
        }
        
        TextSimilarityCalculator textSimilarityCalculator = new TextSimilarityCalculator(0.7);
        StringSimilarityCalculator stringSimilarityCalculator = 
            new TextSimilarityCalculatorAdapter(textSimilarityCalculator);
        
        ConciseNamingStrategy conciseStrategy = new ConciseNamingStrategy(stringSimilarityCalculator);
        PreciseNamingStrategy preciseStrategy = new PreciseNamingStrategy(stringSimilarityCalculator);
        TemplateNamingStrategy templateStrategy = new TemplateNamingStrategy(stringSimilarityCalculator);
        
        System.out.println("\n--- 简洁风格 ---");
        String conciseName = conciseStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + conciseName);
        
        System.out.println("\n--- 精确风格 ---");
        String preciseName = preciseStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + preciseName);
        
        System.out.println("\n--- TEMPLATE策略 ---");
        String templateName = templateStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + templateName);
    }
}
