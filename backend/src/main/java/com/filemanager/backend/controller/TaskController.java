package com.filemanager.backend.controller;

import com.filemanager.domain.dto.TaskRequestDTO;
import com.filemanager.domain.dto.TaskStatusDTO;
import com.filemanager.domain.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @PostMapping
    public ResponseEntity<Map<String, String>> createTask(@RequestBody TaskRequestDTO request) {
        logger.info("[API] POST /api/tasks - 创建任务");
        try {
            String taskId = taskService.createTask(request);
            Map<String, String> result = new HashMap<>();
            result.put("taskId", taskId);
            logger.info("[API] POST /api/tasks - 任务创建成功, taskId: {}", taskId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/tasks - 任务创建失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskStatusDTO> getTaskStatus(@PathVariable String id) {
        logger.info("[API] GET /api/tasks/{} - 获取任务状态", id);
        try {
            TaskStatusDTO status = taskService.getTaskStatus(id);
            if (status != null) {
                logger.info("[API] GET /api/tasks/{} - 任务状态: {}", id, status.getStatus());
            } else {
                logger.warn("[API] GET /api/tasks/{} - 任务不存在", id);
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("[API] GET /api/tasks/{} - 获取任务状态失败", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<TaskStatusDTO>> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        logger.info("[API] GET /api/tasks - 获取任务列表, status: {}, page: {}, size: {}", status, page, size);
        try {
            List<TaskStatusDTO> tasks = taskService.getTasks(status, page, size);
            logger.info("[API] GET /api/tasks - 返回 {} 个任务", tasks.size());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("[API] GET /api/tasks - 获取任务列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeTask(@PathVariable String id) {
        logger.info("[API] POST /api/tasks/{}/execute - 执行任务", id);
        try {
            boolean success = taskService.executeTask(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "任务开始执行");
            logger.info("[API] POST /api/tasks/{}/execute - 任务执行{}", id, success ? "成功" : "失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/tasks/{}/execute - 任务执行失败", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String id) {
        logger.info("[API] POST /api/tasks/{}/cancel - 取消任务", id);
        try {
            boolean success = taskService.cancelTask(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "任务已取消");
            logger.info("[API] POST /api/tasks/{}/cancel - 任务取消{}", id, success ? "成功" : "失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] POST /api/tasks/{}/cancel - 任务取消失败", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String id) {
        logger.info("[API] DELETE /api/tasks/{} - 删除任务", id);
        try {
            boolean success = taskService.deleteTask(id);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "任务已删除");
            logger.info("[API] DELETE /api/tasks/{} - 任务删除{}", id, success ? "成功" : "失败");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[API] DELETE /api/tasks/{} - 任务删除失败", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
