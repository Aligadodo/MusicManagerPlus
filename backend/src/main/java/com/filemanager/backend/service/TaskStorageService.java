package com.filemanager.backend.service;

import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.backend.storage.ITaskStorage;
import com.filemanager.domain.entity.ChangeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskStorageService {

    private static final Logger logger = LoggerFactory.getLogger(TaskStorageService.class);

    private final ITaskStorage storage;
    private final ConfigSnapshotService configSnapshotService;

    @Autowired
    public TaskStorageService(ITaskStorage storage, ConfigSnapshotService configSnapshotService) {
        this.storage = storage;
        this.configSnapshotService = configSnapshotService;
        logger.info("[TaskStorage] TaskStorageService初始化完成");
    }

    public void initializeTaskDirectory(String taskId) {
        storage.initializeTaskDirectory(taskId);
    }

    public String getTaskDirectory(String taskId) {
        return storage.getTaskDirectory(taskId);
    }

    public void saveTaskInfo(TaskInfo taskInfo) {
        storage.saveTaskInfo(taskInfo);
    }

    public TaskInfo loadTaskInfo(String taskId) {
        TaskInfo taskInfo = storage.loadTaskInfo(taskId);
        
        if (taskInfo != null && taskInfo.getConfigSnapshotId() != null && !taskInfo.getConfigSnapshotId().isEmpty()) {
            TaskConfigSnapshot configSnapshot = configSnapshotService.getSnapshot(taskInfo.getConfigSnapshotId());
            if (configSnapshot != null) {
                taskInfo.setConfigSnapshot(configSnapshot);
            }
        }
        
        return taskInfo;
    }

    public void saveConfigSnapshot(String taskId, TaskConfigSnapshot configSnapshot) {
        storage.saveConfigSnapshot(taskId, configSnapshot);
    }

    public TaskConfigSnapshot loadConfigSnapshot(String taskId) {
        return storage.loadConfigSnapshot(taskId);
    }

    public void saveScanStatistics(String taskId, TaskInfo.ScanStage statistics) {
        storage.saveScanStatistics(taskId, statistics);
    }

    public TaskInfo.ScanStage loadScanStatistics(String taskId) {
        return storage.loadScanStatistics(taskId);
    }

    public void writeScanData(String taskId, String jsonData) {
        storage.writeScanData(taskId, jsonData);
    }

    public void finishScanDataWriting(String taskId) {
        storage.finishScanDataWriting(taskId);
    }

    public void savePreviewStatistics(String taskId, TaskInfo.PreviewStage statistics) {
        storage.savePreviewStatistics(taskId, statistics);
    }

    public TaskInfo.PreviewStage loadPreviewStatistics(String taskId) {
        return storage.loadPreviewStatistics(taskId);
    }

    public void writePreviewData(String taskId, String jsonData) {
        storage.writePreviewData(taskId, jsonData);
    }

    public void finishPreviewDataWriting(String taskId) {
        storage.finishPreviewDataWriting(taskId);
    }

    public void saveChangeRecords(String taskId, List<ChangeRecord> changeRecords) {
        storage.saveChangeRecords(taskId, changeRecords);
    }

    public List<ChangeRecord> loadChangeRecords(String taskId) {
        return storage.loadChangeRecords(taskId);
    }

    public void saveExecutionStatistics(String taskId, int executionNum, TaskInfo.ExecutionStage statistics) {
        storage.saveExecutionStatistics(taskId, executionNum, statistics);
    }

    public TaskInfo.ExecutionStage loadExecutionStatistics(String taskId, int executionNum) {
        return storage.loadExecutionStatistics(taskId, executionNum);
    }

    public void writeExecutionData(String taskId, int executionNum, String jsonData) {
        storage.writeExecutionData(taskId, executionNum, jsonData);
    }

    public void finishExecutionDataWriting(String taskId, int executionNum) {
        storage.finishExecutionDataWriting(taskId, executionNum);
    }

    public List<String> getAllTaskIds() {
        return storage.getAllTaskIds();
    }

    public boolean deleteTask(String taskId) {
        return storage.deleteTask(taskId);
    }

    public void writeTaskLog(String taskId, String logEntry) {
        storage.writeTaskLog(taskId, logEntry);
    }

    public List<String> readTaskLog(String taskId, int page, int pageSize) {
        return storage.readTaskLog(taskId, page, pageSize);
    }

    public void clearAllTasks() {
        storage.clearAllTasks();
    }

    public void clearScanData(String taskId) {
        storage.clearScanData(taskId);
    }

    public void clearPreviewData(String taskId) {
        storage.clearPreviewData(taskId);
    }

    public void clearExecutionData(String taskId) {
        storage.clearExecutionData(taskId);
    }

    public List<String> readScanData(String taskId, int page, int pageSize) {
        return storage.readScanData(taskId, page, pageSize);
    }

    public List<String> readPreviewData(String taskId, int page, int pageSize) {
        return storage.readPreviewData(taskId, page, pageSize);
    }

    public List<String> readExecutionData(String taskId, int executionNum, int page, int pageSize) {
        return storage.readExecutionData(taskId, executionNum, page, pageSize);
    }

    public List<Integer> getExecutionHistory(String taskId) {
        return storage.getExecutionHistory(taskId);
    }

    public void shutdown() {
        if (storage instanceof com.filemanager.backend.storage.FileSystemTaskStorage) {
            ((com.filemanager.backend.storage.FileSystemTaskStorage) storage).shutdown();
        } else if (storage instanceof com.filemanager.backend.storage.DatabaseTaskStorage) {
            ((com.filemanager.backend.storage.DatabaseTaskStorage) storage).shutdown();
        }
    }
}