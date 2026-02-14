package com.filemanager.backend.service;

import com.filemanager.backend.entity.SystemConfigPO;

import java.util.List;

public interface SystemConfigService {
    
    SystemConfigPO createConfig(SystemConfigPO config);
    
    SystemConfigPO getConfigById(Long id);
    
    SystemConfigPO getConfigByKey(String configKey);
    
    List<SystemConfigPO> getAllConfigs();
    
    List<SystemConfigPO> getConfigsByCategory(String category);
    
    List<SystemConfigPO> getConfigsByPage(int page, int size);
    
    SystemConfigPO updateConfig(SystemConfigPO config);
    
    boolean deleteConfig(Long id);
    
    boolean deleteConfigByKey(String configKey);
    
    long getTotalConfigCount();
    
    long getConfigCountByCategory(String category);
}
