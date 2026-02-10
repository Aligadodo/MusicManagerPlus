package com.filemanager.backend.service.impl;

import com.filemanager.backend.service.PreviewLimitService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PreviewLimitServiceImpl implements PreviewLimitService {
    
    private int globalPreviewLimit = 100;
    private boolean globalPreviewUnlimited = true;
    
    private int globalExecutionLimit = 100;
    private boolean globalExecutionUnlimited = true;
    
    private final Map<String, Integer> rootPathPreviewLimits = new ConcurrentHashMap<>();
    private final Map<String, Boolean> rootPathPreviewUnlimited = new ConcurrentHashMap<>();
    
    private final Map<String, Integer> rootPathExecutionLimits = new ConcurrentHashMap<>();
    private final Map<String, Boolean> rootPathExecutionUnlimited = new ConcurrentHashMap<>();

    @Override
    public int getGlobalPreviewLimit() {
        return globalPreviewLimit;
    }

    @Override
    public void setGlobalPreviewLimit(int limit) {
        this.globalPreviewLimit = limit;
    }

    @Override
    public boolean isGlobalPreviewUnlimited() {
        return globalPreviewUnlimited;
    }

    @Override
    public void setGlobalPreviewUnlimited(boolean unlimited) {
        this.globalPreviewUnlimited = unlimited;
    }

    @Override
    public int getGlobalExecutionLimit() {
        return globalExecutionLimit;
    }

    @Override
    public void setGlobalExecutionLimit(int limit) {
        this.globalExecutionLimit = limit;
    }

    @Override
    public boolean isGlobalExecutionUnlimited() {
        return globalExecutionUnlimited;
    }

    @Override
    public void setGlobalExecutionUnlimited(boolean unlimited) {
        this.globalExecutionUnlimited = unlimited;
    }

    @Override
    public int getRootPathPreviewLimit(String rootPath) {
        if (rootPath == null) {
            return globalPreviewLimit;
        }
        return rootPathPreviewLimits.getOrDefault(rootPath, globalPreviewLimit);
    }

    @Override
    public void setRootPathPreviewLimit(String rootPath, int limit) {
        if (rootPath != null) {
            rootPathPreviewLimits.put(rootPath, limit);
        }
    }

    @Override
    public boolean isRootPathPreviewUnlimited(String rootPath) {
        if (rootPath == null) {
            return globalPreviewUnlimited;
        }
        return rootPathPreviewUnlimited.getOrDefault(rootPath, globalPreviewUnlimited);
    }

    @Override
    public void setRootPathPreviewUnlimited(String rootPath, boolean unlimited) {
        if (rootPath != null) {
            rootPathPreviewUnlimited.put(rootPath, unlimited);
        }
    }

    @Override
    public int getRootPathExecutionLimit(String rootPath) {
        if (rootPath == null) {
            return globalExecutionLimit;
        }
        return rootPathExecutionLimits.getOrDefault(rootPath, globalExecutionLimit);
    }

    @Override
    public void setRootPathExecutionLimit(String rootPath, int limit) {
        if (rootPath != null) {
            rootPathExecutionLimits.put(rootPath, limit);
        }
    }

    @Override
    public boolean isRootPathExecutionUnlimited(String rootPath) {
        if (rootPath == null) {
            return globalExecutionUnlimited;
        }
        return rootPathExecutionUnlimited.getOrDefault(rootPath, globalExecutionUnlimited);
    }

    @Override
    public void setRootPathExecutionUnlimited(String rootPath, boolean unlimited) {
        if (rootPath != null) {
            rootPathExecutionUnlimited.put(rootPath, unlimited);
        }
    }

    @Override
    public Map<String, Integer> getAllRootPathPreviewLimits() {
        return new HashMap<>(rootPathPreviewLimits);
    }

    @Override
    public Map<String, Integer> getAllRootPathExecutionLimits() {
        return new HashMap<>(rootPathExecutionLimits);
    }

    @Override
    public void clearAllRootPathLimits() {
        rootPathPreviewLimits.clear();
        rootPathPreviewUnlimited.clear();
        rootPathExecutionLimits.clear();
        rootPathExecutionUnlimited.clear();
    }
    
    public void saveConfig(Properties props) {
        if (props == null) return;
        props.setProperty("limit.preview.global", String.valueOf(globalPreviewLimit));
        props.setProperty("limit.preview.unlimited", String.valueOf(globalPreviewUnlimited));
        props.setProperty("limit.execution.global", String.valueOf(globalExecutionLimit));
        props.setProperty("limit.execution.unlimited", String.valueOf(globalExecutionUnlimited));
        
        for (Map.Entry<String, Integer> entry : rootPathPreviewLimits.entrySet()) {
            props.setProperty("limit.preview.root." + entry.getKey(), String.valueOf(entry.getValue()));
        }
        
        for (Map.Entry<String, Boolean> entry : rootPathPreviewUnlimited.entrySet()) {
            props.setProperty("limit.preview.root.unlimited." + entry.getKey(), String.valueOf(entry.getValue()));
        }
        
        for (Map.Entry<String, Integer> entry : rootPathExecutionLimits.entrySet()) {
            props.setProperty("limit.execution.root." + entry.getKey(), String.valueOf(entry.getValue()));
        }
        
        for (Map.Entry<String, Boolean> entry : rootPathExecutionUnlimited.entrySet()) {
            props.setProperty("limit.execution.root.unlimited." + entry.getKey(), String.valueOf(entry.getValue()));
        }
    }
    
    public void loadConfig(Properties props) {
        if (props == null) return;
        
        String previewLimit = props.getProperty("limit.preview.global");
        if (previewLimit != null) {
            globalPreviewLimit = Integer.parseInt(previewLimit);
        }
        
        String previewUnlimited = props.getProperty("limit.preview.unlimited");
        if (previewUnlimited != null) {
            globalPreviewUnlimited = Boolean.parseBoolean(previewUnlimited);
        }
        
        String executionLimit = props.getProperty("limit.execution.global");
        if (executionLimit != null) {
            globalExecutionLimit = Integer.parseInt(executionLimit);
        }
        
        String executionUnlimited = props.getProperty("limit.execution.unlimited");
        if (executionUnlimited != null) {
            globalExecutionUnlimited = Boolean.parseBoolean(executionUnlimited);
        }
        
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("limit.preview.root.") && !key.contains(".unlimited.")) {
                String rootPath = key.substring("limit.preview.root.".length());
                String value = props.getProperty(key);
                rootPathPreviewLimits.put(rootPath, Integer.parseInt(value));
            } else if (key.startsWith("limit.preview.root.unlimited.")) {
                String rootPath = key.substring("limit.preview.root.unlimited.".length());
                String value = props.getProperty(key);
                rootPathPreviewUnlimited.put(rootPath, Boolean.parseBoolean(value));
            } else if (key.startsWith("limit.execution.root.") && !key.contains(".unlimited.")) {
                String rootPath = key.substring("limit.execution.root.".length());
                String value = props.getProperty(key);
                rootPathExecutionLimits.put(rootPath, Integer.parseInt(value));
            } else if (key.startsWith("limit.execution.root.unlimited.")) {
                String rootPath = key.substring("limit.execution.root.unlimited.".length());
                String value = props.getProperty(key);
                rootPathExecutionUnlimited.put(rootPath, Boolean.parseBoolean(value));
            }
        }
    }
}
