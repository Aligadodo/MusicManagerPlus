package com.filemanager.backend.controller;

import com.filemanager.backend.model.*;
import com.filemanager.backend.service.NewTaskService;
import com.filemanager.backend.service.TaskFileStorageService;
import com.filemanager.domain.dto.TaskRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新的任务管理控制器
 * 支持任务快照、预览、执行的完整流程
 */
@RestController
@RequestMapping("/api/new-tasks")
public class NewTaskController {

    private static final Logger logger = LoggerFactory.getLogger(NewTaskController.class);

    @Autowired
    private NewTaskService newTaskService;

    @Autowired
    private TaskFileStorageService storageService;

    @PostMapping("/preview")
    public ResponseEntity<Map<String, String>> createPreviewTask(@RequestBody TaskRequestDTO request) {
        logger.info("[API] POST /api/new-tasks/preview - 创建预览任务");
        try {
            String taskId = newTaskService.createPreviewTask(request);
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("message", "预览任务创建成功");
            logger.info("[API] POST /api/new-tasks/preview - 预览任务创建成功, taskId: {}", taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/new-tasks/preview - 预览任务创建失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<Map<String, String>> createExecuteTask(@RequestBody Map<String, String> request) {
        logger.info("[API] POST /api/new-tasks/execute - 创建执行任务");
        try {
            String previewTaskId = request.get("previewTaskId");
            if (previewTaskId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "previewTaskId不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            String taskId = newTaskService.createExecuteTask(previewTaskId);
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            result.put("message", "执行任务创建成功");
            logger.info("[API] POST /api/new-tasks/execute - 执行任务创建成功, taskId: {}", taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/new-tasks/execute - 执行任务创建失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{taskId}/preview-execute")
    public ResponseEntity<Map<String, Object>> executePreviewTask(@PathVariable String taskId) {
        logger.info("[API] POST /api/new-tasks/{}/preview-execute - 执行预览任务", taskId);
        try {
            newTaskService.executePreviewTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "预览任务开始执行");
            logger.info("[API] POST /api/new-tasks/{}/preview-execute - 预览任务执行开始", taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/new-tasks/{}/preview-execute - 预览任务执行失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{taskId}/execute")
    public ResponseEntity<Map<String, Object>> executeTask(@PathVariable String taskId) {
        logger.info("[API] POST /api/new-tasks/{}/execute - 执行任务", taskId);
        try {
            newTaskService.executeTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "任务开始执行");
            logger.info("[API] POST /api/new-tasks/{}/execute - 任务执行开始", taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/new-tasks/{}/execute - 任务执行失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        logger.info("[API] POST /api/new-tasks/{}/cancel - 取消任务", taskId);
        try {
            boolean success = newTaskService.cancelTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "任务已取消");
            logger.info("[API] POST /api/new-tasks/{}/cancel - 任务取消{}", taskId, success ? "成功" : "失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/new-tasks/{}/cancel - 任务取消失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{taskId}/progress")
    public ResponseEntity<TaskFileStorageService.TaskProgress> getTaskProgress(@PathVariable String taskId) {
        logger.info("[API] GET /api/new-tasks/{}/progress - 获取任务进度", taskId);
        try {
            TaskFileStorageService.TaskProgress progress = newTaskService.getTaskProgress(taskId);
            if (progress != null) {
                logger.info("[API] GET /api/new-tasks/{}/progress - 任务进度: {}", taskId, progress.getStatus());
            } else {
                logger.warn("[API] GET /api/new-tasks/{}/progress - 任务进度不存在", taskId);
            }
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            logger.error("[API] GET /api/new-tasks/{}/progress - 获取任务进度失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{taskId}/preview-result")
    public ResponseEntity<PreviewResult> getPreviewResult(@PathVariable String taskId) {
        logger.info("[API] GET /api/new-tasks/{}/preview-result - 获取预览结果", taskId);
        try {
            PreviewResult result = newTaskService.getPreviewResult(taskId);
            if (result != null) {
                logger.info("[API] GET /api/new-tasks/{}/preview-result - 预览结果获取成功", taskId);
            } else {
                logger.warn("[API] GET /api/new-tasks/{}/preview-result - 预览结果不存在", taskId);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] GET /api/new-tasks/{}/preview-result - 获取预览结果失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{taskId}/execution-result")
    public ResponseEntity<ExecutionResult> getExecutionResult(@PathVariable String taskId) {
        logger.info("[API] GET /api/new-tasks/{}/execution-result - 获取执行结果", taskId);
        try {
            ExecutionResult result = newTaskService.getExecutionResult(taskId);
            if (result != null) {
                logger.info("[API] GET /api/new-tasks/{}/execution-result - 执行结果获取成功", taskId);
            } else {
                logger.warn("[API] GET /api/new-tasks/{}/execution-result - 执行结果不存在", taskId);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] GET /api/new-tasks/{}/execution-result - 获取执行结果失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/preview-results")
    public ResponseEntity<List<PreviewResult>> getAllPreviewResults() {
        logger.info("[API] GET /api/new-tasks/preview-results - 获取所有预览结果");
        try {
            List<PreviewResult> results = newTaskService.getAllPreviewResults();
            logger.info("[API] GET /api/new-tasks/preview-results - 返回 {} 个预览结果", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("[API] GET /api/new-tasks/preview-results - 获取预览结果失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/execution-results")
    public ResponseEntity<List<ExecutionResult>> getAllExecutionResults() {
        logger.info("[API] GET /api/new-tasks/execution-results - 获取所有执行结果");
        try {
            List<ExecutionResult> results = newTaskService.getAllExecutionResults();
            logger.info("[API] GET /api/new-tasks/execution-results - 返回 {} 个执行结果", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("[API] GET /api/new-tasks/execution-results - 获取执行结果失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{taskId}/snapshot")
    public ResponseEntity<TaskSnapshot> getTaskSnapshot(@PathVariable String taskId) {
        logger.info("[API] GET /api/new-tasks/{}/snapshot - 获取任务快照", taskId);
        try {
            TaskSnapshot snapshot = storageService.loadTaskSnapshot(taskId);
            if (snapshot != null) {
                logger.info("[API] GET /api/new-tasks/{}/snapshot - 任务快照获取成功", taskId);
            } else {
                logger.warn("[API] GET /api/new-tasks/{}/snapshot - 任务快照不存在", taskId);
            }
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            logger.error("[API] GET /api/new-tasks/{}/snapshot - 获取任务快照失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String taskId) {
        logger.info("[API] DELETE /api/new-tasks/{} - 删除任务", taskId);
        try {
            boolean snapshotDeleted = storageService.deleteTaskSnapshot(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", snapshotDeleted);
            result.put("message", "任务已删除");
            logger.info("[API] DELETE /api/new-tasks/{} - 任务删除{}", taskId, snapshotDeleted ? "成功" : "失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] DELETE /api/new-tasks/{} - 任务删除失败", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
