package com.filemanager.strategy.collection.test;

import com.filemanager.strategy.collection.FileClusteringAlgorithm;
import com.filemanager.strategy.collection.FilenameNormalizer;
import com.filemanager.strategy.collection.TextSimilarityCalculator;

import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * 测试框架主类，整合所有测试组件
 */
public class TestFramework {
    
    private FileClusteringAlgorithm algorithm;
    private TestCasePersister persister;
    private TextSimilarityCalculator similarityCalculator;
    
    public TestFramework() {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        this.algorithm = new FileClusteringAlgorithm(normalizer, calculator, 0.7, 2);
        this.similarityCalculator = new TextSimilarityCalculator(0.7);
        this.persister = new TestCasePersister();
    }
    
    public TestFramework(double threshold) {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(threshold);
        this.algorithm = new FileClusteringAlgorithm(normalizer, calculator, threshold, 2);
        this.similarityCalculator = new TextSimilarityCalculator(threshold);
        this.persister = new TestCasePersister();
    }
    
    public TestFramework(String storageDir) {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(0.7);
        this.algorithm = new FileClusteringAlgorithm(normalizer, calculator, 0.7, 2);
        this.similarityCalculator = new TextSimilarityCalculator(0.7);
        this.persister = new TestCasePersister(storageDir);
    }
    
    /**
     * 验证测试用例
     * @param testCase 测试用例
     * @return 验证结果
     */
    public TestValidationResult validateTestCase(TestCase testCase) {
        TestValidationResult result = new TestValidationResult(testCase.getTestName());
        
        if (testCase.getAllFolders() == null || testCase.getAllFolders().isEmpty()) {
            System.err.println("测试用例中没有文件夹数据");
            return result;
        }
        
        System.out.println("\n=== 开始验证测试用例: " + testCase.getTestName() + " ===");
        System.out.println("文件夹数量: " + testCase.getAllFolders().size());
        System.out.println("预期合集数量: " + (testCase.getExpectedCollections() != null ? testCase.getExpectedCollections().size() : 0));
        
        // 运行算法生成合集
        Map<String, List<String>> actualCollections = algorithm.clusterFilenames(testCase.getAllFolders());
        System.out.println("算法生成的合集数量: " + actualCollections.size());
        
        // 验证每个预期合集
        if (testCase.getExpectedCollections() != null) {
            for (ExpectedCollection expected : testCase.getExpectedCollections()) {
                validateExpectedCollection(expected, actualCollections, result);
            }
        }
        
        // 找出多余的合集
        Set<String> matchedCollectionNames = new HashSet<>();
        for (TestValidationResult.CollectionMatch match : result.getCollectionMatches()) {
            matchedCollectionNames.add(match.getActualName());
        }
        
        for (String actualName : actualCollections.keySet()) {
            if (!matchedCollectionNames.contains(actualName)) {
                result.addExtraCollection(actualName);
            }
        }
        
        result.setExtraCollections(result.getExtraCollectionNames().size());
        result.calculateMetrics();
        
        // 输出验证结果
        System.out.println(result);
        
        return result;
    }
    
    /**
     * 验证单个预期合集
     */
    private void validateExpectedCollection(ExpectedCollection expected, 
                                         Map<String, List<String>> actualCollections,
                                         TestValidationResult result) {
        String expectedName = expected.getCollectionName();
        List<String> expectedFolders = expected.getFolderNames();
        
        // 寻找最匹配的实际合集
        String bestMatchName = null;
        double bestMatchScore = 0.0;
        double bestNameSimilarity = 0.0;
        double bestFolderOverlap = 0.0;
        
        for (Map.Entry<String, List<String>> entry : actualCollections.entrySet()) {
            String actualName = entry.getKey();
            List<String> actualFolders = entry.getValue();
            
            // 计算名称相似度
            double nameSim = similarityCalculator.calculateSimilarity(expectedName, actualName);
            
            // 计算文件夹重叠率
            double folderOverlap = calculateFolderOverlap(expectedFolders, actualFolders);
            
            // 综合评分（名称相似度占30%，文件夹重叠率占70%）
            double score = nameSim * 0.3 + folderOverlap * 0.7;
            
            if (score > bestMatchScore) {
                bestMatchScore = score;
                bestMatchName = actualName;
                bestNameSimilarity = nameSim;
                bestFolderOverlap = folderOverlap;
            }
        }
        
        // 判断是否匹配（综合评分 >= 0.6）
        if (bestMatchScore >= 0.6) {
            TestValidationResult.CollectionMatch match = new TestValidationResult.CollectionMatch(
                expectedName, bestMatchName,
                expectedFolders.size(), actualCollections.get(bestMatchName).size(),
                bestNameSimilarity, bestFolderOverlap
            );
            result.addCollectionMatch(match);
            result.setMatchedCollections(result.getMatchedCollections() + 1);
        } else {
            result.addMissedCollection(expectedName);
            result.setMissedCollections(result.getMissedCollections() + 1);
        }
    }
    
    /**
     * 计算文件夹重叠率
     */
    private double calculateFolderOverlap(List<String> expectedFolders, List<String> actualFolders) {
        if (expectedFolders.isEmpty() || actualFolders.isEmpty()) {
            return 0.0;
        }
        
        Set<String> expectedSet = new HashSet<>(expectedFolders);
        Set<String> actualSet = new HashSet<>(actualFolders);
        
        int intersection = 0;
        for (String folder : expectedSet) {
            if (actualSet.contains(folder)) {
                intersection++;
            }
        }
        
        // 使用Jaccard相似度：交集 / 并集
        Set<String> union = new HashSet<>(expectedSet);
        union.addAll(actualSet);
        
        return (double) intersection / union.size();
    }
    
