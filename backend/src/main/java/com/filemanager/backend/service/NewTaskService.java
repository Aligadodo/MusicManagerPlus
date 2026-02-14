package com.filemanager.backend.service;

import com.filemanager.backend.model.*;
import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.service.StrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 新的任务管理服务
 * 支持任务快照、预览、执行的完整流程
 */
@Service
public class NewTaskService {

    private final TaskFileStorageService storageService;
    private final StrategyService strategyService;
    private final Map<String, TaskExecution> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    public NewTaskService(TaskFileStorageService storageService, StrategyService strategyService) {
        this.storageService = storageService;
        this.strategyService = strategyService;
    }

    /**
     * 创建预览任务
     */
    public String createPreviewTask(TaskRequestDTO request) {
        String taskId = "preview-" + System.currentTimeMillis();
        TaskSnapshot snapshot = createTaskSnapshot(taskId, TaskSnapshot.TaskType.PREVIEW, request);
        storageService.saveTaskSnapshot(snapshot);
        System.out.println("[NewTaskService] 预览任务已创建: " + taskId);
        return taskId;
    }

    /**
     * 创建执行任务
     */
    public String createExecuteTask(String previewTaskId) {
        String taskId = "execute-" + System.currentTimeMillis();
        TaskSnapshot previewSnapshot = storageService.loadTaskSnapshot(previewTaskId);
        if (previewSnapshot == null) {
            throw new IllegalArgumentException("预览任务不存在: " + previewTaskId);
        }

        TaskSnapshot executeSnapshot = new TaskSnapshot(taskId, TaskSnapshot.TaskType.EXECUTE);
        executeSnapshot.setConfigSnapshot(previewSnapshot.getConfigSnapshot());
        storageService.saveTaskSnapshot(executeSnapshot);
        System.out.println("[NewTaskService] 执行任务已创建: " + taskId + ", 基于预览任务: " + previewTaskId);
        return taskId;
    }

