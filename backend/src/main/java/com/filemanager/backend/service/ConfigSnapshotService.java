package com.filemanager.backend.service;

import com.filemanager.backend.entity.ConfigSnapshotPO;

import java.util.List;

public interface ConfigSnapshotService {
    
    ConfigSnapshotPO createSnapshot(ConfigSnapshotPO snapshot);
    
    ConfigSnapshotPO getSnapshotById(String snapshotId);
    
    List<ConfigSnapshotPO> getAllSnapshots();
    
    List<ConfigSnapshotPO> getSnapshotsByTaskId(String taskId);
    
    List<ConfigSnapshotPO> getSnapshotsByType(String type);
    
    List<ConfigSnapshotPO> getSnapshotsByPage(int page, int size);
    
    ConfigSnapshotPO updateSnapshot(ConfigSnapshotPO snapshot);
    
    boolean deleteSnapshot(String snapshotId);
    
    boolean deleteSnapshotsByTaskId(String taskId);
    
    long getTotalSnapshotCount();
    
    long getSnapshotCountByTaskId(String taskId);
}
