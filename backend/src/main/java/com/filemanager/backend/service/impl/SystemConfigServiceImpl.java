package com.filemanager.backend.service.impl;

import com.filemanager.backend.entity.SystemConfigPO;
import com.filemanager.backend.mapper.SystemConfigMapper;
import com.filemanager.backend.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public SystemConfigPO createConfig(SystemConfigPO config) {
        Date now = new Date();
        config.setCreatedAt(now);
        config.setUpdatedAt(now);
        systemConfigMapper.insert(config);
        return config;
    }

    @Override
    public SystemConfigPO getConfigById(Long id) {
        return systemConfigMapper.selectByConfigKey(String.valueOf(id));
    }

    @Override
    public SystemConfigPO getConfigByKey(String configKey) {
        return systemConfigMapper.selectByConfigKey(configKey);
    }

    @Override
    public List<SystemConfigPO> getAllConfigs() {
        return systemConfigMapper.selectAll();
    }

    @Override
    public List<SystemConfigPO> getConfigsByCategory(String category) {
        return systemConfigMapper.selectByCategory(category);
    }

    @Override
    public List<SystemConfigPO> getConfigsByPage(int page, int size) {
        int offset = (page - 1) * size;
        return systemConfigMapper.selectByPage(null, null, null, offset, size);
    }

    @Override
    public SystemConfigPO updateConfig(SystemConfigPO config) {
        systemConfigMapper.update(config);
        return config;
    }

    @Override
    public boolean deleteConfig(Long id) {
        return systemConfigMapper.deleteByConfigKey(String.valueOf(id)) > 0;
    }

    @Override
    public boolean deleteConfigByKey(String configKey) {
        return systemConfigMapper.deleteByConfigKey(configKey) > 0;
    }

    @Override
    public long getTotalConfigCount() {
        return systemConfigMapper.countByPage(null);
    }

    @Override
    public long getConfigCountByCategory(String category) {
        return systemConfigMapper.countByPage(category);
    }
}
