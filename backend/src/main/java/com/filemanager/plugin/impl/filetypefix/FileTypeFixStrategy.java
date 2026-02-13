package com.filemanager.plugin.impl.filetypefix;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.domain.entity.ChangeRecord;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.ExecutionContext;
import com.filemanager.domain.enums.ScanTarget;
import com.filemanager.domain.enums.ExecStatus;
import com.filemanager.domain.enums.OperationType;
import com.filemanager.plugin.impl.filetypefix.enums.TargetFormat;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

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
    public ScanTarget getTargetType() {
        return ScanTarget.FILES_ONLY;
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
    public List<ChangeRecord> analyze(ChangeRecord currentRecord, 
        List<ChangeRecord> inputRecords, 
        List<File> rootDirs, 
        StrategyConfigDTO config, 
        ExecutionContext context) {
        
        File file = currentRecord.getFileHandle();
        if (!file.isFile()) {
            return Collections.emptyList();
        }
        
        String targetFormat = getConfigValue(config, "targetFormat", "auto_detect");
        
        context.logInfo("分析文件类型修复: " + file.getName() + ", 目标格式: " + targetFormat);
        
        String detectedFormat = detectFileFormat(file);
        String newExtension = getExtension(targetFormat, detectedFormat);
        
        if (newExtension == null || file.getName().toLowerCase().endsWith("." + newExtension)) {
            context.logDebug("文件格式已正确: " + file.getName());
            return Collections.emptyList();
        }
        
        String newPath = file.getPath().replaceAll("\\.[^.]+$", "." + newExtension);
        
        ChangeRecord record = new ChangeRecord(
            currentRecord.getOriginalName(),
            file.getName().replaceAll("\\.[^.]+$", "." + newExtension),
            currentRecord.getFileHandle(),
            true,
            newPath,
            OperationType.RENAME,
            new java.util.HashMap<>(),
            ExecStatus.PENDING
        );
        
        return Collections.singletonList(record);
    }

    @Override
    public void execute(ChangeRecord record, StrategyConfigDTO config, ExecutionContext context) throws Exception {
        File sourceFile = record.getFileHandle();
        File targetFile = new File(record.getNewPath());
        
        if (!sourceFile.exists()) {
            context.logWarn("源文件不存在: " + sourceFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
            return;
        }
        
        boolean backupOriginal = getConfigValue(config, "backupOriginal", true);
        
        if (backupOriginal) {
            String backupPath = sourceFile.getPath() + ".bak";
            File backupFile = new File(backupPath);
            Files.copy(sourceFile.toPath(), backupFile.toPath());
            context.logDebug("创建备份: " + backupPath);
        }
        
        if (sourceFile.renameTo(targetFile)) {
            context.logInfo("修复文件类型: " + sourceFile.getPath() + " -> " + targetFile.getPath());
            record.setStatus(ExecStatus.SUCCESS.name());
        } else {
            context.logError("修复文件类型失败: " + sourceFile.getPath());
            record.setStatus(ExecStatus.FAILED.name());
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
