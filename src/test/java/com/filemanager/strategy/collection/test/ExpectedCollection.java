package com.filemanager.strategy.collection.test;

import java.util.ArrayList;
import java.util.List;

/**
 * 预期的合集组合
 */
public class ExpectedCollection {
    
    private String collectionName;
    private List<String> folderNames;
    private String description;
    
    public ExpectedCollection() {
        this.folderNames = new ArrayList<>();
    }
    
    public ExpectedCollection(String collectionName) {
        this();
        this.collectionName = collectionName;
    }
    
    public ExpectedCollection(String collectionName, List<String> folderNames) {
        this.collectionName = collectionName;
        this.folderNames = folderNames;
    }
    
    public void addFolder(String folderName) {
        if (this.folderNames == null) {
            this.folderNames = new ArrayList<>();
        }
        this.folderNames.add(folderName);
    }
    
    public String getCollectionName() {
        return collectionName;
    }
    
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
    
    public List<String> getFolderNames() {
        return folderNames;
    }
    
    public void setFolderNames(List<String> folderNames) {
        this.folderNames = folderNames;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "合集名称: " + collectionName + 
               ", 文件夹数量: " + (folderNames != null ? folderNames.size() : 0) +
               ", 文件夹: " + folderNames;
    }
}
