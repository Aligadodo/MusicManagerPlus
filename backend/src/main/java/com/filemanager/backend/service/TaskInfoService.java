package com.filemanager.backend.service;

import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.domain.enums.ExecStatus;

import java.util.List;

public interface TaskInfoService {
    
    TaskInfoPO createTask(TaskInfoPO taskInfo);
    
    TaskInfoPO getTaskById(String taskId);
    
    List<TaskInfoPO> getAllTasks();
    
    List<TaskInfoPO> getTasksByStatus(String status);
    
    List<TaskInfoPO> getTasksByPage(int page, int size);
    
    List<TaskInfoPO> searchTasks(String keyword, int page, int size);
    
    TaskInfoPO updateTask(TaskInfoPO taskInfo);
    
    boolean deleteTask(String taskId);
    
    boolean updateTaskStatus(String taskId, String status);
    
    boolean updateTaskProgress(String taskId, int progress);
    
    boolean incrementProcessedFiles(String taskId);
    
    boolean incrementSuccessCount(String taskId);
    
    boolean incrementFailedCount(String taskId);
    
    boolean incrementSkippedCount(String taskId);
    
    long getTotalTaskCount();
    
    long getTaskCountByStatus(String status);
}
