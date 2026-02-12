package com.filemanager.backend.service.impl;

import com.filemanager.backend.config.ConfigManager;
import com.filemanager.backend.service.PreviewLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
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
    
    private final ConfigManager configManager;
    
    // 配置键
    private static final String KEY_PREVIEW_LIMIT = "previewLimit";
    private static final String KEY_EXECUTION_LIMIT = "executionLimit";
    private static final String KEY_PREVIEW_UNLIMITED = "previewUnlimited";
    private static final String KEY_EXECUTION_UNLIMITED = "executionUnlimited";
    
    @Autowired
    public PreviewLimitServiceImpl(ConfigManager configManager) {
        this.configManager = configManager;
        loadLimitsFromConfig();
    }
    
    private void loadLimitsFromConfig() {
        // 加载全局限制
        Integer previewLimit = configManager.getConfig(KEY_PREVIEW_LIMIT, Integer.class);
        if (previewLimit != null) {
            this.globalPreviewLimit = previewLimit;
        }
        
        Integer executionLimit = configManager.getConfig(KEY_EXECUTION_LIMIT, Integer.class);
        if (executionLimit != null) {
            this.globalExecutionLimit = executionLimit;
        }
        
        Boolean previewUnlimited = configManager.getConfig(KEY_PREVIEW_UNLIMITED, Boolean.class);
        if (previewUnlimited != null) {
            this.globalPreviewUnlimited = previewUnlimited;
        }
        
        Boolean executionUnlimited = configManager.getConfig(KEY_EXECUTION_UNLIMITED, Boolean.class);
        if (executionUnlimited != null) {
            this.globalExecutionUnlimited = executionUnlimited;
        }
    }
    
    private void saveLimitsToConfig() {
        configManager.setConfig(KEY_PREVIEW_LIMIT, globalPreviewLimit);
        configManager.setConfig(KEY_EXECUTION_LIMIT, globalExecutionLimit);
        configManager.setConfig(KEY_PREVIEW_UNLIMITED, globalPreviewUnlimited);
        configManager.setConfig(KEY_EXECUTION_UNLIMITED, globalExecutionUnlimited);
    }

    @Override
    public int getGlobalPreviewLimit() {
        return globalPreviewLimit;
    }

    @Override
    public void setGlobalPreviewLimit(int limit) {
        this.globalPreviewLimit = limit;
        saveLimitsToConfig();
    }

    @Override
    public boolean isGlobalPreviewUnlimited() {
        return globalPreviewUnlimited;
    }

    @Override
    public void setGlobalPreviewUnlimited(boolean unlimited) {
        this.globalPreviewUnlimited = unlimited;
        saveLimitsToConfig();
    }

    @Override
    public int getGlobalExecutionLimit() {
        return globalExecutionLimit;
    }

    @Override
    public void setGlobalExecutionLimit(int limit) {
        this.globalExecutionLimit = limit;
        saveLimitsToConfig();
    }

    @Override
    public boolean isGlobalExecutionUnlimited() {
        return globalExecutionUnlimited;
    }

    @Override
    public void setGlobalExecutionUnlimited(boolean unlimited) {
        this.globalExecutionUnlimited = unlimited;
        saveLimitsToConfig();
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
}
