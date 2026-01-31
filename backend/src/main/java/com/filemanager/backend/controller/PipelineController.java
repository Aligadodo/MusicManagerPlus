package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.PluginService;
import com.filemanager.domain.service.TaskService;
import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.ChangeRecordQueryDTO;
import com.filemanager.domain.dto.ChangeRecordResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private final Map<String, List<Map<String, Object>>> pipelines = new ConcurrentHashMap<>();
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private final List<ChangeRecord> currentChanges = new ArrayList<>();
    private final Map<String, Object> taskStatus = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Autowired
    private PluginService pluginService;

    @Autowired
    private TaskService taskService;

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
            pipelines.put("default", pipeline);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "流水线更新成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzePipeline(@RequestBody Map<String, Object> request) {
        try {
            if (taskService.isTaskRunning()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "已有任务在运行，请先中止");
                return ResponseEntity.badRequest().body(result);
            }

            isTaskRunning.set(true);
            currentChanges.clear();
            taskStatus.clear();
            taskStatus.put("status", "分析中");
            taskStatus.put("progress", 0.0);
            taskStatus.put("remainingTime", "计算中...");
            taskStatus.put("completedTasks", 0);
            taskStatus.put("totalTasks", 0);

            List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
            List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

            if (sourceDirectories == null || sourceDirectories.isEmpty()) {
                taskStatus.put("status", "分析失败");
                isTaskRunning.set(false);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "源目录不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            if (pipeline == null || pipeline.isEmpty()) {
                taskStatus.put("status", "分析失败");
                isTaskRunning.set(false);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "流水线不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 异步执行分析任务
            CompletableFuture.runAsync(() -> {
                try {
                    taskStatus.put("totalTasks", pipeline.size());
                    List<ChangeRecord> allChanges = new ArrayList<>();

                    int completed = 0;
                    for (Map<String, Object> pluginConfig : pipeline) {
                        if (!isTaskRunning.get()) {
                            break;
                        }

                        String pluginId = (String) pluginConfig.get("pluginId");
                        Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");

                        PluginConfigDTO config = new PluginConfigDTO();
                        if (configMap != null) {
                            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                config.setValue(entry.getKey(), entry.getValue());
                            }
                        }

                        List<ChangeRecord> changes = pluginService.previewPlugin(pluginId, sourceDirectories, config);
                        allChanges.addAll(changes);
                        completed++;

                        taskStatus.put("completedTasks", completed);
                        taskStatus.put("progress", (double) completed / pipeline.size());
                    }

                    currentChanges.addAll(allChanges);
                    taskStatus.put("status", isTaskRunning.get() ? "分析完成" : "已中止");
                    taskStatus.put("progress", 1.0);
                    taskStatus.put("remainingTime", "00:00:00");
                    taskStatus.put("totalTasks", allChanges.size());
                    taskStatus.put("completedTasks", allChanges.size());
                } catch (Exception e) {
                    e.printStackTrace();
                    taskStatus.put("status", "分析失败: " + e.getMessage());
                } finally {
                    isTaskRunning.set(false);
                }
            }, executorService);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "分析任务已开始执行");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            taskStatus.put("status", "分析失败");
            isTaskRunning.set(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executePipeline(@RequestBody Map<String, Object> request) {
        try {
            if (taskService.isTaskRunning()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "已有任务在运行，请先中止");
                return ResponseEntity.badRequest().body(result);
            }

            isTaskRunning.set(true);
            currentChanges.clear();
            taskStatus.clear();
            taskStatus.put("status", "执行中");
            taskStatus.put("progress", 0.0);
            taskStatus.put("remainingTime", "计算中...");
            taskStatus.put("completedTasks", 0);
            taskStatus.put("totalTasks", 0);

            List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
            List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

            if (sourceDirectories == null || sourceDirectories.isEmpty()) {
                taskStatus.put("status", "执行失败");
                isTaskRunning.set(false);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "源目录不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            if (pipeline == null || pipeline.isEmpty()) {
                taskStatus.put("status", "执行失败");
                isTaskRunning.set(false);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "流水线不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            // 创建任务记录
            TaskRequestDTO taskRequest = new TaskRequestDTO();
            taskRequest.setStrategyId("pipeline");
            taskRequest.setFilePaths(sourceDirectories);
            taskRequest.setTaskName("Pipeline Execution");
            taskRequest.setDescription("Execute pipeline with " + pipeline.size() + " plugins");

            String taskId = taskService.createTask(taskRequest);

            // 异步执行任务
            CompletableFuture.runAsync(() -> {
                try {
                    taskStatus.put("totalTasks", pipeline.size());
                    List<ChangeRecord> allChanges = new ArrayList<>();

                    int completed = 0;
                    for (Map<String, Object> pluginConfig : pipeline) {
                        if (!isTaskRunning.get()) {
                            break;
                        }

                        String pluginId = (String) pluginConfig.get("pluginId");
                        Map<String, Object> configMap = (Map<String, Object>) pluginConfig.get("config");

                        PluginConfigDTO config = new PluginConfigDTO();
                        if (configMap != null) {
                            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                                config.setValue(entry.getKey(), entry.getValue());
                            }
                        }

                        List<ChangeRecord> changes = pluginService.executePlugin(pluginId, sourceDirectories, config);
                        allChanges.addAll(changes);
                        completed++;

                        taskStatus.put("completedTasks", completed);
                        taskStatus.put("progress", (double) completed / pipeline.size());
                    }

                    currentChanges.addAll(allChanges);
                    taskStatus.put("status", isTaskRunning.get() ? "执行完成" : "已中止");
                    taskStatus.put("progress", 1.0);
                    taskStatus.put("remainingTime", "00:00:00");
                    taskStatus.put("totalTasks", allChanges.size());
                    taskStatus.put("completedTasks", allChanges.size());

                    // 执行任务记录
                    taskService.executeTask(taskId);
                } catch (Exception e) {
                    e.printStackTrace();
                    taskStatus.put("status", "执行失败: " + e.getMessage());
                } finally {
                    isTaskRunning.set(false);
                }
            }, executorService);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskId", taskId);
            result.put("message", "执行任务已开始执行");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            taskStatus.put("status", "执行失败");
            isTaskRunning.set(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopPipeline() {
        try {
            isTaskRunning.set(false);
            taskStatus.put("status", "已中止");
            taskStatus.put("progress", 0.0);
            taskStatus.put("remainingTime", "00:00:00");

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
    public ResponseEntity<Map<String, Object>> getPipelineStatus() {
        try {
            Map<String, Object> status = new HashMap<>(taskStatus);
            status.put("running", isTaskRunning.get());
            return ResponseEntity.ok(status);
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
            // 创建查询DTO
            ChangeRecordQueryDTO queryDTO = new ChangeRecordQueryDTO();
            queryDTO.setSearchFilter(searchFilter);
            queryDTO.setStatusFilter(statusFilter);
            queryDTO.setOperationTypeFilter(operationTypeFilter);
            queryDTO.setHideUnchanged(hideUnchanged);
            queryDTO.setPage(page);
            queryDTO.setSize(size);
            queryDTO.setSortBy(sortBy);
            queryDTO.setSortDirection(sortDirection);

            // 应用过滤条件
            List<ChangeRecord> filteredChanges = new ArrayList<>();
            for (ChangeRecord record : currentChanges) {
                // 搜索过滤
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

                // 状态过滤
                if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals("全部")) {
                    if (!statusFilter.equals(record.getStatus())) {
                        continue;
                    }
                }

                // 操作类型过滤
                if (operationTypeFilter != null && !operationTypeFilter.isEmpty() && !operationTypeFilter.equals("全部")) {
                    if (!operationTypeFilter.equals(record.getOperationType())) {
                        continue;
                    }
                }

                // 隐藏未变更的记录
                if (hideUnchanged && !record.isChanged()) {
                    continue;
                }

                filteredChanges.add(record);
            }

            // 排序
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

            // 分页
            long total = filteredChanges.size();
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, filteredChanges.size());
            List<ChangeRecord> paginatedChanges = new ArrayList<>();
            if (startIndex < filteredChanges.size()) {
                paginatedChanges = filteredChanges.subList(startIndex, endIndex);
            }

            // 创建响应DTO
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

    // 辅助方法：比较两个字符串
    private int compareStrings(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 0;
        }
        if (s1 == null) {
            return -1;
        }
        if (s2 == null) {
            return 1;
        }
        return s1.compareTo(s2);
    }
}
