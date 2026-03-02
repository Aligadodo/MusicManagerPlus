package com.filemanager.backend.model;

import java.util.List;
import java.util.Map;

/**
 * 任务配置快照
 * 保存任务执行时的完整配置
 */
public class TaskConfigSnapshot {
    private List<SourceDirectoryConfig> sourceDirectories;
    private PipelineConfig pipelineConfig;
    private GlobalSettings globalSettings;

    public TaskConfigSnapshot() {
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

    /**
     * 源目录配置
     */
    public static class SourceDirectoryConfig {
        private String path;
        private int depth;
        private boolean recursive;
        private List<String> includePatterns;
        private List<String> excludePatterns;

        public SourceDirectoryConfig() {
        }

        public SourceDirectoryConfig(String path, int depth) {
            this.path = path;
            this.depth = depth;
            this.recursive = true;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public boolean isRecursive() {
            return recursive;
        }

        public void setRecursive(boolean recursive) {
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
     * 流水线配置
     */
    public static class PipelineConfig {
        private String pipelineId;
        private String name;
        private String description;
        private List<PipelineItem> items;

        public PipelineConfig() {
            this.items = new java.util.ArrayList<>();
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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<PipelineItem> getItems() {
            return items;
        }

        public void setItems(List<PipelineItem> items) {
            this.items = items;
        }
    }

    /**
     * 流水线项
     */
    public static class PipelineItem {
        private String strategyId;
        private String strategyName;
        private boolean enabled;
        private int order;
        private Map<String, Object> config;

        public PipelineItem() {
            this.enabled = true;
        }

        public String getStrategyId() {
            return strategyId;
        }

        public void setStrategyId(String strategyId) {
            this.strategyId = strategyId;
        }

        public String getStrategyName() {
            return strategyName;
        }

        public void setStrategyName(String strategyName) {
            this.strategyName = strategyName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public Map<String, Object> getConfig() {
            return config;
        }

        public void setConfig(Map<String, Object> config) {
            this.config = config;
        }
    }

    /**
     * 全局设置
     */
    public static class GlobalSettings {
        private int maxThreads;
        private long timeout;
        private boolean dryRun;
        private boolean overwrite;
        private boolean backup;
        private String backupPath;
        private int retryCount;
        private long retryInterval;
        private int previewThreads;
        private int executionThreads;
        private String threadPoolMode;
        private int minRecursionDepth;
        private int maxRecursionDepth;
        private int previewLimit;
        private int executionLimit;
        private boolean autoRefresh;
        private boolean autoExecute;

        public GlobalSettings() {
            this.maxThreads = 10;
            this.timeout = 300000L;
            this.dryRun = false;
            this.overwrite = false;
            this.backup = false;
            this.retryCount = 3;
            this.retryInterval = 1000L;
            this.previewThreads = 10;
            this.executionThreads = 4;
            this.threadPoolMode = "GLOBAL";
            this.minRecursionDepth = 1;
            this.maxRecursionDepth = 3;
            this.previewLimit = 200;
            this.executionLimit = 1000;
            this.autoRefresh = true;
            this.autoExecute = false;
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

        public boolean isOverwrite() {
            return overwrite;
        }

        public void setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
        }

        public boolean isBackup() {
            return backup;
        }

        public void setBackup(boolean backup) {
            this.backup = backup;
        }

        public String getBackupPath() {
            return backupPath;
        }

        public void setBackupPath(String backupPath) {
            this.backupPath = backupPath;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public long getRetryInterval() {
            return retryInterval;
        }

        public void setRetryInterval(long retryInterval) {
            this.retryInterval = retryInterval;
        }

        public int getPreviewThreads() {
            return previewThreads;
        }

        public void setPreviewThreads(int previewThreads) {
            this.previewThreads = previewThreads;
        }

        public int getExecutionThreads() {
            return executionThreads;
        }

        public void setExecutionThreads(int executionThreads) {
            this.executionThreads = executionThreads;
        }

        public String getThreadPoolMode() {
            return threadPoolMode;
        }

        public void setThreadPoolMode(String threadPoolMode) {
            this.threadPoolMode = threadPoolMode;
        }

        public int getMinRecursionDepth() {
            return minRecursionDepth;
        }

        public void setMinRecursionDepth(int minRecursionDepth) {
            this.minRecursionDepth = minRecursionDepth;
        }

        public int getMaxRecursionDepth() {
            return maxRecursionDepth;
        }

        public void setMaxRecursionDepth(int maxRecursionDepth) {
            this.maxRecursionDepth = maxRecursionDepth;
        }

        public int getPreviewLimit() {
            return previewLimit;
        }

        public void setPreviewLimit(int previewLimit) {
            this.previewLimit = previewLimit;
        }

        public int getExecutionLimit() {
            return executionLimit;
        }

        public void setExecutionLimit(int executionLimit) {
            this.executionLimit = executionLimit;
        }

        public boolean isAutoRefresh() {
            return autoRefresh;
        }

        public void setAutoRefresh(boolean autoRefresh) {
            this.autoRefresh = autoRefresh;
        }

        public boolean isAutoExecute() {
            return autoExecute;
        }

        public void setAutoExecute(boolean autoExecute) {
            this.autoExecute = autoExecute;
        }
    }
}
