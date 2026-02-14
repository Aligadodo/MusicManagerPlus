package com.filemanager.backend.service;

import com.filemanager.backend.model.TaskInfo;
import com.filemanager.backend.logging.UnifiedLogger;
import com.filemanager.domain.dto.PipelineTaskStatusDTO;
import com.filemanager.domain.enums.TaskStatus;
import com.filemanager.domain.service.PipelineTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 任务注册中心
 * 统一管理所有任务的注册、状态和生命周期
 */
@Service
public class TaskRegistry {
    
    @Autowired
    private OptimizedTaskStorageService storageService;
    
    private final Map<String, TaskInfo> registeredTasks = new ConcurrentHashMap<>();
    private final Map<String, PipelineTaskStatusDTO> runningTasks = new ConcurrentHashMap<>();
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    
    private static final String TASK_BASE_DIR = "tasks";
    
    public TaskRegistry() {
    }
    
    private PipelineTaskManager getPipelineTaskManager() {
        return PipelineTaskManager.getInstance();
    }
    
    /**
     * 注册新任务
     */
    public String registerTask(TaskInfo taskInfo) {
        if (taskInfo.getTaskId() == null || taskInfo.getTaskId().isEmpty()) {
            taskInfo.setTaskId("task-" + System.currentTimeMillis() + "-" + taskCounter.incrementAndGet());
        }
        
        registeredTasks.put(taskInfo.getTaskId(), taskInfo);
        UnifiedLogger.backendOperation("TaskRegistry", "任务已注册: " + taskInfo.getTaskId());
        
        return taskInfo.getTaskId();
    }
    
    /**
     * 获取任务信息
     */
    public TaskInfo getTask(String taskId) {
        TaskInfo task = registeredTasks.get(taskId);
        if (task == null) {
            try {
                task = storageService.loadTaskInfo(taskId);
                if (task != null) {
                    registeredTasks.put(taskId, task);
                }
            } catch (Exception e) {
                UnifiedLogger.backendError("TaskRegistry", "加载任务信息失败: " + taskId, e);
            }
        }
        return task;
    }
    
    /**
     * 获取所有任务
     */
    public List<TaskInfo> getAllTasks() {
        Map<String, TaskInfo> allTasks = new HashMap<>(registeredTasks);
        
        try {
            List<String> taskIds = storageService.getAllTaskIds();
            for (String taskId : taskIds) {
                if (!allTasks.containsKey(taskId)) {
                    TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
                    if (taskInfo != null) {
                        allTasks.put(taskId, taskInfo);
                        registeredTasks.put(taskId, taskInfo);
                    }
                }
            }
        } catch (Exception e) {
            UnifiedLogger.backendError("TaskRegistry", "加载所有任务失败", e);
        }
        
        return allTasks.values().stream()
                .sorted(Comparator.comparing(TaskInfo::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * 根据状态筛选任务
     */
    public List<TaskInfo> getTasksByStatus(TaskInfo.TaskStatus status) {
        return getAllTasks().stream()
                .filter(task -> task.getStatus() == status)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取运行中的任务
     */
    public List<TaskInfo> getRunningTasks() {
        return getAllTasks().stream()
                .filter(task -> task.getStatus() == TaskInfo.TaskStatus.SCANNING || 
                               task.getStatus() == TaskInfo.TaskStatus.PREVIEWING || 
                               task.getStatus() == TaskInfo.TaskStatus.EXECUTING)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, TaskInfo.TaskStatus status) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setStatus(status);
            task.setUpdatedAt(System.currentTimeMillis());
            saveTask(task);
            
            PipelineTaskStatusDTO pipelineTask = runningTasks.get(taskId);
            if (pipelineTask != null) {
                com.filemanager.domain.enums.TaskStatus domainStatus = convertToDomainStatus(status);
                getPipelineTaskManager().updateTaskStatus(taskId, domainStatus);
            }
            
            UnifiedLogger.backendOperation("TaskRegistry", "任务状态更新: " + taskId + " -> " + status);
        }
    }
    
    /**
     * 转换 TaskInfo.TaskStatus 到 TaskStatus
     */
    private com.filemanager.domain.enums.TaskStatus convertToDomainStatus(TaskInfo.TaskStatus status) {
        switch (status) {
            case SCANNING:
                return com.filemanager.domain.enums.TaskStatus.PREVIEWING;
            case PREVIEWING:
                return com.filemanager.domain.enums.TaskStatus.PREVIEWING;
            case EXECUTING:
                return com.filemanager.domain.enums.TaskStatus.EXECUTING;
            case COMPLETED:
                return com.filemanager.domain.enums.TaskStatus.EXECUTION_COMPLETED;
            case FAILED:
                return com.filemanager.domain.enums.TaskStatus.EXECUTION_FAILED;
            case CANCELLED:
                return com.filemanager.domain.enums.TaskStatus.CANCELLED;
            default:
                return com.filemanager.domain.enums.TaskStatus.READY;
        }
    }
    
    /**
     * 更新任务进度
     */
    public void updateTaskProgress(String taskId, double progress) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setOverallProgress(progress);
            task.setUpdatedAt(System.currentTimeMillis());
            saveTask(task);
        }
    }
    
