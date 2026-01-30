package com.filemanager.domain.service;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public interface TaskService {
    String createTask(TaskRequestDTO request);
    TaskStatusDTO getTaskStatus(String taskId);
    List<TaskStatusDTO> getTasks(String status, int page, int size);
    boolean executeTask(String taskId);
    boolean cancelTask(String taskId);
    List<ChangeRecord> getTaskResults(String taskId);
    boolean deleteTask(String taskId);
}
