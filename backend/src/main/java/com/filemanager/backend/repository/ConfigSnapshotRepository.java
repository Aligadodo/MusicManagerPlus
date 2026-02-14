package com.filemanager.backend.repository;

import com.filemanager.backend.entity.ConfigSnapshotPO;
import com.filemanager.backend.mapper.ConfigSnapshotMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class ConfigSnapshotRepository {
    
    @Autowired
    private ConfigSnapshotMapper configSnapshotMapper;
    
    public int create(ConfigSnapshotPO snapshot) {
        return configSnapshotMapper.insert(snapshot);
    }
    
    public int update(ConfigSnapshotPO snapshot) {
        return configSnapshotMapper.update(snapshot);
    }
    
    public int delete(String snapshotId) {
        return configSnapshotMapper.deleteBySnapshotId(snapshotId);
    }
    
    public ConfigSnapshotPO findById(String snapshotId) {
        return configSnapshotMapper.selectBySnapshotId(snapshotId);
    }
    
    public List<ConfigSnapshotPO> findAll() {
        return configSnapshotMapper.selectAll();
    }
    
    public List<ConfigSnapshotPO> findBySnapshotType(String snapshotType) {
        return configSnapshotMapper.selectBySnapshotType(snapshotType);
    }
    
    public List<ConfigSnapshotPO> findByIsTemplate(Boolean isTemplate) {
        return configSnapshotMapper.selectByIsTemplate(isTemplate);
    }
    
    public List<ConfigSnapshotPO> findByPage(String snapshotType, Boolean isTemplate, 
                                               Date startDate, Date endDate, 
                                               String sortBy, String sortOrder, 
                                               int page, int size) {
        int offset = (page - 1) * size;
        return configSnapshotMapper.selectByPage(snapshotType, isTemplate, startDate, endDate, 
                                               sortBy, sortOrder, offset, size);
    }
    
    public int countByPage(String snapshotType, Boolean isTemplate, 
                          Date startDate, Date endDate) {
        return configSnapshotMapper.countByPage(snapshotType, isTemplate, startDate, endDate);
    }
    
    public int updateConfigData(String snapshotId, String configData) {
        return configSnapshotMapper.updateConfigData(snapshotId, configData);
    }
}
