package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 合集名称生成单测，验证各个场景下的效果
 */
public class CollectionNameGenerationTest {
    
    @Test
    public void testLongyinSeriesGuZheng() {
        System.out.println("=== 测试龙音系列古筝独奏 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一");
        filenames.add("[龙音海文版 CD-0176]小河淌水-陈爱娟古筝独奏之二");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testLongyinSeriesGuzheng() {
        System.out.println("=== 测试龙音系列古琴 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd1");
        filenames.add("龙音系列-梅庵琴谱-龚一[龙音香港版 RC-011007-3C].cd2");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testGuqinMusic() {
        System.out.println("=== 测试古琴音乐 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨].cd1");
        filenames.add("【古琴音乐】吴景略《古琴艺术》2CD 1998[FLAC+CUE整轨]cd2");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testRockRecords() {
        System.out.println("=== 测试滚石系列 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD1");
        filenames.add("滚石系列-张国荣《风继续吹》1983[WAV+CUE分轨].CD2");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testYearPrefix() {
        System.out.println("=== 测试年份前缀 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("1998-王菲《唱游》[FLAC+CUE整轨].CD1");
        filenames.add("1998-王菲《唱游》[FLAC+CUE整轨].CD2");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testBookTitle() {
        System.out.println("=== 测试书名号 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("《红楼梦》主题曲合集[FLAC].CD1");
        filenames.add("《红楼梦》主题曲合集[FLAC].CD2");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testDashSeparator() {
        System.out.println("=== 测试横线分隔 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("周杰伦-七里香[FLAC].CD1");
        filenames.add("周杰伦-七里香[FLAC].CD2");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testGeneric() {
        System.out.println("=== 测试通用模式 ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("周杰伦-七里香-CD1");
        filenames.add("周杰伦-七里香-CD2");
        
        testNamingStrategies(filenames);
    }
    
    @Test
    public void testLongyinSeriesMultiple() {
        System.out.println("=== 测试龙音系列多CD ===");
        
        List<String> filenames = new ArrayList<>();
        filenames.add("[龙音海文版 CD-0221]茉莉芬芳-陈爱娟古筝独奏之一");
        filenames.add("[龙音海文版 CD-0176]小河淌水-陈爱娟古筝独奏之二");
        filenames.add("[龙音海文版 CD-0222]高山流水-陈爱娟古筝独奏之三");
        
        testNamingStrategies(filenames);
    }
    
    private void testNamingStrategies(List<String> filenames) {
        TextSimilarityCalculator textSimilarityCalculator = new TextSimilarityCalculator(0.7);
        StringSimilarityCalculator stringSimilarityCalculator = 
            new TextSimilarityCalculatorAdapter(textSimilarityCalculator);
        
        System.out.println("文件名列表:");
        for (String filename : filenames) {
            System.out.println("  - " + filename);
        }
        
        System.out.println("\n--- 简洁风格 ---");
        ConciseNamingStrategy conciseStrategy = new ConciseNamingStrategy(stringSimilarityCalculator);
        String conciseName = conciseStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + conciseName);
        
        System.out.println("\n--- 精确风格 ---");
        PreciseNamingStrategy preciseStrategy = new PreciseNamingStrategy(stringSimilarityCalculator);
        String preciseName = preciseStrategy.generateCollectionName(filenames);
        System.out.println("生成的合集名称: " + preciseName);
        
        System.out.println("\n" + "==================================================\n");
    }
}