    /**
     * 批量验证所有测试用例
     * @return 所有验证结果
     */
    public List<TestValidationResult> validateAllTestCases() {
        List<TestCase> testCases = persister.loadAllTestCases();
        List<TestValidationResult> results = new ArrayList<>();
        
        System.out.println("\n=== 批量验证测试用例 ===");
        System.out.println("找到 " + testCases.size() + " 个测试用例");
        
        for (TestCase testCase : testCases) {
            TestValidationResult result = validateTestCase(testCase);
            results.add(result);
            
            // 保存验证结果
            String resultFilename = testCase.getTestName() + "_" + testCase.getTimestamp();
            persister.saveValidationResult(result, resultFilename);
        }
        
        // 输出汇总统计
        printSummaryStatistics(results);
        
        return results;
    }
    
    /**
     * 从test-data目录加载并验证所有测试用例
     * @return 所有验证结果
     */
    public List<TestValidationResult> validateFromTestDataDir() {
        String testDataDir = persister.getStorageDir() + File.separator + "test-data";
        File dir = new File(testDataDir);
        
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("测试数据目录不存在: " + testDataDir);
            return new ArrayList<>();
        }
        
        File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.err.println("测试数据目录中没有JSON文件: " + testDataDir);
            return new ArrayList<>();
        }
        
        List<TestValidationResult> results = new ArrayList<>();
        
        System.out.println("\n=== 从test-data目录批量验证测试用例 ===");
        System.out.println("找到 " + jsonFiles.length + " 个测试用例文件");
        
        for (File jsonFile : jsonFiles) {
            System.out.println("\n--- 处理文件: " + jsonFile.getName() + " ---");
            
            try {
                // 读取JSON文件
                StringBuilder sb = new StringBuilder();
                try (FileReader reader = new FileReader(jsonFile)) {
                    int ch;
                    while ((ch = reader.read()) != -1) {
                        sb.append((char) ch);
                    }
                }
                
                // 解析JSON为TestCase对象
                TestCase testCase = com.alibaba.fastjson.JSON.parseObject(sb.toString(), TestCase.class);
                
                if (testCase != null) {
                    // 验证测试用例
                    TestValidationResult result = validateTestCase(testCase);
                    results.add(result);
                    
                    // 保存验证结果
                    String resultFilename = testCase.getTestName() + "_" + testCase.getTimestamp();
                    persister.saveValidationResult(result, resultFilename);
                }
            } catch (Exception e) {
                System.err.println("处理测试用例文件失败: " + jsonFile.getName());
                e.printStackTrace();
            }
        }
        
        // 输出汇总统计
        printSummaryStatistics(results);
        
        return results;
    }
    
    /**
     * 输出汇总统计
     */
    private void printSummaryStatistics(List<TestValidationResult> results) {
        System.out.println("\n=== 汇总统计 ===");
        System.out.println("总测试用例数: " + results.size());
        
        if (results.isEmpty()) {
            return;
        }
        
        double totalMatchRate = 0.0;
        int excellentCount = 0;
        int goodCount = 0;
        int averageCount = 0;
        int passCount = 0;
        int failCount = 0;
        
        for (TestValidationResult result : results) {
            totalMatchRate += result.getMatchRate();
            
            String level = result.getScoreLevel();
            switch (level) {
                case "优秀":
                    excellentCount++;
                    break;
                case "良好":
                    goodCount++;
                    break;
                case "一般":
                    averageCount++;
                    break;
                case "及格":
                    passCount++;
                    break;
                case "不及格":
                    failCount++;
                    break;
            }
        }
        
        double avgMatchRate = totalMatchRate / results.size();
        
        System.out.println("平均匹配率: " + String.format("%.2f%%", avgMatchRate * 100));
        System.out.println("优秀: " + excellentCount);
        System.out.println("良好: " + goodCount);
        System.out.println("一般: " + averageCount);
        System.out.println("及格: " + passCount);
        System.out.println("不及格: " + failCount);
    }
    
    /**
     * 保存测试用例
     * @param testCase 测试用例
     * @param filename 文件名
     * @return 是否保存成功
     */
    public boolean saveTestCase(TestCase testCase, String filename) {
        return persister.saveTestCase(testCase, filename);
    }
    
    /**
     * 加载测试用例
     * @param filename 文件名
     * @return 测试用例
     */
    public TestCase loadTestCase(String filename) {
        return persister.loadTestCase(filename);
    }
    
    /**
     * 设置相似度阈值
     * @param threshold 相似度阈值
     */
    public void setThreshold(double threshold) {
        FilenameNormalizer normalizer = new FilenameNormalizer(false, false);
        TextSimilarityCalculator calculator = new TextSimilarityCalculator(threshold);
        this.algorithm = new FileClusteringAlgorithm(normalizer, calculator, threshold, 2);
        this.similarityCalculator = new TextSimilarityCalculator(threshold);
    }
    
    /**
     * 设置测试用例存储目录
     * @param storageDir 存储目录
     */
    public void setStorageDir(String storageDir) {
        this.persister = new TestCasePersister(storageDir);
    }
    
    // Getters
    public TestCasePersister getPersister() {
        return persister;
    }
}
