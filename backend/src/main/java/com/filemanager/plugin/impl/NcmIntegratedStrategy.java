package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;

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
    protected void initConfigFields() {
        addConfigField("operationMode", "操作模式", "select", (Object) "转换", 
            "操作模式", true, 
            Arrays.asList("转换", "缓存转换", "歌词下载", "元数据修复"));
        addConfigField("outputFormat", "输出格式", "select", (Object) "MP3", 
            "输出格式", true, 
            Arrays.asList("MP3", "FLAC", "WAV"));
        addConfigField("outputDirectory", "输出目录", "directory", (Object) "", 
            "输出目录", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "operationMode", (Object) "转换");
        setConfigValue(config, "outputFormat", (Object) "MP3");
        setConfigValue(config, "outputDirectory", (Object) "");
    }
}