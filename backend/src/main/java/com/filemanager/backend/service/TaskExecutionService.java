package com.filemanager.backend.service;

import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.backend.entity.ChangeRecordPO;
import com.filemanager.backend.mapper.TaskInfoMapper;
import com.filemanager.backend.mapper.ChangeRecordMapper;
import com.filemanager.backend.model.*;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.service.StrategyService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

/**
 * 任务执行服务
 * 支持流式处理、多阶段执行、任务事务机制
 */
@Service
public class TaskExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(TaskExecutionService.class);
    
    private final TaskStorageService storageService;
    private final StrategyService strategyService;
    private final WebSocketMessageService webSocketService;
    private final ConfigSnapshotService configSnapshotService;
    private final TaskInfoMapper taskInfoMapper;
    private final ChangeRecordMapper changeRecordMapper;
    private final Map<String, TaskExecution> runningTasks = new ConcurrentHashMap<>();
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(5);
    private final ExecutorService processingExecutor = Executors.newFixedThreadPool(10);

    @Autowired
    public TaskExecutionService(TaskStorageService storageService, 
                                         StrategyService strategyService,
                                         WebSocketMessageService webSocketService,
                                         ConfigSnapshotService configSnapshotService,
                                         TaskInfoMapper taskInfoMapper,
                                         ChangeRecordMapper changeRecordMapper) {
        this.storageService = storageService;
        this.strategyService = strategyService;
        this.webSocketService = webSocketService;
        this.configSnapshotService = configSnapshotService;
        this.taskInfoMapper = taskInfoMapper;
        this.changeRecordMapper = changeRecordMapper;
    }

    /**
     * 创建任务
     */
    public String createTask(TaskRequestDTO request) {
        String taskId = "task-" + System.currentTimeMillis();
        
        // 初始化任务目录
        storageService.initializeTaskDirectory(taskId);
        
        // 创建任务信息
        TaskInfo taskInfo = new TaskInfo(taskId);
        taskInfo.setTaskName(request.getTaskName() != null ? request.getTaskName() : "未命名任务");
        
        // 创建配置快照
        TaskConfigSnapshot configSnapshot = createConfigSnapshot(request);
        taskInfo.setConfigSnapshot(configSnapshot);
        
        // 使用 ConfigSnapshotService 获取或创建快照
        String snapshotId = configSnapshotService.getOrCreateSnapshot(configSnapshot, "TASK_CONFIG");
        
        // 将快照ID保存到任务信息中
        taskInfo.setConfigSnapshotId(snapshotId);
        
        // 保存任务信息和配置快照
        storageService.saveTaskInfo(taskInfo);
        storageService.saveConfigSnapshot(taskId, configSnapshot);
        
        // 保存任务信息到数据库
        TaskInfoPO taskInfoPO = new TaskInfoPO();
        taskInfoPO.setTaskId(taskId);
        taskInfoPO.setTaskName(taskInfo.getTaskName());
        taskInfoPO.setStatus(taskInfo.getStatus().name());
        taskInfoPO.setCurrentStage(taskInfo.getCurrentStage());
        taskInfoPO.setOverallProgress(taskInfo.getOverallProgress());
        taskInfoPO.setMessage(taskInfo.getMessage());
        taskInfoPO.setConfigSnapshotId(snapshotId);
        taskInfoPO.setCreatedAt(new Date(taskInfo.getCreatedAt()));
        taskInfoPO.setUpdatedAt(new Date(taskInfo.getUpdatedAt()));
        taskInfoMapper.insert(taskInfoPO);
        
        logger.info("[TaskExecution] 任务已创建: {}，配置快照ID: {}", taskId, snapshotId);
        return taskId;
    }
    
    private void updateTaskInfoInDatabase(TaskInfo taskInfo) {
        try {
            TaskInfoPO taskInfoPO = taskInfoMapper.selectByTaskId(taskInfo.getTaskId());
            if (taskInfoPO == null) {
                taskInfoPO = new TaskInfoPO();
                taskInfoPO.setTaskId(taskInfo.getTaskId());
            }
            
            taskInfoPO.setTaskName(taskInfo.getTaskName());
            taskInfoPO.setStatus(taskInfo.getStatus().name());
            taskInfoPO.setCurrentStage(taskInfo.getCurrentStage());
            taskInfoPO.setOverallProgress(taskInfo.getOverallProgress());
            taskInfoPO.setMessage(taskInfo.getMessage());
            taskInfoPO.setConfigSnapshotId(taskInfo.getConfigSnapshotId());
            taskInfoPO.setCreatedAt(new Date(taskInfo.getCreatedAt()));
            taskInfoPO.setUpdatedAt(new Date(taskInfo.getUpdatedAt()));
            
            // 如果任务已完成，设置完成时间
            if (taskInfo.getStatus() == TaskInfo.TaskStatus.COMPLETED) {
                taskInfoPO.setCompletedAt(new Date());
            }
            
            if (taskInfoPO.getTaskId() == null || taskInfoMapper.selectByTaskId(taskInfo.getTaskId()) == null) {
                taskInfoMapper.insert(taskInfoPO);
            } else {
                taskInfoMapper.update(taskInfoPO);
            }
        } catch (Exception e) {
            logger.error("[TaskExecution] 更新数据库任务信息失败: {}", taskInfo.getTaskId(), e);
        }
    }

    /**
     * 执行文件扫描
     */
    public void executeScan(String taskId) {
        logger.info("[TaskExecution] 开始执行文件扫描: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        TaskExecution execution = new TaskExecution(taskId, taskInfo, storageService, strategyService, webSocketService, this);
        runningTasks.put(taskId, execution);
        
        Future<?> future = taskExecutor.submit(() -> {
            try {
                execution.executeScan();
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        execution.setFuture(future);
        logger.info("[TaskExecution] 文件扫描已提交: {}", taskId);
    }

    /**
     * 执行预览分析
     */
    public void executePreview(String taskId) {
        logger.info("[TaskExecution] 开始执行预览分析: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        logger.info("[TaskExecution] 当前扫描状态: {}", taskInfo.getStages().getScan().getStatus());
        
        // 检查扫描是否完成，最多等待10秒
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            if ("COMPLETED".equals(taskInfo.getStages().getScan().getStatus())) {
                logger.info("[TaskExecution] 扫描已完成，可以开始预览分析");
                break;
            }
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(1000);
                    taskInfo = storageService.loadTaskInfo(taskId);
                    logger.info("[TaskExecution] 重试加载任务信息，扫描状态: {}", taskInfo.getStages().getScan().getStatus());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待扫描完成被中断", e);
                }
            }
        }
        
        if (!"COMPLETED".equals(taskInfo.getStages().getScan().getStatus())) {
            logger.error("[TaskExecution] 扫描未完成，当前状态: {}", taskInfo.getStages().getScan().getStatus());
            throw new IllegalStateException("文件扫描未完成，无法执行预览分析");
        }
        
        TaskExecution execution = new TaskExecution(taskId, taskInfo, storageService, strategyService, webSocketService, this);
        runningTasks.put(taskId, execution);
        
        Future<?> future = taskExecutor.submit(() -> {
            try {
                execution.executePreview();
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        execution.setFuture(future);
        logger.info("[TaskExecution] 预览分析已提交: {}", taskId);
    }

    /**
     * 执行任务
     */
    public void executeTask(String taskId) {
        logger.info("[TaskExecution] 开始执行任务: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        // 检查预览是否完成，最多等待5秒
        int maxRetries = 5;
        for (int i = 0; i < maxRetries; i++) {
            if ("PREVIEWED".equals(taskInfo.getStages().getPreview().getStatus())) {
                break;
            }
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(1000);
                    taskInfo = storageService.loadTaskInfo(taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("等待预览完成被中断", e);
                }
            }
        }
        
        if (!"PREVIEWED".equals(taskInfo.getStages().getPreview().getStatus()) && 
            !"COMPLETED".equals(taskInfo.getStages().getPreview().getStatus())) {
            throw new IllegalStateException("预览分析未完成，无法执行任务");
        }
        
        // 获取执行次数
        int executionNum = taskInfo.getStages().getExecution().getExecutionCount() + 1;
        
        // 创建执行目录
        try {
            Files.createDirectories(Paths.get(storageService.getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum)));
        } catch (IOException e) {
            logger.error("[TaskExecution] 创建执行目录失败: {} - execution_{}", taskId, executionNum, e);
        }
        
        TaskExecution execution = new TaskExecution(taskId, taskInfo, storageService, strategyService, webSocketService, this);
        runningTasks.put(taskId, execution);
        
        Future<?> future = taskExecutor.submit(() -> {
            try {
                execution.execute(executionNum);
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        execution.setFuture(future);
        logger.info("[TaskExecution] 任务执行已提交: {} - execution_{}", taskId, executionNum);
    }

    /**
     * 执行选中的记录
     */
    public void executeSelected(String taskId, List<String> selectedRecordIds) {
        logger.info("[TaskExecution] 开始执行选中的记录: {}, 数量: {}", taskId, selectedRecordIds.size());
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        if (taskInfo.getStatus() != TaskInfo.TaskStatus.PREVIEWED) {
            throw new IllegalStateException("任务未完成预览，无法执行选中的记录");
        }
        
        if (selectedRecordIds == null || selectedRecordIds.isEmpty()) {
            throw new IllegalArgumentException("选中的记录列表不能为空");
        }
        
        // 获取执行次数
        int executionNum = taskInfo.getStages().getExecution().getExecutionCount() + 1;
        
        // 创建执行目录
        try {
            Files.createDirectories(Paths.get(storageService.getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum)));
        } catch (IOException e) {
            logger.error("[TaskExecution] 创建执行目录失败: {} - execution_{}", taskId, executionNum, e);
        }
        
        TaskExecution execution = new TaskExecution(taskId, taskInfo, storageService, strategyService, webSocketService, this);
        runningTasks.put(taskId, execution);
        
        Future<?> future = taskExecutor.submit(() -> {
            try {
                execution.executeSelected(executionNum, selectedRecordIds);
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        execution.setFuture(future);
        logger.info("[TaskExecution] 选中记录执行已提交: {} - execution_{}", taskId, executionNum);
    }

    /**
     * 重试失败的记录
     */
    public void retryFailed(String taskId) {
        logger.info("[TaskExecution] 开始重试失败的记录: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        if (taskInfo.getStatus() != TaskInfo.TaskStatus.FAILED && 
            taskInfo.getStatus() != TaskInfo.TaskStatus.EXECUTING) {
            throw new IllegalStateException("任务状态不允许重试，当前状态: " + taskInfo.getStatus());
        }
        
        // 获取执行次数
        int executionNum = taskInfo.getStages().getExecution().getExecutionCount() + 1;
        
        // 创建执行目录
        try {
            Files.createDirectories(Paths.get(storageService.getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum)));
        } catch (IOException e) {
            logger.error("[TaskExecution] 创建执行目录失败: {} - execution_{}", taskId, executionNum, e);
        }
        
        TaskExecution execution = new TaskExecution(taskId, taskInfo, storageService, strategyService, webSocketService, this);
        runningTasks.put(taskId, execution);
        
        Future<?> future = taskExecutor.submit(() -> {
            try {
                execution.retryFailed(executionNum);
            } finally {
                runningTasks.remove(taskId);
            }
        });
        
        execution.setFuture(future);
        logger.info("[TaskExecution] 失败记录重试已提交: {} - execution_{}", taskId, executionNum);
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
        logger.info("[TaskExecution] 任务已取消: {}", taskId);
        return true;
    }

    public boolean cancelStage(String taskId, String stageType) {
        TaskExecution execution = runningTasks.get(taskId);
        if (execution == null) {
            return false;
        }
        
        Future<?> future = execution.getFuture();
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        
        execution.cancelStage(stageType);
        logger.info("[TaskExecution] 阶段已取消: taskId={}, stageType={}", taskId, stageType);
        return true;
    }

    /**
     * 暂停任务
     */
    public boolean pauseTask(String taskId) {
        TaskExecution execution = runningTasks.get(taskId);
        if (execution == null) {
            return false;
        }
        
        execution.pause();
        logger.info("[TaskExecution] 任务已暂停: {}", taskId);
        return true;
    }

    /**
     * 恢复任务
     */
    public boolean resumeTask(String taskId) {
        TaskExecution execution = runningTasks.get(taskId);
        if (execution == null) {
            return false;
        }
        
        execution.resume();
        logger.info("[TaskExecution] 任务已恢复: {}", taskId);
        return true;
    }

    /**
     * 重新扫描
     */
    public void restartScan(String taskId) {
        logger.info("[TaskExecution] 开始重新扫描: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        // 清空扫描数据
        storageService.clearScanData(taskId);
        
        // 重置扫描阶段状态
        TaskInfo.ScanStage scanStage = taskInfo.getStages().getScan();
        scanStage.setStatus("PENDING");
        scanStage.setTotalFiles(0);
        scanStage.setTotalSize(0);
        scanStage.setScanStartTime(0);
        scanStage.setScanEndTime(0);
        scanStage.setScanDuration(0);
        
        // 重置预览和执行阶段
        TaskInfo.PreviewStage previewStage = taskInfo.getStages().getPreview();
        previewStage.setStatus("PENDING");
        previewStage.setTotalFiles(0);
        previewStage.setProcessedFiles(0);
        previewStage.setChangedFiles(0);
        previewStage.setUnchangedFiles(0);
        previewStage.setPreviewStartTime(0);
        previewStage.setPreviewEndTime(0);
        previewStage.setPreviewDuration(0);
        
        TaskInfo.ExecutionStage executionStage = taskInfo.getStages().getExecution();
        executionStage.setStatus("PENDING");
        executionStage.setExecutionCount(0);
        executionStage.setTotalFiles(0);
        executionStage.setProcessedFiles(0);
        executionStage.setSuccessCount(0);
        executionStage.setFailedCount(0);
        executionStage.setSkippedCount(0);
        executionStage.setExecutionStartTime(0);
        executionStage.setExecutionEndTime(0);
        executionStage.setExecutionDuration(0);
        
        // 重置任务状态
        taskInfo.setCurrentStage("CREATED");
        taskInfo.setStatus(TaskInfo.TaskStatus.CREATED);
        taskInfo.setMessage("准备重新扫描");
        
        storageService.saveTaskInfo(taskInfo);
        storageService.writeTaskLog(taskId, "[INFO] [RESTART] 准备重新扫描");
        updateTaskInfoInDatabase(taskInfo);
        
        // 实际启动扫描
        executeScan(taskId);
        
        logger.info("[TaskExecution] 重新扫描已启动: {}", taskId);
    }

    /**
     * 重新预览
     */
    public void restartPreview(String taskId) {
        logger.info("[TaskExecution] 开始重新预览: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        // 清空预览数据
        storageService.clearPreviewData(taskId);
        
        // 重置预览阶段状态
        TaskInfo.PreviewStage previewStage = taskInfo.getStages().getPreview();
        previewStage.setStatus("PENDING");
        previewStage.setTotalFiles(0);
        previewStage.setProcessedFiles(0);
        previewStage.setChangedFiles(0);
        previewStage.setUnchangedFiles(0);
        previewStage.setPreviewStartTime(0);
        previewStage.setPreviewEndTime(0);
        previewStage.setPreviewDuration(0);
        
        // 重置执行阶段
        TaskInfo.ExecutionStage executionStage = taskInfo.getStages().getExecution();
        executionStage.setStatus("PENDING");
        executionStage.setExecutionCount(0);
        executionStage.setTotalFiles(0);
        executionStage.setProcessedFiles(0);
        executionStage.setSuccessCount(0);
        executionStage.setFailedCount(0);
        executionStage.setSkippedCount(0);
        executionStage.setExecutionStartTime(0);
        executionStage.setExecutionEndTime(0);
        executionStage.setExecutionDuration(0);
        
        // 重置任务状态
        taskInfo.setCurrentStage("SCANNED");
        taskInfo.setStatus(TaskInfo.TaskStatus.SCANNED);
        taskInfo.setMessage("准备重新预览");
        
        storageService.saveTaskInfo(taskInfo);
        storageService.writeTaskLog(taskId, "[INFO] [RESTART] 准备重新预览");
        updateTaskInfoInDatabase(taskInfo);
        
        // 实际启动预览
        executePreview(taskId);
        
        logger.info("[TaskExecution] 重新预览已启动: {}", taskId);
    }

    /**
     * 重新执行
     */
    public void restartExecution(String taskId) {
        logger.info("[TaskExecution] 开始重新执行: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        // 清空执行数据
        storageService.clearExecutionData(taskId);
        
        // 重置执行阶段状态
        TaskInfo.ExecutionStage executionStage = taskInfo.getStages().getExecution();
        executionStage.setStatus("PENDING");
        executionStage.setExecutionCount(0);
        executionStage.setTotalFiles(0);
        executionStage.setProcessedFiles(0);
        executionStage.setSuccessCount(0);
        executionStage.setFailedCount(0);
        executionStage.setSkippedCount(0);
        executionStage.setExecutionStartTime(0);
        executionStage.setExecutionEndTime(0);
        executionStage.setExecutionDuration(0);
        
        // 重置任务状态
        taskInfo.setCurrentStage("PREVIEWED");
        taskInfo.setStatus(TaskInfo.TaskStatus.PREVIEWED);
        taskInfo.setMessage("准备重新执行");
        
        storageService.saveTaskInfo(taskInfo);
        storageService.writeTaskLog(taskId, "[INFO] [RESTART] 准备重新执行");
        updateTaskInfoInDatabase(taskInfo);
        
        // 实际启动执行
        executeTask(taskId);
        
        logger.info("[TaskExecution] 重新执行已启动: {}", taskId);
    }
    
    /**
     * 重新运行任务（从扫描开始）
     */
    public void rerunTask(String taskId) {
        logger.info("[TaskExecution] 开始重新运行任务: {}", taskId);
        
        TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
        if (taskInfo == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        
        // 清空所有阶段数据
        storageService.clearScanData(taskId);
        storageService.clearPreviewData(taskId);
        storageService.clearExecutionData(taskId);
        
        // 重置扫描阶段状态
        TaskInfo.ScanStage scanStage = taskInfo.getStages().getScan();
        scanStage.setStatus("PENDING");
        scanStage.setTotalFiles(0);
        scanStage.setTotalSize(0);
        scanStage.setScanStartTime(0);
        scanStage.setScanEndTime(0);
        scanStage.setScanDuration(0);
        
        // 重置预览阶段状态
        TaskInfo.PreviewStage previewStage = taskInfo.getStages().getPreview();
        previewStage.setStatus("PENDING");
        previewStage.setTotalFiles(0);
        previewStage.setProcessedFiles(0);
        previewStage.setChangedFiles(0);
        previewStage.setUnchangedFiles(0);
        previewStage.setPreviewStartTime(0);
        previewStage.setPreviewEndTime(0);
        previewStage.setPreviewDuration(0);
        
        // 重置执行阶段状态
        TaskInfo.ExecutionStage executionStage = taskInfo.getStages().getExecution();
        executionStage.setStatus("PENDING");
        executionStage.setExecutionCount(0);
        executionStage.setTotalFiles(0);
        executionStage.setProcessedFiles(0);
        executionStage.setSuccessCount(0);
        executionStage.setFailedCount(0);
        executionStage.setSkippedCount(0);
        executionStage.setExecutionStartTime(0);
        executionStage.setExecutionEndTime(0);
        executionStage.setExecutionDuration(0);
        
        // 重置任务状态
        taskInfo.setCurrentStage("CREATED");
        taskInfo.setStatus(TaskInfo.TaskStatus.CREATED);
        taskInfo.setMessage("准备重新运行任务");
        
        storageService.saveTaskInfo(taskInfo);
        storageService.writeTaskLog(taskId, "[INFO] [RERUN] 准备重新运行任务");
        updateTaskInfoInDatabase(taskInfo);
        
        // 实际启动扫描
        executeScan(taskId);
        
        logger.info("[TaskExecution] 重新运行任务已启动: {}", taskId);
    }

    /**
     * 获取任务进度
     */
    public TaskInfo getTaskProgress(String taskId) {
        return storageService.loadTaskInfo(taskId);
    }

    /**
     * 创建配置快照
     */
    private TaskConfigSnapshot createConfigSnapshot(TaskRequestDTO request) {
        TaskConfigSnapshot configSnapshot = new TaskConfigSnapshot();
        
        // 源目录配置
        List<TaskConfigSnapshot.SourceDirectoryConfig> sourceDirs = new ArrayList<>();
        if (request.getSourceDirectories() != null) {
            for (TaskRequestDTO.SourceDirectoryDTO dto : request.getSourceDirectories()) {
                TaskConfigSnapshot.SourceDirectoryConfig sourceDir = new TaskConfigSnapshot.SourceDirectoryConfig();
                sourceDir.setPath(dto.getPath());
                sourceDir.setDepth(dto.getDepth() != null ? dto.getDepth() : 4);
                sourceDir.setRecursive(dto.isRecursive() != null ? dto.isRecursive() : true);
                sourceDir.setIncludePatterns(dto.getIncludePatterns());
                sourceDir.setExcludePatterns(dto.getExcludePatterns());
                sourceDirs.add(sourceDir);
            }
        }
        configSnapshot.setSourceDirectories(sourceDirs);
        
        // 流水线配置
        TaskConfigSnapshot.PipelineConfig pipelineConfig = new TaskConfigSnapshot.PipelineConfig();
        pipelineConfig.setPipelineId(request.getPipelineId() != null ? request.getPipelineId() : "default-pipeline");
        pipelineConfig.setName("默认流水线");
        pipelineConfig.setItems(new ArrayList<>());
        configSnapshot.setPipelineConfig(pipelineConfig);
        
        // 全局设置
        TaskConfigSnapshot.GlobalSettings globalSettings = new TaskConfigSnapshot.GlobalSettings();
        if (request.getGlobalSettings() != null) {
            TaskRequestDTO.GlobalSettingsDTO dto = request.getGlobalSettings();
            if (dto.getMaxThreads() != null) {
                globalSettings.setMaxThreads(dto.getMaxThreads());
            }
            if (dto.getTimeout() != null) {
                globalSettings.setTimeout(dto.getTimeout());
            }
            if (dto.isDryRun() != null) {
                globalSettings.setDryRun(dto.isDryRun());
            }
            if (dto.isOverwrite() != null) {
                globalSettings.setOverwrite(dto.isOverwrite());
            }
            if (dto.isBackup() != null) {
                globalSettings.setBackup(dto.isBackup());
            }
            if (dto.getBackupPath() != null) {
                globalSettings.setBackupPath(dto.getBackupPath());
            }
            if (dto.getRetryCount() != null) {
                globalSettings.setRetryCount(dto.getRetryCount());
            }
            if (dto.getRetryInterval() != null) {
                globalSettings.setRetryInterval(dto.getRetryInterval());
            }
        }
        configSnapshot.setGlobalSettings(globalSettings);
        
        return configSnapshot;
    }

    /**
     * 任务执行器
     */
    private class TaskExecution {
        private final String taskId;
        private final TaskInfo taskInfo;
        private final TaskStorageService storageService;
        private final StrategyService strategyService;
        private final WebSocketMessageService webSocketService;
        private final TaskExecutionService taskExecutionService;
        private Future<?> future;
        private volatile boolean cancelled = false;
        private volatile boolean paused = false;
        private final Object pauseLock = new Object();

        public TaskExecution(String taskId, TaskInfo taskInfo, 
                          TaskStorageService storageService, 
                          StrategyService strategyService,
                          WebSocketMessageService webSocketService,
                          TaskExecutionService taskExecutionService) {
            this.taskId = taskId;
            this.taskInfo = taskInfo;
            this.storageService = storageService;
            this.strategyService = strategyService;
            this.webSocketService = webSocketService;
            this.taskExecutionService = taskExecutionService;
        }

        public void pause() {
            this.paused = true;
            
            // 更新任务状态为已暂停
            taskInfo.setStatus(TaskInfo.TaskStatus.CANCELLED);
            taskInfo.setMessage("任务已暂停");
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [PAUSE] 任务已暂停");
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
        }

        public void resume() {
            this.paused = false;
            
            // 唤醒等待的线程
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
            
            // 更新任务状态为运行中
            taskInfo.setStatus(TaskInfo.TaskStatus.SCANNING);
            taskInfo.setMessage("任务已恢复");
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [RESUME] 任务已恢复");
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
        }

        public void checkPause() throws InterruptedException {
            if (paused) {
                synchronized (pauseLock) {
                    while (paused) {
                        pauseLock.wait();
                    }
                }
            }
        }

        public void executeScan() {
            logger.info("[TaskExecution] 开始文件扫描: {}", taskId);
            
            // 更新任务状态
            taskInfo.setCurrentStage("SCANNING");
            taskInfo.setStatus(TaskInfo.TaskStatus.SCANNING);
            taskInfo.getStages().getScan().setStatus("RUNNING");
            taskInfo.getStages().getScan().setScanStartTime(System.currentTimeMillis());
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [SCAN] 开始扫描文件");
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
            
            try {
                // 扫描文件
                List<String> filePaths = scanFiles();
                logger.info("[TaskExecution] 扫描到 {} 个文件", filePaths.size());
                
                // 流式写入扫描数据
                for (String filePath : filePaths) {
                    if (cancelled) break;
                    
                    File file = new File(filePath);
                    Map<String, Object> scanRecord = new HashMap<>();
                    scanRecord.put("filePath", filePath);
                    scanRecord.put("fileName", file.getName());
                    scanRecord.put("fileSize", file.length());
                    scanRecord.put("lastModified", file.lastModified());
                    
                    String jsonData = toJson(scanRecord);
                    storageService.writeScanData(taskId, jsonData);
                }
                
                logger.info("[TaskExecution] 已写入 {} 条扫描记录", filePaths.size());
                
                // 标记扫描数据写入完成
                logger.info("[TaskExecution] 准备标记扫描数据写入完成");
                storageService.finishScanDataWriting(taskId);
                
                // 等待数据写入器完成写入
                logger.info("[TaskExecution] 等待数据写入器完成写入");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 验证文件是否存在
                Path scanDataPath = Paths.get(storageService.getTaskDirectory(taskId) + "/scan/data.json");
                logger.info("[TaskExecution] 扫描数据文件路径: {}, 文件存在: {}", scanDataPath, Files.exists(scanDataPath));
                if (Files.exists(scanDataPath)) {
                    logger.info("[TaskExecution] 扫描数据文件大小: {} bytes", Files.size(scanDataPath));
                }
                
                // 更新统计信息
                TaskInfo.ScanStage scanStage = taskInfo.getStages().getScan();
                scanStage.setTotalFiles(filePaths.size());
                scanStage.setScanEndTime(System.currentTimeMillis());
                scanStage.setScanDuration(scanStage.getScanEndTime() - scanStage.getScanStartTime());
                scanStage.setStatus("COMPLETED");
                storageService.saveScanStatistics(taskId, scanStage);
                
                // 更新任务状态
                taskInfo.setCurrentStage("SCANNED");
                taskInfo.setStatus(TaskInfo.TaskStatus.SCANNED);
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[INFO] [SCAN] 扫描完成，共 " + filePaths.size() + " 个文件");
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
                
                logger.info("[TaskExecution] 文件扫描完成: {}", taskId);
                
            } catch (Exception e) {
                logger.error("[TaskExecution] 文件扫描失败: {}", taskId, e);
                
                TaskInfo.ScanStage scanStage = taskInfo.getStages().getScan();
                scanStage.setScanEndTime(System.currentTimeMillis());
                scanStage.setScanDuration(scanStage.getScanEndTime() - scanStage.getScanStartTime());
                scanStage.setStatus("FAILED");
                
                taskInfo.setStatus(TaskInfo.TaskStatus.FAILED);
                taskInfo.setMessage("扫描失败: " + e.getMessage());
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[ERROR] [SCAN] 扫描失败: " + e.getMessage());
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
            }
        }

        public void executePreview() {
            logger.info("[TaskExecution] 开始预览分析: {}", taskId);
            
            // 更新任务状态
            taskInfo.setCurrentStage("PREVIEWING");
            taskInfo.setStatus(TaskInfo.TaskStatus.PREVIEWING);
            taskInfo.getStages().getPreview().setStatus("RUNNING");
            taskInfo.getStages().getPreview().setPreviewStartTime(System.currentTimeMillis());
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [PREVIEW] 开始预览分析");
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
            
            List<ChangeRecordPO> changeRecords = new ArrayList<>();
            
            try {
                // 流式读取扫描数据并分析
                Path scanDataPath = Paths.get(storageService.getTaskDirectory(taskId) + "/scan/data.json");
                logger.info("[TaskExecution] 开始读取扫描数据: {}", scanDataPath);
                logger.info("[TaskExecution] 文件是否存在: {}", Files.exists(scanDataPath));
                
                if (!Files.exists(scanDataPath)) {
                    throw new RuntimeException("扫描数据文件不存在: " + scanDataPath);
                }
                
                AtomicInteger processedCount = new AtomicInteger(0);
                AtomicInteger changedCount = new AtomicInteger(0);
                
                try (BufferedReader reader = Files.newBufferedReader(scanDataPath)) {
                    String line;
                    int lineNum = 0;
                    while ((line = reader.readLine()) != null) {
                        lineNum++;
                        if (cancelled) break;
                        
                        logger.debug("[TaskExecution] 读取第 {} 行: {}", lineNum, line);
                        
                        // 解析扫描记录
                        Map<String, Object> scanRecord = parseJson(line);
                        String filePath = (String) scanRecord.get("filePath");
                        String fileName = (String) scanRecord.get("fileName");
                        
                        logger.debug("[TaskExecution] 处理扫描记录: filePath={}, fileName={}, scanRecord={}", filePath, fileName, scanRecord);
                        
                        if (fileName == null || fileName.isEmpty()) {
                            logger.warn("[TaskExecution] fileName为空，跳过此记录: scanRecord={}", scanRecord);
                            continue;
                        }
                        
                        // 执行策略分析（简化处理）
                        Map<String, Object> previewRecord = new HashMap<>();
                        previewRecord.put("originalName", fileName);
                        previewRecord.put("newName", fileName);
                        previewRecord.put("filePath", filePath);
                        previewRecord.put("operationType", "NONE");
                        previewRecord.put("changed", false);
                        previewRecord.put("extraParams", new HashMap<>());
                        
                        // 写入预览数据
                        String jsonData = toJson(previewRecord);
                        storageService.writePreviewData(taskId, jsonData);
                        
                        // 创建变更记录
                        ChangeRecordPO changeRecord = new ChangeRecordPO();
                        changeRecord.setTaskId(taskId);
                        changeRecord.setRecordId(UUID.randomUUID().toString());
                        changeRecord.setOriginalName(fileName);
                        changeRecord.setNewName(fileName);
                        changeRecord.setFilePath(filePath);
                        changeRecord.setOperationType("NONE");
                        changeRecord.setStatus("PREVIEWED");
                        changeRecord.setChanged(false);
                        changeRecord.setSelected(false);
                        changeRecord.setCreatedAt(new Date());
                        changeRecords.add(changeRecord);
                        
                        processedCount.incrementAndGet();
                        
                        // 定期更新统计信息（每100条）
                        if (processedCount.get() % 100 == 0) {
                            updatePreviewStatistics(processedCount.get(), changedCount.get());
                        }
                    }
                }
                
                // 批量插入变更记录到数据库
                if (!changeRecords.isEmpty()) {
                    try {
                        changeRecordMapper.batchInsert(changeRecords);
                        logger.info("[TaskExecution] 已插入 {} 条变更记录到数据库", changeRecords.size());
                    } catch (Exception e) {
                        logger.error("[TaskExecution] 插入变更记录失败", e);
                    }
                }
                
                // 标记预览数据写入完成
                storageService.finishPreviewDataWriting(taskId);
                
                // 等待数据写入器完成写入
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 更新最终统计信息
                TaskInfo.PreviewStage previewStage = taskInfo.getStages().getPreview();
                previewStage.setTotalFiles(processedCount.get());
                previewStage.setProcessedFiles(processedCount.get());
                previewStage.setChangedFiles(changedCount.get());
                previewStage.setUnchangedFiles(processedCount.get() - changedCount.get());
                previewStage.setPreviewEndTime(System.currentTimeMillis());
                previewStage.setPreviewDuration(previewStage.getPreviewEndTime() - previewStage.getPreviewStartTime());
                previewStage.setStatus("COMPLETED");
                storageService.savePreviewStatistics(taskId, previewStage);
                
                // 更新任务状态
                taskInfo.setCurrentStage("PREVIEWED");
                taskInfo.setStatus(TaskInfo.TaskStatus.PREVIEWED);
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[INFO] [PREVIEW] 预览完成，共 " + processedCount.get() + " 个文件");
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
                
                logger.info("[TaskExecution] 预览分析完成: {}", taskId);
                
            } catch (Exception e) {
                logger.error("[TaskExecution] 预览分析失败: {}", taskId, e);
                
                TaskInfo.PreviewStage previewStage = taskInfo.getStages().getPreview();
                previewStage.setPreviewEndTime(System.currentTimeMillis());
                previewStage.setPreviewDuration(previewStage.getPreviewEndTime() - previewStage.getPreviewStartTime());
                previewStage.setStatus("FAILED");
                
                taskInfo.setStatus(TaskInfo.TaskStatus.FAILED);
                taskInfo.setMessage("预览失败: " + e.getMessage());
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[ERROR] [PREVIEW] 预览失败: " + e.getMessage());
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
            }
        }

        public void execute(int executionNum) {
            logger.info("[TaskExecution] 开始执行任务: {} - execution_{}", taskId, executionNum);
            
            // 更新任务状态
            taskInfo.setCurrentStage("EXECUTING");
            taskInfo.setStatus(TaskInfo.TaskStatus.EXECUTING);
            TaskInfo.ExecutionStage executionStage = taskInfo.getStages().getExecution();
            executionStage.setStatus("RUNNING");
            executionStage.setExecutionCount(executionNum);
            executionStage.setCurrentExecution("execution_" + String.format("%03d", executionNum));
            executionStage.setExecutionStartTime(System.currentTimeMillis());
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [EXECUTION] 开始执行任务: execution_" + String.format("%03d", executionNum));
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
            
            try {
                // 流式读取预览数据并执行
                Path previewDataPath = Paths.get(storageService.getTaskDirectory(taskId) + "/preview/data.json");
                AtomicInteger processedCount = new AtomicInteger(0);
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failedCount = new AtomicInteger(0);
                AtomicInteger skippedCount = new AtomicInteger(0);
                
                try (BufferedReader reader = Files.newBufferedReader(previewDataPath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (cancelled) break;
                        
                        // 解析预览记录
                        Map<String, Object> previewRecord = parseJson(line);
                        
                        // 执行变更（简化处理）
                        Map<String, Object> executionRecord = new HashMap<>(previewRecord);
                        executionRecord.put("status", "SUCCESS");
                        executionRecord.put("executionTime", System.currentTimeMillis());
                        executionRecord.put("errorMessage", null);
                        executionRecord.put("retryCount", 0);
                        
                        // 写入执行数据
                        String jsonData = toJson(executionRecord);
                        storageService.writeExecutionData(taskId, executionNum, jsonData);
                        
                        processedCount.incrementAndGet();
                        successCount.incrementAndGet();
                        
                        // 定期更新统计信息（每100条）
                        if (processedCount.get() % 100 == 0) {
                            updateExecutionStatistics(executionNum, processedCount.get(), successCount.get(), failedCount.get(), skippedCount.get());
                        }
                    }
                }
                
                // 标记执行数据写入完成
                storageService.finishExecutionDataWriting(taskId, executionNum);
                
                // 等待数据写入器完成写入
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 更新最终统计信息
                executionStage.setTotalFiles(processedCount.get());
                executionStage.setProcessedFiles(processedCount.get());
                executionStage.setSuccessCount(successCount.get());
                executionStage.setFailedCount(failedCount.get());
                executionStage.setSkippedCount(skippedCount.get());
                executionStage.setExecutionEndTime(System.currentTimeMillis());
                executionStage.setExecutionDuration(executionStage.getExecutionEndTime() - executionStage.getExecutionStartTime());
                executionStage.setStatus("COMPLETED");
                storageService.saveExecutionStatistics(taskId, executionNum, executionStage);
                
                // 更新任务状态
                taskInfo.setStatus(TaskInfo.TaskStatus.COMPLETED);
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[INFO] [EXECUTION] 执行完成: execution_" + String.format("%03d", executionNum));
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
                
                logger.info("[TaskExecution] 任务执行完成: {} - execution_{}", taskId, executionNum);
                
            } catch (Exception e) {
                logger.error("[TaskExecution] 任务执行失败: {} - execution_{}", taskId, executionNum, e);
                
                executionStage.setExecutionEndTime(System.currentTimeMillis());
                executionStage.setExecutionDuration(executionStage.getExecutionEndTime() - executionStage.getExecutionStartTime());
                executionStage.setStatus("FAILED");
                
                taskInfo.setStatus(TaskInfo.TaskStatus.FAILED);
                taskInfo.setMessage("执行失败: " + e.getMessage());
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[ERROR] [EXECUTION] 执行失败: " + e.getMessage());
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
            }
        }

        public void executeSelected(int executionNum, List<String> selectedRecordIds) {
            logger.info("[TaskExecution] 开始执行选中的记录: {} - execution_{}", taskId, executionNum);
            
            // 更新任务状态
            taskInfo.setCurrentStage("EXECUTING");
            taskInfo.setStatus(TaskInfo.TaskStatus.EXECUTING);
            TaskInfo.ExecutionStage executionStage = taskInfo.getStages().getExecution();
            executionStage.setStatus("RUNNING");
            executionStage.setExecutionCount(executionNum);
            executionStage.setCurrentExecution("execution_" + String.format("%03d", executionNum));
            executionStage.setExecutionStartTime(System.currentTimeMillis());
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [EXECUTION] 开始执行选中的记录: execution_" + String.format("%03d", executionNum));
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
            
            try {
                // 流式读取预览数据并执行选中的记录
                Path previewDataPath = Paths.get(storageService.getTaskDirectory(taskId) + "/preview/data.json");
                AtomicInteger processedCount = new AtomicInteger(0);
                AtomicInteger successCount = new AtomicInteger(0);
                AtomicInteger failedCount = new AtomicInteger(0);
                AtomicInteger skippedCount = new AtomicInteger(0);
                
                try (BufferedReader reader = Files.newBufferedReader(previewDataPath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (cancelled) break;
                        
                        // 解析预览记录
                        Map<String, Object> previewRecord = parseJson(line);
                        String recordId = (String) previewRecord.get("filePath");
                        
                        // 检查是否在选中列表中
                        if (!selectedRecordIds.contains(recordId)) {
                            skippedCount.incrementAndGet();
                            continue;
                        }
                        
                        // 执行变更（简化处理）
                        Map<String, Object> executionRecord = new HashMap<>(previewRecord);
                        executionRecord.put("status", "SUCCESS");
                        executionRecord.put("executionTime", System.currentTimeMillis());
                        executionRecord.put("errorMessage", null);
                        executionRecord.put("retryCount", 0);
                        
                        // 写入执行数据
                        String jsonData = toJson(executionRecord);
                        storageService.writeExecutionData(taskId, executionNum, jsonData);
                        
                        processedCount.incrementAndGet();
                        successCount.incrementAndGet();
                        
                        // 定期更新统计信息（每100条）
                        if (processedCount.get() % 100 == 0) {
                            updateExecutionStatistics(executionNum, processedCount.get(), successCount.get(), failedCount.get(), skippedCount.get());
                        }
                    }
                }
                
                // 更新最终统计信息
                executionStage.setTotalFiles(selectedRecordIds.size());
                executionStage.setProcessedFiles(processedCount.get());
                executionStage.setSuccessCount(successCount.get());
                executionStage.setFailedCount(failedCount.get());
                executionStage.setSkippedCount(skippedCount.get());
                executionStage.setExecutionEndTime(System.currentTimeMillis());
                executionStage.setExecutionDuration(executionStage.getExecutionEndTime() - executionStage.getExecutionStartTime());
                executionStage.setStatus("COMPLETED");
                storageService.saveExecutionStatistics(taskId, executionNum, executionStage);
                
                // 更新任务状态
                taskInfo.setStatus(TaskInfo.TaskStatus.COMPLETED);
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[INFO] [EXECUTION] 选中记录执行完成: execution_" + String.format("%03d", executionNum));
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
                
                logger.info("[TaskExecution] 选中记录执行完成: {} - execution_{}", taskId, executionNum);
                
            } catch (Exception e) {
                logger.error("[TaskExecution] 选中记录执行失败: {} - execution_{}", taskId, executionNum, e);
                
                executionStage.setExecutionEndTime(System.currentTimeMillis());
                executionStage.setExecutionDuration(executionStage.getExecutionEndTime() - executionStage.getExecutionStartTime());
                executionStage.setStatus("FAILED");
                
                taskInfo.setStatus(TaskInfo.TaskStatus.FAILED);
                taskInfo.setMessage("执行失败: " + e.getMessage());
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                storageService.saveTaskInfo(taskInfo);
                storageService.writeTaskLog(taskId, "[ERROR] [EXECUTION] 执行失败: " + e.getMessage());
                webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
                updateTaskInfoInDatabase(taskInfo);
            }
        }

        public void retryFailed(int executionNum) {
            logger.info("[TaskExecution] 开始重试失败的记录: {} - execution_{}", taskId, executionNum);
            
            // 获取上一次执行失败的记录
            int lastExecutionNum = executionNum - 1;
            List<String> failedRecordIds = new ArrayList<>();
            
            try {
                Path lastExecutionDataPath = Paths.get(storageService.getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", lastExecutionNum) + "/data.json");
                try (BufferedReader reader = Files.newBufferedReader(lastExecutionDataPath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Map<String, Object> record = parseJson(line);
                        if ("FAILED".equals(record.get("status"))) {
                            failedRecordIds.add((String) record.get("filePath"));
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("[TaskExecution] 读取上一次执行结果失败: {} - execution_{}", taskId, lastExecutionNum, e);
            }
            
            if (failedRecordIds.isEmpty()) {
                logger.info("[TaskExecution] 没有失败的记录需要重试: {} - execution_{}", taskId, executionNum);
                return;
            }
            
            // 执行选中的失败记录
            executeSelected(executionNum, failedRecordIds);
        }

        public void cancel() {
            this.cancelled = true;
            
            // 更新任务状态为已取消
            taskInfo.setStatus(TaskInfo.TaskStatus.CANCELLED);
            taskInfo.setMessage("任务已取消");
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [CANCEL] 任务已取消");
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
        }

        public void cancelStage(String stageType) {
            this.cancelled = true;
            
            String stageName = "";
            switch (stageType.toUpperCase()) {
                case "SCAN":
                    stageName = "扫描";
                    taskInfo.getStages().getScan().setStatus("CANCELLED");
                    taskInfo.getStages().getScan().setScanEndTime(System.currentTimeMillis());
                    taskInfo.getStages().getScan().setScanDuration(
                        taskInfo.getStages().getScan().getScanEndTime() - 
                        taskInfo.getStages().getScan().getScanStartTime()
                    );
                    taskInfo.setCurrentStage("SCANNED");
                    taskInfo.setStatus(TaskInfo.TaskStatus.SCANNED);
                    break;
                case "PREVIEW":
                    stageName = "预览";
                    taskInfo.getStages().getPreview().setStatus("CANCELLED");
                    taskInfo.getStages().getPreview().setPreviewEndTime(System.currentTimeMillis());
                    taskInfo.getStages().getPreview().setPreviewDuration(
                        taskInfo.getStages().getPreview().getPreviewEndTime() - 
                        taskInfo.getStages().getPreview().getPreviewStartTime()
                    );
                    taskInfo.setCurrentStage("PREVIEWED");
                    taskInfo.setStatus(TaskInfo.TaskStatus.PREVIEWED);
                    break;
                case "EXECUTION":
                    stageName = "执行";
                    taskInfo.getStages().getExecution().setStatus("CANCELLED");
                    taskInfo.getStages().getExecution().setExecutionEndTime(System.currentTimeMillis());
                    taskInfo.getStages().getExecution().setExecutionDuration(
                        taskInfo.getStages().getExecution().getExecutionEndTime() - 
                        taskInfo.getStages().getExecution().getExecutionStartTime()
                    );
                    taskInfo.setCurrentStage("COMPLETED");
                    taskInfo.setStatus(TaskInfo.TaskStatus.COMPLETED);
                    break;
                default:
                    logger.warn("[TaskExecution] 无效的阶段类型: {}", stageType);
                    return;
            }
            
            taskInfo.setMessage(stageName + "已终止");
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            storageService.saveTaskInfo(taskInfo);
            storageService.writeTaskLog(taskId, "[INFO] [CANCEL] " + stageName + "已终止");
            webSocketService.sendTaskInfoUpdate(taskId, taskInfo);
            updateTaskInfoInDatabase(taskInfo);
            
            logger.info("[TaskExecution] 阶段已终止: taskId={}, stageType={}", taskId, stageType);
        }

        public Future<?> getFuture() {
            return future;
        }

        public void setFuture(Future<?> future) {
            this.future = future;
        }

        private List<String> scanFiles() {
            List<String> filePaths = new ArrayList<>();
            TaskConfigSnapshot configSnapshot = taskInfo.getConfigSnapshot();
            
            for (TaskConfigSnapshot.SourceDirectoryConfig dirConfig : configSnapshot.getSourceDirectories()) {
                scanDirectory(dirConfig.getPath(), dirConfig.isRecursive(), dirConfig.getDepth(), filePaths);
            }
            
            return filePaths;
        }

        private void scanDirectory(String path, boolean recursive, int depth, List<String> filePaths) {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }
            
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            
            for (File file : files) {
                if (file.isDirectory() && recursive && depth > 0) {
                    scanDirectory(file.getAbsolutePath(), recursive, depth - 1, filePaths);
                } else if (file.isFile()) {
                    filePaths.add(file.getAbsolutePath());
                }
            }
        }

        private void updatePreviewStatistics(int processedCount, int changedCount) {
            TaskInfo.PreviewStage previewStage = taskInfo.getStages().getPreview();
            previewStage.setProcessedFiles(processedCount);
            previewStage.setChangedFiles(changedCount);
            previewStage.setUnchangedFiles(processedCount - changedCount);
            storageService.savePreviewStatistics(taskId, previewStage);
        }

        private void updateExecutionStatistics(int executionNum, int processedCount, int successCount, int failedCount, int skippedCount) {
            TaskInfo.ExecutionStage executionStage = taskInfo.getStages().getExecution();
            executionStage.setProcessedFiles(processedCount);
            executionStage.setSuccessCount(successCount);
            executionStage.setFailedCount(failedCount);
            executionStage.setSkippedCount(skippedCount);
            storageService.saveExecutionStatistics(taskId, executionNum, executionStage);
        }

        private String toJson(Map<String, Object> map) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    sb.append("\"").append(value).append("\"");
                } else {
                    sb.append(value);
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }

        private Map<String, Object> parseJson(String json) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                logger.error("[TaskExecution] 解析JSON失败: {}", json, e);
                return new HashMap<>();
            }
        }
    }
}
