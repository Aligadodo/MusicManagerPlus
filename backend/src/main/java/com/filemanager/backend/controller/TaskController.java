package com.filemanager.backend.controller;

import com.filemanager.backend.model.*;
import com.filemanager.backend.service.TaskExecutionService;
import com.filemanager.backend.service.TaskStorageService;
import com.filemanager.backend.service.ChangeRecordService;
import com.filemanager.backend.service.TaskOperationLogService;
import com.filemanager.backend.mapper.TaskInfoMapper;
import com.filemanager.backend.entity.TaskInfoPO;
import com.filemanager.domain.dto.TaskRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务管理API控制器
 * 提供任务管理的RESTful API接口
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskStorageService storageService;
    private final TaskExecutionService executionService;
    private final ChangeRecordService changeRecordService;
    private final TaskOperationLogService taskOperationLogService;
    private final TaskInfoMapper taskInfoMapper;

    @Autowired
    public TaskController(TaskStorageService storageService, 
                         TaskExecutionService executionService,
                         ChangeRecordService changeRecordService,
                         TaskOperationLogService taskOperationLogService,
                         TaskInfoMapper taskInfoMapper) {
        this.storageService = storageService;
        this.executionService = executionService;
        this.changeRecordService = changeRecordService;
        this.taskOperationLogService = taskOperationLogService;
        this.taskInfoMapper = taskInfoMapper;
        logger.info("[TaskController] TaskController初始化成功");
    }

    /**
     * 测试端点
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("TaskController工作正常");
    }

    /**
     * 创建任务
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody TaskRequestDTO request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("[TaskController] 开始创建任务，请求参数: {}", request);
            String taskId = executionService.createTask(request);
            logger.info("[TaskController] 任务已创建，ID: {}", taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            logger.info("[TaskController] 任务信息已加载: {}", taskInfo);
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "任务已创建");
            
            logger.info("[TaskController] 任务创建成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 任务创建失败", e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "CREATE_TASK_FAILED");
            error.put("message", "任务创建失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取任务列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getTaskList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<TaskInfoPO> taskInfoList = taskInfoMapper.selectAll();
            
            List<Map<String, Object>> taskList = new java.util.ArrayList<>();
            for (TaskInfoPO taskInfoPO : taskInfoList) {
                if (status != null && !status.isEmpty() && !taskInfoPO.getStatus().equals(status)) {
                    continue;
                }
                if (keyword != null && !keyword.isEmpty() && 
                    !taskInfoPO.getTaskName().contains(keyword) && 
                    !taskInfoPO.getTaskId().contains(keyword)) {
                    continue;
                }
                taskList.add(taskInfoPOToMap(taskInfoPO));
            }
            
            int total = taskList.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            int fromIndex = (page - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            
            List<Map<String, Object>> pagedList = new java.util.ArrayList<>();
            if (fromIndex < total) {
                pagedList = taskList.subList(fromIndex, toIndex);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", pagedList);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", totalPages);
            
            response.put("success", true);
            response.put("data", data);
            
            logger.debug("[TaskController] 获取任务列表成功，共 {} 个任务", total);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取任务列表失败", e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_TASK_LIST_FAILED");
            error.put("message", "获取任务列表失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskDetail(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "TASK_NOT_FOUND");
                error.put("message", "任务不存在");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", taskInfoToDetailMap(taskInfo));
            
            logger.debug("[TaskController] 获取任务详情成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取任务详情失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_TASK_DETAIL_FAILED");
            error.put("message", "获取任务详情失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("[TaskController] 开始删除任务: {}", taskId);
            
            // 检查任务状态，不允许删除正在运行中的任务
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                // 尝试从数据库加载任务信息
                TaskInfoPO taskInfoPO = null;
                try {
                    taskInfoPO = taskInfoMapper.selectByTaskId(taskId);
                } catch (Exception e) {
                    logger.warn("[TaskController] 从数据库加载任务信息失败: {}", taskId, e);
                }
                
                if (taskInfoPO == null) {
                    response.put("success", false);
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", "TASK_NOT_FOUND");
                    error.put("message", "任务不存在");
                    response.put("error", error);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
                
                // 检查数据库中的任务状态
                String status = taskInfoPO.getStatus();
                if ("SCANNING".equals(status) || "PREVIEWING".equals(status) || "EXECUTING".equals(status)) {
                    response.put("success", false);
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", "TASK_RUNNING");
                    error.put("message", "任务正在运行中，无法删除");
                    response.put("error", error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            } else {
                // 检查内存中的任务状态
                TaskInfo.TaskStatus status = taskInfo.getStatus();
                if (status == TaskInfo.TaskStatus.SCANNING || status == TaskInfo.TaskStatus.PREVIEWING || status == TaskInfo.TaskStatus.EXECUTING) {
                    response.put("success", false);
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", "TASK_RUNNING");
                    error.put("message", "任务正在运行中，无法删除");
                    response.put("error", error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            }
            
            boolean deleted = storageService.deleteTask(taskId);
            
            if (!deleted) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "TASK_NOT_FOUND");
                error.put("message", "任务不存在");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            try {
                changeRecordService.deleteRecordsByTaskId(taskId);
                logger.info("[TaskController] 已删除任务变更记录: {}", taskId);
            } catch (Exception e) {
                logger.warn("[TaskController] 删除任务变更记录失败: {}", taskId, e);
            }
            
            try {
                taskOperationLogService.deleteLogsByTaskId(taskId);
                logger.info("[TaskController] 已删除任务操作日志: {}", taskId);
            } catch (Exception e) {
                logger.warn("[TaskController] 删除任务操作日志失败: {}", taskId, e);
            }
            
            try {
                taskInfoMapper.deleteByTaskId(taskId);
                logger.info("[TaskController] 已删除数据库中的任务记录: {}", taskId);
            } catch (Exception e) {
                logger.warn("[TaskController] 删除数据库中的任务记录失败: {}", taskId, e);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("deleted", true);
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "任务已删除");
            
            logger.info("[TaskController] 任务删除成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 任务删除失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "DELETE_TASK_FAILED");
            error.put("message", "任务删除失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 取消任务
     */
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean cancelled = executionService.cancelTask(taskId);
            
            if (!cancelled) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "TASK_NOT_RUNNING");
                error.put("message", "任务未在运行中");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("cancelled", true);
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "任务已取消");
            
            logger.info("[TaskController] 任务取消成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 任务取消失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "CANCEL_TASK_FAILED");
            error.put("message", "任务取消失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 取消阶段
     */
    @PostMapping("/{taskId}/stage/{stageType}/cancel")
    public ResponseEntity<Map<String, Object>> cancelStage(
            @PathVariable String taskId,
            @PathVariable String stageType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("[TaskController] 开始取消阶段: taskId={}, stageType={}", taskId, stageType);
            
            boolean cancelled = executionService.cancelStage(taskId, stageType);
            
            if (!cancelled) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "TASK_NOT_RUNNING");
                error.put("message", "任务未在运行中");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("stageType", stageType);
            data.put("cancelled", true);
            data.put("taskInfo", taskInfoToMap(taskInfo));
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "阶段已取消");
            
            logger.info("[TaskController] 阶段取消成功: taskId={}, stageType={}", taskId, stageType);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 阶段取消失败: taskId={}, stageType={}", taskId, stageType, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "CANCEL_STAGE_FAILED");
            error.put("message", "阶段取消失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 暂停任务
     */
    @PostMapping("/{taskId}/pause")
    public ResponseEntity<Map<String, Object>> pauseTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean paused = executionService.pauseTask(taskId);
            
            if (!paused) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "TASK_NOT_RUNNING");
                error.put("message", "任务未在运行中");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("paused", true);
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "任务已暂停");
            
            logger.info("[TaskController] 任务暂停成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 任务暂停失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "PAUSE_TASK_FAILED");
            error.put("message", "任务暂停失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 恢复任务
     */
    @PostMapping("/{taskId}/resume")
    public ResponseEntity<Map<String, Object>> resumeTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean resumed = executionService.resumeTask(taskId);
            
            if (!resumed) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "TASK_NOT_PAUSED");
                error.put("message", "任务未暂停");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("resumed", true);
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "任务已恢复");
            
            logger.info("[TaskController] 任务恢复成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 任务恢复失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "RESUME_TASK_FAILED");
            error.put("message", "任务恢复失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 开始扫描
     */
    @PostMapping("/{taskId}/scan")
    public ResponseEntity<Map<String, Object>> startScan(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            executionService.executeScan(taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "扫描已开始");
            
            logger.info("[TaskController] 扫描已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 扫描启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "START_SCAN_FAILED");
            error.put("message", "扫描启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 开始预览
     */
    @PostMapping("/{taskId}/preview")
    public ResponseEntity<Map<String, Object>> startPreview(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            executionService.executePreview(taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "预览已开始");
            
            logger.info("[TaskController] 预览已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 预览启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "START_PREVIEW_FAILED");
            error.put("message", "预览启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 开始执行
     */
    @PostMapping("/{taskId}/execute")
    public ResponseEntity<Map<String, Object>> startExecute(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> params) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean executeAll = true;
            List<String> selectedRecordIds = new java.util.ArrayList<>();
            
            if (params != null) {
                if (params.containsKey("executeAll")) {
                    executeAll = (boolean) params.get("executeAll");
                }
                if (params.containsKey("selectedRecordIds")) {
                    selectedRecordIds = (List<String>) params.get("selectedRecordIds");
                }
            }
            
            if (executeAll) {
                executionService.executeTask(taskId);
            } else {
                executionService.executeSelected(taskId, selectedRecordIds);
            }
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("executionNum", taskInfo.getStages().getExecution().getExecutionCount());
            data.put("status", taskInfo.getStatus().name());
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "执行已开始");
            
            logger.info("[TaskController] 执行已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 执行启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "START_EXECUTE_FAILED");
            error.put("message", "执行启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 重试失败
     */
    @PostMapping("/{taskId}/retry")
    public ResponseEntity<Map<String, Object>> retryFailed(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            executionService.retryFailed(taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("executionNum", taskInfo.getStages().getExecution().getExecutionCount());
            data.put("status", taskInfo.getStatus().name());
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "重试已开始");
            
            logger.info("[TaskController] 重试已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 重试启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "RETRY_FAILED");
            error.put("message", "重试启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 重新扫描
     */
    @PostMapping("/{taskId}/restart/scan")
    public ResponseEntity<Map<String, Object>> restartScan(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            executionService.restartScan(taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "重新扫描已开始");
            
            logger.info("[TaskController] 重新扫描已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 重新扫描启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "RESTART_SCAN_FAILED");
            error.put("message", "重新扫描启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 重新预览
     */
    @PostMapping("/{taskId}/restart/preview")
    public ResponseEntity<Map<String, Object>> restartPreview(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            executionService.restartPreview(taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "重新预览已开始");
            
            logger.info("[TaskController] 重新预览已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 重新预览启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "RESTART_PREVIEW_FAILED");
            error.put("message", "重新预览启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 重新执行
     */
    @PostMapping("/{taskId}/restart/execution")
    public ResponseEntity<Map<String, Object>> restartExecution(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            executionService.restartExecution(taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "重新执行已开始");
            
            logger.info("[TaskController] 重新执行已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 重新执行启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "RESTART_EXECUTION_FAILED");
            error.put("message", "重新执行启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 重新运行任务（从扫描开始）
     */
    @PostMapping("/{taskId}/rerun")
    public ResponseEntity<Map<String, Object>> rerunTask(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            executionService.rerunTask(taskId);
            
            TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
            if (taskInfo == null) {
                taskInfo = executionService.getTaskProgress(taskId);
            }
            
            response.put("success", true);
            response.put("data", taskInfoToMap(taskInfo));
            response.put("message", "重新运行已开始");
            
            logger.info("[TaskController] 重新运行已开始: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 重新运行启动失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "RERUN_TASK_FAILED");
            error.put("message", "重新运行启动失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取扫描统计信息
     */
    @GetMapping("/{taskId}/scan/statistics")
    public ResponseEntity<Map<String, Object>> getScanStatistics(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            TaskInfo.ScanStage scanStage = storageService.loadScanStatistics(taskId);
            
            if (scanStage == null) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "SCAN_STATISTICS_NOT_FOUND");
                error.put("message", "扫描统计信息不存在");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", scanStageToMap(scanStage));
            
            logger.debug("[TaskController] 获取扫描统计成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取扫描统计失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_SCAN_STATISTICS_FAILED");
            error.put("message", "获取扫描统计失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取扫描文件列表
     */
    @GetMapping("/{taskId}/scan/files")
    public ResponseEntity<Map<String, Object>> getScanFiles(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> records = storageService.readScanData(taskId, page, pageSize);
            
            int total = records.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", records);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", totalPages);
            
            response.put("success", true);
            response.put("data", data);
            
            logger.debug("[TaskController] 获取扫描文件列表成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取扫描文件列表失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_SCAN_FILES_FAILED");
            error.put("message", "获取扫描文件列表失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 导出扫描文件列表
     */
    @GetMapping("/{taskId}/scan/files/export")
    public ResponseEntity<byte[]> exportScanFiles(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            String filePath = storageService.getTaskDirectory(taskId) + "/scan/data.json";
            File file = new File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            String contentType = "text/csv";
            String extension = "csv";
            
            if ("json".equals(format)) {
                contentType = "application/json";
                extension = "json";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", "scan_files_" + taskId + "." + extension);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
            
        } catch (IOException e) {
            logger.error("[TaskController] 导出扫描文件列表失败: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取预览统计信息
     */
    @GetMapping("/{taskId}/preview/statistics")
    public ResponseEntity<Map<String, Object>> getPreviewStatistics(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            TaskInfo.PreviewStage previewStage = storageService.loadPreviewStatistics(taskId);
            
            if (previewStage == null) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "PREVIEW_STATISTICS_NOT_FOUND");
                error.put("message", "预览统计信息不存在");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", previewStageToMap(previewStage));
            
            logger.debug("[TaskController] 获取预览统计成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取预览统计失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_PREVIEW_STATISTICS_FAILED");
            error.put("message", "获取预览统计失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取预览变更记录
     */
    @GetMapping("/{taskId}/preview/records")
    public ResponseEntity<Map<String, Object>> getPreviewRecords(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Boolean changed,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> records = storageService.readPreviewData(taskId, page, pageSize);
            
            int total = records.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", records);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", totalPages);
            
            response.put("success", true);
            response.put("data", data);
            
            logger.debug("[TaskController] 获取预览变更记录成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取预览变更记录失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_PREVIEW_RECORDS_FAILED");
            error.put("message", "获取预览变更记录失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 导出预览变更记录
     */
    @GetMapping("/{taskId}/preview/records/export")
    public ResponseEntity<byte[]> exportPreviewRecords(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            String filePath = storageService.getTaskDirectory(taskId) + "/preview/data.json";
            File file = new File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            String contentType = "text/csv";
            String extension = "csv";
            
            if ("json".equals(format)) {
                contentType = "application/json";
                extension = "json";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", "preview_records_" + taskId + "." + extension);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
            
        } catch (IOException e) {
            logger.error("[TaskController] 导出预览变更记录失败: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取执行历史
     */
    @GetMapping("/{taskId}/execution/history")
    public ResponseEntity<Map<String, Object>> getExecutionHistory(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Integer> executionNums = storageService.getExecutionHistory(taskId);
            
            List<Map<String, Object>> history = new java.util.ArrayList<>();
            for (int executionNum : executionNums) {
                String executionId = "execution_" + String.format("%03d", executionNum);
                TaskInfo.ExecutionStage executionStage = storageService.loadExecutionStatistics(taskId, executionNum);
                if (executionStage != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("executionNum", executionNum);
                    item.put("executionId", executionId);
                    item.putAll(executionStageToMap(executionStage));
                    history.add(item);
                }
            }
            
            response.put("success", true);
            response.put("data", history);
            
            logger.debug("[TaskController] 获取执行历史成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取执行历史失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_EXECUTION_HISTORY_FAILED");
            error.put("message", "获取执行历史失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取执行统计信息
     */
    @GetMapping("/{taskId}/execution/{executionNum}/statistics")
    public ResponseEntity<Map<String, Object>> getExecutionStatistics(
            @PathVariable String taskId,
            @PathVariable int executionNum) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            TaskInfo.ExecutionStage executionStage = storageService.loadExecutionStatistics(taskId, executionNum);
            
            if (executionStage == null) {
                response.put("success", false);
                Map<String, Object> error = new HashMap<>();
                error.put("code", "EXECUTION_STATISTICS_NOT_FOUND");
                error.put("message", "执行统计信息不存在");
                response.put("error", error);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("data", executionStageToMap(executionStage));
            
            logger.debug("[TaskController] 获取执行统计成功: {} - execution_{}", taskId, executionNum);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取执行统计失败: {} - execution_{}", taskId, executionNum, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_EXECUTION_STATISTICS_FAILED");
            error.put("message", "获取执行统计失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取执行记录
     */
    @GetMapping("/{taskId}/execution/{executionNum}/records")
    public ResponseEntity<Map<String, Object>> getExecutionRecords(
            @PathVariable String taskId,
            @PathVariable int executionNum,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> records = storageService.readExecutionData(taskId, executionNum, page, pageSize);
            
            int total = records.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", records);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", totalPages);
            
            response.put("success", true);
            response.put("data", data);
            
            logger.debug("[TaskController] 获取执行记录成功: {} - execution_{}", taskId, executionNum);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取执行记录失败: {} - execution_{}", taskId, executionNum, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_EXECUTION_RECORDS_FAILED");
            error.put("message", "获取执行记录失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 导出执行记录
     */
    @GetMapping("/{taskId}/execution/{executionNum}/records/export")
    public ResponseEntity<byte[]> exportExecutionRecords(
            @PathVariable String taskId,
            @PathVariable int executionNum,
            @RequestParam(defaultValue = "csv") String format) {
        try {
            String filePath = storageService.getTaskDirectory(taskId) + "/execution/execution_" + String.format("%03d", executionNum) + "/data.json";
            File file = new File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            String contentType = "text/csv";
            String extension = "csv";
            
            if ("json".equals(format)) {
                contentType = "application/json";
                extension = "json";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", "execution_records_" + taskId + "_execution_" + String.format("%03d", executionNum) + "." + extension);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
            
        } catch (IOException e) {
            logger.error("[TaskController] 导出执行记录失败: {} - execution_{}", taskId, executionNum, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取任务日志
     */
    @GetMapping("/{taskId}/logs")
    public ResponseEntity<Map<String, Object>> getTaskLogs(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> logs = storageService.readTaskLog(taskId, page, pageSize);
            
            int total = logs.size();
            int totalPages = (int) Math.ceil((double) total / pageSize);
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", logs);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            data.put("totalPages", totalPages);
            
            response.put("success", true);
            response.put("data", data);
            
            logger.debug("[TaskController] 获取任务日志成功: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 获取任务日志失败: {}", taskId, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "GET_TASK_LOGS_FAILED");
            error.put("message", "获取任务日志失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 导出任务日志
     */
    @GetMapping("/{taskId}/logs/export")
    public ResponseEntity<byte[]> exportTaskLogs(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "txt") String format) {
        try {
            String filePath = storageService.getTaskDirectory(taskId) + "/task.log";
            File file = new File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            byte[] fileContent = Files.readAllBytes(file.toPath());
            
            String contentType = "text/plain";
            String extension = "txt";
            
            if ("json".equals(format)) {
                contentType = "application/json";
                extension = "json";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", "task_logs_" + taskId + "." + extension);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
            
        } catch (IOException e) {
            logger.error("[TaskController] 导出任务日志失败: {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> taskInfoToMap(TaskInfo taskInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskInfo.getTaskId());
        map.put("taskName", taskInfo.getTaskName());
        map.put("createdAt", taskInfo.getCreatedAt());
        map.put("status", taskInfo.getStatus().name());
        map.put("currentStage", taskInfo.getCurrentStage());
        map.put("progress", taskInfo.getOverallProgress());
        map.put("message", taskInfo.getMessage());
        return map;
    }

    private Map<String, Object> taskInfoPOToMap(TaskInfoPO taskInfoPO) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskInfoPO.getTaskId());
        map.put("taskName", taskInfoPO.getTaskName());
        map.put("createdAt", taskInfoPO.getCreatedAt().getTime());
        map.put("status", taskInfoPO.getStatus());
        map.put("currentStage", taskInfoPO.getCurrentStage());
        map.put("progress", taskInfoPO.getOverallProgress());
        map.put("message", taskInfoPO.getMessage());
        return map;
    }

    private Map<String, Object> taskInfoToDetailMap(TaskInfo taskInfo) {
        Map<String, Object> map = new HashMap<>();
        map.put("taskId", taskInfo.getTaskId());
        map.put("taskName", taskInfo.getTaskName());
        map.put("createdAt", taskInfo.getCreatedAt());
        map.put("status", taskInfo.getStatus().name());
        map.put("currentStage", taskInfo.getCurrentStage());
        map.put("progress", taskInfo.getOverallProgress());
        map.put("message", taskInfo.getMessage());
        map.put("configSnapshotId", taskInfo.getConfigSnapshotId());
        
        if (taskInfo.getStages() != null) {
            Map<String, Object> stages = new HashMap<>();
            stages.put("scan", scanStageToMap(taskInfo.getStages().getScan()));
            stages.put("preview", previewStageToMap(taskInfo.getStages().getPreview()));
            stages.put("execution", executionStageToMap(taskInfo.getStages().getExecution()));
            map.put("stages", stages);
        }
        
        TaskConfigSnapshot configSnapshot = taskInfo.getConfigSnapshot();
        if (configSnapshot != null) {
            Map<String, Object> configMap = new HashMap<>();
            
            List<TaskConfigSnapshot.SourceDirectoryConfig> sourceDirs = configSnapshot.getSourceDirectories();
            if (sourceDirs != null && !sourceDirs.isEmpty()) {
                List<Map<String, Object>> sourceDirList = new java.util.ArrayList<>();
                for (TaskConfigSnapshot.SourceDirectoryConfig sourceDir : sourceDirs) {
                    Map<String, Object> sourceDirMap = new HashMap<>();
                    sourceDirMap.put("path", sourceDir.getPath());
                    sourceDirMap.put("depth", sourceDir.getDepth());
                    sourceDirMap.put("recursive", sourceDir.isRecursive());
                    sourceDirMap.put("includePatterns", sourceDir.getIncludePatterns());
                    sourceDirMap.put("excludePatterns", sourceDir.getExcludePatterns());
                    sourceDirList.add(sourceDirMap);
                }
                configMap.put("sourceDirectories", sourceDirList);
            }
            
            TaskConfigSnapshot.PipelineConfig pipelineConfig = configSnapshot.getPipelineConfig();
            if (pipelineConfig != null) {
                Map<String, Object> pipelineMap = new HashMap<>();
                pipelineMap.put("pipelineId", pipelineConfig.getPipelineId());
                pipelineMap.put("name", pipelineConfig.getName());
                pipelineMap.put("description", pipelineConfig.getDescription());
                pipelineMap.put("items", pipelineConfig.getItems());
                configMap.put("pipelineConfig", pipelineMap);
            }
            
            TaskConfigSnapshot.GlobalSettings globalSettings = configSnapshot.getGlobalSettings();
            if (globalSettings != null) {
                Map<String, Object> settingsMap = new HashMap<>();
                settingsMap.put("maxThreads", globalSettings.getMaxThreads());
                settingsMap.put("timeout", globalSettings.getTimeout());
                settingsMap.put("dryRun", globalSettings.isDryRun());
                settingsMap.put("overwrite", globalSettings.isOverwrite());
                settingsMap.put("backup", globalSettings.isBackup());
                settingsMap.put("backupPath", globalSettings.getBackupPath());
                settingsMap.put("retryCount", globalSettings.getRetryCount());
                settingsMap.put("retryInterval", globalSettings.getRetryInterval());
                configMap.put("globalSettings", settingsMap);
            }
            
            map.put("configSnapshot", configMap);
        }
        
        return map;
    }

    private Map<String, Object> scanStageToMap(TaskInfo.ScanStage scanStage) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", scanStage.getStatus());
        map.put("scanStartTime", scanStage.getScanStartTime());
        map.put("scanEndTime", scanStage.getScanEndTime());
        map.put("scanDuration", scanStage.getScanDuration());
        map.put("totalFiles", scanStage.getTotalFiles());
        return map;
    }

    private Map<String, Object> previewStageToMap(TaskInfo.PreviewStage previewStage) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", previewStage.getStatus());
        map.put("previewStartTime", previewStage.getPreviewStartTime());
        map.put("previewEndTime", previewStage.getPreviewEndTime());
        map.put("previewDuration", previewStage.getPreviewDuration());
        map.put("totalFiles", previewStage.getTotalFiles());
        map.put("processedFiles", previewStage.getProcessedFiles());
        map.put("changedFiles", previewStage.getChangedFiles());
        map.put("unchangedFiles", previewStage.getUnchangedFiles());
        return map;
    }

    private Map<String, Object> executionStageToMap(TaskInfo.ExecutionStage executionStage) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", executionStage.getStatus());
        map.put("executionCount", executionStage.getExecutionCount());
        map.put("currentExecution", executionStage.getCurrentExecution());
        map.put("executionStartTime", executionStage.getExecutionStartTime());
        map.put("executionEndTime", executionStage.getExecutionEndTime());
        map.put("executionDuration", executionStage.getExecutionDuration());
        map.put("totalFiles", executionStage.getTotalFiles());
        map.put("processedFiles", executionStage.getProcessedFiles());
        map.put("successCount", executionStage.getSuccessCount());
        map.put("failedCount", executionStage.getFailedCount());
        map.put("skippedCount", executionStage.getSkippedCount());
        return map;
    }



    /**
     * 清空所有任务
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAllTasks() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("[TaskController] 开始清空所有任务");
            
            List<String> allTaskIds = storageService.getAllTaskIds();
            int deletedCount = 0;
            int skippedCount = 0;
            
            for (String taskId : allTaskIds) {
                try {
                    // 检查任务状态，跳过正在运行中的任务
                    boolean isRunning = false;
                    
                    // 尝试从内存加载任务信息
                    TaskInfo taskInfo = storageService.loadTaskInfo(taskId);
                    if (taskInfo != null) {
                        TaskInfo.TaskStatus status = taskInfo.getStatus();
                        if (status == TaskInfo.TaskStatus.SCANNING || status == TaskInfo.TaskStatus.PREVIEWING || status == TaskInfo.TaskStatus.EXECUTING) {
                            isRunning = true;
                        }
                    } else {
                        // 尝试从数据库加载任务信息
                        try {
                            TaskInfoPO taskInfoPO = taskInfoMapper.selectByTaskId(taskId);
                            if (taskInfoPO != null) {
                                String status = taskInfoPO.getStatus();
                                if ("SCANNING".equals(status) || "PREVIEWING".equals(status) || "EXECUTING".equals(status)) {
                                    isRunning = true;
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("[TaskController] 从数据库加载任务信息失败: {}", taskId, e);
                        }
                    }
                    
                    if (isRunning) {
                        logger.warn("[TaskController] 跳过正在运行中的任务: {}", taskId);
                        skippedCount++;
                        continue;
                    }
                    
                    storageService.deleteTask(taskId);
                    changeRecordService.deleteRecordsByTaskId(taskId);
                    taskOperationLogService.deleteLogsByTaskId(taskId);
                    try {
                        taskInfoMapper.deleteByTaskId(taskId);
                        logger.info("[TaskController] 已删除数据库中的任务记录: {}", taskId);
                    } catch (Exception e) {
                        logger.warn("[TaskController] 删除数据库中的任务记录失败: {}", taskId, e);
                    }
                    deletedCount++;
                } catch (Exception e) {
                    logger.warn("[TaskController] 删除任务失败: {}", taskId, e);
                }
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("deletedCount", deletedCount);
            data.put("skippedCount", skippedCount);
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "所有非运行中任务已清空");
            
            logger.info("[TaskController] 清空所有任务成功，共删除 {} 个任务，跳过 {} 个正在运行中的任务", deletedCount, skippedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 清空所有任务失败", e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "CLEAR_ALL_TASKS_FAILED");
            error.put("message", "清空所有任务失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 清理阶段数据
     */
    @DeleteMapping("/{taskId}/stage/{stageType}/data")
    public ResponseEntity<Map<String, Object>> clearStageData(
            @PathVariable String taskId,
            @PathVariable String stageType) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("[TaskController] 开始清理阶段数据: taskId={}, stageType={}", taskId, stageType);
            
            switch (stageType.toUpperCase()) {
                case "PREVIEW":
                    changeRecordService.deleteRecordsByTaskId(taskId);
                    logger.info("[TaskController] 已清理预览阶段数据: {}", taskId);
                    break;
                case "EXECUTION":
                    changeRecordService.deleteRecordsByTaskId(taskId);
                    logger.info("[TaskController] 已清理执行阶段数据: {}", taskId);
                    break;
                default:
                    response.put("success", false);
                    Map<String, Object> error = new HashMap<>();
                    error.put("code", "INVALID_STAGE_TYPE");
                    error.put("message", "无效的阶段类型: " + stageType);
                    response.put("error", error);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("taskId", taskId);
            data.put("stageType", stageType);
            data.put("cleared", true);
            
            response.put("success", true);
            response.put("data", data);
            response.put("message", "阶段数据已清理");
            
            logger.info("[TaskController] 阶段数据清理成功: taskId={}, stageType={}", taskId, stageType);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[TaskController] 清理阶段数据失败: taskId={}, stageType={}", taskId, stageType, e);
            
            response.put("success", false);
            Map<String, Object> error = new HashMap<>();
            error.put("code", "CLEAR_STAGE_DATA_FAILED");
            error.put("message", "清理阶段数据失败");
            error.put("details", e.getMessage());
            response.put("error", error);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
