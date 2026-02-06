package com.filemanager.plugin.impl.fileunzip.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum OutputMode implements PluginEnum {
    
    AUTO_SUBDIRECTORY("auto_subdirectory", "自动创建子目录", "Auto Subdirectory", "自动创建子目录", "Automatically create subdirectory"),
    CURRENT_DIRECTORY("current_directory", "解压到当前目录", "Current Directory", "解压到当前目录", "Extract to current directory"),
    SPECIFIED_DIRECTORY("specified_directory", "指定目录", "Specified Directory", "指定目录", "Specified directory");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    OutputMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
        this.code = code;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
    }
    
    @Override
    public String getCode() {
        return code;
    }
    
    @Override
    public String getNameZh() {
        return nameZh;
    }
    
    @Override
    public String getNameEn() {
        return nameEn;
    }
    
    @Override
    public String getDescriptionZh() {
        return descriptionZh;
    }
    
    @Override
    public String getDescriptionEn() {
        return descriptionEn;
    }
    
    public boolean isAutoSubdirectory() {
        return this == AUTO_SUBDIRECTORY;
    }
    
    public boolean isCurrentDirectory() {
        return this == CURRENT_DIRECTORY;
    }
    
    public boolean isSpecifiedDirectory() {
        return this == SPECIFIED_DIRECTORY;
    }
    
    public boolean requiresCustomPath() {
        return this == SPECIFIED_DIRECTORY;
    }
    
    public static OutputMode fromCode(String code) {
        return PluginEnum.fromCode(code, OutputMode.class, AUTO_SUBDIRECTORY);
    }
}
