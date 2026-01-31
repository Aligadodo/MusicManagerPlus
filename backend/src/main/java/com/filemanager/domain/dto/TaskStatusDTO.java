package com.filemanager.domain.dto;

/**
 * 任务状态DTO
 */
public class TaskStatusDTO {
    private String taskId;          // 任务ID
    private String status;          // 任务状态：PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    private String taskName;        // 任务名称
    private String description;     // 任务描述
    private long startTime;         // 开始时间
    private long endTime;           // 结束时间
    private int progress;           // 进度（0-100）
    private String message;         // 状态消息

    public TaskStatusDTO() {
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
