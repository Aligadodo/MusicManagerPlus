package com.filemanager.domain.dto;

import java.util.List;
import java.util.Map;

/**
 * 任务请求DTO
 */
public class TaskRequestDTO {
    private String taskName;
    private String description;
    private List<SourceDirectoryDTO> sourceDirectories;
    private String pipelineId;
    private GlobalSettingsDTO globalSettings;

    public TaskRequestDTO() {
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

    public List<SourceDirectoryDTO> getSourceDirectories() {
        return sourceDirectories;
    }

    public void setSourceDirectories(List<SourceDirectoryDTO> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(String pipelineId) {
        this.pipelineId = pipelineId;
    }

    public GlobalSettingsDTO getGlobalSettings() {
        return globalSettings;
    }

    public void setGlobalSettings(GlobalSettingsDTO globalSettings) {
        this.globalSettings = globalSettings;
    }

    /**
     * 源目录DTO
     */
    public static class SourceDirectoryDTO {
        private String path;
        private Integer depth;
        private Boolean recursive;
        private List<String> includePatterns;
        private List<String> excludePatterns;

        public SourceDirectoryDTO() {
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Integer getDepth() {
            return depth;
        }

        public void setDepth(Integer depth) {
            this.depth = depth;
        }

        public Boolean isRecursive() {
            return recursive;
        }

        public void setRecursive(Boolean recursive) {
            this.recursive = recursive;
        }

        public List<String> getIncludePatterns() {
            return includePatterns;
        }

        public void setIncludePatterns(List<String> includePatterns) {
            this.includePatterns = includePatterns;
        }

        public List<String> getExcludePatterns() {
            return excludePatterns;
        }

        public void setExcludePatterns(List<String> excludePatterns) {
            this.excludePatterns = excludePatterns;
        }
    }

    /**
     * 全局设置DTO
     */
    public static class GlobalSettingsDTO {
        private Integer maxThreads;
        private Long timeout;
        private Boolean dryRun;
        private Boolean overwrite;
        private Boolean backup;
        private String backupPath;
        private Integer retryCount;
        private Long retryInterval;

        public GlobalSettingsDTO() {
        }

        public Integer getMaxThreads() {
            return maxThreads;
        }

        public void setMaxThreads(Integer maxThreads) {
            this.maxThreads = maxThreads;
        }

        public Long getTimeout() {
            return timeout;
        }

        public void setTimeout(Long timeout) {
            this.timeout = timeout;
        }

        public Boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(Boolean dryRun) {
            this.dryRun = dryRun;
        }

        public Boolean isOverwrite() {
            return overwrite;
        }

        public void setOverwrite(Boolean overwrite) {
            this.overwrite = overwrite;
        }

        public Boolean isBackup() {
            return backup;
        }

        public void setBackup(Boolean backup) {
            this.backup = backup;
        }

        public String getBackupPath() {
            return backupPath;
        }

        public void setBackupPath(String backupPath) {
            this.backupPath = backupPath;
        }

        public Integer getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(Integer retryCount) {
            this.retryCount = retryCount;
        }

        public Long getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(Long retryInterval) {
            this.retryInterval = retryInterval;
        }
    }
}
