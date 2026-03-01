package com.filemanager.backend.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemanager.backend.entity.*;
import com.filemanager.backend.mapper.*;
import com.filemanager.backend.model.TaskConfigSnapshot;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.backend.service.ConfigSnapshotService;
import com.filemanager.domain.entity.ChangeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component("databaseTaskStorage")
public class DatabaseTaskStorage implements ITaskStorage {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTaskStorage.class);
    private static final String BASE_DIR = System.getProperty("user.home") + "/.MusicManagerPlus/tasks";

    private final ObjectMapper objectMapper;
    private final ExecutorService writeExecutor;
    private final Map<String, BlockingQueue<String>> writeQueues = new ConcurrentHashMap<>();
    private final TaskInfoMapper taskInfoMapper;
    private final TaskStageMapper taskStageMapper;
    private final ChangeRecordMapper changeRecordMapper;
    private final TaskOperationLogMapper taskOperationLogMapper;
    private final ConfigSnapshotService configSnapshotService;

    @Autowired
    public DatabaseTaskStorage(
            TaskInfoMapper taskInfoMapper,
            TaskStageMapper taskStageMapper,
            ChangeRecordMapper changeRecordMapper,
            TaskOperationLogMapper taskOperationLogMapper,
            ConfigSnapshotService configSnapshotService) {
        this.objectMapper = new ObjectMapper();
        this.writeExecutor = Executors.newFixedThreadPool(5);
        this.taskInfoMapper = taskInfoMapper;
        this.taskStageMapper = taskStageMapper;
        this.changeRecordMapper = changeRecordMapper;
        this.taskOperationLogMapper = taskOperationLogMapper;
        this.configSnapshotService = configSnapshotService;
        initializeBaseDirectory();
    }

    private void initializeBaseDirectory() {
        try {
            Files.createDirectories(Paths.get(BASE_DIR));
            logger.info("[DatabaseStorage] 基础目录初始化完成: {}", BASE_DIR);
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 基础目录初始化失败", e);
        }
    }

    @Override
    public void initializeTaskDirectory(String taskId) {
        try {
            Path taskPath = Paths.get(getTaskDirectory(taskId));
            
            if (!Files.exists(taskPath)) {
                Files.createDirectories(taskPath);
            }
            
            Path scanPath = taskPath.resolve("scan");
            if (!Files.exists(scanPath)) {
                Files.createDirectories(scanPath);
            }
            
            Path previewPath = taskPath.resolve("preview");
            if (!Files.exists(previewPath)) {
                Files.createDirectories(previewPath);
            }
            
            Path executionPath = taskPath.resolve("execution");
            if (!Files.exists(executionPath)) {
                Files.createDirectories(executionPath);
            }
            
            logger.info("[DatabaseStorage] 任务目录初始化完成: {}", taskId);
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 任务目录初始化失败: {}", taskId, e);
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
            
            TaskInfoPO taskInfoPO = convertToTaskInfoPO(taskInfo);
            
            TaskInfoPO existing = taskInfoMapper.selectByTaskId(taskInfo.getTaskId());
            if (existing != null) {
                taskInfoMapper.update(taskInfoPO);
                logger.debug("[DatabaseStorage] 任务信息已更新: {}", taskInfo.getTaskId());
            } else {
                try {
                    taskInfoMapper.insert(taskInfoPO);
                    logger.debug("[DatabaseStorage] 任务信息已保存: {}", taskInfo.getTaskId());
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("PRIMARY KEY constraint failed")) {
                        logger.warn("[DatabaseStorage] 主键冲突，尝试更新任务信息: {}", taskInfo.getTaskId());
                        taskInfoMapper.update(taskInfoPO);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 保存任务信息失败: {}", taskInfo.getTaskId(), e);
            throw new RuntimeException("保存任务信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public TaskInfo loadTaskInfo(String taskId) {
        try {
            TaskInfoPO taskInfoPO = taskInfoMapper.selectByTaskId(taskId);
            if (taskInfoPO == null) {
                logger.warn("[DatabaseStorage] 任务信息不存在: {}", taskId);
                return null;
            }
            
            TaskInfo taskInfo = convertFromTaskInfoPO(taskInfoPO);
            
            if (taskInfo.getConfigSnapshotId() != null && !taskInfo.getConfigSnapshotId().isEmpty()) {
                TaskConfigSnapshot configSnapshot = configSnapshotService.getSnapshot(taskInfo.getConfigSnapshotId());
                if (configSnapshot != null) {
                    taskInfo.setConfigSnapshot(configSnapshot);
                }
            }
            
            return taskInfo;
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 加载任务信息失败: {}", taskId, e);
            return null;
        }
    }

    @Override
    public void saveConfigSnapshot(String taskId, TaskConfigSnapshot configSnapshot) {
        try {
            String filePath = getTaskDirectory(taskId) + "/config.json";
            objectMapper.writeValue(new File(filePath), configSnapshot);
            logger.debug("[DatabaseStorage] 配置快照已保存: {}", taskId);
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 保存配置快照失败: {}", taskId, e);
        }
    }

    @Override
    public TaskConfigSnapshot loadConfigSnapshot(String taskId) {
        try {
            String filePath = getTaskDirectory(taskId) + "/config.json";
            File file = new File(filePath);
            if (!file.exists()) {
                logger.warn("[DatabaseStorage] 配置快照不存在: {}", taskId);
                return null;
            }
            return objectMapper.readValue(file, TaskConfigSnapshot.class);
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 加载配置快照失败: {}", taskId, e);
            return null;
        }
    }

    @Override
    public void saveScanStatistics(String taskId, TaskInfo.ScanStage statistics) {
        try {
            List<TaskStagePO> existingList = taskStageMapper.selectByTaskIdAndStageType(taskId, "SCAN");
            if (!existingList.isEmpty()) {
                TaskStagePO existing = existingList.get(0);
                existing.setStatsJson(objectMapper.writeValueAsString(statistics));
                taskStageMapper.update(existing);
            } else {
                TaskStagePO stagePO = new TaskStagePO();
                stagePO.setTaskId(taskId);
                stagePO.setStageType("SCAN");
                stagePO.setStatus("COMPLETED");
                stagePO.setStatsJson(objectMapper.writeValueAsString(statistics));
                taskStageMapper.insert(stagePO);
            }
            logger.debug("[DatabaseStorage] 扫描统计已保存: {}", taskId);
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 保存扫描统计失败: {}", taskId, e);
        }
    }

    @Override
    public TaskInfo.ScanStage loadScanStatistics(String taskId) {
        try {
            List<TaskStagePO> stagePOList = taskStageMapper.selectByTaskIdAndStageType(taskId, "SCAN");
            if (stagePOList.isEmpty() || stagePOList.get(0).getStatsJson() == null) {
                return null;
            }
            return objectMapper.readValue(stagePOList.get(0).getStatsJson(), TaskInfo.ScanStage.class);
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 加载扫描统计失败: {}", taskId, e);
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
            List<TaskStagePO> existingList = taskStageMapper.selectByTaskIdAndStageType(taskId, "PREVIEW");
            if (!existingList.isEmpty()) {
                TaskStagePO existing = existingList.get(0);
                existing.setStatsJson(objectMapper.writeValueAsString(statistics));
                taskStageMapper.update(existing);
            } else {
                TaskStagePO stagePO = new TaskStagePO();
                stagePO.setTaskId(taskId);
                stagePO.setStageType("PREVIEW");
                stagePO.setStatus("COMPLETED");
                stagePO.setStatsJson(objectMapper.writeValueAsString(statistics));
                taskStageMapper.insert(stagePO);
            }
            logger.debug("[DatabaseStorage] 预览统计已保存: {}", taskId);
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 保存预览统计失败: {}", taskId, e);
        }
    }

    @Override
    public TaskInfo.PreviewStage loadPreviewStatistics(String taskId) {
        try {
            List<TaskStagePO> stagePOList = taskStageMapper.selectByTaskIdAndStageType(taskId, "PREVIEW");
            if (stagePOList.isEmpty() || stagePOList.get(0).getStatsJson() == null) {
                return null;
            }
            return objectMapper.readValue(stagePOList.get(0).getStatsJson(), TaskInfo.PreviewStage.class);
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 加载预览统计失败: {}", taskId, e);
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
            changeRecordMapper.deleteByTaskId(taskId);
            
            List<ChangeRecordPO> recordsPO = changeRecords.stream()
                .map(this::convertToChangeRecordPO)
                .collect(Collectors.toList());
            
            if (!recordsPO.isEmpty()) {
                changeRecordMapper.batchInsert(recordsPO);
                logger.debug("[DatabaseStorage] 变更记录已保存: {} - {} 条记录", taskId, changeRecords.size());
            }
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 保存变更记录失败: {}", taskId, e);
        }
    }

    @Override
    public List<ChangeRecord> loadChangeRecords(String taskId) {
        try {
            List<ChangeRecordPO> recordsPO = changeRecordMapper.selectByTaskId(taskId);
            return recordsPO.stream()
                .map(this::convertFromChangeRecordPO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 加载变更记录失败: {}", taskId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveExecutionStatistics(String taskId, int executionNum, TaskInfo.ExecutionStage statistics) {
        try {
            String stageType = "EXECUTION_" + executionNum;
            List<TaskStagePO> existingList = taskStageMapper.selectByTaskIdAndStageType(taskId, stageType);
            if (!existingList.isEmpty()) {
                TaskStagePO existing = existingList.get(0);
                existing.setStatsJson(objectMapper.writeValueAsString(statistics));
                taskStageMapper.update(existing);
            } else {
                TaskStagePO stagePO = new TaskStagePO();
                stagePO.setTaskId(taskId);
                stagePO.setStageType(stageType);
                stagePO.setStatus("COMPLETED");
                stagePO.setStatsJson(objectMapper.writeValueAsString(statistics));
                taskStageMapper.insert(stagePO);
            }
            logger.debug("[DatabaseStorage] 执行统计已保存: {} - execution_{}", taskId, executionNum);
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 保存执行统计失败: {} - execution_{}", taskId, executionNum, e);
        }
    }

    @Override
    public TaskInfo.ExecutionStage loadExecutionStatistics(String taskId, int executionNum) {
        try {
            String stageType = "EXECUTION_" + executionNum;
            List<TaskStagePO> stagePOList = taskStageMapper.selectByTaskIdAndStageType(taskId, stageType);
            if (stagePOList.isEmpty() || stagePOList.get(0).getStatsJson() == null) {
                return null;
            }
            return objectMapper.readValue(stagePOList.get(0).getStatsJson(), TaskInfo.ExecutionStage.class);
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 加载执行统计失败: {} - execution_{}", taskId, executionNum, e);
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
            List<TaskInfoPO> tasks = taskInfoMapper.selectAll();
            return tasks.stream()
                .map(TaskInfoPO::getTaskId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 获取任务ID列表失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean deleteTask(String taskId) {
        try {
            taskInfoMapper.deleteByTaskId(taskId);
            taskStageMapper.deleteByTaskId(taskId);
            changeRecordMapper.deleteByTaskId(taskId);
            taskOperationLogMapper.deleteByTaskId(taskId);
            
            Path taskPath = Paths.get(getTaskDirectory(taskId));
            if (Files.exists(taskPath)) {
                Files.walk(taskPath)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.error("[DatabaseStorage] 删除文件失败: {}", path, e);
                        }
                    });
            }
            
            logger.info("[DatabaseStorage] 任务已删除: {}", taskId);
            return true;
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 删除任务失败: {}", taskId, e);
            return false;
        }
    }

    @Override
    public void writeTaskLog(String taskId, String logEntry) {
        try {
            TaskOperationLogPO logPO = new TaskOperationLogPO();
            logPO.setTaskId(taskId);
            logPO.setOperationType("LOG");
            logPO.setOperationDetail(logEntry);
            logPO.setOperationTime(new Date());
            logPO.setResult("SUCCESS");
            
            taskOperationLogMapper.insert(logPO);
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 写入任务日志失败: {}", taskId, e);
        }
    }

    @Override
    public List<String> readTaskLog(String taskId, int page, int pageSize) {
        try {
            int offset = (page - 1) * pageSize;
            List<TaskOperationLogPO> logs = taskOperationLogMapper.selectByPage(
                taskId, null, null, null, "operation_time", "DESC", offset, pageSize
            );
            
            return logs.stream()
                .map(log -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    sb.append(log.getOperationTime());
                    sb.append("] ");
                    sb.append(log.getOperationDetail());
                    return sb.toString();
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 读取任务日志失败: {}", taskId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void clearAllTasks() {
        try {
            List<TaskInfoPO> allTasks = taskInfoMapper.selectAll();
            for (TaskInfoPO task : allTasks) {
                deleteTask(task.getTaskId());
            }
            logger.info("[DatabaseStorage] 所有任务已清空");
        } catch (Exception e) {
            logger.error("[DatabaseStorage] 清空所有任务失败", e);
        }
    }

    @Override
    public void clearScanData(String taskId) {
        try {
            Path scanDataPath = Paths.get(getTaskDirectory(taskId) + "/scan/data.json");
            if (Files.exists(scanDataPath)) {
                Files.delete(scanDataPath);
                logger.debug("[DatabaseStorage] 扫描数据已清空: {}", taskId);
            }
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 清空扫描数据失败: {}", taskId, e);
        }
    }

    @Override
    public void clearPreviewData(String taskId) {
        try {
            Path previewDataPath = Paths.get(getTaskDirectory(taskId) + "/preview/data.json");
            if (Files.exists(previewDataPath)) {
                Files.delete(previewDataPath);
                logger.debug("[DatabaseStorage] 预览数据已清空: {}", taskId);
            }
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 清空预览数据失败: {}", taskId, e);
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
                            logger.error("[DatabaseStorage] 删除执行数据失败: {}", path, e);
                        }
                    });
                Files.createDirectories(executionDir);
                logger.debug("[DatabaseStorage] 执行数据已清空: {}", taskId);
            }
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 清空执行数据失败: {}", taskId, e);
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
                        logger.warn("[DatabaseStorage] 无效的执行目录: {}", name);
                    }
                });
            
            history.sort(Integer::compareTo);
            return history;
        } catch (IOException e) {
            logger.error("[DatabaseStorage] 获取执行历史失败: {}", taskId, e);
            return new ArrayList<>();
        }
    }

    public void shutdown() {
        try {
            writeExecutor.shutdown();
            if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
            logger.info("[DatabaseStorage] 已关闭");
        } catch (InterruptedException e) {
            logger.error("[DatabaseStorage] 关闭失败", e);
            writeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private TaskInfoPO convertToTaskInfoPO(TaskInfo taskInfo) {
        TaskInfoPO po = new TaskInfoPO();
        po.setTaskId(taskInfo.getTaskId());
        po.setTaskName(taskInfo.getTaskName());
        po.setStatus(taskInfo.getStatus().name());
        po.setCurrentStage(taskInfo.getCurrentStage());
        po.setOverallProgress(taskInfo.getOverallProgress());
        po.setMessage(taskInfo.getMessage());
        po.setConfigSnapshotId(taskInfo.getConfigSnapshotId());
        
        if (taskInfo.getCreatedAt() > 0) {
            po.setCreatedAt(new Date(taskInfo.getCreatedAt()));
        }
        if (taskInfo.getUpdatedAt() > 0) {
            po.setUpdatedAt(new Date(taskInfo.getUpdatedAt()));
        }
        
        return po;
    }

    private TaskInfo convertFromTaskInfoPO(TaskInfoPO po) {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setTaskId(po.getTaskId());
        taskInfo.setTaskName(po.getTaskName());
        taskInfo.setStatus(TaskInfo.TaskStatus.valueOf(po.getStatus()));
        taskInfo.setCurrentStage(po.getCurrentStage());
        taskInfo.setOverallProgress(po.getOverallProgress() != null ? po.getOverallProgress() : 0.0);
        taskInfo.setMessage(po.getMessage());
        taskInfo.setConfigSnapshotId(po.getConfigSnapshotId());
        
        if (po.getCreatedAt() != null) {
            taskInfo.setCreatedAt(po.getCreatedAt().getTime());
        }
        if (po.getUpdatedAt() != null) {
            taskInfo.setUpdatedAt(po.getUpdatedAt().getTime());
        }
        
        return taskInfo;
    }

    private ChangeRecordPO convertToChangeRecordPO(ChangeRecord record) {
        ChangeRecordPO po = new ChangeRecordPO();
        po.setRecordId(record.getId());
        po.setOriginalName(record.getOriginalName());
        po.setNewName(record.getNewName());
        po.setFilePath(record.getFilePath());
        po.setNewPath(record.getNewPath());
        po.setOperationType(record.getOperationType());
        po.setStatus(record.getStatus());
        po.setChanged(record.isChanged());
        po.setSelected(record.isSelected());
        po.setFailReason(record.getFailReason());
        po.setAnalyzeTime(record.getAnalyzeTime() > 0 ? new Date(record.getAnalyzeTime()) : null);
        po.setExecuteTime(record.getExecuteTime() > 0 ? new Date(record.getExecuteTime()) : null);
        po.setCreatedAt(new Date());
        
        return po;
    }

    private ChangeRecord convertFromChangeRecordPO(ChangeRecordPO po) {
        ChangeRecord record = new ChangeRecord();
        record.setId(po.getRecordId());
        record.setOriginalName(po.getOriginalName());
        record.setNewName(po.getNewName());
        record.setFilePath(po.getFilePath());
        record.setNewPath(po.getNewPath());
        record.setOperationType(po.getOperationType());
        record.setStatus(po.getStatus());
        record.setChanged(po.getChanged() != null ? po.getChanged() : false);
        record.setSelected(po.getSelected() != null ? po.getSelected() : false);
        record.setFailReason(po.getFailReason());
        record.setAnalyzeTime(po.getAnalyzeTime() != null ? po.getAnalyzeTime().getTime() : 0);
        record.setExecuteTime(po.getExecuteTime() != null ? po.getExecuteTime().getTime() : 0);
        
        return record;
    }

    private void startScanDataWriter(String taskId, String queueKey, BlockingQueue<String> queue) {
        writeExecutor.submit(() -> {
            Path filePath = Paths.get(getTaskDirectory(taskId) + "/scan/data.json");
            logger.info("[DatabaseStorage] 开始写入扫描数据: {}", filePath);
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
                        logger.info("[DatabaseStorage] 扫描数据写入完成: {}, 共 {} 条记录", taskId, recordCount);
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("[DatabaseStorage] 扫描数据写入失败: {}", taskId, e);
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
                logger.error("[DatabaseStorage] 预览数据写入失败: {}", taskId, e);
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
                logger.error("[DatabaseStorage] 执行数据写入失败: {} - execution_{}", taskId, executionNum, e);
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
            logger.error("[DatabaseStorage] 读取数据文件失败: {}", filePath, e);
        }
        return records;
    }
}