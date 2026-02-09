package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.enums.RenameMode;

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