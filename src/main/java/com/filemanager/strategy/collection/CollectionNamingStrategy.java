package com.filemanager.strategy.collection;

/**
 * 合集命名策略枚举
 */
public enum CollectionNamingStrategy {
    /**
     * 简洁风格：保留核心关键词，去除大部分冗余信息
     */
    CONCISE("简洁风格", "保留核心关键词，去除年份、文件格式等冗余信息"),
    
    /**
     * 精确风格：尽可能保留更多有用信息
     */
    PRECISE("精确风格", "保留年份、专辑类型等重要信息，只去除CD序号、文件格式等差异信息");
    
    private final String displayName;
    private final String description;
    
    CollectionNamingStrategy(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
}