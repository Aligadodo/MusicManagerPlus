package com.filemanager.backend.controller;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.domain.service.StrategyService;
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
    private StrategyService strategyService;

    @Autowired
    private TaskService taskService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPipeline() {
        try {
            // 从默认管道获取，实际应用中可能需要从数据库或配置文件加载
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

            // 对每个策略执行分析
            for (Map<String, Object> strategyConfig : pipeline) {
                String strategyId = (String) strategyConfig.get("strategyId");
                Map<String, Object> configMap = (Map<String, Object>) strategyConfig.get("config");

                StrategyConfigDTO config = new StrategyConfigDTO();
                if (configMap != null) {
                    for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                        config.setValue(entry.getKey(), entry.getValue());
                    }
                }

                List<ChangeRecord> changes = strategyService.analyzeFiles(strategyId, sourceDirectories, config);
                allChanges.addAll(changes);
            }

            return ResponseEntity.ok(allChanges);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executePipeline(@RequestBody Map<String, Object> request) {
        try {
            List<String> sourceDirectories = (List<String>) request.get("sourceDirectories");
            List<Map<String, Object>> pipeline = (List<Map<String, Object>>) request.get("pipeline");

            // 创建任务请求
            TaskRequestDTO taskRequest = new TaskRequestDTO();
            taskRequest.setFilePaths(sourceDirectories);
            // 这里简化处理，实际应用中可能需要更复杂的任务创建逻辑
            taskRequest.setStrategyId("pipeline");

            String taskId = taskService.createTask(taskRequest);
            taskService.executeTask(taskId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskId", taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
