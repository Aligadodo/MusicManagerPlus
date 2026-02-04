package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;
import java.util.ArrayList;

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
        addConfigField("crossDriveMode", "跨盘动作", "select", (Object) "移动 (Move)", 
            "跨盘操作时的动作", false, 
            Arrays.asList("移动 (Move)", "复制 (Copy)"));
        addConfigField("processScope", "处理范围", "select", (Object) "全部处理", 
            "处理的文件类型范围", false, 
            Arrays.asList("仅处理文件", "仅处理文件夹", "全部处理"));
        addConfigField("rules", "重命名规则", "list", (Object) new ArrayList<>(), 
            "重命名规则列表", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "crossDriveMode", (Object) "移动 (Move)");
        setConfigValue(config, "processScope", (Object) "全部处理");
        setConfigValue(config, "rules", (Object) new ArrayList<>());
    }
}