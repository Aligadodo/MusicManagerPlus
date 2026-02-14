package com.filemanager.backend.model;

import com.filemanager.domain.entity.ChangeRecord;
import java.util.List;
import java.util.Map;

/**
 * 预览结果模型
 * 保存预览任务的执行结果，包含所有变更记录和统计信息
 */
public class PreviewResult {
    private String taskId;
    private List<ChangeRecord> changeRecords;
    private PreviewStatistics statistics;
    private long createdAt;
    private long completedAt;

    public PreviewResult() {
    }

    public PreviewResult(String taskId) {
        this.taskId = taskId;
        this.createdAt = System.currentTimeMillis();
        this.statistics = new PreviewStatistics();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public List<ChangeRecord> getChangeRecords() {
        return changeRecords;
    }

    public void setChangeRecords(List<ChangeRecord> changeRecords) {
        this.changeRecords = changeRecords;
    }

    public PreviewStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(PreviewStatistics statistics) {
        this.statistics = statistics;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * 预览统计信息
     */
    public static class PreviewStatistics {
        private int totalFiles;
        private int processedFiles;
        private int changedFiles;
        private int unchangedFiles;
        private Map<String, Integer> operationStats;

        public PreviewStatistics() {
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public void setTotalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
        }

        public int getProcessedFiles() {
            return processedFiles;
        }

        public void setProcessedFiles(int processedFiles) {
            this.processedFiles = processedFiles;
        }

        public int getChangedFiles() {
            return changedFiles;
        }

        public void setChangedFiles(int changedFiles) {
            this.changedFiles = changedFiles;
        }

        public int getUnchangedFiles() {
            return unchangedFiles;
        }

        public void setUnchangedFiles(int unchangedFiles) {
            this.unchangedFiles = unchangedFiles;
        }

        public Map<String, Integer> getOperationStats() {
            return operationStats;
        }

        public void setOperationStats(Map<String, Integer> operationStats) {
            this.operationStats = operationStats;
        }
    }
}
