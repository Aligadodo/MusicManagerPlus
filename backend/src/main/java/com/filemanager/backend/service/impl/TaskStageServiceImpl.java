package com.filemanager.backend.service.impl;

import com.filemanager.backend.entity.TaskStagePO;
import com.filemanager.backend.mapper.TaskStageMapper;
import com.filemanager.backend.service.TaskStageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskStageServiceImpl implements TaskStageService {

    @Autowired
    private TaskStageMapper taskStageMapper;

    @Override
    public TaskStagePO createStage(TaskStagePO taskStage) {
        taskStageMapper.insert(taskStage);
        return taskStage;
    }

    @Override
    public TaskStagePO getStageById(Long id) {
        return taskStageMapper.selectById(id);
    }

    @Override
    public List<TaskStagePO> getStagesByTaskId(String taskId) {
        return taskStageMapper.selectByTaskId(taskId);
    }

    @Override
    public List<TaskStagePO> getStagesByStatus(String status) {
        List<TaskStagePO> allStages = taskStageMapper.selectByTaskId(null);
        allStages.removeIf(stage -> !status.equals(stage.getStatus()));
        return allStages;
    }

    @Override
    public TaskStagePO updateStage(TaskStagePO taskStage) {
        taskStageMapper.update(taskStage);
        return taskStage;
    }

    @Override
    public boolean deleteStage(Long id) {
        return taskStageMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteStagesByTaskId(String taskId) {
        return taskStageMapper.deleteByTaskId(taskId) > 0;
    }

    @Override
    public List<TaskStagePO> getStagesByPage(int page, int size) {
        List<TaskStagePO> allStages = taskStageMapper.selectByTaskId(null);
        int start = (page - 1) * size;
        int end = Math.min(start + size, allStages.size());
        if (start < allStages.size()) {
            return allStages.subList(start, end);
        }
        return allStages.subList(0, 0);
    }

    @Override
    public long getTotalStageCount() {
        return taskStageMapper.selectByTaskId(null).size();
    }

    @Override
    public long getStageCountByTaskId(String taskId) {
        return taskStageMapper.selectByTaskId(taskId).size();
    }
}
