package com.filemanager.backend.service.impl;

import com.filemanager.backend.entity.TaskOperationLogPO;
import com.filemanager.backend.mapper.TaskOperationLogMapper;
import com.filemanager.backend.service.TaskOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskOperationLogServiceImpl implements TaskOperationLogService {

    @Autowired
    private TaskOperationLogMapper taskOperationLogMapper;

    @Override
    public TaskOperationLogPO createLog(TaskOperationLogPO log) {
        taskOperationLogMapper.insert(log);
        return log;
    }

    @Override
    public TaskOperationLogPO getLogById(Long id) {
        return taskOperationLogMapper.selectById(id);
    }

    @Override
    public List<TaskOperationLogPO> getLogsByTaskId(String taskId) {
        return taskOperationLogMapper.selectByTaskId(taskId);
    }

    @Override
    public List<TaskOperationLogPO> getLogsByOperationType(String operationType) {
        return taskOperationLogMapper.selectByPage(null, operationType, null, null, null, null, 0, 1000);
    }

    @Override
    public List<TaskOperationLogPO> getLogsByPage(int page, int size) {
        int offset = (page - 1) * size;
        return taskOperationLogMapper.selectByPage(null, null, null, null, null, null, offset, size);
    }

    @Override
    public TaskOperationLogPO updateLog(TaskOperationLogPO log) {
        taskOperationLogMapper.update(log);
        return log;
    }

    @Override
    public boolean deleteLog(Long id) {
        return taskOperationLogMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteLogsByTaskId(String taskId) {
        return taskOperationLogMapper.deleteByTaskId(taskId) > 0;
    }

    @Override
    public long getTotalLogCount() {
        return taskOperationLogMapper.countByPage(null, null, null, null);
    }

    @Override
    public long getLogCountByTaskId(String taskId) {
        return taskOperationLogMapper.countByPage(taskId, null, null, null);
    }
}
