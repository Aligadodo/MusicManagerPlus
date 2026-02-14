package com.filemanager.backend.repository;

import com.filemanager.backend.entity.TaskOperationLogPO;
import com.filemanager.backend.mapper.TaskOperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class TaskOperationLogRepository {
    
    @Autowired
    private TaskOperationLogMapper taskOperationLogMapper;
    
    public int create(TaskOperationLogPO log) {
        return taskOperationLogMapper.insert(log);
    }
    
    public int update(TaskOperationLogPO log) {
        return taskOperationLogMapper.update(log);
    }
    
    public int delete(Long id) {
        return taskOperationLogMapper.deleteById(id);
    }
    
    public int deleteByTaskId(String taskId) {
        return taskOperationLogMapper.deleteByTaskId(taskId);
    }
    
    public TaskOperationLogPO findById(Long id) {
        return taskOperationLogMapper.selectById(id);
    }
    
    public List<TaskOperationLogPO> findByTaskId(String taskId) {
        return taskOperationLogMapper.selectByTaskId(taskId);
    }
    
    public List<TaskOperationLogPO> findByTaskIdAndOperationType(String taskId, String operationType) {
        return taskOperationLogMapper.selectByTaskIdAndOperationType(taskId, operationType);
    }
    
    public List<TaskOperationLogPO> findByTaskIdOrderByTime(String taskId, String sortOrder) {
        return taskOperationLogMapper.selectByTaskIdOrderByTime(taskId, sortOrder);
    }
    
    public List<TaskOperationLogPO> findByPage(String taskId, String operationType, 
                                               Date startDate, Date endDate, 
                                               String sortBy, String sortOrder, 
                                               int page, int size) {
        int offset = (page - 1) * size;
        return taskOperationLogMapper.selectByPage(taskId, operationType, startDate, endDate, 
                                               sortBy, sortOrder, offset, size);
    }
    
    public int countByPage(String taskId, String operationType, Date startDate, Date endDate) {
        return taskOperationLogMapper.countByPage(taskId, operationType, startDate, endDate);
    }
}
