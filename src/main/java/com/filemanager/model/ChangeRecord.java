/* 
 * Copyright (c) 2026 hrcao (chrse1997@163.com) 
 * Licensed under GPLv3 + Non-Commercial Clause. 
 * You may not use this file except in compliance with the License. 
 * See the LICENSE file in the project root for more information. 
 * Author: hrcao 
 * Mail: chrse1997@163.com 
 * Date: 2026-01-12 
 */
package com.filemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 28667
 */
@Data
@NoArgsConstructor
public class ChangeRecord {
    private String originalName;
    private String newName;
    // 原始文件句柄
    private File fileHandle;
    private boolean changed;
    // 最终路径
    private String newPath;
    private OperationType opType;
    private Map<String, String> extraParams = new HashMap<>();
    private ExecStatus status = ExecStatus.PENDING;
    // 失败的原因
    private String failReason;
    private boolean isCreate = false;
    private boolean isDeleteOrMove = false;

    // 链式处理中的中间状态文件（如果不为空，说明这是上一步产生的临时状态）
    private File intermediateFile;

    public ChangeRecord(String o, String n, File f, boolean c, String p, OperationType op) {
        this.originalName = o;
        this.newName = n;
        this.fileHandle = f;
        this.changed = c && op != null && OperationType.NONE != op;
        this.newPath = p;
        this.opType = op;
    }

    public ChangeRecord(String originalName, String name, File fileHandle, boolean b, String absolutePath, OperationType op, Map<String, String> params, ExecStatus status) {
        this.originalName = originalName;
        this.newName = name;
        this.fileHandle = fileHandle;
        this.changed = b && op != null && OperationType.NONE != op;
        this.newPath = absolutePath;
        this.opType = op;
        this.extraParams = params;
        this.status = status;
    }
    

    // 获取当前应该处理的“源”文件（可能是原始文件，也可能是链式处理中上一步生成的文件）
    public File getCurrentSource() {
        return intermediateFile != null ? intermediateFile : fileHandle;
    }

    // 手动添加常用的getter和setter方法，确保其他类能够访问这些属性
    public String getOriginalName() { return this.originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getNewName() { return this.newName; }
    public void setNewName(String newName) { this.newName = newName; }
    public File getFileHandle() { return this.fileHandle; }
    public void setFileHandle(File fileHandle) { this.fileHandle = fileHandle; }
    public boolean isChanged() { return this.changed; }
    public void setChanged(boolean changed) { this.changed = changed; }
    public String getNewPath() { return this.newPath; }
    public void setNewPath(String newPath) { this.newPath = newPath; }
    public OperationType getOpType() { return this.opType; }
    public void setOpType(OperationType opType) { this.opType = opType; }
    public Map<String, String> getExtraParams() { return this.extraParams; }
    public void setExtraParams(Map<String, String> extraParams) { this.extraParams = extraParams; }
    public ExecStatus getStatus() { return this.status; }
    public void setStatus(ExecStatus status) { this.status = status; }
    public String getFailReason() { return this.failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public boolean isCreate() { return this.isCreate; }
    public void setCreate(boolean create) { this.isCreate = create; }
    public boolean isDeleteOrMove() { return this.isDeleteOrMove; }
    public void setDeleteOrMove(boolean deleteOrMove) { this.isDeleteOrMove = deleteOrMove; }
    public File getIntermediateFile() { return this.intermediateFile; }
    public void setIntermediateFile(File intermediateFile) { this.intermediateFile = intermediateFile; }
}