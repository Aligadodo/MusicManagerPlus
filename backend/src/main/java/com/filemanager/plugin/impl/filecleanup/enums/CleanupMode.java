package com.filemanager.plugin.impl.filecleanup.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum CleanupMode implements PluginEnum {
    
    FILE_DUPLICATE("file_duplicate", "文件去重", "File Duplicate", "清理重复文件", "Clean up duplicate files"),
    DIRECTORY_DUPLICATE("directory_duplicate", "文件夹去重", "Directory Duplicate", "清理重复文件夹", "Clean up duplicate directories"),
    EMPTY_DIRECTORY("empty_directory", "清理空目录", "Empty Directory", "清理空目录", "Clean up empty directories"),
    DIRECT_CLEANUP("direct_cleanup", "直接清理", "Direct Cleanup", "直接清理文件", "Direct cleanup of files");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    CleanupMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isFileDuplicate() {
        return this == FILE_DUPLICATE;
    }
    
    public boolean isDirectoryDuplicate() {
        return this == DIRECTORY_DUPLICATE;
    }
    
    public boolean isEmptyDirectory() {
        return this == EMPTY_DIRECTORY;
    }
    
    public boolean isDirectCleanup() {
        return this == DIRECT_CLEANUP;
    }
    
    public static CleanupMode fromCode(String code) {
        return PluginEnum.fromCode(code, CleanupMode.class, FILE_DUPLICATE);
    }
}
