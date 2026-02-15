package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PipelineTaskStatusDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.enums.TaskStatus;
import com.filemanager.domain.service.PluginService;
import com.filemanager.domain.service.PipelineTaskManager;
import com.filemanager.domain.service.TaskService;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.ChangeRecordQueryDTO;
import com.filemanager.domain.dto.ChangeRecordResponseDTO;
import com.filemanager.backend.logging.UnifiedLogger;
import com.filemanager.backend.model.TaskInfo;
import com.filemanager.backend.service.FileFilterService;
import com.filemanager.backend.service.FileTypeFilterService;
import com.filemanager.backend.service.TaskStorageService;
import com.filemanager.backend.service.PreviewLimitService;
import com.filemanager.backend.service.TaskRegistry;
import com.filemanager.backend.util.FileScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private final Map<String, List<Map<String, Object>>> pipelines = new ConcurrentHashMap<>();
    private final List<ChangeRecord> currentChanges = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final String configFilePath = "pipeline_config.json";
    private final PipelineTaskManager taskManager = PipelineTaskManager.getInstance();

    @Autowired
    private PluginService pluginService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRegistry taskRegistry;

    @Autowired
    private FileFilterService fileFilterService;

    @Autowired
    private FileTypeFilterService fileTypeFilterService;

    @Autowired
    private PreviewLimitService previewLimitService;

    @Autowired
    private TaskStorageService storageService;

    @javax.annotation.PostConstruct
    public void init() {
        UnifiedLogger.backendOperation("Pipeline", "初始化配置加载");
        loadPipelineConfig();
    }

    private void loadPipelineConfig() {
        try {
            File configFile = new File(configFilePath);
            if (configFile.exists()) {
                UnifiedLogger.backendOperation("Pipeline", "找到配置文件，开始加载: " + configFilePath);
                FileReader reader = new FileReader(configFile);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, Object>> pipeline = mapper.readValue(reader, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                pipelines.put("default", pipeline);
                reader.close();
                UnifiedLogger.backendOperation("Pipeline", "配置加载成功，流水线长度: " + pipeline.size());
            } else {
                UnifiedLogger.backendOperation("Pipeline", "配置文件不存在，使用默认空配置: " + configFilePath);
                pipelines.put("default", new ArrayList<>());
            }
        } catch (Exception e) {
            UnifiedLogger.backendError("Pipeline", "配置加载失败: " + e.getMessage(), e);
            pipelines.put("default", new ArrayList<>());
        }
    }

    private void savePipelineConfig() {
        try {
            List<Map<String, Object>> pipeline = pipelines.getOrDefault("default", new ArrayList<>());
            File configFile = new File(configFilePath);
            FileWriter writer = new FileWriter(configFile);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writeValue(writer, pipeline);
            writer.close();
            UnifiedLogger.backendOperation("Pipeline", "配置保存成功，流水线长度: " + pipeline.size());
        } catch (Exception e) {
            UnifiedLogger.backendError("Pipeline", "配置保存失败: " + e.getMessage(), e);
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPipeline() {
        try {
            List<Map<String, Object>> pipeline = pipelines.getOrDefault("default", new ArrayList<>());
            return ResponseEntity.ok(pipeline);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> updatePipeline(@RequestBody List<Map<String, Object>> pipeline) {
        try {
            UnifiedLogger.backendOperation("Pipeline", "收到更新流水线请求，长度: " + (pipeline != null ? pipeline.size() : 0));
            pipelines.put("default", pipeline);
            savePipelineConfig();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "流水线更新成功");
            UnifiedLogger.backendOperation("Pipeline", "流水线更新完成并保存");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            UnifiedLogger.backendError("Pipeline", "流水线更新失败: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetPipeline() {
        try {
            UnifiedLogger.backendOperation("Pipeline", "收到重置流水线请求");
            pipelines.put("default", new ArrayList<>());
            savePipelineConfig();
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "流水线重置成功");
            UnifiedLogger.backendOperation("Pipeline", "流水线重置完成并保存");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            UnifiedLogger.backendError("Pipeline", "流水线重置失败: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzePipeline(@RequestBody Map<String, Object> request) {
        try {
            currentChanges.clear();

            List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
            List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

            if (sourceDirectories == null || sourceDirectories.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "源目录不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            if (pipeline == null || pipeline.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "流水线不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 创建持久化的任务记录
            TaskRequestDTO taskRequest = new TaskRequestDTO();
            taskRequest.setTaskName("预览任务-" + System.currentTimeMillis());
            taskRequest.setDescription("流水线预览分析");
            
            // 转换源目录
            List<TaskRequestDTO.SourceDirectoryDTO> sourceDirectoryDTOs = new ArrayList<>();
            for (String dir : sourceDirectories) {
                TaskRequestDTO.SourceDirectoryDTO dirDTO = new TaskRequestDTO.SourceDirectoryDTO();
                dirDTO.setPath(dir);
                dirDTO.setRecursive(true);
                sourceDirectoryDTOs.add(dirDTO);
            }
            taskRequest.setSourceDirectories(sourceDirectoryDTOs);
            
            // 设置全局配置
            TaskRequestDTO.GlobalSettingsDTO globalSettings = new TaskRequestDTO.GlobalSettingsDTO();
            globalSettings.setDryRun(true); // 预览模式
            taskRequest.setGlobalSettings(globalSettings);
            
            // 创建任务
            String taskId = taskService.createTask(taskRequest);
            
            // 注册任务到 TaskRegistry
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo != null) {
                taskRegistry.registerTask(taskInfo);
                taskInfo.setStatus(TaskInfo.TaskStatus.PREVIEWING);
                taskInfo.setCurrentStage("PREVIEW");
                taskInfo.setMessage("正在预览分析...");
                taskRegistry.updateTaskStatus(taskId, TaskInfo.TaskStatus.PREVIEWING);
            }
            
            // 同时创建 PipelineTaskManager 中的任务，使用相同的任务ID
            taskManager.createTaskWithId(taskId, "preview");
            taskManager.updateTaskStatus(taskId, com.filemanager.domain.enums.TaskStatus.PREVIEWING);
            taskManager.setCurrentTaskRunning(true);
            taskManager.updateTaskStep(taskId, "初始化预览任务");
            taskManager.updateTaskMessage(taskId, "开始分析流水线...");

            CompletableFuture.runAsync(() -> {
                try {
                    UnifiedLogger.backendOperation("Pipeline", "开始预览分析，任务ID: " + taskId);
                    UnifiedLogger.backendOperation("Pipeline", "源目录: " + sourceDirectories);
                    UnifiedLogger.backendOperation("Pipeline", "流水线节点数量: " + pipeline.size());

                    taskManager.updateTaskStep(taskId, "输出流水线配置信息");
                    StringBuilder configSummary = new StringBuilder();
                    configSummary.append("=== 流水线配置信息 ===\n");
                    configSummary.append("源目录数量: ").append(sourceDirectories.size()).append("\n");
                    for (int i = 0; i < sourceDirectories.size(); i++) {
                        configSummary.append("  目录").append(i + 1).append(": ").append(sourceDirectories.get(i)).append("\n");
                    }
                    configSummary.append("流水线节点数量: ").append(pipeline.size()).append("\n");
                    
                    for (int i = 0; i < pipeline.size(); i++) {
                        Map<String, Object> pluginConfig = pipeline.get(i);
                        String pluginId = (String) pluginConfig.get("pluginId");
                        Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");
                        configSummary.append("  节点").append(i + 1).append(": " ).append(pluginId);
                        if (configMap != null && !configMap.isEmpty()) {
                            configSummary.append(" (参数: ").append(configMap.size()).append("个)");
                        }
                        configSummary.append("\n");
                        
                        if (configMap != null) {
                            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                configSummary.append("    ").append(entry.getKey()).append(": " ).append(entry.getValue()).append("\n");
                            }
                        }
                    }
                    
                    UnifiedLogger.backendOperation("Pipeline", configSummary.toString());
                    taskManager.updateTaskLogMessage(taskId, configSummary.toString());

                    taskManager.updateTaskStep(taskId, "扫描文件");
                    taskManager.updateTaskMessage(taskId, "正在扫描文件...");
                    
                    List<String> allFilePaths = new ArrayList<>();
                    int previewThreads = 10;
                    int minDepth = 0;
                    int maxDepth = 20;
                    
                    int globalLimitValue = previewLimitService.isGlobalPreviewUnlimited() ? 
                        Integer.MAX_VALUE : previewLimitService.getGlobalPreviewLimit();
                    AtomicInteger globalLimit = new AtomicInteger(globalLimitValue);
                    AtomicInteger dirLimit = new AtomicInteger(Integer.MAX_VALUE);
                    AtomicBoolean isTaskRunning = new AtomicBoolean(true);
                    
                    FileScanner fileScanner = new FileScanner(fileFilterService, fileTypeFilterService, isTaskRunning, previewThreads);
                    
                    for (String directory : sourceDirectories) {
                        File dir = new File(directory);
                        if (dir.exists() && dir.isDirectory()) {
                            int dirLimitValue = previewLimitService.isRootPathPreviewUnlimited(directory) ?
                                Integer.MAX_VALUE : previewLimitService.getRootPathPreviewLimit(directory);
                            AtomicInteger currentDirLimit = new AtomicInteger(dirLimitValue);
                            
                            List<File> files = fileScanner.scanFilesRobust(dir, minDepth, maxDepth, globalLimit, currentDirLimit, msg -> {
                                UnifiedLogger.backendOperation("Pipeline", msg);
                                taskManager.updateTaskMessage(taskId, msg);
                            });
                            for (File file : files) {
                                allFilePaths.add(file.getAbsolutePath());
                            }
                            UnifiedLogger.backendOperation("Pipeline", "目录 " + directory + " 包含 " + files.size() + " 个文件");
                        }
                    }
                    
                    int totalFiles = allFilePaths.size();
                    taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), 0, totalFiles);
                    
                    List<File> rootDirs = new ArrayList<>();
                    for (String directory : sourceDirectories) {
                        rootDirs.add(new File(directory));
                    }
                    
                    List<ChangeRecord> allRecords = new ArrayList<>();
                    for (String filePath : allFilePaths) {
                        File file = new File(filePath);
                        ChangeRecord record = new ChangeRecord(
                            file.getName(),
                            file.getName(),
                            file,
                            false,
                            file.getAbsolutePath(),
                            "NONE"
                        );
                        record.setId(String.valueOf(allRecords.size() + 1));
                        allRecords.add(record);
                    }
                    
                    List<ChangeRecord> allChanges = new ArrayList<>();
                    int processed = 0;
                    
                    for (ChangeRecord currentRecord : allRecords) {
                        if (!taskManager.isTaskRunning()) {
                            taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                            taskManager.updateTaskMessage(taskId, "任务已中止");
                            break;
                        }
                        
                        processed++;
                        taskManager.updateTaskProgress(taskId, processed, totalFiles);
                        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), processed, totalFiles);
                        
                        for (Map<String, Object> pluginConfig : pipeline) {
                            if (!taskManager.isTaskRunning()) {
                                taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                                taskManager.updateTaskMessage(taskId, "任务已中止");
                                break;
                            }

                            String pluginId = (String) pluginConfig.get("pluginId");
                            Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");
                            List<Map<String, Object>> preconditionGroupsData = (List<Map<String, Object>>) pluginConfig.get("preconditionGroups");

                            PluginConfigDTO config = new PluginConfigDTO();
                            if (configMap != null) {
                                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                    config.setValue(entry.getKey(), entry.getValue());
                                }
                            }

                            List<PreconditionGroupDTO> preconditionGroups = convertPreconditionGroups(preconditionGroupsData);
                            List<ChangeRecord> changes = pluginService.analyzePlugin(pluginId, currentRecord, allRecords, rootDirs, config, preconditionGroups);
                            
                            if (!changes.isEmpty()) {
                                ChangeRecord change = changes.get(0);
                                if (change.isChanged()) {
                                    currentRecord = change;
                                } else {
                                    allChanges.add(change);
                                }
                            }
                        }
                        
                        allChanges.add(currentRecord);
                        
                        if (processed % 100 == 0) {
                            String progressMessage = String.format("已处理 %d/%d 个文件，发现 %d 个变更", processed, totalFiles, allChanges.size());
                            taskManager.updateTaskMessage(taskId, progressMessage);
                        }
                    }

                    currentChanges.addAll(allChanges);
                    
                    if (taskManager.isTaskRunning()) {
                        taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEW_COMPLETED);
                        taskManager.updateTaskMessage(taskId, "预览完成，共发现 " + allChanges.size() + " 个变更");
                        taskManager.updateTaskChanges(taskId, !allChanges.isEmpty(), allChanges.size());
                        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), totalFiles, totalFiles);
                        UnifiedLogger.backendOperation("Pipeline", "预览完成，共发现 " + allChanges.size() + " 个变更");
                        
                        // 更新持久化的任务记录
                        try {
                            TaskInfo previewTaskInfo = storageService.loadTaskInfo(taskId);
                            if (previewTaskInfo != null) {
                                previewTaskInfo.setStatus(TaskInfo.TaskStatus.PREVIEWED);
                                previewTaskInfo.setCurrentStage("PREVIEW");
                                previewTaskInfo.setOverallProgress(100.0);
                                previewTaskInfo.setMessage("预览完成，共发现 " + allChanges.size() + " 个变更");
                                previewTaskInfo.setChangeRecords(allChanges);
                                storageService.saveTaskInfo(previewTaskInfo);
                                storageService.saveChangeRecords(taskId, allChanges);
                            }
                        } catch (Exception e) {
                            UnifiedLogger.backendError("Pipeline", "更新任务状态失败: " + e.getMessage(), e);
                        }
                    } else {
                        taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                        taskManager.updateTaskMessage(taskId, "任务已中止");
                        
                        // 更新持久化的任务记录
                        try {
                            TaskInfo cancelTaskInfo = storageService.loadTaskInfo(taskId);
                            if (cancelTaskInfo != null) {
                                cancelTaskInfo.setStatus(TaskInfo.TaskStatus.CANCELLED);
                                cancelTaskInfo.setMessage("任务已中止");
                                storageService.saveTaskInfo(cancelTaskInfo);
                            }
                        } catch (Exception e) {
                            UnifiedLogger.backendError("Pipeline", "更新任务状态失败: " + e.getMessage(), e);
                        }
                    }
                } catch (Exception e) {
                    UnifiedLogger.backendError("Pipeline", "预览失败: " + e.getMessage(), e);
                    taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEW_FAILED);
                    taskManager.updateTaskMessage(taskId, "预览失败: " + e.getMessage());
                    
                    // 更新持久化的任务记录
                    try {
                        TaskInfo failedTaskInfo = storageService.loadTaskInfo(taskId);
                        if (failedTaskInfo != null) {
                            failedTaskInfo.setStatus(TaskInfo.TaskStatus.FAILED);
                            failedTaskInfo.setMessage("预览失败: " + e.getMessage());
                            storageService.saveTaskInfo(failedTaskInfo);
                        }
                    } catch (Exception ex) {
                        UnifiedLogger.backendError("Pipeline", "更新任务状态失败: " + ex.getMessage(), ex);
                    }
                } finally {
                    taskManager.setCurrentTaskRunning(false);
                }
            }, executorService);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskId", taskId);
            result.put("message", "预览任务已开始执行");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executePipeline(@RequestBody Map<String, Object> request) {
        try {
            if (taskManager.isTaskRunning()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "已有任务在运行，请先中止");
                return ResponseEntity.badRequest().body(result);
            }

            currentChanges.clear();
            taskManager.clearAllTasks();

            List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
            List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

            if (sourceDirectories == null || sourceDirectories.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "源目录不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            if (pipeline == null || pipeline.isEmpty()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "流水线不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            String taskId = taskManager.createTask("execute");
            taskManager.updateTaskStatus(taskId, TaskStatus.EXECUTING);
            taskManager.setCurrentTaskRunning(true);
            taskManager.updateTaskStep(taskId, "初始化执行任务");
            taskManager.updateTaskMessage(taskId, "开始执行流水线...");

            TaskRequestDTO taskRequest = new TaskRequestDTO();
            taskRequest.setTaskName("Pipeline Execution");
            taskRequest.setDescription("Execute pipeline with " + pipeline.size() + " plugins");
            
            List<TaskRequestDTO.SourceDirectoryDTO> sourceDirDTOs = new java.util.ArrayList<>();
            for (String sourceDir : sourceDirectories) {
                TaskRequestDTO.SourceDirectoryDTO sourceDirDTO = new TaskRequestDTO.SourceDirectoryDTO();
                sourceDirDTO.setPath(sourceDir);
                sourceDirDTO.setDepth(4);
                sourceDirDTO.setRecursive(true);
                sourceDirDTOs.add(sourceDirDTO);
            }
            taskRequest.setSourceDirectories(sourceDirDTOs);

            String taskServiceId = taskService.createTask(taskRequest);

            CompletableFuture.runAsync(() -> {
                try {
                    UnifiedLogger.backendOperation("Pipeline", "开始执行，任务ID: " + taskId);
                    UnifiedLogger.backendOperation("Pipeline", "源目录: " + sourceDirectories);
                    UnifiedLogger.backendOperation("Pipeline", "流水线节点数量: " + pipeline.size());

                    taskManager.updateTaskStep(taskId, "输出流水线配置信息");
                    StringBuilder configSummary = new StringBuilder();
                    configSummary.append("=== 流水线配置信息 ===\n");
                    configSummary.append("源目录数量: ").append(sourceDirectories.size()).append("\n");
                    for (int i = 0; i < sourceDirectories.size(); i++) {
                        configSummary.append("  目录").append(i + 1).append(": ").append(sourceDirectories.get(i)).append("\n");
                    }
                    configSummary.append("流水线节点数量: ").append(pipeline.size()).append("\n");
                    
                    for (int i = 0; i < pipeline.size(); i++) {
                        Map<String, Object> pluginConfig = pipeline.get(i);
                        String pluginId = (String) pluginConfig.get("pluginId");
                        Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");
                        configSummary.append("  节点").append(i + 1).append(": " ).append(pluginId);
                        if (configMap != null && !configMap.isEmpty()) {
                            configSummary.append(" (参数: ").append(configMap.size()).append("个)");
                        }
                        configSummary.append("\n");
                        
                        if (configMap != null) {
                            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                configSummary.append("    ").append(entry.getKey()).append(": " ).append(entry.getValue()).append("\n");
                            }
                        }
                    }
                    
                    UnifiedLogger.backendOperation("Pipeline", configSummary.toString());
                    taskManager.updateTaskLogMessage(taskId, configSummary.toString());

                    taskManager.updateTaskStep(taskId, "扫描文件");
                    taskManager.updateTaskMessage(taskId, "正在扫描文件...");
                    
                    List<String> allFilePaths = new ArrayList<>();
                    int executionThreads = 4;
                    int minDepth = 0;
                    int maxDepth = 20;
                    
                    int globalLimitValue = previewLimitService.isGlobalExecutionUnlimited() ?
                        Integer.MAX_VALUE : previewLimitService.getGlobalExecutionLimit();
                    AtomicInteger globalLimit = new AtomicInteger(globalLimitValue);
                    AtomicInteger dirLimit = new AtomicInteger(Integer.MAX_VALUE);
                    AtomicBoolean isTaskRunning = new AtomicBoolean(true);
                    
                    FileScanner fileScanner = new FileScanner(fileFilterService, fileTypeFilterService, isTaskRunning, executionThreads);
                    
                    for (String directory : sourceDirectories) {
                        File dir = new File(directory);
                        if (dir.exists() && dir.isDirectory()) {
                            int dirLimitValue = previewLimitService.isRootPathExecutionUnlimited(directory) ?
                                Integer.MAX_VALUE : previewLimitService.getRootPathExecutionLimit(directory);
                            AtomicInteger currentDirLimit = new AtomicInteger(dirLimitValue);
                            
                            List<File> files = fileScanner.scanFilesRobust(dir, minDepth, maxDepth, globalLimit, currentDirLimit, msg -> {
                                UnifiedLogger.backendOperation("Pipeline", msg);
                                taskManager.updateTaskMessage(taskId, msg);
                            });
                            for (File file : files) {
                                allFilePaths.add(file.getAbsolutePath());
                            }
                            UnifiedLogger.backendOperation("Pipeline", "目录 " + directory + " 包含 " + files.size() + " 个文件");
                        }
                    }
                    
                    int totalFiles = allFilePaths.size();
                    taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), 0, totalFiles);
                    
                    List<ChangeRecord> allChanges = new ArrayList<>();
                    int processed = 0;
                    
                    for (String filePath : allFilePaths) {
                        if (!taskManager.isTaskRunning()) {
                            taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                            taskManager.updateTaskMessage(taskId, "任务已中止");
                            break;
                        }
                        
                        processed++;
                        taskManager.updateTaskProgress(taskId, processed, totalFiles);
                        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), processed, totalFiles);
                        
                        String fileName = new File(filePath).getName();
                        ChangeRecord record = new ChangeRecord();
                        record.setId(String.valueOf(processed));
                        record.setOriginalName(fileName);
                        record.setNewName(fileName);
                        record.setFilePath(filePath);
                        record.setStatus("PENDING");
                        record.setOperationType("NONE");
                        record.setChanged(false);
                        allChanges.add(record);
                        
                        for (Map<String, Object> pluginConfig : pipeline) {
                            if (!taskManager.isTaskRunning()) {
                                taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                                taskManager.updateTaskMessage(taskId, "任务已中止");
                                break;
                            }

                            String pluginId = (String) pluginConfig.get("pluginId");
                            Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");
                            List<Map<String, Object>> preconditionGroupsData = (List<Map<String, Object>>) pluginConfig.get("preconditionGroups");

                            PluginConfigDTO config = new PluginConfigDTO();
                            if (configMap != null) {
                                for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                    config.setValue(entry.getKey(), entry.getValue());
                                }
                            }

                            List<PreconditionGroupDTO> preconditionGroups = convertPreconditionGroups(preconditionGroupsData);
                            List<ChangeRecord> changes;
                            if (preconditionGroups != null && !preconditionGroups.isEmpty()) {
                                changes = pluginService.executePlugin(pluginId, java.util.Collections.singletonList(filePath), config, preconditionGroups);
                            } else {
                                changes = pluginService.executePlugin(pluginId, java.util.Collections.singletonList(filePath), config);
                            }
                            allChanges.addAll(changes);
                        }
                        
                        if (processed % 100 == 0) {
                            String progressMessage = String.format("已处理 %d/%d 个文件，处理 %d 个变更", processed, totalFiles, allChanges.size());
                            taskManager.updateTaskMessage(taskId, progressMessage);
                        }
                    }

                    currentChanges.addAll(allChanges);
                    
                    if (taskManager.isTaskRunning()) {
                        taskManager.updateTaskStatus(taskId, TaskStatus.EXECUTION_COMPLETED);
                        taskManager.updateTaskMessage(taskId, "执行完成，共处理 " + allChanges.size() + " 个文件");
                        taskManager.updateTaskChanges(taskId, !allChanges.isEmpty(), allChanges.size());
                        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), totalFiles, totalFiles);
                        UnifiedLogger.backendOperation("Pipeline", "执行完成，共处理 " + allChanges.size() + " 个文件");
                        
                        taskService.executeTask(taskServiceId);
                    } else {
                        taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                        taskManager.updateTaskMessage(taskId, "任务已中止");
                    }
                } catch (Exception e) {
                    UnifiedLogger.backendError("Pipeline", "执行失败: " + e.getMessage(), e);
                    taskManager.updateTaskStatus(taskId, TaskStatus.EXECUTION_FAILED);
                    taskManager.updateTaskMessage(taskId, "执行失败: " + e.getMessage());
                } finally {
                    taskManager.setCurrentTaskRunning(false);
                }
            }, executorService);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskId", taskId);
            result.put("message", "执行任务已开始执行");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopPipeline() {
        try {
            taskManager.cancelCurrentTask();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "任务已成功中止");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<PipelineTaskStatusDTO> getPipelineStatus() {
        try {
            PipelineTaskStatusDTO status = taskManager.getCurrentTaskStatus();
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.ok(new PipelineTaskStatusDTO());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/changes")
    public ResponseEntity<ChangeRecordResponseDTO> getChanges(
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String searchFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String operationTypeFilter,
            @RequestParam(required = false, defaultValue = "true") boolean hideUnchanged,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection) {
        try {
            List<ChangeRecord> sourceChanges;
            
            if (taskId != null && !taskId.isEmpty()) {
                sourceChanges = storageService.loadChangeRecords(taskId);
                UnifiedLogger.backendOperation("Pipeline", "从任务加载变更记录: " + taskId + ", 数量: " + sourceChanges.size());
            } else {
                sourceChanges = currentChanges;
                UnifiedLogger.backendOperation("Pipeline", "从内存加载变更记录, 数量: " + sourceChanges.size());
            }
            
            ChangeRecordQueryDTO queryDTO = new ChangeRecordQueryDTO();
            queryDTO.setSearchFilter(searchFilter);
            queryDTO.setStatusFilter(statusFilter);
            queryDTO.setOperationTypeFilter(operationTypeFilter);
            queryDTO.setHideUnchanged(hideUnchanged);
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setSortBy(sortBy);
            queryDTO.setSortDirection(sortDirection);

            List<ChangeRecord> filteredChanges = new ArrayList<>();
            for (ChangeRecord record : sourceChanges) {
                if (searchFilter != null && !searchFilter.isEmpty()) {
                    boolean matchesSearch = false;
                    if (record.getOriginalName() != null && record.getOriginalName().toLowerCase().contains(searchFilter.toLowerCase())) {
                        matchesSearch = true;
                    }
                    if (record.getFilePath() != null && record.getFilePath().toLowerCase().contains(searchFilter.toLowerCase())) {
                        matchesSearch = true;
                    }
                    if (!matchesSearch) {
                        continue;
                    }
                }

                if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals("全部")) {
                    if (!statusFilter.equals(record.getStatus())) {
                        continue;
                    }
                }

                if (operationTypeFilter != null && !operationTypeFilter.isEmpty() && !operationTypeFilter.equals("全部")) {
                    if (!operationTypeFilter.equals(record.getOperationType())) {
                        continue;
                    }
                }

                if (hideUnchanged && !record.isChanged()) {
                    continue;
                }

                filteredChanges.add(record);
            }

            if (sortBy != null && !sortBy.isEmpty()) {
                filteredChanges.sort((a, b) -> {
                    int result = 0;
                    switch (sortBy) {
                        case "originalName":
                            result = compareStrings(a.getOriginalName(), b.getOriginalName());
                            break;
                        case "newName":
                            result = compareStrings(a.getNewName(), b.getNewName());
                            break;
                        case "filePath":
                            result = compareStrings(a.getFilePath(), b.getFilePath());
                            break;
                        case "status":
                            result = compareStrings(a.getStatus(), b.getStatus());
                            break;
                        case "operationType":
                            result = compareStrings(a.getOperationType(), b.getOperationType());
                            break;
                        default:
                            result = compareStrings(a.getId(), b.getId());
                            break;
                    }
                    return "DESC".equals(sortDirection) ? -result : result;
                });
            }

            long total = filteredChanges.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, filteredChanges.size());
            List<ChangeRecord> paginatedChanges = new ArrayList<>();
            if (startIndex < filteredChanges.size()) {
                paginatedChanges = filteredChanges.subList(startIndex, endIndex);
            }

            ChangeRecordResponseDTO responseDTO = new ChangeRecordResponseDTO(
                    paginatedChanges,
                    total,
                    page,
                    size
            );

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private int compareStrings(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareTo(b);
    }

    private List<PreconditionGroupDTO> convertPreconditionGroups(List<Map<String, Object>> preconditionGroupsData) {
        if (preconditionGroupsData == null || preconditionGroupsData.isEmpty()) {
            return null;
        }

        List<PreconditionGroupDTO> groups = new ArrayList<>();
        for (Map<String, Object> groupData : preconditionGroupsData) {
            PreconditionGroupDTO group = new PreconditionGroupDTO();
            group.setId((String) groupData.get("id"));
            group.setName((String) groupData.get("name"));
            group.setDescription((String) groupData.get("description"));
            group.setLogicType((String) groupData.get("logicType"));

            List<Map<String, Object>> preconditionsData = (List<Map<String, Object>>) groupData.get("preconditions");
            if (preconditionsData != null && !preconditionsData.isEmpty()) {
                List<com.filemanager.domain.dto.PreconditionDTO> preconditions = new ArrayList<>();
                for (Map<String, Object> preconditionData : preconditionsData) {
                    com.filemanager.domain.dto.PreconditionDTO precondition = new com.filemanager.domain.dto.PreconditionDTO();
                    precondition.setId((String) preconditionData.get("id"));
                    precondition.setField((String) preconditionData.get("field"));
                    
                    String operatorStr = (String) preconditionData.get("operator");
                    precondition.setOperator(com.filemanager.domain.dto.PreconditionDTO.OperatorType.fromValue(operatorStr));
                    
                    precondition.setValue(preconditionData.get("value"));
                    precondition.setDescription((String) preconditionData.get("description"));
                    preconditions.add(precondition);
                }
                group.setPreconditions(preconditions);
            }

            groups.add(group);
        }
        return groups;
    }

    private int countFiles(File directory) {
        int count = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        count++;
                    } else if (file.isDirectory()) {
                        count += countFiles(file);
                    }
                }
            }
        }
        return count;
    }

    private List<String> collectFiles(File directory) {
        List<String> filePaths = new ArrayList<>();
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        filePaths.add(file.getAbsolutePath());
                    } else if (file.isDirectory()) {
                        filePaths.addAll(collectFiles(file));
                    }
                }
            }
        }
        return filePaths;
    }
}