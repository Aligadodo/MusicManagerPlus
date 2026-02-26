package com.filemanager.backend.storage;

import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.domain.entity.ChangeRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component("fileSystemTaskStorage")
public class FileSystemTaskStorage implements ITaskStorage {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemTaskStorage.class);
    private static final String BASE_DIR = System.getProperty("user.home") + "/.MusicManagerPlus/tasks";

    private final ObjectMapper objectMapper;
    private final ExecutorService writeExecutor;
    private final Map<String, BlockingQueue<String>> writeQueues = new ConcurrentHashMap<>();

    public FileSystemTaskStorage() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.writeExecutor = Executors.newFixedThreadPool(5);
        initializeBaseDirectory();
    }

    private void initializeBaseDirectory() {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
            logger.info("[FileSystemStorage] 基础目录初始化完成: {}", BASE_DIR);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 基础目录初始化失败", e);
        }
    }

    @Override
    public void initializeTaskDirectory(String taskId) {
        try {
            Path taskPath = Paths.get(getTaskDirectory(taskId));
            Files.createDirectories(taskPath);
            
            Files.createDirectories(taskPath.resolve("scan"));
            Files.createDirectories(taskPath.resolve("preview"));
            Files.createDirectories(taskPath.resolve("execution"));
            
            logger.info("[FileSystemStorage] 任务目录初始化完成: {}", taskId);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 任务目录初始化失败: {}", taskId, e);
            throw new RuntimeException("任务目录初始化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getTaskDirectory(String taskId) {
        return BASE_DIR + "/" + taskId;
    }

    @Override
    public void saveTaskInfo(TaskInfo taskInfo) {
        try {
            taskInfo.setUpdatedAt(System.currentTimeMillis());
            String filePath = getTaskDirectory(taskInfo.getTaskId()) + "/task.json";
            objectMapper.writeValue(new File(filePath), taskInfo);
            logger.debug("[FileSystemStorage] 任务信息已保存: {}", taskInfo.getTaskId());
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 保存任务信息失败: {}", taskInfo.getTaskId(), e);
            throw new RuntimeException("保存任务信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public TaskInfo loadTaskInfo(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/task.json";
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("[FileSystemStorage] 任务信息不存在: {}", taskId);
                return null;
            }
            
            if (file.length() == 0) {
                logger.warn("[FileSystemStorage] 任务信息文件为空: {}", taskId);
                return null;
            }
            
            return objectMapper.readValue(file, TaskInfo.class);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 加载任务信息失败: {}", taskId, e);
            return null;
        }
    }

    @Override
    public void saveConfigSnapshot(String taskId, TaskConfigSnapshot configSnapshot) {
        try {
            String filePath = getTaskDirectory(taskId) + "/config.json";
            objectMapper.writeValue(new File(filePath), configSnapshot);
            logger.debug("[FileSystemStorage] 配置快照已保存: {}", taskId);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 保存配置快照失败: {}", taskId, e);
        }
    }

    @Override
    public TaskConfigSnapshot loadConfigSnapshot(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/config.json";
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("[FileSystemStorage] 配置快照不存在: {}", taskId);
                return null;
            }
            return objectMapper.readValue(file, TaskConfigSnapshot.class);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 加载配置快照失败: {}", taskId, e);
            return null;
        }
    }

    @Override
    public void saveScanStatistics(String taskId, TaskInfo.ScanStage statistics) {
        try {
            String filePath = getTaskDirectory(taskId) + "/scan/statistics.json";
            objectMapper.writeValue(new File(filePath), statistics);
            logger.debug("[FileSystemStorage] 扫描统计已保存: {}", taskId);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 保存扫描统计失败: {}", taskId, e);
        }
    }

    @Override
    public TaskInfo.ScanStage loadScanStatistics(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/scan/statistics.json";
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, TaskInfo.ScanStage.class);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 加载扫描统计失败: {}", taskId, e);
            return null;
        }
    }

    @Override
    public void writeScanData(String taskId, String jsonData) {
        String queueKey = taskId + "_scan";
        BlockingQueue<String> queue = writeQueues.computeIfAbsent(queueKey, k -> {
            BlockingQueue<String> newQueue = new LinkedBlockingQueue<>(1000);
            startScanDataWriter(taskId, queueKey, newQueue);
            return newQueue;
        });
        queue.offer(jsonData);
    }

    @Override
    public void finishScanDataWriting(String taskId) {
        String queueKey = taskId + "_scan";
        writeQueues.remove(queueKey);
    }

    @Override
    public void savePreviewStatistics(String taskId, TaskInfo.PreviewStage statistics) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/statistics.json";
            objectMapper.writeValue(new File(filePath), statistics);
            logger.debug("[FileSystemStorage] 预览统计已保存: {}", taskId);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 保存预览统计失败: {}", taskId, e);
        }
    }

    @Override
    public TaskInfo.PreviewStage loadPreviewStatistics(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/statistics.json";
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, TaskInfo.PreviewStage.class);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 加载预览统计失败: {}", taskId, e);
            return null;
        }
    }

    @Override
    public void writePreviewData(String taskId, String jsonData) {
        String queueKey = taskId + "_preview";
        BlockingQueue<String> queue = writeQueues.computeIfAbsent(queueKey, k -> {
            BlockingQueue<String> newQueue = new LinkedBlockingQueue<>(1000);
            startPreviewDataWriter(taskId, queueKey, newQueue);
            return newQueue;
        });
        queue.offer(jsonData);
    }

    @Override
    public void finishPreviewDataWriting(String taskId) {
        String queueKey = taskId + "_preview";
        writeQueues.remove(queueKey);
    }

    @Override
    public void saveChangeRecords(String taskId, List<ChangeRecord> changeRecords) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/changes.json";
            objectMapper.writeValue(new File(filePath), changeRecords);
            logger.debug("[FileSystemStorage] 变更记录已保存: {} - {} 条记录", taskId, changeRecords.size());
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 保存变更记录失败: {}", taskId, e);
        }
    }

    @Override
    public List<ChangeRecord> loadChangeRecords(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/preview/changes.json";
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("[FileSystemStorage] 变更记录不存在: {}", taskId);
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, new TypeReference<List<ChangeRecord>>() {});
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 加载变更记录失败: {}", taskId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveExecutionStatistics(String taskId, int executionNum, TaskInfo.ExecutionStage statistics) {
        try {
            String filePath = getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/statistics.json";
            objectMapper.writeValue(new File(filePath), statistics);
            logger.debug("[FileSystemStorage] 执行统计已保存: {} - execution_{}", taskId, executionNum);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 保存执行统计失败: {} - execution_{}", taskId, executionNum, e);
        }
    }

    @Override
    public TaskInfo.ExecutionStage loadExecutionStatistics(String taskId, int executionNum) {
        try {
            String filePath = getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/statistics.json";
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, TaskInfo.ExecutionStage.class);
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 加载执行统计失败: {} - execution_{}", taskId, executionNum, e);
            return null;
        }
    }

    @Override
    public void writeExecutionData(String taskId, int executionNum, String jsonData) {
        String queueKey = taskId + "_execution_" + executionNum;
        BlockingQueue<String> queue = writeQueues.computeIfAbsent(queueKey, k -> {
            BlockingQueue<String> newQueue = new LinkedBlockingQueue<>(1000);
            startExecutionDataWriter(taskId, executionNum, queueKey, newQueue);
            return newQueue;
        });
        queue.offer(jsonData);
    }

    @Override
    public void finishExecutionDataWriting(String taskId, int executionNum) {
        String queueKey = taskId + "_execution_" + executionNum;
        writeQueues.remove(queueKey);
    }

    @Override
    public List<String> getAllTaskIds() {
        try {
            Path baseDir = Paths.get(BASE_DIR);
            if (!Files.exists(baseDir)) {
                return new ArrayList<>();
            }
            
            List<String> taskIds = new ArrayList<>();
            Files.list(baseDir)
                .filter(Files::isDirectory)
                .forEach(path -> taskIds.add(path.getFileName().toString()));
            
            return taskIds;
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 获取任务ID列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
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
                            logger.error("[FileSystemStorage] 删除文件失败: {}", path, e);
                        }
                    });
                logger.info("[FileSystemStorage] 任务已删除: {}", taskId);
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 删除任务失败: {}", taskId, e);
            return false;
        }
    }

    @Override
    public void writeTaskLog(String taskId, String logEntry) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(getTaskDirectory(taskId) + "/task.log"), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 写入任务日志失败: {}", taskId, e);
        }
    }

    @Override
    public List<String> readTaskLog(String taskId, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/task.log", page, pageSize);
    }

    @Override
    public void clearAllTasks() {
        try {
            Path baseDir = Paths.get(BASE_DIR);
            if (Files.exists(baseDir)) {
                Files.list(baseDir)
                    .filter(Files::isDirectory)
                    .forEach(taskPath -> {
                        try {
                            Files.walk(taskPath)
                                .sorted((a, b) -> -a.compareTo(b))
                                .forEach(path -> {
                                    try {
                                        Files.delete(path);
                                    } catch (IOException e) {
                                        logger.error("[FileSystemStorage] 删除文件失败: {}", path, e);
                                    }
                                });
                        } catch (IOException e) {
                            logger.error("[FileSystemStorage] 删除任务失败: {}", taskPath, e);
                        }
                    });
                logger.info("[FileSystemStorage] 所有任务已清空");
            }
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 清空所有任务失败", e);
        }
    }

    @Override
    public void clearScanData(String taskId) {
        try {
            Path scanDataPath = Paths.get(getTaskDirectory(taskId) + "/scan/data.json");
            if (Files.exists(scanDataPath)) {
                Files.delete(scanDataPath);
                logger.debug("[FileSystemStorage] 扫描数据已清空: {}", taskId);
            }
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 清空扫描数据失败: {}", taskId, e);
        }
    }

    @Override
    public void clearPreviewData(String taskId) {
        try {
            Path previewDataPath = Paths.get(getTaskDirectory(taskId) + "/preview/data.json");
            if (Files.exists(previewDataPath)) {
                Files.delete(previewDataPath);
                logger.debug("[FileSystemStorage] 预览数据已清空: {}", taskId);
            }
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 清空预览数据失败: {}", taskId, e);
        }
    }

    @Override
    public void clearExecutionData(String taskId) {
        try {
            Path executionDir = Paths.get(getTaskDirectory(taskId) + "/execution");
            if (Files.exists(executionDir)) {
                Files.walk(executionDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.error("[FileSystemStorage] 删除执行数据失败: {}", path, e);
                        }
                    });
                Files.createDirectories(executionDir);
                logger.debug("[FileSystemStorage] 执行数据已清空: {}", taskId);
            }
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 清空执行数据失败: {}", taskId, e);
        }
    }

    @Override
    public List<String> readScanData(String taskId, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/scan/data.json", page, pageSize);
    }

    @Override
    public List<String> readPreviewData(String taskId, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/preview/data.json", page, pageSize);
    }

    @Override
    public List<String> readExecutionData(String taskId, int executionNum, int page, int pageSize) {
        return readDataFile(getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/data.json", page, pageSize);
    }

    @Override
    public List<Integer> getExecutionHistory(String taskId) {
        try {
            Path executionDir = Paths.get(getTaskDirectory(taskId) + "/execution");
            if (!Files.exists(executionDir)) {
                return new ArrayList<>();
            }
            
            List<Integer> history = new ArrayList<>();
            Files.list(executionDir)
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith("execution_"))
                .forEach(name -> {
                    try {
                        String numStr = name.substring("execution_".length());
                        history.add(Integer.parseInt(numStr));
                    } catch (NumberFormatException e) {
                        logger.warn("[FileSystemStorage] 无效的执行目录: {}", name);
                    }
                });
            
            history.sort(Integer::compareTo);
            return history;
        } catch (IOException e) {
            logger.error("[FileSystemStorage] 获取执行历史失败: {}", taskId, e);
            return new ArrayList<>();
        }
    }

    public void shutdown() {
        try {
            writeExecutor.shutdown();
            if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
            logger.info("[FileSystemStorage] 已关闭");
        } catch (InterruptedException e) {
            logger.error("[FileSystemStorage] 关闭失败", e);
            writeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void startScanDataWriter(String taskId, String queueKey, BlockingQueue<String> queue) {
        writeExecutor.submit(() -> {
            Path filePath = Paths.get(getTaskDirectory(taskId) + "/scan/data.json");
            logger.info("[FileSystemStorage] 开始写入扫描数据: {}", filePath);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                String record;
                int recordCount = 0;
                while (true) {
                    record = queue.poll(1, TimeUnit.SECONDS);
                    if (record != null) {
                        writer.write(record);
                        writer.newLine();
                        recordCount++;
                    } else if (!writeQueues.containsKey(queueKey)) {
                        logger.info("[FileSystemStorage] 扫描数据写入完成: {}, 共 {} 条记录", taskId, recordCount);
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("[FileSystemStorage] 扫描数据写入失败: {}", taskId, e);
            } finally {
                writeQueues.remove(queueKey);
            }
        });
    }

    private void startPreviewDataWriter(String taskId, String queueKey, BlockingQueue<String> queue) {
        writeExecutor.submit(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(getTaskDirectory(taskId) + "/preview/data.json"))) {
                String record;
                while (true) {
                    record = queue.poll(1, TimeUnit.SECONDS);
                    if (record != null) {
                        writer.write(record);
                        writer.newLine();
                    } else if (!writeQueues.containsKey(queueKey)) {
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("[FileSystemStorage] 预览数据写入失败: {}", taskId, e);
            } finally {
                writeQueues.remove(queueKey);
            }
        });
    }

    private void startExecutionDataWriter(String taskId, int executionNum, String queueKey, BlockingQueue<String> queue) {
        writeExecutor.submit(() -> {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/data.json"))) {
                String record;
                while (true) {
                    record = queue.poll(1, TimeUnit.SECONDS);
                    if (record != null) {
                        writer.write(record);
                        writer.newLine();
                    } else if (!writeQueues.containsKey(queueKey)) {
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("[FileSystemStorage] 执行数据写入失败: {} - execution_{}", taskId, executionNum, e);
            } finally {
                writeQueues.remove(queueKey);
            }
        });
    }

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
            logger.error("[FileSystemStorage] 读取数据文件失败: {}", filePath, e);
        }
        return records;
    }
}