package com.filemanager.backend.service.impl;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
import com.filemanager.domain.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        TaskExecution execution = tasks.get(taskId);
        if (execution == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }

        if (execution.getStatus().getStatus().equals("RUNNING")) {
            throw new IllegalStateException("Task is already running");
        }

        Future<?> future = executorService.submit(() -> {
            try {
                execution.execute(strategyService);
            } catch (Exception e) {
                execution.setError(e.getMessage());
            }
        });

        execution.setFuture(future);
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
            status.setStatus("RUNNING");
            status.setProgress(0);
            status.setMessage("Task started");

            try {
                // 执行任务逻辑
                results.addAll(strategyService.executeStrategy(
                        request.getStrategyId(),
                        request.getFilePaths(),
                        strategyService.getStrategyConfig(request.getStrategyId())
                ));

                status.setStatus("COMPLETED");
                status.setProgress(100);
                status.setMessage("Task completed successfully");
                status.setEndTime(System.currentTimeMillis());
            } catch (Exception e) {
                status.setStatus("FAILED");
                status.setMessage("Task failed: " + e.getMessage());
                status.setEndTime(System.currentTimeMillis());
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
