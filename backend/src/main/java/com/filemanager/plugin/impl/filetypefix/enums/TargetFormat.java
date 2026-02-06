package com.filemanager.plugin.impl.filetypefix.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum TargetFormat implements PluginEnum {
    
    AUTO_DETECT("auto_detect", "自动检测", "Auto Detect", "自动检测文件格式", "Automatically detect file format"),
    WAV("wav", "WAV", "WAV", "WAV音频格式", "WAV audio format"),
    FLAC("flac", "FLAC", "FLAC", "FLAC无损音频格式", "FLAC lossless audio format"),
    MP3("mp3", "MP3", "MP3", "MP3音频格式", "MP3 audio format"),
    AAC("aac", "AAC", "AAC", "AAC音频格式", "AAC audio format"),
    OGG("ogg", "OGG", "OGG", "OGG音频格式", "OGG audio format");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    TargetFormat(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isAutoDetect() {
        return this == AUTO_DETECT;
    }
    
    public boolean isSpecificFormat() {
        return this != AUTO_DETECT;
    }
    
    public static TargetFormat fromCode(String code) {
        return PluginEnum.fromCode(code, TargetFormat.class, AUTO_DETECT);
    }
}
