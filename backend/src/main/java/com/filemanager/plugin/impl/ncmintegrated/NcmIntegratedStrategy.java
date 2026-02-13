package com.filemanager.plugin.impl.ncmintegrated;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.impl.ncmintegrated.enums.NcmOperationMode;
import com.filemanager.plugin.impl.ncmintegrated.enums.NcmOutputFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
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
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File file = currentRecord.getFileHandle();
        if (!file.isFile()) {
            return Collections.emptyList();
        }
        
        String operationMode = getConfigValue(config, "operationMode", "convert");
        String outputFormat = getConfigValue(config, "outputFormat", "mp3");
        
        context.logInfo("分析网易云音乐操作: " + file.getName() + ", 模式: " + operationMode);
        
        Map<String, String> params = new HashMap<>();
        params.put("operationMode", operationMode);
        params.put("outputFormat", outputFormat);
        
        OperationType opType = getOperationType(operationMode);
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            currentRecord.getOriginalName(),
            currentRecord.getFileHandle(),
            true,
            file.getPath(),
            opType,
            params,
            ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File sourceFile = record.getFileHandle();
        String operationMode = getConfigValue(config, "operationMode", "convert");
        String outputFormat = getConfigValue(config, "outputFormat", "mp3");
        String outputDirectory = getConfigValue(config, "outputDirectory", "");
        
        if (!sourceFile.exists()) {
            context.logWarn("源文件不存在: " + sourceFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        try {
            String targetPath = generateTargetPath(sourceFile, outputDirectory, outputFormat);
            File targetFile = new File(targetPath);
            
            if (targetFile.exists()) {
                context.logWarn("目标文件已存在: " + targetPath);
                record.setStatus(ExecStatus.FAILED.name());
                return;
            }
            
            context.logInfo("执行网易云音乐操作: " + sourceFile.getPath() + " -> " + targetPath);
            
            record.setStatus(ExecStatus.SUCCESS.name());
        } catch (Exception e) {
            context.logError("执行网易云音乐操作失败: " + sourceFile.getPath() + ", 错误: " + e.getMessage());
            record.setStatus(ExecStatus.FAILED.name());
        }
    }

    private OperationType getOperationType(String operationMode) {
        switch (operationMode) {
            case "convert":
                return OperationType.NCM_CONVERT;
            case "cache_scan":
                return OperationType.NCM_CACHE_SCAN;
            case "lyric_download":
                return OperationType.NCM_LYRIC_DOWNLOAD;
            default:
                return OperationType.NCM_CONVERT;
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