package com.filemanager.backend.repository;

import com.filemanager.backend.entity.TaskStagePO;
import com.filemanager.backend.mapper.TaskStageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TaskStageRepository {
    
    @Autowired
    private TaskStageMapper taskStageMapper;
    
    public int create(TaskStagePO taskStage) {
        return taskStageMapper.insert(taskStage);
    }
    
    public int update(TaskStagePO taskStage) {
        return taskStageMapper.update(taskStage);
    }
    
    public int delete(Long id) {
        return taskStageMapper.deleteById(id);
    }
    
    public int deleteByTaskId(String taskId) {
        return taskStageMapper.deleteByTaskId(taskId);
    }
    
    public TaskStagePO findById(Long id) {
        return taskStageMapper.selectById(id);
    }
    
    public List<TaskStagePO> findByTaskId(String taskId) {
        return taskStageMapper.selectByTaskId(taskId);
    }
    
    public List<TaskStagePO> findByTaskIdAndStageType(String taskId, String stageType) {
        return taskStageMapper.selectByTaskIdAndStageType(taskId, stageType);
    }
    
    public List<TaskStagePO> findByTaskIdAndStatus(String taskId, String status) {
        return taskStageMapper.selectByTaskIdAndStatus(taskId, status);
    }
    
    public int updateStatus(Long id, String status) {
        return taskStageMapper.updateStatus(id, status);
    }
    
    public int updateDuration(Long id, Long duration) {
        return taskStageMapper.updateDuration(id, duration);
    }
}
