package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.enums.RenameMode;
import java.io.File;

public class CueFileRenameStrategy extends AbstractConfigurableStrategy {

    public CueFileRenameStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "cue-file-rename";
    }

    @Override
    public String getName() {
        return "CUE文件重命名";
    }

    @Override
    public String getDescription() {
        return "根据音频文件名或目录名重命名CUE文件";
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
        addEnumConfigField("renameMode", "重命名模式", "select", (Object) RenameMode.BASED_ON_AUDIO_FILE.getCode(), 
            "CUE文件的重命名模式", true, 
            getRenameModeOptions());
        addConfigField("customTemplate", "自定义模板", "string", (Object) "", 
            "自定义重命名模板", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "renameMode", (Object) RenameMode.BASED_ON_AUDIO_FILE.getCode());
        setConfigValue(config, "customTemplate", (Object) "");
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String renameMode = getConfigValue(config, "renameMode", "based_on_audio_file");
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("RENAME");
        record.setReason("CUE文件重命名: " + renameMode);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String renameMode = getConfigValue(config, "renameMode", "based_on_audio_file");
        String customTemplate = getConfigValue(config, "customTemplate", "");
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        if (!filePath.toLowerCase().endsWith(".cue")) {
            context.logDebug("Not a CUE file: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String newName = generateNewName(sourceFile, renameMode, customTemplate, context);
            if (newName == null || newName.equals(sourceFile.getName())) {
                context.logDebug("No rename needed for: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            File targetFile = new File(sourceFile.getParent(), newName);
            
            if (targetFile.exists()) {
                context.logWarn("Target file already exists: " + targetFile.getPath());
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            sourceFile.renameTo(targetFile);
            
            context.logInfo("Renamed CUE file: " + filePath + " -> " + targetFile.getPath());
            ChangeRecord record = createChangeRecord(filePath, targetFile.getPath(), "SUCCESS");
            record.setOperationType("RENAME");
            record.setReason("CUE文件重命名: " + renameMode);
            return record;
        } catch (Exception e) {
            context.logError("Error renaming CUE file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String generateNewName(File cueFile, String renameMode, String customTemplate, ExecutionContext context) {
        File parentDir = cueFile.getParentFile();
        if (parentDir == null) {
            return cueFile.getName();
        }
        
        switch (renameMode) {
            case "based_on_audio_file":
                return findAudioBasedName(cueFile, parentDir, context);
            case "based_on_directory":
                return parentDir.getName() + ".cue";
            case "custom_template":
                if (customTemplate != null && !customTemplate.isEmpty()) {
                    return customTemplate + ".cue";
                }
                return cueFile.getName();
            default:
                return cueFile.getName();
        }
    }

    private String findAudioBasedName(File cueFile, File parentDir, ExecutionContext context) {
        File[] files = parentDir.listFiles();
        if (files == null) {
            return cueFile.getName();
        }
        
        for (File file : files) {
            if (file.isFile() && !file.getName().toLowerCase().endsWith(".cue")) {
                String fileName = file.getName();
                int lastDotIndex = fileName.lastIndexOf('.');
                if (lastDotIndex > 0) {
                    return fileName.substring(0, lastDotIndex) + ".cue";
                }
            }
        }
        
        return cueFile.getName();
    }
    
    private java.util.List<EnumOptionDTO> getRenameModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (RenameMode mode : RenameMode.values()) {
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
}