    /**
     * 更新任务消息
     */
    public void updateTaskMessage(String taskId, String message) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setMessage(message);
            task.setUpdatedAt(System.currentTimeMillis());
            saveTask(task);
            
            if (runningTasks.containsKey(taskId)) {
                getPipelineTaskManager().updateTaskMessage(taskId, message);
            }
        }
    }
    
    /**
     * 更新任务当前阶段
     */
    public void updateTaskStage(String taskId, String stage) {
        TaskInfo task = getTask(taskId);
        if (task != null) {
            task.setCurrentStage(stage);
            task.setUpdatedAt(System.currentTimeMillis());
            saveTask(task);
        }
    }
    
    /**
     * 注册运行中的任务
     */
    public void registerRunningTask(String taskId, PipelineTaskStatusDTO pipelineTask) {
        runningTasks.put(taskId, pipelineTask);
        getPipelineTaskManager().createTaskWithId(taskId, "registered");
        UnifiedLogger.backendOperation("TaskRegistry", "运行中任务已注册: " + taskId);
    }
    
    /**
     * 取消运行中的任务
     */
    public void cancelRunningTask(String taskId) {
        PipelineTaskStatusDTO pipelineTask = runningTasks.get(taskId);
        if (pipelineTask != null) {
            getPipelineTaskManager().updateTaskStatus(taskId, com.filemanager.domain.enums.TaskStatus.CANCELLED);
            runningTasks.remove(taskId);
            updateTaskStatus(taskId, TaskInfo.TaskStatus.CANCELLED);
            UnifiedLogger.backendOperation("TaskRegistry", "任务已取消: " + taskId);
        }
    }
    
    /**
     * 完成运行中的任务
     */
    public void completeRunningTask(String taskId) {
        PipelineTaskStatusDTO pipelineTask = runningTasks.get(taskId);
        if (pipelineTask != null) {
            runningTasks.remove(taskId);
            UnifiedLogger.backendOperation("TaskRegistry", "任务已完成: " + taskId);
        }
    }
    
    /**
     * 重新执行任务
     */
    public boolean restartTask(String taskId, String fromStage) {
        TaskInfo task = getTask(taskId);
        if (task == null) {
            UnifiedLogger.backendError("TaskRegistry", "任务不存在: " + taskId, null);
            return false;
        }
        
        if (task.getStatus() == TaskInfo.TaskStatus.SCANNING || 
            task.getStatus() == TaskInfo.TaskStatus.PREVIEWING || 
            task.getStatus() == TaskInfo.TaskStatus.EXECUTING) {
            UnifiedLogger.backendError("TaskRegistry", "任务正在运行中，无法重新执行: " + taskId, null);
            return false;
        }
        
        try {
            task.setStatus(TaskInfo.TaskStatus.SCANNED);
            task.setCurrentStage(fromStage);
            task.setOverallProgress(0.0);
            task.setMessage("准备重新执行");
            task.setUpdatedAt(System.currentTimeMillis());
            saveTask(task);
            
            UnifiedLogger.backendOperation("TaskRegistry", "任务已准备重新执行: " + taskId + " 从阶段: " + fromStage);
            return true;
        } catch (Exception e) {
            UnifiedLogger.backendError("TaskRegistry", "重新执行任务失败: " + taskId, e);
            return false;
        }
    }
    
    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        TaskInfo task = getTask(taskId);
        if (task == null) {
            UnifiedLogger.backendError("TaskRegistry", "任务不存在: " + taskId, null);
            return false;
        }
        
        if (task.getStatus() == TaskInfo.TaskStatus.SCANNING || 
            task.getStatus() == TaskInfo.TaskStatus.PREVIEWING || 
            task.getStatus() == TaskInfo.TaskStatus.EXECUTING) {
            UnifiedLogger.backendError("TaskRegistry", "任务正在运行中，无法删除: " + taskId, null);
            return false;
        }
        
        try {
            registeredTasks.remove(taskId);
            runningTasks.remove(taskId);
            
            storageService.deleteTask(taskId);
            
            deleteTaskDirectory(taskId);
            
            UnifiedLogger.backendOperation("TaskRegistry", "任务已删除: " + taskId);
            return true;
        } catch (Exception e) {
            UnifiedLogger.backendError("TaskRegistry", "删除任务失败: " + taskId, e);
            return false;
        }
    }
    
    /**
     * 删除任务目录
     */
    private void deleteTaskDirectory(String taskId) {
        try {
            Path taskDir = Paths.get(TASK_BASE_DIR, taskId);
            if (Files.exists(taskDir)) {
                Files.walk(taskDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                UnifiedLogger.backendOperation("TaskRegistry", "任务目录已删除: " + taskDir);
            }
        } catch (IOException e) {
            UnifiedLogger.backendError("TaskRegistry", "删除任务目录失败: " + taskId, e);
        }
    }
    
    /**
     * 获取任务统计信息
     */
    public Map<String, Object> getTaskStatistics() {
        List<TaskInfo> allTasks = getAllTasks();
        
        Map<TaskInfo.TaskStatus, Long> statusCount = allTasks.stream()
                .collect(Collectors.groupingBy(TaskInfo::getStatus, Collectors.counting()));
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalTasks", allTasks.size());
        statistics.put("runningTasks", runningTasks.size());
        statistics.put("statusCount", statusCount);
        statistics.put("recentTasks", allTasks.stream().limit(10).map(TaskInfo::getTaskId).collect(Collectors.toList()));
        
        return statistics;
    }
    
    /**
     * 获取运行中任务的实时状态
     */
    public Map<String, Object> getRunningTasksStatus() {
        Map<String, Object> status = new HashMap<>();
        
        for (Map.Entry<String, PipelineTaskStatusDTO> entry : runningTasks.entrySet()) {
            String taskId = entry.getKey();
            PipelineTaskStatusDTO pipelineTask = entry.getValue();
            TaskInfo taskInfo = getTask(taskId);
            
            Map<String, Object> taskStatus = new HashMap<>();
            taskStatus.put("taskId", taskId);
            taskStatus.put("status", taskInfo != null ? taskInfo.getStatus() : TaskInfo.TaskStatus.CREATED);
            taskStatus.put("currentStage", taskInfo != null ? taskInfo.getCurrentStage() : "UNKNOWN");
            taskStatus.put("progress", taskInfo != null ? taskInfo.getOverallProgress() : 0.0);
            taskStatus.put("message", taskInfo != null ? taskInfo.getMessage() : "");
            taskStatus.put("pipelineStatus", pipelineTask.getStatus());
            taskStatus.put("pipelineProgress", pipelineTask.getProgress());
            taskStatus.put("currentStep", pipelineTask.getCurrentStep());
            
            status.put(taskId, taskStatus);
        }
        
        return status;
    }
    
    /**
     * 保存任务信息
     */
    private void saveTask(TaskInfo task) {
        try {
            storageService.saveTaskInfo(task);
        } catch (Exception e) {
            UnifiedLogger.backendError("TaskRegistry", "保存任务信息失败: " + task.getTaskId(), e);
        }
    }
    
    /**
     * 清理已完成任务
     */
    public void cleanupCompletedTasks(int daysToKeep) {
        long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60L * 60L * 1000L);
        
        List<TaskInfo> allTasks = getAllTasks();
        List<String> tasksToDelete = allTasks.stream()
                .filter(task -> task.getUpdatedAt() < cutoffTime)
                .filter(task -> task.getStatus() == TaskInfo.TaskStatus.COMPLETED || 
                               task.getStatus() == TaskInfo.TaskStatus.FAILED || 
                               task.getStatus() == TaskInfo.TaskStatus.CANCELLED)
                .map(TaskInfo::getTaskId)
                .collect(Collectors.toList());
        
        for (String taskId : tasksToDelete) {
            deleteTask(taskId);
        }
        
        UnifiedLogger.backendOperation("TaskRegistry", "清理了 " + tasksToDelete.size() + " 个过期任务");
    }
}