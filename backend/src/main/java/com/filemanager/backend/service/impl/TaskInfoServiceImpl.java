package com.filemanager.backend.service.impl;

import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.backend.mapper.TaskInfoMapper;
import com.filemanager.backend.service.TaskInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskInfoServiceImpl implements TaskInfoService {

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Override
    public TaskInfoPO createTask(TaskInfoPO taskInfo) {
        taskInfoMapper.insert(taskInfo);
        return taskInfo;
    }

    @Override
    public TaskInfoPO getTaskById(String taskId) {
        return taskInfoMapper.selectByTaskId(taskId);
    }

    @Override
    public List<TaskInfoPO> getAllTasks() {
        return taskInfoMapper.selectAll();
    }

    @Override
    public List<TaskInfoPO> getTasksByStatus(String status) {
        return taskInfoMapper.selectByStatus(status);
    }

    @Override
    public List<TaskInfoPO> getTasksByPage(int page, int size) {
        int offset = (page - 1) * size;
        return taskInfoMapper.selectByPage(null, null, null, null, "created_at", "DESC", offset, size);
    }

    @Override
    public List<TaskInfoPO> searchTasks(String keyword, int page, int size) {
        int offset = (page - 1) * size;
        return taskInfoMapper.selectByPage(null, null, null, keyword, "created_at", "DESC", offset, size);
    }

    @Override
    public TaskInfoPO updateTask(TaskInfoPO taskInfo) {
        taskInfoMapper.update(taskInfo);
        return taskInfo;
    }

    @Override
    public boolean deleteTask(String taskId) {
        return taskInfoMapper.deleteByTaskId(taskId) > 0;
    }

    @Override
    public boolean updateTaskStatus(String taskId, String status) {
        return taskInfoMapper.updateStatus(taskId, status) > 0;
    }

    @Override
    public boolean updateTaskProgress(String taskId, int progress) {
        return taskInfoMapper.updateProgress(taskId, (double) progress) > 0;
    }

    @Override
    public boolean incrementProcessedFiles(String taskId) {
        return taskInfoMapper.updateMessage(taskId, "Processed files incremented") > 0;
    }

    @Override
    public boolean incrementSuccessCount(String taskId) {
        return taskInfoMapper.updateMessage(taskId, "Success count incremented") > 0;
    }

    @Override
    public boolean incrementFailedCount(String taskId) {
        return taskInfoMapper.updateMessage(taskId, "Failed count incremented") > 0;
    }

    @Override
    public boolean incrementSkippedCount(String taskId) {
        return taskInfoMapper.updateMessage(taskId, "Skipped count incremented") > 0;
    }

    @Override
    public long getTotalTaskCount() {
        return taskInfoMapper.countByPage(null, null, null, null);
    }

    @Override
    public long getTaskCountByStatus(String status) {
        return taskInfoMapper.countByPage(status, null, null, null);
    }
}
