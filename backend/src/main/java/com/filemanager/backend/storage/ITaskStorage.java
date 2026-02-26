package com.filemanager.backend.storage;

import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.domain.entity.ChangeRecord;

import java.util.List;

public interface ITaskStorage {
    
    void initializeTaskDirectory(String taskId);
    
    String getTaskDirectory(String taskId);
    
    void saveTaskInfo(TaskInfo taskInfo);
    
    TaskInfo loadTaskInfo(String taskId);
    
    void saveConfigSnapshot(String taskId, TaskConfigSnapshot configSnapshot);
    
    TaskConfigSnapshot loadConfigSnapshot(String taskId);
    
    void saveScanStatistics(String taskId, TaskInfo.ScanStage statistics);
    
    TaskInfo.ScanStage loadScanStatistics(String taskId);
    
    void writeScanData(String taskId, String jsonData);
    
    void finishScanDataWriting(String taskId);
    
    void savePreviewStatistics(String taskId, TaskInfo.PreviewStage statistics);
    
    TaskInfo.PreviewStage loadPreviewStatistics(String taskId);
    
    void writePreviewData(String taskId, String jsonData);
    
    void finishPreviewDataWriting(String taskId);
    
    void saveChangeRecords(String taskId, List<ChangeRecord> changeRecords);
    
    List<ChangeRecord> loadChangeRecords(String taskId);
    
    void saveExecutionStatistics(String taskId, int executionNum, TaskInfo.ExecutionStage statistics);
    
    TaskInfo.ExecutionStage loadExecutionStatistics(String taskId, int executionNum);
    
    void writeExecutionData(String taskId, int executionNum, String jsonData);
    
    void finishExecutionDataWriting(String taskId, int executionNum);
    
    List<String> getAllTaskIds();
    
    boolean deleteTask(String taskId);
    
    void writeTaskLog(String taskId, String logEntry);
    
    List<String> readTaskLog(String taskId, int page, int pageSize);
    
    void clearAllTasks();
    
    void clearScanData(String taskId);
    
    void clearPreviewData(String taskId);
    
    void clearExecutionData(String taskId);
    
    List<String> readScanData(String taskId, int page, int pageSize);
    
    List<String> readPreviewData(String taskId, int page, int pageSize);
    
    List<String> readExecutionData(String taskId, int executionNum, int page, int pageSize);
    
    List<Integer> getExecutionHistory(String taskId);
}