package com.filemanager.plugin.impl.ncmintegrated.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum NcmOperationMode implements PluginEnum {
    
    CONVERT("convert", "转换", "Convert", "转换NCM格式为普通音频格式", "Convert NCM format to standard audio format"),
    CACHE_CONVERT("cache_convert", "缓存转换", "Cache Convert", "从缓存转换NCM格式", "Convert NCM format from cache"),
    LYRICS_DOWNLOAD("lyrics_download", "歌词下载", "Lyrics Download", "下载歌曲歌词", "Download song lyrics"),
    METADATA_FIX("metadata_fix", "元数据修复", "Metadata Fix", "修复音频文件的元数据信息", "Fix metadata of audio files");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    
    NcmOperationMode(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn) {
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
    
    public boolean isConvert() {
        return this == CONVERT;
    }
    
    public boolean isCacheConvert() {
        return this == CACHE_CONVERT;
    }
    
    public boolean isLyricsDownload() {
        return this == LYRICS_DOWNLOAD;
    }
    
    public boolean isMetadataFix() {
        return this == METADATA_FIX;
    }
    
    public static NcmOperationMode fromCode(String code) {
        return PluginEnum.fromCode(code, NcmOperationMode.class, CONVERT);
    }
}
