package com.filemanager.backend.service;

import com.filemanager.backend.entity.ChangeRecordPO;

import java.util.List;

public interface ChangeRecordService {
    
    ChangeRecordPO createRecord(ChangeRecordPO changeRecord);
    
    ChangeRecordPO getRecordById(Long id);
    
    List<ChangeRecordPO> getRecordsByTaskId(String taskId);
    
    List<ChangeRecordPO> getRecordsByStatus(String status);
    
    List<ChangeRecordPO> getRecordsByOperationType(String operationType);
    
    List<ChangeRecordPO> searchRecords(String keyword, String searchFields, int page, int size);
    
    List<ChangeRecordPO> getRecordsByPage(String taskId, String status, String operationType, Boolean changed, String keyword, String searchFields, String sortBy, String sortOrder, int page, int size);
    
    ChangeRecordPO updateRecord(ChangeRecordPO changeRecord);
    
    boolean deleteRecord(Long id);
    
    boolean deleteRecordsByTaskId(String taskId);
    
    long getTotalRecordCount();
    
    long getRecordCountByTaskId(String taskId);
    
    long getRecordCountByStatus(String status);
    
    long countByPage(String taskId, String status, String operationType, Boolean changed, String keyword);
}
