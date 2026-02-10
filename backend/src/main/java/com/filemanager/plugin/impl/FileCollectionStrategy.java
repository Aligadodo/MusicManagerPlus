package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import java.io.File;

public class FileCollectionStrategy extends AbstractConfigurableStrategy {

    public FileCollectionStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-collection";
    }

    @Override
    public String getName() {
        return "文件收集策略";
    }

    @Override
    public String getDescription() {
        return "根据配置规则收集和整理文件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public java.util.List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new java.util.ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addConfigField("targetDirectory", "目标目录", "directory", (Object) "/tmp/collected", 
            "文件收集的目标目录", true);
        addConfigField("recursive", "递归收集", "boolean", (Object) true, 
            "是否递归收集子目录中的文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetDirectory", (Object) "/tmp/collected");
        setConfigValue(config, "recursive", (Object) true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetDirectory = getConfigValue(config, "targetDirectory", "/tmp/collected");
        File sourceFile = new File(filePath);
        File targetFile = new File(targetDirectory, sourceFile.getName());
        
        ChangeRecord record = createChangeRecord(filePath, targetFile.getPath(), "PENDING");
        record.setOperationType("COLLECT");
        record.setReason("文件收集到目标目录");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetDirectory = getConfigValue(config, "targetDirectory", "/tmp/collected");
        boolean recursive = getConfigValue(config, "recursive", true);
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            File targetDir = new File(targetDirectory);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
                context.logDebug("Created target directory: " + targetDir.getPath());
            }
            
            File targetFile = new File(targetDir, sourceFile.getName());
            
            if (targetFile.exists()) {
                context.logWarn("Target file already exists: " + targetFile.getPath());
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            java.nio.file.Files.copy(sourceFile.toPath(), targetFile.toPath());
            
            context.logInfo("Collected file: " + filePath + " -> " + targetFile.getPath());
            ChangeRecord record = createChangeRecord(filePath, targetFile.getPath(), "SUCCESS");
            record.setOperationType("COLLECT");
            record.setReason("文件收集到目标目录");
            return record;
        } catch (Exception e) {
            context.logError("Error collecting file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }
}