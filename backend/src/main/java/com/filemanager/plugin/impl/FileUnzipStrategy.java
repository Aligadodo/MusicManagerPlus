package com.filemanager.plugin.impl;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import java.util.Arrays;
import java.util.ArrayList;

public class FileUnzipStrategy extends AbstractConfigurableStrategy {

    public FileUnzipStrategy() {
        super();
    }

    @Override
    public String getId() {
        return "file-unzip";
    }

    @Override
    public String getName() {
        return "批量智能解压";
    }

    @Override
    public String getDescription() {
        return "批量智能解压工具，支持多种解压引擎和智能目录处理。";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void initConfigFields() {
        addConfigField("engine", "解压引擎", "select", (Object) "Java 内置引擎", 
            "解压引擎选择", true, 
            Arrays.asList("Java 内置引擎", "7-Zip 引擎", "Bandizip 命令行工具"));
        addConfigField("exePath", "可执行文件路径", "string", (Object) "", 
            "外部解压工具的可执行文件路径", false);
        addConfigField("outputMode", "输出模式", "select", (Object) "自动创建子目录", 
            "输出目录模式", true, 
            Arrays.asList("自动创建子目录", "解压到当前目录", "指定目录"));
        addConfigField("customPath", "自定义路径", "directory", (Object) "", 
            "自定义输出路径", false);
        addConfigField("smartFolder", "智能文件夹", "boolean", (Object) true, 
            "智能识别解压后的文件夹结构", false);
        addConfigField("mergeSameName", "合并同名文件夹", "boolean", (Object) false, 
            "合并同名的文件夹", false);
        addConfigField("deleteSource", "解压成功后删除源文件", "boolean", (Object) false, 
            "解压成功后删除原始压缩文件", false);
        addConfigField("overwrite", "覆盖已存在文件", "boolean", (Object) false, 
            "覆盖已存在的文件", false);
        addConfigField("deleteOnFail", "解压失败后删除源文件", "boolean", (Object) false, 
            "解压失败后删除原始压缩文件", false);
        addConfigField("nestedFolderMerge", "嵌套文件夹合并", "boolean", (Object) false, 
            "合并嵌套的文件夹", false);
        addConfigField("passwords", "密码列表", "list", (Object) new ArrayList<>(), 
            "解压密码列表", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "engine", (Object) "Java 内置引擎");
        setConfigValue(config, "exePath", (Object) "");
        setConfigValue(config, "outputMode", (Object) "自动创建子目录");
        setConfigValue(config, "customPath", (Object) "");
        setConfigValue(config, "smartFolder", (Object) true);
        setConfigValue(config, "mergeSameName", (Object) false);
        setConfigValue(config, "deleteSource", (Object) false);
        setConfigValue(config, "overwrite", (Object) false);
        setConfigValue(config, "deleteOnFail", (Object) false);
        setConfigValue(config, "nestedFolderMerge", (Object) false);
        setConfigValue(config, "passwords", (Object) new ArrayList<>());
    }
}