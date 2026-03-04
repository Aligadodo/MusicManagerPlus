package com.filemanager.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * 任务记录 DTO
 * 统一的数据传输对象，用于扫描/预览/执行三个阶段的数据展示
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskRecordDTO {
    
    // 基础字段（所有阶段通用）
    private String id;                              // 记录ID
    private String originalName;                    // 原文件名
    private String newName;                         // 新文件名
    private String originalPath;                    // 原路径
    private String newPath;                         // 新路径
    
    // 文件信息字段（扫描阶段主要使用）
    private Long fileSize;                          // 文件大小（字节）
    private String fileType;                        // 文件类型（扩展名）
    private Long lastModified;                      // 最后修改时间戳
    private Map<String, Object> metadata;           // 元数据
    
    // 操作相关字段（预览和执行阶段使用）
    private String operationType;                   // 操作类型
    private String status;                          // 状态
    private String reason;                          // 变更原因/说明
    private String failReason;                      // 失败原因
    private Map<String, String> extraParams;        // 额外参数
    
    // 状态标记字段
    private Boolean changed;                        // 是否发生变更
    private Boolean isCreate;                       // 是否是新建文件
    private Boolean isDeleteOrMove;                 // 是否是删除或移动操作
    private Boolean selected;                       // 是否被选中
    
    // 时间相关字段
    private Long analyzeTime;                       // 分析时间戳
    private Long executeTime;                       // 执行时间戳
    private Long duration;                          // 执行耗时（毫秒）
    
    // 执行相关字段
    private Integer retryCount;                     // 重试次数
    private List<String> processInfo;               // 处理信息列表
    
    public TaskRecordDTO() {
    }
    
    // 静态构造方法：从扫描文件创建
    public static TaskRecordDTO fromScanFile(java.io.File file) {
        TaskRecordDTO dto = new TaskRecordDTO();
        dto.setId(String.valueOf(file.hashCode()));
        dto.setOriginalName(file.getName());
        dto.setOriginalPath(file.getAbsolutePath());
        dto.setFileSize(file.length());
        dto.setLastModified(file.lastModified());
        
        // 提取文件类型
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            dto.setFileType(fileName.substring(dotIndex + 1).toLowerCase());
        }
        
        return dto;
    }
    
    // 静态构造方法：从变更记录创建
    public static TaskRecordDTO fromChangeRecord(com.filemanager.domain.entity.ChangeRecord record) {
        TaskRecordDTO dto = new TaskRecordDTO();
        dto.setId(record.getId());
        dto.setOriginalName(record.getOriginalName());
        dto.setNewName(record.getNewName());
        dto.setOriginalPath(record.getFilePath());
        dto.setNewPath(record.getNewPath());
        dto.setOperationType(record.getOperationType());
        dto.setStatus(record.getStatus());
        dto.setReason(record.getReason());
        dto.setFailReason(record.getFailReason());
        dto.setExtraParams(record.getExtraParams());
        dto.setChanged(record.isChanged());
        dto.setIsCreate(record.isCreate());
        dto.setIsDeleteOrMove(record.isDeleteOrMove());
        dto.setSelected(record.isSelected());
        dto.setAnalyzeTime(record.getAnalyzeTime());
        dto.setExecuteTime(record.getExecuteTime());
        
        // 设置文件信息（如果有）
        if (record.getFileHandle() != null) {
            dto.setFileSize(record.getFileHandle().length());
            String fileName = record.getFileHandle().getName();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                dto.setFileType(fileName.substring(dotIndex + 1));
            }
        }
        
        return dto;
    }
    
    // 静态构造方法：从执行记录创建
    public static TaskRecordDTO fromExecutionRecord(com.filemanager.backend.model.ExecutionResult.ExecutionRecord record) {
        TaskRecordDTO dto = fromChangeRecord(record);
        
        // 执行记录特有的字段
        dto.setDuration(record.getExecuteTime() - record.getAnalyzeTime());
        dto.setRetryCount(record.getRetryCount());
        
        return dto;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getOriginalName() {
        return originalName;
    }
    
    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }
    
    public String getNewName() {
        return newName;
    }
    
    public void setNewName(String newName) {
        this.newName = newName;
    }
    
    public String getOriginalPath() {
        return originalPath;
    }
    
    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }
    
    public String getNewPath() {
        return newPath;
    }
    
    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public Long getLastModified() {
        return lastModified;
    }
    
    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public String getOperationType() {
        return operationType;
    }
    
    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getFailReason() {
        return failReason;
    }
    
    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
    
    public Map<String, String> getExtraParams() {
        return extraParams;
    }
    
    public void setExtraParams(Map<String, String> extraParams) {
        this.extraParams = extraParams;
    }
    
    public Boolean getChanged() {
        return changed;
    }
    
    public void setChanged(Boolean changed) {
        this.changed = changed;
    }
    
    public Boolean getIsCreate() {
        return isCreate;
    }
    
    public void setIsCreate(Boolean isCreate) {
        this.isCreate = isCreate;
    }
    
    public Boolean getIsDeleteOrMove() {
        return isDeleteOrMove;
    }
    
    public void setIsDeleteOrMove(Boolean isDeleteOrMove) {
        this.isDeleteOrMove = isDeleteOrMove;
    }
    
    public Boolean getSelected() {
        return selected;
    }
    
    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
    
    public Long getAnalyzeTime() {
        return analyzeTime;
    }
    
    public void setAnalyzeTime(Long analyzeTime) {
        this.analyzeTime = analyzeTime;
    }
    
    public Long getExecuteTime() {
        return executeTime;
    }
    
    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }
    
    public Long getDuration() {
        return duration;
    }
    
    public void setDuration(Long duration) {
        this.duration = duration;
    }
    
    public Integer getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
    
    public List<String> getProcessInfo() {
        return processInfo;
    }
    
    public void setProcessInfo(List<String> processInfo) {
        this.processInfo = processInfo;
    }
    
    @Override
    public String toString() {
        return "TaskRecordDTO{" +
                "id='" + id + '\'' +
                ", originalName='" + originalName + '\'' +
                ", newName='" + newName + '\'' +
                ", operationType='" + operationType + '\'' +
                ", status='" + status + '\'' +
                ", changed=" + changed +
                '}';
    }
}
