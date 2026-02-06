package com.filemanager.plugin.impl.filetypefix;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.filetypefix.enums.TargetFormat;

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
    protected void initConfigFields() {
        addConfigField("targetFormat", "目标格式", "select", (Object) TargetFormat.AUTO_DETECT.getCode(), 
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
    
    private java.util.List<String> getTargetFormatOptions() {
        return java.util.Arrays.asList(
            TargetFormat.AUTO_DETECT.getCode(),
            TargetFormat.WAV.getCode(),
            TargetFormat.FLAC.getCode(),
            TargetFormat.MP3.getCode(),
            TargetFormat.AAC.getCode(),
            TargetFormat.OGG.getCode()
        );
    }
}
