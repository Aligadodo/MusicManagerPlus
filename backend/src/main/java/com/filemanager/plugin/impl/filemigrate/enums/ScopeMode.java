package com.filemanager.plugin.impl.filemigrate.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum ScopeMode implements PluginEnum {
    
    ALL("all", "全部", "All", "处理所有文件和目录", "Process all files and directories"),
    CURRENT_DIRECTORY("current_directory", "当前目录", "Current Directory", "仅处理当前目录", "Process only current directory"),
    SPECIFIED_DEPTH("specified_depth", "指定深度", "Specified Depth", "处理指定深度的文件和目录", "Process files and directories at specified depth");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    ScopeMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isAll() {
        return this == ALL;
    }
    
    public boolean isCurrentDirectory() {
        return this == CURRENT_DIRECTORY;
    }
    
    public boolean isSpecifiedDepth() {
        return this == SPECIFIED_DEPTH;
    }
    
    public boolean requiresDepthValue() {
        return this == SPECIFIED_DEPTH;
    }
    
    public static ScopeMode fromCode(String code) {
        return PluginEnum.fromCode(code, ScopeMode.class, ALL);
    }
}
