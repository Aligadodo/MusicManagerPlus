package com.filemanager.backend.service;

import java.util.Map;

public interface PreviewLimitService {
    int getGlobalPreviewLimit();
    void setGlobalPreviewLimit(int limit);
    boolean isGlobalPreviewUnlimited();
    void setGlobalPreviewUnlimited(boolean unlimited);
    
    int getGlobalExecutionLimit();
    void setGlobalExecutionLimit(int limit);
    boolean isGlobalExecutionUnlimited();
    void setGlobalExecutionUnlimited(boolean unlimited);
    
    int getRootPathPreviewLimit(String rootPath);
    void setRootPathPreviewLimit(String rootPath, int limit);
    boolean isRootPathPreviewUnlimited(String rootPath);
    void setRootPathPreviewUnlimited(String rootPath, boolean unlimited);
    
    int getRootPathExecutionLimit(String rootPath);
    void setRootPathExecutionLimit(String rootPath, int limit);
    boolean isRootPathExecutionUnlimited(String rootPath);
    void setRootPathExecutionUnlimited(String rootPath, boolean unlimited);
    
    Map<String, Integer> getAllRootPathPreviewLimits();
    Map<String, Integer> getAllRootPathExecutionLimits();
    
    void clearAllRootPathLimits();
}
