package com.filemanager.plugin.impl.audioconverter.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum OutputDirMode implements PluginEnum {
    
    SUBDIRECTORY("subdirectory", "子目录", "Subdirectory", "在源文件所在目录下创建子目录", "Create subdirectory in source file directory"),
    SPECIFIED_DIR("specified_dir", "指定目录", "Specified Directory", "输出到指定的目录", "Output to specified directory"),
    ROOT_DIR("root_dir", "根目录", "Root Directory", "输出到根目录", "Output to root directory");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    OutputDirMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public static OutputDirMode fromCode(String code) {
        return PluginEnum.fromCode(code, OutputDirMode.class, SUBDIRECTORY);
    }
}