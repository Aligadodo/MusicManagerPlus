package com.filemanager.domain.dto;

import java.util.List;

/**
 * 任务请求DTO
 */
public class TaskRequestDTO {
    private String strategyId;     // 策略ID
    private List<String> filePaths; // 文件路径列表
    private String taskName;        // 任务名称
    private String description;     // 任务描述

    public TaskRequestDTO() {
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
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
}
