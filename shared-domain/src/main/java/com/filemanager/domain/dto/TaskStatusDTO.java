package com.filemanager.domain.dto;

import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public class TaskStatusDTO {
    private String taskId;
    private TaskStatus status;
    private double progress;
    private String message;
    private long startTime;
    private Long endTime;
    private List<ChangeRecord> changes;

    public enum TaskStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED;

        public boolean isFinalState() {
            return this == SUCCESS || this == FAILED || this == CANCELLED;
        }
    }

    public TaskStatusDTO() {
    }

    public TaskStatusDTO(String taskId, TaskStatus status, double progress, String message, long startTime, Long endTime, List<ChangeRecord> changes) {
        this.taskId = taskId;
        this.status = status;
        this.progress = progress;
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
        this.changes = changes;
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
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public List<ChangeRecord> getChanges() {
        return changes;
    }

    public void setChanges(List<ChangeRecord> changes) {
        this.changes = changes;
    }
}
