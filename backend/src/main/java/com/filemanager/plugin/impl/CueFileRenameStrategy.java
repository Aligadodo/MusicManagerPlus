package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;

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
    protected void initConfigFields() {
        addConfigField("renameMode", "重命名模式", "select", (Object) "基于音频文件名", 
            "CUE文件的重命名模式", true, 
            Arrays.asList("基于音频文件名", "基于目录名", "自定义"));
        addConfigField("customTemplate", "自定义模板", "string", (Object) "", 
            "自定义重命名模板", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "renameMode", (Object) "基于音频文件名");
        setConfigValue(config, "customTemplate", (Object) "");
    }
}