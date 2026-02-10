package com.filemanager.domain.entity;

import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeRecord {
    private String id;
    private String originalName;
    private String newName;
    private File fileHandle;
    private boolean changed;
    private String newPath;
    private String operationType;
    private Map<String, String> extraParams = new HashMap<>();
    private String status;
    private String failReason;
    private boolean isCreate = false;
    private boolean isDeleteOrMove = false;
    private boolean selected = false;
    private File intermediateFile;
    private List<String> processInfo = new ArrayList<>();
    private long analyzeTime;
    private long executeTime;
    private String filePath;
    private String reason;

    public ChangeRecord() {
    }

    public ChangeRecord(String o, String n, File f, boolean c, String p, String op) {
        this.originalName = o;
        this.newName = n;
        this.fileHandle = f;
        this.changed = c && op != null && !"NONE".equals(op);
        this.newPath = p;
        this.operationType = op;
    }

    public ChangeRecord(String originalName, String newName, File fileHandle, 
        boolean changed, String newPath, OperationType opType) {
        this.originalName = originalName;
        this.newName = newName;
        this.fileHandle = fileHandle;
        this.changed = changed && opType != null && OperationType.NONE != opType;
        this.newPath = newPath;
        this.operationType = opType != null ? opType.name() : "NONE";
    }

    public ChangeRecord(String originalName, String newName, File fileHandle, 
        boolean changed, String newPath, OperationType opType, 
        Map<String, String> extraParams, ExecStatus status) {
        this(originalName, newName, fileHandle, changed, newPath, opType);
        this.extraParams = extraParams != null ? extraParams : new HashMap<>();
        this.status = status != null ? status.name() : "PENDING";
    }

    public File getCurrentSource() {
        return intermediateFile != null ? intermediateFile : fileHandle;
    }

    public void addProcessInfo(String info) {
        this.processInfo.add(info);
    }

    public void addProcessInfo(String key, String value) {
        addProcessInfo(key + ": " + value);
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

    public File getFileHandle() {
        return fileHandle;
    }

    public void setFileHandle(File fileHandle) {
        this.fileHandle = fileHandle;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public OperationType getOperationTypeEnum() {
        if (operationType == null) {
            return OperationType.NONE;
        }
        try {
            return OperationType.valueOf(operationType);
        } catch (IllegalArgumentException e) {
            return OperationType.NONE;
        }
    }

    public void setOperationType(OperationType opType) {
        this.operationType = opType != null ? opType.name() : "NONE";
    }

    public Map<String, String> getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(Map<String, String> extraParams) {
        this.extraParams = extraParams;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ExecStatus getStatusEnum() {
        if (status == null) {
            return ExecStatus.PENDING;
        }
        try {
            return ExecStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return ExecStatus.PENDING;
        }
    }

    public void setStatus(ExecStatus status) {
        this.status = status != null ? status.name() : "PENDING";
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public boolean isCreate() {
        return isCreate;
    }

    public void setCreate(boolean create) {
        this.isCreate = create;
    }

    public boolean isDeleteOrMove() {
        return isDeleteOrMove;
    }

    public void setDeleteOrMove(boolean deleteOrMove) {
        this.isDeleteOrMove = deleteOrMove;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public File getIntermediateFile() {
        return intermediateFile;
    }

    public void setIntermediateFile(File intermediateFile) {
        this.intermediateFile = intermediateFile;
    }

    public List<String> getProcessInfo() {
        return processInfo;
    }

    public void setProcessInfo(List<String> processInfo) {
        this.processInfo = processInfo;
    }

    public long getAnalyzeTime() {
        return analyzeTime;
    }

    public void setAnalyzeTime(long analyzeTime) {
        this.analyzeTime = analyzeTime;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
