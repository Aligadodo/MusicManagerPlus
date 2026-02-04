package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;

public class FileMigrateStrategy extends AbstractConfigurableStrategy {

    public FileMigrateStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-migrate";
    }

    @Override
    public String getName() {
        return "文件批量归档和移动";
    }

    @Override
    public String getDescription() {
        return "文件批量归档和移动工具，支持复制/移动操作，多种路径模式选择。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void initConfigFields() {
        addConfigField("operationMode", "操作模式", "select", (Object) "移动 (MOVE)", 
            "文件的操作方式", true, 
            Arrays.asList("移动 (MOVE)", "复制 (COPY)"));
        addConfigField("outputDirMode", "输出目录模式", "select", (Object) "子目录", 
            "输出目录模式", true, 
            Arrays.asList("子目录", "指定目录", "根目录"));
        addConfigField("outputPath", "输出路径", "directory", (Object) "Archive", 
            "目标路径", true);
        addConfigField("scope", "生效范围", "select", (Object) "全部", 
            "文件处理的生效范围", false, 
            Arrays.asList("全部", "当前目录", "指定深度"));
        addConfigField("depth", "深度值", "number", (Object) 0, 
            "指定生效范围的深度值", false);
        addConfigField("keepLargest", "保留最大文件", "boolean", (Object) true, 
            "去重时保留最大的文件", false);
        addConfigField("keepEarliest", "保留最早文件", "boolean", (Object) true, 
            "去重时保留日期最早的文件", false);
        addConfigField("keepExt", "优先后缀", "string", (Object) "wav", 
            "去重时优先保留的文件后缀", false);
        addConfigField("audioSpecial", "音频特殊处理", "boolean", (Object) true, 
            "去重时对音频文件进行特殊处理", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "operationMode", (Object) "移动 (MOVE)");
        setConfigValue(config, "outputDirMode", (Object) "子目录");
        setConfigValue(config, "outputPath", (Object) "Archive");
        setConfigValue(config, "scope", (Object) "全部");
        setConfigValue(config, "depth", (Object) 0);
        setConfigValue(config, "keepLargest", (Object) true);
        setConfigValue(config, "keepEarliest", (Object) true);
        setConfigValue(config, "keepExt", (Object) "wav");
        setConfigValue(config, "audioSpecial", (Object) true);
    }
}