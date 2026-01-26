package com.filemanager.strategy.collection.test;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试用例，包含文件夹列表和预期的合集组合
 */
public class TestCase {
    
    private String testName;
    private String description;
    private List<String> allFolders;
    private List<ExpectedCollection> expectedCollections;
    private long timestamp;
    
    public TestCase() {
        this.allFolders = new ArrayList<>();
        this.expectedCollections = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public TestCase(String testName) {
        this();
        this.testName = testName;
    }
    
    public void addFolder(String folderName) {
        if (this.allFolders == null) {
            this.allFolders = new ArrayList<>();
        }
        this.allFolders.add(folderName);
    }
    
    public void addExpectedCollection(ExpectedCollection collection) {
        if (this.expectedCollections == null) {
            this.expectedCollections = new ArrayList<>();
        }
        this.expectedCollections.add(collection);
    }
    
    public String getTestName() {
        return testName;
    }
    
    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getAllFolders() {
        return allFolders;
    }
    
    public void setAllFolders(List<String> allFolders) {
        this.allFolders = allFolders;
    }
    
    public List<ExpectedCollection> getExpectedCollections() {
        return expectedCollections;
    }
    
    public void setExpectedCollections(List<ExpectedCollection> expectedCollections) {
        this.expectedCollections = expectedCollections;
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
        sb.append("=== 测试用例 ===\n");
        sb.append("测试名称: ").append(testName).append("\n");
        sb.append("描述: ").append(description != null ? description : "无").append("\n");
        sb.append("时间戳: ").append(timestamp).append("\n");
        sb.append("总文件夹数量: ").append(allFolders != null ? allFolders.size() : 0).append("\n");
        sb.append("预期合集数量: ").append(expectedCollections != null ? expectedCollections.size() : 0).append("\n");
        
        if (expectedCollections != null && !expectedCollections.isEmpty()) {
            sb.append("\n=== 预期合集 ===\n");
            for (int i = 0; i < expectedCollections.size(); i++) {
                ExpectedCollection collection = expectedCollections.get(i);
                sb.append("合集 ").append(i + 1).append(": ").append(collection.getCollectionName()).append("\n");
                sb.append("  文件夹数量: ").append(collection.getFolderNames() != null ? collection.getFolderNames().size() : 0).append("\n");
                if (collection.getFolderNames() != null) {
                    for (String folder : collection.getFolderNames()) {
                        sb.append("    - ").append(folder).append("\n");
                    }
                }
            }
        }
        
        return sb.toString();
    }
}
