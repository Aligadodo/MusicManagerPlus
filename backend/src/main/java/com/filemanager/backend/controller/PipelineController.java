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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.*;

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

            String taskId = taskManager.createTask("preview");
            taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEWING);
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
                    
                    List<ChangeRecord> allChanges = new ArrayList<>();
                    int totalFiles = 0;
                    int scannedFiles = 0;
                    
                    for (String directory : sourceDirectories) {
                        File dir = new File(directory);
                        if (dir.exists() && dir.isDirectory()) {
                            int fileCount = countFiles(dir);
                            totalFiles += fileCount;
                            UnifiedLogger.backendOperation("Pipeline", "目录 " + directory + " 包含 " + fileCount + " 个文件");
                        }
                    }
                    
                    taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), 0, totalFiles);

                    int completed = 0;
                    for (Map<String, Object> pluginConfig : pipeline) {
                        if (!taskManager.isTaskRunning()) {
                            taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                            taskManager.updateTaskMessage(taskId, "任务已中止");
                            break;
                        }

                        String pluginId = (String) pluginConfig.get("pluginId");
                        Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");
                        List<Map<String, Object>> preconditionGroupsData = (List<Map<String, Object>>) pluginConfig.get("preconditionGroups");

                        taskManager.updateTaskStep(taskId, "执行节点: " + pluginId);
                        taskManager.updateTaskMessage(taskId, "正在执行节点: " + pluginId);
                        UnifiedLogger.backendOperation("Pipeline", "执行节点: " + pluginId);

                        PluginConfigDTO config = new PluginConfigDTO();
                        if (configMap != null) {
                            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                config.setValue(entry.getKey(), entry.getValue());
                            }
                        }

                        List<PreconditionGroupDTO> preconditionGroups = convertPreconditionGroups(preconditionGroupsData);
                        List<ChangeRecord> changes;
                        if (preconditionGroups != null && !preconditionGroups.isEmpty()) {
                            changes = pluginService.previewPlugin(pluginId, sourceDirectories, config, preconditionGroups);
                        } else {
                            changes = pluginService.previewPlugin(pluginId, sourceDirectories, config);
                        }
                        allChanges.addAll(changes);
                        completed++;

                        scannedFiles = (int) ((double) completed / pipeline.size() * totalFiles);
                        taskManager.updateTaskProgress(taskId, completed, pipeline.size());
                        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), scannedFiles, totalFiles);
                        
                        String progressMessage = String.format("节点 %d/%d 完成，发现 %d 个变更", completed, pipeline.size(), changes.size());
                        taskManager.updateTaskMessage(taskId, progressMessage);
                        UnifiedLogger.backendOperation("Pipeline", progressMessage);
                    }

                    currentChanges.addAll(allChanges);
                    
                    if (taskManager.isTaskRunning()) {
                        taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEW_COMPLETED);
                        taskManager.updateTaskMessage(taskId, "预览完成，共发现 " + allChanges.size() + " 个变更");
                        taskManager.updateTaskChanges(taskId, !allChanges.isEmpty(), allChanges.size());
                        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), totalFiles, totalFiles);
                        UnifiedLogger.backendOperation("Pipeline", "预览完成，共发现 " + allChanges.size() + " 个变更");
                    } else {
                        taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                        taskManager.updateTaskMessage(taskId, "任务已中止");
                    }
                } catch (Exception e) {
                    UnifiedLogger.backendError("Pipeline", "预览失败: " + e.getMessage(), e);
                    taskManager.updateTaskStatus(taskId, TaskStatus.PREVIEW_FAILED);
                    taskManager.updateTaskMessage(taskId, "预览失败: " + e.getMessage());
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
            taskRequest.setStrategyId("pipeline");
            taskRequest.setFilePaths(sourceDirectories);
            taskRequest.setTaskName("Pipeline Execution");
            taskRequest.setDescription("Execute pipeline with " + pipeline.size() + " plugins");

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
                    
                    List<ChangeRecord> allChanges = new ArrayList<>();
                    int totalFiles = 0;
                    int scannedFiles = 0;
                    
                    for (String directory : sourceDirectories) {
                        File dir = new File(directory);
                        if (dir.exists() && dir.isDirectory()) {
                            int fileCount = countFiles(dir);
                            totalFiles += fileCount;
                            UnifiedLogger.backendOperation("Pipeline", "目录 " + directory + " 包含 " + fileCount + " 个文件");
                        }
                    }
                    
                    taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), 0, totalFiles);

                    int completed = 0;
                    for (Map<String, Object> pluginConfig : pipeline) {
                        if (!taskManager.isTaskRunning()) {
                            taskManager.updateTaskStatus(taskId, TaskStatus.CANCELLED);
                            taskManager.updateTaskMessage(taskId, "任务已中止");
                            break;
                        }

                        String pluginId = (String) pluginConfig.get("pluginId");
                        Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");
                        List<Map<String, Object>> preconditionGroupsData = (List<Map<String, Object>>) pluginConfig.get("preconditionGroups");

                        taskManager.updateTaskStep(taskId, "执行节点: " + pluginId);
                        taskManager.updateTaskMessage(taskId, "正在执行节点: " + pluginId);
                        UnifiedLogger.backendOperation("Pipeline", "执行节点: " + pluginId);

                        PluginConfigDTO config = new PluginConfigDTO();
                        if (configMap != null) {
                            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                config.setValue(entry.getKey(), entry.getValue());
                            }
                        }

                        List<PreconditionGroupDTO> preconditionGroups = convertPreconditionGroups(preconditionGroupsData);
                        List<ChangeRecord> changes;
                        if (preconditionGroups != null && !preconditionGroups.isEmpty()) {
                            changes = pluginService.executePlugin(pluginId, sourceDirectories, config, preconditionGroups);
                        } else {
                            changes = pluginService.executePlugin(pluginId, sourceDirectories, config);
                        }
                        allChanges.addAll(changes);
                        completed++;

                        scannedFiles = (int) ((double) completed / pipeline.size() * totalFiles);
                        taskManager.updateTaskProgress(taskId, completed, pipeline.size());
                        taskManager.updateTaskScanningInfo(taskId, sourceDirectories.get(0), scannedFiles, totalFiles);
                        
                        String progressMessage = String.format("节点 %d/%d 完成，处理 %d 个文件", completed, pipeline.size(), changes.size());
                        taskManager.updateTaskMessage(taskId, progressMessage);
                        UnifiedLogger.backendOperation("Pipeline", progressMessage);
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
            @RequestParam(required = false) String searchFilter,
            @RequestParam(required = false) String statusFilter,
            @RequestParam(required = false) String operationTypeFilter,
            @RequestParam(required = false, defaultValue = "true") boolean hideUnchanged,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection) {
        try {
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
            for (ChangeRecord record : currentChanges) {
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
}