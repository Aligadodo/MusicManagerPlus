package com.filemanager.backend.service;

import com.filemanager.backend.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务文件存储服务
 * 负责任务快照、预览结果、执行结果的持久化存储
 */
@Service
public class TaskFileStorageService {

    private static final String BASE_DIR = System.getProperty("user.home") + "/.MusicManagerPlus/tasks";
    private static final String SNAPSHOTS_DIR = BASE_DIR + "/snapshots";
    private static final String PREVIEWS_DIR = BASE_DIR + "/previews";
    private static final String EXECUTIONS_DIR = BASE_DIR + "/executions";
    private static final String PROGRESS_DIR = BASE_DIR + "/progress";
    private static final String LOGS_DIR = System.getProperty("user.home") + "/.MusicManagerPlus/logs";

    private final ObjectMapper objectMapper;

    public TaskFileStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        initializeDirectories();
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
            Files.createDirectories(Paths.get(SNAPSHOTS_DIR));
            Files.createDirectories(Paths.get(PREVIEWS_DIR));
            Files.createDirectories(Paths.get(EXECUTIONS_DIR));
            Files.createDirectories(Paths.get(PROGRESS_DIR));
            Files.createDirectories(Paths.get(LOGS_DIR));
            System.out.println("[TaskFileStorage] 目录初始化完成");
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 目录初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存任务快照
     */
    public void saveTaskSnapshot(TaskSnapshot snapshot) {
        try {
            String filePath = SNAPSHOTS_DIR + "/" + snapshot.getTaskId() + ".json";
            objectMapper.writeValue(new File(filePath), snapshot);
            System.out.println("[TaskFileStorage] 任务快照已保存: " + snapshot.getTaskId());
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 保存任务快照失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载任务快照
     */
    public TaskSnapshot loadTaskSnapshot(String taskId) {
        try {
            String filePath = SNAPSHOTS_DIR + "/" + taskId + ".json";
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("[TaskFileStorage] 任务快照不存在: " + taskId);
                return null;
            }
            return objectMapper.readValue(file, TaskSnapshot.class);
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 加载任务快照失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除任务快照
     */
    public boolean deleteTaskSnapshot(String taskId) {
        try {
            String filePath = SNAPSHOTS_DIR + "/" + taskId + ".json";
            File file = new File(filePath);
            if (file.exists()) {
                boolean deleted = file.delete();
                System.out.println("[TaskFileStorage] 任务快照已删除: " + taskId + ", 结果: " + deleted);
                return deleted;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[TaskFileStorage] 删除任务快照失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存预览结果
     */
    public void savePreviewResult(PreviewResult previewResult) {
        try {
            previewResult.setCompletedAt(System.currentTimeMillis());
            String filePath = PREVIEWS_DIR + "/" + previewResult.getTaskId() + ".json";
            objectMapper.writeValue(new File(filePath), previewResult);
            System.out.println("[TaskFileStorage] 预览结果已保存: " + previewResult.getTaskId());
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 保存预览结果失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载预览结果
     */
    public PreviewResult loadPreviewResult(String taskId) {
        try {
            String filePath = PREVIEWS_DIR + "/" + taskId + ".json";
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("[TaskFileStorage] 预览结果不存在: " + taskId);
                return null;
            }
            return objectMapper.readValue(file, PreviewResult.class);
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 加载预览结果失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取预览结果列表
     */
    public List<PreviewResult> loadAllPreviewResults() {
        try {
            File dir = new File(PREVIEWS_DIR);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files == null) {
                return new ArrayList<>();
            }

            List<PreviewResult> results = new ArrayList<>();
            for (File file : files) {
                try {
                    PreviewResult result = objectMapper.readValue(file, PreviewResult.class);
                    results.add(result);
                } catch (IOException e) {
                    System.err.println("[TaskFileStorage] 加载预览结果失败: " + file.getName());
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("[TaskFileStorage] 加载预览结果列表失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 保存执行结果
     */
    public void saveExecutionResult(ExecutionResult executionResult) {
        try {
            executionResult.setCompletedAt(System.currentTimeMillis());
            executionResult.setDuration(executionResult.getCompletedAt() - executionResult.getStartedAt());
            String filePath = EXECUTIONS_DIR + "/" + executionResult.getTaskId() + ".json";
            objectMapper.writeValue(new File(filePath), executionResult);
            System.out.println("[TaskFileStorage] 执行结果已保存: " + executionResult.getTaskId());
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 保存执行结果失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载执行结果
     */
    public ExecutionResult loadExecutionResult(String taskId) {
        try {
            String filePath = EXECUTIONS_DIR + "/" + taskId + ".json";
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("[TaskFileStorage] 执行结果不存在: " + taskId);
                return null;
            }
            return objectMapper.readValue(file, ExecutionResult.class);
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 加载执行结果失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取执行结果列表
     */
    public List<ExecutionResult> loadAllExecutionResults() {
        try {
            File dir = new File(EXECUTIONS_DIR);
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files == null) {
                return new ArrayList<>();
            }

            List<ExecutionResult> results = new ArrayList<>();
            for (File file : files) {
                try {
                    ExecutionResult result = objectMapper.readValue(file, ExecutionResult.class);
                    results.add(result);
                } catch (IOException e) {
                    System.err.println("[TaskFileStorage] 加载执行结果失败: " + file.getName());
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("[TaskFileStorage] 加载执行结果列表失败: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 保存任务进度
     */
    public void saveTaskProgress(String taskId, TaskProgress progress) {
        try {
            String filePath = PROGRESS_DIR + "/" + taskId + ".json";
            objectMapper.writeValue(new File(filePath), progress);
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 保存任务进度失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 加载任务进度
     */
    public TaskProgress loadTaskProgress(String taskId) {
        try {
            String filePath = PROGRESS_DIR + "/" + taskId + ".json";
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, TaskProgress.class);
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 加载任务进度失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取任务日志文件
     */
    public File getTaskLogFile(String taskId) {
        return new File(LOGS_DIR + "/" + taskId + ".log");
    }

    /**
     * 写入任务日志
     */
    public void writeTaskLog(String taskId, String message) {
        try {
            File logFile = getTaskLogFile(taskId);
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(message + "\n");
            }
        } catch (IOException e) {
            System.err.println("[TaskFileStorage] 写入任务日志失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 任务进度模型
     */
    public static class TaskProgress {
        private String taskId;
        private String status;
        private double progress;
        private String message;
        private int totalFiles;
        private int processedFiles;
        private int successCount;
        private int failedCount;
        private int skippedCount;
        private long updatedAt;

        public TaskProgress() {
        }

        public TaskProgress(String taskId) {
            this.taskId = taskId;
            this.updatedAt = System.currentTimeMillis();
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public double getProgress() {
            return progress;
        }

        public void setProgress(double progress) {
            this.progress = progress;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
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

        public long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
