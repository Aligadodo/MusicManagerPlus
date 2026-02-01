package com.filemanager.domain.dto;

import com.filemanager.domain.enums.TaskStatus;

/**
 * 任务状态DTO
 */
public class PipelineTaskStatusDTO {
    private String taskId;
    private TaskStatus status;
    private String statusDescription;
    private long startTime;
    private long endTime;
    private int progress;
    private String remainingTime;
    private int completedTasks;
    private int totalTasks;
    private String currentStep;
    private String message;
    private boolean hasChanges;
    private int changeCount;
    private String currentDirectory;
    private int scannedFiles;
    private int totalFiles;
    private String logMessage;
    
    public PipelineTaskStatusDTO() {
        this.status = TaskStatus.READY;
        this.statusDescription = TaskStatus.READY.getDescription();
        this.progress = 0;
        this.remainingTime = "00:00:00";
        this.completedTasks = 0;
        this.totalTasks = 0;
        this.hasChanges = false;
        this.changeCount = 0;
        this.scannedFiles = 0;
        this.totalFiles = 0;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
        this.statusDescription = status.getDescription();
    }
    
    public String getStatusDescription() {
        return statusDescription;
    }
    
    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public String getRemainingTime() {
        return remainingTime;
    }
    
    public void setRemainingTime(String remainingTime) {
        this.remainingTime = remainingTime;
    }
    
    public int getCompletedTasks() {
        return completedTasks;
    }
    
    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }
    
    public int getTotalTasks() {
        return totalTasks;
    }
    
    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }
    
    public String getCurrentStep() {
        return currentStep;
    }
    
    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public boolean isHasChanges() {
        return hasChanges;
    }
    
    public void setHasChanges(boolean hasChanges) {
        this.hasChanges = hasChanges;
    }
    
    public int getChangeCount() {
        return changeCount;
    }
    
    public void setChangeCount(int changeCount) {
        this.changeCount = changeCount;
    }
    
    public String getCurrentDirectory() {
        return currentDirectory;
    }
    
    public void setCurrentDirectory(String currentDirectory) {
        this.currentDirectory = currentDirectory;
    }
    
    public int getScannedFiles() {
        return scannedFiles;
    }
    
    public void setScannedFiles(int scannedFiles) {
        this.scannedFiles = scannedFiles;
    }
    
    public int getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public String getLogMessage() {
        return logMessage;
    }
    
    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }
}