package com.filemanager.plugin.impl.filecleanup;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.impl.filecleanup.enums.CleanupMode;
import com.filemanager.plugin.impl.filecleanup.enums.DeleteMethod;
import com.filemanager.plugin.impl.filecleanup.enums.FileSizeRange;
import com.filemanager.plugin.util.MD5Calculator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileCleanupStrategy extends AbstractConfigurableStrategy {

    public FileCleanupStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-cleanup";
    }

    @Override
    public String getName() {
        return "文件清理与去重";
    }

    @Override
    public String getDescription() {
        return "智能识别重复文件/文件夹、清理空目录、合并同名父子文件夹。支持按盘符结构伪删除。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public ScanTarget getTargetType() {
        return ScanTarget.ALL;
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("mode", "清理模式", "select", (Object) CleanupMode.FILE_DUPLICATE.getCode(), 
            "清理的逻辑规则", true, 
            getCleanupModeOptions());
        addEnumConfigField("method", "删除方式", "select", (Object) DeleteMethod.PSEUDO_DELETE.getCode(), 
            "删除的方式", true, 
            getDeleteMethodOptions());
        addConfigField("trashPath", "回收站路径", "text", (Object) ".EchoTrash", 
            "回收站的位置", false);
        addConfigField("keepLargest", "保留体积/质量最佳的副本", "boolean", (Object) true, 
            "保留最大的文件", false);
        addConfigField("keepEarliest", "保留日期最早/最晚的副本", "boolean", (Object) true, 
            "保留日期最早的文件", false);
        addConfigField("keepExt", "优先后缀", "text", (Object) "wav", 
            "去重时优先保留的文件后缀", false);
        addConfigField("preprocessLower", "文件名转小写", "boolean", (Object) true, 
            "将文件名转换为小写后进行比较", false);
        addConfigField("preprocessUpper", "文件名转大写", "boolean", (Object) false, 
            "将文件名转换为大写后进行比较", false);
        addConfigField("preprocessSimplified", "文件名转简体中文", "boolean", (Object) false, 
            "将文件名中的繁体中文转换为简体中文后进行比较", false);
        addEnumConfigField("sizeRange", "文件大小范围", "select", (Object) FileSizeRange.ALL.getCode(), 
            "要处理的文件大小范围", false, 
            getFileSizeRangeOptions());
        addConfigField("audioSpecial", "音频文件特殊处理", "boolean", (Object) true, 
            "对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "mode", (Object) CleanupMode.FILE_DUPLICATE.getCode());
        setConfigValue(config, "method", (Object) DeleteMethod.PSEUDO_DELETE.getCode());
        setConfigValue(config, "trashPath", (Object) ".EchoTrash");
        setConfigValue(config, "keepLargest", (Object) true);
        setConfigValue(config, "keepEarliest", (Object) true);
        setConfigValue(config, "keepExt", (Object) "wav");
        setConfigValue(config, "preprocessLower", (Object) true);
        setConfigValue(config, "preprocessUpper", (Object) false);
        setConfigValue(config, "preprocessSimplified", (Object) false);
        setConfigValue(config, "sizeRange", (Object) FileSizeRange.ALL.getCode());
        setConfigValue(config, "audioSpecial", (Object) true);
    }

    @Override
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File file = currentRecord.getFileHandle();
        String mode = getConfigValue(config, "mode", "file_duplicate");
        String method = getConfigValue(config, "method", "pseudo_delete");
        
        context.logInfo("分析文件清理: " + file.getName() + ", 模式: " + mode);
        
        Map<String, String> params = new HashMap<>();
        params.put("mode", mode);
        params.put("method", method);
        
        OperationType opType = getOperationType(mode);
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            "",
            currentRecord.getFileHandle(),
            true,
            "",
            opType,
            params,
            ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File file = record.getFileHandle();
        String mode = getConfigValue(config, "mode", "file_duplicate");
        String method = getConfigValue(config, "method", "pseudo_delete");
        String trashPath = getConfigValue(config, "trashPath", ".EchoTrash");
        
        if (!file.exists()) {
            context.logWarn("文件/目录不存在: " + file.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        try {
            if ("pseudo_delete".equals(method)) {
                moveToTrash(file, trashPath, context);
            } else {
                if (file.isDirectory()) {
                    deleteDirectory(file, context);
                } else {
                    deleteFile(file, context);
                }
            }
            
            context.logInfo("清理完成: " + file.getPath());
            record.setStatus(ExecStatus.SUCCESS.name());
        } catch (Exception e) {
            context.logError("清理失败: " + file.getPath() + ", 错误: " + e.getMessage());
            record.setStatus(ExecStatus.FAILED.name());
        }
    }

    private OperationType getOperationType(String mode) {
        switch (mode) {
            case "file_duplicate":
                return OperationType.CLEANUP;
            case "folder_duplicate":
                return OperationType.CLEANUP;
            case "empty_directory":
                return OperationType.CLEANUP;
            case "direct_cleanup":
                return OperationType.DELETE;
            default:
                return OperationType.CLEANUP;
        }
    }

    private boolean checkFileSizeRange(File file, StrategyConfigDTO config, ExecutionContext context) {
        String sizeRange = getConfigValue(config, "sizeRange", "all");
        if ("all".equals(sizeRange)) {
            return true;
        }
        
        long fileSize = file.length();
        long sizeInMB = fileSize / (1024 * 1024);
        
        switch (sizeRange) {
            case "less_than_1mb":
                return sizeInMB < 1;
            case "less_than_10mb":
                return sizeInMB < 10;
            case "less_than_100mb":
                return sizeInMB < 100;
            case "less_than_1gb":
                return sizeInMB < 1024;
            case "greater_than_1mb":
                return sizeInMB > 1;
            case "greater_than_10mb":
                return sizeInMB > 10;
            case "greater_than_100mb":
                return sizeInMB > 100;
            case "greater_than_1gb":
                return sizeInMB > 1024;
            default:
                return true;
        }
    }

    private ChangeRecord handleFileDuplication(File file, StrategyConfigDTO config, ExecutionContext context) {
        String method = getConfigValue(config, "method", "pseudo_delete");
        String trashPath = getConfigValue(config, "trashPath", ".EchoTrash");
        boolean keepLargest = getConfigValue(config, "keepLargest", true);
        boolean keepEarliest = getConfigValue(config, "keepEarliest", true);
        String keepExt = getConfigValue(config, "keepExt", "wav");
        
        context.logInfo("Processing file duplication for: " + file.getName());
        
        try {
            List<File> duplicateFiles = findDuplicateFiles(file, config, context);
            
            if (duplicateFiles.isEmpty()) {
                context.logDebug("No duplicate files found for: " + file.getName());
                return createChangeRecord(file.getPath(), file.getPath(), "SKIPPED");
            }
            
            File fileToKeep = selectFileToKeep(file, duplicateFiles, keepLargest, keepEarliest, keepExt, config, context);
            
            if (!fileToKeep.equals(file)) {
                if ("pseudo_delete".equals(method)) {
                    moveToTrash(file, trashPath, context);
                } else {
                    deleteFile(file, context);
                }
                
                ChangeRecord record = createChangeRecord(file.getPath(), "", "SUCCESS");
                record.setOperationType("文件去重");
                record.setReason("删除方式: " + method + "，保留文件: " + fileToKeep.getName());
                return record;
            }
            
            context.logDebug("File is one to keep: " + file.getName());
            return createChangeRecord(file.getPath(), file.getPath(), "SKIPPED");
            
        } catch (Exception e) {
            context.logError("Error processing file duplication: " + e.getMessage());
            return createChangeRecord(file.getPath(), file.getPath(), "ERROR");
        }
    }

    private List<File> findDuplicateFiles(File file, StrategyConfigDTO config, ExecutionContext context) throws IOException {
        List<File> duplicates = new ArrayList<>();
        
        String currentMD5 = MD5Calculator.calculateMD5(file);
        
        File parentDir = file.getParentFile();
        if (parentDir == null) {
            return duplicates;
        }
        
        File[] filesInDir = parentDir.listFiles();
        if (filesInDir == null) {
            return duplicates;
        }
        
        for (File otherFile : filesInDir) {
            if (otherFile.equals(file) || !otherFile.isFile()) {
                continue;
            }
            
            if (otherFile.length() != file.length()) {
                continue;
            }
            
            if (!checkFileSizeRange(otherFile, config, context)) {
                continue;
            }
            
            try {
                String otherMD5 = MD5Calculator.calculateMD5(otherFile);
                if (currentMD5.equals(otherMD5)) {
                    duplicates.add(otherFile);
                }
            } catch (IOException e) {
                context.logDebug("Error calculating MD5 for file: " + otherFile.getName());
            }
        }
        
        return duplicates;
    }

    private File selectFileToKeep(File file, List<File> duplicates, 
            boolean keepLargest, boolean keepEarliest, String keepExt,
            StrategyConfigDTO config, ExecutionContext context) {
        
        List<File> allFiles = new ArrayList<>(duplicates);
        allFiles.add(file);
        
        File selectedFile = file;
        
        if (keepExt != null && !keepExt.isEmpty()) {
            for (File f : allFiles) {
                if (f.getName().toLowerCase().endsWith("." + keepExt.toLowerCase())) {
                    selectedFile = f;
                    break;
                }
            }
        }
        
        if (keepLargest) {
            for (File f : allFiles) {
                if (f.length() > selectedFile.length()) {
                    selectedFile = f;
                }
            }
        }
        
        if (keepEarliest) {
            long earliestTime = selectedFile.lastModified();
            for (File f : allFiles) {
                if (f.lastModified() < earliestTime) {
                    earliestTime = f.lastModified();
                    selectedFile = f;
                }
            }
        }
        
        return selectedFile;
    }

    private ChangeRecord handleFolderDuplication(File folder, StrategyConfigDTO config, ExecutionContext context) {
        String method = getConfigValue(config, "method", "pseudo_delete");
        String trashPath = getConfigValue(config, "trashPath", ".EchoTrash");
        boolean keepLargest = getConfigValue(config, "keepLargest", true);
        boolean keepEarliest = getConfigValue(config, "keepEarliest", true);
        
        if (!folder.isDirectory()) {
            return createChangeRecord(folder.getPath(), folder.getPath(), "SKIPPED");
        }
        
        context.logInfo("Processing folder duplication for: " + folder.getName());
        
        try {
            List<File> duplicateFolders = findDuplicateFolders(folder, config, context);
            
            if (duplicateFolders.isEmpty()) {
                context.logDebug("No duplicate folders found for: " + folder.getName());
                return createChangeRecord(folder.getPath(), folder.getPath(), "SKIPPED");
            }
            
            File folderToKeep = selectFolderToKeep(folder, duplicateFolders, keepLargest, keepEarliest, config, context);
            
            if (!folderToKeep.equals(folder)) {
                if ("pseudo_delete".equals(method)) {
                    moveToTrash(folder, trashPath, context);
                } else {
                    deleteDirectory(folder, context);
                }
                
                ChangeRecord record = createChangeRecord(folder.getPath(), "", "SUCCESS");
                record.setOperationType("文件夹去重");
                record.setReason("删除方式: " + method + "，保留文件夹: " + folderToKeep.getName());
                return record;
            }
            
            context.logDebug("Folder is one to keep: " + folder.getName());
            return createChangeRecord(folder.getPath(), folder.getPath(), "SKIPPED");
            
        } catch (Exception e) {
            context.logError("Error processing folder duplication: " + e.getMessage());
            return createChangeRecord(folder.getPath(), folder.getPath(), "ERROR");
        }
    }

    private List<File> findDuplicateFolders(File folder, StrategyConfigDTO config, ExecutionContext context) throws IOException {
        List<File> duplicates = new ArrayList<>();
        
        File parentDir = folder.getParentFile();
        if (parentDir == null) {
            return duplicates;
        }
        
        File[] foldersInDir = parentDir.listFiles(File::isDirectory);
        if (foldersInDir == null) {
            return duplicates;
        }
        
        for (File otherFolder : foldersInDir) {
            if (otherFolder.equals(folder)) {
                continue;
            }
            
            if (!folder.getName().equalsIgnoreCase(otherFolder.getName())) {
                continue;
            }
            
            if (areFoldersEqual(folder, otherFolder, context)) {
                duplicates.add(otherFolder);
            }
        }
        
        return duplicates;
    }

    private boolean areFoldersEqual(File folder1, File folder2, ExecutionContext context) {
        File[] files1 = folder1.listFiles();
        File[] files2 = folder2.listFiles();
        
        if (files1 == null || files2 == null) {
            return false;
        }
        
        if (files1.length != files2.length) {
            return false;
        }
        
        Map<String, File> fileMap1 = new HashMap<>();
        for (File file : files1) {
            fileMap1.put(file.getName(), file);
        }
        
        for (File file : files2) {
            File matchingFile = fileMap1.get(file.getName());
            if (matchingFile == null) {
                return false;
            }
            
            if (file.isFile() && matchingFile.isFile()) {
                if (file.length() != matchingFile.length()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private File selectFolderToKeep(File folder, List<File> duplicates, 
            boolean keepLargest, boolean keepEarliest,
            StrategyConfigDTO config, ExecutionContext context) {
        
        List<File> allFolders = new ArrayList<>(duplicates);
        allFolders.add(folder);
        
        File selectedFolder = folder;
        
        if (keepLargest) {
            long maxSize = calculateFolderSize(selectedFolder);
            for (File f : allFolders) {
                long size = calculateFolderSize(f);
                if (size > maxSize) {
                    maxSize = size;
                    selectedFolder = f;
                }
            }
        }
        
        if (keepEarliest) {
            long earliestTime = selectedFolder.lastModified();
            for (File f : allFolders) {
                if (f.lastModified() < earliestTime) {
                    earliestTime = f.lastModified();
                    selectedFolder = f;
                }
            }
        }
        
        return selectedFolder;
    }

    private long calculateFolderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else if (file.isDirectory()) {
                    size += calculateFolderSize(file);
                }
            }
        }
        return size;
    }

    private ChangeRecord handleEmptyDirectory(File directory, StrategyConfigDTO config, ExecutionContext context) {
        String method = getConfigValue(config, "method", "pseudo_delete");
        String trashPath = getConfigValue(config, "trashPath", ".EchoTrash");
        
        if (!directory.isDirectory()) {
            return createChangeRecord(directory.getPath(), directory.getPath(), "SKIPPED");
        }
        
        if (!isEmptyOrOnlyEmptySubdirectories(directory)) {
            context.logDebug("Directory is not empty or contains non-empty subdirectories: " + directory.getPath());
            return createChangeRecord(directory.getPath(), directory.getPath(), "SKIPPED");
        }
        
        context.logInfo("Cleaning empty directory: " + directory.getPath());
        
        try {
            if ("pseudo_delete".equals(method)) {
                moveToTrash(directory, trashPath, context);
            } else if ("direct_delete".equals(method)) {
                deleteDirectory(directory, context);
            }
            
            ChangeRecord record = createChangeRecord(directory.getPath(), "", "SUCCESS");
            record.setOperationType("清理空目录");
            record.setReason("删除方式: " + method);
            return record;
        } catch (Exception e) {
            context.logError("Error cleaning empty directory: " + e.getMessage());
            return createChangeRecord(directory.getPath(), directory.getPath(), "ERROR");
        }
    }

    private ChangeRecord handleDirectCleanup(File file, String method, String trashPath, ExecutionContext context) {
        try {
            if ("pseudo_delete".equals(method)) {
                moveToTrash(file, trashPath, context);
            } else if ("direct_delete".equals(method)) {
                if (file.isDirectory()) {
                    deleteDirectory(file, context);
                } else {
                    deleteFile(file, context);
                }
            }
            
            ChangeRecord record = createChangeRecord(file.getPath(), "", "SUCCESS");
            record.setOperationType("直接清理");
            record.setReason("删除方式: " + method);
            return record;
        } catch (Exception e) {
            context.logError("Error in direct cleanup: " + e.getMessage());
            return createChangeRecord(file.getPath(), file.getPath(), "ERROR");
        }
    }

    private boolean isEmptyOrOnlyEmptySubdirectories(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        
        String[] files = directory.list();
        if (files == null || files.length == 0) {
            return true;
        }
        
        for (String fileName : files) {
            File file = new File(directory, fileName);
            if (file.isFile()) {
                return false;
            } else if (file.isDirectory() && !isEmptyOrOnlyEmptySubdirectories(file)) {
                return false;
            }
        }
        
        return true;
    }

    private void moveToTrash(File file, String trashPath, ExecutionContext context) throws IOException {
        File trashDir = new File(trashPath);
        if (!trashDir.exists()) {
            trashDir.mkdirs();
        }
        
        File targetFile = new File(trashDir, file.getName());
        if (targetFile.exists()) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            targetFile = new File(trashDir, file.getName() + "_" + timestamp);
        }
        
        if (file.isDirectory()) {
            moveDirectory(file, targetFile, context);
        } else {
            Files.move(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        context.logInfo("Moved to trash: " + file.getPath() + " -> " + targetFile.getPath());
    }

    private void moveDirectory(File source, File target, ExecutionContext context) throws IOException {
        if (!target.exists()) {
            target.mkdirs();
        }
        
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    moveDirectory(file, targetFile, context);
                } else {
                    Files.move(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        
        Files.delete(source.toPath());
        context.logInfo("Moved directory: " + source.getPath() + " -> " + target.getPath());
    }

    private void deleteFile(File file, ExecutionContext context) throws IOException {
        Files.delete(file.toPath());
        context.logInfo("Deleted file: " + file.getPath());
    }

    private void deleteDirectory(File directory, ExecutionContext context) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file, context);
                } else {
                    deleteFile(file, context);
                }
            }
        }
        Files.delete(directory.toPath());
        context.logInfo("Deleted directory: " + directory.getPath());
    }
    
    private java.util.List<EnumOptionDTO> getCleanupModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (CleanupMode mode : CleanupMode.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(mode.getCode());
            option.setLabel(mode.getNameZh());
            option.setNameEn(mode.getNameEn());
            option.setDescriptionZh(mode.getDescriptionZh());
            option.setDescriptionEn(mode.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
    
    private java.util.List<EnumOptionDTO> getDeleteMethodOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (DeleteMethod method : DeleteMethod.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(method.getCode());
            option.setLabel(method.getNameZh());
            option.setNameEn(method.getNameEn());
            option.setDescriptionZh(method.getDescriptionZh());
            option.setDescriptionEn(method.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
    
    private java.util.List<EnumOptionDTO> getFileSizeRangeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (FileSizeRange range : FileSizeRange.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(range.getCode());
            option.setLabel(range.getNameZh());
            option.setNameEn(range.getNameEn());
            option.setDescriptionZh(range.getDescriptionZh());
            option.setDescriptionEn(range.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}