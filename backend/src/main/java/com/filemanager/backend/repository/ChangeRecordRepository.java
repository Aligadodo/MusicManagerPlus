package com.filemanager.backend.repository;

import com.filemanager.backend.entity.ChangeRecordPO;
import com.filemanager.backend.mapper.ChangeRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class ChangeRecordRepository {
    
    @Autowired
    private ChangeRecordMapper changeRecordMapper;
    
    public int create(ChangeRecordPO changeRecord) {
        return changeRecordMapper.insert(changeRecord);
    }
    
    public int batchCreate(List<ChangeRecordPO> records) {
        return changeRecordMapper.batchInsert(records);
    }
    
    public int update(ChangeRecordPO changeRecord) {
        return changeRecordMapper.update(changeRecord);
    }
    
    public int delete(Long id) {
        return changeRecordMapper.deleteById(id);
    }
    
    public int deleteByTaskId(String taskId) {
        return changeRecordMapper.deleteByTaskId(taskId);
    }
    
    public ChangeRecordPO findById(Long id) {
        return changeRecordMapper.selectById(id);
    }
    
    public List<ChangeRecordPO> findByTaskId(String taskId) {
        return changeRecordMapper.selectByTaskId(taskId);
    }
    
    public List<ChangeRecordPO> findByTaskIdAndStatus(String taskId, String status) {
        return changeRecordMapper.selectByTaskIdAndStatus(taskId, status);
    }
    
    public List<ChangeRecordPO> findByTaskIdAndOperationType(String taskId, String operationType) {
        return changeRecordMapper.selectByTaskIdAndOperationType(taskId, operationType);
    }
    
    public List<ChangeRecordPO> findByTaskIdAndChanged(String taskId, Boolean changed) {
        return changeRecordMapper.selectByTaskIdAndChanged(taskId, changed);
    }
    
    public List<ChangeRecordPO> findByPage(String taskId, String status, String operationType, 
                                           Boolean changed, String searchFields, String keyword, 
                                           String sortBy, String sortOrder, int page, int size) {
        int offset = (page - 1) * size;
        return changeRecordMapper.selectByPage(taskId, status, operationType, changed, 
                                           searchFields, keyword, sortBy, sortOrder, offset, size);
    }
    
    public int countByPage(String taskId, String status, String operationType, 
                         Boolean changed, String searchFields, String keyword) {
        return changeRecordMapper.countByPage(taskId, status, operationType, changed, 
                                         searchFields, keyword);
    }
    
    public int updateStatus(Long id, String status) {
        return changeRecordMapper.updateStatus(id, status);
    }
    
    public int updateSelected(Long id, Boolean selected) {
        return changeRecordMapper.updateSelected(id, selected);
    }
}
