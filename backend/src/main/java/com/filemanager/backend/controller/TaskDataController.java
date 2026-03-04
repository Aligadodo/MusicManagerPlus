package com.filemanager.backend.controller;

import com.filemanager.backend.dto.PaginationParams;
import com.filemanager.backend.dto.PaginatedResponse;
import com.filemanager.backend.dto.TaskRecordDTO;
import com.filemanager.backend.service.TaskDataQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务数据查询控制器
 * 提供扫描/预览/执行三个阶段的数据分页查询API
 */
@RestController
@RequestMapping("/api/tasks/{taskId}")
@CrossOrigin(origins = "*")
public class TaskDataController {

    private static final Logger logger = LoggerFactory.getLogger(TaskDataController.class);

    private final TaskDataQueryService taskDataQueryService;

    @Autowired
    public TaskDataController(TaskDataQueryService taskDataQueryService) {
        this.taskDataQueryService = taskDataQueryService;
    }

    /**
     * 查询扫描记录
     * @param taskId 任务ID
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认20）
     * @param search 搜索关键词
     * @param fileType 文件类型筛选
     * @param minSize 最小文件大小
     * @param maxSize 最大文件大小
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param sortField 排序字段
     * @param sortOrder 排序方向（asc/desc）
     * @return 分页响应
     */
    @GetMapping("/scan/records")
    public ResponseEntity<Map<String, Object>> queryScanRecords(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        logger.info("[TaskDataController] 查询扫描记录, taskId={}, page={}, pageSize={}", taskId, page, pageSize);

        try {
            // 构建查询参数
            PaginationParams params = new PaginationParams();
            params.setPage(page);
            params.setPageSize(pageSize);
            params.setSearch(search);
            params.setFileType(fileType);
            params.setMinSize(minSize);
            params.setMaxSize(maxSize);
            params.setStartTime(startTime);
            params.setEndTime(endTime);
            params.setSortField(sortField);
            params.setSortOrder(sortOrder);

            // 执行查询
            PaginatedResponse<TaskRecordDTO> response = taskDataQueryService.queryScanRecords(taskId, params);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "查询成功");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("[TaskDataController] 查询扫描记录失败, taskId={}", taskId, e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("data", PaginatedResponse.empty(page, pageSize));
            error.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.ok(error);
        }
    }

    /**
     * 查询预览记录
     * @param taskId 任务ID
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认20）
     * @param search 搜索关键词
     * @param operationType 操作类型筛选
     * @param status 状态筛选
     * @param changed 是否变更筛选
     * @param sortField 排序字段
     * @param sortOrder 排序方向（asc/desc）
     * @return 分页响应
     */
    @GetMapping("/preview/records")
    public ResponseEntity<Map<String, Object>> queryPreviewRecords(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean changed,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        logger.info("[TaskDataController] 查询预览记录, taskId={}, page={}, pageSize={}", taskId, page, pageSize);

        try {
            // 构建查询参数
            PaginationParams params = new PaginationParams();
            params.setPage(page);
            params.setPageSize(pageSize);
            params.setSearch(search);
            params.setOperationType(operationType);
            params.setStatus(status);
            params.setChanged(changed);
            params.setSortField(sortField);
            params.setSortOrder(sortOrder);

            // 执行查询
            PaginatedResponse<TaskRecordDTO> response = taskDataQueryService.queryPreviewRecords(taskId, params);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "查询成功");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("[TaskDataController] 查询预览记录失败, taskId={}", taskId, e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("data", PaginatedResponse.empty(page, pageSize));
            error.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.ok(error);
        }
    }

    /**
     * 查询执行记录
     * @param taskId 任务ID
     * @param page 页码（默认1）
     * @param pageSize 每页数量（默认20）
     * @param search 搜索关键词
     * @param operationType 操作类型筛选
     * @param status 状态筛选
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @param sortField 排序字段
     * @param sortOrder 排序方向（asc/desc）
     * @return 分页响应
     */
    @GetMapping("/execution/records")
    public ResponseEntity<Map<String, Object>> queryExecutionRecords(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String sortField,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        logger.info("[TaskDataController] 查询执行记录, taskId={}, page={}, pageSize={}", taskId, page, pageSize);

        try {
            // 构建查询参数
            PaginationParams params = new PaginationParams();
            params.setPage(page);
            params.setPageSize(pageSize);
            params.setSearch(search);
            params.setOperationType(operationType);
            params.setStatus(status);
            params.setStartTime(startTime);
            params.setEndTime(endTime);
            params.setSortField(sortField);
            params.setSortOrder(sortOrder);

            // 执行查询
            PaginatedResponse<TaskRecordDTO> response = taskDataQueryService.queryExecutionRecords(taskId, params);

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("message", "查询成功");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("[TaskDataController] 查询执行记录失败, taskId={}", taskId, e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("data", PaginatedResponse.empty(page, pageSize));
            error.put("message", "查询失败: " + e.getMessage());

            return ResponseEntity.ok(error);
        }
    }

    /**
     * 获取单条扫描记录详情
     * @param taskId 任务ID
     * @param recordId 记录ID
     * @return 记录详情
     */
    @GetMapping("/scan/records/{recordId}")
    public ResponseEntity<Map<String, Object>> getScanRecordDetail(
            @PathVariable String taskId,
            @PathVariable String recordId) {

        logger.info("[TaskDataController] 获取扫描记录详情, taskId={}, recordId={}", taskId, recordId);

        // TODO: 实现单条记录查询
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "功能开发中");

        return ResponseEntity.ok(result);
    }

    /**
     * 获取单条预览记录详情
     * @param taskId 任务ID
     * @param recordId 记录ID
     * @return 记录详情
     */
    @GetMapping("/preview/records/{recordId}")
    public ResponseEntity<Map<String, Object>> getPreviewRecordDetail(
            @PathVariable String taskId,
            @PathVariable String recordId) {

        logger.info("[TaskDataController] 获取预览记录详情, taskId={}, recordId={}", taskId, recordId);

        // TODO: 实现单条记录查询
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "功能开发中");

        return ResponseEntity.ok(result);
    }

    /**
     * 获取单条执行记录详情
     * @param taskId 任务ID
     * @param recordId 记录ID
     * @return 记录详情
     */
    @GetMapping("/execution/records/{recordId}")
    public ResponseEntity<Map<String, Object>> getExecutionRecordDetail(
            @PathVariable String taskId,
            @PathVariable String recordId) {

        logger.info("[TaskDataController] 获取执行记录详情, taskId={}, recordId={}", taskId, recordId);

        // TODO: 实现单条记录查询
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "功能开发中");

        return ResponseEntity.ok(result);
    }
}
