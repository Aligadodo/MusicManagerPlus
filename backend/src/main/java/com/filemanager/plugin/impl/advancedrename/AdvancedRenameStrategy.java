package com.filemanager.plugin.impl.advancedrename;

import com.filemanager.domain.dto.StrategyConfigDTO;
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
        addConfigField("crossDriveMode", "跨盘动作", "select", (Object) CrossDriveMode.MOVE.getCode(), 
            "跨盘操作时的动作", false, 
            getCrossDriveModeOptions());
        addConfigField("processScope", "处理范围", "select", (Object) ProcessScope.ALL.getCode(), 
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
    
    private List<String> getCrossDriveModeOptions() {
        return Arrays.asList(
            CrossDriveMode.MOVE.getCode(),
            CrossDriveMode.COPY.getCode()
        );
    }
    
    private List<String> getProcessScopeOptions() {
        return Arrays.asList(
            ProcessScope.FILES_ONLY.getCode(),
            ProcessScope.DIRECTORIES_ONLY.getCode(),
            ProcessScope.ALL.getCode()
        );
    }
}