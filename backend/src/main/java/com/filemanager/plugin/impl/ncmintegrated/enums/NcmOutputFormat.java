package com.filemanager.plugin.impl.ncmintegrated.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum NcmOutputFormat implements PluginEnum {
    
    MP3("mp3", "MP3", "MP3", "MP3音频格式", "MP3 audio format"),
    FLAC("flac", "FLAC", "FLAC", "FLAC无损音频格式", "FLAC lossless audio format"),
    WAV("wav", "WAV", "WAV", "WAV无损音频格式", "WAV lossless audio format");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    NcmOutputFormat(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isMp3() {
        return this == MP3;
    }
    
    public boolean isFlac() {
        return this == FLAC;
    }
    
    public boolean isWav() {
        return this == WAV;
    }
    
    public static NcmOutputFormat fromCode(String code) {
        return PluginEnum.fromCode(code, NcmOutputFormat.class, MP3);
    }
}
