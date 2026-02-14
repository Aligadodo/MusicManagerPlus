package com.filemanager.backend.service;

import com.filemanager.backend.entity.TaskStagePO;

import java.util.List;

public interface TaskStageService {
    
    TaskStagePO createStage(TaskStagePO taskStage);
    
    TaskStagePO getStageById(Long id);
    
    List<TaskStagePO> getStagesByTaskId(String taskId);
    
    List<TaskStagePO> getStagesByStatus(String status);
    
    TaskStagePO updateStage(TaskStagePO taskStage);
    
    boolean deleteStage(Long id);
    
    boolean deleteStagesByTaskId(String taskId);
    
    List<TaskStagePO> getStagesByPage(int page, int size);
    
    long getTotalStageCount();
    
    long getStageCountByTaskId(String taskId);
}
