package com.filemanager.plugin.impl.filetypefix;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.filetypefix.enums.TargetFormat;
import java.io.File;

public class FileTypeFixStrategy extends AbstractConfigurableStrategy {

    public FileTypeFixStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-type-fix";
    }

    @Override
    public String getName() {
        return "文件类型修复";
    }

    @Override
    public String getDescription() {
        return "修复损坏或格式错误的文件";
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
        addEnumConfigField("targetFormat", "目标格式", "select", (Object) TargetFormat.AUTO_DETECT.getCode(), 
            "修复后的文件格式", true, 
            getTargetFormatOptions());
        addConfigField("keepOriginal", "保留原始文件", "boolean", (Object) true, 
            "是否保留原始文件", false);
        addConfigField("backupOriginal", "备份原始文件", "boolean", (Object) true, 
            "是否备份原始文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetFormat", (Object) TargetFormat.AUTO_DETECT.getCode());
        setConfigValue(config, "keepOriginal", (Object) true);
        setConfigValue(config, "backupOriginal", (Object) true);
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetFormat = getConfigValue(config, "targetFormat", "auto_detect");
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType("TYPE_FIX");
        record.setReason("文件类型修复: " + targetFormat);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String targetFormat = getConfigValue(config, "targetFormat", "auto_detect");
        boolean keepOriginal = getConfigValue(config, "keepOriginal", true);
        boolean backupOriginal = getConfigValue(config, "backupOriginal", true);
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String detectedFormat = detectFileFormat(sourceFile);
            String newExtension = getExtension(targetFormat, detectedFormat);
            
            if (newExtension == null || sourceFile.getName().toLowerCase().endsWith("." + newExtension)) {
                context.logDebug("File format already correct: " + filePath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            String newPath = sourceFile.getPath().replaceAll("\\.[^.]+$", "." + newExtension);
            File targetFile = new File(newPath);
            
            if (backupOriginal) {
                String backupPath = filePath + ".bak";
                File backupFile = new File(backupPath);
                java.nio.file.Files.copy(sourceFile.toPath(), backupFile.toPath());
                context.logDebug("Created backup: " + backupPath);
            }
            
            sourceFile.renameTo(targetFile);
            
            context.logInfo("Fixed file type: " + filePath + " -> " + newPath);
            ChangeRecord record = createChangeRecord(filePath, newPath, "SUCCESS");
            record.setOperationType("TYPE_FIX");
            record.setReason("文件类型修复: " + targetFormat);
            return record;
        } catch (Exception e) {
            context.logError("Error fixing file type for " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String detectFileFormat(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".mp3")) return "mp3";
        if (name.endsWith(".flac")) return "flac";
        if (name.endsWith(".wav")) return "wav";
        if (name.endsWith(".aac")) return "aac";
        if (name.endsWith(".ogg")) return "ogg";
        if (name.endsWith(".m4a")) return "m4a";
        return "unknown";
    }

    private String getExtension(String targetFormat, String detectedFormat) {
        if (targetFormat.equals("auto_detect")) {
            return detectedFormat.equals("unknown") ? null : detectedFormat;
        }
        return targetFormat;
    }
    
    private java.util.List<EnumOptionDTO> getTargetFormatOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (TargetFormat format : TargetFormat.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(format.getCode());
            option.setLabel(format.getNameZh());
            option.setNameEn(format.getNameEn());
            option.setDescriptionZh(format.getDescriptionZh());
            option.setDescriptionEn(format.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}
