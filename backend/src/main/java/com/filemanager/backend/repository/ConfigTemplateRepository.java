package com.filemanager.backend.repository;

import com.filemanager.backend.entity.ConfigTemplatePO;
import com.filemanager.backend.mapper.ConfigTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ConfigTemplateRepository {
    
    @Autowired
    private ConfigTemplateMapper configTemplateMapper;
    
    public int create(ConfigTemplatePO template) {
        return configTemplateMapper.insert(template);
    }
    
    public int update(ConfigTemplatePO template) {
        return configTemplateMapper.update(template);
    }
    
    public int delete(String templateId) {
        return configTemplateMapper.deleteByTemplateId(templateId);
    }
    
    public ConfigTemplatePO findById(String templateId) {
        return configTemplateMapper.selectByTemplateId(templateId);
    }
    
    public List<ConfigTemplatePO> findAll() {
        return configTemplateMapper.selectAll();
    }
    
    public List<ConfigTemplatePO> findByTemplateType(String templateType) {
        return configTemplateMapper.selectByTemplateType(templateType);
    }
    
    public List<ConfigTemplatePO> findByCategory(String category) {
        return configTemplateMapper.selectByCategory(category);
    }
    
    public List<ConfigTemplatePO> findByIsDefault(Boolean isDefault) {
        return configTemplateMapper.selectByIsDefault(isDefault);
    }
    
    public List<ConfigTemplatePO> findByPage(String templateType, String category, 
                                               Boolean isDefault, String sortBy, 
                                               String sortOrder, int page, int size) {
        int offset = (page - 1) * size;
        return configTemplateMapper.selectByPage(templateType, category, isDefault, 
                                               sortBy, sortOrder, offset, size);
    }
    
    public int countByPage(String templateType, String category, Boolean isDefault) {
        return configTemplateMapper.countByPage(templateType, category, isDefault);
    }
    
    public int incrementUsageCount(String templateId) {
        return configTemplateMapper.incrementUsageCount(templateId);
    }
}
