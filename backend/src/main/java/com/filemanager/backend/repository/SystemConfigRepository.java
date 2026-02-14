package com.filemanager.backend.repository;

import com.filemanager.backend.entity.SystemConfigPO;
import com.filemanager.backend.mapper.SystemConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SystemConfigRepository {
    
    @Autowired
    private SystemConfigMapper systemConfigMapper;
    
    public int create(SystemConfigPO config) {
        return systemConfigMapper.insert(config);
    }
    
    public int update(SystemConfigPO config) {
        return systemConfigMapper.update(config);
    }
    
    public int delete(String configKey) {
        return systemConfigMapper.deleteByConfigKey(configKey);
    }
    
    public SystemConfigPO findById(String configKey) {
        return systemConfigMapper.selectByConfigKey(configKey);
    }
    
    public List<SystemConfigPO> findAll() {
        return systemConfigMapper.selectAll();
    }
    
    public List<SystemConfigPO> findByCategory(String category) {
        return systemConfigMapper.selectByCategory(category);
    }
    
    public List<SystemConfigPO> findByPage(String category, String sortBy, 
                                              String sortOrder, int page, int size) {
        int offset = (page - 1) * size;
        return systemConfigMapper.selectByPage(category, sortBy, sortOrder, offset, size);
    }
    
    public int countByPage(String category) {
        return systemConfigMapper.countByPage(category);
    }
}
