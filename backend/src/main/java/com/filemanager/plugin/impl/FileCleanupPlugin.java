package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractPlugin;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.util.MD5Calculator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件清理与去重策略插件
 * 支持文件去重、文件夹去重、空目录清理
 */
public class FileCleanupPlugin extends AbstractPlugin {

    public FileCleanupPlugin() {
        super("file-cleanup", "文件清理与去重", "智能识别重复文件/文件夹、清理空目录、合并同名父子文件夹。支持按盘符结构伪删除。", "1.0.0");
    }

    @Override
    protected void initParameters() {
        addParameter("mode", "清理模式", "select", "文件去重", "清理的逻辑规则", true, 
            Arrays.asList("文件去重", "文件夹去重", "清理空目录", "直接清理"));
        addParameter("method", "删除方式", "select", "伪删除", "删除的方式", true,
            Arrays.asList("伪删除", "直接删除", "可回滚删除"));
        addParameter("trashPath", "回收站路径", "string", ".EchoTrash", "回收站的位置", false);
        addParameter("keepLargest", "保留体积/质量最佳的副本", "boolean", true, "保留最大的文件", false);
        addParameter("keepEarliest", "保留日期最早/最晚的副本", "boolean", true, "保留日期最早的文件", false);
        addParameter("keepExt", "优先后缀", "string", "wav", "去重时优先保留的文件后缀", false);
        addParameter("preprocessLower", "文件名转小写", "boolean", true, "将文件名转换为小写后进行比较", false);
        addParameter("preprocessUpper", "文件名转大写", "boolean", false, "将文件名转换为大写后进行比较", false);
        addParameter("preprocessSimplified", "文件名转简体中文", "boolean", false, "将文件名中的繁体中文转换为简体中文后进行比较", false);
        addParameter("sizeRange", "文件大小范围", "select", "全部", "要处理的文件大小范围", false,
            Arrays.asList("全部", "小于1MB", "小于10MB", "小于100MB", "小于1GB", 
                      "大于1MB", "大于10MB", "大于100MB", "大于1GB"));
        addParameter("audioSpecial", "音频文件特殊处理", "boolean", true, "对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件", false);
    }

