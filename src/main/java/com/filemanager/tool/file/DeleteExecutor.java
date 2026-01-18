package com.filemanager.tool.file;

import com.filemanager.model.ChangeRecord;
import com.filemanager.model.CleanupParams;
import com.filemanager.strategy.FileCleanupStrategy;
import com.filemanager.type.OperationType;
import com.filemanager.type.ExecStatus;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DeleteExecutor {
    private final CleanupParams params;
    private final long taskStartTimestamp;

    public DeleteExecutor(CleanupParams params, long taskStartTimestamp) {
        this.params = params;
        this.taskStartTimestamp = taskStartTimestamp;
    }

    public void execute(ChangeRecord rec) throws Exception {
        File file = rec.getFileHandle();
        if (!file.exists()) return; // 可能已经被处理了

        if (rec.getOpType() == OperationType.DELETE) {
            // 检查是否是合并操作
            Map<String, String> params = rec.getExtraParams();
            if (params != null && "merge_folder".equals(params.get("operation"))) {
                // 文件夹合并操作
                String childDirPath = params.get("childDir");
                String parentDirPath = params.get("parentDir");
                
                if (childDirPath != null && parentDirPath != null) {
                    File childDir = new File(childDirPath);
                    File parentDir = new File(parentDirPath);
                    
                    if (childDir.exists() && parentDir.exists() && parentDir.isDirectory()) {
                        // 将子文件夹中的所有文件移动到父文件夹
                        File[] subFiles = childDir.listFiles();
                        if (subFiles != null) {
                            for (File subFile : subFiles) {
                                if (subFile.isFile()) {
                                    File destFile = new File(parentDir, subFile.getName());
                                    
                                    // 检查是否存在冲突
                                    boolean conflict = destFile.exists();
                                    
                                    // 处理冲突：当前默认覆盖，后续可以扩展为支持用户选择
                                    StandardCopyOption[] copyOptions = conflict ? 
                                            new StandardCopyOption[]{StandardCopyOption.REPLACE_EXISTING} : 
                                            new StandardCopyOption[]{};
                                    
                                    // 尝试移动
                                    try {
                                        Files.move(subFile.toPath(), destFile.toPath(), copyOptions);
                                    } catch (IOException e) {
                                        // 跨盘移动 fallback
                                        Files.copy(subFile.toPath(), destFile.toPath(), copyOptions);
                                        Files.delete(subFile.toPath());
                                    }
                                } else if (subFile.isDirectory()) {
                                    // 如果是子目录，递归处理（创建对应目录并复制内容）
                                    File destDir = new File(parentDir, subFile.getName());
                                    if (!destDir.exists()) {
                                        destDir.mkdirs();
                                    }
                                    
                                    // 递归复制子目录内容
                                    copyDirectory(subFile, destDir);
                                    deleteDirectoryRecursively(subFile);
                                }
                            }
                        }
                        
                        // 删除空的子文件夹
                        deleteDirectoryRecursively(childDir);
                    }
                }
            } else if (params != null && "merge_move".equals(params.get("operation"))) {
                // 兼容旧的合并移动操作（可以考虑在后续版本中移除）
                String destPath = params.get("destPath");
                if (destPath != null && !destPath.isEmpty()) {
                    File destFile = new File(destPath);
                    if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
                    // 尝试移动
                    try {
                        Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        // 跨盘移动 fallback
                        if (file.isDirectory()) copyDirectory(file, destFile);
                        else Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        deleteDirectoryRecursively(file); // 移完后删源
                    }
                }
            } else {
                // 普通删除操作
                String method = params != null ? params.get("method") : null;

                if ("DIRECT".equals(method)) {
                    // 递归删除（如果是文件夹）
                    if (file.isDirectory()) deleteDirectoryRecursively(file);
                    else Files.delete(file.toPath());
                } else {
                    // 伪删除：移动到 Trash
                    String trashPath = rec.getNewPath();
                    if (trashPath != null && !trashPath.isEmpty()) {
                        File trashFile = new File(trashPath);
                        if (!trashFile.getParentFile().exists()) trashFile.getParentFile().mkdirs();
                        // 尝试移动
                        try {
                            Files.move(file.toPath(), trashFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            // 跨盘移动 fallback
                            if (file.isDirectory()) copyDirectory(file, trashFile);
                            else Files.copy(file.toPath(), trashFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                            deleteDirectoryRecursively(file); // 移完后删源
                        }
                    }
                }
            }
        }
    }

    public ChangeRecord createDeleteRecord(File f, String reason) {
        String newPath;
        String newName;
        Map<String, String> paramsMap = new HashMap<>();

        if (params.getMethod() == FileCleanupStrategy.DeleteMethod.DIRECT_DELETE) {
            newPath = "PERMANENT_DELETE";
            newName = "[删除] " + f.getName();
            paramsMap.put("method", "DIRECT");
        } else {
            // 伪删除或可回滚删除路径计算
            File trashRoot;
            Path sourcePath = f.toPath();
            Path root = sourcePath.getRoot();
            
            // 基础回收站路径
            if (Paths.get(params.getTrashPath()).isAbsolute()) {
                // 模式 B: 固定绝对路径
                File fixedTrash = new File(params.getTrashPath());
                String driveTag = root.toString().replace(":", "").replace(File.separator, "") + "_Drive";
                Path relativeToRoot = root.relativize(sourcePath).getParent();
                trashRoot = new File(fixedTrash, driveTag);
                if (relativeToRoot != null) trashRoot = new File(trashRoot, relativeToRoot.toString());
            } else {
                // 模式 A: 相对路径
                File driveRoot = root.toFile();
                trashRoot = new File(driveRoot, params.getTrashPath());
                Path relativeToRoot = root.relativize(sourcePath).getParent();
                if (relativeToRoot != null) trashRoot = new File(trashRoot, relativeToRoot.toString());
            }
            
            // 可回滚删除：添加时间戳子目录
            if (params.getMethod() == FileCleanupStrategy.DeleteMethod.ROLLBACKABLE_DELETE) {
                // 使用应用启动时间戳作为统一的时间戳 (格式: yyyyMMdd_HHmmss)
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date(taskStartTimestamp));
                trashRoot = new File(trashRoot, timestamp);
            }

            File trashFile = new File(trashRoot, f.getName());
            newPath = trashFile.getAbsolutePath();
            newName = "[回收] " + f.getName();
            paramsMap.put("method", params.getMethod() == FileCleanupStrategy.DeleteMethod.ROLLBACKABLE_DELETE ? "ROLLBACKABLE" : "PSEUDO");
        }
        paramsMap.put("reason", reason);

        // 在 ChangeRecord 中复用 RENAME 只是为了显示，OpType 必须是 DELETE
        return new ChangeRecord(f.getName(), newName, f, true, newPath, OperationType.DELETE, paramsMap, ExecStatus.PENDING);
    }

    private void deleteDirectoryRecursively(File file) throws IOException {
        Files.walkFileTree(file.toPath(), new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public java.nio.file.FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return java.nio.file.FileVisitResult.CONTINUE;
            }

            @Override
            public java.nio.file.FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return java.nio.file.FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyDirectory(File source, File target) throws IOException {
        Files.walk(source.toPath()).forEach(sourcePath -> {
            Path targetPath = target.toPath().resolve(source.toPath().relativize(sourcePath));
            try {
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    // Getters and setters if needed
    public CleanupParams getParams() {
        return params;
    }

    public long getTaskStartTimestamp() {
        return taskStartTimestamp;
    }
}