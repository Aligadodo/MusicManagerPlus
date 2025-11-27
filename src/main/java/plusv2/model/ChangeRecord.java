package plusv2.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import plusv2.type.ExecStatus;
import plusv2.type.OperationType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// 使用 NoArgsConstructor 配合手动全参构造，避免 @AllArgsConstructor 隐患
@Data
@NoArgsConstructor
public class ChangeRecord {
    private String originalName;
    private String newName;
    private File fileHandle;
    private boolean changed;
    private String newPath;
    private OperationType opType;
    private Map<String, String> extraParams;
    private ExecStatus status = ExecStatus.PENDING;

    // 全参数构造方法
    public ChangeRecord(String originalName, String newName, File fileHandle,
                        boolean changed, String newPath, OperationType opType,
                        Map<String, String> extraParams, ExecStatus status) {
        this.originalName = originalName;
        this.newName = newName;
        this.fileHandle = fileHandle;
        this.changed = changed;
        this.newPath = newPath;
        this.opType = opType;
        this.extraParams = extraParams;
        this.status = status;
    }

    // 便捷构造方法
    public ChangeRecord(String oName, String nName, File f, boolean c, String nPath, OperationType type) {
        this(oName, nName, f, c, nPath, type, new HashMap<>(), ExecStatus.PENDING);
    }

    // 带参数的便捷构造方法
    public ChangeRecord(String oName, String nName, File f, boolean c, String nPath, OperationType type, Map<String, String> params) {
        this(oName, nName, f, c, nPath, type, params, ExecStatus.PENDING);
    }

    public ChangeRecord(String originalName, String newName, File fileHandle, boolean changed, String newPath, boolean isMove) {
        this.originalName = originalName;
        this.newName = newName;
        this.fileHandle = fileHandle;
        this.changed = changed;
        this.newPath = newPath;
        if (isMove) {
            this.opType = OperationType.MOVE;
        } else {
            this.opType = OperationType.RENAME;
        }
    }

    public String getOriginalPath() {
        return fileHandle != null ? fileHandle.getAbsolutePath() : "";
    }

    @Override
    public String toString() {
        return originalName;
    }
}