    @Override
    protected void initDefaultConfig() {
        setDefaultConfigValue("mode", "文件去重");
        setDefaultConfigValue("method", "伪删除");
        setDefaultConfigValue("trashPath", ".EchoTrash");
        setDefaultConfigValue("keepLargest", true);
        setDefaultConfigValue("keepEarliest", true);
        setDefaultConfigValue("keepExt", "wav");
        setDefaultConfigValue("preprocessLower", true);
        setDefaultConfigValue("preprocessUpper", false);
        setDefaultConfigValue("preprocessSimplified", false);
        setDefaultConfigValue("sizeRange", "全部");
        setDefaultConfigValue("audioSpecial", true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String mode = getConfigValue(config, "mode", "文件去重");
        String method = getConfigValue(config, "method", "伪删除");
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType(mode);
        record.setReason("删除方式: " + method);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, PluginConfigDTO config, ExecutionContext context) {
        String mode = getConfigValue(config, "mode", "文件去重");
        String method = getConfigValue(config, "method", "伪删除");
        String trashPath = getConfigValue(config, "trashPath", ".EchoTrash");
        
        File file = new File(filePath);
        if (!file.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        // 检查文件大小范围
        if (!checkFileSizeRange(file, config, context)) {
            context.logDebug("File size not in range: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            switch (mode) {
                case "文件去重":
                    return handleFileDuplication(file, config, context);
                case "文件夹去重":
                    return handleFolderDuplication(file, config, context);
                case "清理空目录":
                    return handleEmptyDirectory(file, config, context);
                case "直接清理":
                    return handleDirectCleanup(file, method, trashPath, context);
                default:
                    context.logWarn("Unknown cleanup mode: " + mode);
                    return createChangeRecord(filePath, filePath, "SKIPPED");
            }
        } catch (Exception e) {
            context.logError("Error processing file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    /**
     * 检查文件大小范围
     */
    private boolean checkFileSizeRange(File file, PluginConfigDTO config, ExecutionContext context) {
        String sizeRange = getConfigValue(config, "sizeRange", "全部");
        if ("全部".equals(sizeRange)) {
            return true;
        }
        
        long fileSize = file.length();
        long sizeInMB = fileSize / (1024 * 1024);
        
        switch (sizeRange) {
            case "小于1MB":
                return sizeInMB < 1;
            case "小于10MB":
                return sizeInMB < 10;
            case "小于100MB":
                return sizeInMB < 100;
            case "小于1GB":
                return sizeInMB < 1024;
            case "大于1MB":
                return sizeInMB > 1;
            case "大于10MB":
                return sizeInMB > 10;
            case "大于100MB":
                return sizeInMB > 100;
            case "大于1GB":
                return sizeInMB > 1024;
            default:
                return true;
        }
    }

    /**
     * 处理文件去重
     */
    private ChangeRecord handleFileDuplication(File file, PluginConfigDTO config, ExecutionContext context) {
        String method = getConfigValue(config, "method", "伪删除");
        String trashPath = getConfigValue(config, "trashPath", ".EchoTrash");
        boolean keepLargest = getConfigValue(config, "keepLargest", true);
        boolean keepEarliest = getConfigValue(config, "keepEarliest", true);
        String keepExt = getConfigValue(config, "keepExt", "wav");
        
        context.logInfo("Processing file duplication for: " + file.getName());
        
        try {
            // 查找重复文件
            List<File> duplicateFiles = findDuplicateFiles(file, config, context);
            
            if (duplicateFiles.isEmpty()) {
                context.logDebug("No duplicate files found for: " + file.getName());
                return createChangeRecord(file.getPath(), file.getPath(), "SKIPPED");
            }
            
            // 选择要保留的文件
            File fileToKeep = selectFileToKeep(file, duplicateFiles, keepLargest, keepEarliest, keepExt, config, context);
            
            // 如果当前文件不是要保留的文件，则删除它
            if (!fileToKeep.equals(file)) {
                if ("伪删除".equals(method)) {
                    moveToTrash(file, trashPath, context);
                } else {
                    deleteFile(file, context);
                }
                
                ChangeRecord record = createChangeRecord(file.getPath(), "", "SUCCESS");
                record.setOperationType("文件去重");
                record.setReason("删除方式: " + method + "，保留文件: " + fileToKeep.getName());
                return record;
            }
            
            // 当前文件是要保留的文件，跳过处理
            context.logDebug("File is the one to keep: " + file.getName());
            return createChangeRecord(file.getPath(), file.getPath(), "SKIPPED");
            
        } catch (Exception e) {
            context.logError("Error processing file duplication: " + e.getMessage());
            return createChangeRecord(file.getPath(), file.getPath(), "ERROR");
        }
    }

    /**
     * 查找重复文件
     */
    private List<File> findDuplicateFiles(File file, PluginConfigDTO config, ExecutionContext context) throws IOException {
        List<File> duplicates = new ArrayList<>();
        
        // 计算当前文件的MD5
        String currentMD5 = MD5Calculator.calculateMD5(file);
        
        // 在同一目录下查找MD5相同的文件
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
            
            // 检查文件大小是否相同
            if (otherFile.length() != file.length()) {
                continue;
            }
            
            // 检查文件大小范围
            if (!checkFileSizeRange(otherFile, config, context)) {
                continue;
            }
            
            // 计算其他文件的MD5
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

    /**
     * 选择要保留的文件
     */
    private File selectFileToKeep(File file, List<File> duplicates, 
            boolean keepLargest, boolean keepEarliest, String keepExt,
            PluginConfigDTO config, ExecutionContext context) {
        
        List<File> allFiles = new ArrayList<>(duplicates);
        allFiles.add(file);
        
        File selectedFile = file;
        
        // 优先保留指定后缀的文件
        if (keepExt != null && !keepExt.isEmpty()) {
            for (File f : allFiles) {
                if (f.getName().toLowerCase().endsWith("." + keepExt.toLowerCase())) {
                    selectedFile = f;
                    break;
                }
            }
        }
        
        // 保留最大的文件
        if (keepLargest) {
            for (File f : allFiles) {
                if (f.length() > selectedFile.length()) {
                    selectedFile = f;
                }
            }
        }
        
        // 保留日期最早的文件
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

    /**
     * 处理文件夹去重
     */
    private ChangeRecord handleFolderDuplication(File folder, PluginConfigDTO config, ExecutionContext context) {
        String method = getConfigValue(config, "method", "伪删除");
        String trashPath = getConfigValue(config, "trashPath", ".EchoTrash");
        boolean keepLargest = getConfigValue(config, "keepLargest", true);
        boolean keepEarliest = getConfigValue(config, "keepEarliest", true);
        
        if (!folder.isDirectory()) {
            return createChangeRecord(folder.getPath(), folder.getPath(), "SKIPPED");
        }
        
        context.logInfo("Processing folder duplication for: " + folder.getName());
        
        try {
            // 查找重复文件夹
            List<File> duplicateFolders = findDuplicateFolders(folder, config, context);
            
            if (duplicateFolders.isEmpty()) {
                context.logDebug("No duplicate folders found for: " + folder.getName());
                return createChangeRecord(folder.getPath(), folder.getPath(), "SKIPPED");
            }
            
            // 选择要保留的文件夹
            File folderToKeep = selectFolderToKeep(folder, duplicateFolders, keepLargest, keepEarliest, config, context);
            
            // 如果当前文件夹不是要保留的文件夹，则删除它
            if (!folderToKeep.equals(folder)) {
                if ("伪删除".equals(method)) {
                    moveToTrash(folder, trashPath, context);
                } else {
                    deleteDirectory(folder, context);
                }
                
                ChangeRecord record = createChangeRecord(folder.getPath(), "", "SUCCESS");
                record.setOperationType("文件夹去重");
                record.setReason("删除方式: " + method + "，保留文件夹: " + folderToKeep.getName());
                return record;
            }
            
            // 当前文件夹是要保留的文件夹，跳过处理
            context.logDebug("Folder is one to keep: " + folder.getName());
            return createChangeRecord(folder.getPath(), folder.getPath(), "SKIPPED");
            
        } catch (Exception e) {
            context.logError("Error processing folder duplication: " + e.getMessage());
            return createChangeRecord(folder.getPath(), folder.getPath(), "ERROR");
        }
    }

    /**
     * 查找重复文件夹
     */
    private List<File> findDuplicateFolders(File folder, PluginConfigDTO config, ExecutionContext context) throws IOException {
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
            
            // 比较文件夹名称
            if (!folder.getName().equalsIgnoreCase(otherFolder.getName())) {
                continue;
            }
            
            // 比较文件夹内容
            if (areFoldersEqual(folder, otherFolder, context)) {
                duplicates.add(otherFolder);
            }
        }
        
        return duplicates;
    }

    /**
     * 比较两个文件夹是否相同
     */
    private boolean areFoldersEqual(File folder1, File folder2, ExecutionContext context) {
        File[] files1 = folder1.listFiles();
        File[] files2 = folder2.listFiles();
        
        if (files1 == null || files2 == null) {
            return false;
        }
        
        if (files1.length != files2.length) {
            return false;
        }
        
        // 简化处理：比较文件数量和名称
        // 实际实现应该比较文件内容和结构
        Map<String, File> fileMap1 = new HashMap<>();
        for (File file : files1) {
            fileMap1.put(file.getName(), file);
        }
        
        for (File file : files2) {
            File matchingFile = fileMap1.get(file.getName());
            if (matchingFile == null) {
                return false;
            }
            
            // 如果是文件，比较大小
            if (file.isFile() && matchingFile.isFile()) {
                if (file.length() != matchingFile.length()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * 选择要保留的文件夹
     */
    private File selectFolderToKeep(File folder, List<File> duplicates, 
            boolean keepLargest, boolean keepEarliest,
            PluginConfigDTO config, ExecutionContext context) {
        
        List<File> allFolders = new ArrayList<>(duplicates);
        allFolders.add(folder);
        
        File selectedFolder = folder;
        
        // 保留最大的文件夹
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
        
        // 保留日期最早的文件夹
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

    /**
     * 计算文件夹大小
     */
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

    /**
     * 处理空目录清理
     */
    private ChangeRecord handleEmptyDirectory(File directory, PluginConfigDTO config, ExecutionContext context) {
        String method = getConfigValue(config, "method", "伪删除");
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
            if ("伪删除".equals(method)) {
                moveToTrash(directory, trashPath, context);
            } else if ("直接删除".equals(method)) {
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

    /**
     * 处理直接清理
     */
    private ChangeRecord handleDirectCleanup(File file, String method, String trashPath, ExecutionContext context) {
        try {
            if ("伪删除".equals(method)) {
                moveToTrash(file, trashPath, context);
            } else if ("直接删除".equals(method)) {
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

    /**
     * 检查目录是否为空
     */
    private boolean isEmptyDirectory(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        
        String[] files = directory.list();
        return files == null || files.length == 0;
    }

    /**
     * 检查目录是否为空或只包含空子目录
     */
    private boolean isEmptyOrOnlyEmptySubdirectories(File directory) {
        if (!directory.isDirectory()) {
            return false;
        }
        
        String[] files = directory.list();
        if (files == null || files.length == 0) {
            return true;
        }
        
        // 检查是否只包含空子目录
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

    /**
     * 移动到回收站
     */
    private void moveToTrash(File file, String trashPath, ExecutionContext context) throws IOException {
        File trashDir = new File(trashPath);
        if (!trashDir.exists()) {
            trashDir.mkdirs();
        }
        
        File targetFile = new File(trashDir, file.getName());
        if (targetFile.exists()) {
            // 如果目标文件已存在，添加时间戳
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

    /**
     * 移动目录
     */
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
        
        source.delete();
    }

    /**
     * 删除文件
     */
    private void deleteFile(File file, ExecutionContext context) throws IOException {
        Files.delete(file.toPath());
        context.logInfo("Deleted file: " + file.getPath());
    }

    /**
     * 删除目录
     */
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

    /**
     * 计算文件的MD5哈希值
     */
    private String calculateMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        byte[] digest = md.digest(fileBytes);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }

    /**
     * 预处理文件名
     */
    private String preprocessFilename(String filename, PluginConfigDTO config) {
        boolean toLower = getConfigValue(config, "preprocessLower", false);
        boolean toUpper = getConfigValue(config, "preprocessUpper", false);
        boolean toSimplified = getConfigValue(config, "preprocessSimplified", false);
        
        if (toLower) {
            filename = filename.toLowerCase();
        } else if (toUpper) {
            filename = filename.toUpperCase();
        }
        
        if (toSimplified) {
            // 这里需要实现繁体中文转简体中文的逻辑
            // 可以使用第三方库如opencc4j
        }
        
        return filename;
    }
}
