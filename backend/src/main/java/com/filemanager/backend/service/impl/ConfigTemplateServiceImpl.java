package com.filemanager.backend.service.impl;

import com.filemanager.backend.entity.ConfigTemplatePO;
import com.filemanager.backend.mapper.ConfigTemplateMapper;
import com.filemanager.backend.service.ConfigTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ConfigTemplateServiceImpl implements ConfigTemplateService {

    @Autowired
    private ConfigTemplateMapper configTemplateMapper;

    @Override
    public ConfigTemplatePO createTemplate(ConfigTemplatePO template) {
        Date now = new Date();
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        configTemplateMapper.insert(template);
        return template;
    }

    @Override
    public ConfigTemplatePO getTemplateById(String templateId) {
        return configTemplateMapper.selectByTemplateId(templateId);
    }

    @Override
    public List<ConfigTemplatePO> getAllTemplates() {
        return configTemplateMapper.selectAll();
    }

    @Override
    public List<ConfigTemplatePO> getTemplatesByCategory(String category) {
        return configTemplateMapper.selectByCategory(category);
    }

    @Override
    public List<ConfigTemplatePO> getTemplatesByPage(int page, int size) {
        int offset = (page - 1) * size;
        return configTemplateMapper.selectByPage(null, null, null, null, null, offset, size);
    }

    @Override
    public ConfigTemplatePO updateTemplate(ConfigTemplatePO template) {
        configTemplateMapper.update(template);
        return template;
    }

    @Override
    public boolean deleteTemplate(String templateId) {
        return configTemplateMapper.deleteByTemplateId(templateId) > 0;
    }

    @Override
    public long getTotalTemplateCount() {
        return configTemplateMapper.countByPage(null, null, null);
    }

    @Override
    public long getTemplateCountByCategory(String category) {
        return configTemplateMapper.countByPage(null, category, null);
    }
}
