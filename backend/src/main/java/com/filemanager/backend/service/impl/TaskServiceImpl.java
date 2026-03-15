package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.domain.service.TaskService;
import com.filemanager.backend.storage.DatabaseTaskStorage;
import com.filemanager.backend.model.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final Map<String, TaskExecution> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    
    private final StrategyService strategyService;
    private final DatabaseTaskStorage taskStorage;
    
    @Autowired
    public TaskServiceImpl(StrategyService strategyService, DatabaseTaskStorage taskStorage) {
        this.strategyService = strategyService;
        this.taskStorage = taskStorage;
        // 实现动态线程池配置
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;
        this.executorService = new java.util.concurrent.ThreadPoolExecutor(
            corePoolSize,
            maxPoolSize,
            keepAliveTime,
            java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(),
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        // 初始化时从数据库加载任务
        loadTasksFromDatabase();
    }
    
    private void loadTasksFromDatabase() {
        try {
            List<String> taskIds = taskStorage.getAllTaskIds();
            for (String taskId : taskIds) {
                TaskInfo taskInfo = taskStorage.loadTaskInfo(taskId);
                if (taskInfo != null) {
                    // 这里需要根据实际情况创建TaskExecution对象
                    // 由于TaskExecution需要TaskRequestDTO，这里简化处理
                    // 实际项目中需要从数据库加载完整的任务信息
                }
            }
            logger.info("从数据库加载任务完成，共加载 {} 个任务", taskIds.size());
        } catch (Exception e) {
            logger.error("从数据库加载任务失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public String createTask(TaskRequestDTO request) {
        String taskId = "task-" + java.util.UUID.randomUUID().toString();
        TaskExecution execution = new TaskExecution(taskId, request);
        tasks.put(taskId, execution);
        
        // 持久化任务信息到数据库
        try {
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setTaskId(taskId);
            taskInfo.setTaskName(request.getTaskName() != null ? request.getTaskName() : "Unnamed Task");
            taskInfo.setStatus(TaskInfo.TaskStatus.CREATED);
            taskInfo.setCurrentStage("CREATED");
            taskInfo.setOverallProgress(0.0);
            taskInfo.setMessage("Task created");
            taskInfo.setCreatedAt(System.currentTimeMillis());
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            
            taskStorage.saveTaskInfo(taskInfo);
            logger.info("任务已持久化到数据库: {}", taskId);
        } catch (Exception e) {
            logger.error("持久化任务失败: {}", e.getMessage(), e);
        }
        
        return taskId;
    }

    @Override
    public TaskStatusDTO getTaskStatus(String taskId) {
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        return execution.getStatus();
    }

    @Override
    public List<TaskStatusDTO> getTasks(String status, int page, int size) {
        List<TaskStatusDTO> result = new ArrayList<>();
        for (TaskExecution execution : tasks.values()) {
            TaskStatusDTO taskStatus = execution.getStatus();
            if (status == null || taskStatus.getStatus().equals(status)) {
                result.add(taskStatus);
            }
        }
        // 简单的分页处理
        int start = (page - 1) * size;
        int end = Math.min(start + size, result.size());
        if (start < result.size()) {
            return result.subList(start, end);
        }
        return new ArrayList<>();
    }

    @Override
    public boolean executeTask(String taskId) {
        logger.info("开始执行任务: {}", taskId);
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            logger.warn("任务不存在: {}", taskId);
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (execution.getStatus().getStatus().equals(TaskInfo.TaskStatus.EXECUTING.name())) {
            logger.warn("任务已经在运行中: {}", taskId);
            throw new IllegalStateException("Task is already running");
        }

        logger.info("提交任务到执行线程池: {}", taskId);
        Future<?> future = executorService.submit(() -> {
            logger.info("任务开始执行: {}", taskId);
            try {
                execution.execute(strategyService);
                logger.info("任务执行完成: {}, 状态: {}", taskId, execution.getStatus().getStatus());
            } catch (Exception e) {
                logger.error("任务执行异常: {}, 错误: {}", taskId, e.getMessage(), e);
                execution.setError(e.getMessage());
            }
        });

        execution.setFuture(future);
        logger.info("任务提交成功: {}", taskId);
        return true;
    }

    @Override
    public boolean cancelTask(String taskId) {
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            return false;
        }

        Future<?> future = execution.getFuture();
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }

        execution.getStatus().setStatus(TaskInfo.TaskStatus.CANCELLED.name());
        execution.getStatus().setMessage("Task cancelled");
        
        // 更新数据库中的任务状态
        try {
            TaskInfo taskInfo = new TaskInfo();
            taskInfo.setTaskId(taskId);
            taskInfo.setStatus(TaskInfo.TaskStatus.CANCELLED);
            taskInfo.setCurrentStage(TaskInfo.TaskStatus.CANCELLED.name());
            taskInfo.setMessage("Task cancelled");
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            
            taskStorage.saveTaskInfo(taskInfo);
            logger.info("任务已取消并更新到数据库: {}", taskId);
        } catch (Exception e) {
            logger.error("更新取消状态失败: {}", e.getMessage(), e);
        }
        
        return true;
    }

    @Override
    public List<ChangeRecord> getTaskResults(String taskId) {
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            return new ArrayList<>();
        }
        return execution.getResults();
    }

    @Override
    public boolean deleteTask(String taskId) {
        boolean removed = tasks.remove(taskId) != null;
        
        // 从数据库中删除任务
        if (removed) {
            try {
                taskStorage.deleteTask(taskId);
                logger.info("任务已从数据库中删除: {}", taskId);
            } catch (Exception e) {
                logger.error("从数据库删除任务失败: {}", e.getMessage(), e);
            }
        }
        
        return removed;
    }

    @Override
    public boolean isTaskRunning() {
        for (TaskExecution execution : tasks.values()) {
            String status = execution.getStatus().getStatus();
            if (status.equals(TaskInfo.TaskStatus.SCANNING.name()) ||
                status.equals(TaskInfo.TaskStatus.PREVIEWING.name()) ||
                status.equals(TaskInfo.TaskStatus.EXECUTING.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int clearAllTasks() {
        int count = tasks.size();
        tasks.clear();
        return count;
    }

    private class TaskExecution {
        private final String taskId;
        private final TaskRequestDTO request;
        private final TaskStatusDTO status;
        private Future<?> future;
        private final List<ChangeRecord> results;

        public TaskExecution(String taskId, TaskRequestDTO request) {
            this.taskId = taskId;
            this.request = request;
            this.status = new TaskStatusDTO();
            this.status.setTaskId(taskId);
            this.status.setStatus(TaskInfo.TaskStatus.CREATED.name());
            this.status.setProgress(0);
            this.status.setStartTime(System.currentTimeMillis());
            this.results = new ArrayList<>();
        }

        public void execute(StrategyService strategyService) {
            logger.info("开始执行任务: {}", taskId);
            
            List<String> filePaths = new java.util.ArrayList<>();
            if (request.getSourceDirectories() != null) {
                for (TaskRequestDTO.SourceDirectoryDTO sourceDir : request.getSourceDirectories()) {
                    filePaths.add(sourceDir.getPath());
                }
            }
            
            String strategyId = request.getPipelineId() != null ? request.getPipelineId() : "default-pipeline";
            
            logger.info("策略ID: {}, 文件数量: {}", strategyId, filePaths.size());
            
            status.setStatus(TaskInfo.TaskStatus.EXECUTING.name());
            status.setProgress(0);
            status.setMessage("Task started");
            status.setTotalFiles(filePaths.size());
            status.setProcessedFiles(0);
            status.setSuccessCount(0);
            status.setFailedCount(0);
            status.setSkippedCount(0);
            status.setOperationStats(new HashMap<>());
            
            // 更新数据库中的任务状态
            updateTaskStatusInDatabase(TaskInfo.TaskStatus.EXECUTING, "Task started");

            // 获取超时时间（毫秒），默认60分钟
            long timeoutMs = 60 * 60 * 1000;
            if (request.getGlobalSettings() != null && request.getGlobalSettings().getTimeout() != null) {
                timeoutMs = request.getGlobalSettings().getTimeout();
            }
            logger.info("任务超时设置: {} 毫秒", timeoutMs);

            // 创建一个内部任务来执行策略
            Future<?> executionFuture = executorService.submit(() -> {
                try {
                    logger.info("开始获取策略配置: {}", strategyId);
                    com.filemanager.domain.service.StrategyService strategyServiceLocal = strategyService;
                    com.filemanager.domain.dto.StrategyConfigDTO config = strategyServiceLocal.getStrategyConfig(strategyId);
                    logger.info("策略配置获取成功: {}", strategyId);
                    
                    logger.info("开始执行策略: {}", strategyId);
                    // 执行任务逻辑
                    java.util.List<com.filemanager.domain.entity.ChangeRecord> executionResults = strategyServiceLocal.executeStrategy(
                            strategyId,
                            filePaths,
                            config
                    );
                    logger.info("策略执行完成: {}, 结果数量: {}", strategyId, (executionResults != null ? executionResults.size() : 0));
                    
                    // 检查是否找到策略
                    if (executionResults == null || executionResults.isEmpty()) {
                        throw new Exception("Strategy not found: " + strategyId);
                    }
                    
                    if (executionResults != null) {
                        results.addAll(executionResults);
                        
                        // 统计结果
                        Map<String, Integer> operationStats = new HashMap<>();
                        for (ChangeRecord record : executionResults) {
                            String opType = record.getOperationType();
                            operationStats.put(opType, operationStats.getOrDefault(opType, 0) + 1);
                            
                            String recordStatus = record.getStatus();
                            if (ExecStatus.SUCCESS.name().equals(recordStatus)) {
                                status.setSuccessCount(status.getSuccessCount() + 1);
                            } else if (ExecStatus.FAILED.name().equals(recordStatus)) {
                                status.setFailedCount(status.getFailedCount() + 1);
                            } else if ("SKIPPED".equals(recordStatus)) {
                                status.setSkippedCount(status.getSkippedCount() + 1);
                            }
                        }
                        status.setOperationStats(operationStats);
                        status.setProcessedFiles(executionResults.size());
                        
                        // 保存变更记录到数据库
                        try {
                            taskStorage.saveChangeRecords(taskId, executionResults);
                            logger.info("变更记录已保存到数据库: {} 条", executionResults.size());
                        } catch (Exception e) {
                            logger.error("保存变更记录失败: {}", e.getMessage(), e);
                        }
                        
                        logger.info("结果添加完成: {} 条记录", results.size());
                        logger.info("统计信息 - 成功: {}, 失败: {}, 跳过: {}", 
                            status.getSuccessCount(), status.getFailedCount(), status.getSkippedCount());
                    }

                    logger.info("任务执行成功: {}", taskId);
                    status.setStatus(TaskInfo.TaskStatus.COMPLETED.name());
                    status.setProgress(100);
                    status.setMessage("Task completed successfully");
                    status.setEndTime(System.currentTimeMillis());
                    
                    // 更新数据库中的任务状态
                    updateTaskStatusInDatabase(TaskInfo.TaskStatus.COMPLETED, "Task completed successfully");
                    
                    logger.info("任务执行完成: {}, 最终状态: {}, 处理文件数: {}, 生成结果数: {}", 
                        taskId, TaskInfo.TaskStatus.COMPLETED.name(), 
                        filePaths.size(), results.size());
                } catch (Exception e) {
                    logger.error("任务执行失败: {}, 错误信息: {}", taskId, e.getMessage(), e);
                    
                    status.setStatus(TaskInfo.TaskStatus.FAILED.name());
                    status.setMessage("Task failed: " + e.getMessage());
                    status.setEndTime(System.currentTimeMillis());
                    
                    // 更新数据库中的任务状态
                    updateTaskStatusInDatabase(TaskInfo.TaskStatus.FAILED, "Task failed: " + e.getMessage());
                    
                    logger.info("任务最终状态: {}", TaskInfo.TaskStatus.FAILED.name());
                }
            });

            // 等待任务执行，设置超时
            try {
                executionFuture.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                logger.error("任务执行超时: {}, 超过 {} 毫秒", taskId, timeoutMs);
                executionFuture.cancel(true);
                
                status.setStatus(TaskInfo.TaskStatus.FAILED.name());
                status.setMessage("Task timeout after " + timeoutMs + " ms");
                status.setEndTime(System.currentTimeMillis());
                
                // 更新数据库中的任务状态
                updateTaskStatusInDatabase(TaskInfo.TaskStatus.FAILED, "Task timeout after " + timeoutMs + " ms");
                
                logger.info("任务最终状态: {}", TaskInfo.TaskStatus.FAILED.name());
            } catch (Exception e) {
                logger.error("等待任务执行时发生异常: {}, 错误信息: {}", taskId, e.getMessage(), e);
            }
        }

        public void setError(String errorMessage) {
            status.setStatus(TaskInfo.TaskStatus.FAILED.name());
            status.setMessage(errorMessage);
            status.setEndTime(System.currentTimeMillis());
            
            // 更新数据库中的任务状态
            updateTaskStatusInDatabase(TaskInfo.TaskStatus.FAILED, errorMessage);
        }

        private void updateTaskStatusInDatabase(TaskInfo.TaskStatus taskStatus, String message) {
            try {
                TaskInfo taskInfo = new TaskInfo();
                taskInfo.setTaskId(taskId);
                taskInfo.setStatus(taskStatus);
                taskInfo.setCurrentStage(taskStatus.name());
                taskInfo.setOverallProgress(status.getProgress());
                taskInfo.setMessage(message);
                taskInfo.setUpdatedAt(System.currentTimeMillis());
                
                taskStorage.saveTaskInfo(taskInfo);
                logger.info("任务状态已更新到数据库: {} - {}", taskId, taskStatus.name());
            } catch (Exception e) {
                logger.error("更新任务状态失败: {}", e.getMessage(), e);
            }
        }

        public TaskStatusDTO getStatus() {
            return status;
        }

        public Future<?> getFuture() {
            return future;
        }

        public void setFuture(Future<?> future) {
            this.future = future;
        }

        public List<ChangeRecord> getResults() {
            return results;
        }
    }
}
