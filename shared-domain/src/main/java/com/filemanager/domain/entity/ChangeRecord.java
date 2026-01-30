package com.filemanager.domain.entity;

public class ChangeRecord {
    private String id;
    private String originalName;
    private String newName;
    private String filePath;
    private boolean changed;
    private OperationType operationType;
    private ExecStatus status;
    private String failReason;

    public enum OperationType {
        RENAME,
        MOVE,
        DELETE,
        COPY,
        METADATA_UPDATE
    }

    public enum ExecStatus {
        PENDING,
        SUCCESS,
        FAILED
    }

    public ChangeRecord() {
    }

    public ChangeRecord(String id, String originalName, String newName, String filePath, boolean changed, OperationType operationType, ExecStatus status, String failReason) {
        this.id = id;
        this.originalName = originalName;
        this.newName = newName;
        this.filePath = filePath;
        this.changed = changed;
        this.operationType = operationType;
        this.status = status;
        this.failReason = failReason;
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

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public ExecStatus getStatus() {
        return status;
    }

    public void setStatus(ExecStatus status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }
}
