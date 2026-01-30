package com.filemanager.plugin.cleanup;

import com.filemanager.domain.dto.PluginConfigDTO;
import com.filemanager.domain.dto.PluginParameterDTO;
import com.filemanager.domain.dto.PreconditionDTO;
import com.filemanager.domain.dto.PreconditionGroupDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.IPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileCleanupPlugin implements IPlugin {
    private static final String MODE_DEDUP_FILES = "dedup_files";
    private static final String MODE_DEDUP_FOLDERS = "dedup_folders";
    private static final String MODE_REMOVE_EMPTY_DIRS = "remove_empty_dirs";
    private static final String MODE_DIRECT_CLEANUP = "direct_cleanup";
    private static final String MODE_MERGE_SAME_NAME = "merge_same_name";
    private static final String MODE_MERGE_NESTED = "merge_nested";
    
    private static final String METHOD_DIRECT_DELETE = "direct_delete";
    private static final String METHOD_PSEUDO_DELETE = "pseudo_delete";
    
    @Override
    public String getId() {
        return "file-cleanup";
    }

    @Override
    public String getName() {
        return "文件清理插件";
    }

    @Override
    public String getDescription() {
        return "支持文件去重、文件夹去重、空目录清理、文件夹合并等多种清理模式";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public PluginConfigDTO getDefaultConfig() {
        PluginConfigDTO config = new PluginConfigDTO();
        config.setValue("cleanupMode", MODE_DEDUP_FILES);
        config.setValue("deleteMethod", METHOD_PSEUDO_DELETE);
        config.setValue("trashPath", ".EchoTrash");
        config.setValue("keepLargest", true);
        config.setValue("keepEarliest", true);
        config.setValue("keepExt", "wav");
        config.setValue("preprocessLower", true);
        config.setValue("preprocessUpper", false);
        config.setValue("preprocessSimplified", false);
        config.setValue("audioSpecial", true);
        config.setValue("minFileSizeKB", 0);
        config.setValue("maxFileSizeKB", 10240);
        config.setParameters(getParameters());
        config.setPreconditionGroups(getDefaultPreconditionGroups());
        return config;
    }

    @Override
    public List<PluginParameterDTO> getParameters() {
        List<PluginParameterDTO> parameters = new ArrayList<>();
        
        PluginParameterDTO modeParam = new PluginParameterDTO();
        modeParam.setName("cleanupMode");
        modeParam.setLabel("清理模式");
        modeParam.setDescription("选择清理模式");
        modeParam.setType("select");
        modeParam.setDefaultValue(MODE_DEDUP_FILES);
        modeParam.setRequired(true);
        modeParam.setOptions(new String[]{
            MODE_DEDUP_FILES,
            MODE_DEDUP_FOLDERS,
            MODE_REMOVE_EMPTY_DIRS,
            MODE_DIRECT_CLEANUP,
            MODE_MERGE_SAME_NAME,
            MODE_MERGE_NESTED
        });
        parameters.add(modeParam);
        
        PluginParameterDTO methodParam = new PluginParameterDTO();
        methodParam.setName("deleteMethod");
        methodParam.setLabel("删除方法");
        methodParam.setDescription("选择删除方法");
        methodParam.setType("select");
        methodParam.setDefaultValue(METHOD_PSEUDO_DELETE);
        methodParam.setRequired(true);
        methodParam.setOptions(new String[]{METHOD_DIRECT_DELETE, METHOD_PSEUDO_DELETE});
        parameters.add(methodParam);
        
        PluginParameterDTO trashPathParam = new PluginParameterDTO();
        trashPathParam.setName("trashPath");
        trashPathParam.setLabel("回收站路径");
        trashPathParam.setDescription("伪删除时的回收站路径");
        trashPathParam.setType("directory");
        trashPathParam.setDefaultValue(".EchoTrash");
        trashPathParam.setRequired(false);
        parameters.add(trashPathParam);
        
        PluginParameterDTO keepLargestParam = new PluginParameterDTO();
        keepLargestParam.setName("keepLargest");
        keepLargestParam.setLabel("保留最大文件");
        keepLargestParam.setDescription("去重时保留最大的文件");
        keepLargestParam.setType("boolean");
        keepLargestParam.setDefaultValue(true);
        keepLargestParam.setRequired(false);
        parameters.add(keepLargestParam);
        
        PluginParameterDTO keepEarliestParam = new PluginParameterDTO();
        keepEarliestParam.setName("keepEarliest");
        keepEarliestParam.setLabel("保留最早文件");
        keepEarliestParam.setDescription("去重时保留最早的文件");
        keepEarliestParam.setType("boolean");
        keepEarliestParam.setDefaultValue(true);
        keepEarliestParam.setRequired(false);
        parameters.add(keepEarliestParam);
        
        PluginParameterDTO keepExtParam = new PluginParameterDTO();
        keepExtParam.setName("keepExt");
        keepExtParam.setLabel("保留扩展名");
        keepExtParam.setDescription("去重时保留的扩展名");
        keepExtParam.setType("text");
        keepExtParam.setDefaultValue("wav");
        keepExtParam.setRequired(false);
        parameters.add(keepExtParam);
        
        PluginParameterDTO preprocessLowerParam = new PluginParameterDTO();
        preprocessLowerParam.setName("preprocessLower");
        preprocessLowerParam.setLabel("预处理转小写");
        preprocessLowerParam.setDescription("去重前将文件名转为小写");
        preprocessLowerParam.setType("boolean");
        preprocessLowerParam.setDefaultValue(true);
        preprocessLowerParam.setRequired(false);
        parameters.add(preprocessLowerParam);
        
        PluginParameterDTO preprocessUpperParam = new PluginParameterDTO();
        preprocessUpperParam.setName("preprocessUpper");
        preprocessUpperParam.setLabel("预处理转大写");
        preprocessUpperParam.setDescription("去重前将文件名转为大写");
        preprocessUpperParam.setType("boolean");
        preprocessUpperParam.setDefaultValue(false);
        preprocessUpperParam.setRequired(false);
        parameters.add(preprocessUpperParam);
        
        PluginParameterDTO preprocessSimplifiedParam = new PluginParameterDTO();
        preprocessSimplifiedParam.setName("preprocessSimplified");
        preprocessSimplifiedParam.setLabel("预处理转简体");
        preprocessSimplifiedParam.setDescription("去重前将文件名转为简体中文");
        preprocessSimplifiedParam.setType("boolean");
        preprocessSimplifiedParam.setDefaultValue(false);
        preprocessSimplifiedParam.setRequired(false);
        parameters.add(preprocessSimplifiedParam);
        
        PluginParameterDTO audioSpecialParam = new PluginParameterDTO();
        audioSpecialParam.setName("audioSpecial");
        audioSpecialParam.setLabel("音频特殊处理");
        audioSpecialParam.setDescription("对音频文件进行特殊处理");
        audioSpecialParam.setType("boolean");
        audioSpecialParam.setDefaultValue(true);
        audioSpecialParam.setRequired(false);
        parameters.add(audioSpecialParam);
        
        PluginParameterDTO minFileSizeParam = new PluginParameterDTO();
        minFileSizeParam.setName("minFileSizeKB");
        minFileSizeParam.setLabel("最小文件大小（KB）");
        minFileSizeParam.setDescription("小于此大小的文件将被清理");
        minFileSizeParam.setType("number");
        minFileSizeParam.setDefaultValue(0);
        minFileSizeParam.setRequired(false);
        parameters.add(minFileSizeParam);
        
        PluginParameterDTO maxFileSizeParam = new PluginParameterDTO();
        maxFileSizeParam.setName("maxFileSizeKB");
        maxFileSizeParam.setLabel("最大文件大小（KB）");
        maxFileSizeParam.setDescription("大于此大小的文件将被清理");
        maxFileSizeParam.setType("number");
        maxFileSizeParam.setDefaultValue(10240);
        maxFileSizeParam.setRequired(false);
        parameters.add(maxFileSizeParam);
        
        return parameters;
    }

    @Override
    public List<PreconditionGroupDTO> getDefaultPreconditionGroups() {
        List<PreconditionGroupDTO> groups = new ArrayList<>();
        
        PreconditionGroupDTO group = new PreconditionGroupDTO();
        group.setId("default");
        group.setName("默认条件组");
        group.setDescription("文件清理的默认前置条件");
        group.setLogicType(PreconditionGroupDTO.LogicType.AND);
        
        List<PreconditionDTO> preconditions = new ArrayList<>();
        
        PreconditionDTO sizeCondition = new PreconditionDTO();
        sizeCondition.setId("size-condition");
        sizeCondition.setField("fileSize");
        sizeCondition.setOperator(PreconditionDTO.OperatorType.GREATER_THAN);
        sizeCondition.setValue(0);
        sizeCondition.setDescription("文件大小大于0");
        preconditions.add(sizeCondition);
        
        group.setPreconditions(preconditions);
        groups.add(group);
        
        return groups;
    }

    @Override
    public List<ChangeRecord> execute(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        String mode = (String) config.getValue("cleanupMode", MODE_DEDUP_FILES);
        String method = (String) config.getValue("deleteMethod", METHOD_PSEUDO_DELETE);
        String trashPath = (String) config.getValue("trashPath", ".EchoTrash");
        boolean keepLargest = (Boolean) config.getValue("keepLargest", true);
        boolean keepEarliest = (Boolean) config.getValue("keepEarliest", true);
        String keepExt = (String) config.getValue("keepExt", "wav");
        boolean preprocessLower = (Boolean) config.getValue("preprocessLower", true);
        boolean preprocessUpper = (Boolean) config.getValue("preprocessUpper", false);
        boolean preprocessSimplified = (Boolean) config.getValue("preprocessSimplified", false);
        boolean audioSpecial = (Boolean) config.getValue("audioSpecial", true);
        int minFileSizeKB = (Integer) config.getValue("minFileSizeKB", 0);
        int maxFileSizeKB = (Integer) config.getValue("maxFileSizeKB", 10240);
        
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (!file.exists()) {
                continue;
            }
            
            List<ChangeRecord> fileChanges = analyzeFile(file, mode, keepLargest, keepEarliest, keepExt, 
                    preprocessLower, preprocessUpper, preprocessSimplified, audioSpecial, 
                    minFileSizeKB, maxFileSizeKB);
            
            for (ChangeRecord change : fileChanges) {
                if (METHOD_PSEUDO_DELETE.equals(method)) {
                    executePseudoDelete(change, trashPath);
                } else {
                    executeDirectDelete(change);
                }
                change.setStatus(ChangeRecord.ExecStatus.SUCCESS);
                changes.add(change);
            }
        }
        
        return changes;
    }

    @Override
    public List<ChangeRecord> preview(List<String> filePaths, PluginConfigDTO config, ExecutionContext context) {
        List<ChangeRecord> changes = new ArrayList<>();
        String mode = (String) config.getValue("cleanupMode", MODE_DEDUP_FILES);
        boolean keepLargest = (Boolean) config.getValue("keepLargest", true);
        boolean keepEarliest = (Boolean) config.getValue("keepEarliest", true);
        String keepExt = (String) config.getValue("keepExt", "wav");
        boolean preprocessLower = (Boolean) config.getValue("preprocessLower", true);
        boolean preprocessUpper = (Boolean) config.getValue("preprocessUpper", false);
        boolean preprocessSimplified = (Boolean) config.getValue("preprocessSimplified", false);
        boolean audioSpecial = (Boolean) config.getValue("audioSpecial", true);
        int minFileSizeKB = (Integer) config.getValue("minFileSizeKB", 0);
        int maxFileSizeKB = (Integer) config.getValue("maxFileSizeKB", 10240);
        
        for (String filePath : filePaths) {
            File file = new File(filePath);
            if (!file.exists()) {
                continue;
            }
            
            List<ChangeRecord> fileChanges = analyzeFile(file, mode, keepLargest, keepEarliest, keepExt, 
                    preprocessLower, preprocessUpper, preprocessSimplified, audioSpecial, 
                    minFileSizeKB, maxFileSizeKB);
            
            for (ChangeRecord change : fileChanges) {
                change.setStatus(ChangeRecord.ExecStatus.PENDING);
                changes.add(change);
            }
        }
        
        return changes;
    }
    
    private List<ChangeRecord> analyzeFile(File file, String mode, boolean keepLargest, boolean keepEarliest, 
            String keepExt, boolean preprocessLower, boolean preprocessUpper, boolean preprocessSimplified, 
            boolean audioSpecial, int minFileSizeKB, int maxFileSizeKB) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        if (MODE_REMOVE_EMPTY_DIRS.equals(mode)) {
            if (file.isDirectory() && isDirectoryEmpty(file)) {
                ChangeRecord record = new ChangeRecord();
                record.setId("change-" + System.currentTimeMillis() + "-" + file.hashCode());
                record.setOriginalName(file.getAbsolutePath());
                record.setNewName(null);
                record.setFilePath(file.getAbsolutePath());
                record.setChanged(true);
                record.setOperationType(ChangeRecord.OperationType.DELETE);
                record.setReason("空文件夹 (无子文件)");
                changes.add(record);
            }
        } else if (MODE_DEDUP_FOLDERS.equals(mode)) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null && files.length >= 2) {
                    changes.addAll(analyzeDuplicateFolders(Arrays.asList(files)));
                }
            }
        } else if (MODE_DIRECT_CLEANUP.equals(mode)) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File subFile : files) {
                        if (subFile.isFile() && isFileSizeInRange(subFile, minFileSizeKB, maxFileSizeKB)) {
                            ChangeRecord record = new ChangeRecord();
                            record.setId("change-" + System.currentTimeMillis() + "-" + subFile.hashCode());
                            record.setOriginalName(subFile.getAbsolutePath());
                            record.setNewName(null);
                            record.setFilePath(subFile.getAbsolutePath());
                            record.setChanged(true);
                            record.setOperationType(ChangeRecord.OperationType.DELETE);
                            record.setReason("直接清理文件");
                            changes.add(record);
                        }
                    }
                }
            } else {
                if (isFileSizeInRange(file, minFileSizeKB, maxFileSizeKB)) {
                    ChangeRecord record = new ChangeRecord();
                    record.setId("change-" + System.currentTimeMillis() + "-" + file.hashCode());
                    record.setOriginalName(file.getAbsolutePath());
                    record.setNewName(null);
                    record.setFilePath(file.getAbsolutePath());
                    record.setChanged(true);
                    record.setOperationType(ChangeRecord.OperationType.DELETE);
                    record.setReason("直接清理文件");
                    changes.add(record);
                }
            }
        } else if (MODE_MERGE_SAME_NAME.equals(mode)) {
            if (file.isDirectory()) {
                changes.addAll(analyzeMergeSameNameFolders(file));
            }
        } else if (MODE_MERGE_NESTED.equals(mode)) {
            if (file.isDirectory()) {
                changes.addAll(analyzeMergeNestedFolders(file));
            }
        } else {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null && files.length >= 2) {
                    List<File> filteredFiles = Arrays.stream(files)
                            .filter(subFile -> subFile.isFile() && isFileSizeInRange(subFile, minFileSizeKB, maxFileSizeKB))
                            .collect(Collectors.toList());
                    if (filteredFiles.size() >= 2) {
                        changes.addAll(analyzeDuplicateFiles(filteredFiles, keepLargest, keepEarliest, keepExt, 
                                preprocessLower, preprocessUpper, preprocessSimplified, audioSpecial));
                    }
                }
            }
        }
        
        return changes;
    }
    
    private List<ChangeRecord> analyzeDuplicateFiles(List<File> files, boolean keepLargest, boolean keepEarliest, 
            String keepExt, boolean preprocessLower, boolean preprocessUpper, boolean preprocessSimplified, boolean audioSpecial) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        Map<String, List<File>> nameGroups = new HashMap<>();
        for (File file : files) {
            String name = preprocessName(file.getName(), preprocessLower, preprocessUpper, preprocessSimplified);
            nameGroups.computeIfAbsent(name, k -> new ArrayList<>()).add(file);
        }
        
        for (Map.Entry<String, List<File>> entry : nameGroups.entrySet()) {
            List<File> duplicates = entry.getValue();
            if (duplicates.size() < 2) {
                continue;
            }
            
            File keepFile = selectFileToKeep(duplicates, keepLargest, keepEarliest, keepExt, audioSpecial);
            for (File file : duplicates) {
                if (file.equals(keepFile)) {
                    continue;
                }
                
                ChangeRecord record = new ChangeRecord();
                record.setId("change-" + System.currentTimeMillis() + "-" + file.hashCode());
                record.setOriginalName(file.getAbsolutePath());
                record.setNewName(null);
                record.setFilePath(file.getAbsolutePath());
                record.setChanged(true);
                record.setOperationType(ChangeRecord.OperationType.DELETE);
                record.setReason("重复文件 (保留: " + keepFile.getName() + ")");
                changes.add(record);
            }
        }
        
        return changes;
    }
    
    private List<ChangeRecord> analyzeDuplicateFolders(List<File> folders) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        Map<String, List<File>> nameGroups = new HashMap<>();
        for (File folder : folders) {
            if (!folder.isDirectory()) {
                continue;
            }
            String name = folder.getName().toLowerCase();
            nameGroups.computeIfAbsent(name, k -> new ArrayList<>()).add(folder);
        }
        
        for (Map.Entry<String, List<File>> entry : nameGroups.entrySet()) {
            List<File> duplicates = entry.getValue();
            if (duplicates.size() < 2) {
                continue;
            }
            
            File keepFolder = duplicates.get(0);
            for (int i =1; i < duplicates.size(); i++) {
                File folder = duplicates.get(i);
                ChangeRecord record = new ChangeRecord();
                record.setId("change-" + System.currentTimeMillis() + "-" + folder.hashCode());
                record.setOriginalName(folder.getAbsolutePath());
                record.setNewName(null);
                record.setFilePath(folder.getAbsolutePath());
                record.setChanged(true);
                record.setOperationType(ChangeRecord.OperationType.DELETE);
                record.setReason("重复文件夹 (保留: " + keepFolder.getName() + ")");
                changes.add(record);
            }
        }
        
        return changes;
    }
    
    private List<ChangeRecord> analyzeMergeSameNameFolders(File parentDir) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        File[] files = parentDir.listFiles();
        if (files == null) {
            return changes;
        }
        
        Map<String, File> fileMap = new HashMap<>();
        Map<String, File> dirMap = new HashMap<>();
        
        for (File file : files) {
            String name = file.getName().toLowerCase();
            if (file.isFile()) {
                fileMap.put(name, file);
            } else if (file.isDirectory()) {
                dirMap.put(name, file);
            }
        }
        
        for (Map.Entry<String, File> entry : dirMap.entrySet()) {
            String name = entry.getKey();
            File dir = entry.getValue();
            
            if (fileMap.containsKey(name)) {
                File file = fileMap.get(name);
                ChangeRecord record = new ChangeRecord();
                record.setId("change-" + System.currentTimeMillis() + "-" + dir.hashCode());
                record.setOriginalName(dir.getAbsolutePath());
                record.setNewName(file.getAbsolutePath());
                record.setFilePath(dir.getAbsolutePath());
                record.setChanged(true);
                record.setOperationType(ChangeRecord.OperationType.MERGE);
                record.setReason("同名父子文件夹合并");
                Map<String, Object> params = new HashMap<>();
                params.put("operation", "merge_folder");
                params.put("childDir", dir.getAbsolutePath());
                params.put("parentDir", file.getAbsolutePath());
                record.setExtraParams(params);
                changes.add(record);
            }
        }
        
        return changes;
    }
    
    private List<ChangeRecord> analyzeMergeNestedFolders(File parentDir) {
        List<ChangeRecord> changes = new ArrayList<>();
        
        File[] files = parentDir.listFiles();
        if (files == null) {
            return changes;
        }
        
        for (File file : files) {
            if (!file.isDirectory()) {
                continue;
            }
            
            File[] subFiles = file.listFiles();
            if (subFiles == null || subFiles.length == 0) {
                continue;
            }
            
            for (File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    ChangeRecord record = new ChangeRecord();
                    record.setId("change-" + System.currentTimeMillis() + "-" + subFile.hashCode());
                    record.setOriginalName(subFile.getAbsolutePath());
                    record.setNewName(new File(parentDir, subFile.getName()).getAbsolutePath());
                    record.setFilePath(subFile.getAbsolutePath());
                    record.setChanged(true);
                    record.setOperationType(ChangeRecord.OperationType.MERGE);
                    record.setReason("嵌套文件夹合并");
                    Map<String, Object> params = new HashMap<>();
                    params.put("operation", "merge_nested_folder");
                    params.put("childDir", subFile.getAbsolutePath());
                    params.put("parentDir", parentDir.getAbsolutePath());
                    record.setExtraParams(params);
                    changes.add(record);
                }
            }
        }
        
        return changes;
    }
    
    private File selectFileToKeep(List<File> files, boolean keepLargest, boolean keepEarliest, 
            String keepExt, boolean audioSpecial) {
        File selected = files.get(0);
        
        if (keepLargest) {
            for (File file : files) {
                if (file.length() > selected.length()) {
                    selected = file;
                }
            }
        }
        
        if (keepEarliest) {
            for (File file : files) {
                if (file.lastModified() < selected.lastModified()) {
                    selected = file;
                }
            }
        }
        
        if (audioSpecial && keepExt != null && !keepExt.isEmpty()) {
            for (File file : files) {
                String ext = getFileExtension(file);
                if (keepExt.equalsIgnoreCase(ext)) {
                    selected = file;
                    break;
                }
            }
        }
        
        return selected;
    }
    
    private String preprocessName(String name, boolean toLower, boolean toUpper, boolean toSimplified) {
        String result = name;
        
        if (toLower) {
            result = result.toLowerCase();
        }
        
        if (toUpper) {
            result = result.toUpperCase();
        }
        
        if (toSimplified) {
            result = toSimplifiedChinese(result);
        }
        
        return result;
    }
    
    private String toSimplifiedChinese(String text) {
        return text;
    }
    
    private String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex > 0 ? name.substring(dotIndex + 1) : "";
    }
    
    private boolean isFileSizeInRange(File file, int minSizeKB, int maxSizeKB) {
        long sizeKB = file.length() / 1024;
        return sizeKB >= minSizeKB && sizeKB <= maxSizeKB;
    }
    
    private boolean isDirectoryEmpty(File dir) {
        File[] files = dir.listFiles();
        return files == null || files.length == 0;
    }
    
    private void executeDirectDelete(ChangeRecord record) {
        File file = new File(record.getFilePath());
        if (file.exists()) {
            if (file.isDirectory()) {
                deleteDirectoryRecursively(file);
            } else {
                file.delete();
            }
        }
    }
    
    private void executePseudoDelete(ChangeRecord record, String trashPath) {
        File file = new File(record.getFilePath());
        if (!file.exists()) {
            return;
        }
        
        File trashDir = new File(trashPath);
        if (!trashDir.exists()) {
            trashDir.mkdirs();
        }
        
        File destFile = new File(trashDir, file.getName());
        try {
            Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void deleteDirectoryRecursively(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}