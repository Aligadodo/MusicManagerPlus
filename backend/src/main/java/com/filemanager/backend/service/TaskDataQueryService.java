package com.filemanager.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filemanager.backend.dto.PaginationParams;
import com.filemanager.backend.dto.PaginatedResponse;
import com.filemanager.backend.dto.TaskRecordDTO;
import com.filemanager.backend.storage.ITaskStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 任务数据查询服务
 * 提供扫描/预览/执行三个阶段的数据分页查询功能
 */
@Service
public class TaskDataQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TaskDataQueryService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ITaskStorage taskStorage;

    @Autowired
    public TaskDataQueryService(@Qualifier("databaseTaskStorage") ITaskStorage taskStorage) {
        this.taskStorage = taskStorage;
    }

    /**
     * 查询扫描记录
     * @param taskId 任务ID
     * @param params 分页查询参数
     * @return 分页响应
     */
    public PaginatedResponse<TaskRecordDTO> queryScanRecords(String taskId, PaginationParams params) {
        logger.info("[TaskDataQuery] 查询扫描记录，taskId={}, params={}", taskId, params);

        try {
            // 1. 读取扫描结果文件
            String taskDir = taskStorage.getTaskDirectory(taskId);
            Path scanResultPath = Paths.get(taskDir, "scan", "data.json");

            if (!Files.exists(scanResultPath)) {
                logger.warn("[TaskDataQuery] 扫描结果文件不存在: {}", scanResultPath);
                return PaginatedResponse.empty(params.getPage(), params.getPageSize());
            }

            // 2. 读取扫描文件列表
            List<File> scannedFiles = readScanResultFile(scanResultPath);
            logger.debug("[TaskDataQuery] 读取到 {} 个扫描文件", scannedFiles.size());

            // 3. 转换为 DTO 列表
            List<TaskRecordDTO> records = scannedFiles.stream()
                    .map(TaskRecordDTO::fromScanFile)
                    .collect(Collectors.toList());

            // 4. 应用筛选条件
            records = applyScanFilters(records, params);

            // 5. 应用排序
            records = applySorting(records, params);

            // 6. 应用分页
            return applyPagination(records, params);

        } catch (Exception e) {
            logger.error("[TaskDataQuery] 查询扫描记录失败, taskId={}", taskId, e);
            return PaginatedResponse.empty(params.getPage(), params.getPageSize());
        }
    }

    /**
     * 查询预览记录
     * @param taskId 任务ID
     * @param params 分页查询参数
     * @return 分页响应
     */
    public PaginatedResponse<TaskRecordDTO> queryPreviewRecords(String taskId, PaginationParams params) {
        logger.info("[TaskDataQuery] 查询预览记录，taskId={}, params={}", taskId, params);

        try {
            // 1. 读取预览结果文件
            String taskDir = taskStorage.getTaskDirectory(taskId);
            Path previewResultPath = Paths.get(taskDir, "preview", "data.json");

            if (!Files.exists(previewResultPath)) {
                logger.warn("[TaskDataQuery] 预览结果文件不存在: {}", previewResultPath);
                return PaginatedResponse.empty(params.getPage(), params.getPageSize());
            }

            // 2. 读取变更记录列表
            List<com.filemanager.domain.entity.ChangeRecord> changeRecords = readPreviewResultFile(previewResultPath);
            logger.debug("[TaskDataQuery] 读取到 {} 条变更记录", changeRecords.size());

            // 3. 转换为 DTO 列表
            List<TaskRecordDTO> records = changeRecords.stream()
                    .map(TaskRecordDTO::fromChangeRecord)
                    .collect(Collectors.toList());

            // 4. 应用筛选条件
            records = applyPreviewFilters(records, params);

            // 5. 应用排序
            records = applySorting(records, params);

            // 6. 应用分页
            return applyPagination(records, params);

        } catch (Exception e) {
            logger.error("[TaskDataQuery] 查询预览记录失败, taskId={}", taskId, e);
            return PaginatedResponse.empty(params.getPage(), params.getPageSize());
        }
    }

    /**
     * 查询执行记录
     * @param taskId 任务ID
     * @param params 分页查询参数
     * @return 分页响应
     */
    public PaginatedResponse<TaskRecordDTO> queryExecutionRecords(String taskId, PaginationParams params) {
        logger.info("[TaskDataQuery] 查询执行记录，taskId={}, params={}", taskId, params);

        try {
            // 1. 读取执行结果文件
            String taskDir = taskStorage.getTaskDirectory(taskId);
            Path executionResultPath = Paths.get(taskDir, "execution", "data.json");

            if (!Files.exists(executionResultPath)) {
                logger.warn("[TaskDataQuery] 执行结果文件不存在: {}", executionResultPath);
                return PaginatedResponse.empty(params.getPage(), params.getPageSize());
            }

            // 2. 读取执行记录列表
            List<com.filemanager.backend.model.ExecutionResult.ExecutionRecord> executionRecords = 
                    readExecutionResultFile(executionResultPath);
            logger.debug("[TaskDataQuery] 读取到 {} 条执行记录", executionRecords.size());

            // 3. 转换为 DTO 列表
            List<TaskRecordDTO> records = executionRecords.stream()
                    .map(TaskRecordDTO::fromExecutionRecord)
                    .collect(Collectors.toList());

            // 4. 应用筛选条件
            records = applyExecutionFilters(records, params);

            // 5. 应用排序
            records = applySorting(records, params);

            // 6. 应用分页
            return applyPagination(records, params);

        } catch (Exception e) {
            logger.error("[TaskDataQuery] 查询执行记录失败, taskId={}", taskId, e);
            return PaginatedResponse.empty(params.getPage(), params.getPageSize());
        }
    }

    /**
     * 读取扫描结果文件
     */
    private List<File> readScanResultFile(Path scanResultPath) throws IOException {
        // 扫描结果文件格式：每行一个 JSON 对象，包含文件信息
        List<File> files = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(scanResultPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                try {
                    // 解析 JSON 对象
                    Map<String, Object> fileInfo = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {});
                    
                    // 从 JSON 中提取文件路径
                    String filePath = (String) fileInfo.get("filePath");
                    if (filePath != null) {
                        File file = new File(filePath);
                        // 不再检查文件是否存在，直接添加到列表
                        // 因为扫描时的文件路径可能是临时文件，现在可能已不存在
                        files.add(file);
                    }
                } catch (Exception e) {
                    logger.warn("[TaskDataQuery] 解析扫描结果行失败: {}", line);
                }
            }
        }
        
        return files;
    }

    /**
     * 读取预览结果文件
     */
    private List<com.filemanager.domain.entity.ChangeRecord> readPreviewResultFile(Path previewResultPath) throws IOException {
        String content = new String(Files.readAllBytes(previewResultPath));
        
        // 尝试解析为 PreviewResult 对象
        try {
            com.filemanager.backend.model.PreviewResult previewResult = 
                    objectMapper.readValue(content, com.filemanager.backend.model.PreviewResult.class);
            return previewResult.getChangeRecords() != null ? 
                    previewResult.getChangeRecords() : new ArrayList<>();
        } catch (Exception e) {
            // 如果解析失败，尝试直接解析为 ChangeRecord 列表
            try {
                return objectMapper.readValue(content, 
                        new TypeReference<List<com.filemanager.domain.entity.ChangeRecord>>() {});
            } catch (Exception ex) {
                logger.error("[TaskDataQuery] 解析预览结果文件失败", ex);
                return new ArrayList<>();
            }
        }
    }

    /**
     * 读取执行结果文件
     */
    private List<com.filemanager.backend.model.ExecutionResult.ExecutionRecord> readExecutionResultFile(Path executionResultPath) throws IOException {
        String content = new String(Files.readAllBytes(executionResultPath));
        
        // 尝试解析为 ExecutionResult 对象
        try {
            com.filemanager.backend.model.ExecutionResult executionResult = 
                    objectMapper.readValue(content, com.filemanager.backend.model.ExecutionResult.class);
            return executionResult.getExecutionRecords() != null ? 
                    executionResult.getExecutionRecords() : new ArrayList<>();
        } catch (Exception e) {
            // 如果解析失败，尝试直接解析为 ExecutionRecord 列表
            try {
                return objectMapper.readValue(content, 
                        new TypeReference<List<com.filemanager.backend.model.ExecutionResult.ExecutionRecord>>() {});
            } catch (Exception ex) {
                logger.error("[TaskDataQuery] 解析执行结果文件失败", ex);
                return new ArrayList<>();
            }
        }
    }

    /**
     * 应用扫描阶段筛选条件
     */
    private List<TaskRecordDTO> applyScanFilters(List<TaskRecordDTO> records, PaginationParams params) {
        return records.stream()
                .filter(record -> {
                    // 搜索关键词筛选
                    if (params.getSearch() != null && !params.getSearch().isEmpty()) {
                        String search = params.getSearch().toLowerCase();
                        boolean matches = false;
                        if (record.getOriginalName() != null) {
                            matches = record.getOriginalName().toLowerCase().contains(search);
                        }
                        if (!matches && record.getOriginalPath() != null) {
                            matches = record.getOriginalPath().toLowerCase().contains(search);
                        }
                        if (!matches) return false;
                    }
                    
                    // 文件类型筛选
                    if (params.getFileType() != null && !params.getFileType().isEmpty()) {
                        if (record.getFileType() == null || 
                            !record.getFileType().equalsIgnoreCase(params.getFileType())) {
                            return false;
                        }
                    }
                    
                    // 文件大小范围筛选
                    if (params.getMinSize() != null) {
                        if (record.getFileSize() == null || record.getFileSize() < params.getMinSize()) {
                            return false;
                        }
                    }
                    if (params.getMaxSize() != null) {
                        if (record.getFileSize() == null || record.getFileSize() > params.getMaxSize()) {
                            return false;
                        }
                    }
                    
                    // 时间范围筛选
                    if (params.getStartTime() != null) {
                        if (record.getLastModified() == null || record.getLastModified() < params.getStartTime()) {
                            return false;
                        }
                    }
                    if (params.getEndTime() != null) {
                        if (record.getLastModified() == null || record.getLastModified() > params.getEndTime()) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 应用预览阶段筛选条件
     */
    private List<TaskRecordDTO> applyPreviewFilters(List<TaskRecordDTO> records, PaginationParams params) {
        return records.stream()
                .filter(record -> {
                    // 搜索关键词筛选
                    if (params.getSearch() != null && !params.getSearch().isEmpty()) {
                        String search = params.getSearch().toLowerCase();
                        boolean matches = false;
                        if (record.getOriginalName() != null) {
                            matches = record.getOriginalName().toLowerCase().contains(search);
                        }
                        if (!matches && record.getNewName() != null) {
                            matches = record.getNewName().toLowerCase().contains(search);
                        }
                        if (!matches) return false;
                    }
                    
                    // 操作类型筛选
                    if (params.getOperationType() != null && !params.getOperationType().isEmpty()) {
                        if (record.getOperationType() == null || 
                            !record.getOperationType().equalsIgnoreCase(params.getOperationType())) {
                            return false;
                        }
                    }
                    
                    // 状态筛选
                    if (params.getStatus() != null && !params.getStatus().isEmpty()) {
                        if (record.getStatus() == null || 
                            !record.getStatus().equalsIgnoreCase(params.getStatus())) {
                            return false;
                        }
                    }
                    
                    // 是否变更筛选
                    if (params.getChanged() != null) {
                        if (record.getChanged() == null || !record.getChanged().equals(params.getChanged())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 应用执行阶段筛选条件
     */
    private List<TaskRecordDTO> applyExecutionFilters(List<TaskRecordDTO> records, PaginationParams params) {
        return records.stream()
                .filter(record -> {
                    // 搜索关键词筛选
                    if (params.getSearch() != null && !params.getSearch().isEmpty()) {
                        String search = params.getSearch().toLowerCase();
                        boolean matches = false;
                        if (record.getOriginalName() != null) {
                            matches = record.getOriginalName().toLowerCase().contains(search);
                        }
                        if (!matches && record.getNewName() != null) {
                            matches = record.getNewName().toLowerCase().contains(search);
                        }
                        if (!matches) return false;
                    }
                    
                    // 操作类型筛选
                    if (params.getOperationType() != null && !params.getOperationType().isEmpty()) {
                        if (record.getOperationType() == null || 
                            !record.getOperationType().equalsIgnoreCase(params.getOperationType())) {
                            return false;
                        }
                    }
                    
                    // 状态筛选
                    if (params.getStatus() != null && !params.getStatus().isEmpty()) {
                        if (record.getStatus() == null || 
                            !record.getStatus().equalsIgnoreCase(params.getStatus())) {
                            return false;
                        }
                    }
                    
                    // 时间范围筛选
                    if (params.getStartTime() != null) {
                        if (record.getExecuteTime() == null || record.getExecuteTime() < params.getStartTime()) {
                            return false;
                        }
                    }
                    if (params.getEndTime() != null) {
                        if (record.getExecuteTime() == null || record.getExecuteTime() > params.getEndTime()) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 应用排序
     */
    private List<TaskRecordDTO> applySorting(List<TaskRecordDTO> records, PaginationParams params) {
        if (params.getSortField() == null || params.getSortField().isEmpty()) {
            return records;
        }

        Comparator<TaskRecordDTO> comparator = getComparator(params.getSortField());
        if (comparator == null) {
            return records;
        }

        if ("desc".equalsIgnoreCase(params.getSortOrder())) {
            comparator = comparator.reversed();
        }

        return records.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    /**
     * 获取字段比较器
     */
    private Comparator<TaskRecordDTO> getComparator(String field) {
        switch (field) {
            case "originalName":
            case "fileName":
                return Comparator.comparing(TaskRecordDTO::getOriginalName, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "newName":
                return Comparator.comparing(TaskRecordDTO::getNewName, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "originalPath":
            case "filePath":
                return Comparator.comparing(TaskRecordDTO::getOriginalPath, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "newPath":
                return Comparator.comparing(TaskRecordDTO::getNewPath, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "fileSize":
                return Comparator.comparing(TaskRecordDTO::getFileSize, 
                        Comparator.nullsLast(Long::compareTo));
            case "fileType":
                return Comparator.comparing(TaskRecordDTO::getFileType, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "lastModified":
                return Comparator.comparing(TaskRecordDTO::getLastModified, 
                        Comparator.nullsLast(Long::compareTo));
            case "operationType":
                return Comparator.comparing(TaskRecordDTO::getOperationType, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "status":
                return Comparator.comparing(TaskRecordDTO::getStatus, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
            case "changed":
                return Comparator.comparing(TaskRecordDTO::getChanged, 
                        Comparator.nullsLast(Boolean::compareTo));
            case "analyzeTime":
                return Comparator.comparing(TaskRecordDTO::getAnalyzeTime, 
                        Comparator.nullsLast(Long::compareTo));
            case "executeTime":
            case "executionTime":
                return Comparator.comparing(TaskRecordDTO::getExecuteTime, 
                        Comparator.nullsLast(Long::compareTo));
            case "duration":
                return Comparator.comparing(TaskRecordDTO::getDuration, 
                        Comparator.nullsLast(Long::compareTo));
            default:
                return null;
        }
    }

    /**
     * 应用分页
     */
    private PaginatedResponse<TaskRecordDTO> applyPagination(List<TaskRecordDTO> records, PaginationParams params) {
        int total = records.size();
        int page = params.getPage();
        int pageSize = params.getPageSize();
        int offset = params.getOffset();

        // 计算分页
        List<TaskRecordDTO> pageRecords;
        if (offset >= total) {
            pageRecords = new ArrayList<>();
        } else {
            int end = Math.min(offset + pageSize, total);
            pageRecords = records.subList(offset, end);
        }

        return PaginatedResponse.of(pageRecords, total, page, pageSize);
    }
}
