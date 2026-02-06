package com.filemanager.plugin.impl.filecleanup;

import com.filemanager.domain.dto.StrategyConfigDTO;
import com.filemanager.plugin.AbstractConfigurableStrategy;
import com.filemanager.plugin.impl.filecleanup.enums.CleanupMode;
import com.filemanager.plugin.impl.filecleanup.enums.DeleteMethod;
import com.filemanager.plugin.impl.filecleanup.enums.FileSizeRange;

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
        addConfigField("mode", "清理模式", "select", (Object) CleanupMode.FILE_DUPLICATE.getCode(), 
            "清理的逻辑规则", true, 
            getCleanupModeOptions());
        addConfigField("method", "删除方式", "select", (Object) DeleteMethod.PSEUDO_DELETE.getCode(), 
            "删除的方式", true, 
            getDeleteMethodOptions());
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
        addConfigField("sizeRange", "文件大小范围", "select", (Object) FileSizeRange.ALL.getCode(), 
            "要处理的文件大小范围", false, 
            getFileSizeRangeOptions());
        addConfigField("audioSpecial", "音频文件特殊处理", "boolean", (Object) true, 
            "对音频文件进行特殊处理，确保时间长度一致时优先保留质量较高的文件", false);
    }

    @Override
    protected void initDefaultConfigValues(StrategyConfigDTO config) {
        setConfigValue(config, "mode", (Object) CleanupMode.FILE_DUPLICATE.getCode());
        setConfigValue(config, "method", (Object) DeleteMethod.PSEUDO_DELETE.getCode());
        setConfigValue(config, "trashPath", (Object) ".EchoTrash");
        setConfigValue(config, "keepLargest", (Object) true);
        setConfigValue(config, "keepEarliest", (Object) true);
        setConfigValue(config, "keepExt", (Object) "wav");
        setConfigValue(config, "preprocessLower", (Object) true);
        setConfigValue(config, "preprocessUpper", (Object) false);
        setConfigValue(config, "preprocessSimplified", (Object) false);
        setConfigValue(config, "sizeRange", (Object) FileSizeRange.ALL.getCode());
        setConfigValue(config, "audioSpecial", (Object) true);
    }
    
    private java.util.List<String> getCleanupModeOptions() {
        return java.util.Arrays.asList(
            CleanupMode.FILE_DUPLICATE.getCode(),
            CleanupMode.DIRECTORY_DUPLICATE.getCode(),
            CleanupMode.EMPTY_DIRECTORY.getCode(),
            CleanupMode.DIRECT_CLEANUP.getCode()
        );
    }
    
    private java.util.List<String> getDeleteMethodOptions() {
        return java.util.Arrays.asList(
            DeleteMethod.PSEUDO_DELETE.getCode(),
            DeleteMethod.DIRECT_DELETE.getCode(),
            DeleteMethod.ROLLBACK_DELETE.getCode()
        );
    }
    
    private java.util.List<String> getFileSizeRangeOptions() {
        return java.util.Arrays.asList(
            FileSizeRange.ALL.getCode(),
            FileSizeRange.LESS_THAN_1MB.getCode(),
            FileSizeRange.LESS_THAN_10MB.getCode(),
            FileSizeRange.LESS_THAN_100MB.getCode(),
            FileSizeRange.LESS_THAN_1GB.getCode(),
            FileSizeRange.GREATER_THAN_1MB.getCode(),
            FileSizeRange.GREATER_THAN_10MB.getCode(),
            FileSizeRange.GREATER_THAN_100MB.getCode(),
            FileSizeRange.GREATER_THAN_1GB.getCode()
        );
    }
}
