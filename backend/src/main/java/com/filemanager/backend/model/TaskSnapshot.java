package com.filemanager.backend.model;

import java.util.List;
import java.util.Map;

/**
 * 任务快照模型
 * 保存任务执行时的配置快照，确保任务执行不受用户修改配置影响
 */
public class TaskSnapshot {
    private String taskId;
    private TaskType taskType;
    private long createdAt;
    private ConfigSnapshot configSnapshot;
    private String status;
    private double progress;
    private String message;
    private long startedAt;
    private long completedAt;

    public TaskSnapshot() {
    }

    public TaskSnapshot(String taskId, TaskType taskType) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.createdAt = System.currentTimeMillis();
        this.status = "CREATED";
        this.progress = 0.0;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public ConfigSnapshot getConfigSnapshot() {
        return configSnapshot;
    }

    public void setConfigSnapshot(ConfigSnapshot configSnapshot) {
        this.configSnapshot = configSnapshot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        PREVIEW("预览任务"),
        EXECUTE("执行任务");

        private final String description;

        TaskType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 配置快照
     */
    public static class ConfigSnapshot {
        private List<SourceDirectoryConfig> sourceDirectories;
        private PipelineConfig pipelineConfig;
        private GlobalSettings globalSettings;

        public ConfigSnapshot() {
        }

        public List<SourceDirectoryConfig> getSourceDirectories() {
            return sourceDirectories;
        }

        public void setSourceDirectories(List<SourceDirectoryConfig> sourceDirectories) {
            this.sourceDirectories = sourceDirectories;
        }

        public PipelineConfig getPipelineConfig() {
            return pipelineConfig;
        }

        public void setPipelineConfig(PipelineConfig pipelineConfig) {
            this.pipelineConfig = pipelineConfig;
        }

        public GlobalSettings getGlobalSettings() {
            return globalSettings;
        }

        public void setGlobalSettings(GlobalSettings globalSettings) {
            this.globalSettings = globalSettings;
        }
    }

    /**
     * 源目录配置
     */
    public static class SourceDirectoryConfig {
        private String path;
        private int threadCount;

        public SourceDirectoryConfig() {
        }

        public SourceDirectoryConfig(String path, int threadCount) {
            this.path = path;
            this.threadCount = threadCount;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getThreadCount() {
            return threadCount;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }
    }

    /**
     * 流水线配置
     */
    public static class PipelineConfig {
        private String pipelineId;
        private String name;
        private List<PipelineItem> items;

        public PipelineConfig() {
        }

        public String getPipelineId() {
            return pipelineId;
        }

        public void setPipelineId(String pipelineId) {
            this.pipelineId = pipelineId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<PipelineItem> getItems() {
            return items;
        }

        public void setItems(List<PipelineItem> items) {
            this.items = items;
        }
    }

    /**
     * 流水线项目配置
     */
    public static class PipelineItem {
        private String pluginId;
        private boolean enabled;
        private Map<String, Object> config;
        private List<Object> preconditionGroups;

        public PipelineItem() {
        }

        public String getPluginId() {
            return pluginId;
        }

        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }

        public List<Object> getPreconditionGroups() {
            return preconditionGroups;
        }

        public void setPreconditionGroups(List<Object> preconditionGroups) {
            this.preconditionGroups = preconditionGroups;
        }
    }

    /**
     * 全局设置
     */
    public static class GlobalSettings {
        private int maxThreads;
        private long timeout;
        private boolean dryRun;

        public GlobalSettings() {
        }

        public int getMaxThreads() {
            return maxThreads;
        }

        public void setMaxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }
    }
}
