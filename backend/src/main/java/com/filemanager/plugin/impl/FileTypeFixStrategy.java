package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;

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
        addConfigField("targetFormat", "目标格式", "select", (Object) "自动检测", 
            "修复后的文件格式", true, 
            Arrays.asList("自动检测", "WAV", "FLAC", "MP3", "AAC", "OGG"));
        addConfigField("keepOriginal", "保留原始文件", "boolean", (Object) true, 
            "是否保留原始文件", false);
        addConfigField("backupOriginal", "备份原始文件", "boolean", (Object) true, 
            "是否备份原始文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetFormat", (Object) "自动检测");
        setConfigValue(config, "keepOriginal", (Object) true);
        setConfigValue(config, "backupOriginal", (Object) true);
    }
}