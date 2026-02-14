package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.domain.service.TaskService;
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

    private final Map<String, TaskExecution> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Autowired
    private StrategyService strategyService;

    @Override
    public String createTask(TaskRequestDTO request) {
        String taskId = "task-" + System.currentTimeMillis();
        TaskExecution execution = new TaskExecution(taskId, request);
        tasks.put(taskId, execution);
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
        System.out.println("[Task] 开始执行任务: " + taskId);
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            System.out.println("[Task] 任务不存在: " + taskId);
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (execution.getStatus().getStatus().equals("RUNNING")) {
            System.out.println("[Task] 任务已经在运行中: " + taskId);
            throw new IllegalStateException("Task is already running");
        }

        System.out.println("[Task] 提交任务到执行线程池: " + taskId);
        Future<?> future = executorService.submit(() -> {
            System.out.println("[Task] 任务开始执行: " + taskId);
            try {
                execution.execute(strategyService);
                System.out.println("[Task] 任务执行完成: " + taskId + ", 状态: " + execution.getStatus().getStatus());
            } catch (Exception e) {
                System.err.println("[Task] 任务执行异常: " + taskId + ", 错误: " + e.getMessage());
                execution.setError(e.getMessage());
            }
        });

        execution.setFuture(future);
        System.out.println("[Task] 任务提交成功: " + taskId);
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

        execution.getStatus().setStatus("CANCELLED");
        execution.getStatus().setMessage("Task cancelled");
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
        return tasks.remove(taskId) != null;
    }

    @Override
    public boolean isTaskRunning() {
        for (TaskExecution execution : tasks.values()) {
            if (execution.getStatus().getStatus().equals("RUNNING")) {
                return true;
            }
        }
        return false;
    }

    private static class TaskExecution {
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
            this.status.setStatus("PENDING");
            this.status.setProgress(0);
            this.status.setStartTime(System.currentTimeMillis());
            this.results = new ArrayList<>();
        }

        public void execute(StrategyService strategyService) {
            System.out.println("[TaskExecution] 开始执行任务: " + taskId);
            
            List<String> filePaths = new java.util.ArrayList<>();
            if (request.getSourceDirectories() != null) {
                for (TaskRequestDTO.SourceDirectoryDTO sourceDir : request.getSourceDirectories()) {
                    filePaths.add(sourceDir.getPath());
                }
            }
            
            String strategyId = request.getPipelineId() != null ? request.getPipelineId() : "default-pipeline";
            
            System.out.println("[TaskExecution] 策略ID: " + strategyId);
            System.out.println("[TaskExecution] 文件数量: " + filePaths.size());
            
            status.setStatus("RUNNING");
            status.setProgress(0);
            status.setMessage("Task started");
            status.setTotalFiles(filePaths.size());
            status.setProcessedFiles(0);
            status.setSuccessCount(0);
            status.setFailedCount(0);
            status.setSkippedCount(0);
            status.setOperationStats(new HashMap<>());

            try {
                System.out.println("[TaskExecution] 开始获取策略配置: " + strategyId);
                com.filemanager.domain.service.StrategyService strategyServiceLocal = strategyService;
                com.filemanager.domain.dto.StrategyConfigDTO config = strategyServiceLocal.getStrategyConfig(strategyId);
                System.out.println("[TaskExecution] 策略配置获取成功: " + strategyId);
                
                System.out.println("[TaskExecution] 开始执行策略: " + strategyId);
                // 执行任务逻辑
                java.util.List<com.filemanager.domain.entity.ChangeRecord> executionResults = strategyServiceLocal.executeStrategy(
                        strategyId,
                        filePaths,
                        config
                );
                System.out.println("[TaskExecution] 策略执行完成: " + strategyId + ", 结果数量: " + (executionResults != null ? executionResults.size() : 0));
                
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
                    
                    System.out.println("[TaskExecution] 结果添加完成: " + results.size() + " 条记录");
                    System.out.println("[TaskExecution] 统计信息 - 成功: " + status.getSuccessCount() + ", 失败: " + status.getFailedCount() + ", 跳过: " + status.getSkippedCount());
                }

                System.out.println("[TaskExecution] 任务执行成功: " + taskId);
                status.setStatus("COMPLETED");
                status.setProgress(100);
                status.setMessage("Task completed successfully");
                status.setEndTime(System.currentTimeMillis());
                
                System.out.println("[TaskExecution] 任务执行完成: " + taskId);
                System.out.println("[TaskExecution] 最终状态: COMPLETED");
                System.out.println("[TaskExecution] 处理文件数: " + filePaths.size());
                System.out.println("[TaskExecution] 生成结果数: " + results.size());
            } catch (Exception e) {
                System.err.println("[TaskExecution] 任务执行失败: " + taskId);
                System.err.println("[TaskExecution] 错误信息: " + e.getMessage());
                e.printStackTrace();
                
                status.setStatus("FAILED");
                status.setMessage("Task failed: " + e.getMessage());
                status.setEndTime(System.currentTimeMillis());
                
                System.out.println("[TaskExecution] 任务最终状态: FAILED");
            }
        }

        public void setError(String errorMessage) {
            status.setStatus("FAILED");
            status.setMessage(errorMessage);
            status.setEndTime(System.currentTimeMillis());
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
