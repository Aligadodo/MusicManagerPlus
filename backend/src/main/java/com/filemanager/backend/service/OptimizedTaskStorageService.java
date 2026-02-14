package com.filemanager.backend.service;

import com.filemanager.backend.model.*;
import com.filemanager.domain.entity.ChangeRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 优化的任务文件存储服务
 * 支持数据与统计信息分离存储、流式处理、多阶段结果展示
 */
@Service
public class OptimizedTaskStorageService {

    private static final Logger logger = LoggerFactory.getLogger(OptimizedTaskStorageService.class);
    private static final String BASE_DIR = System.getProperty("user.home") + "/.MusicManagerPlus/tasks";

    private final ObjectMapper objectMapper;
    private final ExecutorService writeExecutor;
    private final Map<String, BlockingQueue<String>> writeQueues = new ConcurrentHashMap<>();

    public OptimizedTaskStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.writeExecutor = Executors.newFixedThreadPool(5);
        initializeBaseDirectory();
    }

    private void initializeBaseDirectory() {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
            logger.info("[OptimizedTaskStorage] 基础目录初始化完成: {}", BASE_DIR);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 基础目录初始化失败", e);
        }
    }

    /**
     * 初始化任务目录结构
     */
    public void initializeTaskDirectory(String taskId) {
        try {
            Path taskPath = Paths.get(getTaskDirectory(taskId));
            Files.createDirectories(taskPath);
            
            Files.createDirectories(taskPath.resolve("scan"));
            Files.createDirectories(taskPath.resolve("preview"));
            Files.createDirectories(taskPath.resolve("execution"));
            
            logger.info("[OptimizedTaskStorage] 任务目录初始化完成: {}", taskId);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 任务目录初始化失败: {}", taskId, e);
        }
    }

    /**
     * 获取任务目录路径
     */
    public String getTaskDirectory(String taskId) {
        return BASE_DIR + "/" + taskId;
    }

    /**
     * 保存任务信息
     */
    public void saveTaskInfo(TaskInfo taskInfo) {
        try {
            String filePath = getTaskDirectory(taskInfo.getTaskId()) + "/task.json";
            objectMapper.writeValue(new File(filePath), taskInfo);
            logger.debug("[OptimizedTaskStorage] 任务信息已保存: {}", taskInfo.getTaskId());
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 保存任务信息失败: {}", taskInfo.getTaskId(), e);
        }
    }

    /**
     * 加载任务信息
     */
    public TaskInfo loadTaskInfo(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/task.json";
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("[OptimizedTaskStorage] 任务信息不存在: {}", taskId);
                return null;
            }
            return objectMapper.readValue(file, TaskInfo.class);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 加载任务信息失败: {}", taskId, e);
            return null;
        }
    }

    /**
     * 保存配置快照
     */
    public void saveConfigSnapshot(String taskId, TaskConfigSnapshot configSnapshot) {
        try {
            String filePath = getTaskDirectory(taskId) + "/config.json";
            objectMapper.writeValue(new File(filePath), configSnapshot);
            logger.debug("[OptimizedTaskStorage] 配置快照已保存: {}", taskId);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 保存配置快照失败: {}", taskId, e);
        }
    }

    /**
     * 加载配置快照
     */
    public TaskConfigSnapshot loadConfigSnapshot(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/config.json";
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("[OptimizedTaskStorage] 配置快照不存在: {}", taskId);
                return null;
            }
            return objectMapper.readValue(file, TaskConfigSnapshot.class);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 加载配置快照失败: {}", taskId, e);
            return null;
        }
    }

    /**
     * 保存扫描统计信息
     */
    public void saveScanStatistics(String taskId, TaskInfo.ScanStage statistics) {
        try {
            String filePath = getTaskDirectory(taskId) + "/scan/statistics.json";
            objectMapper.writeValue(new File(filePath), statistics);
            logger.debug("[OptimizedTaskStorage] 扫描统计已保存: {}", taskId);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 保存扫描统计失败: {}", taskId, e);
        }
    }

    /**
     * 加载扫描统计信息
     */
    public TaskInfo.ScanStage loadScanStatistics(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/scan/statistics.json";
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, TaskInfo.ScanStage.class);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 加载扫描统计失败: {}", taskId, e);
            return null;
        }
    }

    /**
     * 保存预览统计信息
     */
    public void savePreviewStatistics(String taskId, TaskInfo.PreviewStage statistics) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/statistics.json";
            objectMapper.writeValue(new File(filePath), statistics);
            logger.debug("[OptimizedTaskStorage] 预览统计已保存: {}", taskId);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 保存预览统计失败: {}", taskId, e);
        }
    }

    /**
     * 加载预览统计信息
     */
    public TaskInfo.PreviewStage loadPreviewStatistics(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/statistics.json";
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, TaskInfo.PreviewStage.class);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 加载预览统计失败: {}", taskId, e);
            return null;
        }
    }

    /**
     * 保存变更记录
     */
    public void saveChangeRecords(String taskId, List<ChangeRecord> changeRecords) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/changes.json";
            objectMapper.writeValue(new File(filePath), changeRecords);
            logger.debug("[OptimizedTaskStorage] 变更记录已保存: {} - {} 条记录", taskId, changeRecords.size());
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 保存变更记录失败: {}", taskId, e);
        }
    }

    /**
     * 加载变更记录
     */
    public List<ChangeRecord> loadChangeRecords(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/changes.json";
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("[OptimizedTaskStorage] 变更记录不存在: {}", taskId);
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<ChangeRecord>>() {});
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 加载变更记录失败: {}", taskId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存执行统计信息
     */
    public void saveExecutionStatistics(String taskId, int executionNum, TaskInfo.ExecutionStage statistics) {
        try {
            String filePath = getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/statistics.json";
            objectMapper.writeValue(new File(filePath), statistics);
            logger.debug("[OptimizedTaskStorage] 执行统计已保存: {} - execution_{}", taskId, executionNum);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 保存执行统计失败: {} - execution_{}", taskId, executionNum, e);
        }
    }

    /**
     * 加载执行统计信息
     */
    public TaskInfo.ExecutionStage loadExecutionStatistics(String taskId, int executionNum) {
        try {
            String filePath = getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/statistics.json";
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, TaskInfo.ExecutionStage.class);
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 加载执行统计失败: {} - execution_{}", taskId, executionNum, e);
            return null;
        }
    }

    /**
     * 流式写入扫描数据
     */
    public void writeScanData(String taskId, String jsonData) {
        String queueKey = taskId + "_scan";
        BlockingQueue<String> queue = writeQueues.computeIfAbsent(queueKey, k -> new LinkedBlockingQueue<>(1000));
        queue.offer(jsonData);
        
        if (!isWriterRunning(queueKey)) {
            startScanDataWriter(taskId, queueKey, queue);
        }
    }

    /**
     * 流式写入预览数据
     */
    public void writePreviewData(String taskId, String jsonData) {
        String queueKey = taskId + "_preview";
        BlockingQueue<String> queue = writeQueues.computeIfAbsent(queueKey, k -> new LinkedBlockingQueue<>(1000));
        queue.offer(jsonData);
        
        if (!isWriterRunning(queueKey)) {
            startPreviewDataWriter(taskId, queueKey, queue);
        }
    }

    /**
     * 流式写入执行数据
     */
    public void writeExecutionData(String taskId, int executionNum, String jsonData) {
        String queueKey = taskId + "_execution_" + executionNum;
        BlockingQueue<String> queue = writeQueues.computeIfAbsent(queueKey, k -> new LinkedBlockingQueue<>(1000));
        queue.offer(jsonData);
        
        if (!isWriterRunning(queueKey)) {
            startExecutionDataWriter(taskId, executionNum, queueKey, queue);
        }
    }

    /**
     * 启动扫描数据写入器
     */
    private void startScanDataWriter(String taskId, String queueKey, BlockingQueue<String> queue) {
        writeExecutor.submit(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(getTaskDirectory(taskId) + "/scan/data.json"))) {
                String record;
                while ((record = queue.poll(1, TimeUnit.SECONDS)) != null) {
                    writer.write(record);
                    writer.newLine();
                }
                logger.debug("[OptimizedTaskStorage] 扫描数据写入完成: {}", taskId);
            } catch (IOException | InterruptedException e) {
                logger.error("[OptimizedTaskStorage] 扫描数据写入失败: {}", taskId, e);
            } finally {
                writeQueues.remove(queueKey);
            }
        });
    }

    /**
     * 启动预览数据写入器
     */
    private void startPreviewDataWriter(String taskId, String queueKey, BlockingQueue<String> queue) {
        writeExecutor.submit(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(getTaskDirectory(taskId) + "/preview/data.json"))) {
                String record;
                while ((record = queue.poll(1, TimeUnit.SECONDS)) != null) {
                    writer.write(record);
                    writer.newLine();
                }
                logger.debug("[OptimizedTaskStorage] 预览数据写入完成: {}", taskId);
            } catch (IOException | InterruptedException e) {
                logger.error("[OptimizedTaskStorage] 预览数据写入失败: {}", taskId, e);
            } finally {
                writeQueues.remove(queueKey);
            }
        });
    }

    /**
     * 启动执行数据写入器
     */
    private void startExecutionDataWriter(String taskId, int executionNum, String queueKey, BlockingQueue<String> queue) {
        writeExecutor.submit(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/data.json"))) {
                String record;
                while ((record = queue.poll(1, TimeUnit.SECONDS)) != null) {
                    writer.write(record);
                    writer.newLine();
                }
                logger.debug("[OptimizedTaskStorage] 执行数据写入完成: {} - execution_{}", taskId, executionNum);
            } catch (IOException | InterruptedException e) {
                logger.error("[OptimizedTaskStorage] 执行数据写入失败: {} - execution_{}", taskId, executionNum, e);
            } finally {
                writeQueues.remove(queueKey);
            }
        });
    }

    /**
     * 检查写入器是否正在运行
     */
    private boolean isWriterRunning(String queueKey) {
        return writeQueues.containsKey(queueKey);
    }

    /**
     * 流式读取扫描数据
     */
    public List<String> readScanData(String taskId, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/scan/data.json", page, pageSize);
    }

    /**
     * 流式读取预览数据
     */
    public List<String> readPreviewData(String taskId, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/preview/data.json", page, pageSize);
    }

    /**
     * 流式读取执行数据
     */
    public List<String> readExecutionData(String taskId, int executionNum, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/data.json", page, pageSize);
    }

    /**
     * 读取数据文件（分页）
     */
    private List<String> readDataFile(String filePath, int page, int pageSize) {
        List<String> records = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            int skipCount = (page - 1) * pageSize;
            int readCount = 0;
            
            while ((line = reader.readLine()) != null) {
                if (skipCount > 0) {
                    skipCount--;
                    continue;
                }
                if (readCount >= pageSize) {
                    break;
                }
                records.add(line);
                readCount++;
            }
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 读取数据文件失败: {}", filePath, e);
        }
        return records;
    }

    /**
     * 写入任务日志
     */
    public void writeTaskLog(String taskId, String logMessage) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(getTaskDirectory(taskId) + "/task.log"), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 写入任务日志失败: {}", taskId, e);
        }
    }

    /**
     * 读取任务日志
     */
    public List<String> readTaskLog(String taskId, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/task.log", page, pageSize);
    }

    /**
     * 删除任务
     */
    public boolean deleteTask(String taskId) {
        try {
            Path taskPath = Paths.get(getTaskDirectory(taskId));
            if (Files.exists(taskPath)) {
                Files.walk(taskPath)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.error("[OptimizedTaskStorage] 删除文件失败: {}", path, e);
                        }
                    });
                logger.info("[OptimizedTaskStorage] 任务已删除: {}", taskId);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("[OptimizedTaskStorage] 删除任务失败: {}", taskId, e);
            return false;
        }
    }

    /**
     * 获取所有任务ID
     */
    public List<String> getAllTaskIds() {
        List<String> taskIds = new ArrayList<>();
        try {
            File baseDir = new File(BASE_DIR);
            File[] taskDirs = baseDir.listFiles();
            if (taskDirs != null) {
                for (File taskDir : taskDirs) {
                    if (taskDir.isDirectory()) {
                        taskIds.add(taskDir.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[OptimizedTaskStorage] 获取任务ID列表失败", e);
        }
        return taskIds;
    }

    /**
     * 获取执行历史列表
     */
    public List<String> getExecutionHistory(String taskId) {
        List<String> executions = new ArrayList<>();
        try {
            Path executionPath = Paths.get(getTaskDirectory(taskId) + "/execution");
            if (Files.exists(executionPath)) {
                File[] executionDirs = executionPath.toFile().listFiles(File::isDirectory);
                if (executionDirs != null) {
                    for (File executionDir : executionDirs) {
                        executions.add(executionDir.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[OptimizedTaskStorage] 获取执行历史失败: {}", taskId, e);
        }
        return executions;
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        logger.info("[OptimizedTaskStorage] 正在关闭服务...");
        writeExecutor.shutdown();
        try {
            if (!writeExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            writeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("[OptimizedTaskStorage] 服务已关闭");
    }
}
