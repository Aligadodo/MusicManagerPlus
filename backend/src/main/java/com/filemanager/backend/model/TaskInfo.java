package com.filemanager.backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.filemanager.domain.entity.ChangeRecord;
import java.util.List;
import java.util.Map;

/**
 * 任务信息模型
 * 包含任务的基本信息和各阶段的状态
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskInfo {
    @JsonProperty("taskId")
    private String taskId;
    @JsonProperty("taskName")
    private String taskName;
    @JsonProperty("createdAt")
    private long createdAt;
    @JsonProperty("updatedAt")
    private long updatedAt;
    @JsonProperty("currentStage")
    private String currentStage;
    @JsonProperty("status")
    private TaskStatus status;
    @JsonProperty("overallProgress")
    private double overallProgress;
    @JsonProperty("message")
    private String message;
    @JsonProperty("configSnapshot")
    private TaskConfigSnapshot configSnapshot;
    @JsonProperty("stages")
    private TaskStages stages;
    @JsonProperty("changeRecords")
    private List<ChangeRecord> changeRecords;

    public TaskInfo() {
    }

    public TaskInfo(String taskId) {
        this.taskId = taskId;
        this.createdAt = System.currentTimeMillis();
        this.status = TaskStatus.CREATED;
        this.currentStage = "CREATED";
        this.overallProgress = 0.0;
        this.message = "任务已创建";
        this.stages = new TaskStages();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public double getOverallProgress() {
        return overallProgress;
    }

    public void setOverallProgress(double overallProgress) {
        this.overallProgress = overallProgress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TaskConfigSnapshot getConfigSnapshot() {
        return configSnapshot;
    }

    public void setConfigSnapshot(TaskConfigSnapshot configSnapshot) {
        this.configSnapshot = configSnapshot;
    }

    public TaskStages getStages() {
        return stages;
    }

    public void setStages(TaskStages stages) {
        this.stages = stages;
    }

    public List<ChangeRecord> getChangeRecords() {
        return changeRecords;
    }

    public void setChangeRecords(List<ChangeRecord> changeRecords) {
        this.changeRecords = changeRecords;
    }

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        CREATED("已创建"),
        SCANNING("正在扫描"),
        SCANNED("扫描完成"),
        PREVIEWING("正在预览"),
        PREVIEWED("预览完成"),
        EXECUTING("正在执行"),
        COMPLETED("执行完成"),
        FAILED("执行失败"),
        CANCELLED("已取消");

        private final String description;

        TaskStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 任务各阶段信息
     */
    public static class TaskStages {
        private ScanStage scan;
        private PreviewStage preview;
        private ExecutionStage execution;

        public TaskStages() {
            this.scan = new ScanStage();
            this.preview = new PreviewStage();
            this.execution = new ExecutionStage();
        }

        public ScanStage getScan() {
            return scan;
        }

        public void setScan(ScanStage scan) {
            this.scan = scan;
        }

        public PreviewStage getPreview() {
            return preview;
        }

        public void setPreview(PreviewStage preview) {
            this.preview = preview;
        }

        public ExecutionStage getExecution() {
            return execution;
        }

        public void setExecution(ExecutionStage execution) {
            this.execution = execution;
        }
    }

    /**
     * 扫描阶段
     */
    public static class ScanStage {
        private String status;
        private int totalFiles;
        private long totalSize;
        private long scanStartTime;
        private long scanEndTime;
        private long scanDuration;
        private Map<String, Integer> fileTypeStats;

        public ScanStage() {
            this.status = "PENDING";
            this.fileTypeStats = new java.util.HashMap<>();
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public void setTotalFiles(int totalFiles) {
            this.totalFiles = totalFiles;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public long getScanStartTime() {
            return scanStartTime;
        }

        public void setScanStartTime(long scanStartTime) {
            this.scanStartTime = scanStartTime;
        }

        public long getScanEndTime() {
            return scanEndTime;
        }

        public void setScanEndTime(long scanEndTime) {
            this.scanEndTime = scanEndTime;
        }

        public long getScanDuration() {
            return scanDuration;
        }

        public void setScanDuration(long scanDuration) {
            this.scanDuration = scanDuration;
        }

        public Map<String, Integer> getFileTypeStats() {
            return fileTypeStats;
        }

        public void setFileTypeStats(Map<String, Integer> fileTypeStats) {
            this.fileTypeStats = fileTypeStats;
        }
    }

    /**
     * 预览阶段
     */
    public static class PreviewStage {
        private String status;
        private int totalFiles;
        private int processedFiles;
        private int changedFiles;
        private int unchangedFiles;
        private long previewStartTime;
        private long previewEndTime;
        private long previewDuration;
        private Map<String, Integer> operationStats;

        public PreviewStage() {
            this.status = "PENDING";
            this.operationStats = new java.util.HashMap<>();
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
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

        public long getPreviewStartTime() {
            return previewStartTime;
        }

        public void setPreviewStartTime(long previewStartTime) {
            this.previewStartTime = previewStartTime;
        }

        public long getPreviewEndTime() {
            return previewEndTime;
        }

        public void setPreviewEndTime(long previewEndTime) {
            this.previewEndTime = previewEndTime;
        }

        public long getPreviewDuration() {
            return previewDuration;
        }

        public void setPreviewDuration(long previewDuration) {
            this.previewDuration = previewDuration;
        }

        public Map<String, Integer> getOperationStats() {
            return operationStats;
        }

        public void setOperationStats(Map<String, Integer> operationStats) {
            this.operationStats = operationStats;
        }
    }

    /**
     * 执行阶段
     */
    public static class ExecutionStage {
        private String status;
        private int executionCount;
        private String currentExecution;
        private int totalFiles;
        private int processedFiles;
        private int successCount;
        private int failedCount;
        private int skippedCount;
        private long executionStartTime;
        private long executionEndTime;
        private long executionDuration;
        private Map<String, OperationStats> operationStats;

        public ExecutionStage() {
            this.status = "PENDING";
            this.operationStats = new java.util.HashMap<>();
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getExecutionCount() {
            return executionCount;
        }

        public void setExecutionCount(int executionCount) {
            this.executionCount = executionCount;
        }

        public String getCurrentExecution() {
            return currentExecution;
        }

        public void setCurrentExecution(String currentExecution) {
            this.currentExecution = currentExecution;
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

        public long getExecutionStartTime() {
            return executionStartTime;
        }

        public void setExecutionStartTime(long executionStartTime) {
            this.executionStartTime = executionStartTime;
        }

        public long getExecutionEndTime() {
            return executionEndTime;
        }

        public void setExecutionEndTime(long executionEndTime) {
            this.executionEndTime = executionEndTime;
        }

        public long getExecutionDuration() {
            return executionDuration;
        }

        public void setExecutionDuration(long executionDuration) {
            this.executionDuration = executionDuration;
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
