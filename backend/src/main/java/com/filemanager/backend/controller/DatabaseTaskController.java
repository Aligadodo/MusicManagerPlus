package com.filemanager.backend.controller;

import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.backend.entity.TaskStagePO;
import com.filemanager.backend.entity.ChangeRecordPO;
import com.filemanager.backend.entity.TaskOperationLogPO;
import com.filemanager.backend.service.TaskInfoService;
import com.filemanager.backend.service.TaskStageService;
import com.filemanager.backend.service.ChangeRecordService;
import com.filemanager.backend.service.TaskOperationLogService;
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
@RequestMapping("/api/database/tasks")
@CrossOrigin(origins = "*")
public class DatabaseTaskController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseTaskController.class);

    @Autowired
    private TaskInfoService taskInfoService;

    @Autowired
    private TaskStageService taskStageService;

    @Autowired
    private ChangeRecordService changeRecordService;

    @Autowired
    private TaskOperationLogService taskOperationLogService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("DatabaseTaskController工作正常");
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TaskInfoPO> tasks;
            long total;
            
            if (keyword != null && !keyword.isEmpty()) {
                tasks = taskInfoService.searchTasks(keyword, page, size);
                total = taskInfoService.getTotalTaskCount();
            } else if (status != null && !status.isEmpty()) {
                tasks = taskInfoService.getTasksByPage(page, size);
                total = taskInfoService.getTaskCountByStatus(status);
            } else {
                tasks = taskInfoService.getTasksByPage(page, size);
                total = taskInfoService.getTotalTaskCount();
            }
            
            response.put("success", true);
            response.put("data", tasks);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取任务列表失败", e);
            response.put("success", false);
            response.put("message", "获取任务列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            TaskInfoPO task = taskInfoService.getTaskById(taskId);
            if (task == null) {
                response.put("success", false);
                response.put("message", "任务不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", task);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取任务详情失败: " + taskId, e);
            response.put("success", false);
            response.put("message", "获取任务详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{taskId}/stages")
    public ResponseEntity<Map<String, Object>> getTaskStages(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TaskStagePO> stages = taskStageService.getStagesByTaskId(taskId);
            
            response.put("success", true);
            response.put("data", stages);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取任务阶段失败: " + taskId, e);
            response.put("success", false);
            response.put("message", "获取任务阶段失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{taskId}/changes")
    public ResponseEntity<Map<String, Object>> getTaskChanges(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<ChangeRecordPO> changes = changeRecordService.getRecordsByPage(
                taskId, status, operationType, null, keyword, null, null, null, page, size);
            long total = changeRecordService.countByPage(taskId, status, operationType, null, keyword);
            
            response.put("success", true);
            response.put("data", changes);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取任务变更记录失败: " + taskId, e);
            response.put("success", false);
            response.put("message", "获取任务变更记录失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{taskId}/logs")
    public ResponseEntity<Map<String, Object>> getTaskLogs(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TaskOperationLogPO> logs = taskOperationLogService.getLogsByTaskId(taskId);
            long total = taskOperationLogService.getLogCountByTaskId(taskId);
            
            int start = (page - 1) * size;
            int end = Math.min(start + size, logs.size());
            if (start < logs.size()) {
                response.put("data", logs.subList(start, end));
            } else {
                response.put("data", new java.util.ArrayList<>());
            }
            
            response.put("success", true);
            response.put("total", total);
            response.put("page", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取任务日志失败: " + taskId, e);
            response.put("success", false);
            response.put("message", "获取任务日志失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = taskInfoService.deleteTask(taskId);
            
            if (success) {
                response.put("success", true);
                response.put("message", "任务已删除");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "删除任务失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            logger.error("删除任务失败: " + taskId, e);
            response.put("success", false);
            response.put("message", "删除任务失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalTasks", taskInfoService.getTotalTaskCount());
            statistics.put("totalChanges", changeRecordService.getTotalRecordCount());
            statistics.put("totalLogs", taskOperationLogService.getTotalLogCount());
            
            response.put("success", true);
            response.put("data", statistics);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
