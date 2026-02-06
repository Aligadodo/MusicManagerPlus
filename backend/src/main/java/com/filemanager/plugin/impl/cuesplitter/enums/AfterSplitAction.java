package com.filemanager.plugin.impl.cuesplitter.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum AfterSplitAction implements PluginEnum {
    
    DO_NOTHING("do_nothing", "什么都不做 (默认)", "Do Nothing", "不进行任何操作", "Do nothing"),
    DELETE_SOURCE("delete_source", "删除原始文件", "Delete Source", "删除原始音频文件", "Delete original audio file"),
    ARCHIVE_SOURCE("archive_source", "归档原始文件", "Archive Source", "归档原始音频文件", "Archive original audio file");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    AfterSplitAction(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isDoNothing() {
        return this == DO_NOTHING;
    }
    
    public boolean isDeleteSource() {
        return this == DELETE_SOURCE;
    }
    
    public boolean isArchiveSource() {
        return this == ARCHIVE_SOURCE;
    }
    
    public boolean requiresArchiveDir() {
        return this == ARCHIVE_SOURCE;
    }
    
    public static AfterSplitAction fromCode(String code) {
        return PluginEnum.fromCode(code, AfterSplitAction.class, DO_NOTHING);
    }
}