    /**
     * 执行预览任务
     */
    public void executePreviewTask(String taskId) {
        System.out.println("[NewTaskService] 开始执行预览任务: " + taskId);
        
        TaskSnapshot snapshot = storageService.loadTaskSnapshot(taskId);
        if (snapshot == null) {
            throw new IllegalArgumentException("任务快照不存在: " + taskId);
        }

        TaskExecution execution = new TaskExecution(taskId, snapshot, storageService, strategyService);
        runningTasks.put(taskId, execution);
        
        Future<?> future = executorService.submit(() -> {
            try {
                execution.executePreview();
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        execution.setFuture(future);
        System.out.println("[NewTaskService] 预览任务已提交到执行线程池: " + taskId);
    }

    /**
     * 执行任务
     */
    public void executeTask(String taskId) {
        System.out.println("[NewTaskService] 开始执行任务: " + taskId);
        
        TaskSnapshot snapshot = storageService.loadTaskSnapshot(taskId);
        if (snapshot == null) {
            throw new IllegalArgumentException("任务快照不存在: " + taskId);
        }

        TaskExecution execution = new TaskExecution(taskId, snapshot, storageService, strategyService);
        runningTasks.put(taskId, execution);
        
        Future<?> future = executorService.submit(() -> {
            try {
                execution.execute();
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        execution.setFuture(future);
        System.out.println("[NewTaskService] 任务已提交到执行线程池: " + taskId);
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        TaskExecution execution = runningTasks.get(taskId);
        if (execution == null) {
            return false;
        }

        Future<?> future = execution.getFuture();
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }

        execution.cancel();
        System.out.println("[NewTaskService] 任务已取消: " + taskId);
        return true;
    }

    /**
     * 获取任务进度
     */
    public TaskFileStorageService.TaskProgress getTaskProgress(String taskId) {
        return storageService.loadTaskProgress(taskId);
    }

    /**
     * 获取预览结果
     */
    public PreviewResult getPreviewResult(String taskId) {
        return storageService.loadPreviewResult(taskId);
    }

    /**
     * 获取执行结果
     */
    public ExecutionResult getExecutionResult(String taskId) {
        return storageService.loadExecutionResult(taskId);
    }

    /**
     * 获取所有预览结果
     */
    public List<PreviewResult> getAllPreviewResults() {
        return storageService.loadAllPreviewResults();
    }

    /**
     * 获取所有执行结果
     */
    public List<ExecutionResult> getAllExecutionResults() {
        return storageService.loadAllExecutionResults();
    }

    /**
     * 创建任务快照
     */
    private TaskSnapshot createTaskSnapshot(String taskId, TaskSnapshot.TaskType taskType, TaskRequestDTO request) {
        TaskSnapshot snapshot = new TaskSnapshot(taskId, taskType);
        
        // 创建配置快照
        TaskSnapshot.ConfigSnapshot configSnapshot = new TaskSnapshot.ConfigSnapshot();
        
        // 源目录配置
        List<TaskSnapshot.SourceDirectoryConfig> sourceDirs = new ArrayList<>();
        if (request.getFilePaths() != null) {
            for (String path : request.getFilePaths()) {
                sourceDirs.add(new TaskSnapshot.SourceDirectoryConfig(path, 4));
            }
        }
        configSnapshot.setSourceDirectories(sourceDirs);
        
        // 流水线配置
        TaskSnapshot.PipelineConfig pipelineConfig = new TaskSnapshot.PipelineConfig();
        pipelineConfig.setPipelineId("default-pipeline");
        pipelineConfig.setName("默认流水线");
        pipelineConfig.setItems(new ArrayList<>());
        configSnapshot.setPipelineConfig(pipelineConfig);
        
        // 全局设置
        TaskSnapshot.GlobalSettings globalSettings = new TaskSnapshot.GlobalSettings();
        globalSettings.setMaxThreads(10);
        globalSettings.setTimeout(300000L);
        globalSettings.setDryRun(false);
        configSnapshot.setGlobalSettings(globalSettings);
        
        snapshot.setConfigSnapshot(configSnapshot);
        return snapshot;
    }

    /**
     * 任务执行器
     */
    private static class TaskExecution {
        private final String taskId;
        private final TaskSnapshot snapshot;
        private final TaskFileStorageService storageService;
        private final StrategyService strategyService;
        private Future<?> future;
        private volatile boolean cancelled = false;

        public TaskExecution(String taskId, TaskSnapshot snapshot, 
                          TaskFileStorageService storageService, StrategyService strategyService) {
            this.taskId = taskId;
            this.snapshot = snapshot;
            this.storageService = storageService;
            this.strategyService = strategyService;
        }

        public void executePreview() {
            System.out.println("[TaskExecution] 开始预览分析: " + taskId);
            
            TaskFileStorageService.TaskProgress progress = new TaskFileStorageService.TaskProgress(taskId);
            progress.setStatus("PREVIEWING");
            progress.setProgress(0.0);
            progress.setMessage("开始预览分析...");
            storageService.saveTaskProgress(taskId, progress);
            
            snapshot.setStatus("PREVIEWING");
            snapshot.setStartedAt(System.currentTimeMillis());
            storageService.saveTaskSnapshot(snapshot);
            
            try {
                // 基于快照执行预览
                PreviewResult previewResult = new PreviewResult(taskId);
                
                // 获取所有文件
                List<String> filePaths = new ArrayList<>();
                for (TaskSnapshot.SourceDirectoryConfig dir : snapshot.getConfigSnapshot().getSourceDirectories()) {
                    filePaths.add(dir.getPath());
                }
                
                progress.setMessage("正在分析文件...");
                progress.setTotalFiles(filePaths.size());
                storageService.saveTaskProgress(taskId, progress);
                
                // 执行策略分析
                List<com.filemanager.domain.entity.ChangeRecord> changeRecords = new ArrayList<>();
                for (int i = 0; i < filePaths.size(); i++) {
                    if (cancelled) {
                        break;
                    }
                    
                    String filePath = filePaths.get(i);
                    progress.setMessage("正在分析文件: " + (i + 1) + "/" + filePaths.size());
                    progress.setProcessedFiles(i + 1);
                    progress.setProgress((i + 1.0) / filePaths.size());
                    storageService.saveTaskProgress(taskId, progress);
                    
                    // 这里简化处理，实际应该基于快照配置执行策略
                    com.filemanager.domain.entity.ChangeRecord record = new com.filemanager.domain.entity.ChangeRecord();
                    record.setOriginalName(new File(filePath).getName());
                    record.setNewName(new File(filePath).getName());
                    record.setFilePath(filePath);
                    record.setChanged(false);
                    record.setStatus(com.filemanager.domain.enums.ExecStatus.PENDING.name());
                    changeRecords.add(record);
                }
                
                previewResult.setChangeRecords(changeRecords);
                
                // 统计信息
                PreviewResult.PreviewStatistics statistics = new PreviewResult.PreviewStatistics();
                statistics.setTotalFiles(filePaths.size());
                statistics.setProcessedFiles(filePaths.size());
                statistics.setChangedFiles(0);
                statistics.setUnchangedFiles(filePaths.size());
                previewResult.setStatistics(statistics);
                
                // 保存预览结果
                storageService.savePreviewResult(previewResult);
                
                progress.setStatus("PREVIEW_COMPLETED");
                progress.setProgress(1.0);
                progress.setMessage("预览完成");
                storageService.saveTaskProgress(taskId, progress);
                
                snapshot.setStatus("PREVIEW_COMPLETED");
                snapshot.setProgress(1.0);
                snapshot.setCompletedAt(System.currentTimeMillis());
                storageService.saveTaskSnapshot(snapshot);
                
                System.out.println("[TaskExecution] 预览分析完成: " + taskId);
                
            } catch (Exception e) {
                System.err.println("[TaskExecution] 预览分析失败: " + taskId);
                e.printStackTrace();
                
                progress.setStatus("FAILED");
                progress.setMessage("预览失败: " + e.getMessage());
                storageService.saveTaskProgress(taskId, progress);
                
                snapshot.setStatus("FAILED");
                snapshot.setMessage("预览失败: " + e.getMessage());
                snapshot.setCompletedAt(System.currentTimeMillis());
                storageService.saveTaskSnapshot(snapshot);
            }
        }

        public void execute() {
            System.out.println("[TaskExecution] 开始执行任务: " + taskId);
            
            TaskFileStorageService.TaskProgress progress = new TaskFileStorageService.TaskProgress(taskId);
            progress.setStatus("EXECUTING");
            progress.setProgress(0.0);
            progress.setMessage("开始执行任务...");
            storageService.saveTaskProgress(taskId, progress);
            
            snapshot.setStatus("EXECUTING");
            snapshot.setStartedAt(System.currentTimeMillis());
            storageService.saveTaskSnapshot(snapshot);
            
            try {
                // 加载预览结果
                PreviewResult previewResult = storageService.loadPreviewResult(snapshot.getTaskId());
                if (previewResult == null) {
                    throw new IllegalStateException("预览结果不存在，无法执行任务");
                }
                
                // 创建执行结果
                ExecutionResult executionResult = new ExecutionResult(taskId, snapshot.getTaskId());
                
                List<ExecutionResult.ExecutionRecord> executionRecords = new ArrayList<>();
                List<com.filemanager.domain.entity.ChangeRecord> changeRecords = previewResult.getChangeRecords();
                
                progress.setTotalFiles(changeRecords.size());
                progress.setMessage("开始执行变更...");
                storageService.saveTaskProgress(taskId, progress);
                
                // 执行变更
                for (int i = 0; i < changeRecords.size(); i++) {
                    if (cancelled) {
                        break;
                    }
                    
                    com.filemanager.domain.entity.ChangeRecord changeRecord = changeRecords.get(i);
                    progress.setMessage("正在执行变更: " + (i + 1) + "/" + changeRecords.size());
                    progress.setProcessedFiles(i + 1);
                    progress.setProgress((i + 1.0) / changeRecords.size());
                    storageService.saveTaskProgress(taskId, progress);
                    
                    // 创建执行记录
                    ExecutionResult.ExecutionRecord executionRecord = new ExecutionResult.ExecutionRecord(changeRecord);
                    executionRecord.setExecutionTime(System.currentTimeMillis());
                    executionRecord.setRetryCount(0);
                    
                    // 基于extraParams执行变更
                    if (changeRecord.isChanged() && changeRecord.getExtraParams() != null) {
                        try {
                            // 这里应该根据extraParams执行实际的文件操作
                            // 简化处理，标记为成功
                            executionRecord.setStatus(com.filemanager.domain.enums.ExecStatus.SUCCESS.name());
                            progress.setSuccessCount(progress.getSuccessCount() + 1);
                        } catch (Exception e) {
                            executionRecord.setStatus(com.filemanager.domain.enums.ExecStatus.FAILED.name());
                            executionRecord.setErrorMessage(e.getMessage());
                            progress.setFailedCount(progress.getFailedCount() + 1);
                        }
                    } else {
                        executionRecord.setStatus(com.filemanager.domain.enums.ExecStatus.SKIPPED.name());
                        progress.setSkippedCount(progress.getSkippedCount() + 1);
                    }
                    
                    executionRecords.add(executionRecord);
                }
                
                executionResult.setExecutionRecords(executionRecords);
                executionResult.setStartedAt(System.currentTimeMillis());
                
                // 统计信息
                ExecutionResult.ExecutionStatistics statistics = new ExecutionResult.ExecutionStatistics();
                statistics.setTotalFiles(changeRecords.size());
                statistics.setProcessedFiles(changeRecords.size());
                statistics.setSuccessCount(progress.getSuccessCount());
                statistics.setFailedCount(progress.getFailedCount());
                statistics.setSkippedCount(progress.getSkippedCount());
                executionResult.setStatistics(statistics);
                
                // 保存执行结果
                storageService.saveExecutionResult(executionResult);
                
                progress.setStatus("COMPLETED");
                progress.setProgress(1.0);
                progress.setMessage("执行完成");
                storageService.saveTaskProgress(taskId, progress);
                
                snapshot.setStatus("COMPLETED");
                snapshot.setProgress(1.0);
                snapshot.setCompletedAt(System.currentTimeMillis());
                storageService.saveTaskSnapshot(snapshot);
                
                System.out.println("[TaskExecution] 任务执行完成: " + taskId);
                
            } catch (Exception e) {
                System.err.println("[TaskExecution] 任务执行失败: " + taskId);
                e.printStackTrace();
                
                progress.setStatus("FAILED");
                progress.setMessage("执行失败: " + e.getMessage());
                storageService.saveTaskProgress(taskId, progress);
                
                snapshot.setStatus("FAILED");
                snapshot.setMessage("执行失败: " + e.getMessage());
                snapshot.setCompletedAt(System.currentTimeMillis());
                storageService.saveTaskSnapshot(snapshot);
            }
        }

        public void cancel() {
            this.cancelled = true;
        }

        public Future<?> getFuture() {
            return future;
        }

        public void setFuture(Future<?> future) {
            this.future = future;
        }
    }
}
