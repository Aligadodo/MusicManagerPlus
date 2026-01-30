package com.filemanager.backend.controller;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.PluginService;
import com.filemanager.domain.service.TaskService;
import com.filemanager.domain.dto.TaskRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {

    private final Map<String, List<Map<String, Object>>> pipelines = new ConcurrentHashMap<>();

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
    public ResponseEntity<List<ChangeRecord>> analyzePipeline(@RequestBody Map<String, Object> request) {
        try {
            List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
            List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

            List<ChangeRecord> allChanges = new ArrayList<>();

            if (sourceDirectories == null || sourceDirectories.isEmpty()) {
                return ResponseEntity.badRequest().body(allChanges);
            }

            if (pipeline == null || pipeline.isEmpty()) {
                return ResponseEntity.badRequest().body(allChanges);
            }

            for (Map<String, Object> pluginConfig : pipeline) {
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
            }

            return ResponseEntity.ok(allChanges);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executePipeline(@RequestBody Map<String, Object> request) {
        try {
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

            List<ChangeRecord> allChanges = new ArrayList<>();

            for (Map<String, Object> pluginConfig : pipeline) {
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
            }

            TaskRequestDTO taskRequest = new TaskRequestDTO();
            taskRequest.setStrategyId("pipeline");
            taskRequest.setFilePaths(sourceDirectories);
            taskRequest.setTaskName("Pipeline Execution");
            taskRequest.setDescription("Execute pipeline with " + pipeline.size() + " plugins");

            String taskId = taskService.createTask(taskRequest);
            taskService.executeTask(taskId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskId", taskId);
            result.put("changeCount", allChanges.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
