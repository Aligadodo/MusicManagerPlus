package com.filemanager.plugin.impl.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum RenameMode implements PluginEnum {
    
    BASED_ON_AUDIO_FILE("based_on_audio_file", "基于音频文件名", "Based on Audio File", "根据音频文件名重命名CUE文件", "Rename CUE file based on audio file name"),
    BASED_ON_DIRECTORY("based_on_directory", "基于目录名", "Based on Directory", "根据目录名重命名CUE文件", "Rename CUE file based on directory name"),
    CUSTOM("custom", "自定义", "Custom", "使用自定义模板重命名CUE文件", "Rename CUE file using custom template");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    RenameMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isBasedOnAudioFile() {
        return this == BASED_ON_AUDIO_FILE;
    }
    
    public boolean isBasedOnDirectory() {
        return this == BASED_ON_DIRECTORY;
    }
    
    public boolean isCustom() {
        return this == CUSTOM;
    }
    
    public static RenameMode fromCode(String code) {
        return PluginEnum.fromCode(code, RenameMode.class, BASED_ON_AUDIO_FILE);
    }
}
