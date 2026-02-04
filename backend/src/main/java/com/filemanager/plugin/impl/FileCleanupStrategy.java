package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;

public class FileCleanupStrategy extends AbstractConfigurableStrategy {

    public FileCleanupStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-cleanup";
    }

    @Override
    public String getName() {
        return "文件清理与去重";
    }

    @Override
    public String getDescription() {
        return "智能识别重复文件/文件夹、清理空目录、合并同名父子文件夹。支持按盘符结构伪删除。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void initConfigFields() {
        addConfigField("mode", "清理模式", "select", (Object) "文件去重", 
            "清理的逻辑规则", true, 
            Arrays.asList("文件去重", "文件夹去重", "清理空目录", "直接清理"));
        addConfigField("method", "删除方式", "select", (Object) "伪删除", 
            "删除的方式", true, 
            Arrays.asList("伪删除", "直接删除", "可回滚删除"));
        addConfigField("trashPath", "回收站路径", "string", (Object) ".EchoTrash", 
            "回收站的位置", false);
        addConfigField("keepLargest", "保留体积/质量最佳的副本", "boolean", (Object) true, 
            "保留最大的文件", false);
        addConfigField("keepEarliest", "保留日期最早/最晚的副本", "boolean", (Object) true, 
            "保留日期最早的文件", false);
        addConfigField("keepExt", "优先后缀", "string", (Object) "wav", 
            "去重时优先保留的文件后缀", false);
        addConfigField("preprocessLower", "文件名转小写", "boolean", (Object) true, 
            "将文件名转换为小写后进行比较", false);
        addConfigField("preprocessUpper", "文件名转大写", "boolean", (Object) false, 
            "将文件名转换为大写后进行比较", false);
        addConfigField("preprocessSimplified", "文件名转简体中文", "boolean", (Object) false, 
            "将文件名中的繁体中文转换为简体中文后进行比较", false);
        addConfigField("sizeRange", "文件大小范围", "select", (Object) "全部", 
            "要处理的文件大小范围", false, 
            Arrays.asList("全部", "小于1MB", "小于10MB", "小于100MB", "小于1GB", 
                      "大于1MB", "大于10MB", "大于100MB", "大于1GB"));
        addConfigField("audioSpecial", "音频文件特殊处理", "boolean", (Object) true, 
            "对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "mode", (Object) "文件去重");
        setConfigValue(config, "method", (Object) "伪删除");
        setConfigValue(config, "trashPath", (Object) ".EchoTrash");
        setConfigValue(config, "keepLargest", (Object) true);
        setConfigValue(config, "keepEarliest", (Object) true);
        setConfigValue(config, "keepExt", (Object) "wav");
        setConfigValue(config, "preprocessLower", (Object) true);
        setConfigValue(config, "preprocessUpper", (Object) false);
        setConfigValue(config, "preprocessSimplified", (Object) false);
        setConfigValue(config, "sizeRange", (Object) "全部");
        setConfigValue(config, "audioSpecial", (Object) true);
    }
}