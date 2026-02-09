package com.filemanager.plugin.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface NamingStrategy {
    
    String generateName(FileCluster cluster, Map<String, Object> context);
    
    String getId();
    
    String getName();
    
    String getDescription();
    
    default void validateContext(Map<String, Object> context) {
    }
}

class ExactNamingStrategy implements NamingStrategy {
    
    @Override
    public String getId() {
        return "exact";
    }
    
    @Override
    public String getName() {
        return "精确命名策略";
    }
    
    @Override
    public String getDescription() {
        return "使用完整的最长公共前缀作为合集名称";
    }
    
    @Override
    public String generateName(FileCluster cluster, Map<String, Object> context) {
        String commonPrefix = cluster.getCommonPrefix();
        
        if (commonPrefix == null || commonPrefix.isEmpty()) {
            return "合集_" + System.currentTimeMillis();
        }
        
        return commonPrefix;
    }
}

class SimpleNamingStrategy implements NamingStrategy {
    
    @Override
    public String getId() {
        return "simple";
    }
    
    @Override
    public String getName() {
        return "简洁命名策略";
    }
    
    @Override
    public String getDescription() {
        return "使用简化的公共前缀作为合集名称";
    }
    
    @Override
    public String generateName(FileCluster cluster, Map<String, Object> context) {
        String commonPrefix = cluster.getCommonPrefix();
        
        if (commonPrefix == null || commonPrefix.isEmpty()) {
            return "合集_" + System.currentTimeMillis();
        }
        
        return simplifyName(commonPrefix);
    }
    
    private String simplifyName(String name) {
        name = name.trim();
        
        name = name.replaceAll("\\s+", " ");
        
        name = name.replaceAll("[_\\-]+", " ");
        
        name = name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", "");
        
        name = name.trim();
        
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        
        return name;
    }
}

class TemplateNamingStrategy implements NamingStrategy {
    
    private static final String DEFAULT_TEMPLATE = "{prefix}{name}{suffix}";
    
    @Override
    public String getId() {
        return "template";
    }
    
    @Override
    public String getName() {
        return "模板命名策略";
    }
    
    @Override
    public String getDescription() {
        return "使用自定义模板生成合集名称";
    }
    
    @Override
    public void validateContext(Map<String, Object> context) {
        if (context == null || !context.containsKey("template")) {
            throw new IllegalArgumentException("模板命名策略需要提供template参数");
        }
    }
    
    @Override
    public String generateName(FileCluster cluster, Map<String, Object> context) {
        String template = (String) context.getOrDefault("template", DEFAULT_TEMPLATE);
        String commonPrefix = cluster.getCommonPrefix();
        
        if (commonPrefix == null || commonPrefix.isEmpty()) {
            commonPrefix = "合集";
        }
        
        String prefix = (String) context.getOrDefault("prefix", "");
        String suffix = (String) context.getOrDefault("suffix", "");
        
        String name = template
            .replace("{name}", commonPrefix)
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{size}", String.valueOf(cluster.size()))
            .replace("{similarity}", String.format("%.2f", cluster.getAverageSimilarity()));
        
        return name;
    }
}

class UniversalNamingStrategy implements NamingStrategy {
    
    @Override
    public String getId() {
        return "universal";
    }
    
    @Override
    public String getName() {
        return "通用命名策略";
    }
    
    @Override
    public String getDescription() {
        return "使用通用名称作为合集名称";
    }
    
    @Override
    public String generateName(FileCluster cluster, Map<String, Object> context) {
        return "合集_" + System.currentTimeMillis();
    }
}

class NamingStrategyFactory {
    
    public static NamingStrategy getStrategy(String strategyId) {
        if (strategyId == null || strategyId.isEmpty()) {
            return new ExactNamingStrategy();
        }
        
        switch (strategyId.toLowerCase()) {
            case "exact":
                return new ExactNamingStrategy();
            case "simple":
                return new SimpleNamingStrategy();
            case "template":
                return new TemplateNamingStrategy();
            case "universal":
                return new UniversalNamingStrategy();
            default:
                return new ExactNamingStrategy();
        }
    }
    
    public static List<NamingStrategy> getAllStrategies() {
        List<NamingStrategy> strategies = new ArrayList<>();
        strategies.add(new ExactNamingStrategy());
        strategies.add(new SimpleNamingStrategy());
        strategies.add(new TemplateNamingStrategy());
        strategies.add(new UniversalNamingStrategy());
        return strategies;
    }
    
    public static NamingStrategy getDefaultStrategy() {
        return new ExactNamingStrategy();
    }
}
