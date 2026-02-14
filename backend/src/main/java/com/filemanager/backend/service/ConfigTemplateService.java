package com.filemanager.backend.service;

import com.filemanager.backend.entity.ConfigTemplatePO;

import java.util.List;

public interface ConfigTemplateService {
    
    ConfigTemplatePO createTemplate(ConfigTemplatePO template);
    
    ConfigTemplatePO getTemplateById(String templateId);
    
    List<ConfigTemplatePO> getAllTemplates();
    
    List<ConfigTemplatePO> getTemplatesByCategory(String category);
    
    List<ConfigTemplatePO> getTemplatesByPage(int page, int size);
    
    ConfigTemplatePO updateTemplate(ConfigTemplatePO template);
    
    boolean deleteTemplate(String templateId);
    
    long getTotalTemplateCount();
    
    long getTemplateCountByCategory(String category);
}
