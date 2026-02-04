package com.filemanager.plugin.impl.advancedrename.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum ProcessScope implements PluginEnum {
    
    FILES_ONLY("files_only", "仅处理文件", "Files Only", "只处理文件，不处理文件夹", "Process files only, skip directories"),
    DIRECTORIES_ONLY("directories_only", "仅处理文件夹", "Directories Only", "只处理文件夹，不处理文件", "Process directories only, skip files"),
    ALL("all", "全部处理", "All", "处理所有文件和文件夹", "Process all files and directories");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    ProcessScope(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean shouldProcessFiles() {
        return this == FILES_ONLY || this == ALL;
    }
    
    public boolean shouldProcessDirectories() {
        return this == DIRECTORIES_ONLY || this == ALL;
    }
    
    public static ProcessScope fromCode(String code) {
        return PluginEnum.fromCode(code, ProcessScope.class, ALL);
    }
}