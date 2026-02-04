package com.filemanager.plugin.impl.audioconverter.enums;

import com.filemanager.plugin.enums.PluginEnum;

public enum AudioFormat implements PluginEnum {
    
    WAV_CD_STANDARD("wav_cd_standard", "WAV (CD标准)", "WAV (CD Standard)", "CD标准WAV格式，44.1kHz 16bit立体声", "CD standard WAV format, 44.1kHz 16bit stereo", "wav"),
    WAV("wav", "WAV", "WAV", "标准WAV格式", "Standard WAV format", "wav"),
    FLAC("flac", "FLAC", "FLAC", "无损压缩音频格式", "Lossless audio compression format", "flac"),
    MP3("mp3", "MP3", "MP3", "有损压缩音频格式", "Lossy audio compression format", "mp3"),
    ALAC("alac", "ALAC", "ALAC", "苹果无损音频格式", "Apple Lossless Audio Codec", "m4a"),
    AAC("aac", "AAC", "AAC", "高级音频编码格式", "Advanced Audio Coding", "aac"),
    OGG("ogg", "OGG", "OGG", "开源音频格式", "Open source audio format", "ogg");
    
    private final String code;
    private final String nameZh;
    private final String nameEn;
    private final String descriptionZh;
    private final String descriptionEn;
    private final String extension;
    
    AudioFormat(String code, String nameZh, String nameEn, String descriptionZh, String descriptionEn, String extension) {
        this.code = code;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.descriptionZh = descriptionZh;
        this.descriptionEn = descriptionEn;
        this.extension = extension;
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
    
    public String getExtension() {
        return extension;
    }
    
    public String getFFmpegFormat() {
        if (this == ALAC) {
            return "ipod";
        }
        return extension;
    }
    
    public static AudioFormat fromCode(String code) {
        return PluginEnum.fromCode(code, AudioFormat.class, WAV_CD_STANDARD);
    }
}