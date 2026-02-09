package com.filemanager.plugin.impl.advancedrename;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.domain.dto.EnumOptionDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.advancedrename.enums.CrossDriveMode;
import com.filemanager.plugin.impl.advancedrename.enums.ProcessScope;

import java.util.Arrays;
import java.util.List;

public class AdvancedRenameStrategy extends AbstractConfigurableStrategy {

    public AdvancedRenameStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "advanced-rename";
    }

    @Override
    public String getName() {
        return "高级重命名策略";
    }

    @Override
    public String getDescription() {
        return "基于规则的高级文件重命名功能，支持多种条件和操作";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void initConfigFields() {
        addEnumConfigField("crossDriveMode", "跨盘动作", "select", (Object) CrossDriveMode.MOVE.getCode(), 
            "跨盘操作时的动作", false, 
            getCrossDriveModeOptions());
        addEnumConfigField("processScope", "处理范围", "select", (Object) ProcessScope.ALL.getCode(), 
            "处理的文件类型范围", false, 
            getProcessScopeOptions());
        addConfigField("rules", "重命名规则", "list", (Object) new java.util.ArrayList<>(), 
            "重命名规则列表", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "crossDriveMode", (Object) CrossDriveMode.MOVE.getCode());
        setConfigValue(config, "processScope", (Object) ProcessScope.ALL.getCode());
        setConfigValue(config, "rules", (Object) new java.util.ArrayList<>());
    }
    
    private java.util.List<EnumOptionDTO> getCrossDriveModeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (CrossDriveMode mode : CrossDriveMode.values()) {
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
    
    private java.util.List<EnumOptionDTO> getProcessScopeOptions() {
        java.util.List<EnumOptionDTO> options = new java.util.ArrayList<>();
        for (ProcessScope scope : ProcessScope.values()) {
            EnumOptionDTO option = new EnumOptionDTO();
            option.setValue(scope.getCode());
            option.setLabel(scope.getNameZh());
            option.setNameEn(scope.getNameEn());
            option.setDescriptionZh(scope.getDescriptionZh());
            option.setDescriptionEn(scope.getDescriptionEn());
            options.add(option);
        }
        return options;
    }
}