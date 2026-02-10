package com.filemanager.plugin.impl.ncmintegrated;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.plugin.impl.ncmintegrated.enums.NcmOperationMode;
import com.filemanager.plugin.impl.ncmintegrated.enums.NcmOutputFormat;
import java.io.File;
import java.util.ArrayList;

public class NcmIntegratedStrategy extends AbstractConfigurableStrategy {

    public NcmIntegratedStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "ncm-integrated";
    }

    @Override
    public String getName() {
        return "网易云音乐集成";
    }

    @Override
    public String getDescription() {
        return "网易云音乐格式转换和元数据修复";
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
        addEnumConfigField("operationMode", "操作模式", "select", (Object) NcmOperationMode.CONVERT.getCode(), 
            "操作模式", true, 
            getOperationModeOptions());
        addEnumConfigField("outputFormat", "输出格式", "select", (Object) NcmOutputFormat.MP3.getCode(), 
            "输出格式", true, 
            getOutputFormatOptions());
        addConfigField("outputDirectory", "输出目录", "directory", (Object) "", 
            "输出目录", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "operationMode", (Object) NcmOperationMode.CONVERT.getCode());
        setConfigValue(config, "outputFormat", (Object) NcmOutputFormat.MP3.getCode());
        setConfigValue(config, "outputDirectory", (Object) "");
    }

    @Override
    protected ChangeRecord createPreviewRecord(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String operationMode = getConfigValue(config, "operationMode", "convert");
        String outputFormat = getConfigValue(config, "outputFormat", "mp3");
        
        ChangeRecord record = createChangeRecord(filePath, filePath, "PENDING");
        record.setOperationType(operationMode.toUpperCase());
        record.setReason("网易云音乐格式转换: " + outputFormat);
        return record;
    }

    @Override
    protected ChangeRecord executeForFile(String filePath, StrategyConfigDTO config, ExecutionContext context) {
        String operationMode = getConfigValue(config, "operationMode", "convert");
        String outputFormat = getConfigValue(config, "outputFormat", "mp3");
        String outputDirectory = getConfigValue(config, "outputDirectory", "");
        
        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            context.logWarn("File does not exist: " + filePath);
            return createChangeRecord(filePath, filePath, "SKIPPED");
        }
        
        try {
            String targetPath = generateTargetPath(sourceFile, outputDirectory, outputFormat);
            File targetFile = new File(targetPath);
            
            if (targetFile.exists()) {
                context.logWarn("Target file already exists: " + targetPath);
                return createChangeRecord(filePath, filePath, "SKIPPED");
            }
            
            context.logInfo("NCM conversion: " + filePath + " -> " + targetPath);
            ChangeRecord record = createChangeRecord(filePath, targetPath, "SUCCESS");
            record.setOperationType(operationMode.toUpperCase());
            record.setReason("网易云音乐格式转换: " + outputFormat);
            return record;
        } catch (Exception e) {
            context.logError("Error processing NCM file " + filePath + ": " + e.getMessage());
            return createChangeRecord(filePath, filePath, "ERROR");
        }
    }

    private String generateTargetPath(File sourceFile, String outputDirectory, String outputFormat) {
        String baseName = sourceFile.getName();
        if (baseName.toLowerCase().endsWith(".ncm")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }
        
        String extension = getExtension(outputFormat);
        String targetFileName = baseName + "." + extension;
        
        if (outputDirectory != null && !outputDirectory.isEmpty()) {
            return outputDirectory + File.separator + targetFileName;
        } else {
            return sourceFile.getParent() + File.separator + targetFileName;
        }
    }

    private String getExtension(String format) {
        switch (format) {
            case "mp3":
                return "mp3";
            case "flac":
                return "flac";
            case "wav":
                return "wav";
            case "aac":
                return "aac";
            case "ogg":
                return "ogg";
            default:
                return "mp3";
        }
    }
    
    private java.util.List<EnumOptionDTO> getOperationModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (NcmOperationMode mode : NcmOperationMode.values()) {
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
    
    private java.util.List<EnumOptionDTO> getOutputFormatOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (NcmOutputFormat format : NcmOutputFormat.values()) {
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