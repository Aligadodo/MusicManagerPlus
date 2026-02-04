package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;

public class FileCollectionStrategy extends AbstractConfigurableStrategy {

    public FileCollectionStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-collection";
    }

    @Override
    public String getName() {
        return "文件收集策略";
    }

    @Override
    public String getDescription() {
        return "根据配置规则收集和整理文件";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void initConfigFields() {
        addConfigField("targetDirectory", "目标目录", "directory", (Object) "/tmp/collected", 
            "文件收集的目标目录", true);
        addConfigField("recursive", "递归收集", "boolean", (Object) true, 
            "是否递归收集子目录中的文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "targetDirectory", (Object) "/tmp/collected");
        setConfigValue(config, "recursive", (Object) true);
    }
}