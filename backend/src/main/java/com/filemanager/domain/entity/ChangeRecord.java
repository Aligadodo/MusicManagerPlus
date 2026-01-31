package com.filemanager.domain.entity;

/**
 * 变更记录实体类
 */
public class ChangeRecord {
    private String id;              // 记录ID
    private String originalName;    // 原始文件名
    private String newName;         // 新文件名
    private String filePath;        // 文件路径
    private String status;          // 状态：PENDING, SUCCESS, FAILED, SKIPPED
    private String operationType;   // 操作类型：RENAME, MOVE, DELETE, COPY, METADATA_UPDATE
    private String reason;          // 变更原因
    private boolean changed;        // 是否变更

    public ChangeRecord() {
    }

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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }
}
