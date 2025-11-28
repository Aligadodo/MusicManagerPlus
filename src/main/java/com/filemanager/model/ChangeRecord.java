package com.filemanager.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.filemanager.type.ExecStatus;
import com.filemanager.type.OperationType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class ChangeRecord {
    private String originalName;
    private String newName;
    private File fileHandle; // 原始文件句柄
    private boolean changed;
    private String newPath;  // 最终路径
    private OperationType opType;
    private Map<String, String> extraParams = new HashMap<>();
    private ExecStatus status = ExecStatus.PENDING;

    // 链式处理中的中间状态文件（如果不为空，说明这是上一步产生的临时状态）
    private File intermediateFile;

    public ChangeRecord(String o, String n, File f, boolean c, String p, OperationType op) {
        this.originalName=o; this.newName=n; this.fileHandle=f; this.changed=c; this.newPath=p; this.opType=op;
    }

    public ChangeRecord(String originalName, String name, File fileHandle, boolean b, String absolutePath, OperationType operationType, Map<String, String> params, ExecStatus status) {
        this.originalName = originalName;
        this.newName = name;
        this.fileHandle = fileHandle;
        this.changed = b;
        this.newPath = absolutePath;
        this.opType = operationType;
        this.extraParams = params;
        this.status = status;
    }

    // 获取当前应该处理的“源”文件（可能是原始文件，也可能是链式处理中上一步生成的文件）
    public File getCurrentSource() {
        return intermediateFile != null ? intermediateFile : fileHandle;
    }
}