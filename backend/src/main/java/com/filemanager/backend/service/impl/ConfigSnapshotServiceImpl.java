package com.filemanager.backend.service.impl;

import com.filemanager.backend.entity.ConfigSnapshotPO;
import com.filemanager.backend.mapper.ConfigSnapshotMapper;
import com.filemanager.backend.service.ConfigSnapshotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ConfigSnapshotServiceImpl implements ConfigSnapshotService {

    @Autowired
    private ConfigSnapshotMapper configSnapshotMapper;

    @Override
    public ConfigSnapshotPO createSnapshot(ConfigSnapshotPO snapshot) {
        Date now = new Date();
        snapshot.setCreatedAt(now);
        snapshot.setUpdatedAt(now);
        configSnapshotMapper.insert(snapshot);
        return snapshot;
    }

    @Override
    public ConfigSnapshotPO getSnapshotById(String snapshotId) {
        return configSnapshotMapper.selectBySnapshotId(snapshotId);
    }

    @Override
    public List<ConfigSnapshotPO> getAllSnapshots() {
        return configSnapshotMapper.selectAll();
    }

    @Override
    public List<ConfigSnapshotPO> getSnapshotsByTaskId(String taskId) {
        return configSnapshotMapper.selectByPage(null, null, null, null, null, null, 0, 1000);
    }

    @Override
    public List<ConfigSnapshotPO> getSnapshotsByType(String type) {
        return configSnapshotMapper.selectBySnapshotType(type);
    }

    @Override
    public List<ConfigSnapshotPO> getSnapshotsByPage(int page, int size) {
        int offset = (page - 1) * size;
        return configSnapshotMapper.selectByPage(null, null, null, null, null, null, offset, size);
    }

    @Override
    public ConfigSnapshotPO updateSnapshot(ConfigSnapshotPO snapshot) {
        configSnapshotMapper.update(snapshot);
        return snapshot;
    }

    @Override
    public boolean deleteSnapshot(String snapshotId) {
        return configSnapshotMapper.deleteBySnapshotId(snapshotId) > 0;
    }

    @Override
    public boolean deleteSnapshotsByTaskId(String taskId) {
        return configSnapshotMapper.deleteBySnapshotId(taskId) > 0;
    }

    @Override
    public long getTotalSnapshotCount() {
        return configSnapshotMapper.countByPage(null, null, null, null);
    }

    @Override
    public long getSnapshotCountByTaskId(String taskId) {
        return configSnapshotMapper.countByPage(null, null, null, null);
    }
}
