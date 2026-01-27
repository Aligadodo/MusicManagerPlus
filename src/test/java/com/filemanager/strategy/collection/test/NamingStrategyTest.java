package com.filemanager.strategy.collection.test;

import org.junit.Test;

/**
 * 命名策略测试类，用于测试简洁风格和精确风格的命名策略
 */
public class NamingStrategyTest {
    
    @Test
    public void testConciseNamingStrategy() {
        System.out.println("=== 测试简洁风格命名策略 ===\n");
        
        TestFramework framework = new TestFramework();
        
        // 测试古琴音乐 - 简洁风格
        TestCase guqinConcise = TestCaseGenerator.generateGuqinMusicConciseTestCase();
        TestValidationResult guqinResult = framework.validateTestCase(guqinConcise);
        System.out.println("古琴音乐 - 简洁风格测试结果: " + 
            (guqinResult.isAllPassed() ? "通过" : "失败"));
        
        // 测试龙音系列 - 简洁风格
        TestCase longyinConcise = TestCaseGenerator.generateLongyinSeriesConciseTestCase();
        TestValidationResult longyinResult = framework.validateTestCase(longyinConcise);
        System.out.println("龙音系列 - 简洁风格测试结果: " + 
            (longyinResult.isAllPassed() ? "通过" : "失败"));
        
        // 测试滚石系列 - 简洁风格
        TestCase rockRecordsConcise = TestCaseGenerator.generateRockRecordsConciseTestCase();
        TestValidationResult rockRecordsResult = framework.validateTestCase(rockRecordsConcise);
        System.out.println("滚石系列 - 简洁风格测试结果: " + 
            (rockRecordsResult.isAllPassed() ? "通过" : "失败"));
        
        System.out.println("\n=== 简洁风格命名策略测试完成 ===");
    }
    
    @Test
    public void testPreciseNamingStrategy() {
        System.out.println("=== 测试精确风格命名策略 ===\n");
        
        TestFramework framework = new TestFramework();
        
        // 测试古琴音乐 - 精确风格
        TestCase guqinPrecise = TestCaseGenerator.generateGuqinMusicPreciseTestCase();
        TestValidationResult guqinResult = framework.validateTestCase(guqinPrecise);
        System.out.println("古琴音乐 - 精确风格测试结果: " + 
            (guqinResult.isAllPassed() ? "通过" : "失败"));
        
        // 测试龙音系列 - 精确风格
        TestCase longyinPrecise = TestCaseGenerator.generateLongyinSeriesPreciseTestCase();
        TestValidationResult longyinResult = framework.validateTestCase(longyinPrecise);
        System.out.println("龙音系列 - 精确风格测试结果: " + 
            (longyinResult.isAllPassed() ? "通过" : "失败"));
        
        // 测试滚石系列 - 精确风格
        TestCase rockRecordsPrecise = TestCaseGenerator.generateRockRecordsPreciseTestCase();
        TestValidationResult rockRecordsResult = framework.validateTestCase(rockRecordsPrecise);
        System.out.println("滚石系列 - 精确风格测试结果: " + 
            (rockRecordsResult.isAllPassed() ? "通过" : "失败"));
        
        System.out.println("\n=== 精确风格命名策略测试完成 ===");
    }
    
    @Test
    public void testBothStrategiesComparison() {
        System.out.println("=== 对比测试简洁风格和精确风格 ===\n");
        
        TestFramework framework = new TestFramework();
        
        // 对比古琴音乐
        System.out.println("--- 古琴音乐对比 ---");
        TestCase guqinConcise = TestCaseGenerator.generateGuqinMusicConciseTestCase();
        TestCase guqinPrecise = TestCaseGenerator.generateGuqinMusicPreciseTestCase();
        
        TestValidationResult guqinConciseResult = framework.validateTestCase(guqinConcise);
        TestValidationResult guqinPreciseResult = framework.validateTestCase(guqinPrecise);
        
        System.out.println("简洁风格: " + (guqinConciseResult.isAllPassed() ? "通过" : "失败"));
        System.out.println("精确风格: " + (guqinPreciseResult.isAllPassed() ? "通过" : "失败"));
        
        // 对比龙音系列
        System.out.println("\n--- 龙音系列对比 ---");
        TestCase longyinConcise = TestCaseGenerator.generateLongyinSeriesConciseTestCase();
        TestCase longyinPrecise = TestCaseGenerator.generateLongyinSeriesPreciseTestCase();
        
        TestValidationResult longyinConciseResult = framework.validateTestCase(longyinConcise);
        TestValidationResult longyinPreciseResult = framework.validateTestCase(longyinPrecise);
        
        System.out.println("简洁风格: " + (longyinConciseResult.isAllPassed() ? "通过" : "失败"));
        System.out.println("精确风格: " + (longyinPreciseResult.isAllPassed() ? "通过" : "失败"));
        
        // 对比滚石系列
        System.out.println("\n--- 滚石系列对比 ---");
        TestCase rockRecordsConcise = TestCaseGenerator.generateRockRecordsConciseTestCase();
        TestCase rockRecordsPrecise = TestCaseGenerator.generateRockRecordsPreciseTestCase();
        
        TestValidationResult rockRecordsConciseResult = framework.validateTestCase(rockRecordsConcise);
        TestValidationResult rockRecordsPreciseResult = framework.validateTestCase(rockRecordsPrecise);
        
        System.out.println("简洁风格: " + (rockRecordsConciseResult.isAllPassed() ? "通过" : "失败"));
        System.out.println("精确风格: " + (rockRecordsPreciseResult.isAllPassed() ? "通过" : "失败"));
        
        System.out.println("\n=== 对比测试完成 ===");
    }
}