package com.filemanager.domain.service;

import com.filemanager.domain.dto.PipelineTaskStatusDTO;
import com.filemanager.domain.enums.TaskStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流水线任务管理服务
 */
public class PipelineTaskManager {
    private static PipelineTaskManager instance;
    private final Map<String, PipelineTaskStatusDTO> tasks = new ConcurrentHashMap<>();
    private volatile String currentTaskId;
    private volatile boolean isTaskRunning = false;
    
    private PipelineTaskManager() {
    }
    
    public static synchronized PipelineTaskManager getInstance() {
        if (instance == null) {
            instance = new PipelineTaskManager();
        }
        return instance;
    }
    
    public String createTask(String taskType) {
        String taskId = "task-" + System.currentTimeMillis();
        PipelineTaskStatusDTO status = new PipelineTaskStatusDTO();
        status.setTaskId(taskId);
        status.setStatus(TaskStatus.READY);
        status.setStartTime(System.currentTimeMillis());
        tasks.put(taskId, status);
        currentTaskId = taskId;
        return taskId;
    }
    
    public void updateTaskStatus(String taskId, TaskStatus status) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            TaskStatus currentStatus = taskStatus.getStatus();
            if (currentStatus.canTransitionTo(status)) {
                taskStatus.setStatus(status);
                if (status.isCompleted() || status.isFailed() || status == TaskStatus.CANCELLED) {
                    taskStatus.setEndTime(System.currentTimeMillis());
                    if (taskId.equals(currentTaskId)) {
                        isTaskRunning = false;
                    }
                }
            }
        }
    }
    
    public void updateTaskProgress(String taskId, int progress) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            taskStatus.setProgress(progress);
        }
    }
    
    public void updateTaskProgress(String taskId, int completedTasks, int totalTasks) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            taskStatus.setCompletedTasks(completedTasks);
            taskStatus.setTotalTasks(totalTasks);
            if (totalTasks > 0) {
                int progress = (int) ((double) completedTasks / totalTasks * 100);
                taskStatus.setProgress(progress);
            }
        }
    }
    
    public void updateTaskMessage(String taskId, String message) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            taskStatus.setMessage(message);
        }
    }
    
    public void updateTaskStep(String taskId, String currentStep) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            taskStatus.setCurrentStep(currentStep);
        }
    }
    
    public void updateTaskChanges(String taskId, boolean hasChanges, int changeCount) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            taskStatus.setHasChanges(hasChanges);
            taskStatus.setChangeCount(changeCount);
        }
    }
    
    public void updateTaskScanningInfo(String taskId, String currentDirectory, int scannedFiles, int totalFiles) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            taskStatus.setCurrentDirectory(currentDirectory);
            taskStatus.setScannedFiles(scannedFiles);
            taskStatus.setTotalFiles(totalFiles);
        }
    }
    
    public void updateTaskLogMessage(String taskId, String logMessage) {
        PipelineTaskStatusDTO taskStatus = tasks.get(taskId);
        if (taskStatus != null) {
            taskStatus.setLogMessage(logMessage);
        }
    }
    
    public PipelineTaskStatusDTO getTaskStatus(String taskId) {
        return tasks.get(taskId);
    }
    
    public PipelineTaskStatusDTO getCurrentTaskStatus() {
        if (currentTaskId != null) {
            return tasks.get(currentTaskId);
        }
        return null;
    }
    
    public void setCurrentTaskRunning(boolean running) {
        this.isTaskRunning = running;
    }
    
    public boolean isTaskRunning() {
        return isTaskRunning;
    }
    
    public void cancelCurrentTask() {
        if (currentTaskId != null) {
            updateTaskStatus(currentTaskId, TaskStatus.CANCELLED);
            isTaskRunning = false;
        }
    }
    
    public void clearTask(String taskId) {
        tasks.remove(taskId);
        if (taskId.equals(currentTaskId)) {
            currentTaskId = null;
        }
    }
    
    public void clearAllTasks() {
        tasks.clear();
        currentTaskId = null;
        isTaskRunning = false;
    }
}