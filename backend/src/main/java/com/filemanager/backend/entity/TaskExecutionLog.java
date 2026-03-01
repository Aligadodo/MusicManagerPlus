package com.filemanager.backend.entity;

import java.util.Date;

public class TaskExecutionLog {
    private Long id;
    private String taskId;
    private Long timestamp;
    private String logLevel;
    private String logType;
    private String message;
    private String details;
    private Date createdAt;

    public TaskExecutionLog() {
    }

    public TaskExecutionLog(String taskId, String logLevel, String logType, String message) {
        this.taskId = taskId;
        this.timestamp = System.currentTimeMillis();
        this.logLevel = logLevel;
        this.logType = logType;
        this.message = message;
        this.createdAt = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
