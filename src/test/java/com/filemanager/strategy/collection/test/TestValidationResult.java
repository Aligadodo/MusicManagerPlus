package com.filemanager.strategy.collection.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试验证结果，用于验证算法生成的合集与预期合集的匹配情况
 */
public class TestValidationResult {
    
    private String testName;
    private int totalExpectedCollections;
    private int totalActualCollections;
    private int matchedCollections;
    private int missedCollections;
    private int extraCollections;
    private double matchRate;
    private List<CollectionMatch> collectionMatches;
    private List<String> missedCollectionNames;
    private List<String> extraCollectionNames;
    private long timestamp;
    
    public TestValidationResult() {
        this.collectionMatches = new ArrayList<>();
        this.missedCollectionNames = new ArrayList<>();
        this.extraCollectionNames = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public TestValidationResult(String testName) {
        this();
        this.testName = testName;
    }
    
    public void addCollectionMatch(CollectionMatch match) {
        if (this.collectionMatches == null) {
            this.collectionMatches = new ArrayList<>();
        }
        this.collectionMatches.add(match);
    }
    
    public void addMissedCollection(String collectionName) {
        if (this.missedCollectionNames == null) {
            this.missedCollectionNames = new ArrayList<>();
        }
        this.missedCollectionNames.add(collectionName);
    }
    
    public void addExtraCollection(String collectionName) {
        if (this.extraCollectionNames == null) {
            this.extraCollectionNames = new ArrayList<>();
        }
        this.extraCollectionNames.add(collectionName);
    }
    
    public void calculateMetrics() {
        this.totalExpectedCollections = missedCollectionNames.size() + matchedCollections;
        this.totalActualCollections = extraCollectionNames.size() + matchedCollections;
        
        if (totalExpectedCollections > 0) {
            this.matchRate = (double) matchedCollections / totalExpectedCollections;
        } else {
            this.matchRate = 0.0;
        }
    }
    
    public String getScoreLevel() {
        if (matchRate >= 0.9) {
            return "优秀";
        } else if (matchRate >= 0.8) {
            return "良好";
        } else if (matchRate >= 0.7) {
            return "一般";
        } else if (matchRate >= 0.6) {
            return "及格";
        } else {
            return "不及格";
        }
    }
    
    public String getTestName() {
        return testName;
    }
    
    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    public int getTotalExpectedCollections() {
        return totalExpectedCollections;
    }
    
    public void setTotalExpectedCollections(int totalExpectedCollections) {
        this.totalExpectedCollections = totalExpectedCollections;
    }
    
    public int getTotalActualCollections() {
        return totalActualCollections;
    }
    
    public void setTotalActualCollections(int totalActualCollections) {
        this.totalActualCollections = totalActualCollections;
    }
    
    public int getMatchedCollections() {
        return matchedCollections;
    }
    
    public void setMatchedCollections(int matchedCollections) {
        this.matchedCollections = matchedCollections;
    }
    
    public int getMissedCollections() {
        return missedCollections;
    }
    
    public void setMissedCollections(int missedCollections) {
        this.missedCollections = missedCollections;
    }
    
    public int getExtraCollections() {
        return extraCollections;
    }
    
    public void setExtraCollections(int extraCollections) {
        this.extraCollections = extraCollections;
    }
    
    public double getMatchRate() {
        return matchRate;
    }
    
    public void setMatchRate(double matchRate) {
        this.matchRate = matchRate;
    }
    
    public List<CollectionMatch> getCollectionMatches() {
        return collectionMatches;
    }
    
    public void setCollectionMatches(List<CollectionMatch> collectionMatches) {
        this.collectionMatches = collectionMatches;
    }
    
    public List<String> getMissedCollectionNames() {
        return missedCollectionNames;
    }
    
    public void setMissedCollectionNames(List<String> missedCollectionNames) {
        this.missedCollectionNames = missedCollectionNames;
    }
    
    public List<String> getExtraCollectionNames() {
        return extraCollectionNames;
    }
    
    public void setExtraCollectionNames(List<String> extraCollectionNames) {
        this.extraCollectionNames = extraCollectionNames;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 测试验证结果 ===\n");
        sb.append("测试名称: ").append(testName).append("\n");
        sb.append("时间: ").append(new java.util.Date(timestamp)).append("\n");
        sb.append("\n--- 汇总统计 ---\n");
        sb.append("预期合集数量: ").append(totalExpectedCollections).append("\n");
        sb.append("实际合集数量: ").append(totalActualCollections).append("\n");
        sb.append("匹配合集数量: ").append(matchedCollections).append("\n");
        sb.append("遗漏合集数量: ").append(missedCollections).append("\n");
        sb.append("多余合集数量: ").append(extraCollections).append("\n");
        sb.append("匹配率: ").append(String.format("%.2f%%", matchRate * 100)).append("\n");
        sb.append("评分等级: ").append(getScoreLevel()).append("\n");
        
        if (!collectionMatches.isEmpty()) {
            sb.append("\n--- 匹配详情 ---\n");
            for (CollectionMatch match : collectionMatches) {
                sb.append(match).append("\n");
            }
        }
        
        if (!missedCollectionNames.isEmpty()) {
            sb.append("\n--- 遗漏的合集 ---\n");
            for (String name : missedCollectionNames) {
                sb.append("  - ").append(name).append("\n");
            }
        }
        
        if (!extraCollectionNames.isEmpty()) {
            sb.append("\n--- 多余的合集 ---\n");
            for (String name : extraCollectionNames) {
                sb.append("  - ").append(name).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 合集匹配详情
     */
    public static class CollectionMatch {
        private String expectedName;
        private String actualName;
        private int expectedFolderCount;
        private int actualFolderCount;
        private double nameSimilarity;
        private double folderOverlapRate;
        
        public CollectionMatch(String expectedName, String actualName, 
                             int expectedFolderCount, int actualFolderCount,
                             double nameSimilarity, double folderOverlapRate) {
            this.expectedName = expectedName;
            this.actualName = actualName;
            this.expectedFolderCount = expectedFolderCount;
            this.actualFolderCount = actualFolderCount;
            this.nameSimilarity = nameSimilarity;
            this.folderOverlapRate = folderOverlapRate;
        }
        
        public String getExpectedName() {
            return expectedName;
        }
        
        public String getActualName() {
            return actualName;
        }
        
        public int getExpectedFolderCount() {
            return expectedFolderCount;
        }
        
        public int getActualFolderCount() {
            return actualFolderCount;
        }
        
        public double getNameSimilarity() {
            return nameSimilarity;
        }
        
        public double getFolderOverlapRate() {
            return folderOverlapRate;
        }
        
        @Override
        public String toString() {
            return String.format("预期: %s (%d个文件夹) -> 实际: %s (%d个文件夹) | 名称相似度: %.2f%%, 文件夹重叠率: %.2f%%",
                    expectedName, expectedFolderCount, actualName, actualFolderCount,
                    nameSimilarity * 100, folderOverlapRate * 100);
        }
    }
}
