package com.filemanager.domain.dto;

import java.util.List;

public class TaskRequestDTO {
    private String strategyId;
    private List<String> filePaths;
    private StrategyConfigDTO strategyConfig;
    private String taskName;
    private String description;

    public TaskRequestDTO() {
    }

    public TaskRequestDTO(String strategyId, List<String> filePaths, StrategyConfigDTO strategyConfig, String taskName, String description) {
        this.strategyId = strategyId;
        this.filePaths = filePaths;
        this.strategyConfig = strategyConfig;
        this.taskName = taskName;
        this.description = description;
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

    public StrategyConfigDTO getStrategyConfig() {
        return strategyConfig;
    }

    public void setStrategyConfig(StrategyConfigDTO strategyConfig) {
        this.strategyConfig = strategyConfig;
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
