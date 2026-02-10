package com.filemanager.plugin.impl.filecollection;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

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
        return "文件收集";
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
    public List<com.filemanager.domain.dto.PreconditionGroupDTO> getDefaultPreconditionGroups() {
        return new ArrayList<>();
    }

    @Override
    protected void initConfigFields() {
        addConfigField("targetDirectory", "目标目录", "directory", "/tmp/collected", 
            "文件收集的目标目录", true);
        addConfigField("recursive", "递归收集", "boolean", true, 
            "是否递归收集子目录中的文件", false);
        addConfigField("includePatterns", "包含模式", "text", "*.mp3,*.wav,*.flac", 
            "要收集的文件模式列表，多个模式用逗号分隔", false);
        addConfigField("excludePatterns", "排除模式", "text", "*.tmp,*.log", 
            "要排除的文件模式列表，多个模式用逗号分隔", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetDirectory", "/tmp/collected");
        setConfigValue(config, "recursive", true);
        setConfigValue(config, "includePatterns", "*.mp3,*.wav,*.flac");
        setConfigValue(config, "excludePatterns", "*.tmp,*.log");
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetDir = getConfigValue(config, "targetDirectory", "/tmp/collected");
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        String targetPath = targetDir + "/" + fileName;

        ChangeRecord record = createChangeRecord(filePath, targetPath, "PENDING");
        record.setOperationType("MOVE");
        record.setReason("收集文件到目标目录");
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetDir = getConfigValue(config, "targetDirectory", "/tmp/collected");
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }

        try {
            String fileName = sourceFile.getName();
            File targetDirFile = new File(targetDir);
            
            if (!targetDirFile.exists()) {
                targetDirFile.mkdirs();
            }
            
            String targetPath = targetDir + "/" + fileName;
            File targetFile = new File(targetPath);
            
            if (targetFile.exists()) {
                context.logWarn("Target file already exists: " + targetPath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            Files.copy(Paths.get(filePath), Paths.get(targetPath), StandardCopyOption.COPY_ATTRIBUTES);
            
            context.logInfo("Collected file: " + filePath + " -> " + targetPath);
            ChangeRecord record = createChangeRecord(filePath, targetPath, "SUCCESS");
            record.setOperationType("COPY");
            record.setReason("收集文件到目标目录");
            return record;
        } catch (Exception e) {
            context.logError("Error collecting file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }
}
