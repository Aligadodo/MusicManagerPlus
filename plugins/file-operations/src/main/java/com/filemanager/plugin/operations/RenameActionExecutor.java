package com.filemanager.plugin.operations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenameActionExecutor {
    
    public enum ActionType {
        RENAME,
        COPY,
        MOVE,
        DELETE,
        CREATE_DIRECTORY,
        UPDATE_METADATA,
        SKIP
    }
    
    public enum ExecutionStatus {
        SUCCESS,
        FAILED,
        SKIPPED,
        CONFLICT,
        ERROR
    }
    
    public static class Action {
        private ActionType type;
        private String sourcePath;
        private String targetPath;
        private Map<String, Object> metadata;
        private boolean overwrite;
        private boolean backup;
        private String backupPath;
        
        public Action() {
            this.metadata = new HashMap<>();
            this.overwrite = false;
            this.backup = false;
        }
        
        public Action(ActionType type, String sourcePath, String targetPath) {
            this();
            this.type = type;
            this.sourcePath = sourcePath;
            this.targetPath = targetPath;
        }
        
        public ActionType getType() {
            return type;
        }
        
        public void setType(ActionType type) {
            this.type = type;
        }
        
        public String getSourcePath() {
            return sourcePath;
        }
        
        public void setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
        }
        
        public String getTargetPath() {
            return targetPath;
        }
        
        public void setTargetPath(String targetPath) {
            this.targetPath = targetPath;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
        
        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        public boolean isOverwrite() {
            return overwrite;
        }
        
        public void setOverwrite(boolean overwrite) {
            this.overwrite = overwrite;
        }
        
        public boolean isBackup() {
            return backup;
        }
        
        public void setBackup(boolean backup) {
            this.backup = backup;
        }
        
        public String getBackupPath() {
            return backupPath;
        }
        
        public void setBackupPath(String backupPath) {
            this.backupPath = backupPath;
        }
    }
    
    public static class ActionResult {
        private Action action;
        private ExecutionStatus status;
        private String message;
        private Exception error;
        private long executionTime;
        private boolean conflict;
        private String conflictPath;
        
        public ActionResult(Action action) {
            this.action = action;
            this.status = ExecutionStatus.SUCCESS;
            this.message = "Action executed successfully";
            this.executionTime = 0;
            this.conflict = false;
        }
        
        public Action getAction() {
            return action;
        }
        
        public ExecutionStatus getStatus() {
            return status;
        }
        
        public void setStatus(ExecutionStatus status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Exception getError() {
            return error;
        }
        
        public void setError(Exception error) {
            this.error = error;
        }
        
        public long getExecutionTime() {
            return executionTime;
        }
        
        public void setExecutionTime(long executionTime) {
            this.executionTime = executionTime;
        }
        
        public boolean isConflict() {
            return conflict;
        }
        
        public void setConflict(boolean conflict) {
            this.conflict = conflict;
        }
        
        public String getConflictPath() {
            return conflictPath;
        }
        
        public void setConflictPath(String conflictPath) {
            this.conflictPath = conflictPath;
        }
        
        public boolean isSuccess() {
            return status == ExecutionStatus.SUCCESS;
        }
        
        public boolean isFailed() {
            return status == ExecutionStatus.FAILED || status == ExecutionStatus.ERROR;
        }
        
        public boolean isSkipped() {
            return status == ExecutionStatus.SKIPPED;
        }
    }
    
    private boolean dryRun;
    private boolean createBackup;
    private String backupDirectory;
    private boolean stopOnError;
    private List<ActionResult> results;
    
    public RenameActionExecutor() {
        this.dryRun = false;
        this.createBackup = false;
        this.backupDirectory = ".backup";
        this.stopOnError = false;
        this.results = new ArrayList<>();
    }
    
    public RenameActionExecutor(boolean dryRun) {
        this();
        this.dryRun = dryRun;
    }
    
    public boolean isDryRun() {
        return dryRun;
    }
    
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
    
    public boolean isCreateBackup() {
        return createBackup;
    }
    
    public void setCreateBackup(boolean createBackup) {
        this.createBackup = createBackup;
    }
    
    public String getBackupDirectory() {
        return backupDirectory;
    }
    
    public void setBackupDirectory(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }
    
    public boolean isStopOnError() {
        return stopOnError;
    }
    
    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }
    
    public List<ActionResult> getResults() {
        return new ArrayList<>(results);
    }
    
    public void clearResults() {
        results.clear();
    }
    
    public ActionResult execute(Action action) {
        if (action == null) {
            ActionResult result = new ActionResult(action);
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("Action is null");
            return result;
        }
        
        long startTime = System.currentTimeMillis();
        ActionResult result = new ActionResult(action);
        
        try {
            switch (action.getType()) {
                case RENAME:
                    result = executeRename(action);
                    break;
                case COPY:
                    result = executeCopy(action);
                    break;
                case MOVE:
                    result = executeMove(action);
                    break;
                case DELETE:
                    result = executeDelete(action);
                    break;
                case CREATE_DIRECTORY:
                    result = executeCreateDirectory(action);
                    break;
                case UPDATE_METADATA:
                    result = executeUpdateMetadata(action);
                    break;
                case SKIP:
                    result.setStatus(ExecutionStatus.SKIPPED);
                    result.setMessage("Action skipped");
                    break;
                default:
                    result.setStatus(ExecutionStatus.FAILED);
                    result.setMessage("Unknown action type: " + action.getType());
            }
        } catch (Exception e) {
            result.setStatus(ExecutionStatus.ERROR);
            result.setMessage("Error executing action: " + e.getMessage());
            result.setError(e);
        }
        
        result.setExecutionTime(System.currentTimeMillis() - startTime);
        results.add(result);
        
        return result;
    }
    
    public List<ActionResult> executeBatch(List<Action> actions) {
        List<ActionResult> batchResults = new ArrayList<>();
        
        for (Action action : actions) {
            ActionResult result = execute(action);
            batchResults.add(result);
            
            if (stopOnError && result.isFailed()) {
                break;
            }
        }
        
        return batchResults;
    }
    
    private ActionResult executeRename(Action action) throws IOException {
        ActionResult result = new ActionResult(action);
        
        File sourceFile = new File(action.getSourcePath());
        File targetFile = new File(action.getTargetPath());
        
        if (!sourceFile.exists()) {
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("Source file does not exist: " + action.getSourcePath());
            return result;
        }
        
        if (targetFile.exists() && !action.isOverwrite()) {
            result.setStatus(ExecutionStatus.CONFLICT);
            result.setMessage("Target file already exists: " + action.getTargetPath());
            result.setConflict(true);
            result.setConflictPath(action.getTargetPath());
            return result;
        }
        
        if (dryRun) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Dry run: would rename " + action.getSourcePath() + " to " + action.getTargetPath());
            return result;
        }
        
        if (createBackup && sourceFile.exists()) {
            createBackup(sourceFile);
        }
        
        boolean success = sourceFile.renameTo(targetFile);
        
        if (success) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Successfully renamed " + action.getSourcePath() + " to " + action.getTargetPath());
        } else {
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("Failed to rename " + action.getSourcePath() + " to " + action.getTargetPath());
        }
        
        return result;
    }
    
    private ActionResult executeCopy(Action action) throws IOException {
        ActionResult result = new ActionResult(action);
        
        File sourceFile = new File(action.getSourcePath());
        File targetFile = new File(action.getTargetPath());
        
        if (!sourceFile.exists()) {
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("Source file does not exist: " + action.getSourcePath());
            return result;
        }
        
        if (targetFile.exists() && !action.isOverwrite()) {
            result.setStatus(ExecutionStatus.CONFLICT);
            result.setMessage("Target file already exists: " + action.getTargetPath());
            result.setConflict(true);
            result.setConflictPath(action.getTargetPath());
            return result;
        }
        
        if (dryRun) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Dry run: would copy " + action.getSourcePath() + " to " + action.getTargetPath());
            return result;
        }
        
        Path sourcePath = sourceFile.toPath();
        Path targetPath = targetFile.toPath();
        
        StandardCopyOption copyOption = action.isOverwrite() ? 
            StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.COPY_ATTRIBUTES;
        
        Files.copy(sourcePath, targetPath, copyOption);
        
        result.setStatus(ExecutionStatus.SUCCESS);
        result.setMessage("Successfully copied " + action.getSourcePath() + " to " + action.getTargetPath());
        
        return result;
    }
    
    private ActionResult executeMove(Action action) throws IOException {
        ActionResult result = new ActionResult(action);
        
        File sourceFile = new File(action.getSourcePath());
        File targetFile = new File(action.getTargetPath());
        
        if (!sourceFile.exists()) {
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("Source file does not exist: " + action.getSourcePath());
            return result;
        }
        
        if (targetFile.exists() && !action.isOverwrite()) {
            result.setStatus(ExecutionStatus.CONFLICT);
            result.setMessage("Target file already exists: " + action.getTargetPath());
            result.setConflict(true);
            result.setConflictPath(action.getTargetPath());
            return result;
        }
        
        if (dryRun) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Dry run: would move " + action.getSourcePath() + " to " + action.getTargetPath());
            return result;
        }
        
        if (createBackup && sourceFile.exists()) {
            createBackup(sourceFile);
        }
        
        Path sourcePath = sourceFile.toPath();
        Path targetPath = targetFile.toPath();
        
        StandardCopyOption copyOption = action.isOverwrite() ? 
            StandardCopyOption.REPLACE_EXISTING : StandardCopyOption.ATOMIC_MOVE;
        
        Files.move(sourcePath, targetPath, copyOption);
        
        result.setStatus(ExecutionStatus.SUCCESS);
        result.setMessage("Successfully moved " + action.getSourcePath() + " to " + action.getTargetPath());
        
        return result;
    }
    
    private ActionResult executeDelete(Action action) throws IOException {
        ActionResult result = new ActionResult(action);
        
        File file = new File(action.getSourcePath());
        
        if (!file.exists()) {
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("File does not exist: " + action.getSourcePath());
            return result;
        }
        
        if (dryRun) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Dry run: would delete " + action.getSourcePath());
            return result;
        }
        
        if (createBackup && file.exists()) {
            createBackup(file);
        }
        
        boolean success = file.delete();
        
        if (success) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Successfully deleted " + action.getSourcePath());
        } else {
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("Failed to delete " + action.getSourcePath());
        }
        
        return result;
    }
    
    private ActionResult executeCreateDirectory(Action action) throws IOException {
        ActionResult result = new ActionResult(action);
        
        File directory = new File(action.getTargetPath());
        
        if (directory.exists()) {
            result.setStatus(ExecutionStatus.CONFLICT);
            result.setMessage("Directory already exists: " + action.getTargetPath());
            result.setConflict(true);
            result.setConflictPath(action.getTargetPath());
            return result;
        }
        
        if (dryRun) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Dry run: would create directory " + action.getTargetPath());
            return result;
        }
        
        boolean success = directory.mkdirs();
        
        if (success) {
            result.setStatus(ExecutionStatus.SUCCESS);
            result.setMessage("Successfully created directory " + action.getTargetPath());
        } else {
            result.setStatus(ExecutionStatus.FAILED);
            result.setMessage("Failed to create directory " + action.getTargetPath());
        }
        
        return result;
    }
    
    private ActionResult executeUpdateMetadata(Action action) {
        ActionResult result = new ActionResult(action);
        
        result.setStatus(ExecutionStatus.SKIPPED);
        result.setMessage("Metadata update not implemented");
        
        return result;
    }
    
    private void createBackup(File file) throws IOException {
        if (backupDirectory == null || backupDirectory.isEmpty()) {
            return;
        }
        
        File backupDir = new File(backupDirectory);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        String backupFileName = file.getName() + ".backup_" + System.currentTimeMillis();
        File backupFile = new File(backupDir, backupFileName);
        
        Files.copy(file.toPath(), backupFile.toPath());
    }
    
    public static Action createRenameAction(String sourcePath, String targetPath) {
        return new Action(ActionType.RENAME, sourcePath, targetPath);
    }
    
    public static Action createCopyAction(String sourcePath, String targetPath) {
        return new Action(ActionType.COPY, sourcePath, targetPath);
    }
    
    public static Action createMoveAction(String sourcePath, String targetPath) {
        return new Action(ActionType.MOVE, sourcePath, targetPath);
    }
    
    public static Action createDeleteAction(String filePath) {
        Action action = new Action();
        action.setType(ActionType.DELETE);
        action.setSourcePath(filePath);
        return action;
    }
    
    public static Action createCreateDirectoryAction(String directoryPath) {
        Action action = new Action();
        action.setType(ActionType.CREATE_DIRECTORY);
        action.setTargetPath(directoryPath);
        return action;
    }
    
    public static Action createSkipAction() {
        Action action = new Action();
        action.setType(ActionType.SKIP);
        return action;
    }
}
