package com.filemanager.backend.service;

import com.filemanager.backend.entity.TaskOperationLogPO;

import java.util.List;

public interface TaskOperationLogService {
    
    TaskOperationLogPO createLog(TaskOperationLogPO log);
    
    TaskOperationLogPO getLogById(Long id);
    
    List<TaskOperationLogPO> getLogsByTaskId(String taskId);
    
    List<TaskOperationLogPO> getLogsByOperationType(String operationType);
    
    List<TaskOperationLogPO> getLogsByPage(int page, int size);
    
    TaskOperationLogPO updateLog(TaskOperationLogPO log);
    
    boolean deleteLog(Long id);
    
    boolean deleteLogsByTaskId(String taskId);
    
    long getTotalLogCount();
    
    long getLogCountByTaskId(String taskId);
}
