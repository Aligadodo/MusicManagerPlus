package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.enums.NcmOperationMode;
import com.filemanager.plugin.impl.enums.NcmOutputFormat;
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