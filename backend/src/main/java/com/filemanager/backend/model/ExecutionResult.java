package com.filemanager.backend.model;

import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import java.util.List;
import java.util.Map;

/**
 * 执行结果模型
 * 保存执行任务的执行结果，包含详细的执行信息和统计数据
 */
public class ExecutionResult {
    private String taskId;
    private String previewTaskId;
    private List<ExecutionRecord> executionRecords;
    private ExecutionStatistics statistics;
    private long createdAt;
    private long startedAt;
    private long completedAt;
    private long duration;

    public ExecutionResult() {
    }

    public ExecutionResult(String taskId, String previewTaskId) {
        this.taskId = taskId;
        this.previewTaskId = previewTaskId;
        this.createdAt = System.currentTimeMillis();
        this.statistics = new ExecutionStatistics();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getPreviewTaskId() {
        return previewTaskId;
    }

    public void setPreviewTaskId(String previewTaskId) {
        this.previewTaskId = previewTaskId;
    }

    public List<ExecutionRecord> getExecutionRecords() {
        return executionRecords;
    }

    public void setExecutionRecords(List<ExecutionRecord> executionRecords) {
        this.executionRecords = executionRecords;
    }

    public ExecutionStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ExecutionStatistics statistics) {
        this.statistics = statistics;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * 执行记录
     * 扩展ChangeRecord，增加执行相关的信息
     */
    public static class ExecutionRecord extends ChangeRecord {
        private long executionTime;
        private String errorMessage;
        private int retryCount;

        public ExecutionRecord() {
        }

        public ExecutionRecord(ChangeRecord changeRecord) {
            super(changeRecord.getOriginalName(), 
                  changeRecord.getNewName(), 
                  changeRecord.getFileHandle(), 
                  changeRecord.isChanged(), 
                  changeRecord.getNewPath(), 
                  changeRecord.getOperationTypeEnum(), 
                  changeRecord.getExtraParams(), 
                  changeRecord.getStatusEnum());
            
            this.setFilePath(changeRecord.getFilePath());
            this.setReason(changeRecord.getReason());
            this.setAnalyzeTime(changeRecord.getAnalyzeTime());
            this.setExecuteTime(changeRecord.getExecuteTime());
            this.setFailReason(changeRecord.getFailReason());
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public void setExecutionTime(long executionTime) {
            this.executionTime = executionTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }
    }

    /**
     * 执行统计信息
     */
    public static class ExecutionStatistics {
        private int totalFiles;
        private int processedFiles;
        private int successCount;
        private int failedCount;
        private int skippedCount;
        private Map<String, OperationStats> operationStats;

        public ExecutionStatistics() {
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

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(int failedCount) {
            this.failedCount = failedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public void setSkippedCount(int skippedCount) {
            this.skippedCount = skippedCount;
        }

        public Map<String, OperationStats> getOperationStats() {
            return operationStats;
        }

        public void setOperationStats(Map<String, OperationStats> operationStats) {
            this.operationStats = operationStats;
        }
    }

    /**
     * 操作统计
     */
    public static class OperationStats {
        private int success;
        private int failed;
        private int skipped;

        public OperationStats() {
        }

        public OperationStats(int success, int failed, int skipped) {
            this.success = success;
            this.failed = failed;
            this.skipped = skipped;
        }

        public int getSuccess() {
            return success;
        }

        public void setSuccess(int success) {
            this.success = success;
        }

        public int getFailed() {
            return failed;
        }

        public void setFailed(int failed) {
            this.failed = failed;
        }

        public int getSkipped() {
            return skipped;
        }

        public void setSkipped(int skipped) {
            this.skipped = skipped;
        }
    }
}
